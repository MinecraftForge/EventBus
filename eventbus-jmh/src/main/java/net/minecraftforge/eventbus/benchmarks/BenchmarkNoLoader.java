/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberDynamic;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberLambda;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberMixed;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberStatic;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;

import java.util.function.Supplier;

public class BenchmarkNoLoader {
    protected static void validateEnvironment(BenchmarkParams params) {
        System.clearProperty("eventbus.internal.dedupeListeners");

        // EventBus' optimisations rely heavily on static final state, which can only be reset by restarting the JVM.
        // If no forks are used, the number of listeners will be higher than expected.
        if (params.getForks() == 0)
            throw new IllegalStateException("The number of forks must be greater than 0");

        if (!params.getBenchmark().contains("Registering"))
            return;

        // To get accurate registration results without code generation, we need to disable deduplication of listeners.
        System.setProperty("eventbus.internal.dedupeListeners", "false");
    }

    protected static void register(Supplier<Runnable> registrar, int multiplier) {
        for (int i = 0; i < multiplier; i++) {
            registrar.get().run();
        }
    }

    public static abstract class Posting {
        protected static void post(Blackhole bh) {
            new CancelableEvent().post();
            new ResultEvent().post();
            new EventWithData("Foo", 5, true).post();
        }

        @State(Scope.Benchmark)
        public static class Mixed {
//            @Param({"12", "100"}) // 1 is not included because that wouldn't be a mix of subscriber types
            @Param({"1", "2", "3", "4", "5", "10", "20", "30", "40", "50"})
            private int multiplier;

            @Setup(Level.Trial)
            public void setup(BenchmarkParams params) {
                BenchmarkNoLoader.validateEnvironment(params);
                BenchmarkNoLoader.register(SubscriberMixed.Factory.REGISTER, multiplier);
            }

            @Benchmark
            public void postMixed(Blackhole bh) {
                Posting.post(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Dynamic {
//            @Param({"1", "12", "100"})
            @Param({"1", "2", "3", "4", "5", "10", "20", "30", "40", "50"})
            private int multiplier;

            @Setup(Level.Trial)
            public void setup(BenchmarkParams params) {
                BenchmarkNoLoader.validateEnvironment(params);
                BenchmarkNoLoader.register(SubscriberDynamic.Factory.REGISTER, multiplier);
            }

            @Benchmark
            public static void postDynamic(Blackhole bh) {
                Posting.post(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Lambda {
//            @Param({"1", "12", "100"})
            @Param({"1", "2", "3", "4", "5", "10", "20", "30", "40", "50"})
            private int multiplier;

            @Setup(Level.Trial)
            public void setup(BenchmarkParams params) {
                BenchmarkNoLoader.validateEnvironment(params);
                BenchmarkNoLoader.register(SubscriberLambda.Factory.REGISTER, multiplier);
            }

            @Benchmark
            public static void postLambda(Blackhole bh) {
                Posting.post(bh);
            }
        }

        @State(Scope.Benchmark)
        public static class Static {
//            @Param({"1", "12", "100"})
            @Param({"1", "2", "3", "4", "5", "10", "20", "30", "40", "50"})
            private int multiplier;

            @Setup(Level.Trial)
            public void setup(BenchmarkParams params) {
                BenchmarkNoLoader.validateEnvironment(params);
                BenchmarkNoLoader.register(SubscriberStatic.Factory.REGISTER, multiplier);
            }

            @Benchmark
            public void postStatic(Blackhole bh) {
                Posting.post(bh);
            }
        }
    }

    public static class Registering {
        @State(Scope.Benchmark)
        public static class Dynamic {
            @Setup(Level.Trial)
            public void setup(BenchmarkParams params) {
                BenchmarkNoLoader.validateEnvironment(params);
            }

            @Benchmark
            public void registerDynamic() {
                BusGroup.DEFAULT.register(SubscriberDynamic.LOOKUP, new SubscriberDynamic());
            }
        }

        // Todo: [EB][Benchmarks] Need to consider ClassFactory for this... it's allowing the JVM to cache the created
        //       lambda CallSite, while the other two benchmarks are creating a new CallSite every time.
//        @State(Scope.Benchmark)
//        public static class Lambda {
//            @Setup(Level.Trial)
//            public void setup(BenchmarkParams params) {
//                BenchmarkNoLoader.validateEnvironment(params);
//            }
//
//            @Benchmark
//            public void registerLambda() {
//                SubscriberLambda.register();
//            }
//        }

        @State(Scope.Benchmark)
        public static class Static {
            @Setup(Level.Trial)
            public void setup(BenchmarkParams params) {
                BenchmarkNoLoader.validateEnvironment(params);
            }

            @Benchmark
            public void registerStatic() {
                BusGroup.DEFAULT.register(SubscriberStatic.LOOKUP, SubscriberStatic.class);
            }
        }
    }
}
