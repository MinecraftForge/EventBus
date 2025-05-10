/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.internal.EventCharacteristic;

/**
 * A cancellable event returns {@code true} from {@link CancellableEventBus#post(Event)} if it was cancelled.
 * <p>When an event is cancelled, it will not be passed to any further non-{@linkplain Priority#MONITOR monitor}
 * listeners.
 * For further details on a cancellable event's interactions with an EventBus, see {@link CancellableEventBus}.</p>
 *
 * @see CancellableEventBus
 */
public non-sealed interface Cancellable extends EventCharacteristic {}
