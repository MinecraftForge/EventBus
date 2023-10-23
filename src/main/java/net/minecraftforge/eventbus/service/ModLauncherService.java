/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.service;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.eventbus.IEventBusEngine;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;
import java.util.Objects;
import java.util.ServiceLoader;

public class ModLauncherService implements ILaunchPluginService {
    private IEventBusEngine eventBusEngine;

    @Override
    public String name() {
        return "eventbus";
    }

    public IEventBusEngine getEventBusEngine() {
        if (eventBusEngine == null) {
            var service = Launcher.INSTANCE.findLayerManager().flatMap(lm->lm.getLayer(IModuleLayerManager.Layer.PLUGIN)).orElseThrow();
            this.eventBusEngine = ServiceLoader.load(service, IEventBusEngine.class).findFirst().orElseThrow();
        }
        return eventBusEngine;
    }

    @Override
    public int processClassWithFlags(final Phase phase, final ClassNode classNode, final Type classType, String reason) {
        return Objects.equals(reason, "classloading") ? getEventBusEngine().processClass(classNode, classType) : ComputeFlags.NO_REWRITE;
    }

    private static final EnumSet<Phase> DOIT = EnumSet.of(Phase.BEFORE);
    private static final EnumSet<Phase> YAY = EnumSet.of(Phase.AFTER);
    private static final EnumSet<Phase> NAY = EnumSet.noneOf(Phase.class);

    @Override
    public EnumSet<Phase> handlesClass(final Type classType, final boolean isEmpty) {
        if (isEmpty) {
            return getEventBusEngine().findASMEventDispatcher(classType) ? DOIT : NAY;
        } else {
            return getEventBusEngine().handlesClass(classType) ? YAY : NAY;
        }

    }
}
