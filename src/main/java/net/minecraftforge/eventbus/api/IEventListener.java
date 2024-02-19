/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;


/**
 * Event listeners are wrapped with implementations of this interface
 */
public interface IEventListener {
    void invoke(IEvent event);

    default String listenerName() {
        return getClass().getName();
    }
}
