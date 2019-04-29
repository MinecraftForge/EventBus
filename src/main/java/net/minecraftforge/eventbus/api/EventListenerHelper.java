package net.minecraftforge.eventbus.api;

import net.minecraftforge.eventbus.ListenerList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

public class EventListenerHelper
{
	private static Map<Class<?>, ListenerList> listeners = new IdentityHashMap<>();

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
}
