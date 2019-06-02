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

import net.minecraftforge.eventbus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Optional;

import static net.minecraftforge.eventbus.LogMarkers.EVENTBUS;
import static net.minecraftforge.eventbus.Names.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class EventSubclassTransformer
{

    private static final Logger LOGGER = LogManager.getLogger();

    public Optional<ClassNode> transform(final ClassNode classNode, final Type classType)
    {
        try
        {
            if (!buildEvents(classNode)) return Optional.empty();
        }
        catch (Exception e)
        {
            LOGGER.error(EVENTBUS, "An error occurred building event handler", e);
        }
        return Optional.of(classNode);
    }

    private boolean buildEvents(ClassNode classNode) throws Exception
    {
        // Yes, this recursively loads classes until we get this base class. THIS IS NOT A ISSUE. Coremods should handle re-entry just fine.
        // If they do not this a COREMOD issue NOT a Forge/LaunchWrapper issue.
        // well, we should at least use the context classloader - this is forcing all the game classes in through
        // the system classloader otherwise...
        Class<?> parent = null;
        try
        {
            parent = Thread.currentThread().getContextClassLoader().loadClass(classNode.superName.replace('/', '.'));
        }
        catch (ClassNotFoundException e)
        {
            LOGGER.error(EVENTBUS, "Could not find parent {} for class {} in classloader {} on thread {}", classNode.superName, classNode.name, Thread.currentThread().getContextClassLoader(), Thread.currentThread());
            throw e;
        }

        if (!Event.class.isAssignableFrom(parent))
        {
            return false;
        }

        LOGGER.debug(EVENTBUS, "Event transform begin: {}", classNode.name);
        //Class<?> listenerListClazz = Class.forName("net.minecraftforge.fml.common.eventhandler.ListenerList", false, getClass().getClassLoader());
        Type tList = Type.getType(LISTENER_LIST);

        boolean hasSetup           = false;
        boolean hasGetListenerList = false;
        boolean hasDefaultCtr      = false;
        boolean hasCancelable      = false;
        boolean hasResult          = false;
        String voidDesc            = Type.getMethodDescriptor(VOID_TYPE);
        String boolDesc            = Type.getMethodDescriptor(BOOLEAN_TYPE);
        String listDesc            = tList.getDescriptor();
        String listDescM           = Type.getMethodDescriptor(tList);

        for (MethodNode method : classNode.methods)
        {
            if (method.name.equals("setup") && method.desc.equals(voidDesc) && (method.access & ACC_PROTECTED) == ACC_PROTECTED) hasSetup = true;
            if ((method.access & ACC_PUBLIC) == ACC_PUBLIC)
            {
                if (method.name.equals("getListenerList") && method.desc.equals(listDescM)) hasGetListenerList = true;
                if (method.name.equals("isCancelable")    && method.desc.equals(boolDesc))  hasCancelable = true;
                if (method.name.equals("hasResult")       && method.desc.equals(boolDesc))  hasResult = true;
            }
            if (method.name.equals("<init>") && method.desc.equals(voidDesc)) hasDefaultCtr = true;
        }

        if (classNode.visibleAnnotations != null)
        {
            for (AnnotationNode node : classNode.visibleAnnotations)
            {
                if (!hasResult && node.desc.equals(HAS_RESULT))
                {
                    /* Add:
                     *      public boolean hasResult()
                     *      {
                     *            return true;
                     *      }
                     */
                    MethodNode method = new MethodNode(ACC_PUBLIC, "hasResult", boolDesc, null, null);
                    method.instructions.add(new InsnNode(ICONST_1));
                    method.instructions.add(new InsnNode(IRETURN));
                    classNode.methods.add(method);
                }
                else if (!hasCancelable && node.desc.equals(CANCELLABLE))
                {
                    /* Add:
                     *      public boolean isCancelable()
                     *      {
                     *            return true;
                     *      }
                     */
                    MethodNode method = new MethodNode(ACC_PUBLIC, "isCancelable", boolDesc, null, null);
                    method.instructions.add(new InsnNode(ICONST_1));
                    method.instructions.add(new InsnNode(IRETURN));
                    classNode.methods.add(method);
                }
            }
        }

        if (hasSetup)
        {
            if (!hasGetListenerList) {
                LOGGER.error(EVENTBUS, "Event class {} defines a custom setup() method and is missing getListenerList", classNode.name);
                throw new RuntimeException("Event class defines setup() but does not define getListenerList! " + classNode.name);
            } else {
                LOGGER.debug(EVENTBUS, "Transforming event complete - already done: {}", classNode.name);
                return true;
            }
        }

        Type tThis = Type.getObjectType(classNode.name);
        Type tSuper = Type.getObjectType(classNode.superName);

        //Add private static volatile ListenerList LISTENER_LIST
        classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_VOLATILE, "LISTENER_LIST", listDesc, null, null));

        /*Add:
         *      public <init>()
         *      {
         *              super();
         *      }
         */
        if (!hasDefaultCtr)
        {
            MethodNode method = new MethodNode(ACC_PUBLIC, "<init>", voidDesc, null, null);
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL, tSuper.getInternalName(), "<init>", voidDesc, false));
            method.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(method);
        }

        MethodNode method = generateSetupMethod(tThis, tSuper, tList);
        classNode.methods.add(method);

        /*Add:
         *      public ListenerList getListenerList()
         *      {
         *              return this.LISTENER_LIST;
         *      }
         */
        method = new MethodNode(ACC_PUBLIC, "getListenerList", listDescM, null, null);
        method.instructions.add(new FieldInsnNode(GETSTATIC, classNode.name, "LISTENER_LIST", listDesc));
        method.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(method);
        LOGGER.debug(EVENTBUS, "Event transform complete: {}", classNode.name);
        return true;
    }


    /*
        protected void setup() {
            super.setup();
            if (LISTENER_LIST != null) return;
            synchronized (getClass()) {
                if (LISTENER_LIST != null) return;
                LISTENER_LIST = new ListenerList(this.getParentListenerList());
            }
        }
     */
    private MethodNode generateSetupMethod(Type thisType, Type superType, Type llType) {
        Type objType = Type.getType(Object.class);
        Type clzType = Type.getType(Class.class);
        MethodNode methodVisitor = new MethodNode(ACC_PROTECTED, "setup", getMethodDescriptor(VOID_TYPE), null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        Label label1 = new Label();
        Label label2 = new Label();
        methodVisitor.visitTryCatchBlock(label0, label1, label2, null);
        Label label3 = new Label();
        Label label4 = new Label();
        methodVisitor.visitTryCatchBlock(label3, label4, label2, null);
        Label label5 = new Label();
        methodVisitor.visitTryCatchBlock(label2, label5, label2, null);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, superType.getInternalName(), "setup", getMethodDescriptor(VOID_TYPE), false);
        methodVisitor.visitFieldInsn(GETSTATIC, thisType.getInternalName(), "LISTENER_LIST", llType.getDescriptor());
        Label label8 = new Label();
        methodVisitor.visitJumpInsn(IFNULL, label8);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitLabel(label8);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, objType.getInternalName(), "getClass", getMethodDescriptor(clzType), false);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ASTORE, 1);
        methodVisitor.visitInsn(MONITORENTER);
        methodVisitor.visitLabel(label0);
        methodVisitor.visitFieldInsn(GETSTATIC, thisType.getInternalName(), "LISTENER_LIST", llType.getDescriptor());
        methodVisitor.visitJumpInsn(IFNULL, label3);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitInsn(MONITOREXIT);
        methodVisitor.visitLabel(label1);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{objType.getInternalName()}, 0, null);
        methodVisitor.visitTypeInsn(NEW, llType.getInternalName());
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, thisType.getInternalName(), "getParentListenerList", getMethodDescriptor(llType), false);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, llType.getInternalName(), "<init>", getMethodDescriptor(VOID_TYPE, llType), false);
        methodVisitor.visitFieldInsn(PUTSTATIC, thisType.getInternalName(), "LISTENER_LIST", llType.getDescriptor());
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitInsn(MONITOREXIT);
        methodVisitor.visitLabel(label4);
        Label label10 = new Label();
        methodVisitor.visitJumpInsn(GOTO, label10);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
        methodVisitor.visitVarInsn(ASTORE, 2);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitInsn(MONITOREXIT);
        methodVisitor.visitLabel(label5);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitInsn(ATHROW);
        methodVisitor.visitLabel(label10);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitEnd();
        return methodVisitor;
    }
}
