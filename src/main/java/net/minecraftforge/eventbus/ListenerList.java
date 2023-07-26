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

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;


public class ListenerList
{
    private static final ArrayList<ListenerList> allLists = new ArrayList<>();
    private static int maxSize = 0;

    @Nullable
    private final ListenerList parent;
    private ListenerListInst[] lists = new ListenerListInst[0];

    public ListenerList()
    {
        this(null);
    }

    public ListenerList(@Nullable ListenerList parent)
    {
        // parent needs to be set before resize !
        this.parent = parent;
        extendMasterList(this);
        resizeLists(maxSize);
    }

    private synchronized static void extendMasterList(ListenerList inst)
    {
        allLists.add(inst);
    }

    static void resize(int max)
    {
        if (max > maxSize)
        {
            synchronized (ListenerList.class)
            {
                if (max > maxSize)
                {
                    for (int i = 0, allListsSize = allLists.size(); i < allListsSize; i++) {
                        allLists.get(i).resizeLists(max);
                    }
                    maxSize = max;
                }
            }
        }
    }

    private synchronized void resizeLists(int max)
    {
        if (parent != null)
        {
            parent.resizeLists(max);
        }

        if (lists.length >= max)
        {
            return;
        }

        ListenerListInst[] newList = new ListenerListInst[max];
        int x = 0;
        for (; x < lists.length; x++)
        {
            newList[x] = lists[x];
        }
        for(; x < max; x++)
        {
            if (parent != null)
            {
                newList[x] = new ListenerListInst(parent.getInstance(x));
            }
            else
            {
                newList[x] = new ListenerListInst();
            }
        }
        lists = newList;
    }

    public static synchronized void clearBusID(int id)
    {
        for (ListenerList list : allLists)
        {
            list.lists[id].dispose();
        }
    }

    protected ListenerListInst getInstance(int id)
    {
        return lists[id];
    }

    public IEventListener[] getListeners(int id)
    {
        return lists[id].getListeners();
    }

    public void register(int id, EventPriority priority, IEventListener listener)
    {
        lists[id].register(priority, listener);
    }

    public void unregister(int id, IEventListener listener)
    {
        lists[id].unregister(listener);
    }

    public static synchronized void unregisterAll(int id, IEventListener listener)
    {
        for (int i = 0, allListsSize = allLists.size(); i < allListsSize; i++) {
            allLists.get(i).unregister(id, listener);
        }
    }

    private static class ListenerListInst
    {
        private boolean rebuild = true;
        private AtomicReference<IEventListener[]> listeners = new AtomicReference<>();
        private final ArrayList<IEventListener>[] priorities;
        private ListenerListInst parent;
        private List<ListenerListInst> children;
        private final Semaphore writeLock = new Semaphore(1, true);

        @SuppressWarnings("unchecked") // we're creating an array of ArrayList<IEventListener> objects
        private ListenerListInst()
        {
            final int count = EventPriority.VALUES_LENGTH;
            priorities = new ArrayList[count];

            for (int x = 0; x < count; x++)
            {
                priorities[x] = new ArrayList<IEventListener>();
            }
        }

        public void dispose()
        {
            writeLock.acquireUninterruptibly();
            Arrays.fill(priorities, null);
            writeLock.release();
            parent = null;
            listeners = null;
            if (children != null)
                children.clear();
        }

        private ListenerListInst(ListenerListInst parent)
        {
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
        public ArrayList<IEventListener> getListeners(EventPriority priority)
        {
            writeLock.acquireUninterruptibly();
            ArrayList<IEventListener> ret = new ArrayList<>(priorities[priority.ordinal()]);
            writeLock.release();
            if (parent != null)
            {
                ret.addAll(parent.getListeners(priority));
            }
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
        public IEventListener[] getListeners()
        {
            if (shouldRebuild()) buildCache();
            return listeners.get();
        }

        protected boolean shouldRebuild()
        {
            return rebuild;// || (parent != null && parent.shouldRebuild());
        }

        protected void forceRebuild()
        {
            this.rebuild = true;
            if (this.children != null) {
                synchronized (this.children) {
                    final List<ListenerListInst> listenerListInsts = this.children;
                    for (int i = 0, size = listenerListInsts.size(); i < size; i++) {
                        listenerListInsts.get(i).forceRebuild();
                    }
                }
            }
        }

        private void addChild(ListenerListInst child)
        {
            if (this.children == null)
                this.children = Collections.synchronizedList(new ArrayList<>(2));
            this.children.add(child);
        }

        /**
         * Rebuild the local Array of listeners, returns early if there is no work to do.
         */
        private void buildCache()
        {
            if(parent != null && parent.shouldRebuild())
            {
                parent.buildCache();
            }
            final ArrayList<IEventListener> ret = new ArrayList<>();

            final EventPriority[] values = EventPriority.values();
            for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
                final EventPriority value = values[i];
                final ArrayList<IEventListener> listeners = getListeners(value);
                if (!listeners.isEmpty()) {
                    ret.add(value); // Add the priority to notify the event of its current phase.
                    ret.addAll(listeners);
                }
            }
            this.listeners.set(ret.toArray(new IEventListener[0]));
            rebuild = false;
        }

        public void register(EventPriority priority, IEventListener listener)
        {
            writeLock.acquireUninterruptibly();
            priorities[priority.ordinal()].add(listener);
            writeLock.release();
            this.forceRebuild();
        }

        public void unregister(IEventListener listener)
        {
            writeLock.acquireUninterruptibly();
            Arrays.stream(priorities).filter(list -> list.remove(listener)).forEach(list -> this.forceRebuild());
            writeLock.release();
        }
    }
}
