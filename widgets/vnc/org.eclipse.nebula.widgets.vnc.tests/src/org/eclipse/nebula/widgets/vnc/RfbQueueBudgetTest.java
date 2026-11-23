/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/** Byte-budget contract: a bounded number of messages alone is insufficient. */
public final class RfbQueueBudgetTest {

    @Test public void rejectsOversizedOrNegativeReservations() {
        AtomicInteger bytes = new AtomicInteger();
        assertFalse(RfbClient.reserveQueueBytes(bytes, -1));
        assertFalse(RfbClient.reserveQueueBytes(bytes, 4 * 1024 * 1024 + 1));
        assertEquals(0, bytes.get());
        assertTrue(RfbClient.reserveQueueBytes(bytes, 4 * 1024 * 1024));
        assertEquals(4 * 1024 * 1024, bytes.get());
        assertFalse(RfbClient.reserveQueueBytes(bytes, 1));
    }

    @Test public void permitsExactlyFourConcurrentOneMiBReservations() throws Exception {
        AtomicInteger bytes = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 16; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return RfbClient.reserveQueueBytes(bytes, 1024 * 1024);
                }));
            }
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(5, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertEquals(4, accepted);
            assertEquals(4 * 1024 * 1024, bytes.get());
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }
}
