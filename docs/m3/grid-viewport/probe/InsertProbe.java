// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Out-of-tree measurement probe for docs/m3/GRID_MEDIATED_VIEWPORT.md (compile against the Grid classes and SWT).
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.nebula.widgets.grid.GridItemDataVisualizer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Builds roots x 99 children, then times structural changes in the middle: a child inserted, the first
 * row question after it (setTopIndex + getTopIndex), a child disposed, the first question after it, and
 * a paint. Args: roots.
 */
public class InsertProbe {
	public static void main(String[] args) {
		int roots = Integer.parseInt(args[0]);
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setBounds(10, 10, 400, 450);
		Grid grid = new Grid(new GridItemDataVisualizer(display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_BLACK), null), shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 320, 380);
		grid.setHeaderVisible(true);
		GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setTree(true);
		column.setWidth(200);
		for (int r = 0; r < roots; r++) {
			GridItem root = new GridItem(grid, SWT.NONE);
			root.setText("r" + r);
			for (int c = 0; c < 99; c++) {
				new GridItem(root, SWT.NONE).setText("r" + r + "/" + c);
			}
		}
		shell.open();
		while (display.readAndDispatch()) { }
		grid.setTopIndex((roots / 2) * 100);
		grid.getTopIndex();
		System.gc();
		int steps = 20;
		long insert = 0, afterInsert = 0, remove = 0, afterRemove = 0, paint = 0;
		for (int i = 0; i < steps; i++) {
			GridItem root = grid.getItem((roots / 2 + i) * 100);
			long t0 = System.nanoTime();
			GridItem added = new GridItem(root, SWT.NONE, 0);
			long t1 = System.nanoTime();
			grid.setTopIndex((roots / 2 + i) * 100);
			grid.getTopIndex();
			long t2 = System.nanoTime();
			added.dispose();
			long t3 = System.nanoTime();
			grid.setTopIndex((roots / 2 + i) * 100);
			grid.getTopIndex();
			long t4 = System.nanoTime();
			grid.redraw();
			grid.update();
			long t5 = System.nanoTime();
			insert += t1 - t0;
			afterInsert += t2 - t1;
			remove += t3 - t2;
			afterRemove += t4 - t3;
			paint += t5 - t4;
		}
		System.out.println("mode " + System.getProperty("swt.widget.mode.Grid") + ": " + roots * 100 + " items, per step (us): insert "
				+ insert / 1000 / steps + ", first question after it " + afterInsert / 1000 / steps + ", dispose " + remove / 1000 / steps
				+ ", first question after it " + afterRemove / 1000 / steps + ", paint " + paint / 1000 / steps);
		shell.dispose();
	}
}
