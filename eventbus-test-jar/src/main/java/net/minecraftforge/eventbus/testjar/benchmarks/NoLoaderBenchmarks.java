package net.minecraftforge.eventbus.testjar.benchmarks;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberDynamic;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberLambda;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberStatic;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public final class NoLoaderBenchmarks {
    private NoLoaderBenchmarks() {}

    public static abstract class Post {
        protected static IEventBus createEventBus(int numberOfListeners, Consumer<IEventBus> registrar) {
            var eventBus = BusBuilder.builder().build();
            for (int i = 0; i < numberOfListeners; i++) {
                registrar.accept(eventBus);
            }
            return eventBus;
        }

        protected static void post(IEventBus eventBus, Blackhole bh) { // todo: use the Blackhole
            eventBus.post(new CancelableEvent());
            eventBus.post(new ResultEvent());
            eventBus.post(new EventWithData("Foo", 5, true));
        }

        public static abstract class Mixed {
            protected static final Consumer<IEventBus> MIXED_REGISTRAR = new Consumer<>() {
                private int idx = 0;

                @Override
                public void accept(IEventBus eventBus) {
                    var registrar = switch (idx++ % 3) {
                        case 0 -> SubscriberDynamic.Factory.REGISTER.create();
                        case 1 -> SubscriberStatic.Factory.REGISTER.create();
                        case 2 -> SubscriberLambda.Factory.REGISTER.create();
                        default -> throw new AssertionError();
                    };
                    registrar.accept(eventBus);
                }
            };

            public static final class Single extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(1, MIXED_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Dozen extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(12, MIXED_REGISTRAR);

                public static void post(Blackhole bh) {
                    post(EVENT_BUS, bh);
                }
            }

            public static final class Hundred extends Post {
                private static final IEventBus EVENT_BUS = createEventBus(100, MIXED_REGISTRAR);

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

    public static abstract class Register {
        // This is the number of dynamic classes to make in the setup method.
        // This number just needs to be high enough that we don't get a EmptyStackException
        // It takes a lot of memory, but it prevents optimizations in the EventBus from
        // invalidating out test results. Such optimizations like the duplicate prevention
        // on normal register(Object) calls but not on lambda calls
        private static final int BATCH_COUNT = 100_000;

        protected static void setupIteration(Deque<Consumer<IEventBus>> registrars,
                                             ClassFactory<Consumer<IEventBus>> registrarFactory,
                                             IEventBus eventBus) {
            // Clear the Deque and EventBus
            registrars.clear();

            try {
                var mtd = eventBus.getClass().getDeclaredMethod("clearInternalData");
                mtd.setAccessible(true);
                mtd.invoke(eventBus);
            } catch (Exception e) {
                sneak(e);
            }

            // Fill the Deque
            while (registrars.size() < BATCH_COUNT)
                registrars.push(registrarFactory.create());

            System.gc();
        }

        @SuppressWarnings("unchecked")
        private static <E extends Throwable> void sneak(Throwable e) throws E {
            throw (E) e;
        }

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
