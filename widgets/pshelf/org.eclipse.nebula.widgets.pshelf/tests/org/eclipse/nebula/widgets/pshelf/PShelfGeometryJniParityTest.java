/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.pshelf;

import java.util.Arrays;
import java.util.Random;

/** JNI C17 proof of Java 21's int overflow and double narrowing, not a production provider. */
public final class PShelfGeometryJniParityTest {
    private static long checks;
    private static native int[] steady(int count, int open, int y, int h, int header);
    private static native int[] animated(int count, int previous, int next, int y,
            int h, int header, double progress, boolean fromBottom);
    static { System.loadLibrary("m3_pshelf_geometry_test"); }

    private static void equal(int[] expected, int[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("PShelf Java/JNI mismatch at " + checks
                    + " java=" + Arrays.toString(expected)
                    + " native=" + Arrays.toString(actual));
        }
    }
    private static void compare(int n, int prev, int next, int y, int h,
            int header, double progress, boolean bottom) {
        equal(PShelfViewportGeometry.steady(n, next, y, h, header),
                steady(n, next, y, h, header));
        equal(PShelfViewportGeometry.animated(n, prev, next, y, h, header,
                progress, bottom), animated(n, prev, next, y, h, header, progress, bottom));
    }
    private static void reject(Runnable action) {
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid native geometry was accepted");
    }
    public static void main(String[] args) {
        int[] extrema = {0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x55555555};
        double[] progress = {0, 1, -1, .5, .02, .98,
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int n = 0; n <= 32; n++) {
            for (int prev = -1; prev < n; prev++) {
                for (int next = -1; next < n; next++) {
                    int seed = n + prev * 31 + next * 997;
                    int y = extrema[Math.floorMod(seed, extrema.length)];
                    int h = extrema[Math.floorMod(seed * 3 + 1, extrema.length)];
                    int header = extrema[Math.floorMod(seed * 7 + 2, extrema.length)];
                    for (double p : progress) {
                        compare(n, prev, next, y, h, header, p, false);
                        compare(n, prev, next, y, h, header, p, true);
                    }
                }
            }
        }
        Random random = new Random(0x4d334e4154495645L);
        for (int i = 0; i < 3000; i++) {
            int n = random.nextInt(513);
            int prev = random.nextInt(n + 1) - 1;
            int next = random.nextInt(n + 1) - 1;
            int y = random.nextInt();
            int h = random.nextInt();
            int header = random.nextInt();
            double p = i % 23 == 0 ? Double.NaN : random.nextDouble() * 4 - 2;
            compare(n, prev, next, y, h, header, p, random.nextBoolean());
        }
        compare(100_000, -1, 0, Integer.MAX_VALUE, -1, Integer.MIN_VALUE, .25, true);
        reject(() -> steady(-1, 0, 0, 10, 5));
        reject(() -> animated(-1, 0, 0, 0, 10, 5, .5, true));
        reject(() -> steady(1_048_577, 0, 0, 10, 5));
        reject(() -> animated(1_048_577, 0, 0, 0, 10, 5, .5, true));
        reject(() -> steady(3, 3, 0, 10, 5));
        reject(() -> animated(3, -2, 0, 0, 10, 5, .5, true));
        reject(() -> animated(3, 0, 3, 0, 10, 5, .5, false));
        System.out.println("PSHELF_JNI_PARITY_PASS checks=" + checks);
    }
}
