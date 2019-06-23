package net.minecraftforge.eventbus.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventTag;
import net.minecraftforge.eventbus.api.IEventBus;

public class EventTagTests {
    
    private static final String TAG1 = "Foo";
    private static final String TAG2 = "Bar";
    
    private static class UntaggedEvent extends Event {}

    @EventTag(TAG1)
    private static class TaggedEvent extends Event {}

    @EventTag(TAG1)
    @EventTag(TAG2)
    private static class MultiTaggedEvent extends Event {}
    
    private static void nonThrowingPost(IEventBus bus, Event event) {
        assertDoesNotThrow(() -> bus.post(event));
    }
    
    private static void throwingPost(IEventBus bus, Event event) {
        assertThrows(IllegalArgumentException.class, () -> bus.post(event));
    }
    
    @Test
    public void testUntaggedEventUntaggedBus() {
        nonThrowingPost(new BusBuilder().build(), new UntaggedEvent());
    }
    
    @Test
    public void testUntaggedEventTaggedBus() {
        nonThrowingPost(new BusBuilder().addTag(TAG1).build(), new UntaggedEvent());
    }
    
    @Test
    public void testUntaggedEventMultiTaggedBus() {
        nonThrowingPost(new BusBuilder().addTag(TAG1).addTag(TAG2).build(), new UntaggedEvent());
    }
    
    @Test
    public void testTaggedEventUntaggedBus() {
        throwingPost(new BusBuilder().build(), new TaggedEvent());
    }
    
    @Test
    public void testTaggedEventTaggedBus() {
        nonThrowingPost(new BusBuilder().addTag(TAG1).build(), new TaggedEvent());
    }

    @Test
    public void testTaggedEventMultiTaggedBus() {
        nonThrowingPost(new BusBuilder().addTag(TAG1).addTag(TAG2).build(), new TaggedEvent());
    }
    
    @Test
    public void testMultiTaggedEventUntaggedBus() {
        throwingPost(new BusBuilder().build(), new MultiTaggedEvent());
    }
    
    @Test
    public void testMultiTaggedEventTaggedBus() {
        throwingPost(new BusBuilder().addTag(TAG1).build(), new MultiTaggedEvent());
    }

    @Test
    public void testMultiTaggedEventMultiTaggedBus() {
        nonThrowingPost(new BusBuilder().addTag(TAG1).addTag(TAG2).build(), new MultiTaggedEvent());
    }
    
    public void consumeUntaggedEvent(UntaggedEvent event) {}
    public void consumeTaggedEvent(TaggedEvent event) {}
    public void consumeMultiTaggedEvent(MultiTaggedEvent event) {}
    
    private static <T extends Event> void nonThrowingSubscribe(IEventBus bus, Consumer<T> listener) {
        assertDoesNotThrow(() -> bus.addListener(listener));
    }
    
    private static <T extends Event> void throwingSubscribe(IEventBus bus, Consumer<T> listener) {
        assertThrows(IllegalArgumentException.class, () -> bus.addListener(listener));
    }
    
    @Test
    public void testUntaggedListenerUntaggedBus() {
        nonThrowingSubscribe(new BusBuilder().build(), this::consumeUntaggedEvent);
    }
    
    @Test
    public void testUntaggedListenerTaggedBus() {
        nonThrowingSubscribe(new BusBuilder().addTag(TAG1).build(), this::consumeUntaggedEvent);
    }
    
    @Test
    public void testUntaggedListenerMultiTaggedBus() {
        nonThrowingSubscribe(new BusBuilder().addTag(TAG1).addTag(TAG2).build(), this::consumeUntaggedEvent);
    }
    
    @Test
    public void testTaggedListenerUntaggedBus() {
        throwingSubscribe(new BusBuilder().build(), this::consumeTaggedEvent);
    }
    
    @Test
    public void testTaggedListenerTaggedBus() {
        nonThrowingSubscribe(new BusBuilder().addTag(TAG1).build(), this::consumeTaggedEvent);
    }
    
    @Test
    public void testTaggedListenerMultiTaggedBus() {
        nonThrowingSubscribe(new BusBuilder().addTag(TAG1).addTag(TAG2).build(), this::consumeTaggedEvent);
    }
    
    @Test
    public void testMultiTaggedListenerUntaggedBus() {
        throwingSubscribe(new BusBuilder().build(), this::consumeMultiTaggedEvent);
    }
    
    @Test
    public void testMultiTaggedListenerTaggedBus() {
        throwingSubscribe(new BusBuilder().addTag(TAG1).build(), this::consumeMultiTaggedEvent);
    }
    
    @Test
    public void testMultiTaggedListenerMultiTaggedBus() {
        nonThrowingSubscribe(new BusBuilder().addTag(TAG1).addTag(TAG2).build(), this::consumeMultiTaggedEvent);
    }
}
