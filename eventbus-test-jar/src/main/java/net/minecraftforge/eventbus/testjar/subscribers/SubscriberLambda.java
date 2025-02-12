/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.testjar.subscribers;

import net.minecraftforge.eventbus.testjar.benchmarks.ClassFactory;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;

import java.lang.invoke.MethodHandles;

public final class SubscriberLambda {
    // For posting benchmarks via the ClassFactory
    public static final Runnable register = SubscriberLambda::register;

    // For registration benchmarks
    public static void register() {
        CancelableEvent.BUS.addListener(SubscriberLambda::onCancelableEvent);
        ResultEvent.BUS.addListener(SubscriberLambda::onResultEvent);
        EventWithData.BUS.addListener(SubscriberLambda::onSimpleEvent);
    }

    public static void onCancelableEvent(CancelableEvent event) { }

    public static void onResultEvent(ResultEvent event) { }

    public static void onSimpleEvent(EventWithData event) { }

    public static class Factory {
        public static final ClassFactory<Runnable> REGISTER = new ClassFactory<>(
                SubscriberLambda.class,
                MethodHandles.lookup(),
                (lookup, cls) ->
                        (Runnable) lookup.findStaticVarHandle(cls, "register", Runnable.class)
                                .withInvokeExactBehavior().get()
        );
    }
}
