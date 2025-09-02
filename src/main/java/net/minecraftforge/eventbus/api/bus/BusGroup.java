/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.bus;

import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.eventbus.internal.BusGroupImpl;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 * A collection of {@link EventBus} instances that are grouped together for easier management, allowing for bulk
 * operations.
 */
public sealed interface BusGroup permits BusGroupImpl {
    /**
     * The default BusGroup, which is used when an {@link EventBus} is created without specifying a BusGroup.
     */
    BusGroup DEFAULT = create("default");

    /**
     * Creates a new BusGroup with the given name.
     *
     * @param name The unique name of the BusGroup
     * @return A new BusGroup with the given name
     * @throws IllegalArgumentException if the name is already in use by another BusGroup
     * @apiNote To enforce a base type for all events in this BusGroup, use {@link #create(String, Class)}.
     */
    static BusGroup create(String name) {
        return new BusGroupImpl(name, Event.class);
    }

    /**
     * Creates a new BusGroup with the given name and base type.
     *
     * @param name The unique name of the BusGroup
     * @param baseType The base type that all events in this BusGroup must extend or implement
     * @return A new BusGroup with the given name and base type
     * @throws IllegalArgumentException if the name is already in use by another BusGroup
     */
    static BusGroup create(String name, Class<?> baseType) {
        return new BusGroupImpl(name, baseType);
    }

    /**
     * The unique name of this BusGroup.
     * <p>The uniqueness of this name is enforced when the bus group is {@linkplain #create(String) created}.</p>
     */
    String name();

    /**
     * Starts up all EventBus instances associated with this BusGroup, allowing events to be posted again after a
     * previous call to {@link #shutdown()}.
     */
    void startup();

    /**
     * Shuts down all EventBus instances associated with this BusGroup, preventing any further events from being posted
     * until {@link #startup()} is called.
     *
     * @apiNote If you don't intend on using this BusGroup again, prefer {@link #dispose()} instead as that will also
     *          free up resources.
     */
    void shutdown();

    /**
     * {@linkplain #shutdown() Shuts down} all EventBus instances associated with this BusGroup,
     * {@linkplain #unregister(Collection) unregisters} all listeners and frees no longer needed resources.
     *
     * <p>Warning: This is a destructive operation - this BusGroup should not be used again after calling this method -
     * attempting to do so may throw exceptions or act as a no-op.</p>
     *
     * @apiNote If you plan on using this BusGroup again, prefer {@link #shutdown()} instead.
     */
    void dispose();

    /**
     * Trims the backing lists of all EventBus instances associated with this BusGroup to free up resources.
     *
     * <p>Warning: This is only intended to be called <b>once</b> after all listeners are registered - calling this
     * repeatedly may hurt performance.</p>
     *
     * @apiNote This is an experimental feature that may be removed, renamed or otherwise changed without notice.
     */
    void trim();

    /**
     * Registers all static methods annotated with {@link SubscribeEvent} in the given class.
     *
     * @param callerLookup {@code MethodHandles.lookup()} from the class containing listeners
     * @param utilityClassWithStaticListeners the class containing the static listeners
     * @return A collection of the registered listeners, which can be used to optionally unregister them later
     *
     * @apiNote This method only registers static listeners.
     *          <p>If you want to register both instance and static methods, use
     *          {@link BusGroup#register(MethodHandles.Lookup, Object)} instead.</p>
     * @implNote Internally, bulk registration uses {@link LambdaMetafactory} to create method references to the
     *           annotated methods using the provided {@code callerLookup} - said lookup must have
     *           {@linkplain MethodHandles.Lookup#hasFullPrivilegeAccess() full privilege access} as
     *           {@linkplain LambdaMetafactory LMF} may need to spin an inner class for implementing the lambda, which
     *           inherently allows access to private fields and methods.
     */
    Collection<EventListener> register(MethodHandles.Lookup callerLookup, Class<?> utilityClassWithStaticListeners);

    /**
     * Registers all methods annotated with {@link SubscribeEvent} in the given object.
     *
     * @param callerLookup {@code MethodHandles.lookup()} from the class containing the listeners
     * @param listener the object containing the static and/or instance listeners
     * @return A collection of the registered listeners, which can be used to optionally unregister them later
     *
     * @apiNote If you know all the listeners are static methods, use
     *          {@link BusGroup#register(MethodHandles.Lookup, Class)} instead for better registration performance.
     * @implNote Internally, bulk registration uses {@link LambdaMetafactory} to create method references to the
     *           annotated methods using the provided {@code callerLookup} - said lookup must have
     *           {@linkplain MethodHandles.Lookup#hasFullPrivilegeAccess() full privilege access} as
     *           {@linkplain LambdaMetafactory LMF} may need to spin an inner class for implementing the lambda, which
     *           inherently allows access to private fields and methods.
     */
    Collection<EventListener> register(MethodHandles.Lookup callerLookup, Object listener);

    /**
     * Unregisters the given listeners from this BusGroup.
     * @param listeners A collection of listeners to unregister, obtained from
     *                  {@link #register(MethodHandles.Lookup, Class)} or {@link #register(MethodHandles.Lookup, Object)}
     */
    void unregister(Collection<EventListener> listeners);
}
