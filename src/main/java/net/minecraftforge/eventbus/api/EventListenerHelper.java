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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.eventbus.ListenerList;

public class EventListenerHelper
{
    private static Map<Class<?>, ListenerList> listeners = new ConcurrentHashMap<>();

    /**
     * Returns a {@link ListenerList} object that contains all listeners
     * that are registered to this event class.
     *
     * This supports abstract classes that cannot be instantiated.
     *
     * Note: this is much slower than the instance method {@link Event#getListenerList()}.
     * For performance when emitting events, always call that method instead.
     */
    public static ListenerList getListenerList(Class<?> eventClass)
    {
        return getListenerListInternal(eventClass, false);
    }

    static ListenerList getListenerListInternal(Class<?> eventClass, boolean fromInstanceCall)
    {
        ListenerList ret = listeners.get(eventClass);
        if (ret == null)
        {
            synchronized(eventClass)
            {
                return listeners.computeIfAbsent(eventClass, c -> computeListenerList(eventClass, fromInstanceCall));
            }
        }
        return ret;
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
}
