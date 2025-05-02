/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.internal.EventCharacteristic;
import net.minecraftforge.eventbus.internal.MutableEventInternals;

/**
 * Experimental feature - may be removed, renamed or otherwise changed without notice.
 * <p>Events that are {@link MonitorAware} can provide stronger immutability guarantees to monitor listeners by
 * returning unmodifiable views or throwing exceptions on mutation attempts when monitoring.</p>
 * <p>Only supported for {@link MutableEvent} at this time.</p>
 */
public non-sealed interface MonitorAware extends EventCharacteristic {
    default boolean isMonitoring() {
        assert this instanceof MutableEvent; // note: MutableEvent extends MutableEventInternals
        return ((MutableEventInternals) this).isMonitoring;
    }
}
