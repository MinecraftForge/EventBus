/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

import java.lang.reflect.Type;

/**
 * Provides a generic event - one that is able to be filtered based on the supplied Generic type
 *
 * @param <T> The filtering type
 */
public interface IGenericEvent<T> {
    Type getGenericType();
}
