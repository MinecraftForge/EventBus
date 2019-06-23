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
    
    private static IEventBus busWithTags(String... tags) {
        BusBuilder builder = new BusBuilder().checkTagsOnPost();
        for (String tag : tags) {
            builder.addTag(tag);
        }
        return builder.build();
    }
    
    @Test
    public void testUntaggedEventUntaggedBus() {
        nonThrowingPost(busWithTags(), new UntaggedEvent());
    }
    
    @Test
    public void testUntaggedEventTaggedBus() {
        nonThrowingPost(busWithTags(TAG1), new UntaggedEvent());
    }
    
    @Test
    public void testUntaggedEventMultiTaggedBus() {
        nonThrowingPost(busWithTags(TAG1, TAG2), new UntaggedEvent());
    }
    
    @Test
    public void testTaggedEventUntaggedBus() {
        throwingPost(busWithTags(), new TaggedEvent());
    }
    
    @Test
    public void testTaggedEventTaggedBus() {
        nonThrowingPost(busWithTags(TAG1), new TaggedEvent());
    }

    @Test
    public void testTaggedEventMultiTaggedBus() {
        nonThrowingPost(busWithTags(TAG1, TAG2), new TaggedEvent());
    }
    
    @Test
    public void testMultiTaggedEventUntaggedBus() {
        throwingPost(busWithTags(), new MultiTaggedEvent());
    }
    
    @Test
    public void testMultiTaggedEventTaggedBus() {
        throwingPost(busWithTags(TAG1), new MultiTaggedEvent());
    }

    @Test
    public void testMultiTaggedEventMultiTaggedBus() {
        nonThrowingPost(busWithTags(TAG1, TAG2), new MultiTaggedEvent());
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
        nonThrowingSubscribe(busWithTags(), this::consumeUntaggedEvent);
    }
    
    @Test
    public void testUntaggedListenerTaggedBus() {
        nonThrowingSubscribe(busWithTags(TAG1), this::consumeUntaggedEvent);
    }
    
    @Test
    public void testUntaggedListenerMultiTaggedBus() {
        nonThrowingSubscribe(busWithTags(TAG1, TAG2), this::consumeUntaggedEvent);
    }
    
    @Test
    public void testTaggedListenerUntaggedBus() {
        throwingSubscribe(busWithTags(), this::consumeTaggedEvent);
    }
    
    @Test
    public void testTaggedListenerTaggedBus() {
        nonThrowingSubscribe(busWithTags(TAG1), this::consumeTaggedEvent);
    }
    
    @Test
    public void testTaggedListenerMultiTaggedBus() {
        nonThrowingSubscribe(busWithTags(TAG1, TAG2), this::consumeTaggedEvent);
    }
    
    @Test
    public void testMultiTaggedListenerUntaggedBus() {
        throwingSubscribe(busWithTags(), this::consumeMultiTaggedEvent);
    }
    
    @Test
    public void testMultiTaggedListenerTaggedBus() {
        throwingSubscribe(busWithTags(TAG1), this::consumeMultiTaggedEvent);
    }
    
    @Test
    public void testMultiTaggedListenerMultiTaggedBus() {
        nonThrowingSubscribe(busWithTags(TAG1, TAG2), this::consumeMultiTaggedEvent);
    }
}
