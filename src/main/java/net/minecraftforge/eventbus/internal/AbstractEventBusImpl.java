/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.event.characteristic.MonitorAware;
import net.minecraftforge.eventbus.api.event.characteristic.SelfDestructing;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.Priority;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public sealed interface AbstractEventBusImpl<T extends Event, I> extends EventBus<T>
        permits CancellableEventBusImpl, EventBusImpl {
    //region Record component accessors
    ArrayList<EventListener> backingList();
    ArrayList<EventListener> monitorBackingList();
    List<AbstractEventBusImpl<?, ?>> children();
    AtomicBoolean shutdownFlag();
    AtomicBoolean alreadyInvalidated();
    int eventCharacteristics();
    //endregion

    static int computeEventCharacteristics(Class<?> eventType) {
        int characteristics = 0;

        if (SelfDestructing.class.isAssignableFrom(eventType))
            characteristics |= Constants.CHARACTERISTIC_SELF_DESTRUCTING;

        if (MonitorAware.class.isAssignableFrom(eventType))
            characteristics |= Constants.CHARACTERISTIC_MONITOR_AWARE;

        if (Cancellable.class.isAssignableFrom(eventType))
            characteristics |= Constants.CHARACTERISTIC_CANCELLABLE;

        if (InheritableEvent.class.isAssignableFrom(eventType))
            characteristics |= Constants.CHARACTERISTIC_INHERITABLE;

        return characteristics;
    }

    /**
     * Creates a pre-sized list for possible children of this event.
     */
    static List<AbstractEventBusImpl<?, ?>> makeEventChildrenList(Class<?> eventType, int eventCharacteristics) {
        if (Constants.notInheritable(eventCharacteristics) || Modifier.isFinal(eventType.getModifiers()))
            return Collections.emptyList(); // If the event isn't inheritable, don't factor in any children

        // If it's inheritable and sealed, we can pre-size the list based on the number of permitted subclasses
        var permittedSubclasses = eventType.getPermittedSubclasses();
        if (permittedSubclasses != null)
            return new ArrayList<>(permittedSubclasses.length);

        return new ArrayList<>();
    }

    @Override
    default EventListener addListener(EventListener listener) {
        synchronized (backingList()) {
            boolean added = listener.priority() == Priority.MONITOR
                    ? monitorBackingList().add(listener)
                    : backingList().add(listener);

            if (added) {
                invalidateInvoker();

                if (notInheritable())
                    return listener;

                for (var child : children()) {
                    child.addListener(listener);
                }
            }
            return listener;
        }
    }

    @Override
    default void removeListener(EventListener listener) {
        synchronized (backingList()) {
            boolean removed = listener.priority() == Priority.MONITOR
                    ? monitorBackingList().remove(listener)
                    : backingList().remove(listener);

            if (removed) {
                invalidateInvoker();

                if (notInheritable())
                    return;

                for (var child : children()) {
                    child.removeListener(listener);
                }
            }
        }
    }

    //region Invoker
    /**
     * @return The invoker if it is still valid, otherwise null.
     */
    @Nullable I maybeGetInvoker();

    /**
     * Should only be called from inside a {@code synchronized(backingList)} block.
     */
    void invalidateInvoker();

    /**
     * Should only be called when the invoker returned by {@link #maybeGetInvoker()} is null, indicating that it has
     * been invalidated and needs to be rebuilt.
     */
    I buildInvoker();

    void setNoOpInvoker();

    /**
     * @return The invoker, creating it if necessary. Never returns null.
     */
    default I getInvoker() {
        var invoker = maybeGetInvoker();
        if (invoker == null)
            invoker = buildInvoker();

        return invoker;
    }
    //endregion

    default void startup() {
        if (!shutdownFlag().compareAndSet(true, false))
            return;

        synchronized (backingList()) {
            // Force invalidate the invoker to remove the no-op invoker that might've been set by shutdown()
            alreadyInvalidated().set(false);
            invalidateInvoker();
        }
        children().forEach(AbstractEventBusImpl::startup);
    }

    default void shutdown() {
        if (!shutdownFlag().compareAndSet(false, true))
            return;

        synchronized (backingList()) {
            // When shutdown, set the invoker to a no-op invoker and prevent it from being invalidated
            // on calls to addListener() to keep the no-op invoker
            setNoOpInvoker();
            alreadyInvalidated().set(true);
        }
        children().forEach(AbstractEventBusImpl::shutdown);
    }

    default void dispose() {
        shutdown();
        synchronized (backingList()) {
            backingList().clear();
            monitorBackingList().clear();

            backingList().trimToSize();
            monitorBackingList().trimToSize();
        }
        children().forEach(AbstractEventBusImpl::dispose);
    }

    default void trim() {
        synchronized (backingList()) {
            backingList().trimToSize();
            monitorBackingList().trimToSize();
        }
    }

    private boolean notInheritable() {
        return (eventCharacteristics() & Constants.CHARACTERISTIC_INHERITABLE) == 0;
    }
}
