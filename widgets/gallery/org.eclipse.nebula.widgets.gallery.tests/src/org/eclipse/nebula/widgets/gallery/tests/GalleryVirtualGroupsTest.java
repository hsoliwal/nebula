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
import org.eclipse.swt.graphics.GC;
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
 * Virtual groups (<code>setVirtualGroups(true)</code>) in a SWT.VIRTUAL Gallery: a group out of view is a placeholder
 * until it is drawn. The layout lays out each placeholder once per pass (the original started one re-entrant layout of
 * every group per placeholder it created: quadratic, and a StackOverflowError from about 3,000 groups), and an
 * initialized placeholder is the group itself (the original replaced it with a new GalleryItem, detaching the one
 * <code>getItems()</code> had returned). Viewport mode (the default) only: the original mode is stock.
 */
public class GalleryVirtualGroupsTest {

	Display display;
	Shell shell;
	Gallery gallery;
	CountingRenderer renderer;
	int groupSize;
	boolean expandInSetData = true;

	int groupSetData, itemSetData, paints;
	final List<GalleryItem> requestedGroups = new ArrayList<>();
	final List<Integer> requestedIndex = new ArrayList<>();

	/** The default group renderer, counting its group layouts and its layout passes, and how deep passes nest. */
	static class CountingRenderer extends DefaultGalleryGroupRenderer {
		int layouts, passes, depth, maxDepth;

		@Override
		public void preLayout(GC gc) {
			passes++;
			maxDepth = Math.max(maxDepth, ++depth);
			super.preLayout(gc);
		}

		@Override
		public void postLayout(GC gc) {
			depth--;
			super.postLayout(gc);
		}

		@Override
		public void layout(GC gc, GalleryItem group) {
			layouts++;
			super.layout(gc, group);
		}
	}

	@Before
	public void setUp() {
		Assume.assumeFalse("viewport mode only (the original mode is stock)", GalleryVirtualSelectionTest.originalMode());
		display = Display.getDefault();
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	/* a VIRTUAL gallery with virtual groups, groups of 'size' children expanded by their SetData; not yet populated */
	void create(int size) {
		groupSize = size;
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		gallery = new Gallery(shell, SWT.V_SCROLL | SWT.VIRTUAL | SWT.MULTI);
		renderer = new CountingRenderer();
		renderer.setMinMargin(2);
		renderer.setItemHeight(56);
		renderer.setItemWidth(72);
		renderer.setAutoMargin(true);
		gallery.setGroupRenderer(renderer);
		gallery.setItemRenderer(new DefaultGalleryItemRenderer());
		gallery.setVirtualGroups(true);
		gallery.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				groupSetData++;
				requestedGroups.add(item);
				requestedIndex.add(Integer.valueOf(e.index));
				item.setText("group " + e.index);
				item.setItemCount(groupSize);
				if (expandInSetData) {
					item.setExpanded(true);
				}
			} else {
				itemSetData++;
				item.setText("item " + e.index);
			}
		});
		gallery.addListener(SWT.Paint, e -> paints++);
		shell.layout(true, true);
	}

	void open() {
		int before = paints;
		shell.open();
		settle(before + 1);
	}

	/**
	 * 1,000 placeholder groups: setItemCount lays them out in one pass (two when its scroll bar appears), each once per
	 * pass. The original started a layout pass for each placeholder it created, inside the pass that created it: 1,002
	 * passes nested 1,000 deep, each going over every group (quadratic), and a StackOverflowError from about 3,000
	 * groups. The paint requests only the groups drawn.
	 */
	@Test
	public void placeholdersAreLaidOutOncePerPass() {
		create(1000);
		int before = renderer.layouts, passes = renderer.passes;
		renderer.maxDepth = 0;
		gallery.setItemCount(1000);
		int layouts = renderer.layouts - before;
		// one pass, and a second when the scroll bar it shows resizes the client area
		assertTrue("layout passes for setItemCount(1000): " + (renderer.passes - passes), renderer.passes - passes <= 2);
		assertEquals("nested layout passes", 1, renderer.maxDepth);
		assertTrue("group layouts for 1,000 placeholders: " + layouts, layouts <= 2 * 1000);
		assertEquals("SetData before the first paint", 0, groupSetData + itemSetData);
		GalleryItem[] groups = gallery.getItems();
		for (GalleryItem group : groups) {
			assertTrue("a placeholder for every group (they keep the item count of a placeholder)",
					group != null && group.getItemCount() == gallery.getVirtualGroupDefaultItemCount());
		}
		open();
		assertTrue("groups requested by the first paint: " + groupSetData, groupSetData > 0 && groupSetData <= 8);
		assertTrue("children requested by the first paint: " + itemSetData, itemSetData <= 2 * cellBound());
	}

	/**
	 * 20,000 placeholder groups: setItemCount, the first paint and a jump to the end complete (the original nested one
	 * layout per placeholder and overflowed the stack from about 3,000 groups).
	 */
	@Test
	public void twentyThousandPlaceholderGroups() {
		create(100);
		int before = renderer.layouts;
		gallery.setItemCount(20_000);
		assertTrue("group layouts: " + (renderer.layouts - before), renderer.layouts - before <= 2 * 20_000);
		open();
		scrollTo(Integer.MAX_VALUE);
		assertTrue("groups requested: " + groupSetData, groupSetData <= 16);
		assertTrue("children requested: " + itemSetData, itemSetData <= 4 * cellBound());
	}

	/**
	 * The placeholder that getItems() returns is the group once it is initialized, by its paint or by getItem: the
	 * SetData event names it, at its index, and the gallery keeps answering it (the original created a new item and
	 * left the placeholder detached and undisposed).
	 */
	@Test
	public void aPlaceholderBecomesTheGroup() {
		create(1000);
		gallery.setItemCount(200);
		GalleryItem[] placeholders = gallery.getItems();
		open();
		assertTrue(groupSetData > 0);
		for (int k = 0; k < requestedGroups.size(); k++) {
			int index = requestedIndex.get(k).intValue();
			assertSame("SetData for group " + index, placeholders[index], requestedGroups.get(k));
			assertSame(placeholders[index], gallery.getItem(index));
			assertEquals(index, gallery.indexOf(placeholders[index]));
			assertTrue(placeholders[index].isExpanded());
			assertEquals(1000, placeholders[index].getItemCount());
		}
		int drawn = requestedGroups.size();
		scrollTo(Integer.MAX_VALUE);
		assertTrue("the paint at the end initialized more groups", requestedGroups.size() > drawn);
		for (int k = drawn; k < requestedGroups.size(); k++) {
			int index = requestedIndex.get(k).intValue();
			assertSame("SetData for group " + index, placeholders[index], requestedGroups.get(k));
		}

		// initialized by the API, out of view
		int from = requestedGroups.size();
		GalleryItem group = gallery.getItem(100);
		assertSame(placeholders[100], group);
		assertEquals(from + 1, requestedGroups.size());
		assertSame(group, requestedGroups.get(from));
		assertFalse(group.isDisposed());
		assertEquals(100, gallery.indexOf(group));
		assertEquals("group 100", group.getText());
		GalleryItem[] after = gallery.getItems();
		for (int g = 0; g < after.length; g++) {
			assertSame("slot " + g, placeholders[g], after[g]);
		}
	}

	/**
	 * A placeholder initialized in place takes the state of the new group the original created: no children until its
	 * SetData sets them, collapsed unless its SetData expands it, default attributes.
	 */
	@Test
	public void anInitializedPlaceholderStartsAsANewGroup() {
		expandInSetData = false;
		create(0);
		gallery.setItemCount(50);
		GalleryItem placeholder = gallery.getItems()[40];
		assertTrue("a placeholder is expanded", placeholder.isExpanded());
		assertEquals(gallery.getVirtualGroupDefaultItemCount(), placeholder.getItemCount());
		int[] state = { -1, -1 };
		gallery.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item == placeholder) {
				// the listener added first set the text and an item count of 0; this one records what it then sees
				state[0] = item.isExpanded() ? 1 : 0;
				state[1] = item.getItems().length;
			}
		});
		assertSame(placeholder, gallery.getItem(40));
		assertEquals("collapsed, as a new group", 0, state[0]);
		assertEquals("no children, as a new group", 0, state[1]);
		assertFalse(placeholder.isExpanded());
		assertEquals(0, placeholder.getItemCount());
		assertEquals("group 40", placeholder.getText());
	}

	/**
	 * Recorded side by side on small galleries whose groups are all in view: the same seeded script of selections,
	 * clicks, keys and expansions on a VIRTUAL gallery with virtual groups and on one without. Once their groups are
	 * initialized, the selection (paths, count, order), the Selection and Expand events, the expanded states, the
	 * group bounds and the held items are the same after every step.
	 */
	@Test
	public void smallVirtualGroupsGalleryMatchesTheVirtualGallery() {
		for (int seed = 0; seed < 10; seed++) {
			differential(seed);
		}
	}

	void differential(int seed) {
		Random random = new Random(0x6A110E5L + seed);
		int groups = 1 + random.nextInt(3);
		int[] sizes = new int[groups];
		for (int g = 0; g < groups; g++) {
			sizes[g] = random.nextInt(9);
		}
		int style = SWT.V_SCROLL | SWT.VIRTUAL | (seed % 4 == 3 ? SWT.SINGLE : SWT.MULTI);
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		Gallery plain = new Gallery(shell, style);
		Gallery lazy = new Gallery(shell, style);
		lazy.setVirtualGroups(true);
		lazy.setVirtualGroupDefaultItemCount(4);
		int[] lazyGroups = { 0 };
		for (Gallery each : new Gallery[] { plain, lazy }) {
			each.addListener(SWT.SetData, e -> {
				GalleryItem item = (GalleryItem) e.item;
				if (item.getParentItem() == null) {
					if (each == lazy) {
						lazyGroups[0]++;
					}
					item.setItemCount(sizes[e.index]);
					item.setExpanded(true);
				}
			});
		}
		List<String> plainEvents = new ArrayList<>(), lazyEvents = new ArrayList<>();
		for (int type : new int[] { SWT.Selection, SWT.DefaultSelection, SWT.Expand }) {
			plain.addListener(type, e -> plainEvents.add(e.type + ":" + GalleryVirtualSelectionTest.path(plain, (GalleryItem) e.item)));
			lazy.addListener(type, e -> lazyEvents.add(e.type + ":" + GalleryVirtualSelectionTest.path(lazy, (GalleryItem) e.item)));
		}
		plain.setItemCount(groups);
		lazy.setItemCount(groups);
		gallery = lazy;
		lazy.addListener(SWT.Paint, e -> paints++);
		int before = paints;
		shell.open();
		settle(before + 1);
		assertEquals("seed " + seed + ": every group of the virtual-groups gallery is initialized", groups, lazyGroups[0]);
		List<GalleryItem[]> held = new ArrayList<>();
		List<String> log = new ArrayList<>();
		for (int step = 0; step < 100; step++) {
			int a = random.nextInt(1 << 16);
			int op = random.nextInt(11);
			int g = a % groups;
			int i = sizes[g] == 0 ? -1 : (a / 7) % sizes[g];
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
				int x = 5 + a % 300, y = 5 + (a / 300) % 460;
				int mask = new int[] { SWT.NONE, SWT.NONE, SWT.SHIFT, SWT.MOD1 }[(a >> 3) & 3];
				log.add("click " + x + "," + y + " mask " + mask);
				operation = gallery -> GalleryVirtualSelectionTest.mouse(gallery, x, y, mask);
				break;
			}
			case 5:
			case 6: {
				int key = new int[] { SWT.ARROW_LEFT, SWT.ARROW_RIGHT, SWT.ARROW_UP, SWT.ARROW_DOWN, SWT.PAGE_UP,
						SWT.PAGE_DOWN, SWT.HOME, SWT.END, SWT.CR }[a % 9];
				log.add("key " + key);
				operation = gallery -> {
					Event event = new Event();
					event.keyCode = key;
					gallery.notifyListeners(SWT.KeyDown, event);
				};
				break;
			}
			case 7:
				log.add("toggle " + g);
				operation = gallery -> gallery.getItem(g).setExpanded(!gallery.getItem(g).isExpanded());
				break;
			case 8:
				if (i >= 0) {
					log.add("hold " + g + "/" + i);
					held.add(new GalleryItem[] { plain.getItem(g).getItem(i), lazy.getItem(g).getItem(i) });
				} else {
					log.add("hold " + g);
					held.add(new GalleryItem[] { plain.getItem(g), lazy.getItem(g) });
				}
				break;
			default:
				log.add("deselectAll");
				operation = Gallery::deselectAll;
			}
			if (operation != null) {
				Consumer<Gallery> o = operation;
				String threw = GalleryVirtualSelectionTest.attempt(() -> o.accept(plain));
				assertEquals("seed " + seed + ": " + log, threw, GalleryVirtualSelectionTest.attempt(() -> o.accept(lazy)));
			}
			String where = "seed " + seed + ": " + log;
			assertEquals(where, plainEvents, lazyEvents);
			assertEquals(where, plain.getSelectionCount(), lazy.getSelectionCount());
			assertEquals(where, GalleryVirtualSelectionTest.paths(plain, false), GalleryVirtualSelectionTest.paths(lazy, false));
			for (int k = 0; k < groups; k++) {
				assertEquals(where + " expanded " + k, plain.getItem(k).isExpanded(), lazy.getItem(k).isExpanded());
				assertEquals(where + " bounds " + k, plain.getItem(k).getBounds(), lazy.getItem(k).getBounds());
			}
			for (GalleryItem[] pair : held) {
				assertEquals(where + " held item", GalleryVirtualSelectionTest.path(plain, pair[0]),
						GalleryVirtualSelectionTest.path(lazy, pair[1]));
			}
		}
		shell.dispose();
	}

	/* the cells a 640x480 client can show (72x56 items, 2px margins), plus a partly shown row above and below */
	int cellBound() {
		Rectangle area = gallery.getClientArea();
		return (area.width / 72 + 1) * (area.height / 56 + 2);
	}

	/* moves the scroll bar as a user drag does, and waits for the paints */
	void scrollTo(int position) {
		ScrollBar bar = gallery.getVerticalBar();
		int before = paints;
		bar.setSelection(Math.min(position, bar.getMaximum() - bar.getThumb()));
		bar.notifyListeners(SWT.Selection, new Event());
		gallery.redraw();
		settle(before + 1);
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
