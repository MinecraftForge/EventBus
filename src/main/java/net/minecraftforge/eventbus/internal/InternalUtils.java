/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.eventbus.api.IGenericEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;

public final class InternalUtils {
    private InternalUtils() {}

    private static final MethodHandle GET_PERMITTED_SUBCLASSES;
    static {
        var lookup = MethodHandles.lookup();
        var methodType = MethodType.methodType(Class[].class);
        if (Runtime.version().feature() >= 17) {
            try {
                GET_PERMITTED_SUBCLASSES = lookup
                        .findVirtual(Class.class, "getPermittedSubclasses", methodType)
                        .asType(methodType.insertParameterTypes(0, Class.class));
            } catch (Exception e) {
                throw new ExceptionInInitializerError("Failed to find Class#getPermittedSubclasses(Class): " + e);
            }
        } else {
            GET_PERMITTED_SUBCLASSES = MethodHandles.empty(methodType.insertParameterTypes(0, Class.class));
        }
    }

    public static Class<?>[] getPermittedSubclasses(Class<?> clazz) {
        try {
            return (Class<?>[]) GET_PERMITTED_SUBCLASSES.invokeExact(clazz);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param eventType the event type to check
     * @return true if the given event type could be cancelled by a listener for a subclass of the given event type
     */
    public static boolean couldBeCancelled(Class<?> eventType) {
        return couldBeCancelled(eventType, IGenericEvent.class.isAssignableFrom(eventType));
    }

    /**
     * @param eventType the event type to check
     * @param isGenericEvent true if the given event type is a generic event type
     * @return true if the given event type could be cancelled by a listener for a subclass of the given event type
     */
    public static boolean couldBeCancelled(Class<?> eventType, boolean isGenericEvent) {
        if (isGenericEvent || EventListenerHelper.isCancelable(eventType)) return true;
        if (Modifier.isFinal(eventType.getModifiers())) return false;

        var permittedSubclasses = getPermittedSubclasses(eventType);
        if (permittedSubclasses == null || permittedSubclasses.length == 0) return true;

        for (var subclass : permittedSubclasses)
            if (couldBeCancelled(subclass))
                return true;

        return false;
    }
}
