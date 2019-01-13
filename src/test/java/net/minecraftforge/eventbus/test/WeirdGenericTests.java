package net.minecraftforge.eventbus.test;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class WeirdGenericTests {
	
	boolean genericEventHandled = false;
	
	@Test
	public void testGenericListener() {
		IEventBus bus = new EventBus();
		bus.addGenericListener(List.class, this::handleGenericEvent);
		bus.post(new GenericEvent<List<String>>() {
			public Type getGenericType() {
				return List.class;
			};
		});
		Assertions.assertTrue(genericEventHandled);
	}

	private void handleGenericEvent(GenericEvent<List<String>> evt) {
		genericEventHandled = true;
	}

}
