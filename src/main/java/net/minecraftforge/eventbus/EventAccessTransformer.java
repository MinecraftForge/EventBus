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
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraftforge.eventbus.LogMarkers.EVENTBUS;
import static net.minecraftforge.eventbus.Names.SUBSCRIBE_EVENT;

public class EventAccessTransformer
{

    private static final Logger LOGGER = LogManager.getLogger();

    public boolean transform(final ClassNode classNode, final Type classType)
    {
        AtomicBoolean changes = new AtomicBoolean();
        classNode.methods.stream().
                filter(m-> Optional.ofNullable(m.visibleAnnotations).
                        orElse(Collections.emptyList()).
                        stream().anyMatch(a->Objects.equals(a.desc, SUBSCRIBE_EVENT))).
                peek(m->{if (Modifier.isPrivate(m.access)) illegalPrivateAccess(m, classNode);}).
                filter(m->!Modifier.isPrivate(m.access)).
                peek(mn->LOGGER.debug(EVENTBUS, "Transforming @SubscribeEvent method to public {}.{}", classNode.name, mn.name)).
                peek($ -> classNode.access = changeAccess(classNode.access, changes)).
                forEach(mn1 -> toPublic(mn1, changes));
        return changes.get();
    }

    private void illegalPrivateAccess(final MethodNode mn, final ClassNode cn) {
        LOGGER.error(EVENTBUS, "Illegal private member annotated as @SubscribeEvent : {}.{}", cn.name, mn.name);
        throw new RuntimeException("Illegal private member with @SubscribeEvent annotation");
    }

    private static int changeAccess(final int access, AtomicBoolean changeTracking) {
        int ax = access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
        changeTracking.compareAndSet(false, ax != access);
        return ax;
    }

    private void toPublic(final MethodNode mn, AtomicBoolean changeTracking)
    {
        mn.access = changeAccess(mn.access, changeTracking);
    }
}
