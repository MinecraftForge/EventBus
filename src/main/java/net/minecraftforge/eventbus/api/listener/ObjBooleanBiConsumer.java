/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.listener;

import java.util.function.BiConsumer;

/**
 * A {@link BiConsumer} that takes an object and a primitive boolean, to avoid boxing.
 */
@FunctionalInterface
public interface ObjBooleanBiConsumer<T> {
    void accept(T obj, boolean bool);
}
