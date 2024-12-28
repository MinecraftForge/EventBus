/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.function.Consumer;

public class BenchmarkNoLoader {
    public static class Posting {
        @State(Scope.Benchmark)
        public static class Mixed {
            public static final Consumer<Blackhole> POST_MIXED;
            public static final Consumer<Blackhole> POST_MIXED_DOZEN;
            public static final Consumer<Blackhole> POST_MIXED_HUNDRED;

            static {
                BenchmarkUtils.setupNormalEnvironment();
                POST_MIXED = BenchmarkUtils.getPostingBenchmark("noLoaderMixed", 1);
                POST_MIXED_DOZEN = BenchmarkUtils.getPostingBenchmark("noLoaderMixed", 12);
                POST_MIXED_HUNDRED = BenchmarkUtils.getPostingBenchmark("noLoaderMixed", 100);
            }

            @Benchmark
            public static void postMixed(Blackhole bh) {
                POST_MIXED.accept(bh);
            }

            @Benchmark
            public static void postMixedDozen(Blackhole bh) {
                POST_MIXED_DOZEN.accept(bh);
            }

            @Benchmark
            public static void postMixedHundred(Blackhole bh) {
                POST_MIXED_HUNDRED.accept(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Dynamic {
            public static final Consumer<Blackhole> POST_DYNAMIC;
            public static final Consumer<Blackhole> POST_DYNAMIC_DOZEN;
            public static final Consumer<Blackhole> POST_DYNAMIC_HUNDRED;

            static {
                BenchmarkUtils.setupNormalEnvironment();
                POST_DYNAMIC = BenchmarkUtils.getPostingBenchmark("noLoaderDynamic", 1);
                POST_DYNAMIC_DOZEN = BenchmarkUtils.getPostingBenchmark("noLoaderDynamic", 12);
                POST_DYNAMIC_HUNDRED = BenchmarkUtils.getPostingBenchmark("noLoaderDynamic", 100);
            }

            @Benchmark
            public static void postDynamic(Blackhole bh) {
                POST_DYNAMIC.accept(bh);
            }

            @Benchmark
            public static void postDynamicDozen(Blackhole bh) {
                POST_DYNAMIC_DOZEN.accept(bh);
            }

            @Benchmark
            public static void postDynamicHundred(Blackhole bh) {
                POST_DYNAMIC_HUNDRED.accept(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Lambda {
            public static final Consumer<Blackhole> POST_LAMBDA;
            public static final Consumer<Blackhole> POST_LAMBDA_DOZEN;
            public static final Consumer<Blackhole> POST_LAMBDA_HUNDRED;

            static {
                BenchmarkUtils.setupNormalEnvironment();
                POST_LAMBDA = BenchmarkUtils.getPostingBenchmark("noLoaderLambda", 1);
                POST_LAMBDA_DOZEN = BenchmarkUtils.getPostingBenchmark("noLoaderLambda", 12);
                POST_LAMBDA_HUNDRED = BenchmarkUtils.getPostingBenchmark("noLoaderLambda", 100);
            }

            @Benchmark
            public static void postLambda(Blackhole bh) {
                POST_LAMBDA.accept(bh);
            }

            @Benchmark
            public static void postLambdaDozen(Blackhole bh) {
                POST_LAMBDA_DOZEN.accept(bh);
            }

            @Benchmark
            public static void postLambdaHundred(Blackhole bh) {
                POST_LAMBDA_HUNDRED.accept(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Static {
            public static final Consumer<Blackhole> POST_STATIC;
            public static final Consumer<Blackhole> POST_STATIC_DOZEN;
            public static final Consumer<Blackhole> POST_STATIC_HUNDRED;

            static {
                BenchmarkUtils.setupNormalEnvironment();
                POST_STATIC = BenchmarkUtils.getPostingBenchmark("noLoaderStatic", 1);
                POST_STATIC_DOZEN = BenchmarkUtils.getPostingBenchmark("noLoaderStatic", 12);
                POST_STATIC_HUNDRED = BenchmarkUtils.getPostingBenchmark("noLoaderStatic", 100);
            }

            @Benchmark
            public static void postStatic(Blackhole bh) {
                POST_STATIC.accept(bh);
            }

            @Benchmark
            public static void postStaticDozen(Blackhole bh) {
                POST_STATIC_DOZEN.accept(bh);
            }

            @Benchmark
            public static void postStaticHundred(Blackhole bh) {
                POST_STATIC_HUNDRED.accept(bh);
            }
        }
    }

    public static class Registering {
        @State(Scope.Benchmark)
        public static class Dynamic {
            public static final Runnable SETUP_REGISTER_DYNAMIC;
            public static final Runnable REGISTER_DYNAMIC;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                Runnable[] results = BenchmarkUtils.getRegistrationBenchmark("noLoaderDynamic");
                SETUP_REGISTER_DYNAMIC = results[0];
                REGISTER_DYNAMIC = results[1];
            }

            @Setup(Level.Iteration)
            public void setupIteration() {
                SETUP_REGISTER_DYNAMIC.run();
            }

            @Benchmark
            public void registerDynamic() {
                REGISTER_DYNAMIC.run();
            }
        }

        @State(Scope.Benchmark)
        public static class Lambda {
            public static final Runnable SETUP_REGISTER_LAMBDA;
            public static final Runnable REGISTER_LAMBDA;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                Runnable[] results = BenchmarkUtils.getRegistrationBenchmark("noLoaderLambda");
                SETUP_REGISTER_LAMBDA = results[0];
                REGISTER_LAMBDA = results[1];
            }

            @Setup(Level.Iteration)
            public void setupIteration() {
                SETUP_REGISTER_LAMBDA.run();
            }

            @Benchmark
            public void registerLambda() {
                REGISTER_LAMBDA.run();
            }
        }

        @State(Scope.Benchmark)
        public static class Static {
            public static final Runnable SETUP_REGISTER_STATIC;
            public static final Runnable REGISTER_STATIC;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                Runnable[] results = BenchmarkUtils.getRegistrationBenchmark("noLoaderStatic");
                SETUP_REGISTER_STATIC = results[0];
                REGISTER_STATIC = results[1];
            }

            @Setup(Level.Iteration)
            public void setupIteration() {
                SETUP_REGISTER_STATIC.run();
            }

            @Benchmark
            public void registerStatic() {
                REGISTER_STATIC.run();
            }
        }
    }
}
