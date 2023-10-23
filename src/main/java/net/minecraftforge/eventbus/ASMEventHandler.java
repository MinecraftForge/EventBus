/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.*;

import java.lang.reflect.*;
import static org.objectweb.asm.Type.getMethodDescriptor;

public class ASMEventHandler implements IEventListener {

    private final IEventListenerFactory factory;
    private final IEventListener handler;
    private final SubscribeEvent subInfo;
    private String readable;
    private Type filter = null;

    public ASMEventHandler(IEventListenerFactory factory, Object target, Method method, boolean isGeneric) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        this.factory = factory;
        handler = this.factory.create(method, target);

        subInfo = method.getAnnotation(SubscribeEvent.class);
        readable = "ASM: " + target + " " + method.getName() + getMethodDescriptor(method);
        if (isGeneric)
        {
            Type type = method.getGenericParameterTypes()[0];
            if (type instanceof ParameterizedType)
            {
                filter = ((ParameterizedType)type).getActualTypeArguments()[0];
                if (filter instanceof ParameterizedType) // Unlikely that nested generics will ever be relevant for event filtering, so discard them
                {
                    filter = ((ParameterizedType)filter).getRawType();
                }
                else if (filter instanceof WildcardType)
                {
                    // If there's a wildcard filter of Object.class, then remove the filter.
                    final WildcardType wfilter = (WildcardType) filter;
                    if (wfilter.getUpperBounds().length == 1 && wfilter.getUpperBounds()[0] == Object.class && wfilter.getLowerBounds().length == 0) {
                        filter = null;
                    }
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void invoke(Event event)
    {
        if (handler != null)
        {
            if (!event.isCancelable() || !event.isCanceled() || subInfo.receiveCanceled())
            {
                if (filter == null || filter == ((IGenericEvent)event).getGenericType())
                {
                    handler.invoke(event);
                }
            }
        }
    }

    public EventPriority getPriority()
    {
        return subInfo.priority();
    }

    public String toString()
    {
        return readable;
    }
}
