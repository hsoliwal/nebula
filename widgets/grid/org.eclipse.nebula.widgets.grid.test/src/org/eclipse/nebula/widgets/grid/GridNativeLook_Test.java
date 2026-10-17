/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Pixel differential "Grid vs native Table" and "Grid (tree rows) vs native Tree": the same rows,
 * column widths, selection and focus, captured with {@code Control.print} (run the test on the
 * hidden test desktop, so no cursor hovers a row). Each scene reports the number of differing pixels and their bounding box; with
 * {@code -Dgrid.nativelook.out=<dir>} both captures are saved as PNG on a mismatch.
 * <p>
 * A scene passes when its difference is within its documented residual (see
 * docs/m3/GRID_MEDIATED_VIEWPORT.md, "Native look"); {@code -Dgrid.nativelook.measure=true} only
 * reports.
 * </p>
 */
public class GridNativeLook_Test {

	private static final int[] WIDTHS = { 120, 90, 80 };
	private static final int ROWS = 12, WIDTH = 300, HEIGHT = 260;

	/**
	 * Documented residual per scene, in pixels (win32, 100 %, Windows 11 visual styles). The check
	 * box scene shows a vertical scroll bar: its thumb is one row longer in the Grid, whose bar
	 * counts one more row than the native list's (the Grid's bar values are part of its API).
	 */
	private static final Map<String, Integer> RESIDUAL = new LinkedHashMap<>();
	static {
		for (final String scene : new String[] { "rows", "selected-focused", "selected-unfocused", "focused-unselected",
				"disabled", "tree", "tree-selected-focused", "tree-collapsed-selected-unfocused" }) {
			RESIDUAL.put(scene, Integer.valueOf(0));
		}
		RESIDUAL.put("check", Integer.valueOf(40));
	}

	private Display display;
	private Shell shell;
	private Text focusHolder;
	private final StringBuilder report = new StringBuilder();
	private boolean failed;

	@Before
	public void setUp() {
		org.junit.Assume.assumeTrue("the native look is part of the viewport mode", GridViewport.ENABLED && GridNativeLook.ENABLED);
		display = Display.getDefault();
		shell = new Shell(display, SWT.ON_TOP | SWT.NO_TRIM);
		shell.setBounds(60, 60, 2 * WIDTH + 30, HEIGHT + 50);
		focusHolder = new Text(shell, SWT.SINGLE);
		focusHolder.setBounds(5, HEIGHT + 15, 60, 22);
		shell.open();
		shell.forceActive();
	}

	@After
	public void tearDown() {
		if (shell != null) {
			shell.dispose();
		}
	}

	@Test
	public void gridLooksLikeTheNativeTableAndTree() {
		scene("rows", 0, false, false, false);
		scene("selected-focused", 0, true, true, false);
		scene("selected-unfocused", 0, true, false, false);
		scene("focused-unselected", 0, false, true, false);
		scene("check", SWT.CHECK, false, false, false);
		scene("disabled", 0, true, false, true);
		treeScene("tree", false, true);
		treeScene("tree-selected-focused", true, true);
		treeScene("tree-collapsed-selected-unfocused", true, false);
		System.out.println(report);
		assertTrue(report.toString(), !failed || Boolean.getBoolean("grid.nativelook.measure"));
	}

	private void scene(final String name, final int style, final boolean select, final boolean focus,
			final boolean disabled) {
		final Table table = new Table(shell, SWT.MULTI | SWT.FULL_SELECTION | style);
		table.setBounds(5, 5, WIDTH, HEIGHT);
		table.setHeaderVisible(true);
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | style);
		grid.setBounds(WIDTH + 20, 5, WIDTH, HEIGHT);
		grid.setHeaderVisible(true);
		// the native default; Grid shows lines by default
		grid.setLinesVisible(false);
		for (int c = 0; c < WIDTHS.length; c++) {
			final TableColumn tc = new TableColumn(table, SWT.NONE);
			tc.setText("Column " + c);
			tc.setWidth(WIDTHS[c]);
			final GridColumn gc = new GridColumn(grid, SWT.NONE);
			gc.setText("Column " + c);
			gc.setWidth(WIDTHS[c]);
		}
		for (int r = 0; r < ROWS; r++) {
			final TableItem ti = new TableItem(table, SWT.NONE);
			final GridItem gi = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < WIDTHS.length; c++) {
				ti.setText(c, "Row " + r + " cell " + c);
				gi.setText(c, "Row " + r + " cell " + c);
			}
			if ((style & SWT.CHECK) != 0 && r % 3 == 1) {
				ti.setChecked(true);
				gi.setChecked(true);
			}
		}
		if (select) {
			// the native selection also moves the focus row; the Grid takes it explicitly
			table.setSelection(2);
			grid.setSelection(2);
			grid.setFocusItem(grid.getItem(2));
		} else if (focus) {
			// a focus row without selection
			table.setSelection(4);
			table.deselectAll();
			grid.setFocusItem(grid.getItem(4));
		}
		table.setEnabled(!disabled);
		grid.setEnabled(!disabled);
		compare(name, table, grid, focus);
		table.dispose();
		grid.dispose();
	}

	private void treeScene(final String name, final boolean select, final boolean focus) {
		final Tree tree = new Tree(shell, SWT.MULTI | SWT.FULL_SELECTION);
		tree.setBounds(5, 5, WIDTH, HEIGHT);
		tree.setHeaderVisible(true);
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(WIDTH + 20, 5, WIDTH, HEIGHT);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(false);
		for (int c = 0; c < WIDTHS.length; c++) {
			final TreeColumn tc = new TreeColumn(tree, SWT.NONE);
			tc.setText("Column " + c);
			tc.setWidth(WIDTHS[c]);
			final GridColumn gc = new GridColumn(grid, SWT.NONE);
			gc.setText("Column " + c);
			gc.setWidth(WIDTHS[c]);
			gc.setTree(c == 0);
		}
		TreeItem selectedTreeItem = null;
		GridItem selectedGridItem = null;
		for (int r = 0; r < 4; r++) {
			final TreeItem ti = new TreeItem(tree, SWT.NONE);
			final GridItem gi = new GridItem(grid, SWT.NONE);
			text(ti, gi, "Root " + r);
			for (int k = 0; r % 2 == 0 && k < 3; k++) {
				final TreeItem tk = new TreeItem(ti, SWT.NONE);
				final GridItem gk = new GridItem(gi, SWT.NONE);
				text(tk, gk, "Child " + r + "." + k);
				if (r == 0 && k == 1) {
					selectedTreeItem = tk;
					selectedGridItem = gk;
				}
			}
			if (r == 0 && (focus || !select)) {
				ti.setExpanded(true);
				gi.setExpanded(true);
			}
		}
		if (select) {
			final TreeItem treeItem = focus ? selectedTreeItem : tree.getItem(2);
			final GridItem gridItem = focus ? selectedGridItem : grid.getRootItem(2);
			tree.setSelection(treeItem);
			grid.setSelection(new GridItem[] { gridItem });
			grid.setFocusItem(gridItem);
		}
		// both controls draw focus cues by the same window UI state (the native tree hides its focus
		// rectangle until the keyboard is used); set it once for the shell, as no key was pressed
		hideFocusCues(shell);
		compare(name, tree, grid, select && focus);
		tree.dispose();
		grid.dispose();
	}

	/*
	 * WM_CHANGEUISTATE (UIS_SET, UISF_HIDEFOCUS) to the shell, which updates every child: the test
	 * desktop may leave the UI state of controls created at different times out of step (win32
	 * only, through reflection; elsewhere nothing to do)
	 */
	private void hideFocusCues(final Shell target) {
		try {
			final Class<?> os = Class.forName("org.eclipse.swt.internal.win32.OS");
			final long handle = org.eclipse.swt.widgets.Control.class.getField("handle").getLong(target);
			os.getMethod("SendMessage", long.class, int.class, long.class, long.class).invoke(null, handle, 0x0127,
					0x10001L, 0L);
		} catch (final ReflectiveOperationException notWin32) {
			return;
		}
		settle();
	}

	private static void text(final TreeItem ti, final GridItem gi, final String text) {
		for (int c = 0; c < WIDTHS.length; c++) {
			ti.setText(c, c == 0 ? text : text + " / " + c);
			gi.setText(c, c == 0 ? text : text + " / " + c);
		}
	}

	private void compare(final String name, final Control nativeControl, final Control grid, final boolean focus) {
		final ImageData expected = capture(nativeControl, focus);
		final ImageData actual = capture(grid, focus);
		final String difference = difference(expected, actual);
		final int count = Integer.parseInt(difference.substring(0, difference.indexOf(' ')));
		final Integer residual = RESIDUAL.get(name);
		final boolean ok = residual != null && count <= residual.intValue();
		failed |= !ok;
		report.append(String.format("GridNativeLook_Test: %-22s %s (residual %s)%n", name, difference,
				residual == null ? "not documented" : residual));
		if (count > 0) {
			save(expected, name + "-native");
			save(actual, name + "-grid");
		}
	}

	private ImageData capture(final Control control, final boolean focus) {
		focus(focus ? control : focusHolder);
		// the print can return the window's last presented surface (blank right after the shell
		// opened, or the state before the last change): print until two prints agree
		ImageData previous = null;
		for (int attempt = 0; attempt < 8; attempt++) {
			control.redraw();
			control.update();
			settle();
			final ImageData data = print(control);
			if (previous != null && java.util.Arrays.equals(previous.data, data.data)) {
				return data;
			}
			previous = data;
		}
		return previous;
	}

	private ImageData print(final Control control) {
		// WM_PRINT capture: works on the hidden test desktop, where the screen is not composed
		final Rectangle bounds = control.getBounds();
		final Image image = new Image(display, bounds.width, bounds.height);
		final GC gc = new GC(image);
		try {
			control.print(gc);
			return image.getImageData();
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	/* gives the control the focus and waits until it has it (the test desktop may activate late) */
	private void focus(final Control control) {
		for (int attempt = 0; attempt < 20 && display.getFocusControl() != control; attempt++) {
			control.getShell().forceActive();
			control.setFocus();
			settle();
		}
		assertSame("focus on " + control, control, display.getFocusControl());
	}

	private void settle() {
		final long end = System.currentTimeMillis() + 150;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	/* number of differing pixels and their bounding box */
	static String difference(final ImageData expected, final ImageData actual) {
		int count = 0, left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1;
		for (int y = 0; y < Math.min(expected.height, actual.height); y++) {
			for (int x = 0; x < Math.min(expected.width, actual.width); x++) {
				if (!expected.palette.getRGB(expected.getPixel(x, y))
						.equals(actual.palette.getRGB(actual.getPixel(x, y)))) {
					count++;
					left = Math.min(left, x);
					top = Math.min(top, y);
					right = Math.max(right, x);
					bottom = Math.max(bottom, y);
				}
			}
		}
		return count == 0 ? "0 pixels" : count + " pixels in [" + left + "," + top + " .. " + right + "," + bottom + "]";
	}

	private static void save(final ImageData data, final String name) {
		final String dir = System.getProperty("grid.nativelook.out");
		if (dir == null) {
			return;
		}
		final ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { data };
		loader.save(new File(dir, name + ".png").getPath(), SWT.IMAGE_PNG);
	}
}
