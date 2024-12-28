/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.benchmarks;

import net.minecraftforge.eventbus.api.IEventBus;

import java.util.Deque;
import java.util.function.Consumer;

public abstract class RegistrationBenchmark {
    // This is the number of dynamic classes to make in the setup method.
    // This number just needs to be high enough that we don't get a EmptyStackException
    // It takes a lot of memory, but it prevents optimizations in the EventBus from
    // invalidating out test results. Such optimizations like the duplicate prevention
    // on normal register(Object) calls but not on lambda calls
    protected static final int BATCH_COUNT = 100_000;

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
}
