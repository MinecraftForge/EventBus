package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.test.ITestHandler;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public abstract class LambdaHandlerTest implements ITestHandler {
    boolean hit;

    @Override
    public void before(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(SubEvent.class);
        validator.accept(CancellableEvent.class);
        hit = false;
    }

    public void consumeEvent(Event e) { hit = true; }
    public void consumeSubEvent(SubEvent e) { hit = true; }

    public static class Basic extends LambdaHandlerTest {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            final IEventBus iEventBus = builder.get().build();
            // Inline
            iEventBus.addListener((Event e)-> hit = true);
            iEventBus.post(new Event());
            assertTrue(hit, "Inline Lambda was not called");
            hit = false;
            // Method reference
            iEventBus.addListener(this::consumeEvent);
            iEventBus.post(new Event());
            assertTrue(hit, "Method reference was not called");
            hit = false;
        }
    }

    public static class SubClassEvent extends LambdaHandlerTest {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            final IEventBus iEventBus = builder.get().build();
            // Inline
            iEventBus.addListener((SubEvent e) -> hit = true);
            iEventBus.post(new SubEvent());
            assertTrue(hit, "Inline was not called");
            hit = false;
            iEventBus.post(new Event());
            assertTrue(!hit, "Inline was called on root event");
            // Method Reference
            iEventBus.addListener(this::consumeSubEvent);
            iEventBus.post(new SubEvent());
            assertTrue(hit, "Method reference was not called");
            hit = false;
            iEventBus.post(new Event());
            assertTrue(!hit, "Method reference was called on root event");
        }
    }

    public static class Generics extends LambdaHandlerTest {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            // pathological test because you can't derive the lambda types in all cases...
            // I don't quite understand what this is testing, Care to enlighten me cpw? --Lex
            IEventBus bus = builder.get().build();
            registerSomeGodDamnWrapper(bus, CancellableEvent.class, this::subEventFunction);
            final CancellableEvent event = new CancellableEvent();
            bus.post(event);
            assertTrue(event.isCanceled(), "Event got cancelled");
            final SubEvent subevent = new SubEvent();
            bus.post(subevent);
        }

        private boolean subEventFunction(final CancellableEvent event) {
            return event instanceof CancellableEvent;
        }
    }

    public <T extends Event> void registerSomeGodDamnWrapper(IEventBus bus, Class<T> tClass, Function<T, Boolean> func) {
        bus.addListener(EventPriority.NORMAL, false, tClass, (T event) -> {
            if (func.apply(event)) {
                event.setCanceled(true);
            }
        });
    }

    public static class SubEvent extends Event {}

    @Cancelable
    public static class CancellableEvent extends Event {}
}
