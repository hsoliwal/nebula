/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Random;
import org.junit.Test;

/** Headless geometry/captured-button proof for VncViewer's original SWT event owner. */
public class RfbViewportInputTest {
    @Test
    public void letterboxRejectsGutterInput() {
        assertNull(RfbViewportInput.map(49, 50, 50, 0, 100, 100, 400, 400));
        assertNull(RfbViewportInput.map(150, 50, 50, 0, 100, 100, 400, 400));
        assertNull(RfbViewportInput.map(50, 100, 50, 0, 100, 100, 400, 400));
        assertArrayEquals(new int[] { 0, 0 },
                RfbViewportInput.map(50, 0, 50, 0, 100, 100, 400, 400));
        assertArrayEquals(new int[] { 396, 396 },
                RfbViewportInput.map(149, 99, 50, 0, 100, 100, 400, 400));
    }

    @Test
    public void outsidePressCannotStartRemoteDrag() {
        RfbViewportInput input = new RfbViewportInput();
        assertNull(input.press(1, null));
        assertEquals(0, input.buttons());
        assertArrayEquals(new int[] { 10, 20, 0 }, input.move(new int[] { 10, 20 }));
        assertNull(input.release(1, null));
    }

    @Test
    public void capturedDragAlwaysReleasesWhenLeavingPaintedContent() {
        RfbViewportInput input = new RfbViewportInput();
        assertArrayEquals(new int[] { 10, 20, 1 }, input.press(1, new int[] { 10, 20 }));
        assertArrayEquals(new int[] { 30, 40, 1 }, input.move(new int[] { 30, 40 }));
        assertNull(input.move(null));
        assertArrayEquals(new int[] { 30, 40, 0 }, input.release(1, null));
        assertEquals(0, input.buttons());
        assertNull(input.release(1, null));
    }

    @Test
    public void multipleButtonsPreserveRemainingMaskAndResetOnSessionChange() {
        RfbViewportInput input = new RfbViewportInput();
        assertArrayEquals(new int[] { 0, 0, 1 }, input.press(1, new int[] { 0, 0 }));
        assertArrayEquals(new int[] { 1, 2, 5 }, input.press(4, new int[] { 1, 2 }));
        assertArrayEquals(new int[] { 1, 2, 4 }, input.release(1, null));
        assertEquals(4, input.buttons());
        assertArrayEquals(new int[] { 10, 11, 0 }, input.release(4, new int[] { 10, 11 }));
        input.press(1, new int[] { 13, 17 });
        input.reset();
        assertNull(input.release(1, null));
        assertEquals(0, input.buttons());
    }

    @Test
    public void extremeCoordinatesDoNotWrapOrClampBlackBorders() {
        assertNull(RfbViewportInput.map(Integer.MAX_VALUE, 0,
                Integer.MAX_VALUE - 99, 0, 99, 99, 8192, 8192));
        assertArrayEquals(new int[] { 0, 0 }, RfbViewportInput.map(
                Integer.MAX_VALUE - 99, 0, Integer.MAX_VALUE - 99, 0, 99, 99, 8192, 8192));
        assertArrayEquals(new int[] { 8109, 8109 }, RfbViewportInput.map(
                Integer.MAX_VALUE - 1, 98, Integer.MAX_VALUE - 99, 0, 99, 99, 8192, 8192));
    }

    @Test
    public void randomizedMappedPointAlwaysWithinRemoteSurface() {
        Random random = new Random(0xC0FFEE);
        for (int i = 0; i < 100_000; i++) {
            int remoteWidth = 1 + random.nextInt(8192);
            int remoteHeight = 1 + random.nextInt(8192);
            int left = -300 + random.nextInt(600);
            int top = -300 + random.nextInt(600);
            int width = 1 + random.nextInt(1100);
            int height = 1 + random.nextInt(1100);
            int x = left - 1 + random.nextInt(width + 2);
            int y = top - 1 + random.nextInt(height + 2);
            int[] mapped = RfbViewportInput.map(x, y, left, top, width, height,
                    remoteWidth, remoteHeight);
            if (x < left || y < top || x >= left + width || y >= top + height) {
                assertNull(mapped);
            } else {
                assertEquals((int) ((long) (x - left) * remoteWidth / width), mapped[0]);
                assertEquals((int) ((long) (y - top) * remoteHeight / height), mapped[1]);
            }
        }
    }
}
