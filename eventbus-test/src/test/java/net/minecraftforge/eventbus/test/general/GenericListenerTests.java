package net.minecraftforge.eventbus.test.general;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.test.ITestHandler;
import org.junit.jupiter.api.Assertions;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import static org.junit.jupiter.api.Assertions.*;

public abstract class GenericListenerTests implements ITestHandler {
    boolean genericEventHandled = false;
    protected void handleGenericEvent(GenericEvent<List<String>> evt) {
        genericEventHandled = true;
    }

    public static class Basic extends GenericListenerTests {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            IEventBus bus = builder.get().build();
            bus.addGenericListener(List.class, this::handleGenericEvent);
            bus.post(new GenericEvent<List<String>>() {
                public Type getGenericType() {
                    return List.class;
                }
            });
            assertTrue(genericEventHandled);
        }
    }

    public static class IncorrectRegistration extends GenericListenerTests {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            IEventBus bus = builder.get().build();
            Assertions.assertThrows(IllegalArgumentException.class, () -> bus.addListener(this::handleGenericEvent));
        }
    }

    public static class Wildcard extends GenericListenerTests {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            IEventBus bus = builder.get().build();
            var hdl = new GenericHandler();
            bus.register(hdl);
            bus.post(new GenericEvent<>());
            Assertions.assertTrue(hdl.hit, "Generic event handler was not called");
        }
    }

    public static class GenericHandler {
        boolean hit;
        @SubscribeEvent
        public void handleWildcardGeneric(GenericEvent<?> ge) {
            hit = true;
        }
    }
}
