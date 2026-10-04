// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.algorithms.corpus.ChallengePostingIntersection.Prepared;
import com.synexia.job.IProgressMonitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Sparse OR/AND composition over the existing immutable posting and Java/JNI intersection owner.
 * No corpus-sized universe or bitmap is allocated; similarity and source authority are not involved.
 */
final class ChallengePostingQuery {
    private ChallengePostingQuery() { }

    /**
     * Full union, never a result-prefix shortcut. maxValues is a hard distinct-value budget;
     * exceeding it fails instead of losing candidates before a later AND. Lists may overlap.
     * Memory is O(number of lists + union cardinality), excluding already prepared inputs.
     */
    static Prepared union(List<Prepared> input, int maxValues, IProgressMonitor supplied) {
        if (maxValues < 0) throw new IllegalArgumentException("negative union budget");
        List<Prepared> lists = List.copyOf(input);
        IProgressMonitor monitor = ProgressMonitors.nonNull(supplied);
        ProgressMonitors.checkCanceled(monitor);
        if (lists.size() == 1) {
            Prepared only = lists.getFirst();
            if (only.size() > maxValues) throw new IllegalArgumentException("union budget exceeded");
            return only;
        }
        PriorityQueue<Cursor> heap = unionHeap(lists, monitor);
        int[] result = new int[Math.min(16, maxValues)];
        int size = 0;
        int polls = 0;
        while (!heap.isEmpty()) {
            if ((polls++ & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            Cursor cursor = heap.remove();
            int value = cursor.value();
            if (size == 0 || result[size - 1] != value) {
                if (size == maxValues) throw new IllegalArgumentException("union budget exceeded");
                if (size == result.length) {
                    int capacity = (int) Math.min(maxValues, Math.max(1L, (long) result.length * 2L));
                    result = Arrays.copyOf(result, capacity);
                }
                result[size++] = value;
            }
            cursor.position++;
            if (cursor.position < cursor.postings.size()) heap.add(cursor);
        }
        ProgressMonitors.checkCanceled(monitor);
        return ChallengePostingIntersection.prepare(Arrays.copyOf(result, size), supplied);
    }

    /**
     * AND in stable cardinality order. Only the final intersection is limited: limiting an
     * intermediate prefix could discard a larger ordinal that satisfies a later clause.
     * An empty clause list requires an explicit universe and is rejected here.
     */
    static int[] intersect(List<Prepared> input, int limit, IProgressMonitor supplied) {
        if (limit < 0) throw new IllegalArgumentException("negative result limit");
        ArrayList<Prepared> clauses = new ArrayList<>(List.copyOf(input));
        if (clauses.isEmpty()) throw new IllegalArgumentException("AND requires at least one clause");
        IProgressMonitor monitor = ProgressMonitors.nonNull(supplied);
        ProgressMonitors.checkCanceled(monitor);
        clauses.sort(Comparator.comparingInt(Prepared::size));
        Prepared current = clauses.getFirst();
        if (limit == 0 || current.size() == 0) return new int[0];
        // Keep explicit progress callbacks and full-result/pairwise JNI dispatch unchanged.
        if (supplied == null && clauses.size() >= 3 && limit < current.size()) {
            return intersectLimited(clauses, limit, monitor);
        }
        for (int index = 1; index < clauses.size(); index++) {
            ProgressMonitors.checkCanceled(monitor);
            boolean last = index == clauses.size() - 1;
            int[] result = ChallengePostingIntersection.intersect(
                    current, clauses.get(index), last ? limit : Integer.MAX_VALUE, supplied);
            if (last || result.length == 0) return result;
            current = ChallengePostingIntersection.prepare(result, supplied);
        }
        int[] result = new int[Math.min(limit, current.size())];
        for (int index = 0; index < result.length; index++) {
            if ((index & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            result[index] = current.valueAt(index);
        }
        ProgressMonitors.checkCanceled(monitor);
        return result;
    }

    /**
     * Seek simultaneous agreement; never materialize or truncate an intermediate intersection.
     * Positions are monotone and values remain signed ints, including both extrema. Advancing
     * positions rather than candidate+1 avoids sentinel collisions and integer overflow.
     * Extra storage is O(clause count + final limit), excluding immutable prepared inputs.
     */
    private static int[] intersectLimited(List<Prepared> clauses, int limit, IProgressMonitor monitor) {
        int[] positions = new int[clauses.size()];
        int[] result = new int[limit];
        Prepared lead = clauses.getFirst();
        int count = 0;
        int polls = 0;
        search:
        while (positions[0] < lead.size() && count < limit) {
            int candidate = lead.valueAt(positions[0]);
            for (int lane = 1; lane < clauses.size(); lane++) {
                if ((polls++ & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
                Prepared clause = clauses.get(lane);
                int position = clause.advanceFrom(positions[lane], candidate);
                positions[lane] = position;
                if (position == clause.size()) break search;
                int value = clause.valueAt(position);
                if (value > candidate) {
                    positions[0] = lead.advanceFrom(positions[0], value);
                    continue search;
                }
            }
            result[count++] = candidate;
            positions[0]++;
        }
        ProgressMonitors.checkCanceled(monitor);
        return count == result.length ? result : Arrays.copyOf(result, count);
    }

    /**
     * For a selective AND clause, seek candidates in broad OR facets without building their
     * union. Full union/intersection remains the fallback for broad or monitored queries.
     */
    static int[] intersectGrouped(List<Prepared> requiredInput, List<List<Prepared>> anyOfInput,
            int limit, int unionBudget, IProgressMonitor supplied) {
        if (limit < 0 || unionBudget < 0) throw new IllegalArgumentException("negative budget");
        List<Prepared> required = List.copyOf(requiredInput);
        List<List<Prepared>> groups = anyOfInput.stream().map(List::copyOf).toList();
        IProgressMonitor monitor = ProgressMonitors.nonNull(supplied);
        ProgressMonitors.checkCanceled(monitor);
        if (required.isEmpty() && groups.isEmpty()) {
            throw new IllegalArgumentException("grouped query requires a clause");
        }
        if (limit == 0 || groups.stream().anyMatch(List::isEmpty)
                || required.stream().anyMatch(posting -> posting.size() == 0)) return new int[0];
        // Facet-only top-k: prove every full union fits its budget before skipping materialization.
        // Explicit monitors retain the old callbacks and full-result Java/JNI composition path.
        if (supplied == null && required.isEmpty()) {
            int driver = prefixDriver(groups, limit, unionBudget, monitor);
            if (driver >= 0) return intersectFacetPrefix(groups, driver, limit, monitor);
        }
        Prepared lead = required.stream().min(Comparator.comparingInt(Prepared::size)).orElse(null);
        long alternatives = groups.stream().flatMap(List::stream).mapToLong(Prepared::size).sum();
        if (supplied == null && lead != null && lead.size() > 0
                && (long) lead.size() * 16L <= alternatives) {
            int[] result = new int[Math.min(limit, lead.size())];
            int[] requiredPositions = new int[required.size()];
            int[][] groupPositions = groups.stream().map(group -> new int[group.size()]).toArray(int[][]::new);
            int count = 0;
            for (int candidateIndex = 0; candidateIndex < lead.size() && count < result.length;
                    candidateIndex++) {
                if ((candidateIndex & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
                int candidate = lead.valueAt(candidateIndex);
                boolean matches = true;
                for (int lane = 0; lane < required.size(); lane++) {
                    Prepared posting = required.get(lane);
                    int position = posting.advanceFrom(requiredPositions[lane], candidate);
                    requiredPositions[lane] = position;
                    if (position == posting.size() || posting.valueAt(position) != candidate) {
                        matches = false;
                        break;
                    }
                }
                if (!matches) continue;
                for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                    List<Prepared> group = groups.get(groupIndex);
                    boolean found = false;
                    for (int lane = 0; lane < group.size(); lane++) {
                        Prepared posting = group.get(lane);
                        int position = posting.advanceFrom(groupPositions[groupIndex][lane], candidate);
                        groupPositions[groupIndex][lane] = position;
                        if (position < posting.size() && posting.valueAt(position) == candidate) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) { matches = false; break; }
                }
                if (matches) result[count++] = candidate;
            }
            ProgressMonitors.checkCanceled(monitor);
            return count == result.length ? result : Arrays.copyOf(result, count);
        }
        ArrayList<Prepared> clauses = new ArrayList<>(required);
        for (List<Prepared> group : groups) clauses.add(union(group, unionBudget, supplied));
        return intersect(clauses, limit, supplied);
    }
    /**
     * Sum of list lengths is a conservative union-cardinality bound, not an estimate accepted
     * as a proof. If any bound exceeds the budget, use the old full-union validation instead.
     * This preserves budget failures even when a small final answer would fit in the output.
     */
    private static int prefixDriver(List<List<Prepared>> groups, int limit, int unionBudget,
            IProgressMonitor monitor) {
        if ((long) limit * 16L > unionBudget) return -1;
        int polls = 0;
        long smallest = Long.MAX_VALUE;
        int driver = -1;
        for (int group = 0; group < groups.size(); group++) {
            long upper = 0;
            for (Prepared posting : groups.get(group)) {
                if ((polls++ & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
                upper += posting.size();
                if (upper > unionBudget) return -1;
            }
            if (upper < smallest) {
                smallest = upper;
                driver = group;
            }
        }
        return (long) limit * 16L <= smallest ? driver : -1;
    }

    /**
     * Merge a driver's OR lists lazily; retain a value only after all other OR facets agree.
     * A failed facet supplies the next possible value, so existing exponential seeks can skip
     * whole driver gaps. Never truncate an intermediate union. Full signed-int ordering and both
     * extrema remain valid: exhaustion is represented by positions, not an integer sentinel.
     * Extra storage is O(selected lists + final limit), not O(full union cardinalities).
     */
    private static int[] intersectFacetPrefix(List<List<Prepared>> groups, int driver,
            int limit, IProgressMonitor monitor) {
        PriorityQueue<Cursor> heap = unionHeap(groups.get(driver), monitor);
        int[][] positions = new int[groups.size()][];
        for (int group = 0; group < groups.size(); group++) {
            positions[group] = new int[group == driver ? 0 : groups.get(group).size()];
        }
        int[] result = new int[limit];
        int count = 0;
        int polls = 0;
        int previous = 0;
        boolean havePrevious = false;
        search:
        while (!heap.isEmpty() && count < limit) {
            if ((polls++ & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            Cursor cursor = heap.remove();
            int candidate = cursor.value();
            cursor.position++;
            if (cursor.position < cursor.postings.size()) heap.add(cursor);
            if (havePrevious && candidate == previous) continue;
            havePrevious = true;
            previous = candidate;
            for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                if (groupIndex == driver) continue;
                List<Prepared> group = groups.get(groupIndex);
                boolean matches = false;
                boolean hasFuture = false;
                int next = 0;
                for (int lane = 0; lane < group.size(); lane++) {
                    if ((polls++ & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
                    Prepared posting = group.get(lane);
                    int position = posting.advanceFrom(positions[groupIndex][lane], candidate);
                    positions[groupIndex][lane] = position;
                    if (position == posting.size()) continue;
                    int value = posting.valueAt(position);
                    if (value == candidate) { matches = true; break; }
                    if (!hasFuture || value < next) next = value;
                    hasFuture = true;
                }
                if (matches) continue;
                if (!hasFuture) break search;
                // No member of this facet can match below next. Advance each affected driver
                // cursor once to that lower bound; do not scan the skipped values individually.
                while (!heap.isEmpty() && heap.peek().value() < next) {
                    if ((polls++ & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
                    Cursor skipped = heap.remove();
                    skipped.position = skipped.postings.advanceFrom(skipped.position, next);
                    if (skipped.position < skipped.postings.size()) heap.add(skipped);
                }
                continue search;
            }
            result[count++] = candidate;
        }
        ProgressMonitors.checkCanceled(monitor);
        return count == result.length ? result : Arrays.copyOf(result, count);
    }

    /** Reuse the same stable heap/cursor shape for full unions and bounded facet execution. */
    private static PriorityQueue<Cursor> unionHeap(List<Prepared> lists, IProgressMonitor monitor) {
        PriorityQueue<Cursor> heap = new PriorityQueue<>(
                Comparator.comparingInt(Cursor::value).thenComparingInt(cursor -> cursor.lane));
        for (int lane = 0; lane < lists.size(); lane++) {
            if ((lane & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            Prepared list = lists.get(lane);
            if (list.size() != 0) heap.add(new Cursor(list, lane));
        }
        return heap;
    }

    /** One reusable cursor per selected OR list, not one object per posting. */
    private static final class Cursor {
        private final Prepared postings;
        private final int lane;
        private int position;

        private Cursor(Prepared postings, int lane) {
            this.postings = postings;
            this.lane = lane;
        }

        private int value() {
            return postings.valueAt(position);
        }
    }
}
