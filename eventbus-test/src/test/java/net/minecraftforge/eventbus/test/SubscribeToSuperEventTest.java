package net.minecraftforge.eventbus.test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

import org.junit.jupiter.api.Test;

public class SubscribeToSuperEventTest {

	@Test
	void eventHandlersCanSubscribeToSuperEvents() {
		IEventBus bus = BusBuilder.builder().build();
		AtomicBoolean superEventHandled = new AtomicBoolean(false);
		AtomicBoolean subEventHandled = new AtomicBoolean(false);
		bus.addListener(EventPriority.NORMAL, false, SuperEvent.class, (event) -> {
			Class<? extends SuperEvent> eventClass = event.getClass();
			if (eventClass == SuperEvent.class) {
				superEventHandled.set(true);
			} else if (eventClass == SubEvent.class) {
				subEventHandled.set(true);
			}
		});

		bus.post(new SuperEvent());
		bus.post(new SubEvent());

		assertTrue(superEventHandled.get(), "handled super event");
		assertTrue(subEventHandled.get(), "handled sub event");
	}

	public static class SuperEvent extends Event {

	}

	public static class SubEvent extends SuperEvent {

	}
}
