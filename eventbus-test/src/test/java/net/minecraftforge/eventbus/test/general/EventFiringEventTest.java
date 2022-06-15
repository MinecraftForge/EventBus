package net.minecraftforge.eventbus.test.general;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.test.ITestHandler;

public class EventFiringEventTest implements ITestHandler {
    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(Event1.class);
        validator.accept(AbstractEvent.class);
        validator.accept(AbstractEvent.Event2.class);

        IEventBus bus = builder.get().build();
        AtomicBoolean handled1 = new AtomicBoolean(false);
        AtomicBoolean handled2 = new AtomicBoolean(false);
        bus.addListener(EventPriority.NORMAL, false, Event1.class, (event1) -> {
            bus.post(new AbstractEvent.Event2());
            handled1.set(true);
        });
        bus.addListener(EventPriority.NORMAL, false, AbstractEvent.Event2.class, (event2) -> {
            handled2.set(true);
        });

        bus.post(new Event1());

        assertTrue(handled1.get(), "handled Event1");
        assertTrue(handled2.get(), "handled Event2");
    }

    public static class Event1 extends Event {}

    public static abstract class AbstractEvent extends Event {
        public static class Event2 extends AbstractEvent {}
    }
}
