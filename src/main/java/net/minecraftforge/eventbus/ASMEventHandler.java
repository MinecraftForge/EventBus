/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class ASMEventHandler implements IEventListener
{
    private static final String HANDLER_DESC = Type.getInternalName(IEventListener.class);
    private static final String HANDLER_FUNC_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Event.class));
    private static final HashMap<String, Method> PENDING = new HashMap<>();

    private final IEventListener handler;
    private final SubscribeEvent subInfo;
    private String readable;
    private java.lang.reflect.Type filter = null;

    public ASMEventHandler(Object target, Method method, boolean isGeneric) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        enqueueWrapper(method);
        if (Modifier.isStatic(method.getModifiers()))
            handler = (IEventListener)Class.forName(getUniqueName(method), true, Thread.currentThread().getContextClassLoader()).getDeclaredConstructor().newInstance();
        else
            handler = (IEventListener)Class.forName(getUniqueName(method), true, Thread.currentThread().getContextClassLoader()).getConstructor(Object.class).newInstance(target);
        subInfo = method.getAnnotation(SubscribeEvent.class);
        readable = "ASM: " + target + " " + method.getName() + Type.getMethodDescriptor(method);
        if (isGeneric)
        {
            java.lang.reflect.Type type = method.getGenericParameterTypes()[0];
            if (type instanceof ParameterizedType)
            {
                filter = ((ParameterizedType)type).getActualTypeArguments()[0];
                if (filter instanceof ParameterizedType) // Unlikely that nested generics will ever be relevant for event filtering, so discard them
                {
                    filter = ((ParameterizedType)filter).getRawType();
                }
                else if (filter instanceof WildcardType)
                {
                    // If there's a wildcard filter of Object.class, then remove the filter.
                    final WildcardType wfilter = (WildcardType) filter;
                    if (wfilter.getUpperBounds().length == 1 && wfilter.getUpperBounds()[0] == Object.class && wfilter.getLowerBounds().length == 0) {
                        filter = null;
                    }
                }
            }
        }
    }

    public static boolean hasPendingWrapperClass(final String className) {
        return PENDING.containsKey(className);
    }

    public static void processWrapperClass(final String className, final ClassNode node) {
        Method meth = PENDING.get(className);
        transformNode(className, meth, node);
    }
    @SuppressWarnings("rawtypes")
    @Override
    public void invoke(Event event)
    {
        if (handler != null)
        {
            if (!event.isCancelable() || !event.isCanceled() || subInfo.receiveCanceled())
            {
                if (filter == null || filter == ((IGenericEvent)event).getGenericType())
                {
                    handler.invoke(event);
                }
            }
        }
    }

    public EventPriority getPriority()
    {
        return subInfo.priority();
    }

    private void enqueueWrapper(Method callback) {
        String name = getUniqueName(callback);
        PENDING.putIfAbsent(name, callback);
    }


    private static void transformNode(String name, Method callback, ClassNode target) {
        MethodVisitor mv;

        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        String desc = name.replace('.',  '/');
        String instType = Type.getInternalName(callback.getDeclaringClass());
        String eventType = Type.getInternalName(callback.getParameterTypes()[0]);

        /*
        System.out.println("Name:     " + name);
        System.out.println("Desc:     " + desc);
        System.out.println("InstType: " + instType);
        System.out.println("Callback: " + callback.getName() + Type.getMethodDescriptor(callback));
        System.out.println("Event:    " + eventType);
        */

        target.visit(V16, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[]{ HANDLER_DESC });

        target.visitSource(".dynamic", null);
        {
            if (!isStatic)
                target.visitField(ACC_PUBLIC, "instance", "Ljava/lang/Object;", null, null).visitEnd();
        }
        {
            mv = target.visitMethod(ACC_PUBLIC, "<init>", isStatic ? "()V" : "(Ljava/lang/Object;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            if (!isStatic)
            {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, desc, "instance", "Ljava/lang/Object;");
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = target.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            if (!isStatic)
            {
                mv.visitFieldInsn(GETFIELD, desc, "instance", "Ljava/lang/Object;");
                mv.visitTypeInsn(CHECKCAST, instType);
            }
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, eventType);
            mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, instType, callback.getName(), Type.getMethodDescriptor(callback), false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        target.visitEnd();
    }

    private String getUniqueName(Method callback)
    {
        return String.format("%s.__%s_%s_%s", callback.getDeclaringClass().getPackageName(), callback.getDeclaringClass().getSimpleName(),
                callback.getName(),
                callback.getParameterTypes()[0].getSimpleName());
    }

    public String toString()
    {
        return readable;
    }
}
