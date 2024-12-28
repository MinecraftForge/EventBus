package net.minecraftforge.eventbus.testjar.benchmarks;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberDynamic;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberLambda;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberMixed;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberStatic;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public final class NoLoaderBenchmarks {
    private NoLoaderBenchmarks() {}

    public static abstract class Post extends PostingBenchmark {
        protected static IEventBus createEventBus(int numberOfListeners, Consumer<IEventBus> registrar) {
            return createEventBus(false, numberOfListeners, registrar);
        }

        public static abstract class Mixed {
            public static final class Single extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(1, SubscriberMixed.Factory.create());

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Dozen extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(12, SubscriberMixed.Factory.create());

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Hundred extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(100, SubscriberMixed.Factory.create());

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }
        }

        public static abstract class Dynamic {
            protected static final Consumer<IEventBus> DYNAMIC_REGISTRAR =
                    bus -> SubscriberDynamic.Factory.REGISTER.create().accept(bus);

            public static final class Single extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(1, DYNAMIC_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Dozen extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(12, DYNAMIC_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Hundred extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(100, DYNAMIC_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }
        }

        public static abstract class Lambda {
            protected static final Consumer<IEventBus> LAMBDA_REGISTRAR =
                    bus -> SubscriberLambda.Factory.REGISTER.create().accept(bus);

            public static final class Single extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(1, LAMBDA_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Dozen extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(12, LAMBDA_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Hundred extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(100, LAMBDA_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }
        }

        public static abstract class Static {
            protected static final Consumer<IEventBus> STATIC_REGISTRAR =
                    bus -> SubscriberStatic.Factory.REGISTER.create().accept(bus);

            public static final class Single extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(1, STATIC_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Dozen extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(12, STATIC_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Hundred extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(100, STATIC_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }
        }
    }

    public static abstract class Register extends RegistrationBenchmark {
        public static final class Dynamic extends Register {
            private static final IEventBus EVENT_BUS = BusBuilder.builder().build();
            private static final Deque<Consumer<IEventBus>> REGISTRARS = new ArrayDeque<>(BATCH_COUNT);

            public static void setupIteration() {
                setupIteration(REGISTRARS, SubscriberDynamic.Factory.REGISTER, EVENT_BUS);
            }

            public static void run() {
                REGISTRARS.pop().accept(EVENT_BUS);
            }
        }

        public static final class Lambda extends Register {
            private static final IEventBus EVENT_BUS = BusBuilder.builder().build();
            private static final Deque<Consumer<IEventBus>> REGISTRARS = new ArrayDeque<>(BATCH_COUNT);

            public static void setupIteration() {
                setupIteration(REGISTRARS, SubscriberLambda.Factory.REGISTER, EVENT_BUS);
            }

            public static void run() {
                REGISTRARS.pop().accept(EVENT_BUS);
            }
        }

        public static final class Static extends Register {
            private static final IEventBus EVENT_BUS = BusBuilder.builder().build();
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
