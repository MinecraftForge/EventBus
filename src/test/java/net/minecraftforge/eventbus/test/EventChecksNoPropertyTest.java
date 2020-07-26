package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventChecksNoPropertyTest {
    public interface MarkerEvent {}
    public static class BaseEvent extends Event implements MarkerEvent {

        public BaseEvent() {}
    }

    public static class OtherEvent extends Event {

        public OtherEvent() {}
    }

    private static final String PROP_NAME = "eventbus.checkTypesOnDispatch";

    private static IEventBus bus() {
        return new BusBuilder().markerType(MarkerEvent.class).build();
    }

    @Test
    public void testValidType() {
        IEventBus bus = bus();
        Assertions.assertDoesNotThrow(() -> bus.addListener((BaseEvent e) -> {}));
        Assertions.assertDoesNotThrow(() -> bus.post(new BaseEvent()));
    }

    @Test
    public void testInvalidType() {
        IEventBus bus = bus();
        Assertions.assertThrows(IllegalArgumentException.class, () -> bus.addListener((OtherEvent e) -> {}));
        Assertions.assertDoesNotThrow(() -> bus.post(new OtherEvent()));
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty(PROP_NAME);
    }
}
