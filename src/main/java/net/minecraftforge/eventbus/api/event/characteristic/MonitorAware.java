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
 * Events that are {@link MonitorAware} can provide stronger immutability guarantees to monitor listeners by returning
 * unmodifiable views or throwing exceptions on mutation attempts when monitoring.
 * <p>Only supported for {@link MutableEvent} at this time.</p>
 *
 * @apiNote <strong>This is an experimental feature!</strong> It may be removed, renamed or otherwise changed without
 * notice.
 */
@ApiStatus.Experimental
public non-sealed interface MonitorAware extends EventCharacteristic {
    @Contract(pure = true)
    @ApiStatus.NonExtendable
    default boolean isMonitoring() {
        assert this instanceof MutableEvent; // note: MutableEvent extends MutableEventInternals
        return ((MutableEventInternals) this).isMonitoring;
    }
}
