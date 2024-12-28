package net.minecraftforge.eventbus.testjar.subscribers;

import net.minecraftforge.eventbus.api.IEventBus;

import java.util.function.Consumer;

public class SubscriberMixed {
    public static class Factory {
        public static Consumer<IEventBus> create() {
            return new Consumer<>() {
                private int idx = 0;

                @Override
                public void accept(IEventBus eventBus) {
                    var registrar = switch (idx++ % 3) {
                        case 0 -> SubscriberDynamic.Factory.REGISTER.create();
                        case 1 -> SubscriberStatic.Factory.REGISTER.create();
                        case 2 -> SubscriberLambda.Factory.REGISTER.create();
                        default -> throw new AssertionError();
                    };
                    registrar.accept(eventBus);
                }
            };
        }
    }
}
