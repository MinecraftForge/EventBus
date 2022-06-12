package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.eventbus.testjar.DummyEvent;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ArmsLengthHandler {
    public void call() throws Exception {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        LogManager.getLogger().info("CCL is {}", contextClassLoader);
        IEventBus bus = BusBuilder.builder().setTrackPhases(false).build();
        LogManager.getLogger().info("Bus is {}", bus.getClass().getClassLoader());
        LogManager.getLogger().info("Event is {}", DummyEvent.GoodEvent.class.getClassLoader());
        Set<Runnable> toAdd = new HashSet<>();

        for (int i = 0; i < ParallelTransformedTest.LISTENER_COUNT; i++) { //prepare parallel listener adding
            toAdd.add(() -> bus.addListener((DummyEvent.GoodEvent e)-> ParallelTransformedTest.COUNTER.incrementAndGet()));
        }

        Object realListenerList = Whitebox.getField(DummyEvent.GoodEvent.class, "LISTENER_LIST").get(null);
        toAdd.parallelStream().forEach(Runnable::run); //execute parallel listener adding
        final ListenerList listenerList = Whitebox.invokeMethod(new DummyEvent.GoodEvent(), "getListenerList");
        LogManager.getLogger().info("Orig: {}, final {}", realListenerList, listenerList);
        Object inst = ((Object[])Whitebox.getInternalState(listenerList, "lists"))[0];
        final ArrayList<ArrayList<IEventListener>> priorities = Whitebox.getInternalState(inst, "priorities");
        toAdd = new HashSet<>();
        for (int i = 0; i < ParallelTransformedTest.RUN_ITERATIONS; i++) //prepare parallel event posting
            toAdd.add(() -> bus.post(new DummyEvent.GoodEvent()));
        toAdd.parallelStream().forEach(Runnable::run); //post events parallel

        try {
            long expected = ParallelTransformedTest.LISTENER_COUNT * ParallelTransformedTest.RUN_ITERATIONS;
            int busid = Whitebox.getInternalState(bus, "busID");
            Assertions.assertAll(
                    ()->Assertions.assertEquals(expected, ParallelTransformedTest.COUNTER.get()),
                    ()->Assertions.assertEquals(ParallelTransformedTest.LISTENER_COUNT, listenerList.getListeners(busid).length - 1)

            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
