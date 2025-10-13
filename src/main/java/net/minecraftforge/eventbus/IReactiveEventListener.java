/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.IEventListener;

interface IReactiveEventListener extends IEventListener {
    /**
     * Convert this handler to one that respects canceled states.
     * This is called when we eagerly do the optimization, and then a unknown child adds the ability to be canceled.
     */
	IEventListener toCancelable();
}
