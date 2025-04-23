/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.api.bus;

import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.eventbus.internal.BusGroupImpl;
import net.minecraftforge.eventbus.internal.Event;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 * A bus group is a collection of {@link EventBus} instances that are grouped together for easier management.
 * <p>Using a bus group allows consumers to manage all of their related event buses without needing to manually manage
 * each one.</p>
 *
 * <h2>Example</h2>
 * <p>Here is a small example showing the creation and disposal of a bus group.</p>
 * {@snippet :
 * import net.minecraftforge.eventbus.api.bus.BusGroup;
 * import net.minecraftforge.eventbus.api.bus.EventBus;
 * import net.minecraftforge.eventbus.api.event.RecordEvent;
 * import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
 *
 * import java.lang.invoke.MethodHandles;
 *
 * public class MyClass {
 *     public static final BusGroup BUS_GROUP = BusGroup.create("MyProject", RecordEvent.class);
 *
 *     public record MyEvent(String message) implements RecordEvent {
 *         public static final EventBus<MyEvent> BUS = EventBus.create(BUS_GROUP, MyEvent.class);
 *     }
 *
 *     @SubscribeEvent
 *     private static void onMyEvent(MyEvent event) {
 *         System.out.println("Received event: " + event.message());
 *     }
 *
 *     // if we only have one listener in our class, EventBus will throw an exception saying you should use BusGroup#addListener instead
 *     @SubscribeEvent
 *     private static void alsoOnMyEvent(MyEvent event) {
 *         System.out.println("Double checking, received event: " + event.message());
 *     }
 *
 *     // begin program!
 *     public static void run() {
 *         // the bus group is already started! no need to call startup() on it.
 *
 *         // MethodHandles.lookup() gives EventBus the ability to get method references
 *         // for all the @SubscribeEvent methods in this class.
 *         BUS_GROUP.register(MethodHandles.lookup(), MyClass.class);
 *     }
 *
 *     // close program!
 *     public static void shutdown() {
 *         // dispose will shutdown and then dispose this bus group
 *         // consider it "freed memory" that should not be reused
 *         BUS_GROUP.dispose();
 *     }
 * }
 *}
 */
public sealed interface BusGroup permits BusGroupImpl {
    /**
     * The default bus group, which is used when an {@linkplain EventBus event bus} is created without specifying a
     * group.
     *
     * @apiNote If you require tight controls over your event buses, you should create your own bus group instead. This
     * bus group can be used and mutated by other consumers within the same environment.
     * @see EventBus#create(Class)
     */
    BusGroup DEFAULT = create("default");

    /**
     * Creates a new bus group with the given name.
     * <p>The name for this bus group <i>must be unique.</i> An attempt to create a bus group with a name that is
     * already in use will result in an {@link IllegalArgumentException}. If you must create a new bus group with a name
     * that is in use, the relevant bus group must be {@linkplain #dispose() disposed}.</p>
     *
     * @param name The name
     * @return The new bus group
     * @throws IllegalArgumentException If the name is already in use by another bus group
     * @apiNote To enforce a base type with your bus group, use {@linkplain #create(String, Class)}.
     */
    static BusGroup create(String name) {
        return new BusGroupImpl(name, Event.class);
    }

    /**
     * Creates a new bus group with the given name.
     * <p>The given base type will enforce that all {@linkplain EventBus event buses} created within this group inherit
     * it.</p>
     * <p>The name for this bus group <i>must be unique.</i> An attempt to create a bus group with a name that is
     * already in use will result in an {@link IllegalArgumentException}. If you must create a new bus group with a name
     * that is in use, the relevant bus group must be {@linkplain #dispose() disposed}.</p>
     *
     * @param name The name
     * @return The new bus group
     * @throws IllegalArgumentException If the name is already in use by another bus group
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
     * Starts up all EventBus instances associated with this bus group, allowing events to be posted again after a
     * previous call to {@link #shutdown()}.
     * <p>Calling this method without having previously called {@link #shutdown()} will have no effect.</p>
     */
    void startup();

    /**
     * Shuts down all EventBus instances associated with this bus group, preventing any further events from being posted
     * until {@link #startup()} is called.
     * <p>Calling this method without having previously called {@link #startup()} will have no effect.</p>
     *
     * @apiNote If you need to destroy this bus group and free up the resources it uses, use {@link #dispose()}.
     */
    void shutdown();

    /**
     * {@linkplain #shutdown() Shuts down} all EventBus instances associated with this bus group,
     * {@linkplain #unregister(Collection) unregisters} all listeners and frees resources no longer needed.
     * <p><strong>This will effectively destroy this bus group.</strong> It should not be used again after calling this
     * method.</p>
     *
     * @apiNote If you plan on using this bus group again, use {@link #shutdown()} instead.
     */
    void dispose();

    /**
     * Trims the backing lists of all EventBus instances associated with this BusGroup to free up resources.
     * <p>This is only intended to be called <strong>once</strong> after all listeners are registered. Calling this
     * repeatedly may hurt performance.</p>
     *
     * @apiNote <strong>This is an experimental feature!</strong> It may be removed, renamed or otherwise changed
     * without notice.
     */
    void trim();

    /**
     * Registers all <i>static</i> methods annotated with {@link SubscribeEvent} in the given class.
     * <p>This is done by getting method references for those methods using the given
     * {@linkplain MethodHandles.Lookup method handles lookup}. This lookup <strong>must be acquiored from
     * {@link MethodHandles#lookup()}.</strong> Using {@link MethodHandles#publicLookup()} is unsupported because it
     * doesn't work with {@link java.lang.invoke.LambdaMetafactory} as it could allow for access to private fields
     * through inner class generation.</p>
     *
     * @param callerLookup                    {@link MethodHandles#lookup()} from the class containing listeners
     * @param utilityClassWithStaticListeners the class containing the static listeners
     * @return A collection of the registered listeners, which can be used to optionally unregister them later
     * @apiNote This method only registers static listeners.
     * <p>If you want to register both instance and static methods, use
     * {@link BusGroup#register(MethodHandles.Lookup, Object)} instead.</p>
     */
    Collection<EventListener> register(MethodHandles.Lookup callerLookup, Class<?> utilityClassWithStaticListeners);

    /**
     * Registers all methods annotated with {@link SubscribeEvent} in the given object.
     * <p>Both the static <i>and</i> instance methods for the given object are registered. Keep in mind that, unlike
     * with {@link #register(MethodHandles.Lookup, Class)}, you will need to register each object instance of the class
     * using this method.</p>
     * <p>This is done by getting method references for those methods using the given
     * {@linkplain MethodHandles.Lookup method handles lookup}. This lookup <strong>must be acquiored from
     * {@link MethodHandles#lookup()}.</strong> Using {@link MethodHandles#publicLookup()} is unsupported because it
     * doesn't work with {@link java.lang.invoke.LambdaMetafactory} as it could allow for access to private fields
     * through inner class generation.</p>
     *
     * @param callerLookup {@code MethodHandles.lookup()} from the class containing the listeners
     * @param listener     the object containing the static and/or instance listeners
     * @return A collection of the registered listeners, which can be used to optionally unregister them later
     * @apiNote If you know all the listeners are static methods, use
     * {@link BusGroup#register(MethodHandles.Lookup, Class)} instead for better registration performance.
     */
    Collection<EventListener> register(MethodHandles.Lookup callerLookup, Object listener);

    /**
     * Unregisters the given listeners from this bus group.
     *
     * @param listeners A collection of listeners to unregister, obtained from
     *                  {@link #register(MethodHandles.Lookup, Class)} or
     *                  {@link #register(MethodHandles.Lookup, Object)}
     */
    void unregister(Collection<EventListener> listeners);
}
