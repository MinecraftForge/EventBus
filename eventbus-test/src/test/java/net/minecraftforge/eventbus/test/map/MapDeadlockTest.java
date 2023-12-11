/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.test.map;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

@Disabled // These are time consuming tests, only run when screwing with the backing maps
public class MapDeadlockTest extends MapTestBase {
    private static final int ITERATIONS = 2500;

    @RepeatedTest(ITERATIONS)
    public void concurrent() {
        test(cache("concurrent"));
    }
    @RepeatedTest(ITERATIONS)
    public void copy() {
        test(cache("copy"));
    }
    @Disabled // This will illustrate why syncing is needed, it should fail every run because of concurrent modification detection
    @RepeatedTest(ITERATIONS)
    public void hashMap() {
        test(new HashMap<>());
    }
    @Disabled // This will illustrate why syncing is needed, it should fail a bunch of times for desynced values
    @RepeatedTest(ITERATIONS)
    public void identityHashMap() {
        test(new IdentityHashMap<>());
    }
    @RepeatedTest(ITERATIONS)
    public void identityHashMapSync() {
        test(Collections.synchronizedMap(new IdentityHashMap<>()));
    }
    // This will illustrate that ConcurrentHashMap is not a suitable implementation, because it doesn't allow
    // recursion which can happen often in our use case. It will either deadlock on a synchronized block or
    // throw a IllegalStateException("Recursive update")
    @Disabled
    @RepeatedTest(ITERATIONS)
    public void concurrentHashMap() {
        test(new ConcurrentHashMap<>());
    }
    private void test(Map<String, String> map) {
        var threads = 10;
        var pool = Executors.newFixedThreadPool(threads);
        var cdl = new CountDownLatch(1);
        var key = "key";

        var tasks = new ArrayList<Callable<Result>>();
        for (int x = 0; x < threads; x ++) {
            final int id = x;
            tasks.add(() -> {
                cdl.await(); // Wait for the starting gun!
                computeValue(map, "worker " + id + " recursive ", 10);
                return new Result(id, map.computeIfAbsent(key, k -> "worker " + id));
            });
        }

        var futures = new ArrayList<Future<Result>>();
        for (var task : tasks)
            futures.add(pool.submit(task));

        // Start the races!
        cdl.countDown();

        assertTimeoutPreemptively(Duration.ofSeconds(TIMEOUT),
            () -> {
                var results = new ArrayList<Result>();
                for (var future : futures)
                    results.add(future.get());
                validate(results);
            },
            () -> getWorkerError(pool, "Encountered Deadlock")
        );
    }

    private static String computeValue(Map<String, String> map, String prefix, int depth) {
        if (depth == 0) return prefix;
        return map.computeIfAbsent(prefix + depth, k -> computeValue(map, prefix, depth - 1));
    }

    private record Result(int worker, String data) {};
    private static void validate(List<Result> results) {
        var target = results.get(0).data();
        for (var result : results) {
            if (result.data() != target) {
                var error = new StringBuilder();
                error.append("Workers got desynced results:\n");
                for (var tmp : results)
                    error.append("\r" + tmp.worker() + ": " + tmp.data());
                fail(error.toString());
            }
        }
    }
}
