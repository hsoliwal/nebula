// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import org.junit.Test;

/** Exact built-in viewport paths must not regress to a whole-collection scan. */
public class CollectionAllNineResidentPaint_Test {
	private static final int[] MODES = { CollectionModel.KANBAN, CollectionModel.SWIPE,
			CollectionModel.PULL, CollectionModel.INFINITE, CollectionModel.REORDER,
			CollectionModel.MESSAGES, CollectionModel.PALETTE, CollectionModel.GALLERY,
			CollectionModel.MASONRY };

	private static CollectionModel builtIn(int mode) {
		return switch (mode) {
			case CollectionModel.KANBAN -> new KanbanBoardModel();
			case CollectionModel.SWIPE -> new SwipeActionsModel();
			case CollectionModel.PULL -> new PullToRefreshModel();
			case CollectionModel.INFINITE -> new InfiniteScrollModel();
			case CollectionModel.REORDER -> new ReorderDragModel();
			case CollectionModel.MESSAGES -> new MessageListModel();
			case CollectionModel.PALETTE -> new CommandPaletteModel();
			case CollectionModel.MASONRY -> new MasonryModel();
			default -> new CollectionModel("gallery", WebStates.role("grid"),
					CollectionModel.GALLERY, WebStates.RECT, WebStates.V_DEFAULT);
		};
	}

	private static class FullScan extends CollectionPlaneChecks {
		FullScan(int mode) {
			super("legacy", WebStates.role("grid"), mode, WebStates.RECT,
					WebStates.V_DEFAULT);
		}
	}

	private static void assertPublicParity(CollectionModel current, CollectionModel baseline) {
		assertArrayEquals(baseline.snapshot(), current.snapshot());
		WebPaint expected = new WebPaint(), actual = new WebPaint();
		baseline.paint(expected);
		current.paint(actual);
		assertEquals("paint record count", expected.count(), actual.count());
		CollectionPlaneChecks.assertPaint(current, expected, actual, "paint transcript");
		for (int y = -8; y < 190; y += 13) {
			for (int x = -5; x < 360; x += 19) {
				assertEquals("hit " + x + ":" + y,
						baseline.hitTest(x, y), current.hitTest(x, y));
			}
		}
	}

	@Test
	public void nineExactOwnersEnterTheResidentFastPath() throws Exception {
		Method eligible = CollectionModel.class.getDeclaredMethod("residentOnlyOwner");
		eligible.setAccessible(true);
		for (int mode : MODES) {
			CollectionModel model = builtIn(mode);
			assertTrue("mode " + mode, (Boolean) eligible.invoke(model));
			assertFalse("external subclass " + mode,
					(Boolean) eligible.invoke(new FullScan(mode)));
		}
	}

	@Test
	public void originalModelFactoryParityForEveryMode() {
		// Compare generic original-API factories against an unrecognized subclass.
		// This avoids leaf-specific concept/shape rendering differences.
		for (int mode : MODES) {
			CollectionModel model = new CollectionModel("native", WebStates.role("grid"), mode,
					WebStates.RECT, WebStates.V_DEFAULT);
			CollectionModel previous = new FullScan(mode);
			model.setLogicalCount(256);
			previous.setLogicalCount(256);
			for (int k = 0; k < 256; k++) {
				if (mode == CollectionModel.KANBAN) {
					model.setGroup(k, k % 4);
					previous.setGroup(k, k % 4);
				}
				if (mode == CollectionModel.MASONRY && k % 17 == 0) {
					model.setValue(k, 140);
					previous.setValue(k, 140);
				}
			}
			model.layout(320, 128);
			previous.layout(320, 128);
			assertPublicParity(model, previous);
			for (long offset : new long[] { 0, 47, 1_001, 8_400, 0 }) {
				model.scrollTo(offset);
				previous.scrollTo(offset);
				assertPublicParity(model, previous);
			}
			model.setText(3, "keep-3");
			previous.setText(3, "keep-3");
			model.setText(19, "keep-19");
			previous.setText(19, "keep-19");
			model.setText(171, "keep-171");
			previous.setText(171, "keep-171");
			model.setFilter("keep");
			previous.setFilter("keep");
			assertPublicParity(model, previous);
			model.setRtl(true);
			previous.setRtl(true);
			assertPublicParity(model, previous);
			model.setRtl(false);
			previous.setRtl(false);
			assertPublicParity(model, previous);
		}
	}


	@Test
	public void builtInFocusAndFilteredRevealMatchOriginalSubclassTraversal() {
		for (int mode : MODES) {
			CollectionModel current = new CollectionModel("native", WebStates.role("grid"),
					mode, WebStates.RECT, WebStates.V_DEFAULT);
			CollectionModel full = new FullScan(mode);
			current.setLogicalCount(2_000);
			full.setLogicalCount(2_000);
			for (int id : new int[] { 10, 500, 1_990 }) {
				current.setText(id, "keep-" + id);
				full.setText(id, "keep-" + id);
			}
			current.layout(320, 128);
			full.layout(320, 128);
			for (int id : new int[] { 1_990, 10, 500, 1_990 }) {
				current.setFocus(id);
				full.setFocus(id);
				assertPublicParity(current, full);
			}
			current.setFilter("keep");
			full.setFilter("keep");
			for (int id : new int[] { 1_990, 500, 37, 10 }) {
				current.setFocus(id);
				full.setFocus(id);
				assertPublicParity(current, full);
			}
		}
	}

	@Test
	public void everyBuiltInHasBoundedResidentDisplayListForHundredThousandItems() {
		for (int mode : MODES) {
			CollectionModel model = builtIn(mode);
			model.setLogicalCount(100_000);
			model.layout(320, 128);
			model.scrollTo(15_000L);
			WebPaint output = new WebPaint();
			model.paint(output);
			// On Kanban headers also paint a small constant number of commands.
			assertTrue("mode " + mode + " resident=" + model.residentCount(),
					output.count() <= Math.max(20, model.residentCount() * 8 + 12));
			assertEquals(-1, model.hitTest(-100, -100));
			model.setFilter("not-present");
			model.paint(output);
			// Kanban's independent column index preserves its historical
			// filter semantics and can still paint cards when logicalCount=0.
			if (mode != CollectionModel.KANBAN) {
				assertEquals("empty filter " + mode, 0, output.count());
			}
		}
	}

	private static final class SpecialLayout extends CollectionModel {
		SpecialLayout(int mode) {
			super("custom", WebStates.role("grid"), mode, WebStates.RECT, 0);
		}
		@Override
		protected void doLayout() {
			super.doLayout();
			if (count > 90) {
				setRect(90, 4, 4, 20, 20);
			}
		}
	}
	@Test
	public void unknownSubclassesRetainOffWindowHitAndPaintTargets() {
		for (int mode : MODES) {
			SpecialLayout custom = new SpecialLayout(mode);
			custom.setLogicalCount(100);
			custom.layout(320, 128);
			custom.scrollTo(0);
			// Nonresident 90 must remain observable.
			assertEquals(90, custom.hitTest(5, 5));
			WebPaint paint = new WebPaint();
			custom.paint(paint);
			assertTrue(paint.count() > 0);
		}
	}

	@Test
	public void kanbanUsesColumnResidentsRatherThanOneLogicalWindow() {
		CollectionModel model = new KanbanBoardModel();
		model.setLogicalCount(1000);
		for (int i = 0; i < 1000; i++) {
			model.setGroup(i, i % 4);
		}
		model.layout(320, 128);
		model.scrollTo(45L);
		// A column can have a visible row whose source id is far outside the
		// contiguous window of source items. The column index must include it.
		int target = model.hitTest(14, 50);
		assertTrue("kanban column hit must stay in the original lane", target >= 0);
		model.setState(target, WebStates.HIDDEN, true);
		assertEquals(-1, model.hitTest(14, 50));
	}
}
