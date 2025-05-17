/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.test.map;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import net.minecraftforge.eventbus.internal.Cache;
import net.minecraftforge.eventbus.test.Whitebox;

public class MapTestBase {
    protected static final long TIMEOUT = 1; // number of seconds to wait before retrying. Bump this up to debug what's going on.
    protected static String getWorkerError(ExecutorService pool, String cause) {
        var error = new StringBuilder();
        error.append(cause).append(": \n");
        for (var worker : Whitebox.<Set<?>>getInternalState(pool, "workers")) {
            Thread thread = Whitebox.getInternalState(worker, "thread");
            error.append("\tThread: ").append(thread.getName()).append('\n');
            for (StackTraceElement ele : thread.getStackTrace())
                error.append("\t\tat ").append(ele).append("\n");
        }
        System.out.println(error.toString());
        return error.toString();
    }

    protected <K, V> Map<K, V> cache(String type) {
        System.setProperty("eb.cache_type", type);
        Map<K, V> ret = new MapLike<>();
        System.getProperties().remove("eb.cache_type");
        return ret;
    }

    // Hacky because I dont want to deal with implementing the entire Map interface
    protected static class MapLike<K, V> implements Map<K, V> {
        private final Cache<K, V> lock = InternalUtils.cache();
        @SuppressWarnings("unchecked")
        private final Function<K, V> get = Whitebox.getMethod(lock, "get", (Class<K>)Object.class);

        @SuppressWarnings("unchecked")
        @Override
        public V get(Object key) {
            return get.apply((K)key);
        }
        @Override
        public V put(K key, V value) {
            return lock.computeIfAbsent(key, () -> value);
        }
        @Override
        public V putIfAbsent(K key, V value) {
            return lock.computeIfAbsent(key, () -> value);
        }
        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            return lock.computeIfAbsent(key, () ->  mappingFunction.apply(key));
        }
        @Override public int size() { throw new UnsupportedOperationException(); }
        @Override public boolean isEmpty() { throw new UnsupportedOperationException(); }
        @Override public boolean containsKey(Object key)  { throw new UnsupportedOperationException(); }
        @Override public boolean containsValue(Object value)  { throw new UnsupportedOperationException(); }
        @Override public V remove(Object key) { throw new UnsupportedOperationException(); }
        @Override public void putAll(Map<? extends K, ? extends V> m) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public Set<K> keySet() { throw new UnsupportedOperationException(); }
        @Override public Collection<V> values()  { throw new UnsupportedOperationException(); }
        @Override public Set<Entry<K, V>> entrySet() { throw new UnsupportedOperationException(); }
    }
}
