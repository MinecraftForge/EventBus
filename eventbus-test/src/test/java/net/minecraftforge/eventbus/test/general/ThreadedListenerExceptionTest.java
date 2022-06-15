package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.eventbus.test.ITestHandler;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ThreadedListenerExceptionTest implements ITestHandler {

    private static boolean failed;

    @Override
    public void before() {
        failed = false;
    }


    private static ExecutorService executorService = Executors.newFixedThreadPool(100);

//    @Test
//    public void testListenerHammering() {
//        assertTimeoutPreemptively(Duration.ofSeconds(30), this::testListenerListConstruction);
//    }

    @BeforeEach
    public void beforeEach() throws Exception {
        failed = false;
    }

    private IEventBus bus(BusBuilder builder) {
        return builder.setExceptionHandler((bus, event, listeners, index, throwable) -> {
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

    }

    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(TestEvent.class);
        var bus = bus(builder.get());

        final List<Callable<Object>> callables = Collections.nCopies(50, Executors.callable(() -> bus.addListener(ThreadedListenerExceptionTest::testEvent1)));
        try {
            executorService.invokeAll(callables).stream().forEach(f->{
                try {
                    // wait for everybody
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTimeoutPreemptively(Duration.ofMillis(10000), () -> testListenerList(bus));
    }

    public void testListenerList(final IEventBus bus) throws Exception {
        final List<Callable<Object>> callables = Collections.nCopies(100, Executors.callable(() -> {
            for (int i = 0; i < 10; i ++) {
                bus.post(new TestEvent());
            }
        }));
        executorService.invokeAll(callables).stream().forEach(f->{
            try {
                // wait for everybody
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        assertFalse(failed);
    }

    private static void testEvent1(TestEvent evt) {}
    public static class TestEvent extends Event {}
}
