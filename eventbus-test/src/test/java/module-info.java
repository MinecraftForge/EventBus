/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
open module net.minecraftforge.eventbus.test {
    requires net.minecraftforge.eventbus;
    requires net.minecraftforge.eventbus.validator;
    requires org.junit.jupiter.api;
    requires org.jspecify;
    requires java.compiler;
    requires compile.testing;
    requires org.jetbrains.annotations;
    requires net.minecraftforge.eventbus.testjars;

    exports net.minecraftforge.eventbus.test;
    exports net.minecraftforge.eventbus.test.compiletime;
}
