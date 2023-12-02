/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.benchmarks;

import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberDynamic;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberLambda;
import net.minecraftforge.eventbus.testjar.subscribers.SubscriberStatic;
import net.minecraftforge.unsafe.UnsafeFieldAccess;
import net.minecraftforge.unsafe.UnsafeHacks;

public abstract class Register implements Benchmark {
    // This is the number of dynamic classes to make in the setup method.
    // This number just needs to be high enough that we don't get a EmptyStackException
    // It takes a lot of memory, but it prevents optimizations in the EventBus from
    // invalidating out test results. Such optimizations like the duplicate prevention
    // on normal register(Object) calls but not on lambda calls
    private static final int BATCH_COUNT = 100_000;
    protected final Stack<Consumer<IEventBus>> stack = new Stack<>();
    private Supplier<IEventBus> busFactory;
    private IEventBus bus;
    private ClassFactory<Consumer<IEventBus>> factory;

    protected abstract ClassFactory<Consumer<IEventBus>> factory();

    @Override
    public void setup(Supplier<IEventBus> busFactory)  {
        this.busFactory = busFactory;
        factory = factory();
    }

    @Override
    public void setupIteration() {
        while (stack.size() < BATCH_COUNT)
            stack.push(factory.create());
        if (bus != null) {
            @SuppressWarnings("unchecked")
            UnsafeFieldAccess<IEventBus, Map<?, ?>> listeners = UnsafeHacks.findField((Class<IEventBus>)bus.getClass(), "listeners");
            var keys = new HashSet<>(listeners.get(bus).keySet());
            for (var key : keys)
                bus.unregister(key);
        }
        bus = busFactory.get();
        System.gc();
    }

    @Override
    public void run() {
        //System.out.println(getClass().getName() + " " + stack.size());
        stack.pop().accept(bus);
    }

    public static class Lambda extends Register {
        @Override
        protected ClassFactory<Consumer<IEventBus>> factory() {
            return SubscriberLambda.Factory.REGISTER;
        }
    }

    public static class Dynamic extends Register {
        @Override
        protected ClassFactory<Consumer<IEventBus>> factory() {
            return SubscriberDynamic.Factory.REGISTER;
        }
    }

    public static class Static extends Register {
        @Override
        protected ClassFactory<Consumer<IEventBus>> factory() {
            return SubscriberStatic.Factory.REGISTER;
        }
    }
}
