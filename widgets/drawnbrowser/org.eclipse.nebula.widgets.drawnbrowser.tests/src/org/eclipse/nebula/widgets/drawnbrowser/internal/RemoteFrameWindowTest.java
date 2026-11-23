/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.*;
import java.util.Random;
import org.junit.Test;

/** Source-only Java 21 tests; no SWT Display or browser credentials. */
public class RemoteFrameWindowTest {
    @Test public void onlyMonotonicFramesCanEnter() {
        RemoteFrameWindow w = new RemoteFrameWindow();
        assertTrue(w.receive(4, 0, 700, 361, 350, 181));
        assertFalse(w.receive(4, 0, 700, 361, 350, 181));
        assertFalse(w.receive(3, 99, 700, 361, 350, 181));
        assertTrue(w.receive(4, 1, 700, 361, 350, 181));
        assertTrue(w.receive(5, 0, 700, 361, 350, 181));
        w.retire();
        assertFalse(w.receive(5, 3, 700, 361, 350, 181));
        assertTrue(w.receive(6, 0, 700, 361, 350, 181));
    }

    @Test public void paintConfirmationGuardsInputAndLetterbox() {
        RemoteFrameWindow w = new RemoteFrameWindow();
        w.receive(10, 1, 400, 200, 800, 400);
        assertNull(w.cssPoint(10, 1, 100, 100));
        assertFalse(w.markPainted(10, 0, 1000, 1000, 0, 0, 1000, 1000));
        RemoteFrameWindow.Rect b = w.content(1000, 1000);
        assertEquals(new RemoteFrameWindow.Rect(0, 250, 1000, 500), b);
        assertFalse(w.markPainted(10, 1, 1000, 1000, 0, 250, 500, 500));
        assertNull(w.cssPoint(10, 1, 500, 500));
        assertTrue(w.markPainted(10, 1, 1000, 1000, 0, 250, 1000, 500));
        assertArrayEquals(new int[] {400, 200}, w.cssPoint(10, 1, 500, 500));
        assertNull(w.cssPoint(10, 1, 500, 249));
        assertNull(w.cssPoint(10, 1, 500, 750));
        assertNull(w.cssPoint(10, 2, 500, 500));
        assertFalse(w.markPainted(10, 1, 1000, 1000, 0, 250, 1000, 500));
        w.invalidatePaint();
        assertNull(w.cssPoint(10, 1, 500, 500));
        assertTrue(w.markPainted(10, 1, 1000, 1000, 0, 250, 1000, 500));
        assertArrayEquals(new int[] {400, 200}, w.cssPoint(10, 1, 500, 500));
    }

    @Test public void staleFrameCanNeverReauthorizeCoordinates() {
        RemoteFrameWindow w = new RemoteFrameWindow();
        w.receive(0, 7, 900, 600, 900, 600);
        w.markPainted(0, 7, 450, 300, 0, 0, 450, 300);
        assertNotNull(w.cssPoint(0, 7, 10, 10));
        w.receive(0, 8, 900, 600, 900, 600);
        assertNull(w.cssPoint(0, 7, 10, 10));
        assertNull(w.cssPoint(0, 8, 10, 10));
        assertFalse(w.markPainted(0, 7, 450, 300, 0, 0, 450, 300));
        w.markPainted(0, 8, 450, 300, 0, 0, 450, 300);
        assertNotNull(w.cssPoint(0, 8, 10, 10));
        w.retire();
        assertNull(w.cssPoint(0, 8, 10, 10));
    }

    @Test public void invalidDimensionsRefusedWithoutMutation() {
        RemoteFrameWindow w = new RemoteFrameWindow();
        w.receive(2, 7, 800, 400, 800, 400);
        assertThrows(IllegalArgumentException.class,
                () -> w.receive(3, 0, 8192, 8192, 800, 400));
        assertThrows(IllegalArgumentException.class,
                () -> w.receive(3, 0, 800, 400, 32769, 400));
        assertThrows(IllegalArgumentException.class,
                () -> w.receive(-1, 0, 800, 400, 800, 400));
        assertTrue(w.markPainted(2, 7, 400, 200, 0, 0, 400, 200));
        assertNotNull(w.cssPoint(2, 7, 0, 0));
    }

    @Test public void randomizedAspectAndPointerProjection() {
        Random r = new Random(0x71d3_2026);
        RemoteFrameWindow w = new RemoteFrameWindow();
        for (int i = 0; i < 20_000; i++) {
            int srcW = 1 + r.nextInt(4096), srcH = 1 + r.nextInt(4096);
            int viewW = 1 + r.nextInt(4000), viewH = 1 + r.nextInt(4000);
            int cssW = 1 + r.nextInt(3000), cssH = 1 + r.nextInt(3000);
            if ((long) srcW * srcH > RemoteFrameWindow.MAX_PIXELS) continue;
            assertTrue(w.receive(101, i, srcW, srcH, cssW, cssH));
            assertTrue(w.markPainted(101, i, viewW, viewH, 0, 0, viewW, viewH));
            RemoteFrameWindow.Rect b = w.content(viewW, viewH);
            assertTrue(b.x() >= 0 && b.y() >= 0 && b.width() >= 1 && b.height() >= 1);
            assertTrue((long) b.x() + b.width() <= viewW);
            assertTrue((long) b.y() + b.height() <= viewH);
            int x = b.x() + r.nextInt(b.width());
            int y = b.y() + r.nextInt(b.height());
            int[] projected = w.cssPoint(101, i, x, y);
            assertNotNull(projected);
            int px = (int) ((long) (x - b.x()) * srcW / b.width());
            int py = (int) ((long) (y - b.y()) * srcH / b.height());
            assertEquals((int) ((long) px * cssW / srcW), projected[0]);
            assertEquals((int) ((long) py * cssH / srcH), projected[1]);
            assertTrue(projected[0] >= 0 && projected[0] < cssW);
            assertTrue(projected[1] >= 0 && projected[1] < cssH);
        }
    }
}
