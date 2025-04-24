/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.bus;

import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.internal.AbstractEventBusImpl;
import net.minecraftforge.eventbus.internal.BusGroupImpl;
import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.internal.EventBusImpl;
import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;

/**
 * An event bus is a host of listeners for a specific event.
 * <p>It can be thought of much like an actual bus. A bus has passengers that are all headed for the same destination,
 * or at least are on the same route.</p>
 *
 * <h2>Usage</h2>
 * <p>The key idea to understand about an event bus is that is designed to be used with a specific event tied to a
 * specific {@linkplain BusGroup bus group}. While not all listeners on the event bus may behave the same, they are all
 * listening for the same event.</p>
 * <p>Listeners can have different characteristics based on how it is registered (this may be subject to the event's
 * {@linkplain net.minecraftforge.eventbus.api.event.characteristic characteristics}). They are registered through
 * {@link #addListener(Consumer)} or one of its sister methods. Each registering method contains additional details on
 * how the registered listener behaves.</p>
 *
 * <h2>Example</h2>
 * <p>Here is a small example showing the simple registration of two event listeners with different priorities.</p>
 * {@snippet :
 * import net.minecraftforge.eventbus.api.bus.EventBus;
 * import net.minecraftforge.eventbus.api.event.MutableEvent;
 * import net.minecraftforge.eventbus.api.listener.Priority;
 *
 * public class MyCustomEvent extends MutableEvent {
 *     protected static final String HELLO = "Hello, world!";
 *
 *     public static final EventBus<MyCustomEvent> BUS = EventBus.create(MyCustomEvent.class);
 *
 *     private static void onMyCustomEvent(MyCustomEvent event) {
 *         System.out.println("Received custom event: " + event);
 *     }
 *
 *     private static void alsoOnMyCustomEvent(MyCustomEvent event) {
 *         System.out.println("Received custom event (but later!): " + event);
 *     }
 *
 *     public static void run() {
 *         BUS.addListener(MyCustomEvent::onMyCustomEvent);
 *         BUS.addListener(Priority.LOW, MyCustomEvent::alsoOnMyCustomEvent);
 *     }
 * }
 *}
 *
 * <h2>Cancellability</h2>
 * <p>Events have the ability to be {@linkplain Cancellable cancellable}. If you need to use a cancellable event, use
 * {@link CancellableEventBus} instead, which include special handling for posting cancellable events and recieving the
 * cancellation state. As discussed in the documentation for the cancellable characteristic, the cancellation state is
 * <i>not attached</i> to the event instance.</p>
 *
 * @param <T> The type of event for this bus
 */
public sealed interface EventBus<T extends Event> permits CancellableEventBus, AbstractEventBusImpl, EventBusImpl {
    /**
     * Adds a listener to this EventBus with the default priority of {@link Priority#NORMAL}.
     *
     * @param listener The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     */
    EventListener addListener(Consumer<T> listener);

    /**
     * Adds a listener to this EventBus with the given {@linkplain Priority priority}.
     *
     * @param priority The priority for the listener
     * @param listener The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     * @see Priority
     */
    EventListener addListener(byte priority, Consumer<T> listener);

    /**
     * Re-adds a listener to this EventBus that was previously removed with {@link #removeListener(EventListener)}.
     *
     * @param listener The event listener returned immediately after it was initially added
     * @return The listener that was re-added
     * @apiNote Using this over re-adding the listener with {@link #addListener(Consumer)} is recommended for
     * performance, as it removes the need to create another {@link EventListener} state.
     */
    @Contract("_ -> param1")
    EventListener addListener(EventListener listener);

    /**
     * Removes a listener from this EventBus that was previously added with {@link #addListener(Consumer)} or one of its
     * sisters.
     *
     * @param listener The event listener returned immediately after it was initially added
     */
    void removeListener(EventListener listener);

    /**
     * Posts the given event to all listeners registered to this bus.
     *
     * @param event The instance of this event to post to listeners
     * @return {@code true} if the event was cancelled <strong>and</strong> this event bus is a
     * {@linkplain CancellableEventBus cancellable event bus}
     * @apiNote This bus will <strong>always return {@code false}</strong> unless it is a
     * {@linkplain CancellableEventBus cancellable event bus}.
     * @see CancellableEventBus#post(Event)
     */
    boolean post(T event);

    /**
     * Fires the given event to all listeners registered to this bus.
     * <p>After posting, the event itself is returned from this method. <i>It may be mutated.</i></p>
     *
     * @param event The instance of this event to fire to listeners
     * @return The event after being posted
     */
    @Contract(value = "_ -> param1")
    T fire(T event);

    /**
     * Checks if this event bus has any listeners registered to it.
     * <p>If making a new event instance is expensive, you can check against this method to avoid creating a new
     * instance unnecessarily.</p>
     *
     * @return {@code true} if there are any listeners registered
     * @apiNote If event creation is cheap, you should instead call {@link #post(Event)} or {@link #fire(Event)}
     * directly and let the JIT handle the side effects.
     */
    boolean hasListeners();

    /**
     * Creates a new event bus for the given {@linkplain net.minecraftforge.eventbus.api.event event} type on the
     * {@linkplain BusGroup#DEFAULT default bus group}.
     * <p>The returned EventBus <strong>must be stored in a {@code static final} field!</strong> Failing to do so will
     * severely hinder performance.</p>
     * <p>Additionally, there can only be one event bus instance per event type per bus group. If an event bus already
     * exists for the given type, it will be returned instead.</p>
     *
     * @param eventType The event type for the bus
     * @param <E>       The type of event this bus is for
     * @return The newly-created event bus
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // E can be a subtype of Event which is publicly accessible
    static <E extends Event> EventBus<E> create(Class<E> eventType) {
        return create(BusGroup.DEFAULT, eventType);
    }

    /**
     * Creates a new event bus for the given {@linkplain net.minecraftforge.eventbus.api.event event} type on the given
     * {@linkplain BusGroup bus group}.
     * <p>The returned event bus <strong>must be stored in a {@code static final} field!</strong> Failing to do so will
     * severely hinder performance.</p>
     * <p>Additionally, there can only be one event bus instance per event type per bus group. If an event bus already
     * exists for the given type, it will be returned instead.</p>
     *
     * @param busGroup  The bus group to create the event bus on
     * @param eventType The event type for the bus
     * @param <E>       The type of event this bus is for
     * @return The newly-created event bus
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // E can be a subtype of Event which is publicly accessible
    static <E extends Event> EventBus<E> create(BusGroup busGroup, Class<E> eventType) {
        return ((BusGroupImpl) busGroup).getOrCreateEventBus(eventType);
    }
}
