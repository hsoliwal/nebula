// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.grid;

/** Valid-index endpoint queries checked independently against a scalar row oracle. */
public final class GridExtentBoundaryProof {
    private static long checks;
    private static long digest = 41;
    private GridExtentBoundaryProof() { }
    private static void equal(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
        digest = 31 * digest + actual;
    }
    private static void verify(GridRowExtents index, int[] extents) {
        long total = 0;
        int visible = 0;
        for (int extent : extents) { total += extent; if (extent != 0) visible++; }
        equal(total, index.totalExtent()); equal(visible, index.visibleCount());
        long[] offsets = {Long.MIN_VALUE, -1, 0, 1, total / 2,
                total == 0 ? 0 : total - 1, total, total + 1, Long.MAX_VALUE};
        for (long offset : offsets) {
            int expected = offset < 0 ? -1 : 0;
            long prefix = 0;
            if (offset >= 0) {
                for (int extent : extents) {
                    if (extent == 0) continue;
                    if (prefix + extent > offset) break;
                    prefix += extent; expected++;
                }
            }
            equal(expected, index.rankAtExtent(offset));
        }
    }
    public static void main(String[] args) {
        for (int size : new int[]{0,1,2,31,32,33,127,128,129,1025}) {
            for (int mask = 0; mask < 32; mask++) {
                int[] extents = new int[size];
                for (int i = 0; i < size; i++) {
                    extents[i] = (mask & (1 << (i % 5))) == 0 ? 0
                            : (i % 7 == 0 ? Integer.MAX_VALUE : i % 59 + 1);
                }
                var index = new GridRowExtents();
                index.rebuild(size, i -> extents[i]); verify(index, extents);
                for (int i = 0; i < size; i += Math.max(1, size / 17)) {
                    extents[i] = extents[i] == 0 ? Integer.MAX_VALUE : 0;
                    index.set(i, extents[i]); verify(index, extents);
                }
                var appended = new GridRowExtents(); appended.rebuild(0, i -> 0);
                for (int extent : extents) appended.append(extent);
                verify(appended, extents);
            }
        }
        System.out.println("GRID_ENDPOINT_PASS checks=" + checks + " digest=" + Long.toUnsignedString(digest));
    }
}
