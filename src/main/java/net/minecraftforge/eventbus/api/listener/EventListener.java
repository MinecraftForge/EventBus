/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.listener;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.internal.EventListenerImpl;

import java.util.function.Consumer;

/**
 * Users can retain instances of this interface to remove listeners that were previously added to the same
 * {@link EventBus}. You can obtain instances of this interface by calling any of the {@code addListener} methods
 * on an EventBus, such as {@link EventBus#addListener(Consumer)}.
 *
 * @implNote Internally, this acts as a wrapper over lambdas to give them identity, enrich debug info and to allow
 * various conversion operations to different lambda types.
 */
public sealed interface EventListener permits EventListenerImpl {
    @SuppressWarnings("ClassEscapesDefinedScope") // ? can be a subtype of Event which is publicly accessible
    Class<? extends Event> eventType();

    /**
     * The priority of this listener. Higher numbers are called first.
     * @see Priority
     */
    byte priority();

    /**
     * Whether this listener is known to always cancel the {@link Cancellable} event when called.
     * @see CancellableEventBus#addListener(boolean, Consumer)
     */
    default boolean alwaysCancelling() {
        return false;
    }
}
