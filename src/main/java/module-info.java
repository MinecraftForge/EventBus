/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
import org.jspecify.annotations.NullMarked;

/**
 * EventBus is a flexible, high-performance, thread-safe subscriber-publisher framework designed with modern Java in
 * mind.
 *
 * <h2>Overview</h2>
 * <p>The core functionality of EventBus is to provide a simple and efficient way to handle
 * {@linkplain net.minecraftforge.eventbus.api.event events} in a decoupled manner.</p>
 * <p>Each event may have one or more {@linkplain net.minecraftforge.eventbus.api.bus.EventBus buses} associated with
 * it, which are responsible for managing {@linkplain net.minecraftforge.eventbus.api.listener.EventListener listeners}
 * and dispatching instances of the event object to them. To maximise performance, the underlying implementation is
 * tailored on the fly based on the event's type,
 * {@linkplain net.minecraftforge.eventbus.api.event.characteristic characteristics}, inheritance chain and the number
 * and type of listeners registered to the bus.</p>
 *
 * <h2>Example</h2>
 * <p>Here is a basic usage example of EventBus in action:</p>
 * {@snippet :
 * import net.minecraftforge.eventbus.api.event.RecordEvent;
 * import net.minecraftforge.eventbus.api.bus.EventBus;
 *
 * // Define an event and a bus for it
 * record PlayerLoggedInEvent(String username) implements RecordEvent {
 *     public static final EventBus<PlayerLoggedInEvent> BUS = EventBus.create(PlayerLoggedInEvent.class);
 * }
 *
 * // Register an event listener
 * PlayerLoggedInEvent.BUS.addListener(event -> System.out.println("Player logged in: " + event.username()));
 *
 * // Post an event to the registered listeners
 * PlayerLoggedInEvent.BUS.post(new PlayerLoggedInEvent("Paint_Ninja"));
 *}
 * <p>There are several more example usages within the JavaDocs of the different packages and classes in this API
 * module. These examples are non-exhaustive, but provide a good basis on which to build your usage of EventBus.</p>
 *
 * <h2>Nullability</h2>
 * <p>The entirety of EventBus' API is {@link org.jspecify.annotations.NullMarked @NullMarked} and compliant with the
 * <a href="https://jspecify.dev/">jSpecify specification</a>. This means that everything is
 * {@linkplain org.jspecify.annotations.NonNull non-null} by default unless otherwise specified.</p>
 * <p>Attempting to pass a {@code null} value to a method param that isn't explicitly marked as
 * {@link org.jspecify.annotations.Nullable @Nullable} is an <i>unsupported operation</i> and won't be considered a
 * breaking change if a future version throws an exception in such cases when it didn't before.</p>
 */
@NullMarked
module net.minecraftforge.eventbus {
    requires java.logging;
    requires org.jspecify;

    exports net.minecraftforge.eventbus.api.bus;
    exports net.minecraftforge.eventbus.api.event;
    exports net.minecraftforge.eventbus.api.event.characteristic;
    exports net.minecraftforge.eventbus.api.listener;

    exports net.minecraftforge.eventbus.internal to net.minecraftforge.eventbus.test;
    opens net.minecraftforge.eventbus.internal to net.minecraftforge.eventbus.test;
}
