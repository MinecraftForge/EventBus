package net.minecraftforge.eventbus.test;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import net.minecraftforge.eventbus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class DeadlockingEventTest {
    @Test
    public void testConstructEventDeadlock() {
        System.setProperty("test.harness", "out/production/classes,out/test/classes,out/mlservice/classes,out/mlservice/resources,out/testJars/classes,build/classes/java/main,build/classes/java/mlservice,build/classes/java/test,build/classes/java/testJars,build/resources/mlservice");
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.test.DeadlockingEventTest$Callback");
        Launcher.main("--version", "1.0", "--launchTarget", "testharness");
    }

    public static class Callback {
        public static Callable<Void> supplier() {
            return () -> {
                final TransformingClassLoader contextClassLoader = (TransformingClassLoader) Thread.currentThread().getContextClassLoader();
                contextClassLoader.addTargetPackageFilter(s -> !(
                        s.startsWith("net.minecraftforge.eventbus.") &&
                                !s.startsWith("net.minecraftforge.eventbus.test")));

                Class.forName("net.minecraftforge.eventbus.test.DeadlockingEventArmsLength$DummyEvent", true, contextClassLoader);
                Callable<Void> task1 = () -> {
                    LogManager.getLogger().info("Task 1");
                    Class.forName("net.minecraftforge.eventbus.test.DeadlockingEventArmsLength$ParentEvent", true, contextClassLoader);
                    LogManager.getLogger().info("Task 1");
                    return null;
                };
                Callable<Void> task2 = () -> {
                    LogManager.getLogger().info("Task 2");
                    Class.forName("net.minecraftforge.eventbus.test.DeadlockingEventArmsLength$ChildEvent", true, contextClassLoader);
                    LogManager.getLogger().info("Task 2");
                    return null;
                };
                Callable<Void> task3 = () -> {
                    LogManager.getLogger().info("Task 3");
                    contextClassLoader.loadClass("net.minecraftforge.eventbus.test.DeadlockingEventArmsLength$Child2Event");
                    LogManager.getLogger().info("Task 3");
                    return null;
                };
                final List<Future<Void>> futures;
                try {
                    futures = Executors.newFixedThreadPool(4).invokeAll(Arrays.asList(task1, task3, task2, task1), 1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    fail("Interrupted", e);
                    throw new RuntimeException();
                }
                futures.forEach(f -> {
                    try {
                        f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        fail("error", e);
                    }
                });
                return null;
            };
        };
    }
}
