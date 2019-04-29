/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.EventSubclassTransformer;
import net.minecraftforge.eventbus.ListenerList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Base Event class that all other events are derived from
 */
public class Event
{
    @Retention(value = RUNTIME)
    @Target(value = TYPE)
    public @interface HasResult{}

    public enum Result
    {
        DENY,
        DEFAULT,
        ALLOW
    }

    private boolean isCanceled = false;
    private Result result = Result.DEFAULT;
    private static Map<Class<?>, ListenerList> listeners = new IdentityHashMap<>();
    private EventPriority phase = null;

    public Event()
    {
        setup();
    }

    /**
     * Determine if this function is cancelable at all.
     * @return If access to setCanceled should be allowed
     *
     * Note:
     * Events with the Cancelable annotation will have this method automatically added to return true.
     */
    public boolean isCancelable()
    {
        return false;
    }

    /**
     * Determine if this event is canceled and should stop executing.
     * @return The current canceled state
     */
    public boolean isCanceled()
    {
        return isCanceled;
    }

    /**
     * Sets the cancel state of this event. Note, not all events are cancelable, and any attempt to
     * invoke this method on an event that is not cancelable (as determined by {@link #isCancelable}
     * will result in an {@link UnsupportedOperationException}.
     *
     * The functionality of setting the canceled state is defined on a per-event bases.
     *
     * @param cancel The new canceled value
     */
    public void setCanceled(boolean cancel)
    {
        if (!isCancelable())
        {
            throw new UnsupportedOperationException(
                "Attempted to call Event#setCanceled() on a non-cancelable event of type: "
                + this.getClass().getCanonicalName()
            );
        }
        isCanceled = cancel;
    }

    /**
     * Determines if this event expects a significant result value.
     *
     * Note:
     * Events with the HasResult annotation will have this method automatically added to return true.
     */
    public boolean hasResult()
    {
        return false;
    }

    /**
     * Returns the value set as the result of this event
     */
    public Result getResult()
    {
        return result;
    }

    /**
     * Sets the result value for this event, not all events can have a result set, and any attempt to
     * set a result for a event that isn't expecting it will result in a IllegalArgumentException.
     *
     * The functionality of setting the result is defined on a per-event bases.
     *
     * @param value The new result
     */
    public void setResult(Result value)
    {
        result = value;
    }

    /**
     * Called by the base constructor, this is used by ASM generated
     * event classes to setup various functionality such as the listener list.
     */
    protected void setup()
    {
    }

    /**
     * Returns a ListenerList object that contains all listeners
     * that are registered to this event.
     *
     * Note: for better efficiency, this gets overridden automatically
     * using a Transformer, there is no need to override it yourself.
     * @see EventSubclassTransformer
     *
     * @return Listener List
     */
    public ListenerList getListenerList()
    {
        return getListenerListInternal(this.getClass(), true);
    }

    /**
     * Returns a ListenerList object that contains all listeners
     * that are registered to this event class.
     *
     * This supports abstract classes that cannot be instantiated.
     *
     * Note: this is much slower than the instance method {@link #getListenerList()}.
     * For performance when emitting events, always call that method instead.
     *
     * @return Listener List
     */
    static ListenerList getListenerList(Class<?> eventClass)
    {
        return getListenerListInternal(eventClass, false);
    }

    protected final ListenerList getParentListenerList()
    {
        return getListenerListInternal(this.getClass().getSuperclass(), false);
    }

    private static ListenerList getListenerListInternal(Class<?> eventClass, boolean fromInstanceCall)
    {
        return listeners.computeIfAbsent(eventClass, c -> computeListenerList(eventClass, fromInstanceCall));
    }

    private static ListenerList computeListenerList(Class<?> eventClass, boolean fromInstanceCall)
    {
        if (eventClass == Event.class)
        {
            return new ListenerList();
        }

        if (fromInstanceCall || Modifier.isAbstract(eventClass.getModifiers()))
        {
            Class<?> superclass = eventClass.getSuperclass();
            ListenerList parentList = getListenerList(superclass);
            return new ListenerList(parentList);
        }

        try
        {
            Constructor<?> ctr = eventClass.getConstructor();
            ctr.setAccessible(true);
            Event event = (Event) ctr.newInstance();
            return event.getListenerList();
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException("Error computing listener list for " + eventClass.getName(), e);
        }
    }

    @Nullable
    public EventPriority getPhase()
    {
        return this.phase;
    }

    public void setPhase(@Nonnull EventPriority value)
    {
        Objects.requireNonNull(value, "setPhase argument must not be null");
        int prev = phase == null ? -1 : phase.ordinal();
        if (prev >= value.ordinal()) throw new IllegalArgumentException("Attempted to set event phase to "+ value +" when already "+ phase);
        phase = value;
    }
}
