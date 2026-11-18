// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.pshelf;

/**
 * Primitive Y-coordinate plane owned by the original PShelf widget. There is
 * no second viewport Control and no retained boxed ordinate per PShelfItem.
 * Arithmetic deliberately retains original SWT int-sized layout semantics.
 */
final class PShelfViewportGeometry {
    private PShelfViewportGeometry() { }

    static int[] steady(int count, int openIndex, int clientY,
            int clientHeight, int itemHeight) {
        int[] positions = new int[count];
        steadyInto(positions, count, openIndex, clientY, clientHeight, itemHeight);
        return positions;
    }

    /** Rebuild the shelf-owned coordinate lane without allocating during a resize. */
    static void steadyInto(int[] positions, int count, int openIndex, int clientY,
            int clientHeight, int itemHeight) {
        if (positions.length != count) {
            throw new IllegalArgumentException("coordinate lane length");
        }
        int y = clientY;
        for (int i = 0; i < count; i++) {
            positions[i] = y;
            y += itemHeight;
            if (i == openIndex) {
                y = clientY + clientHeight - itemHeight * (count - (i + 1));
            }
        }
    }

    static int[] animated(int count, int previousIndex, int newIndex,
            int clientY, int clientHeight, int itemHeight,
            double progress, boolean openFromBottom) {
        int[] positions = new int[count];
        animatedInto(positions, count, previousIndex, newIndex,
                clientY, clientHeight, itemHeight, progress, openFromBottom);
        return positions;
    }

    /** Same original int arithmetic, written into the parent-owned frame lane. */
    static void animatedInto(int[] positions, int count, int previousIndex, int newIndex,
            int clientY, int clientHeight, int itemHeight,
            double progress, boolean openFromBottom) {
        if (positions.length != count) {
            throw new IllegalArgumentException("coordinate lane length");
        }
        int yTop = clientY;
        int yBottom = clientY + clientHeight
                - itemHeight * (count - (newIndex + 1));
        int area = clientHeight - itemHeight * count;
        int space = (int) (area * (openFromBottom ? 1 - progress : progress));
        boolean insertedSpace = false;
        for (int i = 0; i < count; i++) {
            if (i <= previousIndex) {
                positions[i] = yTop;
                yTop += itemHeight;
            } else if (i > newIndex) {
                positions[i] = yBottom;
                yBottom += itemHeight;
            } else {
                if (!insertedSpace) {
                    yTop += space;
                    insertedSpace = true;
                }
                positions[i] = yTop;
                yTop += itemHeight;
            }
        }
    }
}
