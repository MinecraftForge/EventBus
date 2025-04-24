/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.internal.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * Self-posting events are associated with a default {@linkplain EventBus event bus} in order to offer some convenient
 * instance methods.
 *
 * <h2>Example</h2>
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
 *
 * @apiNote <strong>This is an experimental feature!</strong> It may be removed, renamed or otherwise changed without
 * notice.
 */
@ApiStatus.Experimental
public non-sealed interface SelfPosting<T extends Event> extends EventCharacteristic {
    /**
     * The default event bus for this event. It will be used by the {@link #post()} and {@link #fire()} methods.
     *
     * @return The default event bus for this event
     * @implSpec This method must directly return a {@code static final} field without additional logic or processing.
     * Failure to do so may result in performance hindrances.
     */
    @Contract(pure = true)
    EventBus<T> getDefaultBus();

    /**
     * Posts this event to all listeners registered to its {@linkplain #getDefaultBus() default event bus}.
     *
     * @return {@code true} if the event was cancelled <strong>and</strong> this event bus is a
     * {@linkplain net.minecraftforge.eventbus.api.bus.CancellableEventBus cancellable event bus}
     * @see EventBus#post(Event)
     */
    @ApiStatus.NonExtendable
    @SuppressWarnings("unchecked")
    default boolean post() {
        return getDefaultBus().post((T) this);
    }

    /**
     * Fires this event to all listeners registered to its {@linkplain #getDefaultBus() default event bus}.
     * <p>After posting, this event is returned from this method. <i>It may be mutated.</i></p>
     *
     * @return This event after being posted
     * @see EventBus#fire(Event)
     */
    @ApiStatus.NonExtendable
    @Contract(value = "-> this")
    @SuppressWarnings("unchecked")
    default T fire() {
        return getDefaultBus().fire((T) this);
    }
}
