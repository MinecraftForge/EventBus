/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
import net.minecraftforge.eventbus.validator.*;

module net.minecraftforge.eventbus.validator {
    requires java.compiler;
    requires jdk.compiler;
    requires net.minecraftforge.eventbus;

    exports net.minecraftforge.eventbus.validator;

    provides javax.annotation.processing.Processor with EventTypeValidator, EventBusValidator, SubscribeEventValidator;
}
