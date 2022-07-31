package net.minecraftforge.eventbus;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

/*
 *
 * Helper class that should be faster then ConcurrentHashMap,
 * yet still manages to properly deal with many threads.
 */
public class LockHelper<K,V> {
    private ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Map<K, V> map;

    public LockHelper(Map<K, V> map) {
        this.map = map;
    }

    public V get(K key) {
        var readLock =  lock.readLock();
        readLock.lock();
        var ret = map.get(key);
        readLock.unlock();
        return ret;
    }

    public boolean containsKey(K key) {
        var readLock =  lock.readLock();
        readLock.lock();
        var ret = map.containsKey(key);
        readLock.unlock();
        return ret;
    }

    public V computeIfAbsent(K key, Supplier<V> factory) {
        return get(key, factory, Function.identity());
    }

    @Deprecated(forRemoval = true, since = "6.0") // I chose a stupid name, it should be computeIfAbsent
    public <I> V get(K key, Supplier<I> factory, Function<I, V> finalizer) {
        return computeIfAbsent(key, factory, finalizer);
    }

    public <I> V computeIfAbsent(K key, Supplier<I> factory, Function<I, V> finalizer) {
        // let's take the read lock
        var readLock = lock.readLock();
        readLock.lock();
        var ret = map.get(key);
        readLock.unlock();

        // If the map had a value, return it.
        if (ret != null)
            return ret;

        // Let's pre-compute our new value. This could take a while, as well as recursively call this
        // function. as such, we need to make sure we don't hold a lock when we do this, otherwise
        // we could conflict with the class init global lock that is implicitly present
        var intermediate = factory.get();

        // having computed a value, we'll grab the write lock.
        // We'll also take the read lock, so we're very clear we have _both_ locks here.
        var writeLock = lock.writeLock();
        writeLock.lock();
        readLock.lock();

        // Check if some other thread already created a value
        ret = map.get(key);
        if (ret == null) {
            // Run any finalization we need, this was added because ClassLoaderFactory will actually define the class here
            ret = finalizer.apply(intermediate);
            // Update the map
            map.put(key, ret);
        }
        // Unlock ourselves, as the map has been updated
        readLock.unlock();
        writeLock.unlock();

        return ret;
    }

    public void clearAll() {
        map.clear();
        lock = new ReentrantReadWriteLock(true);
    }
}
