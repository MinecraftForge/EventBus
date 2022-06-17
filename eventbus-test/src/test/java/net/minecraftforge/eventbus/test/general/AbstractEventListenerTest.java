package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.test.ITestHandler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractEventListenerTest implements ITestHandler {
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(AbstractSuperEvent.class);
        validator.accept(AbstractSubEvent.class);

        IEventBus bus = builder.get().build();
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
        assertTrue(AbstractSubEvent.MERGED_STATIC_INIT == 100, "static init merge failed");
    }

    /*
     * Below, we simulate the things that are added by EventSubclassTransformer
     * to show that it will work alongside the static listener map.
     * We do not use the field name LISTNER_LIST as that's how we tell if the transformer has run
     */
    public static abstract class AbstractSuperEvent extends Event {}

    public static class ConcreteSuperEvent extends AbstractSuperEvent {
        private static ListenerList LISTENERS = new ListenerList(EventListenerHelper.getListenerList(ConcreteSuperEvent.class.getSuperclass()));
        public ConcreteSuperEvent() {}

        @Override
        public ListenerList getListenerList()
        {
            return LISTENERS;
        }
    }

    // In transformed world, this will have a 'LISTENER_LIST' injected.
    // Make sure that it merges static init instead of overwrites
    public static class AbstractSubEvent extends ConcreteSuperEvent {
        protected static int MERGED_STATIC_INIT = 100;
    }

    public static class ConcreteSubEvent extends AbstractSubEvent {
        private static ListenerList LISTENERS = new ListenerList(EventListenerHelper.getListenerList(ConcreteSubEvent.class.getSuperclass()));

        public ConcreteSubEvent() {}

        @Override
        public ListenerList getListenerList()
        {
            return LISTENERS;
        }
    }

}
