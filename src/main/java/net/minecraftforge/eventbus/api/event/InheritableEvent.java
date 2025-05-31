/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event;

import net.minecraftforge.eventbus.internal.Event;

/**
 * A hybrid of an event base type and characteristic - implementing this interface on your event type allows for event
 * posting on subclasses to propagate to your event type, essentially opting into event inheritance. This allows for
 * flexible event hierarchies with mixed levels of encapsulation and mutability.
 *
 * <h2>Example</h2>
 * <p>In this example, we are combining InheritableEvent with {@link RecordEvent} and using the Java module system to
 * make the record's constructor effectively internal. This allows for a stronger guarantee of correctness for listeners
 * as it's not possible for someone else to create and post an event of this type at the wrong time or with invalid
 * data.</p>
 * {@snippet :
 * import net.minecraftforge.eventbus.api.bus.EventBus;
 * import net.minecraftforge.eventbus.api.event.InheritableEvent;
 *
 * // in a publicly exported package (e.g. com.example.events.api)
 * public sealed interface PlayerJumpEvent extends InheritableEvent permits PlayerJumpEventImpl {
 *     EventBus<PlayerJumpEvent> BUS = EventBus.create(PlayerJumpEvent.class);
 *
 *     Player player();
 *     int jumpHeight();
 * }
 *
 * // in an internal package (e.g. com.example.events.internal)
 * public record PlayerJumpEventImpl(Player player, int jumpHeight) implements PlayerJumpEvent {
 *     public static final EventBus<PlayerJumpEvent> BUS = EventBus.create(PlayerJumpEvent.class);
 * }
 *
 * // snippet of the module-info.java
 * module com.example.events {
 *     exports com.example.events.api;
 *     // (note that the internal package is not exported)
 * }
 * }
 * <p>Consumers can add listeners to {@code PlayerJumpEvent.BUS} and would receive events posted to
 * {@code PlayerJumpEventImpl.BUS}.</p>
 *
 * <p>Another example would be replacing an abstract class hierarchy with a sealed interface and records implementing
 * them, like so:</p>
 * {@snippet :
 * import net.minecraftforge.eventbus.api.bus.EventBus;
 * import net.minecraftforge.eventbus.api.event.InheritableEvent;
 *
 * public sealed interface JumpEvent extends InheritableEvent {
 *     EventBus<JumpEvent> BUS = EventBus.create(JumpEvent.class);
 *
 *     int jumpHeight();
 *
 *     record PlayerJumpEvent(Player player, int jumpHeight) implements JumpEvent {
 *          public static final EventBus<PlayerJumpEvent> BUS = EventBus.create(PlayerJumpEvent.class);
 *     }
 *
 *     record AnimalJumpEvent(Animal animal, int jumpHeight) implements JumpEvent {
 *          public static final EventBus<AnimalJumpEvent> BUS = EventBus.create(AnimalJumpEvent.class);
 *     }
 * }
 * }
 * <p>This allows consumers to add listeners to {@code JumpEvent} if they only care about the jump height, which is
 * functionally equivalent to adding a distinct listener to all children, but more performant and less error-prone
 * (if a new subclass is added later, it would be accounted for at runtime).</p>
 */
public non-sealed interface InheritableEvent extends Event {}
