/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.subscribers;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.benchmarks.ClassFactory;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;

public class SubscriberLambda {
    public static Consumer<IEventBus> register = SubscriberLambda::register;
    public static void register(IEventBus bus) {
        bus.addListener(SubscriberLambda::onCancelableEvent);
        bus.addListener(SubscriberLambda::onResultEvent);
        bus.addListener(SubscriberLambda::onSimpleEvent);
    }

    public static void onCancelableEvent(CancelableEvent event) { }

    public static void onResultEvent(ResultEvent event) { }

    public static void onSimpleEvent(EventWithData event) { }

    public static class Factory {
        @SuppressWarnings("unchecked")
        public static final ClassFactory<Consumer<IEventBus>> REGISTER = new ClassFactory<>(
                SubscriberLambda.class,
                MethodHandles.lookup(),
                cls -> (Consumer<IEventBus>) cls.getDeclaredField("register").get(null)
        );
    }
}
