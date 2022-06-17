package net.minecraftforge.eventbus.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class Whitebox {

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(final Class<?> clazz, final String methodName) {
        try {
            return (T)Arrays.stream(clazz.getMethods()).filter(m->m.getName().equals(methodName)).findFirst().orElseThrow().invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(final Object obj, final String methodName) {
        try {
            return (T)Arrays.stream(obj.getClass().getMethods()).filter(m->m.getName().equals(methodName)).findFirst().orElseThrow().invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInternalState(final Object object, final String fieldName) {
        try {
            var f = object.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T)f.get(object);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getField(final Class<?> clazz, final String fieldName) {
        try {
            var f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
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
