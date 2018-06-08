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

public class GoodEventDispatcherTest {

    private Object eventBus;
    private boolean gotException;

    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }

    boolean calledback;
    Class<?> transformedClass;

    @Test
    public void testGoodEvents() throws IOException, URISyntaxException {
        System.setProperty("test.harness", "build/classes/java/testJars,build/classes/java/main");
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.test.GoodEventDispatcherTest$TestCallback");
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
            Object evt = Class.forName("net.minecraftforge.eventbus.testjar.DummyEvent$GoodEvent", true, contextClassLoader).newInstance();
            WhiteboxImpl.invokeMethod(eventBus, "post",evt);
            return null;
        };
        Launcher.main("--version", "1.0", "--launchTarget", "testharness");
        assertTrue(calledback, "We got called back");
        assertAll(
                ()-> assertTrue(WhiteboxImpl.getField(transformedClass, "HIT1").getBoolean(null), "HIT1 was hit"),
                ()-> assertTrue(WhiteboxImpl.getField(transformedClass, "HIT2").getBoolean(null), "HIT2 was hit")
        );
    }

    public static class TestCallback {
        private static Callable<Void> callable;
        public static Callable<Void> supplier() {
            return callable;
        }
    }
}
