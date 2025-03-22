/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.BusBuilderImpl;

import java.util.EnumSet;

/**
 * Build a bus
 */
public interface BusBuilder {
    public static BusBuilder builder() {
        return new BusBuilderImpl();
    }

    /* true by default */
    BusBuilder setTrackPhases(boolean trackPhases);
    default BusBuilder setPhasesToTrack(EnumSet<EventPriority> phases) {
        throw new UnsupportedOperationException();
    }
    default BusBuilder setPhasesToTrack(EventPriority... phases) {
        return setPhasesToTrack(EnumSet.of(phases[0], phases));
    }
    default BusBuilder setPhasesToTrack(EventPriority phase) {
        return setPhasesToTrack(EnumSet.of(phase));
    }
    BusBuilder setExceptionHandler(IEventExceptionHandler handler);
    BusBuilder startShutdown();
    BusBuilder checkTypesOnDispatch();
    BusBuilder markerType(Class<?> type);

    /* Use ModLauncher hooks when creating ASM handlers. */
    BusBuilder useModLauncher();

    IEventBus build();
}
