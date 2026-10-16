// SPDX-License-Identifier: EPL-2.0
// Out-of-tree probe for docs/m3/GRID_MEDIATED_VIEWPORT.md: column window on vs off around column groups.
package org.eclipse.nebula.widgets.grid;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

/** Prints the horizontal bar state around the two prints of GridColumnWindow_Test's group case. */
public class ColumnWindowProbe {
	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		shell.setSize(700, 500);
		shell.open();
		Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 30, 660, 420);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		grid.setLinesVisible(true);
		Random random = new Random(3L);
		GridColumnGroup group = null;
		for (int c = 0; c < 60; c++) {
			if (c % 7 == 0) { group = new GridColumnGroup(grid, SWT.NONE); group.setText("G" + c); }
			GridColumn column = new GridColumn(group, SWT.NONE);
			column.setText("Column " + c);
			column.setWidth(20 + random.nextInt(121));
			if (c % 11 == 5) column.setVisible(false);
		}
		for (int r = 0; r < 300; r++) {
			GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 60; c++) item.setText(c, "r" + r + "/" + c + (c % 9 == 0 ? " a longer cell text" : ""));
		}
		grid.setSelection(new int[] { 2, 5, 9 });
		while (display.readAndDispatch()) { }
		grid.redraw(); grid.update();
		ScrollBar bar = grid.getHorizontalBar();
		for (int p = 0; p <= 25; p++) {
			bar.setSelection(p);
			bar.notifyListeners(SWT.Selection, new Event());
			grid.update();
			while (display.readAndDispatch()) { }
			String before = bar.getSelection() + "/" + bar.getMaximum() + " start " + grid.getStartColumnIndex() + " client " + grid.getClientArea();
			grid.columnWindowEnabled = false;
			String h1 = hpx(grid);
			long a = print(grid);
			String h2 = hpx(grid);
			String mid = bar.getSelection() + "/" + bar.getMaximum() + " start " + grid.getStartColumnIndex() + " client " + grid.getClientArea();
			grid.columnWindowEnabled = true;
			long b = print(grid);
			String h3 = hpx(grid);
			System.out.println("p=" + p + " hpx " + h1 + "," + h2 + "," + h3 + " same=" + (a == b));
			String after = bar.getSelection() + "/" + bar.getMaximum() + " start " + grid.getStartColumnIndex() + " client " + grid.getClientArea();
			if (p < 0) System.out.println("p=" + p + " before " + before + " | after full " + mid + " | after window " + after + " headerHeight " + grid.getHeaderHeight() + " itemHeight " + grid.getItemHeight());
		}
		display.dispose();
	}

	static long print(Grid grid) {
		Image image = new Image(grid.getDisplay(), grid.getSize().x, grid.getSize().y);
		GC gc = new GC(image);
		grid.print(gc);
		gc.dispose();
		long h = java.util.Arrays.hashCode(image.getImageData().data);
		image.dispose();
		return h;
	}

	static String hpx(Grid grid) {
		try {
			java.lang.reflect.Method m = Grid.class.getDeclaredMethod("getHScrollSelectionInPixels");
			m.setAccessible(true);
			return String.valueOf(m.invoke(grid));
		} catch (Exception e) {
			return e.toString();
		}
	}
}
