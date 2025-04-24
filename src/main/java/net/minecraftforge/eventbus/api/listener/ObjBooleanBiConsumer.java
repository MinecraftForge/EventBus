/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.listener;

import java.util.function.BiConsumer;

/**
 * A {@linkplain BiConsumer bi-consumer} that accepts an object and a primitive boolean.
 * <p>This is used over {@link BiConsumer}{@code <}{@link Object}<code>, </code>{@link Boolean}{@code >} to avoid
 * boxing.</p>
 *
 * @see BiConsumer
 */
@FunctionalInterface
public interface ObjBooleanBiConsumer<T> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param obj  The object
     * @param bool The primitive boolean
     * @see BiConsumer#accept(Object, Object)
     */
    void accept(T obj, boolean bool);
}
