/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.testjar.benchmarks;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public final class ClassFactory<T> implements Supplier<T> {
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
                return internalName;
            }
        };

        var remapStaticAccess = new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        if ((opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) && owner.equals(binaryName))
                            owner = binaryName + "$New" + count;

                        super.visitFieldInsn(opcode, owner, name, descriptor);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && owner.equals(binaryName))
                            owner = binaryName + "$New" + count;

                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                };
            }
        };

        var reader = new ClassReader(data);
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        reader.accept(remapStaticAccess, 0);
        reader.accept(new ClassRemapper(writer, renamer), 0);
        try {
            var newData = writer.toByteArray();
            var newCls = (Class<?>) lookup.defineClass(newData);
            return mapper.apply(lookup, newCls);
        } catch (Exception e) {
            return sneak(e);
        }
    }

    @Override
    public T get() {
        return create();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E) e;
    }

    @FunctionalInterface
    public interface Mapper<T> {
        T apply(MethodHandles.Lookup lookup, Class<?> cls) throws Exception;
    }
}
