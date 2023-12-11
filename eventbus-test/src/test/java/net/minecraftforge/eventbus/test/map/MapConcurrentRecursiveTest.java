/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.test.map;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

public class MapConcurrentRecursiveTest extends MapTestBase {
    private static final int ITERATIONS = 500;
    private static final int THREADS = 32;
    private static final int OPERATIONS = 100;

    @RepeatedTest(ITERATIONS)
    public void concurrent() {
        test(cache("concurrent"));
    }
    @Disabled // This will time out a lot because it will make many copies of the map and take quite a while doing it.
    @RepeatedTest(ITERATIONS)
    public void copy() {
        test(cache("copy"));
    }
    @Disabled // This has no concurrency protection, and will have tons of cases where it returns a value different then the one initially set
    @RepeatedTest(ITERATIONS)
    public void hashMap() {
        test(new HashMap<>());
    }
    @Disabled // Identity doesn't cause collisions, but because the hashes are shared the internal structure will resize and can cause error
    @RepeatedTest(ITERATIONS)
    public void identityHashMap() {
        test(new IdentityHashMap<>());
    }
    @Disabled // I haven't run into any issues with this, however it is just stupidly slow as every operation is synchronized on the outside
    @RepeatedTest(ITERATIONS)
    public void identityHashMapSync() {
        test(Collections.synchronizedMap(new IdentityHashMap<>()));
    }
    @Disabled // Concurrent can cause a deadlock if called in a recursive way
    @RepeatedTest(ITERATIONS)
    public void concurrentHashMap() {
        test(new ConcurrentHashMap<>());
    }

    private String recursive(Map<SameHash, String> map, int depth) {
        if (depth == 0)
            return map.putIfAbsent(new SameHash(depth), "deep!");
        return map.computeIfAbsent(new SameHash(depth), k -> recursive(map, depth - 1));
    }

    private void test(Map<SameHash, String> map) {
        var pool = Executors.newFixedThreadPool(THREADS);
        var cdl = new CountDownLatch(1);

        var sharedKey = new SameHash(1);
        var sharedValue = "shared";
        map.put(sharedKey, sharedValue);

        var tasks = new ArrayList<Callable<Void>>();
        for (int x = 0; x < THREADS/10*2; x ++) {
            tasks.add(() -> {
                cdl.await(); // Wait for the starting gun!
                for (int y = 0; y < OPERATIONS; y++ ) {
                    var rand = new Random();
                    var key = new SameHash(rand.nextInt(100));
                    map.computeIfAbsent(key, k -> recursive(map, 10));
                }
                return null;
            });
        }
        // Readers, demonstrates that the wrong value can be returned if the map is restructuring while being read.
        for (int x = 0; x < THREADS/10*8; x ++) {
            tasks.add(() -> {
                cdl.await(); // Wait for the starting gun!
                for (int y = 0; y < OPERATIONS; y++) {
                    var got = map.get(sharedKey);
                    assertTrue(got == sharedValue, "Received incorrect values " + got + " - " + sharedValue);
                }
                return null;
            });
        }

        var futures = new ArrayList<Future<Void>>();
        for (var task : tasks)
            futures.add(pool.submit(task));

        // Start the races!
        cdl.countDown();

        assertTimeoutPreemptively(Duration.ofSeconds(TIMEOUT),
            () -> {
                for (var future : futures)
                   future.get();
            },
            () -> getWorkerError(pool, "Thread Timed out")
        );
    }

    private class SameHash {
        private int offset;
        private SameHash(int offset) {
            this.offset = offset;
        }
        @Override
        public int hashCode() { return 0xBADC0DE + offset; }
    }
}
