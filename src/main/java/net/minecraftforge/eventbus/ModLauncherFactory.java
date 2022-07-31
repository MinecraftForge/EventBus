package net.minecraftforge.eventbus;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.objectweb.asm.tree.ClassNode;

public class ModLauncherFactory extends ClassLoaderFactory {
    private static final LockHelper<String, Method> PENDING = new LockHelper<>(new HashMap<>());

    @Override
    protected Class<?> createWrapper(Method callback) throws ClassNotFoundException {
        enqueueWrapper(callback);
        var name = getUniqueName(callback);
        return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
    }

    private void enqueueWrapper(Method callback) {
        String name = getUniqueName(callback);
        PENDING.computeIfAbsent(name, () -> callback);
    }

    public static boolean hasPendingWrapperClass(final String className) {
        return PENDING.containsKey(className);
    }

    public static void processWrapperClass(final String className, final ClassNode node) {
        Method meth = PENDING.get(className);
        transformNode(className, meth, node);
    }
}
