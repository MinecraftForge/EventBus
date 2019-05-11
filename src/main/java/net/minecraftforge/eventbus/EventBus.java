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

package net.minecraftforge.eventbus;

import net.jodah.typetools.TypeResolver;
import net.minecraftforge.eventbus.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.minecraftforge.eventbus.LogMarkers.EVENTBUS;

public class EventBus implements IEventExceptionHandler, IEventBus {
    private static final Logger LOGGER = LogManager.getLogger();
    private static AtomicInteger maxID = new AtomicInteger(0);
    private final boolean trackPhases;

    private ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners = new ConcurrentHashMap<Object, ArrayList<IEventListener>>();
    private final int busID = maxID.getAndIncrement();
    private final IEventExceptionHandler exceptionHandler;
    private volatile boolean shutdown = false;

    private EventBus()
    {
        ListenerList.resize(busID + 1);
        exceptionHandler = this;
        this.trackPhases = true;
    }

    private EventBus(final IEventExceptionHandler handler, boolean trackPhase)
    {
        ListenerList.resize(busID + 1);
        if (handler == null) exceptionHandler = this;
        else exceptionHandler = handler;
        this.trackPhases = trackPhase;
    }

    public EventBus(final BusBuilder busBuilder) {
        this(busBuilder.getExceptionHandler(), busBuilder.getTrackPhases());
    }

    private void registerClass(final Class<?> clazz) {
        Arrays.stream(clazz.getMethods()).
                filter(m->Modifier.isStatic(m.getModifiers())).
                filter(m->m.isAnnotationPresent(SubscribeEvent.class)).
                forEach(m->registerListener(clazz, m, m));
    }

    private Optional<Method> getDeclMethod(final Class<?> clz, final Method in) {
        try {
            return Optional.of(clz.getDeclaredMethod(in.getName(), in.getParameterTypes()));
        } catch (NoSuchMethodException nse) {
            return Optional.empty();
        }

    }
    private void registerObject(final Object obj) {
        final HashSet<Class<?>> classes = new HashSet<>();
        typesFor(obj.getClass(), classes);
        Arrays.stream(obj.getClass().getMethods()).
                filter(m->!Modifier.isStatic(m.getModifiers())).
                forEach(m -> classes.stream().
                        map(c->getDeclMethod(c, m)).
                        filter(rm -> rm.isPresent() && rm.get().isAnnotationPresent(SubscribeEvent.class)).
                        findFirst().
                        ifPresent(rm->registerListener(obj, m, rm.get())));
    }


    private void typesFor(final Class<?> clz, final Set<Class<?>> visited) {
        if (clz.getSuperclass() == null) return;
        typesFor(clz.getSuperclass(),visited);
        Arrays.stream(clz.getInterfaces()).forEach(i->typesFor(i, visited));
        visited.add(clz);
    }

    @Override
    public void register(final Object target)
    {
        if (listeners.containsKey(target))
        {
            return;
        }

        if (target.getClass() == Class.class) {
            registerClass((Class<?>) target);
        } else {
            registerObject(target);
        }
    }

    private void registerListener(final Object target, final Method method, final Method real) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1)
        {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation. " +
                    "It has " + parameterTypes.length + " arguments, " +
                    "but event handler methods require a single argument only."
            );
        }

        Class<?> eventType = parameterTypes[0];

        if (!Event.class.isAssignableFrom(eventType))
        {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation, " +
                            "but takes an argument that is not an Event subtype : " + eventType);
        }

        register(eventType, target, real);
    }

    private <T extends Event> Predicate<T> passCancelled(final boolean ignored) {
        return e-> ignored || !e.isCancelable() || !e.isCanceled();
    }

    private <T extends GenericEvent<? extends F>, F> Predicate<T> passGenericFilter(Class<F> type) {
        return e->e.getGenericType() == type;
    }

    @Override
    public <T extends Event> void addListener(final Consumer<T> consumer) {
        addListener(EventPriority.NORMAL, consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final Consumer<T> consumer) {
        addListener(priority, false, consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Consumer<T> consumer) {
        addListener(priority, passCancelled(receiveCancelled), consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Class<T> eventType, final Consumer<T> consumer) {
        addListener(priority, passCancelled(receiveCancelled), eventType, consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final Consumer<T> consumer) {
        addGenericListener(genericClassFilter, EventPriority.NORMAL, consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority, final Consumer<T> consumer) {
        addGenericListener(genericClassFilter, priority, false, consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority, final boolean receiveCancelled, final Consumer<T> consumer) {
        addListener(priority, passGenericFilter(genericClassFilter).and(passCancelled(receiveCancelled)), consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority, final boolean receiveCancelled, final Class<T> eventType, final Consumer<T> consumer) {
        addListener(priority, passGenericFilter(genericClassFilter).and(passCancelled(receiveCancelled)), eventType, consumer);
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void addListener(final EventPriority priority, final Predicate<? super T> filter, final Consumer<T> consumer) {
        final Class<T> eventClass = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
        if (Objects.equals(eventClass, Event.class))
            LOGGER.warn(EVENTBUS,"Attempting to add a Lambda listener with computed generic type of Event. " +
                    "Are you sure this is what you meant? NOTE : there are complex lambda forms where " +
                    "the generic type information is erased and cannot be recovered at runtime.");
        addListener(priority, filter, eventClass, consumer);
    }

    private <T extends Event> void addListener(final EventPriority priority, final Predicate<? super T> filter, final Class<T> eventClass, final Consumer<T> consumer) {
        addToListeners(consumer, eventClass, e-> doCastFilter(filter, eventClass, consumer, e), priority);
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void doCastFilter(final Predicate<? super T> filter, final Class<T> eventClass, final Consumer<T> consumer, final Event e) {
        T cast = (T)e;
        if (filter.test(cast))
        {
            consumer.accept(cast);
        }
    }

    private void register(Class<?> eventType, Object target, Method method)
    {
        try {
            final ASMEventHandler asm = new ASMEventHandler(target, method, IGenericEvent.class.isAssignableFrom(eventType));

            addToListeners(target, eventType, asm, asm.getPriority());
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error(EVENTBUS,"Error registering event handler: {} {}", eventType, method, e);
        }
    }

    private void addToListeners(final Object target, final Class<?> eventType, final IEventListener listener, final EventPriority priority) {
        ListenerList listenerList = EventListenerHelper.getListenerList(eventType);
        listenerList.register(busID, priority, listener);
        List<IEventListener> others = listeners.computeIfAbsent(target, k -> new ArrayList<>());
        others.add(listener);
    }

    @Override
    public void unregister(Object object)
    {
        ArrayList<IEventListener> list = listeners.remove(object);
        if(list == null)
            return;
        for (IEventListener listener : list)
        {
            ListenerList.unregisterAll(busID, listener);
        }
    }

    @Override
    public boolean post(Event event)
    {
        if (shutdown) return false;

        IEventListener[] listeners = event.getListenerList().getListeners(busID);
        int index = 0;
        try
        {
            for (; index < listeners.length; index++)
            {
                if (!trackPhases && Objects.equals(listeners[index].getClass(), EventPriority.class)) continue;
                listeners[index].invoke(event);
            }
        }
        catch (Throwable throwable)
        {
            exceptionHandler.handleException(this, event, listeners, index, throwable);
            throw throwable;
        }
        return event.isCancelable() && event.isCanceled();
    }

    @Override
    public void handleException(IEventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable)
    {
        LOGGER.error(EVENTBUS, ()->new EventBusErrorMessage(event, index, listeners, throwable));
    }

    @Override
    public void shutdown()
    {
        LOGGER.warn("EventBus {} shutting down - future events will not be posted.", busID);
        this.shutdown = true;
    }
}
