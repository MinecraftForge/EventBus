/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Function;

import net.minecraftforge.unsafe.UnsafeHacks;

public class Whitebox {

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(final Object obj, final String methodName) {
        try {
            return (T)Arrays.stream(obj.getClass().getMethods()).filter(m->m.getName().equals(methodName)).findFirst().orElseThrow().invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <A, R> Function<A, R> getMethod(Object obj, String name, Class<A> arg) {
        try {
            var mtd = obj.getClass().getDeclaredMethod(name, arg);
            UnsafeHacks.setAccessible(mtd);
            return a -> {
                try {
                    return (R)mtd.invoke(obj, a);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInternalState(final Object object, final String fieldName) {
        try {
            if (object instanceof Class<?> cls) {
                var f = cls.getDeclaredField(fieldName);
                UnsafeHacks.setAccessible(f);
                return (T)f.get(null);
            }
            var f = object.getClass().getDeclaredField(fieldName);
            UnsafeHacks.setAccessible(f);
            return (T)f.get(object);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasField(final Class<?> clazz, final String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
