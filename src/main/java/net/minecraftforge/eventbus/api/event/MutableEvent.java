/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.event;

import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.internal.MutableEventInternals;

/**
 * For mutable event classes that may also extend other classes and/or support being extended.
 * <p>More advanced techniques like protected fields and methods are also possible, where the supertype may be a
 * protected abstract class with some internals handled for you, but only the concrete types are actual events.</p>
 *
 * <p>Consider {@link RecordEvent} for better performance and conciseness if field mutability and direct extendability
 * aren't needed.</p>
 *
 * @see RecordEvent
 */
public non-sealed abstract class MutableEvent extends MutableEventInternals implements Event {}
