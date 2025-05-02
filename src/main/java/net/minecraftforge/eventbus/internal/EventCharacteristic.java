/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.event.characteristic.MonitorAware;
import net.minecraftforge.eventbus.api.event.characteristic.SelfDestructing;
import net.minecraftforge.eventbus.api.event.characteristic.SelfPosting;

public sealed interface EventCharacteristic permits Cancellable, MonitorAware, SelfDestructing, SelfPosting {}
