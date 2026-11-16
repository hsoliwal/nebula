/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.grid;

/** Headless test stub: not included in the widget or the release. */
final class GridItem {
    private int slot = -1;
    private int row = -1;
    private final int height;
    private final boolean visible;

    GridItem(final int height, final boolean visible) {
        this.height = height;
        this.visible = visible;
    }
    int getHeight() { return height; }
    boolean isVisible() { return visible; }
    int getRowIndex() { return row; }
    int rowTreeSlot() { return slot; }
    void enteredRowTree(final int nextSlot) { slot = nextSlot; }
    void leftRowTree(final int index) { slot = -1; row = index; }
}
