/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.EventBus;

/**
 * A self-destructing event will {@linkplain net.minecraftforge.eventbus.api.bus.BusGroup#dispose() dispose} of its
 * associated {@link EventBus} after it has been posted to free up resources.
 * <p>This is useful for single-use lifecycle events.</p>
 *
 * @apiNote Similar to {@link net.minecraftforge.eventbus.api.bus.BusGroup#dispose()}, the posting of this event is a
 * destructive action that will cause its resources to be freed. <strong>It must not be used after it is
 * posted!</strong>
 */
public non-sealed interface SelfDestructing extends EventCharacteristic {}
