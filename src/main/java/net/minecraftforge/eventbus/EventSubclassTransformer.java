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
            if (EVENT.equals(classType.getDescriptor())) transformEvent(classNode);
            else if (!buildEvents(classNode)) return Optional.empty();
        }
        catch (Exception e)
        {
            LOGGER.error(EVENTBUS, "An error occurred building event handler", e);
        }
        return Optional.of(classNode);
    }

    /*
     * This transformers Event class itself, to use more efficient versions of various methods,
     * as in ones that don't need reflection.
     */
    private void transformEvent(ClassNode cls) throws Exception
    {
        for (MethodNode method : cls.methods)
        {
            // hasResult/isCancelable methods will be overwritten by the transformer in subclasses, so just `return false`
            if (CANCELABLE_M.equals(method) || HAS_RESULT_M.equals(method)) {
                clear(method);
                method.instructions.add(new InsnNode(ICONST_0));
                method.instructions.add(new InsnNode(IRETURN));
            }
        }
        // Add LISTENER_LIST static field, and replace getListenerList with `return Event.LISTENER_LIST`
        addListenerList(cls, false);
    }

    private static void clear(MethodNode mtd) {
        mtd.instructions.clear();
        mtd.localVariables.clear();
        if (mtd.tryCatchBlocks != null)
            mtd.tryCatchBlocks.clear();
        if (mtd.visibleLocalVariableAnnotations != null)
            mtd.visibleLocalVariableAnnotations.clear();
        if (mtd.invisibleLocalVariableAnnotations != null)
            mtd.invisibleLocalVariableAnnotations.clear();
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

        boolean hasGetListenerList = false;
        boolean hasDefaultCtr      = false;
        boolean hasCancelable      = false;
        boolean hasResult          = false;

        for (MethodNode method : classNode.methods)
        {
            if ((method.access & ACC_PUBLIC) == ACC_PUBLIC)
            {
                     if (LISTENER_LIST_GET.equals(method)) hasGetListenerList = true;
                else if (CANCELABLE_M.equals(method))      hasCancelable = true;
                else if (HAS_RESULT_M.equals(method))      hasResult = true;
            }
            if (INIT_M.equals(method)) hasDefaultCtr = true;
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
                    MethodNode method = new MethodNode(ACC_PUBLIC, HAS_RESULT_M.name(), HAS_RESULT_M.desc(), null, null);
                    method.instructions.add(new InsnNode(ICONST_1));
                    method.instructions.add(new InsnNode(IRETURN));
                    classNode.methods.add(method);
                    hasResult = true;
                }
                else if (!hasCancelable && node.desc.equals(CANCELLABLE))
                {
                    /* Add:
                     *      public boolean isCancelable()
                     *      {
                     *            return true;
                     *      }
                     */
                    MethodNode method = new MethodNode(ACC_PUBLIC, CANCELABLE_M.name(), CANCELABLE_M.desc(), null, null);
                    method.instructions.add(new InsnNode(ICONST_1));
                    method.instructions.add(new InsnNode(IRETURN));
                    classNode.methods.add(method);
                    hasCancelable = true;
                }
            }
        }

        /* If our parent is Event itself, inject hasResult/isCancelable to bypass the
         * map based default implementation. Children will have their own overrides
         * injected to change this value. This is done in case Event itself can't be
         * transformed to use the optimized system.
         */
        if (parent == Event.class) {
            if (!hasResult) {
                /* Add:
                 *      public boolean hasResult()
                 *      {
                 *            return false;
                 *      }
                 */
                MethodNode method = new MethodNode(ACC_PUBLIC, HAS_RESULT_M.name(), HAS_RESULT_M.desc(), null, null);
                method.instructions.add(new InsnNode(ICONST_0));
                method.instructions.add(new InsnNode(IRETURN));
                classNode.methods.add(method);
            }
            if (!hasCancelable) {
                /* Add:
                 *      public boolean isCancelable()
                 *      {
                 *            return false;
                 *      }
                 */
                MethodNode method = new MethodNode(ACC_PUBLIC, CANCELABLE_M.name(), CANCELABLE_M.desc(), null, null);
                method.instructions.add(new InsnNode(ICONST_1));
                method.instructions.add(new InsnNode(IRETURN));
                classNode.methods.add(method);
            }
        }

        Type tSuper = Type.getObjectType(classNode.superName);

        /*Add:
         *      public <init>()
         *      {
         *              super();
         *      }
         */
        if (!hasDefaultCtr)
        {
            MethodNode method = new MethodNode(ACC_PUBLIC, INIT_M.name(), INIT_M.desc(), null, null);
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL, tSuper.getInternalName(), INIT_M.name(), INIT_M.desc(), false));
            method.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(method);
        }

        if (hasGetListenerList) {
            LOGGER.debug(EVENTBUS, "Transforming event complete - already done: {}", classNode.name);
            return true;
        }

        return addListenerList(classNode, true);
    }

    private boolean addListenerList(ClassNode classNode, boolean useSuper)
    {
        Type tList   = getType(LISTENER_LIST);
        Type tHelper = Type.getType(LISTENER_LIST_HELPER);
        Type tThis   = getObjectType(classNode.name);

        /* Add:
         *     private static final ListenerList LISTENER_LIST;
         */
        classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, LISTENER_LIST_F.name(), LISTENER_LIST_F.desc(), null, null));

        /* Add:
         *     static
         *     {
         *         LISTENER_LIST = new ListenerList(EventListenerHelper.getListenerList(CLAZZ.class.getSuperclass());
         *         or
         *         LISTENER_LIST = new ListenerList();
         *     }
         */
        InsnList clinit = new InsnList();
        clinit.add(new TypeInsnNode(NEW, tList.getInternalName()));
        clinit.add(new InsnNode(DUP));
        if (useSuper)
        {
            // lst = new ListenerList(EventListenerHelper.getListenerList(CLAZZ.class.getSuperclass())
            clinit.add(new LdcInsnNode(tThis));
            clinit.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false));
            clinit.add(new MethodInsnNode(INVOKESTATIC, tHelper.getInternalName(), "getListenerList", getMethodDescriptor(tList, getType(Class.class)), false));
            clinit.add(new MethodInsnNode(INVOKESPECIAL, tList.getInternalName(), LISTENER_LIST_INIT.name(), LISTENER_LIST_INIT.desc(), false));
        }
        else
        {
            // lst = new ListenerList()
            clinit.add(new MethodInsnNode(INVOKESPECIAL, tList.getInternalName(), INIT_M.name(), INIT_M.desc(), false));
        }
        // LISTENER_LIST = lst
        clinit.add(new FieldInsnNode(PUTSTATIC, tThis.getInternalName(), LISTENER_LIST_F.name(), LISTENER_LIST_F.desc()));

        MethodNode method = classNode.methods.stream().filter(STATIC_INIT_M::equals).findFirst().orElse(null);
        if (method == null)
        {
            method = new MethodNode(ACC_STATIC, STATIC_INIT_M.name(), STATIC_INIT_M.desc(), null, null);
            method.instructions.add(clinit);
            method.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(method);
        }
        else
        {
            // If a static initializer already exists add our code at the beginning.
            method.instructions.insert(clinit);
        }

        /*Add or replace:
         *      public ListenerList getListenerList()
         *      {
         *              return this.LISTENER_LIST;
         *      }
         */
        method = classNode.methods.stream().filter(LISTENER_LIST_GET::equals).findFirst().orElse(null);
        if (method == null)
            method = new MethodNode(ACC_PUBLIC, LISTENER_LIST_GET.name(), LISTENER_LIST_GET.desc(), null, null);
        else
            clear(method);
        method.instructions.add(new FieldInsnNode(GETSTATIC, classNode.name, LISTENER_LIST_F.name(), LISTENER_LIST_F.desc()));
        method.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(method);
        LOGGER.debug(EVENTBUS, "Event transform complete: {}", classNode.name);
        return true;
    }
}
