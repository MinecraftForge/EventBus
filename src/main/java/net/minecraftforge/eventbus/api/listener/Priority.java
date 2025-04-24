/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.listener;

import net.minecraftforge.eventbus.internal.UtilityInterface;

/**
 * Some common priority values, spread out evenly across the range of a Java signed byte, factoring in the special
 * {@link Priority#MONITOR} priority.
 */
public sealed interface Priority permits UtilityInterface {
    /**
     * Runs first
     */
    byte HIGHEST = Byte.MAX_VALUE;

    /**
     * Runs before {@link #NORMAL} but after {@link #HIGHEST}
     */
    byte HIGH = 64;

    /**
     * The default priority
     */
    byte NORMAL = 0;

    /**
     * Runs after {@link #NORMAL} but before {@link #LOWEST}
     */
    byte LOW = -64;

    /**
     * The last priority that can mutate the event instance
     */
    byte LOWEST = Byte.MIN_VALUE + 1;

    /**
     * A special priority that is only used for monitoring purposes and typically doesn't allow cancelling or mutation.
     * <p>Monitoring listeners are always called last - even if the event is cancelled.</p>
     */
    byte MONITOR = Byte.MIN_VALUE;
}
