/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ListenerList {
    private static final List<ListenerList> allLists = new ArrayList<>();
    private static int maxSize = 0;

    @Nullable
    private final ListenerList parent;
    private ListenerListInst[] lists = new ListenerListInst[0];
    private volatile boolean cancelable = false;

    public ListenerList() {
        this(null);
    }

    public ListenerList(@Nullable ListenerList parent) {
        // parent needs to be set before resize !
        this.parent = parent;
        extendMasterList(this);
        resizeLists(maxSize);
    }

    private synchronized static void extendMasterList(ListenerList inst) {
        allLists.add(inst);
    }

    static void resize(int max) {
        if (max > maxSize) {
            synchronized (ListenerList.class) {
                if (max > maxSize) {
                    allLists.forEach(list -> list.resizeLists(max));
                    maxSize = max;
                }
            }
        }
    }

    final boolean isCancelable() {
        return this.cancelable;
    }

    synchronized void setCancelable() {
        if (this.cancelable)
            return;

        this.cancelable = true;

        if (parent != null)
            parent.setCancelable();

        for (ListenerListInst inst : lists)
            inst.setCancelable();
    }

    private synchronized void resizeLists(int max) {
        if (parent != null)
            parent.resizeLists(max);

        if (lists.length >= max)
            return;

        ListenerListInst[] newList = new ListenerListInst[max];
        int x = 0;
        for (; x < lists.length; x++)
            newList[x] = lists[x];

        for(; x < max; x++) {
            if (parent != null)
                newList[x] = new ListenerListInst(parent.getInstance(x));
            else
                newList[x] = new ListenerListInst();
        }
        lists = newList;
    }

    public static synchronized void clearBusID(int id) {
        for (ListenerList list : allLists)
            list.lists[id].dispose();
    }

    protected ListenerListInst getInstance(int id) {
        return lists[id];
    }

    public IEventListener[] getListeners(int id) {
        return lists[id].getListeners();
    }

    public void register(int id, EventPriority priority, IEventListener listener) {
        lists[id].register(priority, listener);
    }

    public void register(int id, EventBus eventBus, EventPriority priority, IEventListener listener) {
        var list = lists[id];
        list.phasesToTrack = eventBus.phasesToTrack;
        list.register(priority, listener);
    }

    public void unregister(int id, IEventListener listener) {
        lists[id].unregister(listener);
    }

    public static synchronized void unregisterAll(int id, IEventListener listener) {
        for (ListenerList list : allLists)
            list.unregister(id, listener);
    }

    private static class ListenerListInst {
        // Enum#values() performs a defensive copy for each call.
        // As we never modify the returned values array in this class, we can safely reuse it.
        private static final EventPriority[] EVENT_PRIORITY_VALUES = EventPriority.values();
        private static final IEventListener[] NO_LISTENERS = new IEventListener[0];

        /**
         * A lazy-loaded cache of listeners for all priority levels and any phase tracking notifiers.
         * <p><code>null</code> indicates that the cache needs to be rebuilt.</p>
         * @see #getListeners()
         */
        private volatile @Nullable IEventListener[] listeners = NO_LISTENERS;

        /** A lazy-loaded array of lists containing listeners for each priority level. */
        @SuppressWarnings("unchecked")
        private final @Nullable ArrayList<IEventListener>[] priorities =
                (ArrayList<IEventListener>[]) new ArrayList[EVENT_PRIORITY_VALUES.length];

        private ListenerListInst parent;
        private List<ListenerListInst> children;
        private final Semaphore writeLock = new Semaphore(1, true);
        private EnumSet<EventPriority> phasesToTrack = BusBuilderImpl.ALL_PHASES;

        private ListenerListInst() {}

        private ListenerListInst(ListenerListInst parent) {
            this.parent = parent;
            this.parent.addChild(this);
            // We set the NO_LISTENERS so we don't have to rebuild the listener list if nobody registers
            // However the parent can have a listener registered before we know about the sub-class
            if (this.parent.listeners != NO_LISTENERS)
                this.listeners = null;
        }

        public void dispose() {
            writeLock.acquireUninterruptibly();
            for (int i = 0; i < priorities.length; i++) {
                @Nullable ArrayList<IEventListener> priority = priorities[i];
                if (priority != null) {
                    priority.clear();
                    priorities[i] = null;
                }
            }
            writeLock.release();
            parent = null;
            listeners = NO_LISTENERS;
            if (children != null)
                children.clear();
        }

        /**
         * Returns a ArrayList containing all listeners for this event,
         * and all parent events for the specified priority.
         *
         * The list is returned with the listeners for the children events first.
         *
         * @param priority The Priority to get
         * @return ArrayList containing listeners
         */
        public ArrayList<IEventListener> getListeners(EventPriority priority) {
            writeLock.acquireUninterruptibly();
            ArrayList<IEventListener> ret = new ArrayList<>(getListenersForPriority(priority));
            writeLock.release();
            if (parent != null)
                ret.addAll(parent.getListeners(priority));
            return ret;
        }

        /**
         * Returns a full list of all listeners for all priority levels.
         * Including all parent listeners.
         *
         * List is returned in proper priority order.
         *
         * Automatically rebuilds the internal Array cache if its information is out of date.
         *
         * @return Array containing listeners
         */
        public IEventListener[] getListeners() {
            var listeners = this.listeners;
            if (listeners != null)
                return listeners;

            return buildCache();
        }

        protected boolean shouldRebuild() {
            return this.listeners == null;
        }

        protected void forceRebuild() {
            this.listeners = null;
            if (this.children != null) {
                synchronized (this.children) {
                    for (ListenerListInst child : this.children)
                        child.forceRebuild();
                }
            }
        }

        private void addChild(ListenerListInst child) {
            if (this.children == null)
                this.children = Collections.synchronizedList(new ArrayList<>(2));
            this.children.add(child);
        }

        /**
         * Rebuilds the cache of listeners, setting the {@link #listeners} field to the new array.
         *
         * <p>
         *     Important: To avoid a race condition, you must use the return value of this method as the source of truth.
         *     Attempting to read the {@link #listeners} field immediately after calling this method may observe
         *     unexpected results caused by concurrent calls to this method and/or {@link #forceRebuild()}.
         * </p>
         */
        private IEventListener[] buildCache() {
            if (parent != null && parent.shouldRebuild())
                parent.buildCache();

            ArrayList<IEventListener> ret = new ArrayList<>();
            for (EventPriority value : EVENT_PRIORITY_VALUES) {
                List<IEventListener> listeners = getListeners(value);
                if (listeners.isEmpty()) continue;
                if (phasesToTrack.contains(value))
                    ret.add(value); // Add the priority to notify the event of its current phase.
                ret.addAll(listeners);
            }

            var retArray = ret.isEmpty() ? NO_LISTENERS : ret.toArray(new IEventListener[0]);
            this.listeners = retArray;
            return retArray;
        }

        public void register(EventPriority priority, IEventListener listener) {
            if (listener == null) return;
            writeLock.acquireUninterruptibly();
            getListenersForPriority(priority).add(listener);
            writeLock.release();
            this.forceRebuild();
        }

        public void unregister(IEventListener listener) {
            writeLock.acquireUninterruptibly();
            boolean needsRebuild = false;
            for (var list : priorities) {
                if (list == null) continue;
                needsRebuild |= list.remove(listener);
            }
            if (needsRebuild) this.forceRebuild();
            writeLock.release();
        }

        private ArrayList<IEventListener> getListenersForPriority(EventPriority priority) {
            var listenersForPriority = priorities[priority.ordinal()];
            if (listenersForPriority == null)
                listenersForPriority = priorities[priority.ordinal()] = new ArrayList<>();

            return listenersForPriority;
        }

        private void setCancelable() {
            writeLock.acquireUninterruptibly();
            boolean needsRebuild = false;
            for (ArrayList<IEventListener> priority : priorities) {
                if (priority == null)
                    continue;
                for (int x = 0; x < priority.size(); x++) {
                    IEventListener old = priority.get(x);
                    if (old instanceof IReactiveEventListener) {
                        IEventListener cancelable = ((IReactiveEventListener)old).toCancelable();
                        if (old == cancelable)
                            continue;

                        needsRebuild = true;
                        priority.set(x, cancelable);
                    }
                }
            }
            if (needsRebuild) this.forceRebuild();
            writeLock.release();
        }
    }
}
