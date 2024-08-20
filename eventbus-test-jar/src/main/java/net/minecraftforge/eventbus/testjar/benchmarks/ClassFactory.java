/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.benchmarks;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public final class ClassFactory<T> {
    private final Mapper<T> mapper;
    private final String binaryName;
    private final byte[] data;
    private final MethodHandles.Lookup lookup;
    private int count;

    /**
     * @param clazz The class to create new derivations of
     * @param lookup {@code MethodHandles.lookup()} for defining the new classes
     * @param mapper A function to apply on newly defined classes
     */
    public ClassFactory(Class<?> clazz, MethodHandles.Lookup lookup, Mapper<T> mapper) {
        this.mapper = mapper;
        this.binaryName = clazz.getName().replace('.', '/');
        this.data = readData(this.binaryName);
        this.lookup = lookup;
    }

    private static byte[] readData(String name) {
        try {
            var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name + ".class");
            return is.readAllBytes();
        } catch (IOException e) {
            return sneak(e);
        }
    }

    public T create() {
        count++;

        var renamer = new Remapper() {
            @Override
            public String map(String internalName) {
                if (internalName.equals(binaryName)) return binaryName + "$New" + count;
                return BenchmarkManager.rename(internalName);
            }
        };

        var reader = new ClassReader(data);
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassRemapper(writer, renamer), 0);
        try {
            var newData = writer.toByteArray();
            var newCls = (Class<?>) lookup.defineClass(newData);
            return mapper.apply(newCls);
        } catch (Exception e) {
            return sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E) e;
    }

    @FunctionalInterface
    public interface Mapper<T> {
        T apply(Class<?> cls) throws Exception;
    }
}
