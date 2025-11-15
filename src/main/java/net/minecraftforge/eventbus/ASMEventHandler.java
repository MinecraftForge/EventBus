/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.eventbus.internal.InternalUtils;

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
        this(factory, target, method, isGeneric, method.getAnnotation(SubscribeEvent.class));
    }

    private ASMEventHandler(IEventListenerFactory factory, Object target, Method method, boolean isGeneric, SubscribeEvent subInfo) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        handler = factory.create(method, target);

        this.subInfo = subInfo;
        readable = "ASM: " + target + " " + method.getName() + getMethodDescriptor(method);
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
        this.filter = filter;
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

    public String toString() {
        return readable;
    }

    /**
     * Creates a new ASMEventHandler instance, factoring in a time-shifting optimisation.
     *
     * <p>In the case that no post-time checks are needed, an anonymous subclass instance will be returned that calls
     * the listener without additional redundant checks.</p>
     *
     * @implNote The 'all or nothing' nature of the post-time checks is to reduce the likelihood of megamorphic method
     *           invocation, which isn't as performant as monomorphic or bimorphic calls in Java 16
     *           (what EventBus 6.2.x targets).
     */
    public static ASMEventHandler of(IEventListenerFactory factory, Object target, Method method, boolean isGeneric) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        var subInfo = method.getAnnotation(SubscribeEvent.class);
        assert subInfo != null;
        var eventType = method.getParameterTypes()[0];
        if (InternalUtils.couldBeCancelled(eventType, isGeneric))
            return new ASMEventHandler(factory, target, method, isGeneric, subInfo);

        // If we get to this point, no post-time checks are needed, so strip them out
        return new ASMEventHandler(factory, target, method, false, subInfo) {
            @Override
            public void invoke(Event event) {
                handler.invoke(event);
            }
        };
    }
}
