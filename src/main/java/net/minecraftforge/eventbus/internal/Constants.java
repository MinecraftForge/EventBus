/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.listener.EventListener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Constants {
    private Constants() {}

    static final Consumer<Event> NO_OP_CONSUMER = event -> {};
    static final Predicate<Event> NO_OP_PREDICATE = event -> false;
    static final Predicate<Event> ALWAYS_TRUE_PREDICATE = event -> true;

    static final MethodHandle MH_NULL_CONSUMER = MethodHandles.constant(Consumer.class, null);
    static final MethodHandle MH_NO_OP_CONSUMER = MethodHandles.constant(Consumer.class, NO_OP_CONSUMER);

    static final MethodHandle MH_NULL_PREDICATE = MethodHandles.constant(Predicate.class, null);
    static final MethodHandle MH_NO_OP_PREDICATE = MethodHandles.constant(Predicate.class, NO_OP_PREDICATE);

    static final Comparator<EventListener> PRIORITY_COMPARATOR = (a, b) -> b.priority() - a.priority();

    static final int CHARACTERISTIC_SELF_DESTRUCTING = 1;
    static final int CHARACTERISTIC_MONITOR_AWARE = 2;
    static final int CHARACTERISTIC_CANCELLABLE = 4;
    static final int CHARACTERISTIC_INHERITABLE = 8;

    /**
     * If true, performs exhaustive validation on bulk registration to aid debugging.
     */
    static final boolean STRICT_REGISTRATION_CHECKS = Boolean.getBoolean("eventbus.api.strictRegistrationChecks");

    /**
     * If true, allows the same listener to be registered multiple times. Intended for use in benchmarks only.
     */
    static final boolean ALLOW_DUPE_LISTENERS;
    static {
        String dedupe = System.getProperty("eventbus.internal.dedupeListeners", "true");
        ALLOW_DUPE_LISTENERS = dedupe.equals("false");
        if (ALLOW_DUPE_LISTENERS)
            Logger.getGlobal().logp(
                    Level.WARNING,
                    Constants.class.getName(),
                    "<clinit>()",
                    "Allowing duplicate listeners to be registered. This is intended for use in benchmarks only."
            );
    }

    @SuppressWarnings("unchecked")
    static <T> Consumer<T> getNoOpConsumer() {
        return (Consumer<T>) NO_OP_CONSUMER;
    }

    @SuppressWarnings("unchecked")
    static <T> Predicate<T> getNoOpPredicate(boolean alwaysCancelling) {
        return (Predicate<T>) (alwaysCancelling ? ALWAYS_TRUE_PREDICATE : NO_OP_PREDICATE);
    }

    static boolean isSelfDestructing(int characteristics) {
        return (characteristics & CHARACTERISTIC_SELF_DESTRUCTING) != 0;
    }

    static boolean isMonitorAware(int characteristics) {
        return (characteristics & CHARACTERISTIC_MONITOR_AWARE) != 0;
    }

    static boolean isCancellable(int characteristics) {
        return (characteristics & CHARACTERISTIC_CANCELLABLE) != 0;
    }

    static boolean isInheritable(int characteristics) {
        return (characteristics & CHARACTERISTIC_INHERITABLE) != 0;
    }

    static boolean notInheritable(int characteristics) {
        return (characteristics & CHARACTERISTIC_INHERITABLE) == 0;
    }
}
