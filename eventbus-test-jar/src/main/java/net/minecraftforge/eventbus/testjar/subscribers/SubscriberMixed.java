/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.testjar.subscribers;

import java.util.function.Supplier;

public final class SubscriberMixed {
    public static class Factory {
        public static final Supplier<Runnable> REGISTER = Factory::create;

        private static Runnable create() {
            return new Runnable() {
                private int idx = 0;

                @Override
                public void run() {
                    var registrar = switch (idx++ % 3) {
                        case 0 -> SubscriberDynamic.Factory.REGISTER.create();
                        case 1 -> SubscriberStatic.Factory.REGISTER.create();
                        case 2 -> SubscriberLambda.Factory.REGISTER.create();
                        default -> throw new AssertionError();
                    };
                    registrar.run();
                }
            };
        }
    }
}
