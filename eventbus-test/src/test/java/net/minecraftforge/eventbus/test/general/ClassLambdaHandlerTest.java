package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.eventbus.test.ITestHandler;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class ClassLambdaHandlerTest implements ITestHandler {
    boolean hit;

    @Override
    public void before(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(SubEvent.class);
        validator.accept(CancellableEvent.class);
        hit = false;
    }

    public void consumeEvent(Event e) { hit = true; }
    public void consumeSubEvent(SubEvent e) { hit = true; }

    public static class Basic extends ClassLambdaHandlerTest
    {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            final IEventBus iEventBus = builder.get().build();
            // Inline
            iEventBus.addListener(Event.class, (e)-> hit = true);
            iEventBus.post(new Event());
            assertTrue(hit, "Inline Lambda was not called");
            hit = false;
            // Method reference
            iEventBus.addListener(Event.class, this::consumeEvent);
            iEventBus.post(new Event());
            assertTrue(hit, "Method reference was not called");
            hit = false;
        }
    }

    public static class SubClassEvent extends ClassLambdaHandlerTest
    {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            final IEventBus iEventBus = builder.get().build();
            // Inline
            iEventBus.addListener(SubEvent.class, (e) -> hit = true);
            iEventBus.post(new SubEvent());
            assertTrue(hit, "Inline was not called");
            hit = false;
            iEventBus.post(new Event());
            assertTrue(!hit, "Inline was called on root event");
            // Method Reference
            iEventBus.addListener(SubEvent.class, this::consumeSubEvent);
            iEventBus.post(new SubEvent());
            assertTrue(hit, "Method reference was not called");
            hit = false;
            iEventBus.post(new Event());
            assertTrue(!hit, "Method reference was called on root event");
        }
    }

    public static class SubEvent extends Event {}

    @Cancelable
    public static class CancellableEvent extends Event {}
}
