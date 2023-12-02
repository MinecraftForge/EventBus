/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.benchmarks;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.testjar.events.ResultEvent;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberDynamic;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberLambda;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberStatic;

public abstract class Post implements Benchmark {
    protected IEventBus bus;

    @Override
    public void setup(Supplier<IEventBus> busFactory) {
        this.bus = busFactory.get();
        register();
    }

    protected abstract void register();

    @Override
    public void run() {
        bus.post(new CancelableEvent());
        bus.post(new ResultEvent());
        bus.post(new EventWithData("Foo", 5, true)); //Some example data
    }

    private static abstract class Dozen extends Post {
        @Override
        protected void register() {
            var factory = factory();
            for (int x = 0; x < 12; x++)
                factory.accept(bus);
        }

        protected abstract Consumer<IEventBus> factory();
    }

    private static abstract class Hundred extends Post {
        @Override
        protected void register() {
            var factory = factory();
            for (int x = 0; x < 100; x++)
                factory.accept(bus);
        }

        protected abstract Consumer<IEventBus> factory();
    }


    public static class Mixed {
        private static final Consumer<IEventBus> register = new Consumer<>() {
            private int idx = 0;
            @Override
            public void accept(IEventBus bus) {
                switch (idx++ % 3) {
                    case 0: SubscriberDynamic.Factory.REGISTER.create().accept(bus); break;
                    case 1: SubscriberStatic.Factory.REGISTER.create().accept(bus); break;
                    case 2: SubscriberLambda.Factory.REGISTER.create().accept(bus); break;
                }
            }
        };

        public static class Single extends Post {
            @Override
            protected void register() {
                SubscriberDynamic.Factory.REGISTER.create().accept(bus);
                SubscriberStatic.Factory.REGISTER.create().accept(bus);
                SubscriberLambda.Factory.REGISTER.create().accept(bus);
            }
        }

        public static class Dozen extends Post.Dozen {
            @Override
            protected Consumer<IEventBus> factory() {
                return register;
            }
        }

        public static class Hundred extends Post.Hundred {
            @Override
            protected Consumer<IEventBus> factory() {
                return register;
            }
        }
    }

    public static class Dynamic {
        public static class Single extends Post {
            @Override
            protected void register() {
                SubscriberDynamic.Factory.REGISTER.create().accept(bus);
            }
        }

        public static class Dozen extends Post.Dozen {
            @Override
            protected Consumer<IEventBus> factory() {
                return bus -> SubscriberDynamic.Factory.REGISTER.create().accept(bus);
            }
        }

        public static class Hundred extends Post.Hundred {
            @Override
            protected Consumer<IEventBus> factory() {
                return bus -> SubscriberDynamic.Factory.REGISTER.create().accept(bus);
            }
        }
    }

    public static class Lambda {
        public static class Single extends Post {
            @Override
            protected void register() {
                SubscriberLambda.Factory.REGISTER.create().accept(bus);
            }
        }

        public static class Dozen extends Post.Dozen {
            @Override
            protected Consumer<IEventBus> factory() {
                return bus -> SubscriberLambda.Factory.REGISTER.create().accept(bus);
            }
        }

        public static class Hundred extends Post.Hundred {
            @Override
            protected Consumer<IEventBus> factory() {
                return bus -> SubscriberLambda.Factory.REGISTER.create().accept(bus);
            }
        }
    }

    public static class Static {
        public static class Single extends Post {
            @Override
            protected void register() {
                SubscriberStatic.Factory.REGISTER.create().accept(bus);
            }
        }

        public static class Dozen extends Post.Dozen {
            @Override
            protected Consumer<IEventBus> factory() {
                return bus -> SubscriberStatic.Factory.REGISTER.create().accept(bus);
            }
        }

        public static class Hundred extends Post.Hundred {
            @Override
            protected Consumer<IEventBus> factory() {
                return bus -> SubscriberStatic.Factory.REGISTER.create().accept(bus);
            }
        }
    }
}
