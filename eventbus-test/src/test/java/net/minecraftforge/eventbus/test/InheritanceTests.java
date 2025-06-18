/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.internal.EventBusImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InheritanceTests {
    sealed interface AbstractSuperEvent extends InheritableEvent permits ConcreteSuperEvent {
        EventBus<AbstractSuperEvent> BUS = EventBus.create(AbstractSuperEvent.class);
    }

    static sealed class ConcreteSuperEvent extends MutableEvent implements AbstractSuperEvent permits AbstractSubEvent {
        static final EventBus<ConcreteSuperEvent> BUS = EventBus.create(ConcreteSuperEvent.class);
    }

    static sealed abstract class AbstractSubEvent extends ConcreteSuperEvent permits ConcreteSubEvent {
        static final EventBus<AbstractSubEvent> BUS = EventBus.create(AbstractSubEvent.class);
    }

    static final class ConcreteSubEvent extends AbstractSubEvent {
        static final EventBus<ConcreteSubEvent> BUS = EventBus.create(ConcreteSubEvent.class);
    }

    /**
     * Tests that listeners (un)registered to the super class are also (un)registered for its subclasses.
     */
    @Test
    public void testListenerRegistrationInheritance() {
        EventBusImpl<?> abstractSuperEventBus = (EventBusImpl<?>) AbstractSuperEvent.BUS;
        EventBusImpl<?> concreteSuperEventBus = (EventBusImpl<?>) ConcreteSuperEvent.BUS;
        EventBusImpl<?> abstractSubEventBus = (EventBusImpl<?>) AbstractSubEvent.BUS;
        EventBusImpl<?> concreteSubEventBus = (EventBusImpl<?>) ConcreteSubEvent.BUS;

        abstractSuperEventBus.backingList().clear();
        concreteSuperEventBus.backingList().clear();
        abstractSubEventBus.backingList().clear();
        concreteSubEventBus.backingList().clear();

        var listener = AbstractSuperEvent.BUS.addListener(event -> {});

        Assertions.assertEquals(1, abstractSuperEventBus.backingList().size());
        Assertions.assertEquals(1, concreteSuperEventBus.backingList().size());
        Assertions.assertEquals(1, abstractSubEventBus.backingList().size());
        Assertions.assertEquals(1, concreteSubEventBus.backingList().size());

        AbstractSuperEvent.BUS.removeListener(listener);

        Assertions.assertTrue(abstractSuperEventBus.backingList().isEmpty());
        Assertions.assertTrue(concreteSuperEventBus.backingList().isEmpty());
        Assertions.assertTrue(abstractSubEventBus.backingList().isEmpty());
        Assertions.assertTrue(concreteSubEventBus.backingList().isEmpty());
    }

    /**
     * Tests that listeners registered to the superclass are called for each subclass' post.
     */
    @Test
    public void testListenerCallInheritance() {
        EventBusImpl<?> abstractSuperEventBus = (EventBusImpl<?>) AbstractSuperEvent.BUS;
        EventBusImpl<?> concreteSuperEventBus = (EventBusImpl<?>) ConcreteSuperEvent.BUS;
        EventBusImpl<?> abstractSubEventBus = (EventBusImpl<?>) AbstractSubEvent.BUS;
        EventBusImpl<?> concreteSubEventBus = (EventBusImpl<?>) ConcreteSubEvent.BUS;

        abstractSuperEventBus.backingList().clear();
        concreteSuperEventBus.backingList().clear();
        abstractSubEventBus.backingList().clear();
        concreteSubEventBus.backingList().clear();

        var counter = new AtomicInteger();
        var listener = AbstractSuperEvent.BUS.addListener(event -> counter.incrementAndGet());

        Assertions.assertEquals(0, counter.get());
        AbstractSuperEvent.BUS.post(new ConcreteSuperEvent());
        Assertions.assertEquals(1, counter.get());
        ConcreteSuperEvent.BUS.post(new ConcreteSuperEvent());
        Assertions.assertEquals(2, counter.get());
        AbstractSubEvent.BUS.post(new ConcreteSubEvent());
        Assertions.assertEquals(3, counter.get());
        ConcreteSubEvent.BUS.post(new ConcreteSubEvent());
        Assertions.assertEquals(4, counter.get());

        AbstractSuperEvent.BUS.removeListener(listener);
    }

    /**
     * Tests that all listeners across the inheritance hierarchy are called for each subclass' fire.
     */
    @Test
    public void testListenerCallInheritance2() {
        EventBusImpl<?> abstractSuperEventBus = (EventBusImpl<?>) AbstractSuperEvent.BUS;
        EventBusImpl<?> concreteSuperEventBus = (EventBusImpl<?>) ConcreteSuperEvent.BUS;
        EventBusImpl<?> abstractSubEventBus = (EventBusImpl<?>) AbstractSubEvent.BUS;
        EventBusImpl<?> concreteSubEventBus = (EventBusImpl<?>) ConcreteSubEvent.BUS;

        abstractSuperEventBus.backingList().clear();
        concreteSuperEventBus.backingList().clear();
        abstractSubEventBus.backingList().clear();
        concreteSubEventBus.backingList().clear();

        var abstractSuperEventHandled = new AtomicBoolean();
        var concreteSuperEventHandled = new AtomicBoolean();
        var abstractSubEventHandled = new AtomicBoolean();
        var concreteSubEventHandled = new AtomicBoolean();

        var a = abstractSuperEventBus.addListener(event -> abstractSuperEventHandled.set(true));
        var b = concreteSuperEventBus.addListener(event -> concreteSuperEventHandled.set(true));
        var c = abstractSubEventBus.addListener(event -> abstractSubEventHandled.set(true));
        var d = concreteSubEventBus.addListener(event -> concreteSubEventHandled.set(true));

        Assertions.assertFalse(abstractSuperEventHandled.get(), "AbstractSuperEvent should not be handled yet");
        Assertions.assertFalse(concreteSuperEventHandled.get(), "ConcreteSuperEvent should not be handled yet");
        Assertions.assertFalse(abstractSubEventHandled.get(), "AbstractSubEvent should not be handled yet");
        Assertions.assertFalse(concreteSubEventHandled.get(), "ConcreteSubEvent should not be handled yet");

        ConcreteSubEvent.BUS.post(new ConcreteSubEvent());

        Assertions.assertTrue(abstractSuperEventHandled.get(), "AbstractSuperEvent should be handled");
        Assertions.assertTrue(concreteSuperEventHandled.get(), "ConcreteSuperEvent should be handled");
        Assertions.assertTrue(abstractSubEventHandled.get(), "AbstractSubEvent should be handled");
        Assertions.assertTrue(concreteSubEventHandled.get(), "ConcreteSubEvent should be handled");

        abstractSuperEventBus.removeListener(a);
        concreteSuperEventBus.removeListener(b);
        abstractSubEventBus.removeListener(c);
        concreteSubEventBus.removeListener(d);
    }

    /**
     * Tests that a parent listener can handle a child event.
     */
    @Test
    public void testParentListenerGetsChildEvent() {
        class SuperEvent extends MutableEvent implements InheritableEvent {
            static final EventBus<SuperEvent> BUS = EventBus.create(SuperEvent.class);
        }
        final class SubEvent extends SuperEvent {
            static final EventBus<SubEvent> BUS = EventBus.create(SubEvent.class);
        }

        var superEventHandled = new AtomicBoolean();
        var subEventHandled = new AtomicBoolean();
        var listener = SuperEvent.BUS.addListener(event -> {
            var eventClass = event.getClass();
            if (eventClass == SuperEvent.class) {
                superEventHandled.set(true);
            } else if (eventClass == SubEvent.class) {
                subEventHandled.set(true);
            }
        });

        Assertions.assertFalse(superEventHandled.get(), "SuperEvent should not be handled yet");
        Assertions.assertFalse(subEventHandled.get(), "SubEvent should not be handled yet");

        SuperEvent.BUS.post(new SuperEvent());
        SubEvent.BUS.post(new SubEvent());

        Assertions.assertTrue(superEventHandled.get(), "SuperEvent should be handled");
        Assertions.assertTrue(subEventHandled.get(), "SubEvent should be handled");

        SuperEvent.BUS.removeListener(listener);
    }

    /**
     * Tests that listener inheritance fails when the event doesn't implement {@link InheritableEvent}.
     * <p>Inheritance should only work when the event implements {@link InheritableEvent}.</p>
     */
    @Test
    public void testListenerCallInheritanceFailsWithoutOptIn() {
        class SuperEvent extends MutableEvent {
            static final EventBus<SuperEvent> BUS = EventBus.create(SuperEvent.class);
        }
        final class SubEvent extends SuperEvent {
            static final EventBus<SubEvent> BUS = EventBus.create(SubEvent.class);
        }

        var superEventHandled = new AtomicBoolean();
        var subEventHandled = new AtomicBoolean();
        var listener = SuperEvent.BUS.addListener(event -> {
            var eventClass = event.getClass();
            if (eventClass == SuperEvent.class) {
                superEventHandled.set(true);
            } else if (eventClass == SubEvent.class) {
                subEventHandled.set(true);
            }
        });

        Assertions.assertFalse(superEventHandled.get(), "SuperEvent should not be handled yet");
        Assertions.assertFalse(subEventHandled.get(), "SubEvent should not be handled yet");

        SuperEvent.BUS.post(new SuperEvent());
        SubEvent.BUS.post(new SubEvent());

        Assertions.assertTrue(superEventHandled.get(), "SuperEvent should be handled");
        Assertions.assertFalse(subEventHandled.get(), "SubEvent should not be handled as SuperEvent doesn't implement InheritableEvent");

        SuperEvent.BUS.removeListener(listener);
    }

    /**
     * Tests that listener inheritance works when a non-{@link Cancellable} parent event implements {@link InheritableEvent}
     * and a {@link Cancellable} child event inherits from the parent.
     */
    @Test
    public void testListenerCallInheritanceWithCancellable() {
        class SuperEvent implements InheritableEvent {
            static final EventBus<SuperEvent> BUS = EventBus.create(SuperEvent.class);
        }
        final class SubEvent extends SuperEvent implements Cancellable {
            static final EventBus<SubEvent> BUS = EventBus.create(SubEvent.class);
        }

        var handled = new AtomicBoolean();
        var listener = SuperEvent.BUS.addListener(event -> handled.set(true));

        Assertions.assertFalse(handled.get(), "SuperEvent should not be handled yet");

        Assertions.assertDoesNotThrow(() -> SubEvent.BUS.post(new SubEvent()));

        Assertions.assertTrue(handled.get(), "SuperEvent should be handled, even though SubEvent is cancellable");

        SuperEvent.BUS.removeListener(listener);
    }
}
