package net.minecraftforge.eventbus.testjar;

import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventBusTestClass {
    @SubscribeEvent
    public void eventMethod(DummyEvent evt) {
    }
}
