// Copyright 2026 Hitesh Soliwal and contributors.
// SPDX-License-Identifier: Apache-2.0
import java.util.Random;

/** Standalone count-atom discriminator. Not a compiled SWT/Nebula widget. */
public final class GridStoredRowAtomProof {
    private static long checks;
    private static long legacyDiscrepancies;
    private static void equal(int expected, int observed) {
        checks++;
        if (expected != observed) throw new AssertionError("expected=" + expected + ", observed=" + observed);
    }
    private static int historical(Object[][] rows, int[] ids) {
        int count = 0;
        for (int id : ids) {
            if (id < rows.length && rows[id] != null) {
                for (Object value : rows[id]) {
                    if (value != null) { count++; break; }
                }
            }
        }
        return count;
    }
    private static int candidate(Object[][] rows, int[] ids) {
        int count = 0;
        for (int id : ids) {
            if (id < rows.length && rows[id] != null) count++;
        }
        return count;
    }
    private static int expected(Object[][] rows, int[] ids) {
        int count = 0;
        for (int id : ids) {
            // Retained-row membership is based on a row array being resident.
            if (id >= 0 && id < rows.length && rows[id] != null) ++count;
        }
        return count;
    }
    public static void main(String[] args) {
        Object[][] rows = { {null}, {}, {42}, null, {null, null}, {null, 43}, {} };
        int[] ids = {0, 1, 2, 3, 4, 5, 6};
        equal(6, candidate(rows, ids));
        equal(2, historical(rows, ids));
        legacyDiscrepancies++;
        // Removing an allocated row, not nulling its values, removes its membership.
        rows[0] = null;
        equal(5, candidate(rows, ids));
        rows[1] = new Object[]{null};
        equal(5, candidate(rows, ids));
        rows[3] = new Object[0];
        equal(6, candidate(rows, ids));

        Random rng = new Random(0x4d3347524944L);
        for (int trial = 0; trial < 100_000; trial++) {
            int slots = rng.nextInt(30);
            Object[][] sample = new Object[slots][];
            for (int i = 0; i < slots; i++) {
                if (rng.nextInt(7) == 0) continue;
                sample[i] = new Object[rng.nextInt(6)];
                for (int j = 0; j < sample[i].length; j++) {
                    if (rng.nextInt(4) == 0) sample[i][j] = i;
                }
            }
            int[] selected = new int[rng.nextInt(slots+1)];
            for (int i = 0; i < selected.length; i++) {
                selected[i] = rng.nextInt(slots+1); // last value may be outside allocated rows
            }
            int want = expected(sample, selected);
            equal(want, candidate(sample, selected));
            if (historical(sample, selected) != want) legacyDiscrepancies++;
        }
        if (legacyDiscrepancies < 1000) throw new AssertionError("baseline no longer reproduces");
        System.out.println("M3_GRID_COUNT_ATOM_PASS trials=100000 checks=" + checks
                + " legacy_discrepancies=" + legacyDiscrepancies);
    }
}
