package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventListener;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

class EventBusErrorMessage implements Message, StringBuilderFormattable {
    private final Event event;
    private final int index;
    private final IEventListener[] listeners;
    private final Throwable throwable;

    public EventBusErrorMessage(final Event event, final int index, final IEventListener[] listeners, final Throwable throwable) {
        this.event = event;
        this.index = index;
        this.listeners = listeners;
        this.throwable = throwable;
    }

    @Override
    public String getFormattedMessage() {
        return null;
    }

    @Override
    public String getFormat() {
        return "";
    }

    @Override
    public Object[] getParameters() {
        return new Object[] { this.event, this.index, this.listeners };
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        buffer.
                append("Exception caught during firing event\n").
                append("Index: ").append(index).append('\n').
                append("Listeners:");
        for (int x = 0; x < listeners.length; x++)
        {
            buffer.append(x).append(": ").append(listeners[x]).append('\n');
        }
    }
}
