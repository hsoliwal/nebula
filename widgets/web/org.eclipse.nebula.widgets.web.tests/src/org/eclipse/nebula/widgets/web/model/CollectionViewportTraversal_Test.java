/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.web.model;

import static org.eclipse.nebula.widgets.web.model.WebStates.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

/**
 * Differential public-model contracts for resident traversal. The reference is an
 * unrecognized subclass, which retains the original complete traversal and layout.
 */
public class CollectionViewportTraversal_Test {

	private static CollectionModel actual(int mode) {
		return switch (mode) {
		case CollectionModel.KANBAN -> new KanbanBoardModel();
		case CollectionModel.SWIPE -> new SwipeActionsModel();
		case CollectionModel.PULL -> new PullToRefreshModel();
		case CollectionModel.INFINITE -> new InfiniteScrollModel();
		case CollectionModel.REORDER -> new ReorderDragModel();
		case CollectionModel.MESSAGES -> new MessageListModel();
		case CollectionModel.PALETTE -> new CommandPaletteModel();
		case CollectionModel.GALLERY -> new CollectionModel("gallery", role("grid"), mode, ROUND, V_DEFAULT);
		case CollectionModel.MASONRY -> new MasonryModel();
		default -> throw new AssertionError("unknown collection mode " + mode);
		};
	}

	private static int shape(int mode) {
		return mode == CollectionModel.SWIPE || mode == CollectionModel.PULL
				|| mode == CollectionModel.INFINITE ? RECT : ROUND;
	}

	private static final class Reference extends CollectionModel {
		Reference(CollectionModel model, int mode) {
			super(model.concept(), model.role(), mode, shape(mode), model.variant());
		}
	}

	private static void both(CollectionModel a, CollectionModel b, Consumer<CollectionModel> action) {
		action.accept(a);
		action.accept(b);
	}

	private static void populate(CollectionModel model, int n) {
		String[] keys = new String[n], labels = new String[n];
		short[] masks = new short[n], props = new short[n];
		for (int i = 0; i < n; i++) {
			keys[i] = "key-" + i;
			labels[i] = "row-" + i + (i % 3 == 1 ? " drop" : " keep");
			masks[i] = (short) (HIDDEN | DISABLED | SELECTED);
			props[i] = (short) ((i % 17 == 0 ? HIDDEN : 0)
					| (i % 19 == 0 ? DISABLED : 0) | (i % 23 == 0 ? SELECTED : 0));
		}
		model.reconcileProps(keys, labels, masks, props);
		for (int i = 0; i < Math.min(n, 36); i++) {
			model.setGroup(i, i % 3);
			model.setValue(i, 25 + 29 * (i % 7));
			model.setAux(i, -(i % 4) * 11);
		}
		model.layout(896, 192);
	}

	/** Compare resolved text, every record field and all polyline coordinates. */
	private static List<String> transcript(WebPaint paint) {
		List<String> result = new ArrayList<>();
		for (int r = 0; r < paint.count(); r++) {
			StringBuilder line = new StringBuilder();
			for (int field = 0; field < WebPaint.F_REF; field++) {
				line.append(paint.get(r, field)).append(',');
			}
			int op = paint.get(r, WebPaint.F_OP), ref = paint.get(r, WebPaint.F_REF);
			if (op == WebPaint.TEXT) {
				String value = paint.text(ref);
				line.append(value.length()).append(':').append(value);
			} else if (op == WebPaint.POLY) {
				for (int p = 0; p < 2 * paint.get(r, WebPaint.F_ARG); p++) {
					line.append(paint.point(ref + p)).append(',');
				}
			} else {
				line.append(ref);
			}
			result.add(line.toString());
		}
		return result;
	}

	private static WebPaint paint(CollectionModel model) {
		WebPaint paint = new WebPaint();
		paint.rect(-10, -10, 1, 1, 0x123456);
		model.paint(paint);
		return paint;
	}

	private static void equivalent(CollectionModel actual, CollectionModel reference, String where) {
		assertEquals(where + " logical extent", reference.logicalExtent(), actual.logicalExtent());
		assertEquals(where + " logical count", reference.logicalCount(), actual.logicalCount());
		assertArrayEquals(where + " all native lanes and rectangles", reference.snapshot(), actual.snapshot());
		assertEquals(where + " display list", transcript(paint(reference)), transcript(paint(actual)));
		for (int x : new int[] {-1, 0, 1, 8, 17, 111, 223, 447, 895, 896, 1000}) {
			for (int y : new int[] {-200, -1, 0, 1, 31, 32, 63, 95, 191, 192, 300}) {
				assertEquals(where + " hit " + x + "," + y, reference.hitTest(x, y), actual.hitTest(x, y));
			}
		}
		for (int item = 0; item < reference.count(); item++) {
			int width = reference.itemRect(item, 2), height = reference.itemRect(item, 3);
			if (width > 0 && height > 0) {
				int x = reference.itemRect(item, 0) + width / 2;
				int y = reference.itemRect(item, 1) + height / 2;
				assertEquals(where + " resident center " + item,
						reference.hitTest(x, y), actual.hitTest(x, y));
			}
		}
	}

	@Test
	public void allNineOwnersPreserveFullTraversalAcrossStateAndGeometryChanges() {
		for (int mode = CollectionModel.KANBAN; mode <= CollectionModel.MASONRY; mode++) {
			CollectionModel a = actual(mode), b = new Reference(a, mode);
			both(a, b, m -> populate(m, 79));
			both(a, b, m -> m.setFocus(2));
			both(a, b, m -> m.setState(-1, FOCUS, true));
			equivalent(a, b, "mode " + mode + " initial");
			for (int pass = 0; pass < 4; pass++) {
				boolean rtl = (pass & 1) != 0;
				both(a, b, m -> m.setRtl(rtl));
				both(a, b, m -> m.setFilter(rtl ? "keep" : ""));
				for (long offset : new long[] {0, 1, 41, 337, Long.MAX_VALUE, 93, 0}) {
					both(a, b, m -> m.scrollTo(offset));
					equivalent(a, b, "mode " + mode + " pass " + pass + " at " + offset);
				}
				int extent = 43 + 13 * pass;
				both(a, b, m -> m.setItemExtent(extent));
				both(a, b, m -> m.setValue(4, 231));
				both(a, b, m -> m.setGroup(5, 3));
				both(a, b, m -> m.setState(7, HIDDEN, rtl));
				both(a, b, m -> m.layout(rtl ? 337 : 896, rtl ? 131 : 192));
				equivalent(a, b, "mode " + mode + " mutated " + pass);
			}
		}
	}

	@Test
	public void allNineOwnersPreserveEmptyZeroGeometryAndGrowingSnapshots() {
		for (int mode = CollectionModel.KANBAN; mode <= CollectionModel.MASONRY; mode++) {
			CollectionModel a = actual(mode), b = new Reference(a, mode);
			both(a, b, m -> populate(m, 9));
			both(a, b, m -> m.setRtl(true));
			for (int[] size : new int[][] {{0, 192}, {896, 0}, {1, 1}, {896, 192}}) {
				both(a, b, m -> m.layout(size[0], size[1]));
				both(a, b, m -> m.scrollTo(Long.MAX_VALUE));
				equivalent(a, b, "mode " + mode + " size " + size[0] + "," + size[1]);
			}
			both(a, b, m -> m.reconcile(new String[0], new String[0]));
			equivalent(a, b, "mode " + mode + " empty snapshot");
			both(a, b, m -> populate(m, 111));
			both(a, b, m -> m.scrollTo(Long.MAX_VALUE));
			equivalent(a, b, "mode " + mode + " regrown snapshot");
			both(a, b, m -> m.setLogicalCount(3));
			equivalent(a, b, "mode " + mode + " shrunk logical count");
		}
	}

	@Test
	public void hundredThousandRowsPreserveDeepScrollRectanglesPaintAndHits() {
		for (int mode = CollectionModel.KANBAN; mode <= CollectionModel.MASONRY; mode++) {
			CollectionModel a = actual(mode), b = new Reference(a, mode);
			both(a, b, m -> populate(m, 100_000));
			for (boolean rtl : new boolean[] {false, true}) {
				both(a, b, m -> m.setRtl(rtl));
				for (long offset : new long[] {900_000L, 900_041L, 900_013L, Long.MAX_VALUE, 1L}) {
					both(a, b, m -> m.scrollTo(offset));
					equivalent(a, b, "large mode " + mode + " rtl " + rtl + " at " + offset);
					assertTrue("resident output remains small", paint(a).count() < 600);
				}
			}
			both(a, b, m -> m.setFilter("keep"));
			both(a, b, m -> m.scrollTo(700_000L));
			equivalent(a, b, "large mode " + mode + " filtered");
		}
	}

	private static final class CustomRows extends CollectionModel {
		CustomRows() {
			super("custom rows", WebStates.role("list"), INFINITE, RECT, V_DEFAULT);
		}

		@Override
		protected void doLayout() {
			super.doLayout();
			if (count() > 0) setRect(count() - 1, 12, 18, 60, 20);
		}
	}

	private static final class CustomMasonry extends MasonryModel {
		@Override
		protected void doLayout() {
			super.doLayout();
			if (count() > 0) setRect(count() - 1, 12, 18, 60, 20);
		}
	}

	@Test
	public void unknownSubclassesRetainNonresidentRectanglesAndReverseHitOrder() {
		for (CollectionModel model : new CollectionModel[] {new CustomRows(), new CustomMasonry()}) {
			model.setLogicalCount(100);
			model.setText(99, "custom nonresident");
			model.layout(896, 192);
			assertTrue(model.window(3) < 99);
			assertEquals(99, model.hitTest(13, 19));
			assertTrue(transcript(paint(model)).stream().anyMatch(s -> s.contains("custom nonresident")));
			model.scrollTo(41);
			assertEquals(99, model.hitTest(13, 19));
			model.setRtl(true);
			assertEquals(99, model.hitTest(825, 19));
			assertTrue(transcript(paint(model)).stream().anyMatch(s -> s.contains("custom nonresident")));
		}
	}

	private static ReorderDragModel dragging() {
		ReorderDragModel model = new ReorderDragModel();
		model.reconcile(new String[] {"a", "b", "c"}, new String[] {"A", "B", "C"});
		model.layout(320, 160);
		model.pointer(P_DOWN, 10, 10, 0);
		model.pointer(P_DRAG, 12, 12, 0);
		assertTrue(model.is(0, DRAGGING));
		return model;
	}

	@Test
	public void keyedReorderKeepsTheNativeDragAttachedToItsKey() {
		ReorderDragModel model = dragging();
		List<Integer> deltas = new ArrayList<>();
		model.setDelta((item, mask) -> { deltas.add(item); deltas.add(mask); });
		model.reconcileProps(new String[] {"b", "a", "c"}, new String[] {"B", "A", "C"},
				new short[] {(short) SELECTED, 0, 0}, new short[] {(short) SELECTED, 0, 0});
		assertEquals(List.of(-1, WebLanes.D_ITEMS), deltas);
		model.pointer(P_DRAG, 12, 52, 0);
		assertFalse("B must not acquire A's drag", model.is(0, DRAGGING));
		assertTrue("A retains its drag", model.is(1, DRAGGING));
		model.pointer(P_UP, 12, 52, 0);
		assertEquals("B", model.text(0));
		assertEquals("A", model.text(1));
		assertFalse(model.is(0, DRAGGING));
		assertFalse(model.is(1, DRAGGING));
	}

	@Test
	public void removingTheDraggedKeyCancelsItsDragWithoutRetargeting() {
		ReorderDragModel model = dragging();
		model.reconcile(new String[] {"b", "c"}, new String[] {"B", "C"});
		model.pointer(P_DRAG, 12, 52, 0);
		assertFalse("retired A must not retarget B", model.is(0, DRAGGING));
		assertFalse(model.is(1, DRAGGING));
		model.pointer(P_UP, 12, 52, 0);
		assertEquals("B", model.text(0));
		assertEquals("C", model.text(1));
	}

	@Test
	public void rejectedSnapshotLeavesTheExistingDragIntact() {
		ReorderDragModel model = dragging();
		int[] before = model.snapshot();
		int generation = model.generation();
		assertThrows(IllegalArgumentException.class, () -> model.reconcile(
				new String[] {"a", "a"}, new String[] {"A", "A"}));
		assertArrayEquals(before, model.snapshot());
		assertEquals(generation, model.generation());
		model.pointer(P_DRAG, 12, 12, 0);
		assertTrue(model.is(0, DRAGGING));
		model.pointer(P_UP, 12, 12, 0);
		assertFalse(model.is(0, DRAGGING));
	}

	@Test
	public void kanbanLongCoordinatesDoNotWrapOffscreenRowsIntoTheViewport() {
		KanbanBoardModel model = new KanbanBoardModel();
		model.reconcile(new String[] {"0", "1", "2", "3", "4", "5", "6", "7"},
				new String[] {"ROW_0", "ROW_1", "ROW_2", "ROW_3", "ROW_4", "ROW_5", "ROW_6", "ROW_7"});
		model.setItemExtent(1_000_000_000);
		model.layout(320, 160);
		model.scrollTo(1L << 32);
		for (boolean rtl : new boolean[] {false, true}) {
			model.setRtl(rtl);
			assertEquals(0, model.itemRect(0, 2));
			assertTrue(model.itemRect(4, 2) > 0);
			assertEquals(4, model.hitTest(10, 40));
			List<String> painted = transcript(paint(model));
			assertTrue(painted.stream().anyMatch(s -> s.contains("ROW_4")));
			assertFalse(painted.stream().anyMatch(s -> s.contains("ROW_0")));
		}
	}
}
