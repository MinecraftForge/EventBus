/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.RecordEvent;

/**
 * The internal marker interface for all event base types. This is internal and sealed for many reasons:
 * <ul>
 *     <li>To avoid ambiguity in the system where different events in the inheritance chain may technically be events
 *     but not declare their type</li>
 *     <li>To discourage bad usages by library users that could hurt performance. For example, having all listeners
 *     bounce to a generic void handleEvent(Event e) method and doing chains of instanceof checks would be very
 *     slow.</li>
 *     <li>To minimise the public API surface - the more we have to support, the more restricted and complicated
 *     internals become</li>
 * </ul>
 *
 * <p>Library users should use one of the three base types instead:</p>
 * <ul>
 *     <li>{@link MutableEvent} for classes</li>
 *     <li>{@link RecordEvent} for records</li>
 *     <li>{@link InheritableEvent} for interfaces</li>
 * </ul>
 */
public sealed interface Event permits InheritableEvent, MutableEvent, RecordEvent {}
