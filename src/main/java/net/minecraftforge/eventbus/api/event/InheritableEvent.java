/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event;

import net.minecraftforge.eventbus.internal.Event;

/**
 * A hybrid of an event base type and characteristic - implementing this interface on your event type allows for event
 * posting on subclasses to propagate to your event type, essentially opting into event inheritance. This allows for
 * flexible event hierarchies with mixed levels of encapsulation and mutability.
 */
public non-sealed interface InheritableEvent extends Event {}
