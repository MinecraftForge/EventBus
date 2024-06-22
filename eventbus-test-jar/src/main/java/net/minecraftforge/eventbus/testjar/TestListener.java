/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar;

import net.minecraftforge.eventbus.api.IEvent;
import net.minecraftforge.eventbus.api.IEventListener;

public class TestListener implements IEventListener {
    private Object instance;

    TestListener(Object instance) {
        this.instance = instance;
    }

    @Override
    public void invoke(final IEvent event) {
        instance.equals(event);
    }
}
