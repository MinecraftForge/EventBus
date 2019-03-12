package net.minecraftforge.eventbus.benchmarks.compiled;


import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.Supplier;

@Cancelable
public class CancelableEvent extends Event
{
    public static Supplier<Event> makeNew = CancelableEvent::new;
}
