package net.minecraftforge.eventbus.benchmarks.compiled;


import net.minecraftforge.eventbus.api.Event;

import java.util.function.Supplier;

public class SimpleEvent extends Event
{
    public static Supplier<Event> makeNew = SimpleEvent::new;
}
