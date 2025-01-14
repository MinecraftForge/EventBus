/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class ListenerList {
    private static final List<ListenerList> allLists = new ArrayList<>();
    private static int maxSize = 0;

    @Nullable
    private final ListenerList parent;
    private ListenerListInst[] lists = new ListenerListInst[0];

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

        private boolean rebuild = true;
        private AtomicReference<IEventListener[]> listeners = new AtomicReference<>();
        private final @Nullable ArrayList<IEventListener>[] priorities;
        private ListenerListInst parent;
        private List<ListenerListInst> children;
        private final Semaphore writeLock = new Semaphore(1, true);

        @SuppressWarnings("unchecked")
        private ListenerListInst() {
            // Make a lazy-loaded array of lists containing listeners for each priority level.
            priorities = (ArrayList<IEventListener>[]) new ArrayList[EVENT_PRIORITY_VALUES.length];
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
            listeners = null;
            if (children != null)
                children.clear();
        }

        private ListenerListInst(ListenerListInst parent) {
            this();
            this.parent = parent;
            this.parent.addChild(this);
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
            if (shouldRebuild()) buildCache();
            return listeners.get();
        }

        protected boolean shouldRebuild() {
            return rebuild;// || (parent != null && parent.shouldRebuild());
        }

        protected void forceRebuild() {
            this.rebuild = true;
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
         * Rebuild the local Array of listeners, returns early if there is no work to do.
         */
        private void buildCache() {
            if (parent != null && parent.shouldRebuild())
                parent.buildCache();

            ArrayList<IEventListener> ret = new ArrayList<>();
            for (EventPriority value : EVENT_PRIORITY_VALUES) {
                List<IEventListener> listeners = getListeners(value);
                if (listeners.isEmpty()) continue;
                ret.add(value); // Add the priority to notify the event of its current phase.
                ret.addAll(listeners);
            }
            this.listeners.set(ret.toArray(new IEventListener[0]));
            rebuild = false;
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
    }
}
