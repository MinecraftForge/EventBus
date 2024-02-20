/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.EventSubclassTransformer;
import net.minecraftforge.eventbus.ListenerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IEvent {

    /**
     * Determine if this function is cancelable at all.
     * @return If access to setCanceled should be allowed
     *
     * Note:
     * Events with the Cancelable annotation will have this method automatically added to return true.
     */
    default boolean isCancelable() {
        return false;
    }

    /**
     * Determine if this event is canceled and should stop executing.
     * @return The current canceled state
     */
    default boolean isCanceled() {
        return false;
    }

    /**
     * Sets the cancel state of this event. Note, not all events are cancelable, and any attempt to
     * invoke this method on an event that is not cancelable (as determined by {@link #isCancelable}
     * will result in an {@link UnsupportedOperationException}.
     * <br>
     * The functionality of setting the canceled state is defined on a per-event bases.
     * <br>
     * Throws a {@link IllegalStateException} if called during the {@link EventPriority#MINOTOR} phase.<br>
     * Note: If the event bus does not track the phases then this protection doesn't function. Most standard
     * use cases should track phases.
     *
     * @param cancel The new canceled value
     */
    default void setCanceled(boolean cancel) {
        throw new UnsupportedOperationException("Attempted to call Event#setCanceled() on a non-cancelable event");
    }

    /**
     * Determines if this event expects a significant result value.
     *
     * Note:
     * Events with the HasResult annotation will have this method automatically added to return true.
     */
    default boolean hasResult() {
        return false;
    }

    /**
     * Returns the value set as the result of this event
     */
    default Event.Result getResult() {
        return null;
    }

    /**
     * Sets the result value for this event, not all events can have a result set, and any attempt to
     * set a result for a event that isn't expecting it will result in a IllegalArgumentException.
     *
     * The functionality of setting the result is defined on a per-event bases.
     *
     * @param value The new result
     */
    default void setResult(Event.Result value) {
        throw new UnsupportedOperationException("Attempted to call Event#setResult() on an event that does not support results");
    }

    /**
     * Returns a ListenerList object that contains all listeners
     * that are registered to this event.
     *
     * Note: for better efficiency, this gets overridden automatically
     * using a Transformer, there is no need to override it yourself.
     * @see EventSubclassTransformer
     *
     * @return Listener List
     */
    default ListenerList getListenerList() {
        return EventListenerHelper.getListenerListInternal(this.getClass(), true);
    }

    @Nullable
    default EventPriority getPhase() {
        throw new UnsupportedOperationException("Attempted to called Event#getPhase() on an event that does not support phase tracking");
    }

    default void setPhase(@NotNull EventPriority value) {
        // No-op because record based events do not support phase tracking
    }
}
