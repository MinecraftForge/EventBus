/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.Event.HasResult;
import net.minecraftforge.eventbus.internal.Cache;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class EventListenerHelper {
    private static final Cache<Class<?>, ListenerList> listeners = Cache.create();
    private static final ListenerList EVENTS_LIST = new ListenerList();
    private static final Cache<Class<?>, Boolean> cancelable = Cache.create();
    private static final Cache<Class<?>, Boolean> hasResult = Cache.create();

    /**
     * Returns a {@link ListenerList} object that contains all listeners
     * that are registered to this event class.
     *
     * This supports abstract classes that cannot be instantiated.
     *
     * Note: this is much slower than the instance method {@link Event#getListenerList()}.
     * For performance when emitting events, always call that method instead.
     */
    public static ListenerList getListenerList(Class<?> eventClass) {
        return getListenerListInternal(eventClass, false);
    }

    static ListenerList getListenerListInternal(Class<?> eventClass, boolean fromInstanceCall) {
        if (eventClass == Event.class) return EVENTS_LIST; // Small optimization, bypasses all the locks/maps.

        // Attempt to get the ListenerList directly from the cache first. This avoids an allocating lambda on cache hit
        var ret = listeners.get(eventClass);
        if (ret != null) return ret;

        // Cache miss, check again (for thread-safety) and compute the ListenerList if still absent
        return listeners.computeIfAbsent(eventClass, k -> computeListenerList(k, fromInstanceCall));
    }

    private static ListenerList computeListenerList(Class<?> eventClass, boolean fromInstanceCall) {
        if (eventClass == Event.class)
            return EVENTS_LIST;

        if (fromInstanceCall || Modifier.isAbstract(eventClass.getModifiers())) {
            Class<?> superclass = eventClass.getSuperclass();
            ListenerList parentList = getListenerList(superclass);
            return new ListenerList(parentList);
        }

        try {
            Constructor<?> ctr = eventClass.getConstructor();
            ctr.setAccessible(true);
            Event event = (Event) ctr.newInstance();
            return event.getListenerList();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error computing listener list for " + eventClass.getName(), e);
        }
    }

    @ApiStatus.Internal
    public static boolean isCancelable(Class<?> eventClass) {
        return hasAnnotation(eventClass, Cancelable.class, cancelable);
    }

    static boolean hasResult(Class<?> eventClass) {
        return hasAnnotation(eventClass, HasResult.class, hasResult);
    }

    private static boolean hasAnnotation(Class<?> eventClass, Class<? extends Annotation> annotation, Cache<Class<?>, Boolean> cache) {
        if (eventClass == Event.class || eventClass == Object.class)
            return false;

        var ret = cache.get(eventClass);
        if (ret != null) return ret;

        return cache.computeIfAbsent(eventClass, k -> {
            if (k.isAnnotationPresent(annotation))
                return true;
            var parent = k.getSuperclass();
            return parent != null && hasAnnotation(parent, annotation, cache);
        });
    }
}
