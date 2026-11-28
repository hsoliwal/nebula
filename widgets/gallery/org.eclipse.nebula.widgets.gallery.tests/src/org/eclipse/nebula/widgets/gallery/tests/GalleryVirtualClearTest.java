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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
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
 * A SWT.VIRTUAL Gallery keeps its items across a clear, as Table and Tree do: <code>clear(int)</code>,
 * <code>clearAll()</code> and <code>GalleryItem.clearAll()</code> reset the attributes of the items that exist and
 * request them again with SWT.SetData when the items are next requested (layout for the groups, paint or
 * <code>getItem</code> for the children), naming the same item at the same index. No item is created by a clear,
 * none is replaced, the items that were never created stay uncreated, and the expanded state, item count, data and
 * selection of a cleared item stay. Viewport mode (the default) only: the original mode replaces the items.
 */
public class GalleryVirtualClearTest {

	Display display;
	Shell shell;
	Gallery gallery;
	int shellsBefore;
	int groupSize;

	/** SWT.SetData events for groups and for children since the gallery was created. */
	int groupSetData, itemSetData;

	/** The SWT.SetData events in order: the item, and its index. */
	final List<GalleryItem> requested = new ArrayList<>();
	final List<Integer> requestedIndex = new ArrayList<>();

	int paints;

	final List<Object> selectionEvents = new ArrayList<>();

	@Before
	public void setUp() {
		Assume.assumeFalse("viewport mode only (the original mode is stock)", GalleryVirtualSelectionTest.originalMode());
		display = Display.getDefault();
		shellsBefore = display.getShells().length;
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	/* a VIRTUAL gallery of 'groups' expanded groups of 'size' children each, open and painted */
	void create(int groups, int size, int selectionStyle, boolean virtualGroups) {
		groupSize = size;
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		gallery = new Gallery(shell, SWT.V_SCROLL | SWT.VIRTUAL | selectionStyle);
		gallery.setGroupRenderer(groupRenderer());
		gallery.setItemRenderer(new DefaultGalleryItemRenderer());
		gallery.setVirtualGroups(virtualGroups);
		gallery.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			requested.add(item);
			requestedIndex.add(Integer.valueOf(e.index));
			if (item.getParentItem() == null) {
				groupSetData++;
				item.setText("group " + e.index + " #" + groupSetData);
				item.setItemCount(groupSize);
			} else {
				itemSetData++;
				item.setText("item " + e.index + " #" + itemSetData);
			}
		});
		gallery.addListener(SWT.Paint, e -> paints++);
		gallery.addListener(SWT.Selection, e -> selectionEvents.add(e.item));
		gallery.setItemCount(groups);
		if (!virtualGroups) {
			for (int g = 0; g < groups; g++) {
				gallery.getItem(g).setExpanded(true);
			}
		}
		int before = paints;
		shell.open();
		settle(before + 1);
	}

	static DefaultGalleryGroupRenderer groupRenderer() {
		DefaultGalleryGroupRenderer groupRenderer = new DefaultGalleryGroupRenderer();
		groupRenderer.setMinMargin(2);
		groupRenderer.setItemHeight(56);
		groupRenderer.setItemWidth(72);
		groupRenderer.setAutoMargin(true);
		return groupRenderer;
	}

	/**
	 * 200 groups of 1,000 children: clearAll keeps every group and every child that exists, creates no item, requests
	 * every group again (the layout needs them, as before) and only the children in view. The original replaced the
	 * 200 groups (and so dropped every child) with new items.
	 */
	@Test
	public void clearAllRequestsTheSameItemsAgainAndCreatesNone() {
		create(200, 1000, SWT.MULTI, false);
		scrollTo(gallery.getVerticalBar().getMaximum() / 2);
		GalleryItem[] groups = gallery.getItems();
		Set<GalleryItem> existing = identitySet();
		List<GalleryItem[]> children = new ArrayList<>();
		for (GalleryItem group : groups) {
			GalleryItem[] slots = group.getItems();
			children.add(slots);
			existing.add(group);
			for (GalleryItem child : slots) {
				if (child != null) {
					existing.add(child);
				}
			}
		}
		int childrenBefore = existing.size() - groups.length;
		assertTrue("the paints created only the children in view: " + childrenBefore,
				childrenBefore > 0 && childrenBefore <= 2 * cellBound());
		int groupsBefore = groupSetData, itemsBefore = itemSetData, from = requested.size();

		int before = paints;
		gallery.clearAll();
		settle(before + 1);

		GalleryItem[] after = gallery.getItems();
		for (int g = 0; g < groups.length; g++) {
			assertSame("group " + g + " after clearAll", groups[g], after[g]);
			GalleryItem[] slots = after[g].getItems();
			for (int i = 0; i < slots.length; i++) {
				assertSame("child " + g + "/" + i + " after clearAll", children.get(g)[i], slots[i]);
			}
		}
		for (int k = from; k < requested.size(); k++) {
			assertTrue("SetData after clearAll named a new item: " + requested.get(k).getText(),
					existing.contains(requested.get(k)));
		}
		assertEquals("groups requested again by the layout, once each", groups.length, groupSetData - groupsBefore);
		int items = itemSetData - itemsBefore;
		assertTrue("children requested again by the paint (only those in view): " + items, items > 0 && items <= cellBound());
		assertEquals("child controls of the gallery", 0, gallery.getChildren().length);
		assertEquals("shells", shellsBefore + 1, display.getShells().length);
	}

	/**
	 * One group of 100,000 children, scrolled to the middle: the group's clearAll requests only the children in view
	 * again; the cleared children out of view stay cleared (no SetData) until they are shown, and are then requested
	 * again as the same items.
	 */
	@Test
	public void groupClearAllRequestsOnlyTheChildrenInView() {
		create(1, 100_000, SWT.MULTI, false);
		GalleryItem group = gallery.getItem(0);
		GalleryItem top = group.getItem(0);
		scrollTo(gallery.getVerticalBar().getMaximum() / 2);
		int created = created(group), items = itemSetData;

		group.clearAll();
		assertEquals("SetData sent by the group's clearAll", 0, itemSetData - items);
		assertEquals("cleared, not yet requested again", "", top.getText());
		gallery.redraw();
		flush();
		int middle = itemSetData - items;
		assertTrue("children in view requested again: " + middle, middle > 0 && middle <= cellBound());
		assertEquals("items created by the clearAll and its paint", created, created(group));
		assertEquals("the child out of view stays cleared", "", top.getText());

		items = itemSetData;
		int from = requested.size();
		scrollTo(0);
		assertTrue("the first child is requested again when shown", requested.subList(from, requested.size()).contains(top));
		assertSame(top, group.getItem(0));
		assertTrue(top.getText().startsWith("item 0 #"));
		assertTrue("children requested by the scroll back: " + (itemSetData - items), itemSetData - items <= cellBound());
	}

	/**
	 * clear(int) of a group: the group and its children stay, with their expanded state, item count and data; the
	 * group is requested again at once (by the layout, as the original requested its replacement) and its children
	 * when they are painted, each SetData naming the item already held, at its index. The other groups' children
	 * are not requested.
	 */
	@Test
	public void clearKeepsTheGroupItsChildrenAndItsState() {
		create(3, 6, SWT.MULTI, false);
		GalleryItem group = gallery.getItem(1);
		GalleryItem[] children = new GalleryItem[6];
		for (int i = 0; i < 6; i++) {
			children[i] = group.getItem(i);
			assertTrue(children[i].getText().startsWith("item " + i + " #"));
		}
		group.setData("model");
		group.setData("key", "value");
		int from = requested.size();

		gallery.clear(1);

		assertEquals("SetData during clear(1)", 1, requested.size() - from);
		assertSame(group, requested.get(from));
		assertEquals(Integer.valueOf(1), requestedIndex.get(from));
		assertSame(group, gallery.getItem(1));
		assertFalse(group.isDisposed());
		assertEquals(1, gallery.indexOf(group));
		assertTrue("the group stays expanded", group.isExpanded());
		assertEquals(6, group.getItemCount());
		assertEquals("model", group.getData());
		assertEquals("value", group.getData("key"));
		assertTrue(group.getText().startsWith("group 1 #"));
		GalleryItem[] slots = group.getItems();
		for (int i = 0; i < 6; i++) {
			assertSame(children[i], slots[i]);
			assertEquals("child " + i + " cleared, not yet requested", "", children[i].getText());
		}

		from = requested.size();
		gallery.redraw();
		flush();
		assertRequestedOnce(from, children);
		for (int i = 0; i < 6; i++) {
			assertTrue(children[i].getText().startsWith("item " + i + " #"));
		}

		// the group's own clearAll: its children only
		from = requested.size();
		group.clearAll();
		assertEquals("SetData sent by the group's clearAll", 0, requested.size() - from);
		gallery.redraw();
		flush();
		assertRequestedOnce(from, children);

		// clearAll: every group, in order, at once; then every child in view when painted
		GalleryItem[] groups = gallery.getItems();
		from = requested.size();
		gallery.clearAll();
		assertEquals(3, requested.size() - from);
		for (int g = 0; g < 3; g++) {
			assertSame(groups[g], requested.get(from + g));
			assertEquals(Integer.valueOf(g), requestedIndex.get(from + g));
			assertTrue(groups[g].isExpanded());
		}
		from = requested.size();
		gallery.redraw();
		flush();
		assertEquals("children requested again by the paint", 18, requested.size() - from);
		Set<GalleryItem> seen = identitySet();
		for (int k = from; k < requested.size(); k++) {
			GalleryItem item = requested.get(k);
			assertTrue("each child once", seen.add(item));
			assertSame(item, item.getParentItem().getItem(requestedIndex.get(k).intValue()));
		}
	}

	/* the SetData events from 'from' on are exactly one for each of the items, at its index */
	void assertRequestedOnce(int from, GalleryItem[] items) {
		assertEquals("SetData events", items.length, requested.size() - from);
		Set<GalleryItem> seen = identitySet();
		for (int k = from; k < requested.size(); k++) {
			GalleryItem item = requested.get(k);
			int index = requestedIndex.get(k).intValue();
			assertSame("SetData item at index " + index, items[index], item);
			assertTrue("requested once", seen.add(item));
		}
	}

	/**
	 * The selection survives clear(int) and a group's clearAll (same items, same order, no Selection event), keyboard
	 * and mouse keep working from the held anchor, and clearAll deselects as before.
	 */
	@Test
	public void clearKeepsTheSelectionAndSendsNoSelectionEvent() {
		create(3, 6, SWT.MULTI, false);
		GalleryItem first = gallery.getItem(0).getItem(1);
		GalleryItem second = gallery.getItem(1).getItem(2);
		click(first, SWT.NONE);
		click(second, SWT.MOD1);
		gallery.getItem(2).selectAll();
		GalleryItem[] selection = gallery.getSelection();
		assertEquals(8, selection.length);
		int events = selectionEvents.size();

		gallery.clear(1);
		flush();
		assertSelection(selection);
		gallery.getItem(2).clearAll();
		flush();
		assertSelection(selection);
		gallery.clear(0, true);
		flush();
		assertSelection(selection);
		assertEquals("Selection events sent by the clears", events, selectionEvents.size());

		// the items are still in the gallery: a click selects the item already held
		GalleryItem third = gallery.getItem(1).getItem(3);
		click(third, SWT.MOD1);
		GalleryItem[] now = gallery.getSelection();
		assertEquals(9, now.length);
		assertSame(third, now[8]);
		assertSame(third, selectionEvents.get(selectionEvents.size() - 1));
		// shift-click from the held anchor selects the range between the held items
		click(gallery.getItem(1).getItem(5), SWT.SHIFT);
		now = gallery.getSelection();
		assertEquals(3, now.length);
		for (int i = 0; i < 3; i++) {
			assertSame(gallery.getItem(1).getItem(3 + i), now[i]);
		}

		events = selectionEvents.size();
		gallery.clearAll();
		flush();
		assertEquals("clearAll deselects, as before", 0, gallery.getSelectionCount());
		assertEquals(0, gallery.getSelection().length);
		assertEquals("Selection events sent by clearAll", events, selectionEvents.size());
	}

	/**
	 * A cleared item the selection hands out (getSelection, the item of a DefaultSelection or Selection event) is
	 * requested again first, as getItem would request it: it never comes out of the selection with its cleared
	 * attributes (the original handed out an item created for the cleared slot, with its SetData).
	 */
	@Test
	public void theSelectionHandsOutClearedItemsRequestedAgain() {
		create(3, 6, SWT.MULTI, false);
		GalleryItem group = gallery.getItem(1);
		GalleryItem first = group.getItem(0);
		group.clearAll();
		gallery.selectAll();
		int from = requested.size();
		GalleryItem[] selection = gallery.getSelection();
		assertEquals(3 + 18, selection.length);
		for (GalleryItem item : selection) {
			assertFalse("a selected item handed out with its cleared text", item.getText().isEmpty());
		}
		assertEquals("the cleared children requested again by getSelection", 6, requested.size() - from);
		assertSame(first, selection[1 + 6 + 1]);
		from = requested.size();
		assertEquals(selection.length, gallery.getSelection().length);
		assertEquals("a second getSelection requests nothing", 0, requested.size() - from);

		// the item a DefaultSelection (Enter) event names
		gallery.setSelection(new GalleryItem[] { first });
		group.clearAll();
		String[] named = { null };
		gallery.addListener(SWT.DefaultSelection, e -> named[0] = ((GalleryItem) e.item).getText());
		Event enter = new Event();
		enter.keyCode = SWT.CR;
		gallery.notifyListeners(SWT.KeyDown, enter);
		assertTrue("the Enter item is requested again: " + named[0], named[0] != null && named[0].startsWith("item 0 #"));

		// the item a Selection event names when an item is deselected: the item selected last
		gallery.redraw();
		flush();
		GalleryItem second = group.getItem(1);
		click(second, SWT.MOD1);
		group.clearAll();
		int events = selectionEvents.size();
		click(second, SWT.MOD1);
		assertEquals(events + 1, selectionEvents.size());
		assertSame("deselecting names the item selected last", first, selectionEvents.get(events));
		assertTrue("the item a deselection names is requested again: " + first.getText(),
				first.getText().startsWith("item 0 #"));
	}

	/**
	 * A cleared item whose group then leaves the gallery (remove, setItemCount) is handed out by the selection as it is:
	 * no SetData under a group the gallery no longer holds, by getSelection, Enter or a deselection (the original,
	 * whose clear had already detached the group, sent none either).
	 */
	@Test
	public void aClearedGroupThatLeavesSendsNoSetDataUnderIt() {
		for (int operation = 0; operation < 2; operation++) {
			create(3, 6, SWT.MULTI, false);
			GalleryItem group = gallery.getItem(2);
			GalleryItem kept = gallery.getItem(0).getItem(1);
			// selected first and last: the items Enter and a deselection name
			GalleryItem[] selected = { group.getItem(3), kept, group.getItem(4) };
			gallery.setSelection(selected);
			int[] detached = { 0 };
			gallery.addListener(SWT.SetData, e -> {
				GalleryItem parent = ((GalleryItem) e.item).getParentItem();
				if (parent != null && gallery.indexOf(parent) < 0) {
					detached[0]++;
				}
			});
			gallery.clear(2);
			String name = operation == 0 ? "clear(2), remove(2)" : "clear(2), setItemCount(2)";
			if (operation == 0) {
				gallery.remove(2);
			} else {
				gallery.setItemCount(2);
			}
			flush();
			assertTrue(name, gallery.indexOf(group) < 0);
			GalleryItem[] selection = gallery.getSelection();
			assertEquals(name, 3, selection.length);
			for (int i = 0; i < 3; i++) {
				assertSame(name, selected[i], selection[i]);
			}
			assertEquals(name + ": SetData under the detached group by getSelection", 0, detached[0]);

			Object[] named = { null };
			gallery.addListener(SWT.DefaultSelection, e -> named[0] = e.item);
			Event enter = new Event();
			enter.keyCode = SWT.CR;
			gallery.notifyListeners(SWT.KeyDown, enter);
			assertSame(name + ": Enter names the first selected item", selected[0], named[0]);
			assertEquals(name + ": SetData under the detached group by Enter", 0, detached[0]);

			int events = selectionEvents.size();
			click(kept, SWT.MOD1);
			assertEquals(events + 1, selectionEvents.size());
			assertSame(name + ": a deselection names the item selected last", selected[2], selectionEvents.get(events));
			assertEquals(name + ": SetData under the detached group by a deselection", 0, detached[0]);
			shell.dispose();
		}
	}

	/**
	 * A setter on a cleared item populates it, as in Table and Tree: the caller's value is not overwritten by a SetData
	 * when the item is next requested. The other cleared items are requested again.
	 */
	@Test
	public void aSetterPopulatesAClearedItem() {
		create(3, 6, SWT.MULTI, false);
		GalleryItem group = gallery.getItem(1);
		GalleryItem[] children = new GalleryItem[6];
		for (int i = 0; i < 6; i++) {
			children[i] = group.getItem(i);
		}
		group.clearAll();
		children[0].setText("mine");
		int from = requested.size();
		gallery.redraw();
		flush();
		assertEquals("mine", children[0].getText());
		assertSame(children[0], group.getItem(0));
		assertEquals("mine", children[0].getText());
		assertFalse("no SetData for the item the caller populated", requested.subList(from, requested.size()).contains(children[0]));
		assertEquals("the other cleared children requested again", 5, requested.size() - from);
		for (int i = 1; i < 6; i++) {
			assertTrue(children[i].getText().startsWith("item " + i + " #"));
		}
	}

	void assertSelection(GalleryItem[] expected) {
		GalleryItem[] actual = gallery.getSelection();
		assertEquals(expected.length, gallery.getSelectionCount());
		assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertSame("selection[" + i + "]", expected[i], actual[i]);
			assertFalse(actual[i].isDisposed());
			assertTrue("selected item still in the gallery", path(gallery, actual[i]).indexOf('-') < 0);
		}
	}

	/**
	 * After a selectAll in 2 groups of 50,000 children, clear(0) keeps the group in the gallery, with its selection:
	 * the count is unchanged and nothing is created (the original replaced the group).
	 */
	@Test
	public void clearAfterSelectAllKeepsTheGroupAndCreatesNothing() {
		create(2, 50_000, SWT.MULTI, false);
		GalleryItem group = gallery.getItem(0);
		gallery.selectAll();
		int count = gallery.getSelectionCount();
		assertEquals(2 + 100_000, count);
		int created = created(group), items = itemSetData;
		gallery.clear(0);
		flush();
		assertSame(group, gallery.getItem(0));
		assertEquals(count, gallery.getSelectionCount());
		assertEquals("items created by clear(0) and its paint", created, created(group));
		assertTrue("SetData only for the children in view: " + (itemSetData - items), itemSetData - items <= cellBound());
		Event enter = new Event();
		enter.keyCode = SWT.CR;
		int before = itemSetData;
		gallery.notifyListeners(SWT.KeyDown, enter);
		assertEquals("SetData sent by Enter", 0, itemSetData - before);
	}

	/**
	 * Virtual groups: a cleared group waits, like a placeholder group, for its paint to be requested again (not for
	 * the layout), and the request names the same group.
	 */
	@Test
	public void virtualGroupsRequestAClearedGroupWhenItIsDrawn() {
		create(5, 6, SWT.MULTI, true);
		GalleryItem group = gallery.getItem(0);
		group.setExpanded(true);
		flush();
		GalleryItem child = group.getItem(0);
		int groups = groupSetData, from = requested.size();
		gallery.clear(0);
		assertEquals("SetData during clear(0)", 0, groupSetData - groups);
		gallery.redraw();
		flush();
		assertEquals("group requested again when drawn", 1, groupSetData - groups);
		assertTrue(requested.subList(from, requested.size()).contains(group));
		assertSame(group, gallery.getItem(0));
		assertSame(child, group.getItem(0));
		assertTrue(group.isExpanded());
	}

	/**
	 * Recorded side by side: the same seeded script of selections, mouse clicks, expansions and clears on a
	 * non-virtual gallery (whose clear keeps its items) and on a VIRTUAL gallery. Item identity (every held item
	 * stays at its place), the selection (paths, count, order), the Selection events, the expanded states and item
	 * counts are the same after every step. The VIRTUAL gallery also requests the cleared attributes again: every
	 * item painted shows its model text, never a stale or cleared one.
	 */
	@Test
	public void clearMatchesTheNonVirtualGallery() {
		for (int seed = 0; seed < 10; seed++) {
			differential(seed);
		}
	}

	void differential(int seed) {
		Random random = new Random(0xC1EA2L + seed);
		int groups = 1 + random.nextInt(4);
		int[] sizes = new int[groups];
		for (int g = 0; g < groups; g++) {
			sizes[g] = random.nextInt(40);
		}
		int style = SWT.V_SCROLL | (seed % 4 == 3 ? SWT.SINGLE : SWT.MULTI);
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 300);
		shell.setLayout(new FillLayout());
		Gallery serial = new Gallery(shell, style);
		Gallery lazy = new Gallery(shell, style | SWT.VIRTUAL);
		lazy.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setItemCount(sizes[e.index]);
				item.setText("g" + e.index);
			} else {
				item.setText("i" + lazy.indexOf(item.getParentItem()) + "." + e.index);
			}
		});
		List<String> serialEvents = new ArrayList<>(), lazyEvents = new ArrayList<>();
		for (int type : new int[] { SWT.Selection, SWT.DefaultSelection }) {
			serial.addListener(type, e -> serialEvents.add(e.type + ":" + path(serial, (GalleryItem) e.item)));
			lazy.addListener(type, e -> lazyEvents.add(e.type + ":" + path(lazy, (GalleryItem) e.item)));
		}
		for (int g = 0; g < groups; g++) {
			GalleryItem group = new GalleryItem(serial, SWT.NONE);
			group.setText("g" + g);
			group.setExpanded(true);
			for (int i = 0; i < sizes[g]; i++) {
				new GalleryItem(group, SWT.NONE).setText("i" + g + "." + i);
			}
		}
		lazy.setItemCount(groups);
		for (int g = 0; g < groups; g++) {
			lazy.getItem(g).setExpanded(true);
		}
		shell.open();
		flush(lazy);
		// held items: serial and VIRTUAL item obtained at the same path at some step
		List<GalleryItem[]> held = new ArrayList<>();
		List<String> log = new ArrayList<>();
		for (int step = 0; step < 100; step++) {
			int a = random.nextInt(1 << 16);
			int op = random.nextInt(14);
			int g = a % groups;
			int children = sizes[g];
			int i = children == 0 ? -1 : (a / 7) % children;
			Consumer<Gallery> operation = null;
			switch (op) {
			case 0:
				log.add("selectAll");
				operation = Gallery::selectAll;
				break;
			case 1:
				log.add("group " + g + " selectAll");
				operation = gallery -> gallery.getItem(g).selectAll();
				break;
			case 2:
				if (i >= 0) {
					log.add("setSelection " + g + "/" + i);
					operation = gallery -> gallery.setSelection(new GalleryItem[] { gallery.getItem(g).getItem(i) });
				}
				break;
			case 3:
			case 4: {
				int x = 5 + a % 300, y = 5 + (a / 300) % 290;
				int mask = new int[] { SWT.NONE, SWT.NONE, SWT.SHIFT, SWT.MOD1 }[(a >> 3) & 3];
				log.add("click " + x + "," + y + " mask " + mask);
				operation = gallery -> GalleryVirtualSelectionTest.mouse(gallery, x, y, mask);
				break;
			}
			case 5:
				log.add("clear " + g);
				operation = gallery -> gallery.clear(g);
				break;
			case 6:
				log.add("clear " + g + " all");
				operation = gallery -> gallery.clear(g, true);
				break;
			case 7:
				if (step % 3 == 0) {
					log.add("clearAll");
					operation = Gallery::clearAll;
				}
				break;
			case 8:
				if (step % 3 == 0) {
					log.add("clearAll all");
					operation = gallery -> gallery.clearAll(true);
				}
				break;
			case 9:
				log.add("group " + g + " clearAll");
				operation = gallery -> gallery.getItem(g).clearAll();
				break;
			case 10:
				log.add("group " + g + " clearAll all");
				operation = gallery -> gallery.getItem(g).clearAll(true);
				break;
			case 11:
				log.add("toggle " + g);
				operation = gallery -> gallery.getItem(g).setExpanded(!gallery.getItem(g).isExpanded());
				break;
			case 12:
				if (i >= 0) {
					log.add("hold " + g + "/" + i);
					held.add(new GalleryItem[] { serial.getItem(g).getItem(i), lazy.getItem(g).getItem(i) });
				} else {
					log.add("hold " + g);
					held.add(new GalleryItem[] { serial.getItem(g), lazy.getItem(g) });
				}
				break;
			default:
				log.add("deselectAll");
				operation = Gallery::deselectAll;
			}
			if (operation != null) {
				Consumer<Gallery> o = operation;
				String threw = GalleryVirtualSelectionTest.attempt(() -> o.accept(serial));
				assertEquals("seed " + seed + ": " + log, threw, GalleryVirtualSelectionTest.attempt(() -> o.accept(lazy)));
			}
			if (op >= 5 && op <= 10) {
				// the paint requests the cleared items in view again
				flush(lazy);
			}
			String where = "seed " + seed + ": " + log;
			assertEquals(where, serialEvents, lazyEvents);
			assertEquals(where, serial.getSelectionCount(), lazy.getSelectionCount());
			assertEquals(where, GalleryVirtualSelectionTest.paths(serial, false), GalleryVirtualSelectionTest.paths(lazy, false));
			for (GalleryItem item : lazy.getSelection()) {
				assertFalse(where + " a selected item handed out with its cleared text", item.getText().isEmpty());
			}
			for (int k = 0; k < groups; k++) {
				assertEquals(where + " expanded " + k, serial.getItem(k).isExpanded(), lazy.getItem(k).isExpanded());
				assertEquals(where + " count " + k, serial.getItem(k).getItemCount(), lazy.getItem(k).getItemCount());
			}
			for (GalleryItem[] pair : held) {
				assertEquals(where + " held item", path(serial, pair[0]), path(lazy, pair[1]));
			}
			assertNoStaleText(lazy, where);
		}
		shell.dispose();
	}

	/* every item of the VIRTUAL gallery requested since its last clear shows its model text */
	static void assertNoStaleText(Gallery lazy, String where) {
		for (GalleryItem group : lazy.getItems()) {
			if (group == null) {
				continue;
			}
			int g = lazy.indexOf(group);
			String text = group.getText();
			assertTrue(where + " group " + g + " text " + text, text.isEmpty() || text.equals("g" + g));
			GalleryItem[] slots = group.getItems();
			for (int i = 0; i < slots.length; i++) {
				if (slots[i] != null) {
					String child = slots[i].getText();
					assertTrue(where + " child " + g + "/" + i + " text " + child,
							child.isEmpty() || child.equals("i" + g + "." + i));
				}
			}
		}
	}

	/* the group/item index path of an item, "-" for an item no longer in the gallery */
	static String path(Gallery gallery, GalleryItem item) {
		String path = GalleryVirtualSelectionTest.path(gallery, item);
		return "stale".equals(path) ? "-" : path;
	}

	static Set<GalleryItem> identitySet() {
		return Collections.newSetFromMap(new IdentityHashMap<>());
	}

	/* the cells a 640x480 client can show (72x56 items, 2px margins), plus a partly shown row above and below */
	int cellBound() {
		Rectangle area = gallery.getClientArea();
		return (area.width / 72 + 1) * (area.height / 56 + 2);
	}

	static int created(GalleryItem group) {
		int n = 0;
		for (GalleryItem item : group.getItems()) {
			if (item != null) {
				n++;
			}
		}
		return n;
	}

	void click(GalleryItem item, int stateMask) {
		Rectangle bounds = item.getBounds();
		int x = bounds.x + bounds.width / 2, y = bounds.y + bounds.height / 2;
		assertTrue("the item is in view: " + bounds, gallery.getClientArea().contains(x, y));
		assertSame(item, gallery.getItem(new Point(x, y)));
		GalleryVirtualSelectionTest.mouse(gallery, x, y, stateMask);
	}

	/* moves the scroll bar as a user drag does, and waits for the paint */
	void scrollTo(int position) {
		ScrollBar bar = gallery.getVerticalBar();
		int before = paints;
		bar.setSelection(Math.min(position, bar.getMaximum() - bar.getThumb()));
		bar.notifyListeners(SWT.Selection, new Event());
		gallery.redraw();
		settle(before + 1);
	}

	void flush() {
		settle(0);
	}

	void flush(Gallery target) {
		target.redraw();
		target.update();
		while (display.readAndDispatch()) {
			// dispatch everything queued
		}
	}

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
