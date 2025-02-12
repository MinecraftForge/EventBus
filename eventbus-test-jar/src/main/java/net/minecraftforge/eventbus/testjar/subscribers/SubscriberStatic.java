/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.testjar.subscribers;

import java.lang.invoke.MethodHandles;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.eventbus.testjar.benchmarks.ClassFactory;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;

public final class SubscriberStatic {
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @SubscribeEvent
    public static void onCancelableEvent(CancelableEvent event) { }

    @SubscribeEvent
    public static void onResultEvent(ResultEvent event) { }

    @SubscribeEvent
    public static void onSimpleEvent(EventWithData event) { }

    public static class Factory {
        public static final ClassFactory<Runnable> REGISTER = new ClassFactory<>(
                SubscriberStatic.class,
                MethodHandles.lookup(),
                (lookup, cls) -> () -> BusGroup.DEFAULT.register(lookup, cls)
        );
    }
}
