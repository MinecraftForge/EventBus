package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.EventBus;

public interface IEventBus {
    void register(Object target);

    void unregister(Object object);

    boolean post(Event event);

    static IEventBus create() {
        return new EventBus();
    }

    static IEventBus create(IEventExceptionHandler handler) {
        return new EventBus(handler);
    }
}
