/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Resident retirement, RTL coordinates and protected extension contracts. */
public class CollectionResidentRetirement_Test {
	private static CollectionModel[] owners() {
		return new CollectionModel[] { new SwipeActionsModel(), new PullToRefreshModel(),
				new InfiniteScrollModel(), new ReorderDragModel(), new MessageListModel(),
				new CommandPaletteModel(), new MasonryModel(),
				new CollectionModel("gallery", 0, CollectionModel.GALLERY, 0, 0),
				new CollectionModel("rows", 0, CollectionModel.INFINITE, 0, 0) };
	}

	@Test
	public void scrollRetiresThePreviousWindowAcrossEveryBuiltInOwner() {
		for (CollectionModel model : owners()) {
			model.setLogicalCount(10_000);
			model.layout(320, 160);
			assertTrue(model.itemRect(0, 2) > 0);
			model.scrollTo(20_000);
			assertEquals(model.concept(), 0, model.itemRect(0, 2));
			assertEquals(model.concept(), 0, model.itemRect(0, 3));
			int old = model.itemAt(model.window(2));
			model.scrollTo(0);
			assertEquals(model.concept(), 0, model.itemRect(old, 2));
			assertEquals(model.concept(), 0, model.itemRect(old, 3));
			assertTrue(model.itemRect(0, 2) > 0);
			model.layout(320, 0);
			assertEquals(0, model.residentCount());
			assertEquals(0, model.itemRect(0, 2));
			model.layout(320, 160);
			assertTrue(model.itemRect(0, 2) > 0);
		}
	}

	@Test
	public void rtlAndReturnToLtrPreserveEveryRetiredCoordinate() {
		for (CollectionModel model : owners()) {
			model.setLogicalCount(80);
			model.layout(320, 160);
			model.setRtl(true);
			assertEquals(320, model.itemRect(79, 0));
			model.scrollTo(model.itemExtent());
			assertEquals(320, model.itemRect(79, 0));
			model.setRtl(false);
			assertEquals(0, model.itemRect(79, 0));
			model.scrollTo(0);
			assertEquals(0, model.itemRect(79, 0));
			assertEquals(0, model.itemRect(79, 2));
		}
	}

	@Test
	public void filterReconcileRemoveAndRefillDoNotRetainRetiredRects() {
		for (CollectionModel model : owners()) {
			String[] keys = new String[80], labels = new String[80];
			for (int i = 0; i < 80; i++) {
				keys[i] = "key" + i;
				labels[i] = i % 3 == 0 ? "keep" : "drop";
			}
			model.reconcile(keys, labels);
			model.layout(320, 160);
			model.scrollTo(model.logicalExtent());
			model.setFilter("keep");
			assertEquals(0, model.itemRect(79, 2));
			model.reconcile(new String[] { "key79", "key0" }, new String[] { "keep last", "keep first" });
			assertEquals(2, model.count());
			assertTrue(model.itemRect(0, 2) > 0);
			model.remove(0);
			assertEquals("keep first", model.text(0));
			model.clear();
			model.setLogicalCount(80);
			model.scrollTo(0);
			assertEquals(0, model.itemRect(79, 2));
			assertEquals(0, model.itemRect(79, 3));
		}
	}

	private static final class Extension extends CollectionModel {
		final List<Integer> resets = new ArrayList<>();
		Extension() { super("extension", 0, INFINITE, 0, 0); }
		@Override protected void setRect(int item, int x, int y, int w, int h) {
			if (w == 0 && h == 0) resets.add(item);
			super.setRect(item, x, y, w, h);
		}
		@Override protected void doLayout() {
			super.doLayout();
			if (count > 0) setRect(count - 1, 10, 10, 40, 20);
		}
	}

	private static final class LeafExtension extends InfiniteScrollModel {
		@Override protected void doLayout() {
			super.doLayout();
			if (count > 0) setRect(count - 1, 10, 10, 40, 20);
		}
	}

	@Test
	public void externalSubclassKeepsEveryResetHookAndOffWindowDecoration() {
		Extension model = new Extension();
		model.setLogicalCount(100);
		model.layout(320, 160);
		model.resets.clear();
		model.scrollTo(80);
		List<Integer> expected = new ArrayList<>();
		for (int i = 0; i < 100; i++) expected.add(i);
		assertEquals(expected, model.resets);
		assertDecoration(model);
		LeafExtension leaf = new LeafExtension();
		leaf.setLogicalCount(100);
		leaf.layout(320, 160);
		leaf.scrollTo(80);
		assertDecoration(leaf);
	}

	private static void assertDecoration(CollectionModel model) {
		WebPaint paint = new WebPaint();
		model.paint(paint);
		boolean found = false;
		for (int i = 0; i < paint.count(); i++) {
			if (paint.get(i, WebPaint.F_OP) == WebPaint.TEXT
					&& "100".equals(paint.text(paint.get(i, WebPaint.F_REF)))) found = true;
		}
		assertTrue("subclass decoration remains in the display list", found);
	}
}
