/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Test Launcher Service
 */
public final class MockTransformerService implements ITransformationService {
    @Override
    public @NotNull String name() {
        return "test";
    }

    @Override
    public void initialize(IEnvironment environment) {}

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {}

    @SuppressWarnings("rawtypes")
    @Override
    public @NotNull List<ITransformer> transformers() {
        return List.of();
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        var prop = System.getProperty("test.harness.game");
        if (prop == null)
            return List.of();

        var jars = new ArrayList<SecureJar>();
        for (var path : prop.split(","))
            jars.add(SecureJar.from(Path.of(path)));

        return List.of(new Resource(IModuleLayerManager.Layer.GAME, jars));
    }
}
