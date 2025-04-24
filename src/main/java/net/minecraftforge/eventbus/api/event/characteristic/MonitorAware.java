/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.internal.MutableEventInternals;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * Events that are {@linkplain net.minecraftforge.eventbus.api.listener.Priority#MONITOR monitor}-aware are able to
 * provide stronger immutability guarantees to monitor listeners by returning unmodifiable views and ignoring (or
 * throwing exceptions) on mutation attempts when monitoring.
 *
 * @implNote This characteristic is only supported for {@link MutableEvent} at this time. If used on any other type that
 * does not extend it, an {@link IllegalArgumentException} will be thrown when the
 * {@linkplain net.minecraftforge.eventbus.api.bus.EventBus event bus} is created for it.
 * @apiNote <strong>This is an experimental feature!</strong> It may be removed, renamed or otherwise changed without
 * notice.
 */
@ApiStatus.Experimental
public non-sealed interface MonitorAware extends EventCharacteristic {
    /**
     * Checks if this event is currently being
     * {@linkplain net.minecraftforge.eventbus.api.listener.Priority#MONITOR monitored}. This can be used to provide
     * stronger immutability guarantees as described by the documentation of {@link MonitorAware}.
     *
     * @return
     * @implSpec This method
     * <strong>{@linkplain org.jetbrains.annotations.ApiStatus.NonExtendable must not be overridden}!</strong> It uses
     * internals of {@link MutableEvent} in order to determine the monitoring state of the event. If overridden, your
     * event will be unable to determine if it is actually monitoring!
     */
    @Contract(pure = true)
    @ApiStatus.NonExtendable
    default boolean isMonitoring() {
        assert this instanceof MutableEvent; // note: MutableEvent extends MutableEventInternals
        return ((MutableEventInternals) this).isMonitoring;
    }
}
