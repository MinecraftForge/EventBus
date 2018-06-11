package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.EventBus;

import java.util.function.Consumer;

public interface IEventBus {
    <T extends Event> void addListener(Consumer<T> consumer);

    void register(Object target);

    <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Consumer<T> consumer);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Class<T> eventType, Consumer<T> consumer);

    <T extends GenericEvent<F>, F> void addGenericListener(Class<F> filter, Consumer<T> consumer);

    <T extends GenericEvent<F>, F> void addGenericListener(Class<F> filter, EventPriority priority, Consumer<T> consumer);

    <T extends GenericEvent<F>, F> void addGenericListener(Class<F> genericClassFilter, EventPriority priority, boolean receiveCancelled, Consumer<T> consumer);

    <T extends GenericEvent<F>, F> void addGenericListener(Class<F> genericClassFilter, EventPriority priority, boolean receiveCancelled, Class<T> eventType, Consumer<T> consumer);

    void unregister(Object object);

    boolean post(Event event);

    static IEventBus create() {
        return new EventBus();
    }

    static IEventBus create(IEventExceptionHandler handler) {
        return new EventBus(handler);
    }
}
