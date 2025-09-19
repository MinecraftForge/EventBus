package net.minecraftforge.eventbus.testjar.events;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;

/**
 * This event tests the passthrough optimisation.
 * <p>An event is considered passthrough when all the following conditions are met:</p>
 * <ul>
 *     <li>The parent is sealed with a single child</li>
 *     <li>The child is final or a record</li>
 *     <li>They share the same event characteristics</li>
 * </ul>
 * <p>When these conditions are met, the {@code EventBus.create(Impl.class)} call returns the EventBus instance of its
 * parent, instead of creating a new EventBus for the child. All calls to the child go directly to the parent, saving
 * memory.</p>
 */
public sealed interface PassthroughEvent extends InheritableEvent {
    EventBus<PassthroughEvent> BUS = EventBus.create(PassthroughEvent.class);

    record Impl() implements PassthroughEvent {
        public static final EventBus<Impl> BUS = EventBus.create(Impl.class);
    }
}
