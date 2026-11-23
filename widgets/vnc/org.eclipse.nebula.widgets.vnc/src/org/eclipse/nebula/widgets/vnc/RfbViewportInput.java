/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.vnc;

/**
 * Display-free native VNC viewport input coordinates and captured-button state.
 * The SWT viewer owns this object; its methods are called only on the SWT UI thread.
 * An absent coordinate means outside the painted letterboxed image (or stale
 * geometry), never a request to clamp a click onto the remote desktop's edge.
 */
final class RfbViewportInput {
    private int buttons;
    private int lastX;
    private int lastY;

    /**
     * Maps a view-coordinate to a remote pixel only inside the painted frame.
     * Checks use long arithmetic to avoid overflow at the signed-int boundary.
     */
    static int[] map(int x, int y, int left, int top, int width, int height,
            int remoteWidth, int remoteHeight) {
        if (width <= 0 || height <= 0 || remoteWidth <= 0 || remoteHeight <= 0
                || (long) x < left || (long) y < top
                || (long) x >= (long) left + width || (long) y >= (long) top + height) {
            return null;
        }
        return new int[] {
                (int) (((long) x - left) * remoteWidth / width),
                (int) (((long) y - top) * remoteHeight / height)
        };
    }

    /** A press outside the remote content is not captured or transmitted. */
    int[] press(int bit, int[] mapped) {
        if (bit == 0 || mapped == null) {
            return null;
        }
        buttons |= bit;
        remember(mapped);
        return command(lastX, lastY);
    }

    /** A move in a gutter is dropped; visible moves may preserve active drags. */
    int[] move(int[] mapped) {
        if (mapped == null) {
            return null;
        }
        if (buttons != 0) {
            remember(mapped);
        }
        return command(mapped[0], mapped[1]);
    }

    /**
     * Release captured buttons at the last valid remote coordinate when the
     * pointer exits the image; remote buttons must not remain pressed forever.
     */
    int[] release(int bit, int[] mapped) {
        if (bit == 0 || (buttons & bit) == 0) {
            return null;
        }
        buttons &= ~bit;
        if (mapped != null) {
            remember(mapped);
        }
        return command(lastX, lastY);
    }

    int buttons() {
        return buttons;
    }

    void reset() {
        buttons = lastX = lastY = 0;
    }

    private void remember(int[] mapped) {
        lastX = mapped[0];
        lastY = mapped[1];
    }

    private int[] command(int x, int y) {
        return new int[] { x, y, buttons };
    }
}
