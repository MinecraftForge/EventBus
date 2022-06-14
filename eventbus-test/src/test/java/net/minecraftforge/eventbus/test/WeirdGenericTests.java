package net.minecraftforge.eventbus.test;

import java.lang.reflect.Type;
import java.util.List;

import cpw.mods.bootstraplauncher.BootstrapLauncher;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WeirdGenericTests {

    boolean genericEventHandled = false;

    @Test
    public void testGenericListener() {
        IEventBus bus = BusBuilder.builder().build();
        bus.addGenericListener(List.class, this::handleGenericEvent);
        bus.post(new GenericEvent<List<String>>() {
            public Type getGenericType() {
                return List.class;
            }
        });
        Assertions.assertTrue(genericEventHandled);
    }

    @Test
    public void testGenericListenerRegisteredIncorrectly() {
        IEventBus bus = BusBuilder.builder().build();
        Assertions.assertThrows(IllegalArgumentException.class, () -> bus.addListener(this::handleGenericEvent));
    }

    private void handleGenericEvent(GenericEvent<List<String>> evt) {
        genericEventHandled = true;
    }

    @Test
    public void testNoFilterRegisterWithWildcard() throws Exception {
        System.setProperty("legacyClassPath", "");
        System.setProperty("test.harness.game", MockTransformerService.getBasePath());
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.test.WeirdGenericTests$TestCallback");
        BootstrapLauncher.main("--version", "1.0", "--launchTarget", "testharness");
        assertEquals("true", System.getProperty("testCalledSuccessfully"), "We got called back!");
        System.out.println("We did it chat!");
    }

    public static class TestCallback {
        public static ServiceRunner supplier() {
            return () -> {
                IEventBus bus = BusBuilder.builder().build();
                var hdl = new GenericHandler();
                bus.register(hdl);
                bus.post(new GenericEvent<>());
                Assertions.assertTrue(hdl.hit, "Hit the event handler");
                System.setProperty("testCalledSuccessfully", "true");
            };
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
