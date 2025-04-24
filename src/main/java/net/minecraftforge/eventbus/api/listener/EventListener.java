/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.listener;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.internal.EventListenerImpl;
import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;

/**
 * An event listener holds info about a lister that was registered in an {@linkplain EventBus event bus} or
 * {@linkplain net.minecraftforge.eventbus.api.bus.BusGroup bus group}. Consumers should retain instances of this
 * interface in order to remove listeners that were previously added to the same event bus.
 *
 * @implNote Internally, this acts as a wrapper over lambdas to give them identity, enrich debug info and to allow
 * various conversion operations to different lambda types.
 */
public sealed interface EventListener permits EventListenerImpl {
    /**
     * The event type that this listener is listening for.
     *
     * @return The event type
     */
    @Contract(pure = true)
    @SuppressWarnings("ClassEscapesDefinedScope") // ? can be a subtype of Event which is publicly accessible
    Class<? extends Event> eventType();

    /**
     * The priority of this listener.
     * <p>This is used by the {@linkplain EventBus event bus} to determine the order in which to invoke listeners.</p>
     *
     * @return The priority of this listener
     * @see Priority
     */
    @Contract(pure = true)
    byte priority();

    /**
     * Whether this listener will always cancel a
     * {@linkplain net.minecraftforge.eventbus.api.event.characteristic.Cancellable cancellable} event.
     * This is almost always {@code false} unless the listener is created using
     * {@link CancellableEventBus#addListener(boolean, Consumer)} or
     * {@link CancellableEventBus#addListener(byte, boolean, Consumer)}.
     *
     * @return {@code true} if this listener is always cancelling
     * @see CancellableEventBus#addListener(boolean, Consumer)
     */
    @Contract(pure = true)
    default boolean alwaysCancelling() {
        return false;
    }
}
