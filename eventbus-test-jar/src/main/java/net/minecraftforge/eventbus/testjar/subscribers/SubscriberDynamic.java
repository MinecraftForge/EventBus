/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.subscribers;


import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.testjar.benchmarks.ClassFactory;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;

public class SubscriberDynamic {
    @SubscribeEvent
    public void onCancelableEvent(CancelableEvent event) {}

    @SubscribeEvent
    public void onResultEvent(ResultEvent event) { }

    @SubscribeEvent
    public void onSimpleEvent(EventWithData event) { }

    public static class Factory {
        public static final ClassFactory<Consumer<IEventBus>> REGISTER = new ClassFactory<>(
                SubscriberDynamic.class,
                MethodHandles.lookup(),
                cls -> {
                    var inst = cls.getConstructor().newInstance();
                    return bus -> bus.register(inst);
                }
        );
    }
}
