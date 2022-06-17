package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.test.ITestHandler;
import net.minecraftforge.eventbus.test.Whitebox;

import org.apache.logging.log4j.LogManager;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

//TODO: This is disabled, eventually re-enable and figure out what it's actually testing
public class DeadlockingEventTest implements ITestHandler {
    private static final boolean initializeAtClassloading = false;
    private static final long waittimeout = 1; // number of seconds to wait before retrying. Bump this up to debug what's going on.
    public static final int BOUND = 10000;
    public static ThreadPoolExecutor THREAD_POOL;

    @Override
    public void before() {
        // force async logging
        System.setProperty("log4j2.contextSelector","org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        THREAD_POOL = (ThreadPoolExecutor)Executors.newFixedThreadPool(2);
    }

    @Override
    public void after() {
        Whitebox.invokeMethod(EventListenerHelper.class, "clearAll");
        final HashSet<AbstractQueuedSynchronizer> workers = Whitebox.getInternalState(THREAD_POOL, "workers");
        final List<Thread> threads = workers.stream().map(w -> Whitebox.<Thread>getInternalState(w, "thread")).collect(Collectors.toList());
        threads.stream().map(Thread::getStackTrace).forEach(ts->LogManager.getLogger().info("\n"+stack(ts)));
        THREAD_POOL.shutdown();
    }

    private static String stack(StackTraceElement[] elts) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement elt : elts) {
            sb.append("\tat ").append(elt).append("\n");
        }
        return sb.toString();
    }

    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        final var cdl = new CountDownLatch(1);
        final IEventBus bus = builder.get().build();

        Callable<Void> task2 = () -> {
            final int nanos = new Random().nextInt(BOUND);
            LogManager.getLogger().info("Task 2: {}", nanos);
            long start = System.nanoTime();
            cdl.await();
            final Class<? extends Event> clz = getClass("ChildEvent");
            Thread.sleep(0, nanos);
            LogManager.getLogger().info(System.nanoTime() - start);
            assertEquals(clz.getConstructor().newInstance().getListenerList(), EventListenerHelper.getListenerList(clz));
            LogManager.getLogger().info("Task 2");
            return null;
        };

        Callable<Void> task1 = () -> {
            final int nanos = new Random().nextInt(BOUND);
            LogManager.getLogger().info("Task 1: {}", nanos);
            long start = System.nanoTime();
            cdl.await();
            final Class<?> clz = getClass("Listener1");
            Thread.sleep(0, nanos);
            LogManager.getLogger().info(System.nanoTime() - start);
            bus.register(clz);
            LogManager.getLogger().info("Task 1");
            return null;
        };

        final List<Future<Void>> futures = Stream.of(task1, task2).
                map(THREAD_POOL::submit).collect(Collectors.toList());

        cdl.countDown();
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(waittimeout), () -> futures.parallelStream().forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("error", e);
                }
            }));
        } finally {
            futures.forEach(f -> f.cancel(true));
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (CancellationException | InterruptedException | ExecutionException e) {
                    // noop
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<? extends T> getClass(String name) throws ClassNotFoundException {
        var cl = Thread.currentThread().getContextClassLoader();
        return (Class<? extends T>) Class.forName(this.getClass().getName() + '$' + name, initializeAtClassloading, cl);
    }

    public static class ParentEvent extends Event {}
    public static class ChildEvent extends ParentEvent {
        static {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Listener1 {
        @SubscribeEvent
        public static void listen(ChildEvent evt) {}
    }
}
