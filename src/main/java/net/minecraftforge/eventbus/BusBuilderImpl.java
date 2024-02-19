/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventExceptionHandler;

/**
 * BusBuilder Implementation, public for BusBuilder.builder() only, don't use this directly.
 */
public final class BusBuilderImpl implements BusBuilder {
    IEventExceptionHandler exceptionHandler;
    boolean trackPhases = true;
    boolean startShutdown = false;
    boolean checkTypesOnDispatch = false;
    Class<?> markerType = IEvent.class;
    boolean modLauncher = false;

    @Override
    public BusBuilder setTrackPhases(boolean trackPhases) {
        this.trackPhases = trackPhases;
        return this;
    }

    @Override
    public BusBuilder setExceptionHandler(IEventExceptionHandler handler) {
        this.exceptionHandler =  handler;
        return this;
    }

    @Override
    public BusBuilder startShutdown() {
        this.startShutdown = true;
        return this;
    }

    @Override
    public BusBuilder checkTypesOnDispatch() {
        this.checkTypesOnDispatch = true;
        return this;
    }

    @Override
    public BusBuilder markerType(Class<?> type) {
        if (!type.isInterface()) throw new IllegalArgumentException("Cannot specify a class marker type");
        this.markerType = type;
        return this;
    }

    @Override
    public BusBuilder useModLauncher() {
        this.modLauncher = true;
        return this;
    }

    @Override
    public IEventBus build() {
        return new EventBus(this);
    }
}
