package net.minecraftforge.eventbus.test;

import cpw.mods.bootstraplauncher.BootstrapLauncher;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.testjar.DummyEvent;
import net.minecraftforge.eventbus.testjar.EventBusTestClass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class GoodEventDispatcherTest {
    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void testGoodEvents() throws IOException, URISyntaxException {
        System.setProperty("legacyClassPath", "");
//        System.setProperty("test.harness.plugin", "build/classes/java/main");
        System.setProperty("test.harness.game", "build/classes/java/testJars,build/classes/java/test");
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.test.GoodEventDispatcherTest$TestCallback");
        BootstrapLauncher.main("--version", "1.0", "--launchTarget", "testharness");
        assertEquals("true", System.getProperty("testCalledSuccessfully"), "We got called back!");
        System.out.println("We did it chat!");
    }

    public static class TestCallback {

        public static ServiceRunner supplier() {
            return () -> {
                System.setProperty("testCalledSuccessfully", "true");
                var bus = BusBuilder.builder().build();
                var listener = new EventBusTestClass();
                bus.register(listener);
                bus.post(new DummyEvent.GoodEvent());
                assertAll(
                        () -> assertTrue(listener.HIT1, "HIT1 was hit"),
                        () -> assertTrue(listener.HIT2, "HIT2 was hit")
                );
            };
        }
    }
}
