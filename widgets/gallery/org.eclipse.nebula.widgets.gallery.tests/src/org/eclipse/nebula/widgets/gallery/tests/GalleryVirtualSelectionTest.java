/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.gallery.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * The SWT.VIRTUAL Gallery is its own viewport: with 100,000 items, a range selection (selectAll, shift-click, a group's
 * selectAll) creates no GalleryItem and sends no SWT.SetData for the items that are not shown, a paint draws only the
 * cells in view, no child control or shell is created, and the selection API answers (count, order, identity of the
 * items that exist, Selection events) are those of the serial selection. Viewport mode (the default) only: the
 * original mode is the stock implementation.
 */
public class GalleryVirtualSelectionTest {

	static final int ITEMS = 100_000;

	Display display;
	Shell shell;
	Gallery gallery;
	int shellsBefore;
	int groupSize;

	/** SWT.SetData events for children (items, not groups). */
	int itemSetData;

	/** SWT.PaintItem events (one per cell drawn). */
	int paintItems;

	/** SWT.Paint events, and the most cells one of them drew since the last reset. */
	int paints, paintItemsMark, maxCellsPerPaint;

	final List<Object> selectionEvents = new ArrayList<>();
	final List<Object> defaultSelectionEvents = new ArrayList<>();

	@Before
	public void setUp() {
		Assume.assumeFalse("viewport mode only (the original mode is stock)", originalMode());
		display = Display.getDefault();
		shellsBefore = display.getShells().length;
	}

	/*
	 * True when the startup properties select the original Gallery (-Dswt.widget.mode.Gallery=original or
	 * -Dswt.widget.mode=original); any other value is the viewport, the Gallery's only other mode. Read from the
	 * properties: the widget-mode class is internal to the Gallery bundle.
	 */
	static boolean originalMode() {
		String specific = System.getProperty("swt.widget.mode.Gallery");
		String value = specific != null ? specific : System.getProperty("swt.widget.mode");
		return value != null && "original".equals(value.trim().toLowerCase(java.util.Locale.ROOT));
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	/* a VIRTUAL MULTI (or SINGLE) gallery of 'groups' expanded groups of 'size' items each, open and painted */
	void create(int groups, int size, int selectionStyle) {
		groupSize = size;
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		gallery = new Gallery(shell, SWT.V_SCROLL | SWT.VIRTUAL | selectionStyle);
		DefaultGalleryGroupRenderer groupRenderer = new DefaultGalleryGroupRenderer();
		groupRenderer.setMinMargin(2);
		groupRenderer.setItemHeight(56);
		groupRenderer.setItemWidth(72);
		groupRenderer.setAutoMargin(true);
		gallery.setGroupRenderer(groupRenderer);
		gallery.setItemRenderer(new DefaultGalleryItemRenderer());
		gallery.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setText("group " + e.index);
				item.setItemCount(groupSize);
			} else {
				itemSetData++;
				item.setText("item " + e.index);
			}
		});
		gallery.addListener(SWT.PaintItem, e -> paintItems++);
		// added after the Gallery's own paint listener: runs when the paint has drawn its cells
		gallery.addListener(SWT.Paint, e -> {
			paints++;
			maxCellsPerPaint = Math.max(maxCellsPerPaint, paintItems - paintItemsMark);
			paintItemsMark = paintItems;
		});
		gallery.addListener(SWT.Selection, e -> selectionEvents.add(e.item));
		gallery.addListener(SWT.DefaultSelection, e -> defaultSelectionEvents.add(e.item));
		gallery.setItemCount(groups);
		for (int g = 0; g < groups; g++) {
			gallery.getItem(g).setExpanded(true);
		}
		int before = paints;
		shell.open();
		settle(before + 1);
	}

	@Test
	public void selectAllCreatesNoItemAndSendsNoSetData() {
		create(1, ITEMS, SWT.MULTI);
		GalleryItem group = gallery.getItem(0);
		GalleryItem first = group.getItem(0);
		int created = created(group), setData = itemSetData;
		assertTrue("the first paint created only the cells in view: " + created, created > 0 && created <= cellBound());

		int before = paints;
		gallery.selectAll();
		settle(before + 1);

		assertEquals("SetData sent by selectAll and its paint", 0, itemSetData - setData);
		assertEquals("items created by selectAll and its paint", created, created(group));
		assertEquals("selectAll sends no Selection event", 0, selectionEvents.size());
		assertEquals(1 + ITEMS, gallery.getSelectionCount());
		assertNoChildControlOrShell();

		// Enter reports the first selected item (the group) without creating the rest of the selection
		Event enter = new Event();
		enter.keyCode = SWT.CR;
		gallery.notifyListeners(SWT.KeyDown, enter);
		assertEquals(1, defaultSelectionEvents.size());
		assertSame(group, defaultSelectionEvents.get(0));
		assertEquals("SetData sent by Enter", 0, itemSetData - setData);

		// The API hands the items out in selection order; the items that existed keep their identity.
		GalleryItem[] selection = gallery.getSelection();
		assertEquals(1 + ITEMS, selection.length);
		assertSame(group, selection[0]);
		assertSame(first, selection[1]);
		for (int i = 0; i < ITEMS; i++) {
			assertSame("selection[" + (1 + i) + "]", group.getItem(i), selection[1 + i]);
		}
		assertEquals("a second call hands out the same items", 1 + ITEMS, gallery.getSelection().length);
		assertSame(selection[ITEMS], gallery.getSelection()[ITEMS]);
		assertEquals(ITEMS, created(group));
	}

	@Test
	public void shiftClickRangeCreatesOnlyTheItemsInView() {
		create(1, ITEMS, SWT.MULTI);
		GalleryItem group = gallery.getItem(0);
		GalleryItem first = group.getItem(0);
		click(first, SWT.NONE);
		assertEquals(1, selectionEvents.size());
		assertSame(first, selectionEvents.get(0));

		scrollTo(Integer.MAX_VALUE);
		GalleryItem last = group.getItem(ITEMS - 1);
		int created = created(group), setData = itemSetData;
		assertTrue("the paints created only the cells in view: " + created, created <= 2 * cellBound());

		selectionEvents.clear();
		click(last, SWT.SHIFT);
		flush();

		assertEquals("SetData sent by the shift-click range and its paint", 0, itemSetData - setData);
		assertEquals("items created by the shift-click range and its paint", created, created(group));
		assertEquals("one Selection event, naming the clicked item", 1, selectionEvents.size());
		assertSame(last, selectionEvents.get(0));
		assertEquals(ITEMS, gallery.getSelectionCount());
		assertNoChildControlOrShell();

		// Ctrl-click deselects one item in the range; the event names the item selected last, as before
		GalleryItem shown = group.getItem(ITEMS - 2);
		selectionEvents.clear();
		click(shown, SWT.MOD1);
		assertEquals(ITEMS - 1, gallery.getSelectionCount());
		assertEquals(1, selectionEvents.size());
		assertSame(last, selectionEvents.get(0));

		GalleryItem[] selection = gallery.getSelection();
		assertEquals(ITEMS - 1, selection.length);
		assertSame(first, selection[0]);
		assertSame(last, selection[ITEMS - 2]);
		for (int i = 0; i < ITEMS - 2; i++) {
			assertSame(group.getItem(i), selection[i]);
		}
	}

	@Test
	public void singleSelectionShiftClickSelectsTheClickedItemOnly() {
		create(1, ITEMS, SWT.SINGLE);
		GalleryItem group = gallery.getItem(0);
		GalleryItem first = group.getItem(0);
		click(first, SWT.NONE);
		scrollTo(Integer.MAX_VALUE);
		GalleryItem last = group.getItem(ITEMS - 1);
		int created = created(group), setData = itemSetData;
		selectionEvents.clear();

		click(last, SWT.SHIFT);
		flush();

		assertEquals("SetData sent by the shift-click range and its paint", 0, itemSetData - setData);
		assertEquals(created, created(group));
		assertEquals(1, gallery.getSelectionCount());
		assertSame(last, gallery.getSelection()[0]);
		assertEquals(1, selectionEvents.size());
		assertSame(last, selectionEvents.get(0));
	}

	@Test
	public void rangesAcrossGroupsCreateNoItem() {
		int groups = ITEMS / 100;
		create(groups, 100, SWT.MULTI);
		GalleryItem firstGroup = gallery.getItem(0), lastGroup = gallery.getItem(groups - 1);
		int created = createdAll(), setData = itemSetData;

		gallery.selectAll();
		flush();
		assertEquals("SetData sent by selectAll and its paint", 0, itemSetData - setData);
		assertEquals(created, createdAll());
		assertEquals(groups + ITEMS, gallery.getSelectionCount());

		// a group's selectAll (what a click on its title does)
		gallery.deselectAll();
		GalleryItem middle = gallery.getItem(groups / 2);
		middle.selectAll();
		flush();
		assertEquals("SetData sent by a group's selectAll", 0, itemSetData - setData);
		assertEquals(100, gallery.getSelectionCount());

		// shift-click from the first item of the first group to the last item of the last group
		gallery.deselectAll();
		GalleryItem first = firstGroup.getItem(0);
		click(first, SWT.NONE);
		scrollTo(Integer.MAX_VALUE);
		GalleryItem last = lastGroup.getItem(99);
		created = createdAll();
		setData = itemSetData;
		selectionEvents.clear();
		click(last, SWT.SHIFT);
		flush();
		assertEquals("SetData sent by the shift-click range across groups", 0, itemSetData - setData);
		assertEquals(created, createdAll());
		assertEquals(1, selectionEvents.size());
		assertSame(last, selectionEvents.get(0));
		assertEquals(ITEMS, gallery.getSelectionCount());
		assertNoChildControlOrShell();

		GalleryItem[] selection = gallery.getSelection();
		assertEquals(ITEMS, selection.length);
		assertSame(first, selection[0]);
		assertSame(last, selection[ITEMS - 1]);
		for (int i = 0; i < ITEMS; i++) {
			assertSame(gallery.getItem(i / 100).getItem(i % 100), selection[i]);
		}
	}

	@Test
	public void paintDrawsOnlyTheCellsInView() {
		create(1, ITEMS, SWT.MULTI);
		GalleryItem group = gallery.getItem(0);
		gallery.selectAll();
		ScrollBar bar = gallery.getVerticalBar();
		int[] positions = { 0, bar.getMaximum() / 2, bar.getMaximum() / 3, Integer.MAX_VALUE, 0 };
		for (int position : positions) {
			int setData = itemSetData;
			scrollTo(position);
			maxCellsPerPaint = 0;
			int before = paints;
			gallery.redraw();
			settle(before + 1);
			assertTrue("cells painted per frame at " + position + ": " + maxCellsPerPaint,
					maxCellsPerPaint > 0 && maxCellsPerPaint <= cellBound());
			assertTrue("SetData sent by a jump to " + position + ": " + (itemSetData - setData), itemSetData - setData <= cellBound());
		}
		assertTrue("items created after five positions: " + created(group), created(group) <= positions.length * cellBound());
		assertEquals(1 + ITEMS, gallery.getSelectionCount());
		assertNoChildControlOrShell();
	}

	@Test
	public void insertAndRemoveKeepTheSelectedItems() {
		create(1, 1000, SWT.MULTI);
		GalleryItem group = gallery.getItem(0);
		gallery.selectAll();
		assertEquals(1001, gallery.getSelectionCount());

		group.remove(500);
		assertEquals(1000, gallery.getSelectionCount());
		GalleryItem inserted = new GalleryItem(group, SWT.NONE, 10);
		inserted.setText("inserted");
		assertEquals(1000, gallery.getSelectionCount());

		GalleryItem[] selection = gallery.getSelection();
		assertEquals(1000, selection.length);
		assertSame(group, selection[0]);
		for (int i = 0; i < 999; i++) {
			int index = i < 10 ? i : i + 1;
			assertSame("selection[" + (1 + i) + "]", group.getItem(index), selection[1 + i]);
		}
		for (GalleryItem item : selection) {
			assertFalse("the inserted item is not selected", item == inserted);
		}
	}

	/**
	 * Seeded scripts run on a VIRTUAL gallery and on a gallery of created items with the same groups (whose selection
	 * is the serial one): selectAll, deselectAll, a group's selectAll, setSelection, clicks, shift-clicks and
	 * ctrl-clicks (on items and group titles), remove, insert and dispose items and groups, create items, getSelection.
	 * The Selection events, the count and the selection (as group/item index paths, in order) must be the same after
	 * every step.
	 */
	@Test
	public void virtualSelectionAnswersMatchTheSerialSelection() {
		for (int seed = 0; seed < 8; seed++) {
			differential(seed);
		}
	}

	void differential(int seed) {
		Random random = new Random(0x6A11E7L + seed);
		int groups = 1 + random.nextInt(5);
		int[] sizes = new int[groups];
		for (int g = 0; g < groups; g++) {
			sizes[g] = random.nextInt(70);
		}
		int style = SWT.V_SCROLL | (seed % 4 == 3 ? SWT.SINGLE : SWT.MULTI);
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		Gallery serial = new Gallery(shell, style);
		Gallery lazy = new Gallery(shell, style | SWT.VIRTUAL);
		lazy.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setItemCount(sizes[e.index]);
			}
		});
		StringBuilder serialEvents = new StringBuilder(), lazyEvents = new StringBuilder();
		for (int type : new int[] { SWT.Selection, SWT.DefaultSelection }) {
			serial.addListener(type, e -> serialEvents.append(e.type).append(':').append(path(serial, (GalleryItem) e.item)).append(' '));
			lazy.addListener(type, e -> lazyEvents.append(e.type).append(':').append(path(lazy, (GalleryItem) e.item)).append(' '));
		}
		for (int g = 0; g < groups; g++) {
			GalleryItem group = new GalleryItem(serial, SWT.NONE);
			group.setExpanded(true);
			for (int i = 0; i < sizes[g]; i++) {
				new GalleryItem(group, SWT.NONE);
			}
		}
		lazy.setItemCount(groups);
		for (int g = 0; g < groups; g++) {
			lazy.getItem(g).setExpanded(true);
		}
		shell.open();
		List<String> log = new ArrayList<>();
		for (int step = 0; step < 300; step++) {
			int a = random.nextInt(1 << 16);
			int op = random.nextInt(12);
			int count = serial.getItemCount();
			int g = count == 0 ? -1 : a % count;
			int children = g < 0 ? 0 : serial.getItem(g).getItemCount();
			int i = children == 0 ? -1 : (a / 7) % children;
			Consumer<Gallery> operation = null;
			switch (op) {
			case 0:
				log.add("selectAll");
				operation = Gallery::selectAll;
				break;
			case 1:
				log.add("deselectAll");
				operation = Gallery::deselectAll;
				break;
			case 2:
			case 3:
				if (g >= 0) {
					log.add("group " + g + " selectAll");
					operation = gallery -> gallery.getItem(g).selectAll();
				}
				break;
			case 4:
				if (i >= 0) {
					log.add("setSelection " + g + "/" + i);
					operation = gallery -> gallery.setSelection(new GalleryItem[] { gallery.getItem(g).getItem(i) });
				}
				break;
			case 5:
				if (i >= 0) {
					log.add("remove " + g + "/" + i);
					operation = gallery -> gallery.getItem(g).remove(i);
				}
				break;
			case 6:
				if (g >= 0) {
					int at = a % (children + 1);
					log.add("insert " + g + "/" + at);
					operation = gallery -> new GalleryItem(gallery.getItem(g), SWT.NONE, at);
				}
				break;
			case 7:
				if (i >= 0 && a % 3 == 0) {
					log.add("dispose " + g + "/" + i);
					operation = gallery -> gallery.getItem(g).getItem(i).dispose();
				} else if (g >= 0 && count > 1 && a % 3 == 1) {
					log.add("dispose group " + g);
					operation = gallery -> gallery.getItem(g).dispose();
				} else if (i >= 0) {
					log.add("create " + g + "/" + i);
					lazy.getItem(g).getItem(i);
				}
				break;
			case 8:
			case 9: {
				int x = 5 + a % 300, y = 5 + (a / 300) % 460;
				int mask = new int[] { SWT.NONE, SWT.NONE, SWT.SHIFT, SWT.MOD1 }[(a >> 3) & 3];
				log.add("click " + x + "," + y + " mask " + mask);
				operation = gallery -> mouse(gallery, x, y, mask);
				break;
			}
			default:
				log.add("getSelection");
				assertEquals("seed " + seed + ": " + log, paths(serial), paths(lazy));
			}
			if (operation != null) {
				// the original throws on some steps (a shift-click whose anchor was removed, showing an item of a
				// collapsed group): both must throw alike
				Consumer<Gallery> o = operation;
				String threw = attempt(() -> o.accept(serial));
				assertEquals("seed " + seed + ": " + log, threw, attempt(() -> o.accept(lazy)));
				if (!threw.isEmpty()) {
					log.add("threw " + threw);
				}
			}
			assertEquals("seed " + seed + ": " + log, serialEvents.toString(), lazyEvents.toString());
			assertEquals("seed " + seed + ": " + log, serial.getSelectionCount(), lazy.getSelectionCount());
		}
		assertEquals("seed " + seed + ": " + log, paths(serial), paths(lazy));
		shell.dispose();
	}

	/* the selection as group/item index paths, in order */
	static String paths(Gallery gallery) {
		StringBuilder out = new StringBuilder();
		for (GalleryItem item : gallery.getSelection()) {
			out.append(path(gallery, item)).append(' ');
		}
		return out.toString();
	}

	static String path(Gallery gallery, GalleryItem item) {
		if (item == null) {
			return "-";
		}
		GalleryItem parent = item.getParentItem();
		if (item.isDisposed() || parent != null && parent.isDisposed()) {
			return "disposed";
		}
		return parent == null ? String.valueOf(gallery.indexOf(item)) : gallery.indexOf(parent) + "/" + parent.indexOf(item);
	}

	/* the simple name of the exception the operation throws, or "" */
	static String attempt(Runnable operation) {
		try {
			operation.run();
			return "";
		} catch (RuntimeException e) {
			return e.getClass().getSimpleName();
		}
	}

	/* a mouse button 1 press and release at x, y with the modifiers */
	static void mouse(Gallery gallery, int x, int y, int stateMask) {
		for (int type : new int[] { SWT.MouseDown, SWT.MouseUp }) {
			Event event = new Event();
			event.x = x;
			event.y = y;
			event.button = 1;
			event.count = 1;
			event.stateMask = stateMask;
			gallery.notifyListeners(type, event);
		}
	}

	@Test
	public void indexOfAnswersWithoutScanningAfterAJump() {
		int size = 2_000_000;
		create(1, size, SWT.MULTI);
		GalleryItem group = gallery.getItem(0);
		GalleryItem near = group.getItem(0), far = group.getItem(size - 1);
		long start = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			assertEquals(0, group.indexOf(near));
			assertEquals(size - 1, group.indexOf(far));
		}
		long millis = (System.nanoTime() - start) / 1_000_000;
		// The paint loop asks indexOf for every cell it checks for selection. A linear search costs 2 x 1000 x 2M
		// comparisons here (seconds); the index check answers in microseconds.
		assertTrue("2000 alternating indexOf at the two ends of 2,000,000 children took " + millis + " ms", millis < 250);
	}

	/* the cells a 640x480 client can show (72x56 items, 2px margins), plus a partly shown row above and below */
	int cellBound() {
		Rectangle area = gallery.getClientArea();
		return (area.width / 72 + 1) * (area.height / 56 + 2);
	}

	int created(GalleryItem group) {
		int n = 0;
		for (GalleryItem item : group.getItems()) {
			if (item != null) {
				n++;
			}
		}
		return n;
	}

	int createdAll() {
		int n = 0;
		for (GalleryItem group : gallery.getItems()) {
			n += created(group);
		}
		return n;
	}

	void assertNoChildControlOrShell() {
		assertEquals("child controls of the gallery", 0, gallery.getChildren().length);
		assertEquals("shells", shellsBefore + 1, display.getShells().length);
	}

	/* moves the scroll bar as a user drag does, and waits for the paint (a redraw also when the position is the same) */
	void scrollTo(int position) {
		ScrollBar bar = gallery.getVerticalBar();
		int before = paints;
		bar.setSelection(Math.min(position, bar.getMaximum() - bar.getThumb()));
		bar.notifyListeners(SWT.Selection, new Event());
		gallery.redraw();
		settle(before + 1);
	}

	void click(GalleryItem item, int stateMask) {
		Rectangle bounds = item.getBounds();
		int x = bounds.x + bounds.width / 2, y = bounds.y + bounds.height / 2;
		assertTrue("the item is in view: " + bounds, gallery.getClientArea().contains(x, y));
		assertSame(item, gallery.getItem(new org.eclipse.swt.graphics.Point(x, y)));
		for (int type : new int[] { SWT.MouseDown, SWT.MouseUp }) {
			Event event = new Event();
			event.x = x;
			event.y = y;
			event.button = 1;
			event.count = 1;
			event.stateMask = stateMask;
			gallery.notifyListeners(type, event);
		}
	}

	void flush() {
		settle(0);
	}

	/*
	 * Pumps events until at least paintsAtLeast paints were seen and none came for 150 ms: a paint is synchronous with
	 * update() on win32 and comes from the frame clock on GTK.
	 */
	void settle(int paintsAtLeast) {
		gallery.update();
		long start = System.nanoTime(), quietSince = start;
		int seen = paints;
		while (seen < paintsAtLeast || System.nanoTime() - quietSince < 150_000_000L) {
			if (System.nanoTime() - start > 10_000_000_000L) {
				throw new AssertionError("the gallery did not paint: " + seen + " paints, " + paintsAtLeast + " expected");
			}
			if (!display.readAndDispatch()) {
				gallery.update();
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError(e);
				}
			}
			if (paints != seen) {
				seen = paints;
				quietSince = System.nanoTime();
			}
		}
	}
}
