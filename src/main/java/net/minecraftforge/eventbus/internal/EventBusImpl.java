/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.Priority;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VolatileCallSite;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static net.minecraftforge.eventbus.internal.Constants.*;

public record EventBusImpl<T extends Event>(
        String busGroupName,
        Class<T> eventType,
        CallSite invokerCallSite,
        ArrayList<EventListener> backingList,
        ArrayList<EventListener> monitorBackingList,
        List<AbstractEventBusImpl<?, ?>> children,
        AtomicBoolean alreadyInvalidated,
        AtomicBoolean shutdownFlag,
        int eventCharacteristics
) implements EventBus<T>, AbstractEventBusImpl<T, Consumer<T>> {
    public EventBusImpl(String busGroupName, Class<T> eventType, ArrayList<EventListener> backingList, int eventCharacteristics) {
        this(
                busGroupName,
                eventType,
                new VolatileCallSite(backingList.isEmpty() ? MH_NO_OP_CONSUMER : MH_NULL_CONSUMER),
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
        return addListener(new EventListenerImpl.ConsumerListener(eventType, Priority.NORMAL, (Consumer<Event>) (Consumer) listener));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // T extends Event, so this is safe.
    public EventListener addListener(byte priority, Consumer<T> listener) {
        return addListener(
                priority == Priority.MONITOR
                        ? new EventListenerImpl.MonitoringListener(eventType, (Consumer<Event>) (Consumer) listener)
                        : new EventListenerImpl.ConsumerListener(eventType, priority, (Consumer<Event>) (Consumer) listener)
        );
    }

    @Override
    public boolean post(T event) {
        getInvoker().accept(event);
        return false;
    }

    @Override
    public T fire(T event) {
        getInvoker().accept(event);
        return event;
    }

    @Override
    public boolean hasListeners() {
        return getInvoker() != NO_OP_CONSUMER;
    }

    //region Invoker
    @Override // overrides from AbstractEventBusImpl
    @SuppressWarnings("unchecked")
    public @Nullable Consumer<T> maybeGetInvoker() {
        try {
            return (Consumer<T>) invokerCallSite.getTarget().invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t); // should never happen, but we should throw if it somehow does
        }
    }

    @Override // overrides from AbstractEventBusImpl
    public void invalidateInvoker() {
        invokerCallSite.setTarget(backingList.isEmpty() ? MH_NO_OP_CONSUMER : MH_NULL_CONSUMER);
    }

    @Override // overrides from AbstractEventBusImpl
    public Consumer<T> buildInvoker() {
        synchronized (backingList) {
            backingList.sort(PRIORITY_COMPARATOR);

            Consumer<T> invoker = InvokerFactory.createMonitoringInvoker(
                    eventType, eventCharacteristics, backingList, monitorBackingList
            );

            if (Constants.isSelfDestructing(eventCharacteristics))
                invoker = invoker.andThen(event -> dispose());

            setInvoker(invoker);
            alreadyInvalidated.set(false);
            return invoker;
        }
    }

    @Override // overrides from AbstractEventBusImpl
    public void setNoOpInvoker() {
        invokerCallSite.setTarget(MH_NO_OP_CONSUMER);
    }

    /**
     * Should only be called from inside a {@code synchronized(backingList)} block.
     */
    private void setInvoker(Consumer<T> invoker) {
        invokerCallSite.setTarget(MethodHandles.constant(Consumer.class, invoker));
    }
    //endregion
}
