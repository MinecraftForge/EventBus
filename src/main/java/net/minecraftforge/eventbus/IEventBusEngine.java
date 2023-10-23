/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public interface IEventBusEngine {
    int processClass(ClassNode classNode, Type classType);

    boolean handlesClass(Type classType);

    boolean findASMEventDispatcher(Type classType);
}
