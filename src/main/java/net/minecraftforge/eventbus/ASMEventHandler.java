/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.*;

import java.lang.reflect.*;
import static org.objectweb.asm.Type.getMethodDescriptor;

public class ASMEventHandler implements IEventListener {
    protected final IEventListener handler;
    private final SubscribeEvent subInfo;
    private final String readable;
    private final Type filter;

    /**
     * @deprecated Use {@link #of(IEventListenerFactory, Object, Method, boolean)} instead for better performance.
     */
    @Deprecated
    public ASMEventHandler(IEventListenerFactory factory, Object target, Method method, boolean isGeneric) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        this(
            factory.create(method, target),
            method.getAnnotation(SubscribeEvent.class),
            makeReadable(target, method),
            getFilter(isGeneric, method)
        );
    }

    private ASMEventHandler(IEventListener handler, SubscribeEvent subInfo, String readable, Type filter) {
        this.handler = handler;
        this.subInfo = subInfo;
        this.readable = readable;
        this.filter = filter;
    }

    private static String makeReadable(Object target, Method method) {
        return "ASM: " + target + " " + method.getName() + getMethodDescriptor(method);
    }

    private static Type getFilter(boolean isGeneric, Method method) {
        Type filter = null;
        if (isGeneric) {
            Type type = method.getGenericParameterTypes()[0];
            if (type instanceof ParameterizedType) {
                filter = ((ParameterizedType)type).getActualTypeArguments()[0];
                if (filter instanceof ParameterizedType) // Unlikely that nested generics will ever be relevant for event filtering, so discard them
                    filter = ((ParameterizedType)filter).getRawType();
                else if (filter instanceof WildcardType wfilter) {
                    // If there's a wildcard filter of Object.class, then remove the filter.
                    if (wfilter.getUpperBounds().length == 1 && wfilter.getUpperBounds()[0] == Object.class && wfilter.getLowerBounds().length == 0) {
                        filter = null;
                    }
                }
            }
        }
        return filter;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void invoke(Event event) {
        if (!event.isCanceled() || subInfo.receiveCanceled()) {
            if (filter == null || filter == ((IGenericEvent)event).getGenericType())
                handler.invoke(event);
        }
    }

    public EventPriority getPriority() {
        return subInfo.priority();
    }

    @Override
    public String toString() {
        return readable;
    }

    /**
     * Creates a new ASMEventHandler instance, factoring in a time-shifting optimisation.
     *
     * <p>In the case that no post-time checks are needed, an subclass instance will be returned that calls
     * the listener without additional redundant checks.</p>     *
     *
     * @deprecated Use {@link #of(IEventListenerFactory, Object, Method, boolean, boolean)} instead to prevent wrapping in ASMEventHandler type, for better performance
     */
    @Deprecated
    public static ASMEventHandler of(IEventListenerFactory factory, Object target, Method method, boolean isGeneric) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        var subInfo = method.getAnnotation(SubscribeEvent.class);
        assert subInfo != null;
        var eventType = method.getParameterTypes()[0];
        var filter = getFilter(isGeneric, method);
        var readable = makeReadable(target, method);
        var cancelable = EventListenerHelper.isCancelable(eventType);

        var handler = ReactiveEventListener.of(factory.create(method, target), readable, filter, subInfo.receiveCanceled(), cancelable);
        if (handler instanceof IReactiveEventListener)
            return new Reactive(handler, subInfo, readable, filter);
        return new ASMEventHandler(handler, subInfo, readable, filter);
    }


    /**
     * Creates a new ASMEventHandler instance, factoring in a time-shifting optimisation.
     *
     * <p>In the case that no post-time checks are needed, an subclass instance will be returned that calls
     * the listener without additional redundant checks.</p>
     */
    public static IEventListener of(IEventListenerFactory factory, Object target, Method method, boolean isGeneric, boolean forceCancelable) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        var subInfo = method.getAnnotation(SubscribeEvent.class);
        assert subInfo != null;
        var eventType = method.getParameterTypes()[0];
        var filter = getFilter(isGeneric, method);
        var readable = makeReadable(target, method);
        var cancelable = forceCancelable || EventListenerHelper.isCancelable(eventType);

        return ReactiveEventListener.of(factory.create(method, target), readable, filter, subInfo.receiveCanceled(), cancelable);
    }

    private static class Reactive extends ASMEventHandler implements IReactiveEventListener {
        private Reactive(IEventListener handler, SubscribeEvent subInfo, String readable, Type filter) {
            super(handler, subInfo, readable, filter); //Filter can never be null in any paths we call it. But actually don't want to add a null check here because i don't want to re-do the if.
        }

        @Override
        public void invoke(Event event) {
            handler.invoke(event);
        }

        @Override
        public IEventListener toCancelable() {
            return ((IReactiveEventListener)handler).toCancelable();
        }
    }
}
