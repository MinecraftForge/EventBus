package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventListener;

import java.util.function.Supplier;

public class NamedEventListener implements IEventListener {
    public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("eventbus.namelisteners", "false"));
    static IEventListener namedWrapper(IEventListener listener, Supplier<String> name) {
        if (!DEBUG) return listener;
        return new NamedEventListener(listener, name.get());
    }

    private final IEventListener wrap;
    private final String name;

    public NamedEventListener(IEventListener wrap, final String name) {
        this.wrap = wrap;
        this.name = name;
    }

    @Override
    public String listenerName() {
        return this.name;
    }

    @Override
    public void invoke(final Event event) {
        this.wrap.invoke(event);
    }
}
