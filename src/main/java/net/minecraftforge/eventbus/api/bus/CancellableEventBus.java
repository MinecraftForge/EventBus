/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.bus;

import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.internal.BusGroupImpl;
import net.minecraftforge.eventbus.internal.CancellableEventBusImpl;

import java.util.function.Consumer;
import java.util.function.Predicate;

public sealed interface CancellableEventBus<T extends Event & Cancellable>
        extends EventBus<T> permits CancellableEventBusImpl {
    /**
     * Adds an always cancelling listener to this EventBus with the default priority of {@link Priority#NORMAL}.
     * @param alwaysCancelling If true, always cancel the event after calling the listener. This acts as if you
     *                         added a Predicate listener that always returns true, but with additional optimisations.
     *                         <p>If false, you should use {@link #addListener(Consumer)} instead to avoid unnecessary
     *                         breaking changes if the event is no longer cancellable in the future.</p>
     * @param listener The listener to add.
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}.
     * @see #addListener(Predicate) For adding a listener that can cancel the event conditionally
     * @see #addListener(Consumer) For adding a listener that never cancels the event
     */
    default EventListener addListener(boolean alwaysCancelling, Consumer<T> listener) {
        return addListener(Priority.NORMAL, alwaysCancelling, listener);
    }

    /**
     * Adds an always cancelling listener to this EventBus with the specified priority.
     * @param alwaysCancelling If true, always cancel the event after calling the listener. This acts as if you
     *                         added a Predicate listener that always returns true, but with additional optimisations.
     *                         <p>If false, you should use {@link #addListener(byte, Consumer)} instead to avoid
     *                         unnecessary breaking changes if the event is no longer cancellable in the future</p>
     * @param listener The listener to add.
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}.
     * @see Priority For common priority values
     */
    EventListener addListener(byte priority, boolean alwaysCancelling, Consumer<T> listener);

    /**
     * Adds a possibly cancelling listener to this EventBus with the default priority of {@link Priority#NORMAL}.
     * @param listener The listener to add.
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}.
     */
    EventListener addListener(Predicate<T> listener);

    /**
     * Adds a possibly cancelling listener to this EventBus with the specified priority.
     * @param priority The priority of this listener. Higher numbers are called first.
     * @param listener The listener to add.
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}.
     * @see Priority For common priority values
     */
    EventListener addListener(byte priority, Predicate<T> listener);

    /**
     * Adds a cancellation-aware monitoring listener to this EventBus.
     * @param listener The listener to add.
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}.
     */
    EventListener addListener(ObjBooleanBiConsumer<T> listener);

    /**
     * Creates a new CancellableEventBus for the given event type on the default {@link BusGroup}.
     * <p>
     *     <b>Important:</b> The returned EventBus MUST be stored in a {@code static final} field - failing to do so
     *     will severely hurt performance
     * </p>
     * @apiNote There can only be one EventBus instance per event type per BusGroup.
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // E can be a subtype of Event which is publicly accessible
    static <T extends Event & Cancellable> CancellableEventBus<T> create(Class<T> eventType) {
        return create(BusGroup.DEFAULT, eventType);
    }

    /**
     * Creates a new CancellableEventBus for the given event type on the given {@link BusGroup}.
     * <p>
     *     <b>Important:</b> The returned EventBus MUST be stored in a {@code static final} field - failing to do so
     *     will severely hurt performance
     * </p>
     * @apiNote There can only be one EventBus instance per event type per BusGroup.
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // E can be a subtype of Event which is publicly accessible
    static <T extends Event & Cancellable> CancellableEventBus<T> create(BusGroup busGroup, Class<T> eventType) {
        return (CancellableEventBus<T>) ((BusGroupImpl) busGroup).getOrCreateEventBus(eventType);
    }
}
