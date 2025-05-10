/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.internal.EventCharacteristic;

/**
 * A self-destructing event will {@link BusGroup#dispose() dispose} of its associated {@link EventBus} after it has
 * been posted to free up resources, after which it cannot be posted to again.
 * <p>This is useful for single-use lifecycle events.</p>
 *
 * @apiNote The dispose action is similar to {@link BusGroup#dispose()}, but applies to only this event rather than to
 *          all events in the group.
 */
public non-sealed interface SelfDestructing extends EventCharacteristic {}
