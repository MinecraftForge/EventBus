package net.minecraftforge.eventbus.testjar;

import net.minecraftforge.eventbus.api.Event;

public class StaticInitTestEvent extends Event
{

    public static class SubEvent extends StaticInitTestEvent
    {
        static {
            try
            {
                Thread.sleep(1);
            } catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
