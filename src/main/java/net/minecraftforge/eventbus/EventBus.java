/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.jodah.typetools.TypeResolver;
import net.minecraftforge.eventbus.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static net.minecraftforge.eventbus.LogMarkers.EVENTBUS;

public class EventBus implements IEventExceptionHandler, IEventBus {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean checkTypesOnDispatchProperty = Boolean.parseBoolean(System.getProperty("eventbus.checkTypesOnDispatch", "false"));
    private static final AtomicInteger maxID = new AtomicInteger(0);

    private final boolean trackPhases;
    final EnumSet<EventPriority> phasesToTrack;

    private final ConcurrentHashMap<Object, List<IEventListener>> listeners = new ConcurrentHashMap<>();
    private final int busID = maxID.getAndIncrement();
    private final IEventExceptionHandler exceptionHandler;
    private volatile boolean shutdown = false;

    private final Class<?> baseType;
    private final boolean checkTypesOnDispatch;
    private final IEventListenerFactory factory;

    @SuppressWarnings("unused")
    private EventBus() {
        ListenerList.resize(busID + 1);
        exceptionHandler = this;
        this.trackPhases = true;
        this.phasesToTrack = BusBuilderImpl.ALL_PHASES;
        this.baseType = Event.class;
        this.checkTypesOnDispatch = checkTypesOnDispatchProperty;
        this.factory = new ClassLoaderFactory();
    }

    private EventBus(final IEventExceptionHandler handler, boolean trackPhase, EnumSet<EventPriority> phasesToTrack, boolean startShutdown, Class<?> baseType, boolean checkTypesOnDispatch, IEventListenerFactory factory) {
        ListenerList.resize(busID + 1);
        if (handler == null) exceptionHandler = this;
        else exceptionHandler = handler;
        this.trackPhases = trackPhase;
        this.phasesToTrack = trackPhase ? phasesToTrack : BusBuilderImpl.NO_PHASES;
        this.shutdown = startShutdown;
        this.baseType = baseType;
        this.checkTypesOnDispatch = checkTypesOnDispatch || checkTypesOnDispatchProperty;
        this.factory = factory;
    }

    public EventBus(final BusBuilderImpl busBuilder) {
        this(busBuilder.exceptionHandler, busBuilder.trackPhases, busBuilder.phasesToTrack, busBuilder.startShutdown,
             busBuilder.markerType, busBuilder.checkTypesOnDispatch,
             busBuilder.modLauncher ? new ModLauncherFactory() : new ClassLoaderFactory());
    }

    private void registerClass(final Class<?> clazz) {
        for (var method : clazz.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.isAnnotationPresent(SubscribeEvent.class))
                continue;
            registerListener(clazz, method, method);
        }
    }

    private void registerObject(Object obj) {
        var methods = obj.getClass().getMethods();
        record Key(String name, Class<?>[] args) {}
        var unannotated = new HashMap<Key, Method>();
        for (var method : methods) {
            if (Modifier.isStatic(method.getModifiers()))
                continue;
            if (method.isAnnotationPresent(SubscribeEvent.class))
                registerListener(obj, method, method);
            else if (method.getDeclaringClass() != Object.class) // No need to check Object, it shouldn't have the annotations unless someone got really fucky
                unannotated.put(new Key(method.getName(), method.getParameterTypes()), method);
        }

        // Bit of a optimization for the most common use case. No need to search parents if there are no un-annotated methods
        if (unannotated.isEmpty())
            return;

        var classes = new LinkedHashSet<Class<?>>();
        var stack = new Stack<Class<?>>();
        parentTypes(classes, stack, obj.getClass());

        while (!stack.isEmpty()) {
            var cls = stack.pop();
            for (var method : cls.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || !method.isAnnotationPresent(SubscribeEvent.class))
                    continue;
                var needed = unannotated.remove(new Key(method.getName(), method.getParameterTypes()));
                if (needed != null)
                    registerListener(obj, method, method);
            }
            if (!unannotated.isEmpty())
                parentTypes(classes, stack, cls);
        }
    }

    private static void parentTypes(Set<Class<?>> classes, Stack<Class<?>> stack, Class<?> cls) {
        for (var inf : cls.getInterfaces()) {
            if (classes.add(inf))
                stack.push(inf);
        }
        var parent = cls.getSuperclass();
        if (parent != null && parent != Object.class) {
            if (classes.add(parent))
                stack.push(parent);
        }
    }

    @Override
    public void register(final Object target) {
        if (listeners.containsKey(target))
            return;

        if (target.getClass() == Class.class)
            registerClass((Class<?>) target);
        else
            registerObject(target);
    }

    private void registerListener(final Object target, final Method method, final Method real) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation. " +
                    "It has " + parameterTypes.length + " arguments, " +
                    "but event handler methods require a single argument only."
            );
        }

        Class<?> eventType = parameterTypes[0];

        if (!Event.class.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation, " +
                            "but takes an argument that is not an Event subtype : " + eventType);
        }

        if (baseType != Event.class && !baseType.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation, " +
                            "but takes an argument that is not a subtype of the base type " + baseType + ": " + eventType);
        }

        if (!Modifier.isPublic(method.getModifiers()))
            throw new IllegalArgumentException("Failed to create ASMEventHandler for " + target.getClass().getName() + "." + method.getName() + Type.getMethodDescriptor(method) + " it is not public and our transformer is disabled");

        register(eventType, target, real);
    }

    private static void checkNotGeneric(final Consumer<? extends Event> consumer) {
        checkNotGeneric(getEventClass(consumer));
    }

    private static void checkNotGeneric(final Class<? extends Event> eventType) {
        if (GenericEvent.class.isAssignableFrom(eventType))
            throw new IllegalArgumentException("Cannot register a generic event listener with addListener, use addGenericListener");
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
        checkNotGeneric(consumer);
        addListener(priority, consumer, null, receiveCancelled);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Class<T> eventType, final Consumer<T> consumer) {
        checkNotGeneric(eventType);
        addListener(priority, eventType, consumer, null, receiveCancelled);
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
        addListener(priority, consumer, genericClassFilter, receiveCancelled);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority, final boolean receiveCancelled, final Class<T> eventType, final Consumer<T> consumer) {
        addListener(priority, eventType, consumer, genericClassFilter, receiveCancelled);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> Class<T> getEventClass(Consumer<T> consumer) {
        final Class<T> eventClass = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
        if ((Class<?>)eventClass == TypeResolver.Unknown.class) {
            LOGGER.error(EVENTBUS, "Failed to resolve handler for \"{}\"", consumer.toString());
            throw new IllegalStateException("Failed to resolve consumer event type: " + consumer.toString());
        }
        return eventClass;
    }

    private <T extends Event> void addListener(final EventPriority priority, final Consumer<T> consumer, final Class<?> genericFilter, boolean receiveCancelled) {
        Class<T> eventClass = getEventClass(consumer);
        if (Objects.equals(eventClass, Event.class))
            LOGGER.warn(EVENTBUS,"Attempting to add a Lambda listener with computed generic type of Event. " +
                    "Are you sure this is what you meant? NOTE : there are complex lambda forms where " +
                    "the generic type information is erased and cannot be recovered at runtime.");
        addListener(priority, eventClass, consumer, genericFilter, receiveCancelled);
    }

    private <T extends Event> void addListener(final EventPriority priority, final Class<T> eventClass, final Consumer<T> consumer, final Class<?> genericFilter, boolean receiveCancelled) {
        if (baseType != Event.class && !baseType.isAssignableFrom(eventClass)) {
            throw new IllegalArgumentException(
                    "Listener for event " + eventClass + " takes an argument that is not a subtype of the base type " + baseType);
        }

        @SuppressWarnings("unchecked")
        IEventListener listener = new IEventListener() {
            @Override
            public void invoke(Event event) {
                consumer.accept((T)event);
            }

            @Override
            public String toString() {
                return "Lambda Handler: " + consumer.toString();
            }
        };

        ListenerList listenerList = getListenerList(eventClass);
        var cancelable = listenerList.isCancelable() || EventListenerHelper.isCancelable(eventClass);

        IEventListener finalListener = ReactiveEventListener.of(listener, listener.toString(), genericFilter, receiveCancelled, cancelable);
        addToListeners(listenerList, consumer, finalListener, priority);
    }

    private void register(Class<?> eventType, Object target, Method method) {
        try {
            EventPriority priority = method.getAnnotation(SubscribeEvent.class).priority();
            ListenerList listenerList = getListenerList(eventType);
            IEventListener asm = ASMEventHandler.of(this.factory, target, method, IGenericEvent.class.isAssignableFrom(eventType), listenerList.isCancelable());
            addToListeners(listenerList, target, asm, priority);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | ClassNotFoundException e) {
            LOGGER.error(EVENTBUS,"Error registering event handler: {} {}", eventType, method, e);
        }
    }

    private ListenerList getListenerList(Class<?> eventType) {
        ListenerList listenerList = EventListenerHelper.getListenerList(eventType);
        if (!listenerList.isCancelable() && EventListenerHelper.isCancelable(eventType))
            listenerList.setCancelable();

        return listenerList;
    }

    private void addToListeners(final ListenerList listenerList, final Object target, final IEventListener listener, final EventPriority priority) {
        listenerList.register(busID, this, priority, listener);
        List<IEventListener> others = listeners.computeIfAbsent(target, k -> Collections.synchronizedList(new ArrayList<>()));
        others.add(listener);
    }

    @Override
    public void unregister(Object object) {
        List<IEventListener> list = listeners.remove(object);
        if (list == null)
            return;

        for (IEventListener listener : list)
            ListenerList.unregisterAll(busID, listener);
    }

    @Override
    public boolean post(Event event) {
        return post(event, (IEventListener::invoke));
    }

    @Override
    public boolean post(Event event, IEventBusInvokeDispatcher wrapper) {
        if (shutdown) return false;
        if (checkTypesOnDispatch && !baseType.isInstance(event))
            throw new IllegalArgumentException("Cannot post event of type " + event.getClass().getSimpleName() + " to this event. Must match type: " + baseType.getSimpleName());

        IEventListener[] listeners = event.getListenerList().getListeners(busID);
        int index = 0;
        try {
            for (; index < listeners.length; index++) {
                if (!trackPhases && listeners[index].getClass() == EventPriority.class) continue;
                wrapper.invoke(listeners[index], event);
            }
        } catch (Throwable throwable) {
            exceptionHandler.handleException(this, event, listeners, index, throwable);
            throw throwable;
        }
        return event.isCanceled();
    }

    @Override
    public void handleException(IEventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable) {
        LOGGER.error(EVENTBUS, () -> new EventBusErrorMessage(event, index, listeners, throwable));
    }

    @Override
    public void shutdown() {
        LOGGER.fatal(EVENTBUS, "EventBus {} shutting down - future events will not be posted.", busID, new Exception("stacktrace"));
        this.shutdown = true;
    }

    @Override
    public void start() {
        this.shutdown = false;
    }

    // Used for benchmarking to clear any caches so that each iteration starts from fresh
    // Ideally i'd just nuke the entire instance and call it a day, but while we use ModLauncher that isn't viable.
    @SuppressWarnings("unused")
    private void clearInternalData() {
         // Clean Listener Helper
        clearCache(EventListenerHelper.class, "listeners");
        clearCache(EventListenerHelper.class, "cancelable");
        clearCache(EventListenerHelper.class, "hasResult");
        clearCache(ModLauncherFactory.class, "PENDING");
        clearCache(ClassLoaderFactory.class, "cache");
        // Clear Listeners
        //var tmp = new HashSet<>(this.listeners.keySet());
        //for (var listener : tmp)
        //    unregister(listener);
    }

    private void clearCache(Class<?> cls, String name) {
        try {
            var cfld = cls.getDeclaredField(name);
            cfld.setAccessible(true);
            var cache = cfld.get(null);
            var mfld = cache.getClass().getDeclaredField("map");
            mfld.setAccessible(true);
            @SuppressWarnings("rawtypes")
            Map map = (Map)mfld.get(cache);
            map.clear();
        } catch (Exception e) {
            sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }
}
