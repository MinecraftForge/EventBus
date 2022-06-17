package net.minecraftforge.eventbus.test;

import cpw.mods.bootstraplauncher.BootstrapLauncher;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.eventbus.api.BusBuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class TestModLauncherBase {
    private static final String CLASS_NAME = "test.modlauncher.class";
    private static final String METHOD_NAME = "test.modlauncher.method";

    void validate(Class<?> clazz) {
        // We expect transformers to run, so make sure LISTENER_LIST exists, as it's the best indicator
        assertTrue(Whitebox.hasField(clazz, "LISTENER_LIST"), "EventSubclassTransformer did not run on " + clazz.getName());
    }

    BusBuilder builder() {
        return BusBuilder.builder().useModLauncher();
    }

    @BeforeEach
    public void setup() {
        System.clearProperty(CLASS_NAME);
        System.clearProperty(METHOD_NAME);
    }

    @AfterEach
    public void teardown() {
        System.clearProperty(CLASS_NAME);
        System.clearProperty(METHOD_NAME);
    }

    protected void doTest(ITestHandler handler) {
        if (System.getProperty(METHOD_NAME) != null) {
            handler.test(this::validate, this::builder);
        } else {
            String paths;
            try {
                paths = MockTransformerService.getTestJarsPath() + "," + MockTransformerService.getBasePath();
            } catch (Exception e) {
                if (e instanceof RuntimeException re)
                    throw re;
                throw new RuntimeException(e);
            }
            var method = handler.getClass().getEnclosingMethod();
            System.setProperty(CLASS_NAME, method.getDeclaringClass().getName());
            System.setProperty(METHOD_NAME, method.getName());
            System.setProperty("test.harness.game", paths);
            System.setProperty("test.harness.callable", TestCallback.class.getName());
            BootstrapLauncher.main("--version", "1.0", "--launchTarget", "testharness");
        }
    }

    public static class TestCallback {
        public static ServiceRunner supplier() {
            return new ServiceRunner() {
                // This was originally written to allow us to have cleaner nested classes, but JUnit doesn't run tests inside non-static classes.
                // Leaving this here because why not...
                private Object getInstance(Class<?> clazz) throws Throwable {
                    Class<?> outer = clazz.getEnclosingClass();
                    if (outer == null || Modifier.isStatic(clazz.getModifiers()))
                        return clazz.getConstructor().newInstance();

                    var pinst = getInstance(outer);
                    var inst = clazz.getConstructor(outer).newInstance(pinst);
                    return inst;
                }

                @Override
                public void run() throws Throwable {
                    String method = System.getProperty(METHOD_NAME);
                    var inst = getInstance(Class.forName(System.getProperty(CLASS_NAME), true, Thread.currentThread().getContextClassLoader()));
                    var handle = MethodHandles.lookup().findVirtual(inst.getClass(), method, MethodType.methodType(void.class));
                    handle.invoke(inst);
                }
            };
        }
    }
}
