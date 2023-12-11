/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;

/* Internal Utility class, only public so that other classes in the EventBus project can access things.
 * MODDERS SHOULD NEVER TOUCH THIS. And I reserve the right to do any breaking changes I want to this class.
 */
@ApiStatus.Internal
public class InternalUtils {
    public static <K,V> BiFunction<K, Supplier<V>, V> cachePublic() {
        return cache();
    }

    static <K, V> Cache<K, V> cache() {
        var type = System.getProperty("eb.cache_type");
        if (type == null || "concurrent".equals(type))
            return new CacheConcurrent<>();
        if ("copy".equals(type))
            return new CacheCopyOnWrite<>(HashMap::new);
        throw new IllegalArgumentException("Unknown `eb.cache_type` " + type);
    }
}
