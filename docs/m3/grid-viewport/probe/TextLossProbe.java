// SPDX-License-Identifier: EPL-2.0
// Out-of-tree probe used in the regression triage (docs/m3/grid-viewport/REGRESSION_TRIAGE.tsv).
package org.eclipse.nebula.widgets.grid;

import java.util.Random;

import org.eclipse.swt.widgets.*;

/** Replays a differential seed against one Grid and reports the first step after which an item lost its text. */
public class TextLossProbe {
	public static void main(String[] args) throws Exception {
		int seed = Integer.parseInt(args[0]);
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setBounds(10, 10, 330, 400);
		GridDifferentialDriver driver = new GridDifferentialDriver(shell, 5, seed % 4);
		java.lang.reflect.Field f = GridDifferentialDriver.class.getDeclaredField("grid");
		f.setAccessible(true);
		Grid grid = (Grid) f.get(driver);
		shell.open();
		Random random = new Random(0x6E1DL + seed);
		for (int step = 0; step < 160; step++) {
			int[] op = { random.nextInt(GridDifferentialDriver.KINDS), random.nextInt(1 << 20), random.nextInt(1 << 20), random.nextInt(1 << 20) };
			Object answer = driver.apply(op);
			while (display.readAndDispatch()) { }
			GridItem[] items = grid.getItems();
			for (int i = 0; i < items.length; i++) {
				if (items[i].getText(0).isEmpty()) {
					System.out.println("step " + step + " op " + java.util.Arrays.toString(op) + " -> " + answer + ": item " + i + " (row field " + items[i].knownRow() + ", level " + items[i].getLevel() + ") lost its text; count " + items.length);
					display.dispose();
					return;
				}
			}
		}
		System.out.println("no text lost");
		display.dispose();
	}
}
