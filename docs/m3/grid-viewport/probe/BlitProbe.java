// SPDX-License-Identifier: EPL-2.0
// Out-of-tree probe used in the regression triage (docs/m3/grid-viewport/REGRESSION_TRIAGE.tsv).
package org.eclipse.nebula.widgets.grid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/** GridBlitScroll_Test with image dumps: blitted screen vs full repaint after each step. */
public class BlitProbe {
	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
		Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 520, 360);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 6; c++) new GridColumn(grid, SWT.NONE).setWidth(90);
		grid.addListener(SWT.SetData, e -> {
			GridItem item = (GridItem) e.item;
			for (int c = 0; c < 6; c++) item.setText(c, "r" + e.index + "c" + c);
		});
		grid.setItemCount(5000);
		grid.select(new int[] { 3, 9, 40 });
		shell.setSize(560, 420);
		shell.open();
		shell.forceActive();
		settle(display);
		ScrollBar bar = grid.getVerticalBar();
		for (int step : new int[] { 1, 2, 5, 3, 17, 1, 9, 30, 2 }) {
			bar.setSelection(bar.getSelection() + step);
			bar.notifyListeners(SWT.Selection, new Event());
			grid.update();
			settle(display);
			ImageData blitted = screen(grid);
			grid.redraw();
			grid.update();
			settle(display);
			ImageData full = screen(grid);
			boolean same = java.util.Arrays.equals(blitted.data, full.data);
			System.out.println("step " + step + " top " + grid.getTopIndex() + " same " + same + " blits " + grid.blitScrollCount);
			if (!same) {
				save(blitted, args[0] + "/blit-" + step + "-blitted.png");
				save(full, args[0] + "/blit-" + step + "-full.png");
			}
		}
		display.dispose();
	}

	static ImageData screen(Grid grid) {
		Image image = new Image(grid.getDisplay(), grid.getSize().x, grid.getSize().y);
		GC gc = new GC(grid);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		ImageData d = image.getImageData();
		image.dispose();
		return d;
	}

	static void save(ImageData d, String f) {
		ImageLoader l = new ImageLoader();
		l.data = new ImageData[] { d };
		l.save(f, SWT.IMAGE_PNG);
	}

	static void settle(Display display) {
		for (int i = 0; i < 5; i++) while (display.readAndDispatch()) { }
	}
}
