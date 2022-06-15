package net.minecraftforge.eventbus.test.general;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.test.ITestHandler;

import static org.junit.jupiter.api.Assertions.*;

public class NonPublicEventHandler implements ITestHandler {
    private final boolean hasTransformer;
    private static boolean hit = false;

    public NonPublicEventHandler(boolean hasTransformer) {
        this.hasTransformer = hasTransformer;
    }

    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        var bus = builder.get().build();
        assertDoesNotThrow(() -> bus.register(new PUBLIC()));
        testCall(bus, true, "PUBLIC");

        if (hasTransformer) {
            assertDoesNotThrow(() -> bus.register(new PROTECTED()));
            testCall(bus, true, "PROTECTED");
            assertDoesNotThrow(() -> bus.register(new DEFAULT()));
            testCall(bus, true, "DEFAULT");
            //assertDoesNotThrow(() -> bus.register(new PRIVATE()));
            //testCall(bus, true, "PRIVATE");
        } else {
            assertThrows(IllegalArgumentException.class, () -> bus.register(new PROTECTED()));
            assertThrows(IllegalArgumentException.class, () -> bus.register(new DEFAULT()));
            //assertThrows(IllegalArgumentException.class, () -> bus.register(new PRIVATE()));
        }
    }

    private void testCall(IEventBus bus, boolean expected, String name) {
        hit = false;
        bus.post(new Event());
        assertEquals(expected, hit, name + " did not behave correctly");
    }

    public static class PUBLIC {
        @SubscribeEvent
        public void handler(Event e) {
            hit = true;
        }
    }
    /* This will error in our transformer, and there isnt a way to test that.
    public static class PRIVATE {
        @SubscribeEvent
        private void handler(Event e) {
            hit = true;
        }
    }
    */
    public static class PROTECTED {
        @SubscribeEvent
        protected void handler(Event e) {
            hit = true;
        }
    }
    public static class DEFAULT {
        @SubscribeEvent
        void handler(Event e) {
            hit = true;
        }
    }
}
