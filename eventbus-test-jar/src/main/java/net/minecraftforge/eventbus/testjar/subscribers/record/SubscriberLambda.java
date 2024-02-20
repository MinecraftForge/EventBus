/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.subscribers.record;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.benchmarks.ClassFactory;
import net.minecraftforge.eventbus.testjar.events.RecordEvent;

import java.util.function.Consumer;

public class SubscriberLambda {
    public static Consumer<IEventBus> register = SubscriberLambda::register;
    public static void register(IEventBus bus) {
        bus.addListener(SubscriberLambda::onEvent);
    }

    public static void onEvent(RecordEvent event) {}

    public static class Factory {
        @SuppressWarnings("unchecked")
        public static final ClassFactory<Consumer<IEventBus>> REGISTER = new ClassFactory<>("net.minecraftforge.eventbus.testjar.subscribers.record.SubscriberLambda", cls -> (Consumer<IEventBus>)cls.getDeclaredField("register").get(null));
    }
}
