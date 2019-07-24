package net.minecraftforge.eventbus.test;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class DeadlockingEventTest {
    private static final boolean initializeAtClassloading = false;
    @BeforeAll
    static void setup() {
        System.setProperty("test.harness", "out/production/classes,out/test/classes,out/mlservice/classes,out/mlservice/resources,out/testJars/classes,build/classes/java/main,build/classes/java/mlservice,build/classes/java/test,build/classes/java/testJars,build/resources/mlservice");
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.test.DeadlockingEventTest$Callback");
    }
    @RepeatedTest(50)
    void testConstructEventDeadlock() {
        Launcher.main("--version", "1.0", "--launchTarget", "testharness");
    }

    @SuppressWarnings("unchecked")
    @AfterEach
    void clearBusStuff() throws IllegalAccessException {
        final Map<Class<?>, ListenerList> listeners = (Map<Class<?>, ListenerList>) Whitebox.getField(EventListenerHelper.class, "listeners").get(null);
        listeners.clear();
    }

    public static class Callback {
        public static Callable<Void> supplier() {
            return () -> {
                final TransformingClassLoader contextClassLoader = (TransformingClassLoader) Thread.currentThread().getContextClassLoader();
                LogManager.getLogger().info("Class Loader {}", contextClassLoader);
                contextClassLoader.addTargetPackageFilter(s -> !(
                        s.startsWith("net.minecraftforge.eventbus.") &&
                                !s.startsWith("net.minecraftforge.eventbus.test")));

                final IEventBus bus = BusBuilder.builder().build();
                Callable<Void> task2 = () -> {
                    LogManager.getLogger().info("Task 2");
                    final Class<? extends Event> clz = (Class<? extends Event>) Class.forName("net.minecraftforge.eventbus.test.DeadlockingEventArmsLength$ChildEvent", initializeAtClassloading, contextClassLoader);
                    Thread.sleep(0, new Random().nextInt(10000));
                    assertEquals(clz.newInstance().getListenerList(), EventListenerHelper.getListenerList(clz));
                    LogManager.getLogger().info("Task 2");
                    return null;
                };
                Callable<Void> task1 = () -> {
                    LogManager.getLogger().info("Task 1");
                    final Class<?> clz = Class.forName("net.minecraftforge.eventbus.test.DeadlockingEventArmsLength$Listener1", initializeAtClassloading, contextClassLoader);
                    bus.register(clz);
                    LogManager.getLogger().info("Task 1");
                    return null;
                };
                final List<Future<Void>> futures;
                try {
                    futures = Executors.newFixedThreadPool(2).invokeAll(Arrays.asList(task1, task2), 100, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    fail("Interrupted", e);
                    throw new RuntimeException();
                }
                assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                    futures.forEach(f -> {
                        try {
                            f.get();
                        } catch (InterruptedException | ExecutionException e) {
                            fail("error", e);
                        }
                    });
                });
                return null;
            };
        };
    }
}
