/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class EventListenerFactory {
    private EventListenerFactory() {}

    private static final MethodType RETURNS_CONSUMER = MethodType.methodType(Consumer.class);
    private static final MethodType RETURNS_PREDICATE = MethodType.methodType(Predicate.class);
    private static final MethodType RETURNS_MONITOR = MethodType.methodType(ObjBooleanBiConsumer.class);

    /** The method type of the {@link Consumer} functional interface ({@code void accept(Object)}) */
    private static final MethodType CONSUMER_FI_TYPE = MethodType.methodType(void.class, Object.class);

    /** The method type of the {@link Predicate} functional interface ({@code boolean test(Object)}) */
    private static final MethodType PREDICATE_FI_TYPE = CONSUMER_FI_TYPE.changeReturnType(boolean.class);

    /** The method type of the {@link ObjBooleanBiConsumer} functional interface ({@code void accept(Object, boolean)}) */
    private static final MethodType MONITOR_FI_TYPE = MethodType.methodType(void.class, Object.class, boolean.class);

    private static final Map<Method, MethodHandle> LMF_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Collection<EventListener> register(BusGroupImpl busGroup, MethodHandles.Lookup callerLookup,
                                                     Class<?> listenerClass, @Nullable Object listenerInstance) {
        Method[] declaredMethods = listenerClass.getDeclaredMethods();
        if (declaredMethods.length == 0)
            throw new IllegalArgumentException("No declared methods found in " + listenerClass);

        Class<?> firstValidListenerEventType = null;

        var listeners = new ArrayList<EventListener>();
        for (var method : declaredMethods) {
            if (listenerInstance == null && !Modifier.isStatic(method.getModifiers()))
                continue;

            int paramCount = method.getParameterCount();
            if (paramCount == 0 || paramCount > 2)
                continue;

            Class<?> returnType = method.getReturnType();
            if (returnType != void.class && returnType != boolean.class)
                continue;

            if (!method.isAnnotationPresent(SubscribeEvent.class))
                continue;

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!Event.class.isAssignableFrom(parameterTypes[0]))
                throw new IllegalArgumentException("First parameter of a @SubscribeEvent method must be an event");

            Class<? extends Event> eventType = (Class<? extends Event>) parameterTypes[0];
            var subscribeEventAnnotation = method.getAnnotation(SubscribeEvent.class);

            listeners.add(registerListener(busGroup, callerLookup, paramCount, returnType, eventType,
                    subscribeEventAnnotation, method, listenerInstance));

            if (firstValidListenerEventType == null)
                firstValidListenerEventType = eventType;
        }

        if (listeners.isEmpty())
            throw new IllegalArgumentException("No listeners found in " + listenerClass);
        else if (firstValidListenerEventType == null)
            throw new IllegalArgumentException("No valid listeners found in " + listenerClass);
        else if (listeners.size() == 1)
            throw new IllegalArgumentException("Only a single listener found in " + listenerClass + ". You should directly call addListener() on the EventBus of " + firstValidListenerEventType.getSimpleName() + " instead.");

        return listeners;
    }

    /**
     * Same as {@link #register(BusGroupImpl, MethodHandles.Lookup, Class, Object)}, but with strict validation.
     * <p>Useful for debugging and dev environments, but slower than the normal method intended for production use.</p>
     * <p>See also the "eventbus-validator" subproject, which uses an annotation processor to mirror these runtime
     * checks at compile-time.</p>
     * @see Constants#STRICT_REGISTRATION_CHECKS
     */
    @SuppressWarnings({"unchecked"})
    public static Collection<EventListener> registerStrict(BusGroupImpl busGroup, MethodHandles.Lookup callerLookup,
                                                           Class<?> listenerClass, @Nullable Object listenerInstance) {
        Class<? extends Event> firstValidListenerEventType = null;

        Method[] declaredMethods = listenerClass.getDeclaredMethods();
        if (declaredMethods.length == 0) {
            var errMsg = "No declared methods found in " + listenerClass.getName();
            var superClass = listenerClass.getSuperclass();
            if (superClass != null && superClass != Record.class && superClass != Enum.class) {
                errMsg += ". Note that listener inheritance is not supported. " +
                        "If you are trying to inherit listeners, please use @Override and @SubscribeEvent on the method in the subclass.";
            }
            throw fail(listenerClass, errMsg);
        }

        var listeners = new ArrayList<EventListener>();
        for (var method : declaredMethods) {
            var hasSubscribeEvent = method.isAnnotationPresent(SubscribeEvent.class);
            int paramCount = method.getParameterCount();

            if (hasSubscribeEvent && (paramCount == 0 || paramCount > 2))
                throw fail(method, "Invalid number of parameters: " + paramCount + " (expected 1 or 2)");

            if (paramCount == 0)
                continue;

            Class<?> returnType = method.getReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();
            var firstParamExtendsEvent = Event.class.isAssignableFrom(parameterTypes[0]);

            if (!hasSubscribeEvent && firstParamExtendsEvent)
                throw fail(method, "Missing @SubscribeEvent annotation");

            if (hasSubscribeEvent) {
                var firstParamExtendsCancellable = Cancellable.class.isAssignableFrom(parameterTypes[0]);
                var subscribeEventAnnotation = method.getAnnotation(SubscribeEvent.class);
                var isMonitoringListener = subscribeEventAnnotation.priority() == Priority.MONITOR;

                if (!firstParamExtendsEvent)
                    throw fail(method, "First parameter of a @SubscribeEvent method must be an event");

                var eventType = (Class<? extends Event>) parameterTypes[0];

                if (returnType != void.class && returnType != boolean.class)
                    throw fail(method, "Invalid return type: " + returnType.getName() + " (expected void or boolean)");

                if (listenerInstance == null && !Modifier.isStatic(method.getModifiers()))
                    throw fail(method, "Listener instance is null and method is not static");

                if (isMonitoringListener && (returnType == boolean.class || subscribeEventAnnotation.alwaysCancelling()))
                    throw fail(method, "Monitoring listeners cannot cancel events");

                if (paramCount == 2) {
                    if (!firstParamExtendsCancellable)
                        throw fail(method, "Cancellation-aware monitoring listeners are only valid for cancellable events");

                    if (!boolean.class.isAssignableFrom(parameterTypes[1]))
                        throw fail(method, "Second parameter of a cancellation-aware monitoring listener must be a boolean");

                    if (subscribeEventAnnotation.priority() != Priority.MONITOR)
                        throw fail(method, "Cancellation-aware monitoring listeners must have a priority of MONITOR");
                }

                if (!firstParamExtendsCancellable) {
                    if (subscribeEventAnnotation.alwaysCancelling())
                        throw fail(method, "Always cancelling listeners are only valid for cancellable events");

                    if (returnType == boolean.class)
                        throw fail(method, "Return type boolean is only valid for cancellable events");
                }

                listeners.add(registerListener(busGroup, callerLookup, paramCount, returnType, eventType,
                        subscribeEventAnnotation, method, listenerInstance));

                if (firstValidListenerEventType == null)
                    firstValidListenerEventType = eventType;
            }
        }

        if (listeners.isEmpty())
            throw fail(listenerClass, "No listeners found");
        else if (firstValidListenerEventType == null)
            throw fail(listenerClass, "No valid listeners found");
        else if (listeners.size() == 1)
            throw fail(listenerClass, "Only a single listener found. You should directly call addListener() on the EventBus of " + firstValidListenerEventType.getSimpleName() + " instead.");

        return listeners;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EventListener registerListener(BusGroupImpl busGroup, MethodHandles.Lookup callerLookup,
                                                  int paramCount, Class<?> returnType, Class<? extends Event> eventType,
                                                  SubscribeEvent subscribeEventAnnotation, Method method,
                                                  @Nullable Object listenerInstance) {
        // determine the listener type from its parameters and return type
        if (paramCount == 1) {
            var priority = subscribeEventAnnotation.priority();
            if (returnType == void.class) {
                if (Cancellable.class.isAssignableFrom(eventType)) {
                    // Consumer<Event & Cancellable>
                    var eventBus = ((CancellableEventBus) busGroup.getOrCreateEventBus(eventType));
                    if (subscribeEventAnnotation.alwaysCancelling()) {
                        return eventBus.addListener(priority, true, createConsumer(callerLookup, method, listenerInstance));
                    } else {
                        return eventBus.addListener(priority, createConsumer(callerLookup, method, listenerInstance));
                    }
                } else {
                    // Consumer<Event>
                    return busGroup.getOrCreateEventBus(eventType)
                            .addListener(priority, createConsumer(callerLookup, method, listenerInstance));
                }
            } else {
                // Predicate<Event & EventCharacteristic.Cancellable>
                if (!Cancellable.class.isAssignableFrom(eventType))
                    throw fail(method, "Return type boolean is only valid for cancellable events");

                if (subscribeEventAnnotation.alwaysCancelling())
                    throw new IllegalArgumentException("Always cancelling listeners must have a void return type");

                return ((CancellableEventBus) busGroup.getOrCreateEventBus(eventType))
                        .addListener(priority, createPredicate(callerLookup, method, listenerInstance));
            }
        } else {
            // ObjBooleanBiConsumer<Event & Cancellable>
            if (returnType != void.class)
                throw new IllegalArgumentException("Cancellation-aware monitoring listeners must have a void return type");

            if (subscribeEventAnnotation.alwaysCancelling())
                throw new IllegalArgumentException("Monitoring listeners cannot cancel events");

            return ((CancellableEventBus) busGroup.getOrCreateEventBus(eventType))
                    .addListener(createMonitor(callerLookup, method, listenerInstance));
        }
    }

    private static IllegalArgumentException fail(Class<?> listenerClass, String reason) {
        return new IllegalArgumentException("Failed to register " + listenerClass.getName() + ": " + reason);
    }

    private static IllegalArgumentException fail(Method mtd, String reason) {
        return new IllegalArgumentException("Failed to register " + mtd.getDeclaringClass().getName() + "." + mtd.getName() + ": " + reason);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> Consumer<T> createConsumer(MethodHandles.Lookup callerLookup, Method callback,
                                                                @Nullable Object instance) {
        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        var factoryMH = getOrMakeFactory(callerLookup, callback, isStatic, instance, RETURNS_CONSUMER, CONSUMER_FI_TYPE, "accept");

        try {
            return isStatic
                    ? (Consumer<T>) factoryMH.invokeExact()
                    : (Consumer<T>) factoryMH.invokeExact(instance);
        } catch (Exception e) {
            throw makeRuntimeException(callback, e);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> Predicate<T> createPredicate(MethodHandles.Lookup callerLookup, Method callback,
                                                                  @Nullable Object instance) {
        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        var factoryMH = getOrMakeFactory(callerLookup, callback, isStatic, instance, RETURNS_PREDICATE, PREDICATE_FI_TYPE, "test");

        try {
            return isStatic
                    ? (Predicate<T>) factoryMH.invokeExact()
                    : (Predicate<T>) factoryMH.invokeExact(instance);
        } catch (Exception e) {
            throw makeRuntimeException(callback, e);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> ObjBooleanBiConsumer<T> createMonitor(MethodHandles.Lookup callerLookup,
                                                                           Method callback, @Nullable Object instance) {
        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        var factoryMH = getOrMakeFactory(callerLookup, callback, isStatic, instance, RETURNS_MONITOR, MONITOR_FI_TYPE, "accept");

        try {
            return isStatic
                    ? (ObjBooleanBiConsumer<T>) factoryMH.invokeExact()
                    : (ObjBooleanBiConsumer<T>) factoryMH.invokeExact(instance);
        } catch (Exception e) {
            throw makeRuntimeException(callback, e);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static MethodHandle getOrMakeFactory(MethodHandles.Lookup callerLookup, Method callback, boolean isStatic,
                                                 @Nullable Object instance, MethodType factoryReturnType,
                                                 MethodType fiMethodType, String fiMethodName) {
        if (Constants.ALLOW_DUPE_LISTENERS)
            return makeFactory(callerLookup, callback, isStatic, instance, factoryReturnType, fiMethodType, fiMethodName);
        else
            return LMF_CACHE.computeIfAbsent(callback, callbackMethod ->
                    makeFactory(callerLookup, callbackMethod, isStatic, instance, factoryReturnType, fiMethodType, fiMethodName));
    }

    private static MethodHandle makeFactory(MethodHandles.Lookup callerLookup, Method callback, boolean isStatic,
                                            @Nullable Object instance, MethodType factoryReturnType,
                                            MethodType fiMethodType, String fiMethodName) {
        try {
            var mh = callerLookup.unreflect(callback);

            MethodType factoryType = isStatic
                    ? factoryReturnType
                    : factoryReturnType.insertParameterTypes(0, Objects.requireNonNull(instance).getClass());

            MethodHandle lmf = LambdaMetafactory.metafactory(
                    callerLookup, fiMethodName, factoryType, fiMethodType, mh,
                    isStatic ? mh.type() : mh.type().dropParameterTypes(0, 1)
            ).getTarget();

            if (isStatic)
                return lmf;

            // wrap the target MH in an Object -> instance class cast to allow for invokeExact()
            return lmf.asType(factoryType.changeParameterType(0, Object.class));
        } catch (Exception e) {
            throw makeRuntimeException(callback, e);
        }
    }

    private static RuntimeException makeRuntimeException(Method callback, Exception e) {
        return switch (e) {
            case IllegalAccessException iae -> {
                var errMsg = "Failed to create listener";
                if (!Modifier.isPublic(callback.getModifiers()))
                    errMsg += " - is it public?";

                yield new RuntimeException(errMsg, iae);
            }
            case NullPointerException npe -> new RuntimeException(
                    "Failed to create listener - was given a non-static method without an instance to invoke it with",
                    npe
            );
            default -> new RuntimeException("Failed to create listener", e);
        };
    }
}
