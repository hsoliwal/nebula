/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.eclipse.nebula.widgets.grid.model.RowGrouping;
import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Differential test: a virtual Grid driven the classic way (the application sorts and
 * filters its own index, then calls {@code setItemCount} + {@code clearAll} and re-selects)
 * against a Grid driven through {@link RowPermutation}. Every public result compared must be
 * identical, including the painted pixels.
 */
public class GridRowPermutation_Test {

	private static final int ROWS = 400;
	private static final int COLUMNS = 5;

	private Display display;
	private Shell shell;
	private Grid classic;
	private Grid packed;
	private RowPermutation permutation;
	private int[] appView;
	private String[][] data;
	private int[] keys;
	private Text focusSink;
	private final List<String> classicHeaders = new ArrayList<>();

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		// created first: initial keyboard focus stays outside both Grids, so neither gets
		// the focus-in default focus item and focus painting cannot differ
		focusSink = new Text(shell, SWT.SINGLE);
		focusSink.setBounds(0, 0, 60, 20);
		final Random random = new Random(20261013L);
		data = new String[ROWS][COLUMNS];
		keys = new int[ROWS];
		for (int m = 0; m < ROWS; m++) {
			keys[m] = random.nextInt(37);
			for (int c = 0; c < COLUMNS; c++) {
				data[m][c] = "r" + m + "c" + c + "k" + keys[m];
			}
		}
		appView = identity(ROWS);
		classic = grid();
		classic.addListener(SWT.SetData, e -> {
			final int code = appView[e.index];
			if (code < 0) {
				header((GridItem) e.item, classicHeaders.get(~code));
			} else {
				fill((GridItem) e.item, code);
			}
		});
		classic.setItemCount(ROWS);
		packed = grid();
		permutation = new RowPermutation(ROWS);
		packed.addListener(SWT.SetData, e -> {
			final RowGrouping g = packed.getRowGrouping();
			if (g != null && g.isGroupHeader(e.index)) {
				final int group = g.groupOf(e.index);
				header((GridItem) e.item, headerText(g.getGroupKey(group), g.getGroupSize(group)));
			} else {
				fill((GridItem) e.item, modelOf(packed, e.index));
			}
		});
		packed.setItemCount(ROWS);
		packed.setRowPermutation(permutation);
		classic.setBounds(0, 30, 420, 330);
		packed.setBounds(440, 30, 420, 330);
		shell.setSize(900, 420);
		shell.open();
		focusSink.forceFocus();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void installEqualsTheIdentityView() throws Exception {
		assertSame(permutation, packed.getRowPermutation());
		assertEquivalent("install");
	}

	@Test
	public void sortFilterAndClearMatchTheClassicDriver() throws Exception {
		select(3, 10, 57, 200);
		focus(57);
		assertEquivalent("selected");

		permutation.sortByIntKeys(keys, false);
		packed.refreshRowPermutation();
		classicApply(reference(false, null));
		assertEquivalent("sort asc");

		permutation.sortByIntKeys(keys, true);
		packed.refreshRowPermutation();
		classicApply(reference(true, null));
		assertEquivalent("sort desc");

		permutation.setFilter(m -> keys[m] % 3 != 0);
		packed.refreshRowPermutation();
		classicApply(reference(true, m -> keys[m] % 3 != 0));
		assertEquivalent("filter");

		permutation.clearSort();
		packed.refreshRowPermutation();
		classicApply(reference(null, m -> keys[m] % 3 != 0));
		assertEquivalent("clear sort");

		permutation.clearFilter();
		packed.refreshRowPermutation();
		classicApply(identity(ROWS));
		assertEquivalent("clear filter");

		packed.setRowPermutation(null);
		assertNull(packed.getRowPermutation());
		assertEquivalent("uninstall");
	}

	@Test
	public void groupingMatchesTheClassicDriver() throws Exception {
		select(3, 10, 57);
		permutation.sortByIntKeys(keys, true);
		packed.refreshRowPermutation();
		int[] base = reference(true, null);
		classicApply(base);
		assertEquivalent("sorted");

		final int[] groupKeys = new int[ROWS];
		for (int m = 0; m < ROWS; m++) {
			groupKeys[m] = keys[m] % 5;
		}
		final Set<Integer> collapsed = new HashSet<>();
		final RowGrouping grouping = new RowGrouping(permutation);
		grouping.groupBy(groupKeys);
		packed.setRowGrouping(grouping);
		assertSame(grouping, packed.getRowGrouping());
		assertSame(permutation, packed.getRowPermutation());
		classicApply(grouped(base, groupKeys, collapsed));
		assertEquivalent("grouped");

		collapsed.add(grouping.getGroupKey(1));
		grouping.setExpanded(1, false);
		packed.refreshRowPermutation();
		classicApply(grouped(base, groupKeys, collapsed));
		assertEquivalent("collapsed");

		permutation.setFilter(m -> keys[m] % 3 != 0);
		packed.refreshRowPermutation();
		base = reference(true, m -> keys[m] % 3 != 0);
		classicApply(grouped(base, groupKeys, collapsed));
		assertEquivalent("filtered groups");

		grouping.setExpanded(0, grouping.getGroupCount(), true);
		collapsed.clear();
		packed.refreshRowPermutation();
		classicApply(grouped(base, groupKeys, collapsed));
		assertEquivalent("expanded all");

		packed.setRowGrouping(null);
		classicApply(base);
		assertEquivalent("ungrouped");
	}

	@Test
	public void scrolledViewMatchesAfterSort() throws Exception {
		classic.setTopIndex(150);
		packed.setTopIndex(150);
		flush();
		permutation.sortByIntKeys(keys, true);
		packed.refreshRowPermutation();
		classicApply(reference(true, null));
		assertEquivalent("scrolled sort");
	}

	@Test
	public void refreshWhileHiddenDefersOnlyTheViewportNotification() throws Exception {
		packed.setVisible(false);
		classic.setVisible(false);
		final int[] notified = new int[1];
		packed.addViewportChangeListener(() -> notified[0]++);
		permutation.sortByIntKeys(keys, false);
		packed.refreshRowPermutation();
		classicApply(reference(false, null));
		assertEquals("no viewport work while hidden", 0, notified[0]);
		assertEquals(classic.getItemCount(), packed.getItemCount());
		for (int v = 0; v < ROWS; v += 37) {
			assertEquals(classic.getItem(v).getText(1), packed.getItem(v).getText(1));
		}
		packed.setVisible(true);
		classic.setVisible(true);
		flush();
		assertEquals("one catch-up notification after becoming visible", 1, notified[0]);
		assertEquivalent("after show");
	}

	@Test
	public void rejectsNonVirtualGrids() {
		final Grid plain = new Grid(shell, SWT.NONE);
		try {
			plain.setRowPermutation(new RowPermutation(3));
			fail();
		} catch (final IllegalArgumentException expected) {
			// expected
		}
	}

	@Test
	public void recyclesMaterializedItemsInPlace() {
		final GridItem first = packed.getItem(0);
		permutation.sortByIntKeys(keys, true);
		packed.refreshRowPermutation();
		assertSame("item at a surviving index is recycled, not re-created", first, packed.getItem(0));
		assertEquals(data[permutation.modelRow(0)][0], packed.getItem(0).getText(0));
	}

	private void assertEquivalent(final String step) throws Exception {
		flush();
		assertEquals(step, classic.getItemCount(), packed.getItemCount());
		final int n = classic.getItemCount();
		for (int v = 0; v < n; v++) {
			for (int c = 0; c < COLUMNS; c++) {
				assertEquals(step + " v=" + v + " c=" + c, classic.getItem(v).getText(c), packed.getItem(v).getText(c));
			}
		}
		assertArrayEquals(step + " selection", sorted(classic.getSelectionIndices()), sorted(packed.getSelectionIndices()));
		assertEquals(step + " focus", index(classic, classic.getFocusItem()), index(packed, packed.getFocusItem()));
		assertEquals(step + " top", classic.getTopIndex(), packed.getTopIndex());
		for (int y = 5; y < 300; y += 23) {
			final GridItem a = classic.getItem(new Point(40, y));
			final GridItem b = packed.getItem(new Point(40, y));
			assertEquals(step + " hit y=" + y, index(classic, a), index(packed, b));
		}
		assertEquals(step + " pixels", pixels(classic), pixels(packed));
	}

	private void classicApply(final int[] newView) {
		final List<Integer> selectedModels = new ArrayList<>();
		for (final int v : classic.getSelectionIndices()) {
			if (appView[v] >= 0) {
				selectedModels.add(appView[v]);
			}
		}
		final int focusModel = classic.getFocusItem() == null ? -1 : appView[classic.indexOf(classic.getFocusItem())];
		appView = newView;
		classic.setItemCount(newView.length);
		classic.clearAll(false);
		classic.deselectAll();
		final int[] inverse = new int[ROWS];
		Arrays.fill(inverse, -1);
		for (int v = 0; v < newView.length; v++) {
			if (newView[v] >= 0) {
				inverse[newView[v]] = v;
			}
		}
		for (final int m : selectedModels) {
			if (inverse[m] >= 0) {
				classic.select(inverse[m]);
			}
		}
		if (focusModel >= 0 && inverse[focusModel] >= 0) {
			classic.setFocusItem(classic.getItem(inverse[focusModel]));
		}
	}

	private int[] reference(final Boolean descending, final java.util.function.IntPredicate filter) {
		final Integer[] boxed = new Integer[ROWS];
		for (int i = 0; i < ROWS; i++) {
			boxed[i] = i;
		}
		if (descending != null) {
			Arrays.sort(boxed, (a, b) -> descending ? Integer.compare(keys[b], keys[a]) : Integer.compare(keys[a], keys[b]));
		}
		return Arrays.stream(boxed).mapToInt(Integer::intValue).filter(m -> filter == null || filter.test(m)).toArray();
	}

	private void select(final int... indices) {
		for (final int i : indices) {
			classic.select(i);
			packed.select(i);
		}
	}

	private void focus(final int index) {
		classic.setFocusItem(classic.getItem(index));
		packed.setFocusItem(packed.getItem(index));
	}

	private Grid grid() {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		for (int c = 0; c < COLUMNS; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("C" + c);
			column.setWidth(80);
		}
		return grid;
	}

	private static int modelOf(final Grid grid, final int index) {
		final RowGrouping g = grid.getRowGrouping();
		if (g != null) {
			return g.modelRow(index);
		}
		final RowPermutation p = grid.getRowPermutation();
		return p == null ? index : p.modelRow(index);
	}

	private static String headerText(final int key, final int size) {
		return "G" + key + " n=" + size;
	}

	private static void header(final GridItem item, final String text) {
		item.setText(0, text);
		for (int c = 1; c < COLUMNS; c++) {
			item.setText(c, "");
		}
	}

	/** Classic application-side grouping: header code ~g, then the group's rows unless collapsed. */
	private int[] grouped(final int[] rows, final int[] groupKeys, final Set<Integer> collapsedKeys) {
		final Map<Integer, List<Integer>> groups = new LinkedHashMap<>();
		for (final int m : rows) {
			groups.computeIfAbsent(groupKeys[m], k -> new ArrayList<>()).add(m);
		}
		classicHeaders.clear();
		final List<Integer> out = new ArrayList<>();
		for (final Map.Entry<Integer, List<Integer>> e : groups.entrySet()) {
			out.add(~classicHeaders.size());
			classicHeaders.add(headerText(e.getKey(), e.getValue().size()));
			if (!collapsedKeys.contains(e.getKey())) {
				out.addAll(e.getValue());
			}
		}
		return out.stream().mapToInt(Integer::intValue).toArray();
	}

	private void fill(final GridItem item, final int model) {
		for (int c = 0; c < COLUMNS; c++) {
			item.setText(c, data[model][c]);
		}
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain
		}
	}

	private static int index(final Grid grid, final GridItem item) {
		return item == null ? -1 : grid.indexOf(item);
	}

	private static int[] sorted(final int[] a) {
		final int[] c = a.clone();
		Arrays.sort(c);
		return c;
	}

	private static int[] identity(final int n) {
		final int[] a = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = i;
		}
		return a;
	}

	/** SHA-256 of the Grid rendered by {@code Control.print(GC)} (capture mode CONTROL_PRINT). */
	static String pixels(final Grid grid) throws Exception {
		final Point size = grid.getSize();
		final Image image = new Image(grid.getDisplay(), size.x, size.y);
		try {
			final GC gc = new GC(image);
			try {
				// capture mode CONTROL_PRINT: Control.print(GC) renders the widget's own paint
				if (!grid.print(gc)) {
					throw new IllegalStateException("Control.print unsupported");
				}
			} finally {
				gc.dispose();
			}
			final String dump = System.getProperty("grid.pixels.dump");
			if (dump != null) {
				final org.eclipse.swt.graphics.ImageLoader loader = new org.eclipse.swt.graphics.ImageLoader();
				loader.data = new org.eclipse.swt.graphics.ImageData[] { image.getImageData() };
				loader.save(dump + "-" + System.identityHashCode(grid) + "-" + System.nanoTime() + ".png", SWT.IMAGE_PNG);
			}
			final byte[] digest = MessageDigest.getInstance("SHA-256").digest(image.getImageData().data);
			final StringBuilder hex = new StringBuilder();
			for (final byte b : digest) {
				hex.append(String.format("%02x", b & 0xff));
			}
			return hex.toString();
		} finally {
			image.dispose();
		}
	}

	static void assertMaterializedAtMost(final Grid grid, final int bound) {
		assertTrue("materialized " + grid.virtualMaterializedItemCount() + " > " + bound,
				grid.virtualMaterializedItemCount() <= bound);
	}
}
