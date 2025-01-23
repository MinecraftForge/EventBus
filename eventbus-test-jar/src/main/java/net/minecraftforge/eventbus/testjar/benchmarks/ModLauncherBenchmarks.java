/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.benchmarks;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberDynamic;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberLambda;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberMixed;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberStatic;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class ModLauncherBenchmarks {
    private ModLauncherBenchmarks() {}

    public static final class Post {
        private Post() {}
        private static final IEventBus EVENT_BUS = BusBuilder.builder().useModLauncher().build();

        public static void setup(int multiplier, Supplier<Consumer<IEventBus>> registrar) {
            for (int i = 0; i < multiplier; i++)
                registrar.get().accept(EVENT_BUS);
        }

        public static void post(Blackhole bh) {
            EVENT_BUS.post(new CancelableEvent());
            EVENT_BUS.post(new ResultEvent());
            EVENT_BUS.post(new EventWithData("Foo", 5, true));
        }

        public static final class Factory {
            private Factory() {}
            public static final ClassFactory<IntFunction<Consumer<Blackhole>>> MIXED = new ClassFactory<>(
                    Post.class,
                    MethodHandles.lookup(),
                    (lookup, cls) -> multiplier ->
                            BenchmarkManager.setupPostingBenchmark(lookup, cls, multiplier, SubscriberMixed.Factory.REGISTER)
            );

            public static final ClassFactory<IntFunction<Consumer<Blackhole>>> DYNAMIC = new ClassFactory<>(
                    Post.class,
                    MethodHandles.lookup(),
                    (lookup, cls) -> multiplier ->
                            BenchmarkManager.setupPostingBenchmark(lookup, cls, multiplier, SubscriberDynamic.Factory.REGISTER)
            );

            public static final ClassFactory<IntFunction<Consumer<Blackhole>>> LAMBDA = new ClassFactory<>(
                    Post.class,
                    MethodHandles.lookup(),
                    (lookup, cls) -> multiplier ->
                            BenchmarkManager.setupPostingBenchmark(lookup, cls, multiplier, SubscriberLambda.Factory.REGISTER)
            );

            public static final ClassFactory<IntFunction<Consumer<Blackhole>>> STATIC = new ClassFactory<>(
                    Post.class,
                    MethodHandles.lookup(),
                    (lookup, cls) -> multiplier ->
                            BenchmarkManager.setupPostingBenchmark(lookup, cls, multiplier, SubscriberStatic.Factory.REGISTER)
            );
        }
    }

    public static final class Register {
        private Register() {}

        public static final class Dynamic extends RegistrationBenchmark {
            private static final IEventBus EVENT_BUS = BusBuilder.builder().useModLauncher().build();
            private static final Deque<Consumer<IEventBus>> REGISTRARS = new ArrayDeque<>(BATCH_COUNT);

            public static void setupIteration() {
                setupIteration(REGISTRARS, SubscriberDynamic.Factory.REGISTER, EVENT_BUS);
            }

            public static void run() {
                REGISTRARS.pop().accept(EVENT_BUS);
            }
        }

        public static final class Lambda extends RegistrationBenchmark {
            private static final IEventBus EVENT_BUS = BusBuilder.builder().useModLauncher().build();
            private static final Deque<Consumer<IEventBus>> REGISTRARS = new ArrayDeque<>(BATCH_COUNT);

            public static void setupIteration() {
                setupIteration(REGISTRARS, SubscriberLambda.Factory.REGISTER, EVENT_BUS);
            }

            public static void run() {
                REGISTRARS.pop().accept(EVENT_BUS);
            }
        }

        public static final class Static extends RegistrationBenchmark {
            private static final IEventBus EVENT_BUS = BusBuilder.builder().useModLauncher().build();
            private static final Deque<Consumer<IEventBus>> REGISTRARS = new ArrayDeque<>(BATCH_COUNT);

            public static void setupIteration() {
                setupIteration(REGISTRARS, SubscriberStatic.Factory.REGISTER, EVENT_BUS);
            }

            public static void run() {
                REGISTRARS.pop().accept(EVENT_BUS);
            }
        }
    }
}
