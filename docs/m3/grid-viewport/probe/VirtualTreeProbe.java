// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Out-of-tree measurement probe for docs/m3/GRID_MEDIATED_VIEWPORT.md (compile against the Grid classes and SWT).
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * An SWT.VIRTUAL tree Grid of roots x 99 children (the existing API builds every item; the data
 * comes from SetData): heap after the build, SetData requests after the first paint, after 40
 * scroll steps and after expanding every tenth root, and the time of each phase. Args: roots.
 */
public class VirtualTreeProbe {
	public static void main(String[] args) {
		int roots = Integer.parseInt(args[0]);
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setBounds(10, 10, 400, 450);
		Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		grid.setBounds(5, 5, 320, 380);
		grid.setHeaderVisible(true);
		GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setTree(true);
		column.setWidth(200);
		AtomicInteger requests = new AtomicInteger();
		grid.addListener(SWT.SetData, e -> {
			requests.incrementAndGet();
			((GridItem) e.item).setText("row " + e.index);
		});
		long heap0 = used();
		long t0 = System.nanoTime();
		GridItem[] rootItems = new GridItem[roots];
		for (int r = 0; r < roots; r++) {
			rootItems[r] = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 99; c++) {
				new GridItem(rootItems[r], SWT.NONE);
			}
		}
		long t1 = System.nanoTime();
		long heap1 = used();
		shell.open();
		while (display.readAndDispatch()) { }
		int afterPaint = requests.get();
		long t2 = System.nanoTime();
		for (int i = 0; i < 40; i++) {
			grid.setTopIndex((roots / 2 + i) * 100);
			grid.redraw();
			grid.update();
		}
		int afterScroll = requests.get();
		long t3 = System.nanoTime();
		for (int r = 0; r < roots; r += 10) {
			rootItems[r].setExpanded(true);
		}
		grid.setTopIndex(roots * 50);
		grid.redraw();
		grid.update();
		long t4 = System.nanoTime();
		System.out.println("mode " + System.getProperty("swt.widget.mode.Grid") + ": " + roots * 100 + " items; build "
				+ (t1 - t0) / 1_000_000 + " ms, heap +" + (heap1 - heap0) / (1 << 20) + " MB (" + (heap1 - heap0) / (roots * 100L)
				+ " B/item); SetData after paint " + afterPaint + ", after 40 scroll steps " + afterScroll + " (" + (t3 - t2) / 40_000
				+ " us/step), after expanding " + roots / 10 + " roots " + requests.get() + " (" + (t4 - t3) / 1_000_000 + " ms)");
		shell.dispose();
	}

	static long used() {
		Runtime rt = Runtime.getRuntime();
		for (int i = 0; i < 3; i++) {
			System.gc();
		}
		return rt.totalMemory() - rt.freeMemory();
	}
}
