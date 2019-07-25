package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.DummyEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.powermock.reflect.Whitebox;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class ParallelEventTest
{
    private static final int BUS_COUNT = 16;
    private static final int LISTENER_COUNT = 1000;
    private static final int RUN_ITERATIONS = 1000;

    private static final AtomicLong COUNTER = new AtomicLong();

    @BeforeEach
    public void setup() {
        COUNTER.set(0);
    }

    @Disabled
    @RepeatedTest(10)
    public void testMultipleThreadsMultipleBus() {
        Set<IEventBus> busSet = new HashSet<>();
        for (int i = 0; i < BUS_COUNT; i++) {
            busSet.add(BusBuilder.builder().setTrackPhases(false).build()); //make buses for concurrent testing
        }
        busSet.parallelStream().forEach(iEventBus -> { //execute parallel listener adding
            for (int i = 0; i < LISTENER_COUNT; i++)
                iEventBus.addListener(this::handle);
        });
        busSet.parallelStream().forEach(iEventBus -> { //post events parallel
            for (int i = 0; i < RUN_ITERATIONS; i++)
                iEventBus.post(new DummyEvent.GoodEvent());
        });

        long expected = BUS_COUNT * LISTENER_COUNT * RUN_ITERATIONS;
        Assertions.assertEquals(COUNTER.get(), expected);
    }

    @Disabled
    @RepeatedTest(100)
    public void testMultipleThreadsOneBus() {
        IEventBus bus = BusBuilder.builder().setTrackPhases(false).build();

        Set<Runnable> toAdd = new HashSet<>();

        for (int i = 0; i < LISTENER_COUNT; i++) { //prepare parallel listener adding
            toAdd.add(() -> bus.addListener(this::handle));
        }
        toAdd.parallelStream().forEach(Runnable::run); //execute parallel listener adding

        toAdd = new HashSet<>();
        for (int i = 0; i < RUN_ITERATIONS; i++) //prepare parallel event posting
            toAdd.add(() -> bus.post(new DummyEvent.GoodEvent()));
        toAdd.parallelStream().forEach(Runnable::run); //post events parallel

        try {
            long expected = LISTENER_COUNT * RUN_ITERATIONS;
            final ListenerList listenerList = Whitebox.invokeMethod(new DummyEvent.GoodEvent(), "getListenerList");
            int busid = Whitebox.getInternalState(bus, "busID");
            Assertions.assertAll(
                    ()->Assertions.assertEquals(expected, COUNTER.get()),
                    ()->Assertions.assertEquals(LISTENER_COUNT, listenerList.getListeners(busid).length - 1)

            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle(DummyEvent.GoodEvent event) {
        COUNTER.incrementAndGet();
    }
}
