/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.internal.Event;

/**
 * A cancellable event returns {@code true} from {@link CancellableEventBus#post(Event)} if it was cancelled.
 * <p>When an event is cancelled, it will not be passed to any further non-monitor listeners.</p>
 */
public non-sealed interface Cancellable extends EventCharacteristic {}
