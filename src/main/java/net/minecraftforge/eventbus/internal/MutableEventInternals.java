/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.MonitorAware;

public sealed abstract class MutableEventInternals permits MutableEvent {
    /**
     * @see MonitorAware
     */
    public transient boolean isMonitoring;
}
