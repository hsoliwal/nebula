// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.roundedcheckbox;

import java.util.Arrays;
import java.util.Random;

/** Test-only JNI twin for the original Canvas paint primitive coordinates. */
public final class RoundedCheckboxGeometryJniProof {
    private static native int[] coordinates(int width, int height);
    private static long checks;
    static { System.loadLibrary("m3_rounded_checkbox_coordinates"); }

    private static int[] javaCoordinates(int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        return new int[] {
                width - 4, height - 4, width - 9, height - 9,
                centerX, centerY, centerY + 4, centerX + 5, centerY - 3
        };
    }

    private static void compare(int width, int height) {
        int[] java = javaCoordinates(width, height);
        int[] nativeValue = coordinates(width, height);
        checks++;
        if (!Arrays.equals(java, nativeValue)) {
            throw new AssertionError("geometry mismatch at " + checks
                    + " size=" + width + "," + height
                    + " Java=" + Arrays.toString(java)
                    + " JNI=" + Arrays.toString(nativeValue));
        }
    }

    public static void main(String[] args) {
        int[] edges = {0, 1, -1, 4, 5, 9, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE - 3, Integer.MIN_VALUE + 3};
        for (int w : edges) for (int h : edges) compare(w, h);
        Random random = new Random(0x4d334e4154495645L);
        for (int i = 0; i < 120000; i++) compare(random.nextInt(), random.nextInt());
        System.out.println("ROUNDED_CHECKBOX_JNI_COORDINATES_PASS checks=" + checks);
    }
}
