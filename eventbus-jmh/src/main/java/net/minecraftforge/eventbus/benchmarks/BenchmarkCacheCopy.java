/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

public class BenchmarkCacheCopy {
    public static void setup() {
        System.setProperty("eb.cache_type", "copy");
    }

    public static void teardown() {
        System.clearProperty("eb.cache_type");
    }

    public static class Posting {
        @State(Scope.Benchmark)
        public static class Mixed {
            @Setup
            public static void setup() {
                BenchmarkCacheCopy.setup();
                var preload = BenchmarkNoLoader.Posting.Mixed.POST_MIXED;
                preload = BenchmarkNoLoader.Posting.Mixed.POST_MIXED_DOZEN;
                preload = BenchmarkNoLoader.Posting.Mixed.POST_MIXED_HUNDRED;
            }

            @TearDown
            public static void teardown() {
                BenchmarkCacheCopy.teardown();
            }

            @Benchmark
            public static void postMixed(Blackhole bh) {
                BenchmarkNoLoader.Posting.Mixed.POST_MIXED.accept(bh);
            }

            @Benchmark
            public static void postMixedDozen(Blackhole bh) {
                BenchmarkNoLoader.Posting.Mixed.POST_MIXED_DOZEN.accept(bh);
            }

            @Benchmark
            public static void postMixedHundred(Blackhole bh) {
                BenchmarkNoLoader.Posting.Mixed.POST_MIXED_HUNDRED.accept(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Dynamic {
            @Setup
            public static void setup() {
                BenchmarkCacheCopy.setup();
                var preload = BenchmarkNoLoader.Posting.Dynamic.POST_DYNAMIC;
                preload = BenchmarkNoLoader.Posting.Dynamic.POST_DYNAMIC_DOZEN;
                preload = BenchmarkNoLoader.Posting.Dynamic.POST_DYNAMIC_HUNDRED;
            }

            @TearDown
            public static void teardown() {
                BenchmarkCacheCopy.teardown();
            }

            @Benchmark
            public static void postDynamic(Blackhole bh) {
                BenchmarkNoLoader.Posting.Dynamic.POST_DYNAMIC.accept(bh);
            }

            @Benchmark
            public static void postDynamicDozen(Blackhole bh) {
                BenchmarkNoLoader.Posting.Dynamic.POST_DYNAMIC_DOZEN.accept(bh);
            }

            @Benchmark
            public static void postDynamicHundred(Blackhole bh) {
                BenchmarkNoLoader.Posting.Dynamic.POST_DYNAMIC_HUNDRED.accept(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Lambda {
            @Setup
            public static void setup() {
                BenchmarkCacheCopy.setup();
                var preload = BenchmarkNoLoader.Posting.Lambda.POST_LAMBDA;
                preload = BenchmarkNoLoader.Posting.Lambda.POST_LAMBDA_DOZEN;
                preload = BenchmarkNoLoader.Posting.Lambda.POST_LAMBDA_HUNDRED;
            }

            @TearDown
            public static void teardown() {
                BenchmarkCacheCopy.teardown();
            }

            @Benchmark
            public static void postLambda(Blackhole bh) {
                BenchmarkNoLoader.Posting.Lambda.POST_LAMBDA.accept(bh);
            }

            @Benchmark
            public static void postLambdaDozen(Blackhole bh) {
                BenchmarkNoLoader.Posting.Lambda.POST_LAMBDA_DOZEN.accept(bh);
            }

            @Benchmark
            public static void postLambdaHundred(Blackhole bh) {
                BenchmarkNoLoader.Posting.Lambda.POST_LAMBDA_HUNDRED.accept(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Static {
            @Setup
            public static void setup() {
                BenchmarkCacheCopy.setup();
                var preload = BenchmarkNoLoader.Posting.Static.POST_STATIC;
                preload = BenchmarkNoLoader.Posting.Static.POST_STATIC_DOZEN;
                preload = BenchmarkNoLoader.Posting.Static.POST_STATIC_HUNDRED;
            }

            @TearDown
            public static void teardown() {
                BenchmarkCacheCopy.teardown();
            }

            @Benchmark
            public static void postStatic(Blackhole bh) {
                BenchmarkNoLoader.Posting.Static.POST_STATIC.accept(bh);
            }

            @Benchmark
            public static void postStaticDozen(Blackhole bh) {
                BenchmarkNoLoader.Posting.Static.POST_STATIC_DOZEN.accept(bh);
            }

            @Benchmark
            public static void postStaticHundred(Blackhole bh) {
                BenchmarkNoLoader.Posting.Static.POST_STATIC_HUNDRED.accept(bh);
            }
        }
    }

    public static class Registering {
        @State(Scope.Benchmark)
        public static class Dynamic {
            @Setup
            public static void setup() {
                BenchmarkCacheConcurrent.setup();
                var preload = BenchmarkNoLoader.Registering.Dynamic.REGISTER_DYNAMIC;
            }

            @TearDown
            public static void teardown() {
                BenchmarkCacheConcurrent.teardown();
            }

            @Benchmark
            public static void registerDynamic() {
                BenchmarkNoLoader.Registering.Dynamic.REGISTER_DYNAMIC.run();
            }
        }

        @State(Scope.Benchmark)
        public static class Lambda {
            @Setup
            public static void setup() {
                BenchmarkCacheConcurrent.setup();
                var preload = BenchmarkNoLoader.Registering.Lambda.REGISTER_LAMBDA;
            }

            @TearDown
            public static void teardown() {
                BenchmarkCacheConcurrent.teardown();
            }

            @Benchmark
            public static void registerLambda() {
                BenchmarkNoLoader.Registering.Lambda.REGISTER_LAMBDA.run();
            }
        }

        @State(Scope.Benchmark)
        public static class Static {
            @Setup
            public static void setup() {
                BenchmarkCacheConcurrent.setup();
                var preload = BenchmarkNoLoader.Registering.Static.REGISTER_STATIC;
            }

            @TearDown
            public static void teardown() {
                BenchmarkCacheConcurrent.teardown();
            }

            @Benchmark
            public static void registerStatic() {
                BenchmarkNoLoader.Registering.Static.REGISTER_STATIC.run();
            }
        }
    }
}
