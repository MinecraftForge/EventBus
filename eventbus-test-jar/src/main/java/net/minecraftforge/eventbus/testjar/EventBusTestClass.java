/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.testjar;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.lang.invoke.MethodHandles;

public class EventBusTestClass {
    public boolean HIT1 = false;
    public boolean HIT2 = false;

    public void register(BusGroup busGroup) {
        busGroup.register(MethodHandles.lookup(), this);
    }

    @SubscribeEvent
    public void eventMethod(DummyEvent evt) {
        HIT1 = true;
    }

    @SubscribeEvent
    public void eventMethod2(DummyEvent.GoodEvent evt) {
        HIT2 = true;
    }

    @SubscribeEvent
    public void evtMethod3(DummyEvent.CancellableEvent evt) {

    }

    @SubscribeEvent
    public void evtMethod4(DummyEvent.ResultEvent evt) {

    }


    @SubscribeEvent
    public void badEventMethod(DummyEvent.BadEvent evt) {
        throw new RuntimeException("BARF");
    }
}
