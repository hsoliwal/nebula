/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 * This program and the accompanying materials are made available under the
 * Apache License 2.0 License or the Eclipse Public License 2.0.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Contract for the additive, caller-owned Grid index-window API. It is
 * intentionally a compile-time failing receiver oracle until canonical
 * Synexia OpenRewrite generates Grid.getViewportItemRange(int[],int).
 */
public class GridViewportItemRange_Test {
    private Display display;
    private Shell shell;

    @Before
    public void setUp() {
        display = Display.getDefault();
        shell = new Shell(display);
        shell.setBounds(20, 20, 400, 340);
    }

    @After
    public void tearDown() {
        if (shell != null && !shell.isDisposed()) shell.dispose();
    }

    private Grid grid(final int count, final boolean virtual) {
        final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.BORDER | (virtual ? SWT.VIRTUAL : SWT.NONE));
        grid.setBounds(5, 5, 340, 260);
        new GridColumn(grid, SWT.NONE).setWidth(200);
        grid.setItemCount(count);
        return grid;
    }

    private void settle() {
        while (display.readAndDispatch()) {
            // Drain the actual SWT paint/layout events.
        }
    }

    @Test
    public void emptyAndZeroHeightWriteTheCanonicalEmptyInterval() {
        final Grid empty = grid(0, true);
        final int[] out = {77, 88, 99, 111};
        empty.getViewportItemRange(out, 1);
        assertArrayEquals(new int[] {77, 0, 0, 111}, out);

        final Grid zeroHeight = grid(80, true);
        zeroHeight.setBounds(0, 0, 300, 0);
        zeroHeight.getViewportItemRange(out, 1);
        assertArrayEquals(new int[] {77, 0, 0, 111}, out);
    }

    @Test
    public void rangeMatchesTheExistingVisibleRangeWithoutCopyingItsItems() {
        final Grid grid = grid(240, false);
        shell.open();
        settle();
        final int[] range = { -19, -19, -19, -19 };
        grid.getViewportItemRange(range, 1);
        assertEquals(-19, range[0]);
        assertEquals(-19, range[3]);
        assertEquals(grid.getTopIndex(), range[1]);
        assertTrue(range[2] > range[1]);
        assertTrue(range[2] <= grid.getItemCount());
        assertEquals(grid.getVisibleRange().getItems().length, range[2] - range[1]);

        grid.setTopIndex(120);
        settle();
        grid.getViewportItemRange(range, 1);
        assertEquals(grid.getTopIndex(), range[1]);
        assertEquals(grid.getVisibleRange().getItems().length, range[2] - range[1]);
    }

    @Test
    public void distantVirtualModelAvoidsEnumeratingAMillionItems() {
        final Grid grid = grid(1_000_000, true);
        shell.open();
        settle();
        final int[] range = new int[2];
        grid.setTopIndex(990_000);
        final int residentBefore = grid.virtualMaterializedItemCount();
        grid.getViewportItemRange(range, 0);
        final int residentAfter = grid.virtualMaterializedItemCount();
        assertTrue("interval must be bounded", range[1] >= range[0] && range[1] - range[0] < 1024);
        assertTrue("logical indices must be in range", range[1] <= grid.getItemCount());
        assertEquals(grid.getTopIndex(), range[0]);
        assertTrue("the query must not enumerate cold item facades", residentAfter - residentBefore <= 64);
    }

    @Test
    public void invalidOutputNeverReceivesPartialWrites() {
        final Grid grid = grid(15, true);
        final int[] out = {17, 18, 19};
        try {
            grid.getViewportItemRange(out, 2);
            fail("offset with fewer than two slots must be rejected");
        } catch (IllegalArgumentException expected) {
            // SWT.ERROR_INVALID_ARGUMENT
        }
        assertArrayEquals(new int[] {17, 18, 19}, out);

        try {
            grid.getViewportItemRange(null, 0);
            fail("null output must be rejected");
        } catch (IllegalArgumentException expected) {
            // SWT.ERROR_NULL_ARGUMENT
        }
    }
}
