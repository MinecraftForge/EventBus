package net.minecraftforge.eventbus.testjar;

import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TestSubscriber
{

    @SubscribeEvent
    public static void onStaticTestEvent(StaticInitTestEvent.SubEvent event)
    {
    }
}
