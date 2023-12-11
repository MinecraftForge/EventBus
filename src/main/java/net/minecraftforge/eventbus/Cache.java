/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

interface Cache<K,V> extends BiFunction<K, Supplier<V>, V> {
    V get(K key);

    // Only public method to expose
    @Override
    default V apply(K key, Supplier<V> factory) {
        return computeIfAbsent(key, factory);
    }

    default V computeIfAbsent(K key, Supplier<V> factory) {
        return computeIfAbsent(key, factory, Function.identity());
    }

    <I> V computeIfAbsent(K key, Supplier<I> factory, Function<I, V> finalizer);
}
