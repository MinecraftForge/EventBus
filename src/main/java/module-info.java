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
     * Internal classes that may change or be removed at any time without notice. Only exported for tests and may no
     * longer be exported in a future release.
     */
    exports net.minecraftforge.eventbus.internal;

    provides cpw.mods.modlauncher.serviceapi.ILaunchPluginService with net.minecraftforge.eventbus.service.ModLauncherService;
    provides net.minecraftforge.eventbus.IEventBusEngine with net.minecraftforge.eventbus.EventBusEngine;
}
