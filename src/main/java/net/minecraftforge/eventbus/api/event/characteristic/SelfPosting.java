/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.internal.EventCharacteristic;

/**
 * Experimental feature - may be removed, renamed or otherwise changed without notice.
 * <p>{@link SelfPosting} events are associated with a default {@link EventBus} in order to offer some convenience
 * instance methods.</p>
 * <u>Example</u>
 * {@snippet :
 * import net.minecraftforge.eventbus.api.event.RecordEvent;
 *
 * // Event declaration
 * public record ExampleEvent() implements SelfPosting<ExampleEvent>, RecordEvent {
 *     public static final EventBus<ExampleEvent> BUS = EventBus.create(ExampleEvent.class);
 *
 *     @Override
 *     public EventBus<ExampleEvent> getDefaultBus() {
 *         return BUS;
 *     }
 * }
 *
 * // Now you can do this concise posting method
 * new ExampleEvent().post();
 *
 * // instead of this
 * ExampleEvent.BUS.post(new ExampleEvent());
 *}
 */
public non-sealed interface SelfPosting<T extends Event> extends EventCharacteristic {
    /**
     * @implSpec This should directly return a {@code static final} field without additional logic or processing.
     */
    EventBus<T> getDefaultBus();

    /**
     * @see EventBus#post(Event)
     */
    @SuppressWarnings("unchecked")
    default boolean post() {
        return getDefaultBus().post((T) this);
    }

    /**
     * @see EventBus#fire(Event)
     */
    @SuppressWarnings("unchecked")
    default T fire() {
        return getDefaultBus().fire((T) this);
    }
}
