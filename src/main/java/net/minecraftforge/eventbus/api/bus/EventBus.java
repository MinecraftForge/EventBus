/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.bus;

import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.internal.AbstractEventBusImpl;
import net.minecraftforge.eventbus.internal.BusGroupImpl;
import net.minecraftforge.eventbus.internal.EventBusImpl;

import java.util.function.Consumer;

public sealed interface EventBus<T extends Event> permits CancellableEventBus, AbstractEventBusImpl, EventBusImpl {
    /**
     * Adds a listener to this EventBus with the default priority of {@link Priority#NORMAL}.
     * @param listener The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     */
    EventListener addListener(Consumer<T> listener);

    /**
     * Adds a listener to this EventBus with the given priority.
     * @param priority The priority of this listener. Higher numbers are called first.
     * @param listener The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     * @see Priority For common priority values
     */
    EventListener addListener(byte priority, Consumer<T> listener);

    /**
     * Re-adds a listener to this EventBus that was previously removed with {@link #removeListener(EventListener)}.
     * @param listener The exact same reference returned by an {@code addListener} method
     * @return The same reference that was passed in
     */
    EventListener addListener(EventListener listener);

    /**
     * Removes a listener from this EventBus that was previously added with one of the {@code addListener} methods.
     * @param listener The exact same reference returned by an {@code addListener} method
     */
    void removeListener(EventListener listener);

    /**
     * @param event The instance of this event to post to listeners
     * @return {@code true} if the event implements {@link Cancellable} and the event was cancelled
     *         by a listener
     */
    boolean post(T event);

    /**
     * @param event The instance of this event to fire to listeners
     * @return The possibly mutated event instance after all applicable listeners have been called
     */
    T fire(T event);

    /**
     * If making a new event instance is expensive, you can check against this method to avoid creating a new instance
     * unnecessarily.
     * @apiNote You only need to check this if event creation is expensive. If it's cheap, just call {@link #post(Event)}
     *          or {@link #fire(Event)} directly and let the JIT handle it.
     * @return {@code true} if there are any listeners registered to this EventBus.
     */
    boolean hasListeners();

    /**
     * Creates a new EventBus for the given event type on the default {@link BusGroup}.
     * <p>
     *     <b>Important:</b> The returned EventBus MUST be stored in a {@code static final} field - failing to do so
     *     will severely hurt performance
     * </p>
     * @apiNote There can only be one EventBus instance per event type per BusGroup.
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // E can be a subtype of Event which is publicly accessible
    static <E extends Event> EventBus<E> create(Class<E> eventType) {
        return create(BusGroup.DEFAULT, eventType);
    }

    /**
     * Creates a new EventBus for the given event type on the given {@link BusGroup}.
     * <p>
     *     <b>Important:</b> The returned EventBus MUST be stored in a {@code static final} field - failing to do so
     *     will severely hurt performance
     * </p>
     * @apiNote There can only be one EventBus instance per event type per BusGroup.
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // E can be a subtype of Event which is publicly accessible
    static <E extends Event> EventBus<E> create(BusGroup busGroup, Class<E> eventType) {
        return ((BusGroupImpl) busGroup).getOrCreateEventBus(eventType);
    }
}
