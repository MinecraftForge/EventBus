/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class InvokerFactoryUtils {
    private InvokerFactoryUtils() {}

    static <T> List<Consumer<T>> unwrapConsumers(List<EventListener> listeners) {
        return listeners.stream()
                .map(listener -> {
                    if (listener instanceof EventListenerImpl.HasConsumer<?> consumerListener) {
                        return consumerListener.consumer();
                    } else {
                        throw new IllegalStateException("Unexpected listener type: " + listener.getClass());
                    }
                })
                .<Consumer<T>>map(InvokerFactoryUtils::uncheckedCast)
                .toList();
    }

    static <T> List<Consumer<T>> unwrapAlwaysCancellingConsumers(List<EventListener> listeners) {
        var unwrappedConsumers = new ArrayList<Consumer<T>>(listeners.size());
        for (var listener : listeners) {
            if (listener instanceof EventListenerImpl.HasConsumer<?> consumerListener) {
                unwrappedConsumers.add(uncheckedCast(consumerListener.consumer()));
            } else {
                throw new IllegalStateException("Unexpected listener type: " + listener.getClass());
            }

            if (listener instanceof EventListenerImpl.WrappedConsumerListener wrappedConsumerListener
                    && wrappedConsumerListener.alwaysCancelling()) {
                unwrappedConsumers.trimToSize();
                break;
            }
        }
        return unwrappedConsumers;
    }

    static <T> List<Predicate<T>> unwrapPredicates(List<EventListener> listeners) {
        var unwrappedPredicates = new ArrayList<Predicate<T>>(listeners.size());
        for (var listener : listeners) {
            if (listener instanceof EventListenerImpl.HasPredicate<?> predicateListener) {
                unwrappedPredicates.add(uncheckedCast(predicateListener.predicate()));
            } else {
                throw new IllegalStateException("Unexpected listener type: " + listener.getClass());
            }

            // Skip the rest of the listeners if we know this one will always cancel the event (and thus prevent further
            // non-monitoring listeners from being called anyway).
            if (listener instanceof EventListenerImpl.WrappedConsumerListener wrappedConsumerListener
                    && wrappedConsumerListener.alwaysCancelling()) {
                unwrappedPredicates.trimToSize();
                break;
            }
        }
        return unwrappedPredicates;
    }

    static <T> List<ObjBooleanBiConsumer<T>> unwrapMonitors(List<EventListener> monitoringListeners) {
        return monitoringListeners.stream()
                .map(EventListenerImpl.MonitoringListener.class::cast)
                .map(EventListenerImpl.MonitoringListener::booleanBiConsumer)
                .<ObjBooleanBiConsumer<T>>map(InvokerFactoryUtils::uncheckedCast)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> T uncheckedCast(Object obj) {
        return (T) obj;
    }
}
