// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.pshelf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** No native SWT handles required. Exact preimage geometry is the oracle. */
public final class PShelfViewportGeometryTest {
    private static long assertions;

    private static void eq(int[] expected, int[] result, String name) {
        assertions++;
        if (!Arrays.equals(expected, result)) {
            throw new AssertionError(name + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(result));
        }
    }

    private static int[] oldSteady(int count, int open, int clientY, int clientH, int headerH) {
        ArrayList<Integer> boxed = new ArrayList<>();
        int y = clientY;
        for (int i = 0; i < count; i++) {
            boxed.add(Integer.valueOf(y));
            y += headerH;
            if (i == open) y = clientY + clientH - headerH * (count - i - 1);
        }
        int[] result = new int[boxed.size()];
        for (int i = 0; i < result.length; i++) result[i] = boxed.get(i);
        return result;
    }

    private static int[] oldAnimated(int count, int previous, int next,
            int y, int h, int header, double progress, boolean fromBottom) {
        ArrayList<Integer> boxed = new ArrayList<>();
        int yTop = y;
        int yBottom = y + h - header * (count - (next + 1));
        int area = h - header * count;
        int gap = (int) (area * (fromBottom ? 1 - progress : progress));
        boolean assigned = false;
        for (int i = 0; i < count; i++) {
            if (i <= previous) {
                boxed.add(Integer.valueOf(yTop));
                yTop += header;
            } else if (i > next) {
                boxed.add(Integer.valueOf(yBottom));
                yBottom += header;
            } else {
                if (!assigned) {
                    yTop += gap;
                    assigned = true;
                }
                boxed.add(Integer.valueOf(yTop));
                yTop += header;
            }
        }
        int[] result = new int[boxed.size()];
        for (int i = 0; i < result.length; i++) result[i] = boxed.get(i);
        return result;
    }

    public static void main(String[] ignored) {
        double[] progress = {0d, .000001d, .02d, .25d, .5d, .75d, .98d, 1d};
        for (int count = 0; count <= 20; count++) {
            for (int open = -1; open < count; open++) {
                for (int client = -80; client <= 100; client += 30) {
                    for (int header = -5; header <= 40; header += 9) {
                        eq(oldSteady(count, open, -20, client, header),
                                PShelfViewportGeometry.steady(count, open, -20, client, header),
                                "steady");
                        for (int previous = -1; previous < count; previous++) {
                            for (double tick : progress) {
                                for (boolean fromBottom : new boolean[] {false, true}) {
                                    int[] expected = oldAnimated(count, previous, open, -20,
                                            client, header, tick, fromBottom);
                                    eq(expected, PShelfViewportGeometry.animated(count, previous,
                                            open, -20, client, header, tick, fromBottom), "frame");
                                }
                            }
                        }
                    }
                }
            }
        }
        Random random = new Random(0x4d335348454c46L);
        for (int trial = 0; trial < 2000; trial++) {
            int count = random.nextInt(1025);
            int previous = random.nextInt(count + 1) - 1;
            int next = random.nextInt(count + 1) - 1;
            int y = random.nextInt();
            int height = random.nextInt();
            int itemHeight = random.nextInt();
            double p = random.nextDouble();
            eq(oldSteady(count, next, y, height, itemHeight),
                    PShelfViewportGeometry.steady(count, next, y, height, itemHeight),
                    "random steady");
            eq(oldAnimated(count, previous, next, y, height, itemHeight, p, true),
                    PShelfViewportGeometry.animated(count, previous, next,
                            y, height, itemHeight, p, true), "random frame");
        }
        System.out.println("PSHELF_INTRINSIC_GEOMETRY_PASS checks=" + assertions);
    }
}
