package net.minecraftforge.eventbus.benchmarks.compiled;

import net.minecraftforge.eventbus.api.Event;

public class EventWithData extends Event
{
    private final String data;
    private final int foo;
    private final boolean bar;

    public EventWithData(String data, int foo, boolean bar)
    {
        this.data = data;
        this.foo = foo;
        this.bar = bar;
    }

    public int getFoo()
    {
        return foo;
    }

    public String getData()
    {
        return data;
    }

    public boolean isBar()
    {
        return bar;
    }
}
