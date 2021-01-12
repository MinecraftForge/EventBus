package net.minecraftforge.eventbus.api;

public interface IEventBusInvokeDispatcher {
    void invoke(IEventListener listener, Event event);
}
