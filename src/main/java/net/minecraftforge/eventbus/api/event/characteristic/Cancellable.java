/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.internal.Event;

/**
 * The ability to cancel an event is one of the core functionalities of EventBus. This functionality is carried in to
 * this iteration of the EventBus API with the introduction of the cancellable
 * {@linkplain net.minecraftforge.eventbus.api.event.characteristic characteristic}.
 * <p>A cancellable event returns {@code true} from {@link CancellableEventBus#post(Event)} if it was cancelled. When
 * an event is cancelled, it will not be passed to any further
 * non-{@linkplain net.minecraftforge.eventbus.api.listener.Priority#MONITOR monitor} listeners. For further details on
 * a cancellable event's interactions with an event bus, see {@link CancellableEventBus}.</p>
 *
 * @see CancellableEventBus
 */
public non-sealed interface Cancellable extends EventCharacteristic {}
