// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Executes the actual production diff atom, not simulated SWT classes. */
public final class GridVisibleRangeDiffContract {
    private static long assertions;
    private static final List<String> CALLS = new ArrayList<>();

    private GridVisibleRangeDiffContract() {
    }

    public static void main(String[] args) {
        examples();
        randomIdentity();
        equalityFallback();
        cancellation();
        scale();
        System.out.println("Visible-range differential assertions: " + assertions);
        System.out.println("PASS: actual production diff; identity/order/multiplicity/equality/cancellation.");
        System.out.println("NOT RUN here: SWT paint/event runtime or full Nebula Tycho build.");
    }

    private static void examples() {
        Object a = new Object(), b = new Object(), c = new Object();
        verify(new Object[] { a, b, a }, new Object[] { a });
        same(new Object[] { b, a }, GridVisibleRangeDiff.between(
                new Object[] { a, b, a }, new Object[] { a }).removed().toArray());
        verify(new Object[] { a, null, b }, new Object[] { null, c, a });
        verify(new Object[] { a, b }, new Object[] { b, a });
        verify(new Object[0], new Object[] { null, null, a });
        verify(new Object[] { a, null, a }, new Object[0]);
        verify(new Object[0], new Object[0]);
        verify(new Object[] { new HostileHash() }, new Object[] { new HostileHash() });
        Object[] stable = { a, b, c };
        verify(stable, stable);
        expect(NullPointerException.class, () -> GridVisibleRangeDiff.between(null, stable));
        expect(NullPointerException.class, () -> GridVisibleRangeDiff.between(stable, null));
    }

    private static void randomIdentity() {
        Random random = new Random(0x4e4542554c41L);
        Object[] pool = new Object[31];
        for (int i = 1; i < pool.length; i++) {
            pool[i] = (i & 1) == 0 ? new Object() : new HostileHash();
        }
        for (int trial = 0; trial < 20000; trial++) {
            Object[] previous = new Object[random.nextInt(81)];
            Object[] current = new Object[random.nextInt(81)];
            for (int i = 0; i < previous.length; i++) {
                previous[i] = pool[random.nextInt(pool.length)];
            }
            for (int i = 0; i < current.length; i++) {
                current[i] = pool[random.nextInt(pool.length)];
            }
            verify(previous, current);
        }
    }

    private static void equalityFallback() {
        Random random = new Random(1091);
        for (int trial = 0; trial < 2000; trial++) {
            Object[] previous = new Object[random.nextInt(30)];
            Object[] current = new Object[random.nextInt(30)];
            for (int i = 0; i < previous.length; i++) {
                previous[i] = new Equality(i, random.nextInt(7), (trial & 1) != 0);
            }
            for (int i = 0; i < current.length; i++) {
                current[i] = new Equality(100 + i, random.nextInt(7), (trial & 1) != 0);
            }
            CALLS.clear();
            Pair expected = legacy(previous, current);
            List<String> expectedCalls = new ArrayList<>(CALLS);
            CALLS.clear();
            GridVisibleRangeDiff.Difference<Object> actual = GridVisibleRangeDiff.between(previous, current);
            same(expected.removed, actual.removed().toArray());
            same(expected.added, actual.added().toArray());
            truth(expectedCalls.equals(CALLS), "custom equality invocation order/direction");
        }
        Object throwing = new Object() {
            @Override
            public boolean equals(Object other) {
                throw new IllegalArgumentException("custom-equals");
            }
            @Override
            public int hashCode() {
                return 0;
            }
        };
        expect(IllegalArgumentException.class, () -> GridVisibleRangeDiff.between(
                new Object[] { new Object() }, new Object[] { throwing }));
        // Even identical references must not bypass overridden equals.
        expect(IllegalArgumentException.class, () -> GridVisibleRangeDiff.between(
                new Object[] { throwing }, new Object[] { throwing }));
    }

    private static void cancellation() {
        Object a = new Object(), b = new Object();
        Object[] previous = { a, b, a, null }, current = { b, null, null };
        int[] checkpoints = { 0 };
        GridVisibleRangeDiff.between(previous, current, () -> checkpoints[0]++);
        for (int at = 1; at <= checkpoints[0]; at++) {
            int failAt = at;
            int[] calls = { 0 };
            expect(CancellationException.class, () -> GridVisibleRangeDiff.between(previous, current, () -> {
                if (++calls[0] == failAt) {
                    throw new CancellationException("injected");
                }
            }));
            same(new Object[] { a, b, a, null }, previous);
            same(new Object[] { b, null, null }, current);
            verify(previous, current);
        }
        Thread.currentThread().interrupt();
        try {
            verify(previous, current); // Historical call does not acquire cancellation semantics.
            expect(CancellationException.class, () -> GridVisibleRangeDiff.between(previous, current, null));
            truth(Thread.currentThread().isInterrupted(), "interrupted flag preserved");
        } finally {
            Thread.interrupted();
        }
        expect(CancellationException.class, () -> GridVisibleRangeDiff.between(previous, current,
                () -> Thread.currentThread().interrupt()));
        truth(Thread.interrupted(), "callback interruption preserved until caller clears it");
        verify(previous, current);
    }

    private static void scale() {
        int size = 100000;
        Object[] previous = new Object[size], current = new Object[size];
        for (int i = 0; i < size; i++) {
            previous[i] = new Object();
        }
        System.arraycopy(previous, 1, current, 0, size - 1);
        current[size - 1] = new Object();
        int[] work = { 0 };
        GridVisibleRangeDiff.Difference<Object> result = GridVisibleRangeDiff.between(
                previous, current, () -> work[0]++);
        same(new Object[] { previous[0] }, result.removed().toArray());
        same(new Object[] { current[size - 1] }, result.added().toArray());
        truth(work[0] <= 5L * size + 4, "linear identity pass checkpoint bound");
        System.out.println("100000-item shift checkpoints: " + work[0]);
        int[] stationary = { 0 };
        result = GridVisibleRangeDiff.between(previous, previous.clone(), () -> stationary[0]++);
        same(new Object[0], result.removed().toArray());
        same(new Object[0], result.added().toArray());
        truth(stationary[0] <= 3L * size + 2, "stationary range skips identity index construction");
        System.out.println("100000-item stationary checkpoints: " + stationary[0]);
    }

    private static void verify(Object[] previous, Object[] current) {
        Object[] oldCopy = previous.clone(), newCopy = current.clone();
        Pair expected = legacy(previous, current);
        GridVisibleRangeDiff.Difference<Object> actual = GridVisibleRangeDiff.between(previous, current);
        same(expected.removed, actual.removed().toArray());
        same(expected.added, actual.added().toArray());
        same(oldCopy, previous);
        same(newCopy, current);
    }

    /** Exact historical list algorithm, independent from identity indexing. */
    private static Pair legacy(Object[] previous, Object[] current) {
        ArrayList<Object> removed = new ArrayList<>();
        removed.addAll(Arrays.asList(previous));
        ArrayList<Object> added = new ArrayList<>();
        added.addAll(Arrays.asList(current));
        Iterator<Object> iterator = added.iterator();
        while (iterator.hasNext()) {
            if (removed.remove(iterator.next())) {
                iterator.remove();
            }
        }
        return new Pair(removed.toArray(), added.toArray());
    }

    private static final class Pair {
        final Object[] removed;
        final Object[] added;
        Pair(Object[] removed, Object[] added) {
            this.removed = removed;
            this.added = added;
        }
    }

    private static final class HostileHash {
        @Override
        public int hashCode() {
            throw new AssertionError("identity path must not call user hashCode");
        }
    }

    private static final class Equality {
        final int id;
        final int value;
        final boolean asymmetric;
        Equality(int id, int value, boolean asymmetric) {
            this.id = id;
            this.value = value;
            this.asymmetric = asymmetric;
        }
        @Override
        public boolean equals(Object other) {
            CALLS.add(id + ":" + (other instanceof Equality ? ((Equality) other).id : "other"));
            return other instanceof Equality && (asymmetric
                    ? value <= ((Equality) other).value : value == ((Equality) other).value);
        }
        @Override
        public int hashCode() {
            throw new AssertionError("fallback must preserve equals without introducing hashCode");
        }
    }

    private static void same(Object[] expected, Object[] actual) {
        truth(expected.length == actual.length, "length");
        for (int i = 0; i < expected.length; i++) {
            truth(expected[i] == actual[i], "identity and encounter order at " + i);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (failure.getClass() == type) {
                return;
            }
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void truth(boolean condition, String label) {
        assertions++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
