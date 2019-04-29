package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.ListenerList;
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
		AtomicBoolean concreteSuperEventHandled = new AtomicBoolean(false);
		AtomicBoolean abstractSubEventHandled = new AtomicBoolean(false);
		AtomicBoolean concreteSubEventHandled = new AtomicBoolean(false);
		bus.addListener(EventPriority.NORMAL, false, AbstractSuperEvent.class, (event) -> abstractSuperEventHandled.set(true));
		bus.addListener(EventPriority.NORMAL, false, ConcreteSuperEvent.class, (event) -> concreteSuperEventHandled.set(true));
		bus.addListener(EventPriority.NORMAL, false, AbstractSubEvent.class, (event) -> abstractSubEventHandled.set(true));
		bus.addListener(EventPriority.NORMAL, false, ConcreteSubEvent.class, (event) -> concreteSubEventHandled.set(true));

		bus.post(new ConcreteSubEvent());

		assertTrue(abstractSuperEventHandled.get(), "handled abstract super event");
		assertTrue(concreteSuperEventHandled.get(), "handled concrete super event");
		assertTrue(abstractSubEventHandled.get(), "handled abstract sub event");
		assertTrue(concreteSubEventHandled.get(), "handled concrete sub event");
	}

	// Below, we simulate the things that are added by EventSubclassTransformer
	// to show that it will work alongside the static listener map.

	public static abstract class AbstractSuperEvent extends Event {

	}

	public static class ConcreteSuperEvent extends AbstractSuperEvent {

		private static ListenerList LISTENER_LIST;
		public ConcreteSuperEvent() {}

		@Override
		protected void setup()
		{
			super.setup();
			if (LISTENER_LIST != null)
				return;
			LISTENER_LIST = new ListenerList(super.getListenerList());
		}

		@Override
		public ListenerList getListenerList()
		{
			return LISTENER_LIST;
		}
	}

	public static class AbstractSubEvent extends ConcreteSuperEvent {

	}

	public static class ConcreteSubEvent extends AbstractSubEvent {
		private static ListenerList LISTENER_LIST;
		public ConcreteSubEvent() {}

		@Override
		protected void setup()
		{
			super.setup();
			if (LISTENER_LIST != null)
				return;
			LISTENER_LIST = new ListenerList(super.getListenerList());
		}

		@Override
		public ListenerList getListenerList()
		{
			return LISTENER_LIST;
		}
	}

}
