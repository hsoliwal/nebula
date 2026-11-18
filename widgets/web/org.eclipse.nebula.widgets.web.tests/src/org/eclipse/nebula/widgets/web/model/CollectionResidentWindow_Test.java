// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Compares bounded native paint and hit-testing against the old full-scan hooks. */
public class CollectionResidentWindow_Test {
	private static final int[] MODES = {
			CollectionModel.KANBAN, CollectionModel.SWIPE, CollectionModel.PULL,
			CollectionModel.INFINITE, CollectionModel.REORDER, CollectionModel.MESSAGES,
			CollectionModel.PALETTE, CollectionModel.GALLERY, CollectionModel.MASONRY };

	private static final class FullScanCollection extends CollectionPlaneChecks {
		FullScanCollection(int mode) {
			super("native", 0, mode, WebStates.ROUND, WebStates.V_DEFAULT);
		}
	}

	private static final class FullScanMasonry extends MasonryModel {
	}

	/** A downstream subclass is allowed to position items outside the default window. */
	private static final class ExtendedCollection extends CollectionModel {
		ExtendedCollection() {
			super("native", 0, PALETTE, WebStates.ROUND, WebStates.V_DEFAULT);
		}

		@Override
		protected void doLayout() {
			super.doLayout();
			if (count > 0) {
				setRect(0, width + 100, 4, 80, 30);
			}
		}
	}

	private static final class ExtendedMasonry extends MasonryModel {
		@Override
		protected void doLayout() {
			super.doLayout();
			if (count > 0) {
				setRect(0, width + 100, 4, 80, 30);
			}
		}
	}

	@Test
	public void allNineModesRetainPaintAndPointerOrderingAcrossFiltersAndRtl() {
		String[] keys = new String[95], labels = new String[95];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = "id-" + i;
			labels[i] = i == 45 ? "JSX <Item onClick={() => {}} /> // literal"
					: (i % 3 == 0 ? "keep " : "other ") + i;
		}
		for (int mode : MODES) {
			CollectionModel fast = mode == CollectionModel.MASONRY ? new MasonryModel()
					: new CollectionModel("native", 0, mode, WebStates.ROUND, WebStates.V_DEFAULT);
			CollectionModel old = mode == CollectionModel.MASONRY ? new FullScanMasonry()
					: new FullScanCollection(mode);
			initialize(fast, keys, labels);
			initialize(old, keys, labels);
			for (boolean rtl : new boolean[] {false, true}) {
				for (String filter : new String[] {"", "keep", "literal", "missing"}) {
					fast.setRtl(rtl);
					old.setRtl(rtl);
					fast.setFilter(filter);
					old.setFilter(filter);
					fast.scrollTo(577);
					old.scrollTo(577);
					assertArrayEquals("layout mode=" + mode + ",filter=" + filter,
							old.snapshot(), fast.snapshot());
					WebPaint expected = new WebPaint(), actual = new WebPaint();
					old.paint(expected);
					fast.paint(actual);
					assertEquals("paint count mode=" + mode + ",filter=" + filter,
							expected.count(), actual.count());
					CollectionPlaneChecks.assertPaint(fast, expected, actual,
							"paint fields and plane clips mode=" + mode + ",filter=" + filter);
					for (int y = -6; y <= 174; y += 18) {
						for (int x = -6; x <= 434; x += 22) {
							assertEquals("pointer mode=" + mode + ",filter=" + filter
									+ ",rtl=" + rtl + ",x=" + x + ",y=" + y,
									old.hitTest(x, y), fast.hitTest(x, y));
						}
					}
				}
			}
		}
	}

	private static void initialize(CollectionModel m, String[] keys, String[] labels) {
		m.reconcile(keys, labels);
		m.layout(428, 164);
		m.setValue(5, 168);
		m.setValue(27, 224);
		m.setValue(69, 336);
		m.setGroup(5, 1);
		m.setGroup(27, 2);
		m.setState(5, WebStates.HIDDEN, true);
		m.setState(27, WebStates.SELECTED, true);
		m.setState(69, WebStates.DISABLED, true);
		m.setAux(27, -24);
		m.setFocus(27);
	}

	@Test
	public void subclassesRetainOutOfWindowPaintAndPointerHooks() {
		for (CollectionModel model : new CollectionModel[] {new ExtendedCollection(), new ExtendedMasonry()}) {
			model.setLogicalCount(1000);
			model.layout(420, 120);
			model.scrollTo(15_000);
			assertTrue(model.window(2) > 0);
			assertEquals(0, model.hitTest(521, 5));
			WebPaint paint = new WebPaint();
			model.paint(paint);
			boolean retained = false;
			for (int r = 0; r < paint.count(); r++) {
				if (paint.get(r, WebPaint.F_OP) == WebPaint.TEXT
						&& "1".equals(paint.text(paint.get(r, WebPaint.F_REF)))) {
					retained = true;
				}
			}
			assertTrue("off-window subclass item remains painted", retained);
		}
	}

	@Test
	public void hundredThousandItemsKeepBoundedDisplayList() {
		for (int mode : new int[] {CollectionModel.SWIPE, CollectionModel.MESSAGES,
				CollectionModel.PALETTE, CollectionModel.GALLERY, CollectionModel.MASONRY}) {
			CollectionModel model = mode == CollectionModel.MASONRY ? new MasonryModel()
					: new CollectionModel("native", 0, mode, WebStates.ROUND, WebStates.V_DEFAULT);
			model.setLogicalCount(100_000);
			model.layout(896, 192);
			model.scrollTo(900_000L);
			WebPaint paint = new WebPaint();
			model.paint(paint);
			assertTrue("residency mode=" + mode, model.residentCount() > 0 && model.residentCount() < 100);
			assertTrue("paint mode=" + mode, paint.count() <= model.residentCount() * 6 + 8);
			assertEquals(-1, model.hitTest(-100, -100));
		}
	}
}
