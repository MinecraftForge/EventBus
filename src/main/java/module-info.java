/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
open module net.minecraftforge.eventbus {
    uses net.minecraftforge.eventbus.IEventBusEngine;
    requires static cpw.mods.modlauncher;

    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.apache.logging.log4j;
    requires static org.jetbrains.annotations;
    requires net.jodah.typetools;

    exports net.minecraftforge.eventbus;
    exports net.minecraftforge.eventbus.api;

    /**
     * Internal classes that may change or be removed at any time without notice
     */
    exports net.minecraftforge.eventbus.internal to net.minecraftforge.eventbus.test;

    provides cpw.mods.modlauncher.serviceapi.ILaunchPluginService with net.minecraftforge.eventbus.service.ModLauncherService;
    provides net.minecraftforge.eventbus.IEventBusEngine with net.minecraftforge.eventbus.EventBusEngine;
}
