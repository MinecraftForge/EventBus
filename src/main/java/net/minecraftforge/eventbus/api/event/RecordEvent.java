/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.internal.Event;

/**
 * For read-only or shallowly-immutable records.
 * <p>Provides a means for declaring events in a concise and performant manner.</p>
 *
 * <h2>Examples</h2>
 * <p>Here are some conceptual examples of record events:</p>
 * <ul>
 *     <li>Consider a leaderboard event that provides a list of players that listeners can mutate but doesn't allow
 *     the list to be set to null or an immutable list that would result in unexpected behaviour for the other
 *     listeners.</li>
 *     <li>An event where listeners are notified of a player joining the server and can optionally cancel it to kick them
 *     (when combined with the {@link Cancellable} characteristic), but can't set the player to someone else.</li>
 *     <li>Stateless events which do not carry any data inside but are still useful for notifying listeners <i>when</i>
 *     a specific action occurs, such as some types of lifecycle events.</li>
 * </ul>
 *
 * <p>Note that while records are final and cannot extend other classes, inheritance is still possible through other
 * means, such as by implementing a sealed interface and using the Java module system.</p>
 *
 * @apiNote Cancellation is supported for record events as long as they also implement the {@link Cancellable}
 *          characteristic - this is possible thanks to the cancellation state being kept on the stack separately from
 *          the record instance itself.
 * @implSpec This event base type can only be applied to {@linkplain Record Java record classes}.
 *           If you implement this interface on an ordinary class, an exception will be thrown when attempting to
 *           create an associated {@link EventBus}.
 */
public non-sealed interface RecordEvent extends Event {}
