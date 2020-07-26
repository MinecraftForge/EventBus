package net.minecraftforge.eventbus.test;

import java.lang.reflect.Type;
import java.util.List;

import net.minecraftforge.eventbus.api.BusBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class WeirdGenericTests {
	
	boolean genericEventHandled = false;
	
	@Test
	public void testGenericListener() {
		IEventBus bus = BusBuilder.builder().build();
		bus.addGenericListener(List.class, this::handleGenericEvent);
		bus.post(new GenericEvent<List<String>>() {
			public Type getGenericType() {
				return List.class;
			}
		});
		Assertions.assertTrue(genericEventHandled);
	}
	
	@Test
	public void testGenericListenerRegisteredIncorrectly() {
	    IEventBus bus = BusBuilder.builder().build();
	    Assertions.assertThrows(IllegalArgumentException.class, () -> bus.addListener(this::handleGenericEvent));
	}

	private void handleGenericEvent(GenericEvent<List<String>> evt) {
		genericEventHandled = true;
	}

}
