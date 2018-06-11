package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EventLambdaTest {
    boolean hit;
    @Test
    public void eventLambda() {
        final IEventBus iEventBus = IEventBus.create();
        iEventBus.addListener((Event e)-> hit = true);
        iEventBus.post(new Event());
        assertTrue(hit, "Hit event");
    }

    public void consumeSubEvent(SubEvent e) {
        hit = true;
    }
    @Test
    void eventSubLambda() {
        final IEventBus iEventBus = IEventBus.create();
        iEventBus.addListener(this::consumeSubEvent);
        iEventBus.post(new SubEvent());
        assertTrue(hit, "Hit subevent");
        hit = false;
        iEventBus.post(new Event());
        assertTrue(!hit, "Didn't hit parent event");
    }

    // faked asm processing for easy testing
    public static class SubEvent extends Event {
        private static ListenerList LISTENER_LIST;


        protected void setup()
        {
            super.setup();
            if (LISTENER_LIST != null)
            {
                return;
            }
            LISTENER_LIST = new ListenerList(super.getListenerList());
        }
        @Override
        public ListenerList getListenerList() {
            return LISTENER_LIST;
        }
    }
}
