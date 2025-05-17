/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/*
 * An implementation of the Cache class that uses any type of backing map the user wants.
 * But protects against concurrency issues by copying the entire map every time it is written to.
 *
 * This has massive memory issues as it obvious duplicates the map.
 * It also has performance issues for large maps for obvious reasons.
 * But the benefit is no locks during read
 */
class CacheCopyOnWrite<K,V> implements Cache<K, V> {
    private Object lock = new Object();
    private IntFunction<Map<K, V>> factory;
    private final Map<K, V> map;
    private volatile Map<K, V> readable;

    CacheCopyOnWrite(IntFunction<Map<K, V>> factory) {
        this.factory = factory;
        this.map = factory.apply(32);
        this.readable = this.map;
    }

    @Override
    public V get(K key) {
        return readable.get(key);
    }

    @Override
    public <I> V computeIfAbsent(K key, Supplier<I> factory, Function<I, V> finalizer) {
        // This is a put once map, so lets try checking if the map has this value.
        // Should be thread safe to read without lock as any writes will be guarded
        var ret = get(key);

        // If the map had a value, return it.
        if (ret != null)
            return ret;

        // Let's pre-compute our new value. This could take a while, as well as recursively call this
        // function. as such, we need to make sure we don't hold a lock when we do this
        var intermediate = factory.get();

        // We're actually gunna mutate the object now so lets lock it
        synchronized (lock) {
            // Check if some other thread already created a value
            ret = map.get(key);
            if (ret == null) {
                // Run any finalization we need, this was added because ClassLoaderFactory will actually define the class here
                ret = finalizer.apply(intermediate);
                // Update the map
                map.put(key, ret);

                // Now create a copy of the map
                var newMap = this.factory.apply(map.size());
                newMap.putAll(map);

                // And set it to the readable view so get can call it
                readable = newMap;
            }
            return ret;
        }
    }
}
