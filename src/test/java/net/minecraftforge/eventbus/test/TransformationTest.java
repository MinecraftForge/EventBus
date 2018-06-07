package net.minecraftforge.eventbus.test;

import cpw.mods.modlauncher.Launcher;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.message.Message;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.powermock.reflect.internal.WhiteboxImpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransformationTest {

    private Object eventBus;

    @BeforeAll
    public static void setup() {
        Configurator.setRootLevel(Level.DEBUG);
    }

    boolean calledback;
    Class<?> transformedClass;

    @Test
    public void testTestingLaunchHandler() throws IOException, URISyntaxException {
        final String l4j2 = Message.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        final String asm = Type.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        System.setProperty("test.harness", "build/classes/java/testJars,build/classes/java/main,"+l4j2+","+asm);
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.test.TransformationTest$TestCallback");
        calledback = false;
        TestCallback.callable = () -> {
            calledback = true;
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            final Class<?> aClass = Class.forName("net.minecraftforge.eventbus.api.IEventBus", true, contextClassLoader);
            eventBus = WhiteboxImpl.invokeMethod(aClass, "create");
            transformedClass = Class.forName("net.minecraftforge.eventbus.testjar.EventBusTestClass", true, contextClassLoader);
            WhiteboxImpl.invokeMethod(eventBus, "register", transformedClass.newInstance());
            return null;
        };
        Launcher.main("--version", "1.0", "--launchTarget", "testharness");
        assertTrue(calledback, "We got called back");
        assertAll(
                ()-> {}
        );
    }

    private String toBinary(int num)
    {
        return String.format("%16s", Integer.toBinaryString(num)).replace(' ', '0');
    }

    public static class TestCallback {
        private static Callable<Void> callable;
        public static Callable<Void> supplier() {
            return callable;
        }
    }
}
