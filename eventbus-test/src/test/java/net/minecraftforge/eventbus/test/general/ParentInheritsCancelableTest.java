/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.test.general;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.test.ITestHandler;

public class ParentInheritsCancelableTest implements ITestHandler {
    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(SuperEvent.class);

        IEventBus bus = builder.get().build();

        // Register a Non-Cancelable ASMEventHandler to the SuperEvent. Which should result in an Unchecked handler being registered
        Listener listener = new Listener();
        bus.register(listener);

         // Test that it gets invoked
        bus.post(new SuperEvent());
        assertTrue(listener.invoked, "Handler was not invoked for SuperEvent");
        listener.invoked = false;

        // Now we classload the Cancelable event
        validator.accept(SubEvent.class);

        // Lambda handler should be invoked, and so should the normal listener
        AtomicBoolean handled = new AtomicBoolean(false);
        bus.addListener(EventPriority.NORMAL, false, SubEvent.class, (SubEvent event) -> {
        	handled.set(true);
        });

        bus.post(new SubEvent());
        assertTrue(listener.invoked, "Handler was not invoked for SubEvent");
        listener.invoked = false;
        assertTrue(handled.getAndSet(false), "Lambda Handler was not invoked for SubEvent");

        // And finally lets add a listener that cancels the event, its registered after the first lambda, so that should be called, but it cancels so the ASMEventHandler in Super shouldn't be
        AtomicBoolean canceled = new AtomicBoolean(false);
        bus.addListener(EventPriority.NORMAL, false, SubEvent.class, (SubEvent event) -> {
        	canceled.set(true);
        	event.setCanceled(true);
        });


        bus.post(new SubEvent());
        assertTrue(handled.get(), "Lambda Handler was not invoked for SubEvent");
        assertTrue(canceled.get(), "Canceling Handler was not invoked for SubEvent");
        assertFalse(listener.invoked, "Handler was invoked for canceled SubEvent");
    }

    public static class SuperEvent extends Event {}

    @Cancelable
    public static class SubEvent extends SuperEvent {}

    public static class Listener {
    	private boolean invoked = false;

    	@SubscribeEvent
    	public void listener(SuperEvent event) {
    		invoked = true;
    	}
    }
}
