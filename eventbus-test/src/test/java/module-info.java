/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

open module net.minecraftforge.eventbus.test {
    requires net.minecraftforge.eventbus;
    requires org.junit.jupiter.api;

    // Used to bootstrap into a transformed environment. To test our transformers/posting events with optimized implementations
    requires cpw.mods.modlauncher;

    // We log stuff in the DeadlockingEventTest so we can see where it deadlocks if it does.
    requires org.apache.logging.log4j;

    // Used by MockTransformerService
    requires cpw.mods.securejarhandler;
    requires jopt.simple;
    requires org.objectweb.asm.tree;

    // Everyone wants their null safety
    requires static org.jetbrains.annotations;

    // Custom events we're testing
    requires static net.minecraftforge.eventbus.testjars;

    provides cpw.mods.modlauncher.api.ITransformationService with
        net.minecraftforge.eventbus.test.MockTransformerService;
}