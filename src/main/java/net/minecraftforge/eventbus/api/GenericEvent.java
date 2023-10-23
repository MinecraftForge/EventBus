/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

import java.lang.reflect.Type;

/**
 * Implements {@link IGenericEvent} to provide filterable events based on generic type data.
 *
 * Subclasses should extend this if they wish to expose a secondary type based filter (the generic type).
 *
 * @param <T> The type to filter this generic event for
 */
public class GenericEvent<T> extends Event implements IGenericEvent<T>
{
    private Class<T> type;
    public GenericEvent() {}
    protected GenericEvent(Class<T> type)
    {
        this.type = type;
    }

    @Override
    public Type getGenericType()
    {
        return type;
    }
}
