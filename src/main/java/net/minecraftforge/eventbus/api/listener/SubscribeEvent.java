/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.listener;

import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {
    /**
     * The priority of this listener. Listeners with higher priority numbers are called first.
     * @see Priority
     */
    byte priority() default Priority.NORMAL;

    /**
     * If the event is cancellable, setting this to true will make the listener always cancel the event.
     *
     * @implSpec If true, the annotated method must return {@code void} and the event must implement {@link Cancellable}.
     */
    boolean alwaysCancelling() default false;
}
