/*******************************************************************************
 * Copyright (c) 2008, 2026 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.snippets.grid.viewer;

import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.jface.gridviewer.GridTableViewer;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * One-million-row GridTableViewer with a genuinely lazy model and genuinely
 * sparse SWT.VIRTUAL Grid facades.
 *
 * <p>The logical model is only an integer row count. No million-element array
 * is allocated. JFace asks for individual rows through
 * {@link ILazyContentProvider#updateElement(int)}, and the Grid manufactures a
 * stable GridItem facade only for coordinates that are actually touched.</p>
 */
public class GridVirtualTableViewer {

    private static final int ROWS = 1_000_000;

    private static final class Row {
        private final int index;

        Row(final int index) {
            this.index = index;
        }

        @Override
        public String toString() {
            return "Item " + index;
        }
    }

    private static final class LazyRows implements ILazyContentProvider {
        private final GridTableViewer viewer;
        private int rowCount;

        LazyRows(final GridTableViewer viewer) {
            this.viewer = viewer;
        }

        @Override
        public void dispose() {
            // No retained model objects to release.
        }

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
            rowCount = ((Integer) newInput).intValue();
        }

        @Override
        public void updateElement(final int index) {
            if (index < 0 || index >= rowCount) {
                return;
            }
            viewer.replace(new Row(index), index);
        }
    }

    public GridVirtualTableViewer(final Shell shell) {
        final GridTableViewer viewer = new GridTableViewer(shell, SWT.VIRTUAL);
        final Grid grid = viewer.getGrid();

        viewer.setLabelProvider(new LabelProvider());
        viewer.setContentProvider(new LazyRows(viewer));
        viewer.setUseHashlookup(true);

        final GridColumn column = new GridColumn(grid, SWT.NONE);
        column.setWidth(160);
        column.setText("Logical row");

        viewer.setInput(Integer.valueOf(ROWS));
        viewer.setItemCount(ROWS);

        grid.setHeaderVisible(true);
        grid.setLinesVisible(true);
    }

    public static void main(final String[] args) {
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("Nebula Grid - 1,000,000 lazy rows");
        shell.setLayout(new FillLayout());

        new GridVirtualTableViewer(shell);

        shell.setSize(640, 480);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
