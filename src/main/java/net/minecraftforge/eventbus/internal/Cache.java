/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

@ApiStatus.Internal
public interface Cache<K, V> {
    V get(K key);

    default V computeIfAbsent(K key, Supplier<V> factory) {
        return computeIfAbsent(key, factory, Function.identity());
    }

    <I> V computeIfAbsent(K key, Supplier<I> factory, Function<I, V> finalizer);

    static <K, V> Cache<K, V> create() {
        var type = System.getProperty("eb.cache_type");
        if (type == null || "concurrent".equals(type))
            return new CacheConcurrent<>();
        if ("copy".equals(type))
            return new CacheCopyOnWrite<>(HashMap::new);
        throw new IllegalArgumentException("Unknown `eb.cache_type` " + type);
    }
}
