/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.LockHelper;
import net.minecraftforge.eventbus.api.Event.HasResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;

public class EventListenerHelper {
    private static final LockHelper<Class<?>, ListenerList> listeners = new LockHelper<>(new IdentityHashMap<>());
    private static final ListenerList EVENTS_LIST = new ListenerList();
    private static final LockHelper<Class<?>, Boolean> cancelable = new LockHelper<>(new IdentityHashMap<>());
    private static final LockHelper<Class<?>, Boolean> hasResult = new LockHelper<>(new IdentityHashMap<>());
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
        return listeners.computeIfAbsent(eventClass, () -> computeListenerList(eventClass, fromInstanceCall));
    }

    private static ListenerList computeListenerList(Class<?> eventClass, boolean fromInstanceCall) {
        if (eventClass == Event.class)
            return new ListenerList();

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

    @SuppressWarnings("unused") // Used in DeadlockingEventTest
    private static void clearAll() {
        listeners.clearAll();
    }

    static boolean isCancelable(Class<?> eventClass) {
        return hasAnnotation(eventClass, Cancelable.class, cancelable);
    }

    static boolean hasResult(Class<?> eventClass) {
        return hasAnnotation(eventClass, HasResult.class, hasResult);
    }

    private static boolean hasAnnotation(Class<?> eventClass, Class<? extends Annotation> annotation, LockHelper<Class<?>, Boolean> lock) {
        if (eventClass == Event.class)
            return false;

        return lock.computeIfAbsent(eventClass, () -> {
            var parent = eventClass.getSuperclass();
            return eventClass.isAnnotationPresent(annotation) || (parent != null && hasAnnotation(parent, annotation, lock));
        });
    }
}
