/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;
import net.minecraftforge.eventbus.api.listener.Priority;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VolatileCallSite;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.minecraftforge.eventbus.internal.Constants.*;

public record CancellableEventBusImpl<T extends Event & Cancellable>(
        String busGroupName,
        Class<T> eventType,
        CallSite invokerCallSite,
        ArrayList<EventListener> backingList,
        ArrayList<EventListener> monitorBackingList,
        List<AbstractEventBusImpl<?, ?>> children,
        AtomicBoolean alreadyInvalidated,
        AtomicBoolean shutdownFlag,
        int eventCharacteristics
) implements CancellableEventBus<T>, AbstractEventBusImpl<T, Predicate<T>> {
    public CancellableEventBusImpl(String busGroupName, Class<T> eventType, ArrayList<EventListener> backingList, int eventCharacteristics) {
        this(
                busGroupName,
                eventType,
                new VolatileCallSite(backingList.isEmpty() ? MH_NO_OP_PREDICATE : MH_NULL_PREDICATE),
                backingList,
                new ArrayList<>(),
                AbstractEventBusImpl.makeEventChildrenList(eventType, eventCharacteristics),
                new AtomicBoolean(),
                new AtomicBoolean(),
                eventCharacteristics
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // T extends Event, so this is safe.
    public EventListener addListener(Consumer<T> listener) {
        return addListener(new EventListenerImpl.WrappedConsumerListener(eventType, Priority.NORMAL, (Consumer<Event>) (Consumer) listener));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // T extends Event, so this is safe.
    public EventListener addListener(byte priority, Consumer<T> listener) {
        return addListener(
                priority == Priority.MONITOR
                        ? new EventListenerImpl.MonitoringListener(eventType, (Consumer<Event>) (Consumer) listener)
                        : new EventListenerImpl.WrappedConsumerListener(eventType, priority, (Consumer<Event>) (Consumer) listener)
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // T extends Event, so this is safe
    public EventListener addListener(byte priority, boolean alwaysCancelling, Consumer<T> listener) {
        if (!alwaysCancelling) {
            throw new IllegalArgumentException("If you never cancel the event, call addListener(byte, Consumer<T>)" +
                    "instead to avoid the possibility of an unnecessary breaking change if the event is no longer" +
                    "cancellable in the future"
            );
        }

        if (priority == Priority.MONITOR)
            throw new IllegalArgumentException("Monitoring listeners cannot cancel events");

        return addListener(new EventListenerImpl.WrappedConsumerListener(eventType, priority, true, (Consumer<Event>) (Consumer) listener));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // T extends Event, so this is safe
    public EventListener addListener(Predicate<T> listener) {
        return addListener(new EventListenerImpl.PredicateListener(eventType, Priority.NORMAL, (Predicate<Event>) (Predicate) listener));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // T extends Event, so this is safe
    public EventListener addListener(byte priority, Predicate<T> listener) {
        if (priority == Priority.MONITOR)
            throw new IllegalArgumentException("Monitoring listeners cannot cancel events");

        return addListener(new EventListenerImpl.PredicateListener(eventType, priority, (Predicate<Event>) (Predicate) listener));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // T extends Event, so this is safe
    public EventListener addListener(ObjBooleanBiConsumer<T> monitoringListener) {
        return addListener(new EventListenerImpl.MonitoringListener(eventType, (ObjBooleanBiConsumer<Event>) (ObjBooleanBiConsumer) monitoringListener));
    }

    @Override
    public boolean post(T event) {
        return getInvoker().test(event);
    }

    @Override
    public T fire(T event) {
        getInvoker().test(event);
        return event;
    }

    @Override
    public boolean hasListeners() {
        return ((Predicate<?>) getInvoker()) != NO_OP_PREDICATE;
    }

    //region Invoker
    @Override // overrides from AbstractEventBusImpl
    @SuppressWarnings("unchecked")
    public @Nullable Predicate<T> maybeGetInvoker() {
        try {
            return (Predicate<T>) invokerCallSite.getTarget().invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t); // should never happen, but we should throw if it somehow does
        }
    }

    @Override // overrides from AbstractEventBusImpl
    public void invalidateInvoker() {
        invokerCallSite.setTarget(backingList.isEmpty() ? MH_NO_OP_PREDICATE : MH_NULL_PREDICATE);
    }

    @Override // overrides from AbstractEventBusImpl
    public Predicate<T> buildInvoker() {
        synchronized (backingList) {
            backingList.sort(PRIORITY_COMPARATOR);

            if (Constants.isSelfDestructing(eventCharacteristics()))
                monitorBackingList.add(new EventListenerImpl.MonitoringListener(eventType, (event, wasCancelled) -> dispose()));

            Predicate<T> invoker = setInvoker(InvokerFactory.createCancellableMonitoringInvoker(
                    eventType, eventCharacteristics, backingList, monitorBackingList
            ));

            alreadyInvalidated.set(false);
            return invoker;
        }
    }

    @Override // overrides from AbstractEventBusImpl
    public void setNoOpInvoker() {
        invokerCallSite.setTarget(MH_NO_OP_PREDICATE);
    }

    /**
     * Should only be called from inside a {@code synchronized(backingList)} block.
     */
    private Predicate<T> setInvoker(Predicate<T> invoker) {
        invokerCallSite.setTarget(MethodHandles.constant(Predicate.class, invoker));
        return invoker;
    }
    //endregion
}
