/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import static net.minecraftforge.eventbus.LogMarkers.EVENTBUS;
import static net.minecraftforge.eventbus.Names.SUBSCRIBE_EVENT;

public class EventAccessTransformer {
    private static final Logger LOGGER = LogManager.getLogger();

    public boolean transform(ClassNode classNode, Type classType) {
        boolean changed = false;

        for (var method : classNode.methods) {
            if (!hasAnnotation(method)) continue;
            if (Modifier.isPrivate(method.access)) {
                LOGGER.error(EVENTBUS, "Illegal private member annotated as @SubscribeEvent : {}.{}", classNode.name, method.name);
                throw new RuntimeException("Illegal private member with @SubscribeEvent annotation");
            } else {
                LOGGER.debug(EVENTBUS, "Transforming @SubscribeEvent method to public {}.{}", classNode.name, method.name);
                int access = classNode.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
                if (classNode.access != access) {
                    classNode.access = access;
                    changed = true;
                }

                access = method.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
                if (method.access != access) {
                    method.access = access;
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean hasAnnotation(MethodNode method) {
        if (method.visibleAnnotations == null)
            return false;
        for (var ann : method.visibleAnnotations) {
            if (SUBSCRIBE_EVENT.equals(ann.desc))
                return true;
        }
        return false;
    }
}
