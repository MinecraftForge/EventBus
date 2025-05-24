/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
import org.jspecify.annotations.NullMarked;

@NullMarked
module net.minecraftforge.eventbus {
    requires java.logging;
    requires org.jspecify;

    exports net.minecraftforge.eventbus.api.bus;
    exports net.minecraftforge.eventbus.api.event;
    exports net.minecraftforge.eventbus.api.event.characteristic;
    exports net.minecraftforge.eventbus.api.listener;
}
