/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package de.linearbits.swt.simplebrowser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

/** Headless donor-contract and viewport-backing-store regression tests. */
public class HTMLCacheTest {

    @Test
    public void originalUnboundedConstructorRetainsDonorApi() {
        HTMLCache<Integer> cache = new HTMLCache<>();
        for (int index = 0; index < 200; index++) {
            cache.put("key" + index, index);
        }
        assertEquals(200, cache.list().size());
        assertTrue(cache.contains("key199"));
        assertEquals(Integer.valueOf(199), cache.get("key199"));
        cache.put("nullable", null);
        assertTrue(cache.contains("nullable"));
        assertNull(cache.get("nullable"));
        cache.clear();
        assertTrue(cache.list().isEmpty());
    }

    @Test
    public void boundedConstructorEvictsLeastRecentlyAccessedEntry() {
        HTMLCache<String> cache = new HTMLCache<>(2);
        cache.put("a", "A");
        cache.put("b", "B");
        assertEquals("A", cache.get("a"));
        cache.put("c", "C");
        assertTrue(cache.contains("a"));
        assertFalse(cache.contains("b"));
        assertTrue(cache.contains("c"));
        assertEquals(2, cache.list().size());
        cache.put("a", "AA");
        assertEquals("AA", cache.get("a"));
        assertEquals(2, cache.list().size());
    }

    @Test
    public void rejectsNonpositiveCacheCapacity() {
        for (int capacity : new int[] { 0, -1, Integer.MIN_VALUE }) {
            try {
                new HTMLCache<>(capacity);
                fail("Capacity accepted: " + capacity);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("maxEntries"));
            }
        }
    }

    @Test
    public void instancesNeverShareCachedDocuments() {
        HTMLCache<String> first = new HTMLCache<>(1);
        HTMLCache<String> second = new HTMLCache<>(1);
        first.put("page", "trusted");
        assertFalse(second.contains("page"));
        assertEquals("trusted", first.get("page"));
        first.clear();
        assertFalse(first.contains("page"));
    }

    @Test
    public void concurrentPutAndGetCannotBreakCapacity() throws InterruptedException {
        final HTMLCache<Integer> cache = new HTMLCache<>(64);
        final int workers = 6;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        for (int worker = 0; worker < workers; worker++) {
            final int workerId = worker;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    for (int item = 0; item < 500; item++) {
                        String key = workerId + ":" + item;
                        cache.put(key, item);
                        cache.get(key);
                    }
                } catch (Throwable problem) {
                    failure.compareAndSet(null, problem);
                } finally {
                    done.countDown();
                }
            }, "nebula-html-cache-" + worker);
            thread.start();
        }
        start.countDown();
        done.await();
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        assertTrue(cache.list().size() <= 64);
    }
}
