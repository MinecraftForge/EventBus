package net.minecraftforge.eventbus.testjar.benchmarks;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;
import org.openjdk.jmh.infra.Blackhole;

import java.util.function.Consumer;

public abstract class PostingBenchmark {
    protected static IEventBus createEventBus(boolean useModLauncher, int numberOfListeners, Consumer<IEventBus> registrar) {
        IEventBus eventBus;
        var busBuilder = BusBuilder.builder();
        if (useModLauncher) busBuilder = busBuilder.useModLauncher();
        eventBus = busBuilder.build();

        for (int i = 0; i < numberOfListeners; i++) {
            registrar.accept(eventBus);
        }

        return eventBus;
    }

    protected static void post(IEventBus eventBus, Blackhole bh) { // todo: use the Blackhole
        eventBus.post(new CancelableEvent());
        eventBus.post(new ResultEvent());
        eventBus.post(new EventWithData("Foo", 5, true));
    }
}
