/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.benchmarks;

import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.IEventBus;

public interface Benchmark {
    default void setup(Supplier<IEventBus> busFactory) {}
    default void setupIteration() {}
    void run();
    
    @SuppressWarnings("unchecked")
    default <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }
}
