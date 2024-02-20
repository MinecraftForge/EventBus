/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.BenchmarkParams;

@State(Scope.Benchmark)
public class BenchmarkCacheConcurrent extends BenchmarkBase {
    @Setup
    public void setup(BenchmarkParams params) {
        System.setProperty("eb.cache_type", "concurrent");
        setupNormal(params);
    }
    @TearDown
    public void teardown() {
        System.getProperties().remove("eb.cache_type");
    }
    @Setup(Level.Iteration) public void setupIteration() { super.setupIteration(); }

    @Benchmark public void postDynamicRecord() { run(); }
    @Benchmark public void postDynamicDozenRecord() { run(); }
    @Benchmark public void postDynamicHundredRecord() { run(); }
    @Benchmark public void postLambdaRecord() { run(); }
    @Benchmark public void postLambdaDozenRecord() { run(); }
    @Benchmark public void postLambdaHundredRecord() { run(); }
    @Benchmark public void postStaticRecord() { run(); }
    @Benchmark public void postStaticDozenRecord() { run(); }
    @Benchmark public void postStaticHundredRecord() { run(); }
    @Benchmark public void postMixedRecord() { run(); }
    @Benchmark public void postMixedDozenRecord() { run(); }
    @Benchmark public void postMixedHundredRecord() { run(); }

    @Benchmark public void postDynamic() { run(); }
    @Benchmark public void postDynamicDozen() { run(); }
    @Benchmark public void postDynamicHundred() { run(); }
    @Benchmark public void postLambda() { run(); }
    @Benchmark public void postLambdaDozen() { run(); }
    @Benchmark public void postLambdaHundred() { run(); }
    @Benchmark public void postStatic() { run(); }
    @Benchmark public void postStaticDozen() { run(); }
    @Benchmark public void postStaticHundred() { run(); }
    @Benchmark public void postMixed() { run(); }
    @Benchmark public void postMixedDozen() { run(); }
    @Benchmark public void postMixedHundred() { run(); }
    @Benchmark public void registerDynamic() { run(); }
    @Benchmark public void registerLambda() { run(); }
    @Benchmark public void registerStatic() { run(); }
}
