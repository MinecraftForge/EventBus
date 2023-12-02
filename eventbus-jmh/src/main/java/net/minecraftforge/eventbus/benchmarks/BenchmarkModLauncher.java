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
import org.openjdk.jmh.infra.BenchmarkParams;

@State(Scope.Benchmark)
public class BenchmarkModLauncher extends BenchmarkBase {
    @Setup public void setup(BenchmarkParams params) { setupTransformed(params, true); }
    @Setup(Level.Iteration) public void setupIteration() { super.setupIteration(); }

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
