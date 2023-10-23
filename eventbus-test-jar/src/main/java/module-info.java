/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

open module net.minecraftforge.eventbus.testjars {
    requires transitive net.minecraftforge.eventbus;

    exports net.minecraftforge.eventbus.testjar;
    exports net.minecraftforge.eventbus.benchmarks.compiled;
}