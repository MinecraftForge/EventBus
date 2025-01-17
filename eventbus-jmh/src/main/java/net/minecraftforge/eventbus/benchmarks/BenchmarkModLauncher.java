/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.function.Consumer;

public class BenchmarkModLauncher {
    public static class Posting {
        @State(Scope.Benchmark)
        public static class Mixed {
            private static final Consumer<Blackhole> POST_MIXED;
            private static final Consumer<Blackhole> POST_MIXED_DOZEN;
            private static final Consumer<Blackhole> POST_MIXED_HUNDRED;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                POST_MIXED = BenchmarkUtils.getPostingBenchmark("modLauncherMixed", 1);
                POST_MIXED_DOZEN = BenchmarkUtils.getPostingBenchmark("modLauncherMixed", 12);
                POST_MIXED_HUNDRED = BenchmarkUtils.getPostingBenchmark("modLauncherMixed", 100);
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
            private static final Consumer<Blackhole> POST_DYNAMIC;
            private static final Consumer<Blackhole> POST_DYNAMIC_DOZEN;
            private static final Consumer<Blackhole> POST_DYNAMIC_HUNDRED;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                POST_DYNAMIC = BenchmarkUtils.getPostingBenchmark("modLauncherDynamic", 1);
                POST_DYNAMIC_DOZEN = BenchmarkUtils.getPostingBenchmark("modLauncherDynamic", 12);
                POST_DYNAMIC_HUNDRED = BenchmarkUtils.getPostingBenchmark("modLauncherDynamic", 100);
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
            private static final Consumer<Blackhole> POST_LAMBDA;
            private static final Consumer<Blackhole> POST_LAMBDA_DOZEN;
            private static final Consumer<Blackhole> POST_LAMBDA_HUNDRED;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                POST_LAMBDA = BenchmarkUtils.getPostingBenchmark("modLauncherLambda", 1);
                POST_LAMBDA_DOZEN = BenchmarkUtils.getPostingBenchmark("modLauncherLambda", 12);
                POST_LAMBDA_HUNDRED = BenchmarkUtils.getPostingBenchmark("modLauncherLambda", 100);
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
            private static final Consumer<Blackhole> POST_STATIC;
            private static final Consumer<Blackhole> POST_STATIC_DOZEN;
            private static final Consumer<Blackhole> POST_STATIC_HUNDRED;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                POST_STATIC = BenchmarkUtils.getPostingBenchmark("modLauncherStatic", 1);
                POST_STATIC_DOZEN = BenchmarkUtils.getPostingBenchmark("modLauncherStatic", 12);
                POST_STATIC_HUNDRED = BenchmarkUtils.getPostingBenchmark("modLauncherStatic", 100);
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
            private static final Runnable SETUP_REGISTER_DYNAMIC;
            private static final Runnable REGISTER_DYNAMIC;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                Runnable[] results = BenchmarkUtils.getRegistrationBenchmark("modLauncherDynamic");
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
            private static final Runnable SETUP_REGISTER_LAMBDA;
            private static final Runnable REGISTER_LAMBDA;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                Runnable[] results = BenchmarkUtils.getRegistrationBenchmark("modLauncherLambda");
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
            private static final Runnable SETUP_REGISTER_STATIC;
            private static final Runnable REGISTER_STATIC;

            static {
                BenchmarkUtils.setupTransformedEnvironment();
                Runnable[] results = BenchmarkUtils.getRegistrationBenchmark("modLauncherStatic");
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
