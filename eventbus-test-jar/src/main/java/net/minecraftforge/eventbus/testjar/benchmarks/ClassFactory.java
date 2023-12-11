/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.benchmarks;

import java.io.IOException;
import java.lang.reflect.Method;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import net.minecraftforge.unsafe.UnsafeHacks;

public class ClassFactory<T> {
    private final Mapper<T> mapper;
    private final String binaryName;
    private final String name;
    private final byte[] data;
    private final Method define;
    private int count;

    public ClassFactory(String name, Mapper<T> mapper) {
        this.mapper = mapper;
        this.name = name;
        this.binaryName = name.replace('.', '/');
        this.data = readData(this.binaryName);
        this.define = getAccess();
    }

    private static byte[] readData(String name) {
        try {
            var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name + ".class");
            return is.readAllBytes();
        } catch (IOException e) {
            return sneak(e);
        }
    }

    private static Method getAccess() {
        try {
            var define = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            UnsafeHacks.setAccessible(define);
            return define;
        } catch (Exception e) {
            return sneak(e);
        }
    }

    public T create() {
        count++;
        var newName = this.binaryName + "$New" + count;

        var renamer = new Remapper() {
            @Override
            public String map(String internalName) {
                if (internalName.equals(binaryName)) return newName;
                return BenchmarkManager.rename(internalName);
            }
        };

        var reader = new ClassReader(data);
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassRemapper(writer, renamer), 0);
        try {
            var cl = Thread.currentThread().getContextClassLoader();
            var newData = writer.toByteArray();
            var newCls = (Class<?>)define.invoke(cl, this.name + "$New" + count, newData, 0, newData.length);
            return mapper.apply(newCls);
        } catch (Exception e) {
            return sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }

    public static interface Mapper<T> {
        T apply(Class<?> cls) throws Exception;
    }
}
