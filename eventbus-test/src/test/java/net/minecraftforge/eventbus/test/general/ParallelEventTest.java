package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.test.ITestHandler;
import net.minecraftforge.eventbus.test.Whitebox;
import net.minecraftforge.eventbus.testjar.DummyEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ParallelEventTest implements ITestHandler {
    private static final int BUS_COUNT = 16;
    private static final int LISTENER_COUNT = 1000;
    private static final int RUN_ITERATIONS = 1000;

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public void before(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        COUNTER.set(0);
        validator.accept(DummyEvent.class);
        validator.accept(DummyEvent.GoodEvent.class);
    }

    protected void handle(DummyEvent.GoodEvent event) {
        COUNTER.incrementAndGet();
    }

    public static class Multiple extends ParallelEventTest {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            Set<IEventBus> busSet = new HashSet<>();
            for (int i = 0; i < BUS_COUNT; i++) {
                busSet.add(builder.get().setTrackPhases(false).build()); //make buses for concurrent testing
            }
            busSet.parallelStream().forEach(iEventBus -> { //execute parallel listener adding
                for (int i = 0; i < LISTENER_COUNT; i++)
                    iEventBus.addListener(this::handle);
            });

            // Make sure it tracked them all
            busSet.forEach(bus -> {
                int busid = Whitebox.getInternalState(bus, "busID");
                ListenerList afterAdd = Whitebox.invokeMethod(new DummyEvent.GoodEvent(), "getListenerList");
                assertEquals(LISTENER_COUNT, afterAdd.getListeners(busid).length - 1, "Failed to register all event handlers");
            });

            busSet.parallelStream().forEach(iEventBus -> { //post events parallel
                for (int i = 0; i < RUN_ITERATIONS; i++)
                    iEventBus.post(new DummyEvent.GoodEvent());
            });

            assertEquals(COUNTER.get(), BUS_COUNT * LISTENER_COUNT * RUN_ITERATIONS);
        }
    }

    public static class Single extends ParallelEventTest {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            IEventBus bus = builder.get().setTrackPhases(false).build();

            Set<Runnable> toAdd = new HashSet<>();

            for (int i = 0; i < LISTENER_COUNT; i++) { //prepare parallel listener adding
                toAdd.add(() -> bus.addListener(this::handle));
            }
            toAdd.parallelStream().forEach(Runnable::run); //execute parallel listener adding

            // Make sure it tracked them all
            int busid = Whitebox.getInternalState(bus, "busID");
            ListenerList afterAdd = Whitebox.invokeMethod(new DummyEvent.GoodEvent(), "getListenerList");
            assertEquals(LISTENER_COUNT, afterAdd.getListeners(busid).length - 1, "Failed to register all event handlers");

            toAdd = new HashSet<>();
            for (int i = 0; i < RUN_ITERATIONS; i++) //prepare parallel event posting
                toAdd.add(() -> bus.post(new DummyEvent.GoodEvent()));
            toAdd.parallelStream().forEach(Runnable::run); //post events parallel

            assertEquals(COUNTER.get(), LISTENER_COUNT * RUN_ITERATIONS);
        }
    }
}
