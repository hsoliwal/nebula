// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Out-of-tree measurement probe for docs/m3/GRID_MEDIATED_VIEWPORT.md (compile against the Grid classes and SWT).
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.nebula.widgets.grid.GridItemDataVisualizer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Times row selection over a flat Grid of n items (wall time, median of repeats, so other
 * processes on a loaded machine weigh less): select all, count, paint, deselect and select one,
 * a scroll step with everything selected, the selection indices. Args: n.
 */
public class SelectProbe {
	static final ThreadMXBean CPU = ManagementFactory.getThreadMXBean();

	interface Step {
		void run();
	}

	static long median(int repeats, Runnable before, Step step) {
		long[] times = new long[repeats];
		for (int i = 0; i < repeats; i++) {
			before.run();
			long t0 = System.nanoTime();
			step.run();
			times[i] = System.nanoTime() - t0;
		}
		Arrays.sort(times);
		return times[repeats / 2] / 1000;
	}

	public static void main(String[] args) {
		int n = Integer.parseInt(args[0]);
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setBounds(10, 10, 400, 450);
		Grid grid = new Grid(new GridItemDataVisualizer(display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_BLACK), null), shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 320, 380);
		GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setWidth(200);
		for (int r = 0; r < n; r++) {
			new GridItem(grid, SWT.NONE).setText("r" + r);
		}
		shell.open();
		while (display.readAndDispatch()) { }
		System.gc();
		long selectAll = median(9, grid::deselectAll, grid::selectAll);
		long count = median(5, () -> { }, grid::getSelectionCount);
		long paint = median(9, grid::redraw, grid::update);
		long toggle = median(9, () -> { }, () -> {
			grid.deselect(n / 2);
			grid.select(n / 2);
		});
		int[] step = { 0 };
		long scroll = median(21, () -> grid.setTopIndex(n / 2 + 7 * step[0]++), () -> {
			grid.redraw();
			grid.update();
		});
		long indices = median(9, () -> { }, grid::getSelectionIndices);
		System.out.println("mode " + System.getProperty("swt.widget.mode.Grid") + ": " + n + " rows, selected "
				+ grid.getSelectionCount() + " (us, median of repeats): selectAll " + selectAll + ", count " + count + ", paint " + paint
				+ ", deselect+select one " + toggle + ", scroll paint " + scroll + ", indices " + indices);
		shell.dispose();
	}
}
