package net.minecraftforge.eventbus.test;

import cpw.mods.bootstraplauncher.BootstrapLauncher;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.testjar.DummyEvent;
import net.minecraftforge.eventbus.testjar.EventBusTestClass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class BadEventDispatcherTest {
    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void testBadEvent() throws IOException, URISyntaxException {
        System.setProperty("legacyClassPath", "");
//        System.setProperty("test.harness.plugin", "build/classes/java/main");
        System.setProperty("test.harness.game", "build/classes/java/testJars,build/classes/java/test");
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.test.BadEventDispatcherTest$TestCallback");
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
                assertThrows(RuntimeException.class, ()->bus.post(new DummyEvent.BadEvent()));
            };
        }
    }

}
