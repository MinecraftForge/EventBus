package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.EventBus;

import java.util.function.Consumer;

public interface IEventBus {
    <T extends Event> void addListener(Consumer<T> consumer);

    void register(Object target);

    @SuppressWarnings("unchecked")
    <T extends Event> void addListener(Consumer<T> consumer, EventPriority priority);

    void unregister(Object object);

    boolean post(Event event);

    static IEventBus create() {
        return new EventBus();
    }

    static IEventBus create(IEventExceptionHandler handler) {
        return new EventBus(handler);
    }
}
