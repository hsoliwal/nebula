/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** A million cold Grid rows must not imply a million resident facade slots. */
public class GridVirtualItemListCapacity_Test {

    @Test
    public void fourParallelColumnsGrowAsOneSparseDirectory() throws Exception {
        final Display display = Display.getDefault();
        final Shell shell = new Shell(display);
        try {
            final Grid grid = new Grid(shell, SWT.VIRTUAL);
            new GridColumn(grid, SWT.NONE).setWidth(100);
            grid.addListener(SWT.SetData, event ->
                    ((GridItem) event.item).setText("row " + event.index));
            grid.setItemCount(1_000_000);

            final Field itemsField = Grid.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            final GridVirtualItemList list = (GridVirtualItemList) itemsField.get(grid);
            assertEquals(1_000_000, list.size());
            assertEquals(0, list.materializedCount());

            final GridItem first = grid.getItem(42);
            grid.getItem(7_000);
            grid.getItem(999_999);
            assertSame(first, grid.getItem(42));

            final int size = list.materializedCount();
            assertTrue(size >= 3);
            final int[] indices = (int[]) field(list, "indices");
            final GridItem[] values = (GridItem[]) field(list, "values");
            final int[] models = (int[]) field(list, "models");
            final int[] stamps = (int[]) field(list, "stamps");
            assertEquals(indices.length, values.length);
            assertEquals(indices.length, models.length);
            assertEquals(indices.length, stamps.length);
            assertTrue(indices.length >= size);
            assertTrue("physical capacity must follow materialized rows, not logical count",
                    indices.length < 128);
        } finally {
            shell.dispose();
        }
    }

    private static Object field(final GridVirtualItemList owner, final String name)
            throws ReflectiveOperationException {
        final Field field = GridVirtualItemList.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
