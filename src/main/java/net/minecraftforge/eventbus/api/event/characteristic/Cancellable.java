/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.internal.EventCharacteristic;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A cancellable event returns {@code true} from {@link CancellableEventBus#post(Event)} if it was cancelled by a
 * {@linkplain CancellableEventBus#addListener(Predicate) 'maybe cancelling'} or
 * {@linkplain CancellableEventBus#addListener(boolean, Consumer) 'always cancelling'} listener.
 *
 * <p>When an event is cancelled, it will not be passed to any further non-{@linkplain Priority#MONITOR monitor}
 * listeners.
 * For further details on a cancellable event's interactions with an EventBus, see {@link CancellableEventBus}.</p>
 *
 * @implNote Internally, the cancellation state is kept on the stack separately from the event instance itself, allowing
 *           for this characteristic to be applied to any event type - even {@link RecordEvent}s.
 * @see CancellableEventBus
 */
public non-sealed interface Cancellable extends EventCharacteristic {}
