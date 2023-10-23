/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker annotation indicating the event type is able to be cancelled using {@link Event#setCanceled(boolean)}
 */
@Retention(value = RUNTIME)
@Target(value = TYPE)
public @interface Cancelable{}
