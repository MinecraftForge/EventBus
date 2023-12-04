/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

/**
 * Different priorities for {@link Event} listeners.
 *
 * {@link #NORMAL} is the default level for a listener registered without a priority.
 *
 * @see SubscribeEvent#priority()
 */
public enum EventPriority implements IEventListener {
    /**
     * Priority of event listeners, listeners will be sorted with respect to this priority level.
     *
     * Note:
     *   Due to using a ArrayList in the ListenerList,
     *   these need to stay in a contiguous index starting at 0. {Default ordinal}
     */
    HIGHEST, //First to execute
    HIGH,
    NORMAL,
    LOW,
    LOWEST,
    /**
     * When in this state, {@link Event#setCanceled(boolean)} will throw an exception if called with any value.
     */
    MONITOR; //Last to execute

    @Override
    public void invoke(Event event) {
        event.setPhase(this);
    }
}
