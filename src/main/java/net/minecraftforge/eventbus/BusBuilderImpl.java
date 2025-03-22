/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.*;

import java.util.EnumSet;

/**
 * BusBuilder Implementation, public for BusBuilder.builder() only, don't use this directly.
 */
public final class BusBuilderImpl implements BusBuilder {
    static final EnumSet<EventPriority> ALL_PHASES = EnumSet.allOf(EventPriority.class);
    static final EnumSet<EventPriority> NO_PHASES = EnumSet.noneOf(EventPriority.class);
    private static final EnumSet<EventPriority> MONITOR_ONLY = EnumSet.of(EventPriority.MONITOR);

    IEventExceptionHandler exceptionHandler;
    boolean trackPhases = true;
    EnumSet<EventPriority> phasesToTrack = ALL_PHASES;
    boolean startShutdown = false;
    boolean checkTypesOnDispatch = false;
    Class<?> markerType = Event.class;
    boolean modLauncher = false;

    @Override
    public BusBuilder setTrackPhases(boolean trackPhases) {
        this.trackPhases = trackPhases;
        this.phasesToTrack = trackPhases ? ALL_PHASES : NO_PHASES;
        return this;
    }

    @Override
    public BusBuilder setPhasesToTrack(EnumSet<EventPriority> phases) {
        if (phases.isEmpty()) {
            this.trackPhases = false;
            this.phasesToTrack = NO_PHASES;
        } else {
            this.trackPhases = true;
            this.phasesToTrack = phases;
        }
        return this;
    }

    @Override
    public BusBuilder setPhasesToTrack(EventPriority phase) {
        return phase == EventPriority.MONITOR
                ? setPhasesToTrack(BusBuilderImpl.MONITOR_ONLY)
                : setPhasesToTrack(EnumSet.of(phase));
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
