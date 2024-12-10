/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.test.general;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.test.ITestHandler;

import static org.junit.jupiter.api.Assertions.*;

public class InterfaceEventHandler implements ITestHandler {
    private static boolean hit = false;

    public InterfaceEventHandler(boolean hasTransformer) {
    }

    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        var bus = builder.get().build();
        assertDoesNotThrow(() -> bus.register(STATIC.class));
        testCall(bus, true, "STATIC");
        assertDoesNotThrow(() -> bus.register(new INSTANCE() {}));
        testCall(bus, true, "STATIC");
    }

    private void testCall(IEventBus bus, boolean expected, String name) {
        hit = false;
        bus.post(new Event());
        assertEquals(expected, hit, name + " did not behave correctly");
    }

    public interface STATIC {
        @SubscribeEvent
        static void handler(Event e) {
            hit = true;
        }
    }
    
    public interface INSTANCE {
        @SubscribeEvent
        default void handler(Event e) {
            hit = true;
        }
    }
}
