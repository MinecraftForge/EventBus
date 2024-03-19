/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.subscribers.record;


import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.testjar.benchmarks.ClassFactory;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.RecordEvent;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;

import java.util.function.Consumer;

public class SubscriberDynamic {
    @SubscribeEvent
    public void onEvent(RecordEvent event) {}

    public static class Factory {
        public static final ClassFactory<Consumer<IEventBus>> REGISTER = new ClassFactory<>("net.minecraftforge.eventbus.testjar.subscribers.record.SubscriberDynamic", cls -> {
            var inst = cls.getConstructor().newInstance();
            return bus -> bus.register(inst);
        });
    }
}
