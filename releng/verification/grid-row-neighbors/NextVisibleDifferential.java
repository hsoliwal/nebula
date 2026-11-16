/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.grid;

import java.util.ArrayList;
import java.util.Random;

/** Reference scan checks the real GridRowTree while never constructing native SWT controls. */
public final class NextVisibleDifferential {
    private static long queries;

    private static void check(GridRowTree tree, ArrayList<Integer> weights) {
        final int n = weights.size();
        int count = 0;
        for (int weight : weights) if (weight > 0) count++;
        if (tree.size() != n || tree.visibleCount() != count) {
            throw new AssertionError("row or visible count mismatch");
        }
        for (int position = -2; position <= n + 2; position++) {
            int expectedNext = -1;
            int expectedPrevious = -1;
            for (int row = 0; row < n; row++) {
                if (weights.get(row) > 0) {
                    if (row >= position && expectedNext == -1) expectedNext = row;
                    if (row <= position) expectedPrevious = row;
                }
            }
            int next = tree.nextVisible(position);
            int previous = tree.previousVisible(position);
            if (next != expectedNext || previous != expectedPrevious) {
                throw new AssertionError("at " + position + " expected " + expectedNext
                        + "/" + expectedPrevious + " got " + next + "/" + previous);
            }
            queries++;
        }
    }

    private static void mutate(Random random, int steps, int maxSize) {
        GridRowTree tree = new GridRowTree();
        ArrayList<Integer> weights = new ArrayList<>();
        ArrayList<Integer> slots = new ArrayList<>();
        for (int step = 0; step < steps; step++) {
            int operation = random.nextInt(5);
            if (operation < 2 && weights.size() < maxSize) {
                int index = random.nextInt(weights.size() + 1);
                int weight = random.nextInt(8) == 0 ? 0 : 1 + random.nextInt(90);
                int slot = tree.insertUnread(index, null, 1, weight)[0];
                weights.add(index, weight);
                slots.add(index, slot);
            } else if (operation < 3 && !weights.isEmpty()) {
                int index = random.nextInt(weights.size());
                tree.removeUnread(slots.remove(index));
                weights.remove(index);
            } else if (operation == 3 && !weights.isEmpty()) {
                int index = random.nextInt(weights.size());
                int weight = random.nextInt(4) == 0 ? 0 : 1 + random.nextInt(99);
                tree.reweighSlot(slots.get(index), weight);
                weights.set(index, weight);
            } else if (!weights.isEmpty()) {
                int updates = 1 + random.nextInt(Math.min(10, weights.size()));
                tree.beginBatch();
                try {
                    for (int k = 0; k < updates; k++) {
                        int index = random.nextInt(weights.size());
                        int weight = random.nextInt(3) == 0 ? 0 : 1 + random.nextInt(80);
                        tree.reweighSlot(slots.get(index), weight);
                        weights.set(index, weight);
                    }
                    check(tree, weights);
                } finally {
                    tree.endBatch();
                }
            }
            check(tree, weights);
        }
        tree.clear();
        weights.clear();
        check(tree, weights);
    }

    public static void main(String[] args) {
        Random random = new Random(8101292L);
        for (int run = 0; run < 60; run++) mutate(random, 120, 80);
        for (int n : new int[] { 0, 1, 2, 3, 7, 16, 32, 63, 127, 255, 1024, 4096 }) {
            GridRowTree tree = new GridRowTree();
            ArrayList<Integer> weights = new ArrayList<>();
            if (n > 0) {
                int[] slots = tree.insertUnread(0, null, n, 0);
                for (int row = 0; row < n; row++) {
                    int weight = ((row & 15) == 0) ? 1 : 0;
                    tree.reweighSlot(slots[row], weight);
                    weights.add(weight);
                }
            }
            if (n <= 255) {
                check(tree, weights);
            } else {
                for (int k = 0; k < 10000; k++) {
                    int position = random.nextInt(n + 5) - 2;
                    int next = -1;
                    int previous = -1;
                    for (int row = 0; row < n; row++) {
                        if (weights.get(row) > 0) {
                            if (row >= position && next == -1) next = row;
                            if (row <= position) previous = row;
                        }
                    }
                    if (tree.nextVisible(position) != next || tree.previousVisible(position) != previous) {
                        throw new AssertionError("n=" + n + " position=" + position);
                    }
                    queries++;
                }
            }
        }
        if (queries != 169397L) {
            throw new AssertionError("coverage drift, expected 169397 queries but got " + queries);
        }
        System.out.println("PASS native GridRowTree data-lane contracts: " + queries + " queries");
    }
}
