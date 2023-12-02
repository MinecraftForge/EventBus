/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

open module net.minecraftforge.eventbus.testjars {
    requires transitive net.minecraftforge.eventbus;
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
	requires net.minecraftforge.unsafe;

    exports net.minecraftforge.eventbus.testjar;
    exports net.minecraftforge.eventbus.testjar.events;
    exports net.minecraftforge.eventbus.testjar.benchmarks;
}