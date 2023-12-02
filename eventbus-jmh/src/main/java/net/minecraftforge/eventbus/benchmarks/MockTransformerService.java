/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import joptsimple.OptionSpecBuilder;
import org.jetbrains.annotations.NotNull;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Test Launcher Service
 */
public class MockTransformerService implements ITransformationService {
    @NotNull
    @Override public String name() { return "test"; }
    @Override public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) { }
    @Override public void argumentValues(OptionResult result) { }
    @Override public void initialize(IEnvironment environment) { }
    @Override public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException { }
    @SuppressWarnings("rawtypes")
    @NotNull
    @Override public List<ITransformer> transformers() { return List.of(); }

    @Override
    public List<Resource> beginScanning(IEnvironment environment) {
        var ret = new ArrayList<>(scan(IModuleLayerManager.Layer.PLUGIN));
        ret.addAll(scan(IModuleLayerManager.Layer.SERVICE));
        return ret;
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        return scan(IModuleLayerManager.Layer.GAME);
    }

    private List<Resource> scan(IModuleLayerManager.Layer layer) {
        var prop = System.getProperty("test.harness." + layer.name().toLowerCase(Locale.ROOT));
        if (prop == null)
            return List.of();
        var jars = new ArrayList<SecureJar>();
        for (var path : prop.split(","))
            jars.add(SecureJar.from(FileSystems.getDefault().getPath(path)));
        return List.of(new Resource(layer, jars));
    }
}
