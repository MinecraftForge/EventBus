package net.minecraftforge.eventbus.test.general;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.test.ITestHandler;

import static org.junit.jupiter.api.Assertions.*;

public abstract class EventBusSubtypeFilterTest implements ITestHandler {
    public interface MarkerEvent {}
    public static class BaseEvent extends Event implements MarkerEvent {}
    public static class OtherEvent extends Event {}

    @Override
    public void before(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(BaseEvent.class);
        validator.accept(OtherEvent.class);
    }

    private static IEventBus bus(Supplier<BusBuilder> builder) {
        return builder.get().markerType(MarkerEvent.class).build();
    }

    private static IEventBus busCheck(Supplier<BusBuilder> builder) {
        return builder.get().markerType(MarkerEvent.class).checkTypesOnDispatch().build();
    }

    public static class Valid extends EventBusSubtypeFilterTest {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            IEventBus bus = busCheck(builder);
            assertDoesNotThrow(() -> bus.addListener((BaseEvent e) -> {}));
            assertDoesNotThrow(() -> bus.post(new BaseEvent()));
        }
    }

    public static class Invalid extends EventBusSubtypeFilterTest {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            IEventBus bus = busCheck(builder);
            assertThrows(IllegalArgumentException.class, () -> bus.addListener((OtherEvent e) -> {}));
            assertThrows(IllegalArgumentException.class, () -> bus.post(new OtherEvent()));
        }
    }

    public static class InvalidNoDispatch extends EventBusSubtypeFilterTest {
        @Override
        public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
            IEventBus bus = bus(builder);
            assertThrows(IllegalArgumentException.class, () -> bus.addListener((OtherEvent e) -> {}));
            assertDoesNotThrow(() -> bus.post(new OtherEvent()));
        }
    }
}
