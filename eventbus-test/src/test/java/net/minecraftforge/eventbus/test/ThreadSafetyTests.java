/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.internal.EventBusImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class ThreadSafetyTests {
    private static final class ThreadedListeners {
        record TestEvent() implements RecordEvent {
            static final EventBus<TestEvent> BUS = EventBus.create(TestEvent.class);
        }

        private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(100);

        private static void testEventListener(TestEvent event) {}

        private static void test() {
            List<Callable<Object>> listeners = Collections.nCopies(
                    50,
                    Executors.callable(new Runnable() {
                        // Note: must be a new anon class instance to avoid deduplication
                        @Override
                        public void run() {
                            TestEvent.BUS.addListener(ThreadedListeners::testEventListener);
                        }
                    })
            );

            Assertions.assertDoesNotThrow(() -> {
                try {
                    EXECUTOR_SERVICE.invokeAll(listeners).forEach(future -> {
                        try {
                            // Wait for every listener to be added
                            future.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread was interrupted", e);
                }
            }, "Adding listeners in parallel should work");

            Assertions.assertTimeoutPreemptively(
                    Duration.ofSeconds(10),
                    () -> {
                        var posters = Collections.nCopies(100, Executors.callable(() -> {
                            for (int i = 0; i < 10; i++) {
                                TestEvent.BUS.post(new TestEvent());
                            }
                        }));

                        Assertions.assertDoesNotThrow(() -> {
                            EXECUTOR_SERVICE.invokeAll(posters).forEach(future -> {
                                try {
                                    // Wait for every poster to finish
                                    future.get();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }, "Posting events in parallel should work");
                    }
            );
        }
    }


    /**
     * Tests that adding listeners and posting events in parallel doesn't throw.
     */
    @Test
    public void testThreadedListeners() {
        ThreadedListeners.test();
    }

    public static final class ParallelEvents {
        record TestEventA() implements RecordEvent {
            static final EventBus<TestEventA> BUS = EventBus.create(TestEventA.class);
        }

        record TestEventB() implements RecordEvent {
            static final EventBus<TestEventB> BUS = EventBus.create(TestEventB.class);
        }

        private static final int LISTENER_COUNT_PER_EVENT = 1_000;
        private static final int RUN_ITERATIONS = 1_000;

        private static final AtomicLong COUNTER_A = new AtomicLong();
        private static final AtomicLong COUNTER_B = new AtomicLong();

        private static void test() {
            // Prepare parallel listener addition
            List<Runnable> adders = new ArrayList<>(Collections.nCopies(
                    LISTENER_COUNT_PER_EVENT,
                    () -> TestEventA.BUS.addListener(new Consumer<>() {
                        // Note: must be a new anon class instance to avoid deduplication
                        @Override
                        public void accept(TestEventA event) {
                            COUNTER_A.incrementAndGet();
                        }
                    })
            ));
            adders.addAll(Collections.nCopies(
                    LISTENER_COUNT_PER_EVENT,
                    () -> TestEventB.BUS.addListener(new Consumer<TestEventB>() {
                        @Override
                        public void accept(TestEventB event) {
                            COUNTER_B.incrementAndGet();
                        }
                    })
            ));
            Collections.shuffle(adders);

            // Execute parallel listener addition
            Assertions.assertDoesNotThrow(
                    () -> adders.parallelStream().forEach(Runnable::run),
                    "Adding listeners in parallel should work"
            );

            // Check that all listeners were added
            var eventBusAInternals = (EventBusImpl<?>) TestEventA.BUS;
            var eventBusBInternals = (EventBusImpl<?>) TestEventB.BUS;
            Assertions.assertEquals(
                    LISTENER_COUNT_PER_EVENT * 2,
                    eventBusAInternals.backingList().size() + eventBusBInternals.backingList().size(),
                    "All listeners should be added"
            );

            // Prepare parallel event posting
            List<Runnable> posters = new ArrayList<>(Collections.nCopies(
                    RUN_ITERATIONS,
                    () -> TestEventA.BUS.post(new TestEventA())
            ));
            posters.addAll(Collections.nCopies(
                    RUN_ITERATIONS,
                    () -> TestEventB.BUS.post(new TestEventB())
            ));
            Collections.shuffle(posters);

            // Execute parallel event posting
            posters.parallelStream().forEach(Runnable::run);

            // Check that all listeners were called the correct number of times
            Assertions.assertEquals(
                    COUNTER_A.get() + COUNTER_B.get(),
                    (LISTENER_COUNT_PER_EVENT * 2) * RUN_ITERATIONS,
                    "All listeners should be called the correct number of times"
            );
        }
    }

    /**
     * Tests that all listeners are properly registered and called when posting events in parallel.
     */
    @Test
    public void testParallelEvents() {
        ParallelEvents.test();
    }
}
