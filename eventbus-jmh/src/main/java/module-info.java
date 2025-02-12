/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
open module net.minecraftforge.eventbus.jmh {
    requires net.minecraftforge.eventbus;
    requires org.jspecify;
    requires jmh.core;
    requires jdk.unsupported; // needed by JMH for Unsafe

    requires net.minecraftforge.eventbus.testjars;

    exports net.minecraftforge.eventbus.benchmarks;
}
