/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to subscribe a method to an {@link Event}
 *
 * This annotation can only be applied to single parameter methods, where the single parameter is a subclass of
 * {@link Event}.
 *
 * Use {@link IEventBus#register(Object)} to submit either an Object instance or a {@link Class} to the event bus
 * for scanning to generate callback {@link IEventListener} wrappers.
 *
 * The Event Bus system generates an ASM wrapper that dispatches to the marked method.
 */
@Retention(value = RUNTIME)
@Target(value = METHOD)
public @interface SubscribeEvent {
    EventPriority priority() default EventPriority.NORMAL;
    boolean receiveCanceled() default false;
}
