package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DeadlockingEventArmsLength {
    public static class DummyEvent extends Event {}
    public static class ParentEvent extends Event {}
    public static class ChildEvent extends ParentEvent {
        static {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Listener1 {
        @SubscribeEvent
        public static void listen(ChildEvent evt) {}
    }
}
