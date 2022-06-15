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

public class ParentHandlersGetInvokedTest implements ITestHandler {
    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(SuperEvent.class);
        validator.accept(SubEvent.class);

        IEventBus bus = builder.get().build();
        AtomicBoolean superEventHandled = new AtomicBoolean(false);
        AtomicBoolean subEventHandled = new AtomicBoolean(false);
        bus.addListener(EventPriority.NORMAL, false, SuperEvent.class, (event) -> {
            Class<? extends SuperEvent> eventClass = event.getClass();
            if (eventClass == SuperEvent.class) {
                superEventHandled.set(true);
            } else if (eventClass == SubEvent.class) {
                subEventHandled.set(true);
            }
        });

        bus.post(new SuperEvent());
        bus.post(new SubEvent());

        assertTrue(superEventHandled.get(), "Handler was not invoked for SuperEvent");
        assertTrue(subEventHandled.get(), "Handler was not invoked for SubEvent");
    }

    public static class SuperEvent extends Event {}
    public static class SubEvent extends SuperEvent {}
}
