package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractEventListenerTest {
	@Test
	void eventHandlersCanSubscribeToAbstractEvents() {
		IEventBus bus = BusBuilder.builder().build();
		AtomicBoolean abstractSuperEventHandled = new AtomicBoolean(false);
		AtomicBoolean subEventHandled = new AtomicBoolean(false);
		bus.addListener(EventPriority.NORMAL, false, AbstractSuperEvent.class, (event) -> {
			abstractSuperEventHandled.set(true);
		});
		bus.addListener(EventPriority.NORMAL, false, ConcreteSubEvent.class, (event) -> {
			subEventHandled.set(true);
		});

		bus.post(new ConcreteSubEvent());

		assertTrue(abstractSuperEventHandled.get(), "handled abstract super event");
		assertTrue(subEventHandled.get(), "handled concrete sub event");
	}

	public static abstract class AbstractSuperEvent extends Event {

	}

	public static class ConcreteSubEvent extends AbstractSuperEvent {

	}
}
