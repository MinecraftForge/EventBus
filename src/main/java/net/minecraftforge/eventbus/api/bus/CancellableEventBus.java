/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.bus;

import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.internal.BusGroupImpl;
import net.minecraftforge.eventbus.internal.CancellableEventBusImpl;
import net.minecraftforge.eventbus.internal.Event;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Events can have characteristics, and one such is the ability to be {@linkplain Cancellable cancelled}. The state of
 * an event's cancellation, however, is not attached to the event itself, but rather returned as a result of
 * {@linkplain #post(Event) posting} the event. The cancellable event bus is a specialized type of event bus that is
 * designed to handle this cancellable nature of events without explicitly requiring events themselves to store their
 * cancellation state.
 * <p>For more details on the event bus in general, see {@link EventBus}.</p>
 *
 * <h2>Usage</h2>
 * <p>Event listeners for cancellable events can have a few additional properties that set them apart from normal
 * listeners. These are the ability to cancel the event or to
 * {@linkplain EventListener#alwaysCancelling() always cancel} the event. The specialized
 * {@link #addListener(Predicate)} method, along with its sisters in this class, are designed to give event listeners
 * that characteristic.</p>
 *
 * <h2>Example</h2>
 * <p>Here is a small example that showcases the different listeners this type of event bus can have.</p>
 * {@snippet :
 * import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
 * import net.minecraftforge.eventbus.api.event.RecordEvent;
 * import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
 * import net.minecraftforge.eventbus.api.listener.Priority;
 *
 * import java.util.Random;
 *
 * public record MyCustomCancellableEvent() implements RecordEvent, Cancellable {
 *     // you MUST use the #create method in CancellableEventBus, or you won't be able to see the cancellation state from #post!
 *     public static final CancellableEventBus<MyCustomCancellableEvent> BUS = CancellableEventBus.create(MyCustomCancellableEvent.class);
 *
 *     // a listener that does not cancel, only listens quietly
 *     private static void onMyCustomEvent(MyCustomCancellableEvent event) {
 *         System.out.println("Received custom event: " + event);
 *     }
 *
 *     // a listener that might cancel the event
 *     private static boolean alsoOnMyCustomEvent(MyCustomCancellableEvent event) {
 *         System.out.println("Received custom event (but later!): " + event);
 *         return new Random().nextBoolean();
 *     }
 *
 *     // a listener that always cancels (registered with CancellableEventBus#addListener(true, this::method))
 *     private static void alwaysCancelMyEvent(MyCustomCancellableEvent event) {
 *         System.err.println("We are cancelling the event!!!");
 *     }
 *
 *     private static void cannotRecieveEvent(MyCustomCancellableEvent event) {
 *         throw new IllegalStateException("Event wasn't cancelled??? " + event);
 *     }
 *
 *     // a monitoring listener that will always receive the event, even if it is cancelled
 *     private static void monitoringCancelledEvent(MyCustomCancellableEvent event, boolean cancelled) {
 *         System.out.println("Monitoring custom event: " + event + ", cancelled: " + cancelled);
 *     }
 *
 *     public static void run() {
 *         BUS.addListener(MyCustomCancellableEvent::onMyCustomEvent);
 *         BUS.addListener(Priority.LOW, MyCustomCancellableEvent::alsoOnMyCustomEvent);              // might cancel
 *         BUS.addListener(Priority.LOWEST + 1, true, MyCustomCancellableEvent::alwaysCancelMyEvent); // will always cancel
 *         BUS.addListener(MyCustomCancellableEvent::cannotRecieveEvent);                             // will never be called
 *         BUS.addListener(MyCustomCancellableEvent::monitoringCancelledEvent);                       // monitors events, even cancelled ones
 *     }
 * }
 *}
 *
 * @param <T> The type of cancellable event for this bus
 */
public sealed interface CancellableEventBus<T extends Event & Cancellable>
    extends EventBus<T> permits CancellableEventBusImpl {
    /**
     * Adds a listener to this EventBus with the default priority of {@link Priority#NORMAL}.
     * <p>This listener, based on the given boolean, may always cancel the event when it is invoked. If it is
     * {@code true}, this method acts as if you {@linkplain #addListener(Predicate) added a predicate listener} that
     * always returns {@code true}, but with additional optimisations.</p>
     * <p>If you plan on passing in {@code false} instead, you should instead consider using
     * {@link #addListener(Consumer)}.</p>
     *
     * @param alwaysCancelling Whether to always cancel the event after calling the listener
     * @param listener         The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     */
    default EventListener addListener(boolean alwaysCancelling, Consumer<T> listener) {
        return addListener(Priority.NORMAL, alwaysCancelling, listener);
    }

    /**
     * Adds a listener to this EventBus with the given {@linkplain Priority priority}.
     * <p>This listener, based on the given boolean, may always cancel the event when it is invoked. If it is
     * {@code true}, this method acts as if you {@linkplain #addListener(Predicate) added a predicate listener} that
     * always returns {@code true}, but with additional optimisations.</p>
     * <p>If you plan on passing in {@code false} instead, you should instead consider using
     * {@link #addListener(Consumer)}.</p>
     *
     * @param priority         The priority for the listener
     * @param alwaysCancelling Whether to always cancel the event after calling the listener
     * @param listener         The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     */
    EventListener addListener(byte priority, boolean alwaysCancelling, Consumer<T> listener);

    /**
     * Adds a listener to this EventBus with the default priority of {@link Priority#NORMAL}.
     * <p>The predicate listener can return {@code true} to cancel the event.</p>
     *
     * @param listener The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     */
    EventListener addListener(Predicate<T> listener);

    /**
     * Adds a listener to this EventBus with the given {@linkplain Priority priority}.
     * <p>The predicate listener can return {@code true} to cancel the event.</p>
     *
     * @param priority The priority for the listener
     * @param listener The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     * @see Priority For common priority values
     */
    EventListener addListener(byte priority, Predicate<T> listener);

    /**
     * Adds a cancellation-aware monitoring listener to this EventBus.
     * <p>This listener will always run at a priority of {@link Priority#MONITOR} and will always receive cancelled
     * events.</p>
     *
     * @param listener The listener to add
     * @return A reference that can be used to remove this listener later with {@link #removeListener(EventListener)}
     * @see Priority#MONITOR
     */
    EventListener addListener(ObjBooleanBiConsumer<T> listener);

    /**
     * Posts the given event to all listeners registered to this bus.
     * <p>Unlike {@link EventBus#post(Event)}, the cancellation state of the posted event is returned by this
     * method.</p>
     *
     * @param event The instance of this event to post to listeners
     * @return {@code true} if the event was cancelled
     */
    @Override
    boolean post(T event);

    /**
     * {@inheritDoc}
     *
     * @deprecated Using this method with a cancellable event bus is not recommended, as it does not capture the event's
     * cancellation state. Use {@link #post(Event)} instead.
     */
    @Deprecated
    @Override
    T fire(T event);

    /**
     * Creates a new cancellable event bus for the given {@linkplain net.minecraftforge.eventbus.api.event event} type
     * on the {@linkplain BusGroup#DEFAULT default bus group}.
     * <p>The returned EventBus <strong>must be stored in a {@code static final} field!</strong> Failing to do so will
     * severely hinder performance.</p>
     * <p>Additionally, there can only be one event bus instance per event type per bus group. If an event bus already
     * exists for the given type, it will be returned instead.</p>
     *
     * @param eventType The cancellable event type for the bus
     * @param <E>       The type of cancellable event this bus is for
     * @return The newly-created event bus
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // E can be a subtype of Event which is publicly accessible
    static <E extends Event & Cancellable> CancellableEventBus<E> create(Class<E> eventType) {
        return create(BusGroup.DEFAULT, eventType);
    }

    /**
     * Creates a new cancellable event bus for the given {@linkplain Cancellable cancellable}
     * {@linkplain net.minecraftforge.eventbus.api.event event} type on the given {@linkplain BusGroup bus group}.
     * <p>The returned EventBus <strong>must be stored in a {@code static final} field!</strong> Failing to do so will
     * severely hinder performance.</p>
     * <p>Additionally, there can only be one event bus instance per event type per bus group. If an event bus already
     * exists for the given type, it will be returned instead.</p>
     *
     * @param busGroup  The bus group to create the event bus on
     * @param eventType The cancellable event type for the bus
     * @param <E>       The type of cancellable event this bus is for
     * @return The newly-created event bus
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // E can be a subtype of Event which is publicly accessible
    static <E extends Event & Cancellable> CancellableEventBus<E> create(BusGroup busGroup, Class<E> eventType) {
        return (CancellableEventBus<E>) ((BusGroupImpl) busGroup).getOrCreateEventBus(eventType);
    }
}
