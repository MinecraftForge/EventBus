/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import java.lang.reflect.Method;
import java.util.Optional;
import org.objectweb.asm.tree.ClassNode;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;

public class ModLauncherFactory extends ClassLoaderFactory {
    private static final Cache<String, Method> PENDING = InternalUtils.cache();
    private Optional<ClassLoader> gameClassLoader = null;

    @Override
    protected Class<?> createWrapper(Method callback) throws ClassNotFoundException {
        enqueueWrapper(callback);
        var name = getUniqueName(callback);
        return Class.forName(name, true, getClassLoader());
    }

    private void enqueueWrapper(Method callback) {
        String name = getUniqueName(callback);
        PENDING.computeIfAbsent(name, () -> callback);
    }

    public static boolean hasPendingWrapperClass(final String className) {
        return PENDING.get(className) != null;
    }

    public static void processWrapperClass(final String className, final ClassNode node) {
        Method meth = PENDING.get(className);
        transformNode(className, meth, node);
    }

    private ClassLoader getClassLoader() {
        var loader = Thread.currentThread().getContextClassLoader();
        if (loader != null)
            return loader;

        if (this.gameClassLoader == null) {
            var gameLayer = Launcher.INSTANCE.findLayerManager().flatMap(lm -> lm.getLayer(IModuleLayerManager.Layer.GAME)).orElseThrow();
            this.gameClassLoader = gameLayer.modules().stream().findFirst().map(Module::getClassLoader);
        }
        return this.gameClassLoader.orElse(null);
    }
}
