/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.*;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.event.characteristic.MonitorAware;
import net.minecraftforge.eventbus.api.listener.EventListener;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public record BusGroupImpl(
        String name,
        Class<?> baseType,
        ConcurrentHashMap<Class<? extends Event>, EventBus<?>> eventBuses
) implements BusGroup {
    private static final Set<String> BUS_GROUP_NAMES = ConcurrentHashMap.newKeySet();

    public BusGroupImpl(String name, Class<?> baseType) {
        this(name, baseType, new ConcurrentHashMap<>());
    }

    public BusGroupImpl {
        if (!BUS_GROUP_NAMES.add(Objects.requireNonNull(name)))
            throw new IllegalArgumentException("BusGroup name \"" + name + "\" is already in use");
    }

    @Override
    public void startup() {
        for (var eventBus : eventBuses.values())
            ((AbstractEventBusImpl<?, ?>) eventBus).startup();
    }

    @Override
    public void shutdown() {
        for (var eventBus : eventBuses.values())
            ((AbstractEventBusImpl<?, ?>) eventBus).shutdown();
    }

    @Override
    public void dispose() {
        for (var eventBus : eventBuses.values())
            ((AbstractEventBusImpl<?, ?>) eventBus).dispose();

        eventBuses.clear();
        BUS_GROUP_NAMES.remove(name);
    }

    @Override
    public void trim() {
        for (var eventBus : eventBuses.values())
            ((AbstractEventBusImpl<?, ?>) eventBus).trim();
    }

    @Override
    public Collection<EventListener> register(MethodHandles.Lookup callerLookup, Class<?> utilityClassWithStaticListeners) {
        return Constants.STRICT_REGISTRATION_CHECKS
                ? EventListenerFactory.registerStrict(this, callerLookup, utilityClassWithStaticListeners, null)
                : EventListenerFactory.register(this, callerLookup, utilityClassWithStaticListeners, null);
    }

    @Override
    public Collection<EventListener> register(MethodHandles.Lookup callerLookup, Object listener) {
        return Constants.STRICT_REGISTRATION_CHECKS
                ? EventListenerFactory.registerStrict(this, callerLookup, listener.getClass(), listener)
                : EventListenerFactory.register(this, callerLookup, listener.getClass(), listener);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void unregister(Collection<EventListener> listeners) {
        if (listeners.isEmpty())
            throw new IllegalArgumentException("Listeners cannot be empty! You should be getting the collection from" +
                    "the BusGroup#register method.");

        for (var listener : listeners) {
            getOrCreateEventBus((Class<? extends Event>) listener.eventType()).removeListener(listener);
        }
    }

    //region Internal access only
    @SuppressWarnings("unchecked")
    private <T extends Event> EventBus<T> createEventBus(Class<T> eventType) {
        if (baseType != Event.class && !baseType.isAssignableFrom(eventType))
            throw new IllegalArgumentException("BusGroup \"" + name + "\" requires all events on it to inherit from " + baseType + " but " + eventType + " doesn't.");

//        if (eventBuses.containsKey(eventType))
//            throw new IllegalArgumentException("EventBus for " + eventType + " already exists on BusGroup \"" + name + "\"");

        if (RecordEvent.class.isAssignableFrom(eventType) && !eventType.isRecord())
            throw new IllegalArgumentException("Event type " + eventType + " is not a record class but implements RecordEvent");

        if (MonitorAware.class.isAssignableFrom(eventType) && !MutableEvent.class.isAssignableFrom(eventType))
            throw new IllegalArgumentException("Event type " + eventType + " implements MonitorAware but is not a MutableEvent");

        int characteristics = AbstractEventBusImpl.computeEventCharacteristics(eventType);

        var backingList = new ArrayList<EventListener>();
        List<EventBus<?>> parents = Collections.emptyList();
        if (Constants.isInheritable(characteristics)) {
            parents = getParentEvents(eventType);
            for (var parent : parents) {
                backingList.addAll(((AbstractEventBusImpl<?, ?>) parent).backingList());
            }
        }

        @SuppressWarnings("rawtypes")
        var bus = Constants.isCancellable(characteristics)
                ? new CancellableEventBusImpl<>(this.name, (Class) (Class<? extends Cancellable>) eventType, backingList, characteristics)
                : new EventBusImpl<>(this.name, eventType, backingList, characteristics);

        if (Constants.isInheritable(characteristics)) {
            for (var parent : parents) {
                ((AbstractEventBusImpl<?, ?>) parent).children().add(bus);
            }
        }

        return bus;
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> EventBus<T> getOrCreateEventBus(Class<T> eventType) {
        return (EventBus<T>) eventBuses.computeIfAbsent(eventType, event -> createEventBus(eventType));
    }
    //endregion

    @Override
    public boolean equals(Object that) {
        return this == that || (that instanceof BusGroupImpl busGroup && name.equals(busGroup.name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    private <T extends Event> List<EventBus<?>> getParentEvents(Class<T> eventType) {
        var parentEvents = new ArrayList<EventBus<?>>();

        // first handle class inheritance (e.g. MyEvent extends ParentEvent)
        Class<? super T> parent = eventType.getSuperclass();
        if (parent != null // has a parent that's not Object
                && InheritableEvent.class.isAssignableFrom(parent) // implements InheritableEvent
                && parent != MutableEvent.class // the parent isn't exactly MutableEvent
        ) {
            @SuppressWarnings("unchecked")
            var parentEvent = getOrCreateEventBus((Class<? extends Event>) parent);
            parentEvents.add(parentEvent);
        }

        // then handle interfaces (e.g. MyEvent implements MyEventInterface)
        for (var iface : eventType.getInterfaces()) {
            if (iface != InheritableEvent.class
                    && InheritableEvent.class.isAssignableFrom(iface)
                    && iface != RecordEvent.class
                    && iface != Event.class
            ) {
                @SuppressWarnings("unchecked")
                var parentEvent = getOrCreateEventBus((Class<? extends Event>) iface);
                parentEvents.add(parentEvent);
            }
        }

        return parentEvents;
    }
}
