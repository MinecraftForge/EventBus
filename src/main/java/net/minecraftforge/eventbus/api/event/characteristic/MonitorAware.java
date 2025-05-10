/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.internal.EventCharacteristic;
import net.minecraftforge.eventbus.internal.MutableEventInternals;

/**
 * Events that are {@linkplain Priority#MONITOR monitor}-aware are able to provide stronger immutability guarantees to
 * monitoring listeners by returning unmodifiable views or throwing exceptions on mutation attempts when monitoring.
 *
 * <p>Events that are {@link MonitorAware} can provide stronger immutability guarantees to monitor listeners by
 * returning unmodifiable views or throwing exceptions on mutation attempts when monitoring.</p>
 * <p>Only supported for {@link MutableEvent} at this time.</p>
 *
 * @apiNote This is an experimental feature that may be removed, renamed or otherwise changed without notice.
 * @implNote This characteristic is only supported for the {@link MutableEvent} base type at this time.
 *           If combined with a different base type (such as {@link RecordEvent}), an exception will be thrown when
 *           attempting to create an associated {@link EventBus}.
 */
public non-sealed interface MonitorAware extends EventCharacteristic {
    default boolean isMonitoring() {
        assert this instanceof MutableEvent; // note: MutableEvent extends MutableEventInternals
        return ((MutableEventInternals) this).isMonitoring;
    }
}
