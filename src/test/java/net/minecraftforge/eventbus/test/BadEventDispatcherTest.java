package net.minecraftforge.eventbus.test;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ITransformingClassLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.internal.WhiteboxImpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BadEventDispatcherTest {

    private Object eventBus;
    private boolean gotException;

    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }

    boolean calledback;
    Class<?> transformedClass;

    @Test
    public void testBadEvent() throws IOException, URISyntaxException {
        System.setProperty("test.harness", "build/classes/java/testJars,build/classes/java/main");
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.test.BadEventDispatcherTest$TestCallback");
        calledback = false;
        TestCallback.callable = () -> {
            calledback = true;
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            ((ITransformingClassLoader)contextClassLoader).
                    addTargetPackageFilter(s->!(
                            s.startsWith("net.minecraftforge.eventbus.") &&
                            !s.startsWith("net.minecraftforge.eventbus.test")));
            final Class<?> aClass = Class.forName("net.minecraftforge.eventbus.api.IEventBus", true, contextClassLoader);
            eventBus = WhiteboxImpl.invokeMethod(aClass, "create");
            transformedClass = Class.forName("net.minecraftforge.eventbus.testjar.EventBusTestClass", true, contextClassLoader);
            WhiteboxImpl.invokeMethod(eventBus, "register", transformedClass.newInstance());
            Object evt = Class.forName("net.minecraftforge.eventbus.testjar.DummyEvent$BadEvent", true, contextClassLoader).newInstance();
            try {
                WhiteboxImpl.invokeMethod(eventBus, "post", evt);
            } catch (RuntimeException ex) {
                gotException = true;
            }
            return null;
        };
        Launcher.main("--version", "1.0", "--launchTarget", "testharness");
        assertTrue(calledback, "We got called back");
        assertTrue(gotException, "We got the exception");
    }

    public static class TestCallback {
        private static Callable<Void> callable;
        public static Callable<Void> supplier() {
            return callable;
        }
    }
}
