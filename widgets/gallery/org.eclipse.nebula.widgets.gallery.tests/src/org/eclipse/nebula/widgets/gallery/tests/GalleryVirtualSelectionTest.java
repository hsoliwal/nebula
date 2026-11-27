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
	 * ctrl-clicks (on items and group titles), remove, insert and dispose items and groups, remove groups and cut them
	 * off with setItemCount, create items, getSelection. The Selection events, the count and the selection (as
	 * group/item index paths, in order) must be the same after every step, and getSelection must never create an item
	 * (send SetData) under a group the gallery no longer holds.
	 * <p>
	 * Once a group leaves the gallery (remove, setItemCount, dispose), the original keeps the items it had created for
	 * its selected children in the selection, stale, and a shift-click whose anchor was in that group selects more of
	 * them; a VIRTUAL gallery never creates the unread ones. When the counts first differ, the selections must still be
	 * equal on their items that are in the gallery; from then on they are compared on those items, and the events
	 * where both name such an item.
	 * </p>
	 */
	@Test
	public void virtualSelectionAnswersMatchTheSerialSelection() {
		for (int seed = 0; seed < 32; seed++) {
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
		int[] detachedSetData = { 0 };
		boolean[] reading = { false };
		lazy.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setItemCount(sizes[e.index]);
			} else if (reading[0] && lazy.indexOf(item.getParentItem()) < 0) {
				detachedSetData[0]++;
			}
		});
		// getSelection on the VIRTUAL gallery, counting the SetData it sends under a group the gallery no longer holds
		java.util.function.Function<Boolean, String> lazyPaths = attachedOnly -> {
			reading[0] = true;
			try {
				return paths(lazy, attachedOnly.booleanValue());
			} finally {
				reading[0] = false;
			}
		};
		List<String> serialEvents = new ArrayList<>(), lazyEvents = new ArrayList<>();
		for (int type : new int[] { SWT.Selection, SWT.DefaultSelection }) {
			serial.addListener(type, e -> serialEvents.add(e.type + ":" + path(serial, (GalleryItem) e.item)));
			lazy.addListener(type, e -> lazyEvents.add(e.type + ":" + path(lazy, (GalleryItem) e.item)));
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
		boolean stale = false;
		for (int step = 0; step < 300; step++) {
			int a = random.nextInt(1 << 16);
			int op = random.nextInt(13);
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
			case 10:
				if (count > 1 && step % 4 == 0) {
					if (a % 2 == 0) {
						log.add("remove group " + g);
						operation = gallery -> gallery.remove(g);
					} else {
						int keep = 1 + a % (count - 1);
						log.add("setItemCount " + keep);
						operation = gallery -> gallery.setItemCount(keep);
					}
				}
				break;
			default:
				log.add("getSelection");
				if (!stale) {
					assertEquals("seed " + seed + ": " + log, paths(serial, false), lazyPaths.apply(Boolean.FALSE));
				}
			}
			int serialEventsBefore = serialEvents.size(), lazyEventsBefore = lazyEvents.size();
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
			assertEquals("getSelection SetData under a detached group, seed " + seed + ": " + log, 0, detachedSetData[0]);
			if (!stale && serial.getSelectionCount() != lazy.getSelectionCount()) {
				// only stale items (no longer in the gallery) may make the counts differ: checked just below
				stale = true;
			}
			if (!stale) {
				assertEquals("seed " + seed + ": " + log, serialEvents, lazyEvents);
				continue;
			}
			assertEquals("seed " + seed + ": " + log, paths(serial, true), lazyPaths.apply(Boolean.TRUE));
			List<String> s = serialEvents.subList(serialEventsBefore, serialEvents.size());
			List<String> l = lazyEvents.subList(lazyEventsBefore, lazyEvents.size());
			assertEquals("seed " + seed + ": " + log, s.size(), l.size());
			for (int e = 0; e < s.size(); e++) {
				// an event names the item selected last, which may be a stale item only the original kept
				if (!s.get(e).endsWith(":stale") && !l.get(e).endsWith(":stale")) {
					assertEquals("seed " + seed + ": " + log, s.get(e), l.get(e));
				}
			}
		}
		assertEquals("seed " + seed + ": " + log, paths(serial, stale), lazyPaths.apply(Boolean.valueOf(stale)));
		assertEquals("getSelection SetData under a detached group, seed " + seed + ": " + log, 0, detachedSetData[0]);
		shell.dispose();
	}

	/* the selection as group/item index paths, in order; without the stale items (no longer in the gallery) if asked */
	static String paths(Gallery gallery, boolean attachedOnly) {
		StringBuilder out = new StringBuilder();
		for (GalleryItem item : gallery.getSelection()) {
			String path = path(gallery, item);
			if (!attachedOnly || !"stale".equals(path)) {
				out.append(path).append(' ');
			}
		}
		return out.toString();
	}

	/* the group/item index path of an item, "stale" for an item (or its group) disposed or no longer in the gallery */
	static String path(Gallery gallery, GalleryItem item) {
		if (item == null) {
			return "-";
		}
		GalleryItem parent = item.getParentItem();
		if (item.isDisposed() || parent != null && parent.isDisposed()) {
			return "stale";
		}
		if (parent == null) {
			int index = gallery.indexOf(item);
			return index < 0 ? "stale" : String.valueOf(index);
		}
		int group = gallery.indexOf(parent), index = parent.indexOf(item);
		return group < 0 || index < 0 ? "stale" : group + "/" + index;
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

	/**
	 * A group that leaves the gallery (remove, VIRTUAL clear, setItemCount) after a selectAll, in 2 groups of 50,000
	 * items: the selection API never creates items, nor sends SetData, under that group (the original had created them
	 * in selectAll; before the runs were released, getSelection sent SetData there for every unread child).
	 */
	@Test
	public void groupsLeavingTheGallerySendNoSetDataUnderThem() {
		for (int operation = 0; operation < 3; operation++) {
			create(2, ITEMS / 2, SWT.MULTI);
			int[] detached = { 0 };
			gallery.addListener(SWT.SetData, e -> {
				GalleryItem parent = ((GalleryItem) e.item).getParentItem();
				if (parent != null && gallery.indexOf(parent) < 0) {
					detached[0]++;
				}
			});
			GalleryItem leaving = gallery.getItem(operation == 2 ? 1 : 0);
			List<GalleryItem> existing = new ArrayList<>();
			for (GalleryItem item : leaving.getItems()) {
				if (item != null) {
					existing.add(item);
				}
			}
			gallery.selectAll();
			String name = new String[] { "remove(0)", "clear(0)", "setItemCount(1)" }[operation];
			if (operation == 0) {
				gallery.remove(0);
			} else if (operation == 1) {
				gallery.clear(0);
			} else {
				gallery.setItemCount(1);
			}
			flush();
			assertTrue(name, gallery.indexOf(leaving) < 0);
			java.util.Set<GalleryItem> selection = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
			selection.addAll(java.util.Arrays.asList(gallery.getSelection()));
			assertEquals(name + ": SetData under the detached group", 0, detached[0]);
			// the items of the detached group that existed keep their entries, as in the original
			for (GalleryItem item : existing) {
				assertTrue(name + ": an existing item of the detached group is still selected", selection.contains(item));
			}
			Event enter = new Event();
			enter.keyCode = SWT.CR;
			gallery.notifyListeners(SWT.KeyDown, enter);
			assertEquals(name + ": SetData under the detached group after Enter", 0, detached[0]);
			shell.dispose();
		}
	}

	/**
	 * A group reselected after its own deselectAll (what collapsing it does), then disposed: no SetData for its
	 * children, the children that existed are disposed, and no selected item is left alive under the disposed group.
	 */
	@Test
	public void disposingAReselectedGroupCreatesNothingUnderIt() {
		create(2, 40, SWT.MULTI);
		GalleryItem group = gallery.getItem(1);
		List<GalleryItem> existing = new ArrayList<>();
		for (GalleryItem item : group.getItems()) {
			if (item != null) {
				existing.add(item);
			}
		}
		assertTrue("the second group is partly in view: " + existing.size(), existing.size() > 0 && existing.size() < 40);
		gallery.selectAll();
		group.deselectAll();
		group.selectAll();
		int setData = itemSetData;
		group.dispose();
		assertEquals("SetData sent while disposing the group", 0, itemSetData - setData);
		for (GalleryItem item : existing) {
			assertTrue("a child that existed is disposed with its group", item.isDisposed());
		}
		for (GalleryItem item : gallery.getSelection()) {
			GalleryItem parent = item.getParentItem();
			assertFalse("a selected item alive under the disposed group", parent == group && !item.isDisposed());
		}
	}

	/**
	 * Dropping many selected groups at once (setItemCount, _setGalleryItems, as a viewer refresh does) releases their
	 * runs in one sweep: linear, not one pass over the selection per dropped group (about 2 s for 16,000 groups before).
	 */
	@Test
	public void droppingManySelectedGroupsIsLinear() {
		int groups = 16_000;
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		gallery = new Gallery(shell, SWT.V_SCROLL | SWT.MULTI | SWT.VIRTUAL);
		gallery.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setItemCount(5);
			}
		});
		gallery.setItemCount(groups);
		long worst = 0;
		for (int operation = 0; operation < 3; operation++) {
			gallery.setItemCount(groups);
			gallery.selectAll();
			assertEquals(groups * 6, gallery.getSelectionCount());
			long start = System.nanoTime();
			if (operation == 0) {
				gallery.setItemCount(0);
			} else if (operation == 1) {
				gallery.setItemCount(groups / 2);
			} else {
				gallery._setGalleryItems(java.util.Arrays.copyOf(gallery.getItems(), groups / 2));
			}
			worst = Math.max(worst, (System.nanoTime() - start) / 1_000_000);
			gallery.deselectAll();
		}
		assertTrue("the slowest drop of 16,000 or 8,000 selected groups took " + worst + " ms", worst < 400);
	}

	/**
	 * The same with stale index hints: 64,000 selected groups put in another order with _setGalleryItems, then dropped.
	 * The dropped groups are found by identity, not by index (0.7 to 2.4 s before, 13 to 131 ms here, master 0.02 to
	 * 0.2 s).
	 */
	@Test
	public void droppingShuffledGroupsIsLinear() {
		int groups = 64_000;
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		gallery = new Gallery(shell, SWT.V_SCROLL | SWT.MULTI | SWT.VIRTUAL);
		gallery.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setItemCount(2);
			}
		});
		long worst = 0;
		for (int operation = 0; operation < 2; operation++) {
			gallery.setItemCount(groups);
			gallery.selectAll();
			List<GalleryItem> shuffled = new ArrayList<>(java.util.Arrays.asList(gallery.getItems()));
			java.util.Collections.shuffle(shuffled, new Random(operation));
			gallery._setGalleryItems(shuffled.toArray(new GalleryItem[0]));
			long start = System.nanoTime();
			gallery.setItemCount(operation == 0 ? 0 : groups / 2);
			worst = Math.max(worst, (System.nanoTime() - start) / 1_000_000);
			gallery.deselectAll();
			gallery.setItemCount(0);
		}
		assertTrue("the slowest drop of 64,000 or 32,000 shuffled selected groups took " + worst + " ms", worst < 300);
	}

	/** A Gallery subclass that counts the selection hook. */
	static class CountingGallery extends Gallery {
		int added;

		CountingGallery(org.eclipse.swt.widgets.Composite parent, int style) {
			super(parent, style);
		}

		@Override
		protected void _addSelection(GalleryItem item) {
			added++;
			super._addSelection(item);
		}
	}

	/** A GalleryItem subclass, used as a group, that counts the selection hook for its children. */
	static class CountingGroup extends GalleryItem {
		int added;

		CountingGroup(Gallery parent, int style) {
			super(parent, style);
		}

		@Override
		protected void _addSelection(GalleryItem item) {
			added++;
			super._addSelection(item);
		}
	}

	/**
	 * Subclasses that override the protected selection hooks keep the original per-item range selection: the hook is
	 * called for every child, as before (each child created, with its SetData, as before).
	 */
	@Test
	public void overriddenSelectionHooksAreCalledForEveryChild() {
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		CountingGallery counting = new CountingGallery(shell, SWT.V_SCROLL | SWT.MULTI | SWT.VIRTUAL);
		int[] setData = { 0 };
		counting.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setItemCount(1000);
			} else {
				setData[0]++;
			}
		});
		counting.setItemCount(1);
		counting.getItem(0).setExpanded(true);
		shell.open();
		flushDisplay();
		counting.selectAll();
		assertEquals("Gallery._addSelection calls: the group and each of its 1000 children", 1001, counting.added);
		assertEquals(1001, counting.getSelectionCount());
		assertEquals("every child created, as by the original range selection", 1000, setData[0]);
		shell.dispose();

		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		Gallery plain = new Gallery(shell, SWT.V_SCROLL | SWT.MULTI | SWT.VIRTUAL);
		CountingGroup group = new CountingGroup(plain, SWT.NONE);
		group.setItemCount(1000);
		group.setExpanded(true);
		shell.open();
		flushDisplay();
		group.added = 0;
		group.selectAll();
		assertEquals("GalleryItem._addSelection calls on the group: each of its 1000 children", 1000, group.added);
		assertEquals(1000, plain.getSelectionCount());
		shell.dispose();
	}

	void flushDisplay() {
		while (display.readAndDispatch()) {
			// dispatch everything queued
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
