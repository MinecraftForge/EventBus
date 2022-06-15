package net.minecraftforge.eventbus.benchmarks.compiled;

import net.minecraftforge.eventbus.api.Event;

public class EventWithData extends Event
{
    private final String data;
    private final int foo;
    private final boolean bar;

    // This constructor is needed for events if they do not pass through the transformer
    public EventWithData()
    {
        this(null, 0, false);
    }

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
