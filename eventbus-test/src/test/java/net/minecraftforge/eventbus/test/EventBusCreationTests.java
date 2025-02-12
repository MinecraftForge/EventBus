/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.event.characteristic.MonitorAware;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

public class EventBusCreationTests {
    /**
     * Tests that only records can implement {@link RecordEvent}.
     */
    @Test
    public void testRecordEventValidation() {
        final class ClassTestEvent implements RecordEvent {}
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> EventBus.create(ClassTestEvent.class),
                "EventBus creation should require that only records can implement RecordEvent"
        );

        record RecordTestEvent() implements RecordEvent {}
        Assertions.assertDoesNotThrow(
                () -> EventBus.create(RecordTestEvent.class),
                "EventBus creation should work for record events"
        );
    }

    /**
     * Tests that base types are enforced when creating an {@link EventBus} with a custom {@link BusGroup}.
     */
    @Test
    public void testBaseTypeValidation() {
        AtomicReference<BusGroup> testGroup = new AtomicReference<>();
        Assertions.assertDoesNotThrow(
                () -> testGroup.set(BusGroup.create("EventBusCreationTests.testBaseTypeValidation", RecordEvent.class)),
                "BusGroup creation should work with a base type"
        );

        final class MutableClassTestEvent extends MutableEvent {}
        record RecordTestEvent() implements RecordEvent {}
        Assertions.assertDoesNotThrow(
                () -> EventBus.create(testGroup.get(), RecordTestEvent.class),
                "EventBus creation should work when the event is a subclass of the base type"
        );
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> EventBus.create(testGroup.get(), MutableClassTestEvent.class),
                "EventBus creation should fail when the event is not a subclass of the base type"
        );
    }

    /**
     * Tests that various event types can be created with an {@link EventBus}.
     */
    @Test
    public void testEventCreation() {
        record RecordTestEvent() implements RecordEvent {}
        Assertions.assertDoesNotThrow(
                () -> EventBus.create(RecordTestEvent.class),
                "EventBus creation should work for record events"
        );

        final class MutableClassTestEvent extends MutableEvent {}
        Assertions.assertDoesNotThrow(
                () -> EventBus.create(MutableClassTestEvent.class),
                "EventBus creation should work for mutable events"
        );

        interface InheritableTestEvent extends InheritableEvent {}
        Assertions.assertDoesNotThrow(
                () -> EventBus.create(InheritableTestEvent.class),
                "EventBus creation should work for inheritable events"
        );

        interface Child extends InheritableTestEvent {}
        Assertions.assertDoesNotThrow(
                () -> EventBus.create(Child.class),
                "EventBus creation should consider the whole inheritance tree when determining if the class/interface/record is some kind of Event"
        );
    }

    /**
     * Tests that only cancellable events can be created with a {@link CancellableEventBus}.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testCancellableEventValidation() {
        record TestEvent() implements RecordEvent {}
        record CancellableTestEvent() implements Cancellable, RecordEvent {}

        Assertions.assertDoesNotThrow(
                () -> CancellableEventBus.create(CancellableTestEvent.class),
                "CancellableEventBus creation should work for cancellable events"
        );
        Assertions.assertThrows(
                Exception.class,
                () -> CancellableEventBus.create((Class) TestEvent.class),
                "CancellableEventBus creation should fail for non-cancellable events"
        );
    }

    /**
     * Tests that {@link MonitorAware} can only be applied to {@link MutableEvent}s.
     */
    @Test
    public void testMonitorAwareEventValidation() {
        record TestEvent() implements RecordEvent, MonitorAware {}

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> EventBus.create(TestEvent.class),
                "EventBus creation should fail for events that implement MonitorAware but are not MutableEvent"
        );
    }
}
