/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Package-private occurrence-difference atom of GridVisibleRangeSupport.
 *
 * <p>Results preserve the original remove-first-equal algorithm's multiplicity,
 * encounter order and object references. Identity indexing is used only when
 * every non-null element inherits Object.equals. Subclasses with custom equality
 * retain the original algorithm, including its equals-call direction/order.</p>
 *
 * <p>Inputs must remain stable during a call; checkpoint callbacks must not
 * mutate them. Neither arrays, elements nor callbacks are retained by this class.
 * The ClassValue retains only per-class equality metadata, not widget instances.
 * No native addresses or widget resources are used.</p>
 */
final class GridVisibleRangeDiff {
    private static final ClassValue<Boolean> IDENTITY_EQUALITY = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("equals", Object.class).getDeclaringClass() == Object.class;
            } catch (NoSuchMethodException | SecurityException unavailable) {
                // Lack of reflective access never grants an optimization assumption.
                return false;
            }
        }
    };

    private GridVisibleRangeDiff() {
    }

    static final class Difference<T> {
        private final List<T> removed;
        private final List<T> added;

        Difference(List<T> removed, List<T> added) {
            this.removed = removed;
            this.added = added;
        }

        List<T> removed() {
            return removed;
        }

        List<T> added() {
            return added;
        }
    }

    /** Historical paint path: preserves its non-cancellable behavior. */
    static <T> Difference<T> between(T[] previous, T[] current) {
        return between(previous, current, null, false);
    }

    /** A caller may adapt its existing progress monitor as monitor::checkCanceled. */
    static <T> Difference<T> between(T[] previous, T[] current, Runnable checkpoint) {
        return between(previous, current, checkpoint, true);
    }

    private static <T> Difference<T> between(T[] previous, T[] current,
            Runnable checkpoint, boolean cancellable) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        check(checkpoint, cancellable);
        if (!identityOnly(previous, checkpoint, cancellable)
                || !identityOnly(current, checkpoint, cancellable)) {
            return equalityDifference(previous, current, checkpoint, cancellable);
        }
        if (sameReferences(previous, current, checkpoint, cancellable)) {
            return new Difference<>(Collections.emptyList(), Collections.emptyList());
        }
        return identityDifference(previous, current, checkpoint, cancellable);
    }

    private static boolean identityOnly(Object[] values, Runnable checkpoint, boolean cancellable) {
        for (Object value : values) {
            check(checkpoint, cancellable);
            if (value != null && !IDENTITY_EQUALITY.get(value.getClass())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameReferences(Object[] previous, Object[] current,
            Runnable checkpoint, boolean cancellable) {
        if (previous.length != current.length) {
            return false;
        }
        for (int i = 0; i < previous.length; i++) {
            check(checkpoint, cancellable);
            if (previous[i] != current[i]) {
                return false;
            }
        }
        return true;
    }

    private static <T> Difference<T> identityDifference(T[] previous, T[] current,
            Runnable checkpoint, boolean cancellable) {
        GridIdentityOccurrenceCounter<T> counts = new GridIdentityOccurrenceTable<>();
        for (T value : previous) {
            check(checkpoint, cancellable);
            counts.addPrevious(value);
        }
        List<T> added = new ArrayList<>();
        for (T value : current) {
            check(checkpoint, cancellable);
            if (!counts.matchCurrent(value)) {
                added.add(value);
            }
        }
        List<T> removed = new ArrayList<>();
        for (T value : previous) {
            check(checkpoint, cancellable);
            if (!counts.skipMatchedPrevious(value)) {
                removed.add(value);
            }
        }
        check(checkpoint, cancellable);
        return new Difference<>(removed, added);
    }

    private static <T> Difference<T> equalityDifference(T[] previous, T[] current,
            Runnable checkpoint, boolean cancellable) {
        ArrayList<T> removed = new ArrayList<>(Arrays.asList(previous));
        ArrayList<T> added = new ArrayList<>(Arrays.asList(current));
        Iterator<T> iterator = added.iterator();
        while (iterator.hasNext()) {
            check(checkpoint, cancellable);
            if (removed.remove(iterator.next())) {
                iterator.remove();
            }
        }
        check(checkpoint, cancellable);
        return new Difference<>(removed, added);
    }

    private static void check(Runnable checkpoint, boolean cancellable) {
        if (!cancellable) {
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Visible-range difference interrupted");
        }
        if (checkpoint != null) {
            checkpoint.run();
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Visible-range difference interrupted");
        }
    }
}
