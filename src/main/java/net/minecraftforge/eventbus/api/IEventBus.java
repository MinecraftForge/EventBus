package net.minecraftforge.eventbus.api;

public interface IEventBus {
    void register(Object target);

    void unregister(Object object);

    boolean post(Event event);
}
