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

import net.minecraftforge.eventbus.ListenerList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EventListenerHelper
{
    private final static Map<Class<?>, ListenerList> listeners = new IdentityHashMap<>();
    private static ReadWriteLock lock = new ReentrantReadWriteLock();
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
        final Lock readLock = lock.readLock();
        ListenerList listenerList;
        readLock.lock();
        try {
            listenerList = listeners.get(eventClass);
        } finally {
            readLock.unlock();
        }
        if (listenerList == null) {
            //We did not find an existing list, compute a new one OUTSIDE of the RW lock
            //Otherwise, this could cause clinit on another thread to hang on the upper readLock as computeListenerList may have to init the class
            //and we would have the write lock.
            ListenerList computed = computeListenerList(eventClass, fromInstanceCall);

            final Lock write = lock.writeLock();
            write.lock();
            try {
                //look if we won the race
                listenerList = listeners.get(eventClass);
                if (listenerList == null) { //if this is false, someone else computed a list a well and put it in the map before us. Use it instead of our list
                    //in this case, we were the first, so let's put the list into the map and use it.
                    listeners.put(eventClass, computed);
                    listenerList = computed;
                }
            } finally {
                write.unlock();
            }
        }
        return listenerList;
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

    private static void clearAll() {
        listeners.clear();
        lock = new ReentrantReadWriteLock();
    }
}
