/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event.characteristic;

import net.minecraftforge.eventbus.internal.Event;

/**
 * There are a number of optional characteristics that an {@link Event} can have which may influence their behaviour
 * and the optimisation strategies that can be applied to them.
 */
sealed interface EventCharacteristic permits Cancellable, MonitorAware, SelfDestructing, SelfPosting {}
