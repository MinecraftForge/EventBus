/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.listener;

/**
 * Some common priority values, spread out evenly across the range of a Java signed byte, factoring in the special
 * {@link Priority#MONITOR} priority.
 */
public final class Priority {
    private Priority() {}

    /**
     * Runs first
     */
    public static final byte HIGHEST = Byte.MAX_VALUE;

    /**
     * Runs before {@link #NORMAL} but after {@link #HIGHEST}
     */
    public static final byte HIGH = 64;

    /**
     * The default priority
     */
    public static final byte NORMAL = 0;

    /**
     * Runs after {@link #NORMAL} but before {@link #LOWEST}
     */
    public static final byte LOW = -64;

    /**
     * The last priority that can mutate the event instance
     */
    public static final byte LOWEST = Byte.MIN_VALUE + 1;

    /**
     * A special priority that is only used for monitoring purposes and typically doesn't allow cancelling or mutation.
     * <p>Monitoring listeners are always called last - even if the event is cancelled.</p>
     */
    public static final byte MONITOR = Byte.MIN_VALUE;
}
