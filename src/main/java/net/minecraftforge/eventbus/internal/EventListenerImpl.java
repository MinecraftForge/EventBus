/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;
import net.minecraftforge.eventbus.api.listener.Priority;

import java.util.function.Consumer;
import java.util.function.Predicate;

public sealed interface EventListenerImpl extends EventListener {
    sealed interface HasConsumer<T extends Event> extends EventListenerImpl {
        Consumer<T> consumer();
    }

    sealed interface HasPredicate<T extends Event> extends EventListenerImpl {
        Predicate<T> predicate();
    }

    record ConsumerListener<T extends Event>(
            Class<T> eventType,
            byte priority,
            Consumer<T> consumer
    ) implements HasConsumer<T> {}

    record PredicateListener<T extends Event & Cancellable>(
            Class<T> eventType,
            byte priority,
            Predicate<T> predicate
    ) implements HasPredicate<T> {
        public PredicateListener {
            assert priority != Priority.MONITOR : "Monitoring listeners cannot cancel events";
        }
    }

    record MonitoringListener<T extends Event>(
            Class<T> eventType,
            ObjBooleanBiConsumer<T> booleanBiConsumer
    ) implements EventListenerImpl {
        public MonitoringListener(Class<T> eventType, Consumer<T> listener) {
            this(eventType, (event, wasCancelled) -> listener.accept(event));
        }

        @Override
        public byte priority() {
            return Priority.MONITOR;
        }
    }

    record WrappedConsumerListener<T extends Event>(
            Class<T> eventType,
            byte priority,
            boolean alwaysCancelling,
            Consumer<T> consumer,
            Predicate<T> predicate
    ) implements HasConsumer<T>, HasPredicate<T> {
        public WrappedConsumerListener(Class<T> eventType, byte priority, Consumer<T> consumer) {
            this(eventType, priority, false, consumer, wrap(false, consumer));
        }

        public WrappedConsumerListener(Class<T> eventType, byte priority, boolean alwaysCancelling, Consumer<T> consumer) {
            this(eventType, priority, alwaysCancelling, consumer, wrap(alwaysCancelling, consumer));
        }

        public WrappedConsumerListener {
            assert !(alwaysCancelling && priority == Priority.MONITOR) : "Monitoring listeners cannot cancel events";
        }

        /**
         * @implNote We avoid capturing the alwaysCancelling field in the lambda so that the bytecode is generated as a
         *           {@code ICONST_1} or {@code ICONST_0} instruction, rather than a field load.
         */
        public static <T extends Event> Predicate<T> wrap(boolean alwaysCancelling, Consumer<T> consumer) {
            if (alwaysCancelling) {
                return event -> {
                    consumer.accept(event);
                    return true;
                };
            } else {
                return event -> {
                    consumer.accept(event);
                    return false;
                };
            }
        }

        // Don't factor in the wrapped predicate for equals() and hashCode()
        @Override
        public boolean equals(Object obj) {
            return obj instanceof WrappedConsumerListener<?> that
                    && this.eventType == that.eventType
                    && this.priority == that.priority
                    && this.alwaysCancelling == that.alwaysCancelling
                    && this.consumer.equals(that.consumer);
        }

        @Override
        public int hashCode() {
            return eventType.hashCode()
                    * 31 + priority
                    * 31 + Boolean.hashCode(alwaysCancelling)
                    * 31 + consumer.hashCode();
        }
    }
}
