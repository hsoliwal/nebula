// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Out-of-tree measurement probe for docs/m3/GRID_MEDIATED_VIEWPORT.md (compile against the Grid classes and SWT).
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.nebula.widgets.grid.GridItemDataVisualizer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Times cell selection over a flat Grid of n rows x 4 columns: select all cells, paint, scroll paint, read the selection. Args: n. */
public class CellSelectProbe {
	public static void main(String[] args) {
		int n = Integer.parseInt(args[0]);
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setBounds(10, 10, 400, 450);
		Grid grid = new Grid(new GridItemDataVisualizer(display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_BLACK), null), shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 320, 380);
		grid.setCellSelectionEnabled(true);
		for (int c = 0; c < 4; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(70);
		}
		for (int r = 0; r < n; r++) {
			new GridItem(grid, SWT.NONE).setText("r" + r);
		}
		shell.open();
		while (display.readAndDispatch()) { }
		System.gc();
		long t0 = System.nanoTime();
		grid.selectAllCells();
		long t1 = System.nanoTime();
		grid.redraw();
		grid.update();
		long t2 = System.nanoTime();
		long scroll = 0;
		for (int i = 0; i < 10; i++) {
			grid.setTopIndex(n / 2 + 7 * i);
			long s0 = System.nanoTime();
			grid.redraw();
			grid.update();
			scroll += System.nanoTime() - s0;
		}
		long t3 = System.nanoTime();
		int cells = grid.getCellSelection().length;
		long t4 = System.nanoTime();
		System.out.println("mode " + System.getProperty("swt.widget.mode.Grid") + ": " + n + " rows x 4, " + cells + " cells (us): selectAllCells "
				+ (t1 - t0) / 1000 + ", paint " + (t2 - t1) / 1000 + ", scroll paint " + scroll / 10 / 1000 + ", getCellSelection " + (t4 - t3) / 1000);
		shell.dispose();
	}
}
