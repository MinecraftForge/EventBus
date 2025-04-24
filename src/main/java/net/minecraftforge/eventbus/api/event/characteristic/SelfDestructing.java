/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.internal.AbstractEventBusImpl;
import net.minecraftforge.eventbus.internal.EventCharacteristic;

/**
 * A self-destructing event will {@link AbstractEventBusImpl#dispose() dispose} of its associated {@link EventBus}
 * after it has been posted to free up resources, after which it cannot be posted to again.
 * <p>This is useful for single-use lifecycle events.</p>
 */
public non-sealed interface SelfDestructing extends EventCharacteristic {}
