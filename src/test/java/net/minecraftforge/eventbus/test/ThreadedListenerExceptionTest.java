package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ThreadedListenerExceptionTest {

    private static boolean failed;

    private static final IEventBus testEventBus = BusBuilder.builder().setExceptionHandler((bus, event, listeners, index, throwable) -> {
        failed = true;
/*

        throwable.printStackTrace();

        try {
            final ListenerList listenerList = event.getListenerList();

            Method getInstance = listenerList.getClass().getDeclaredMethod("getInstance", int.class);
            getInstance.setAccessible(true);

            Object listenerListInst = getInstance.invoke(listenerList, 0);

            Field prioritiesField = listenerListInst.getClass().getDeclaredField("priorities");
            prioritiesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<ArrayList<IEventListener>> priorities = (ArrayList<ArrayList<IEventListener>>) prioritiesField.get(listenerListInst);

            Arrays.stream(EventPriority.values()).forEach(priority -> {
                LogManager.getLogger().error("priority={}, listeners=[{}]", priority.name(), priorities.get(priority.ordinal()).stream().map(Objects::toString).collect(Collectors.joining(",")));
            });

            final Field listeners1 = listenerListInst.getClass().getDeclaredField("listeners");
            listeners1.setAccessible(true);
            final IEventListener[] cache = (IEventListener[]) listeners1.get(listenerListInst);
            LogManager.getLogger().error("cache={}", Arrays.asList(cache));
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException ignored) { }
*/
    }).build();
    private static ExecutorService executorService;

//    @Test
//    public void testListenerHammering() {
//        assertTimeoutPreemptively(Duration.ofSeconds(30), this::testListenerListConstruction);
//    }

    @BeforeAll
    static void beforeClass() {
        executorService = Executors.newFixedThreadPool(100);
    }
    @BeforeEach
    public void beforeEach() throws Exception {
        failed = false;
        final List<Callable<Object>> callables = Collections.nCopies(50, Executors.callable(() -> testEventBus.addListener(ThreadedListenerExceptionTest::testEvent1)));
        executorService.invokeAll(callables).stream().forEach(f->{
            try {
                // wait for everybody
                f.get();
            } catch (InterruptedException | ExecutionException e) {
            }
        });
    }

    @RepeatedTest(100)
    public void testWithTimeout() {
        assertTimeoutPreemptively(Duration.ofMillis(10000), this::testListenerList);
    }

    public void testListenerList() throws Exception {
        final List<Callable<Object>> callables = Collections.nCopies(100, Executors.callable(ThreadedListenerExceptionTest::generateEvents));
        executorService.invokeAll(callables).stream().forEach(f->{
            try {
                // wait for everybody
                f.get();
            } catch (InterruptedException | ExecutionException e) {
            }
        });
        assertFalse(failed);
    }

    private static void generateEvents() {
        for (int i = 0; i < 10; i ++) {
            testEventBus.post(new TestEvent());
        }
    }
    private static void testEvent1(TestEvent evt) {

    }
    private static void testEvent2(TestEvent evt) {

    }
    private static void testEvent3(TestEvent evt) {

    }
    private static void testEvent4(TestEvent evt) {

    }
    private static void testEvent5(TestEvent evt) {

    }

    public static class TestEvent extends Event {

        public TestEvent() { }

        private static class Runner extends Thread {

            @Override
            public void run() {
            }

        }

    }

}
