// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Resident pointer targets must match the legacy full-lane hit test. */
public class WebResidentHitTest_Test {
	private static final class LegacyInput extends InputModel {
		LegacyInput(int mode) {
			super("test", WebStates.role("combobox"), mode, WebStates.RECT, WebStates.V_DEFAULT);
		}
	}
	private static final class LegacySegments extends SegmentsModel {
		LegacySegments(int mode) {
			super("test", WebStates.role("navigation"), mode, WebStates.RECT, WebStates.V_DEFAULT);
		}
	}

	private static void sameTargets(WebModel actual, WebModel legacy) {
		assertArrayEquals("geometry/state", legacy.snapshot(), actual.snapshot());
		for (int y = -6; y < 180; y += 7) {
			for (int x = -9; x < 354; x += 11) {
				assertEquals("point " + x + "," + y,
						legacy.hitTest(x, y), actual.hitTest(x, y));
			}
		}
	}

	@Test
	public void autocompleteAndOtpMatchOriginalAcrossFilterRtlScrollAndHidden() {
		for (int mode : new int[] { InputModel.OTP, InputModel.AUTOCOMPLETE }) {
			InputModel actual = new InputModel("test", WebStates.role("combobox"),
					mode, WebStates.RECT, WebStates.V_DEFAULT);
			InputModel legacy = new LegacyInput(mode);
			actual.setLogicalCount(160);
			legacy.setLogicalCount(160);
			if (mode == InputModel.AUTOCOMPLETE) {
				for (int item : new int[] { 2, 17, 93, 148 }) {
					actual.setText(item, "match-" + item);
					legacy.setText(item, "match-" + item);
				}
				actual.setFilter("match");
				legacy.setFilter("match");
				actual.setState(-1, WebStates.OPEN, true);
				legacy.setState(-1, WebStates.OPEN, true);
			}
			actual.layout(320, 116);
			legacy.layout(320, 116);
			sameTargets(actual, legacy);
			for (long position : new long[] { 0, 24, 64, 320, 0 }) {
				actual.scrollTo(position);
				legacy.scrollTo(position);
				sameTargets(actual, legacy);
			}
			actual.setRtl(true);
			legacy.setRtl(true);
			sameTargets(actual, legacy);
			actual.setRtl(false);
			legacy.setRtl(false);
			sameTargets(actual, legacy);
			int hidden = mode == InputModel.OTP ? 3 : 17;
			actual.setState(hidden, WebStates.HIDDEN, true);
			legacy.setState(hidden, WebStates.HIDDEN, true);
			sameTargets(actual, legacy);
			if (mode == InputModel.AUTOCOMPLETE) {
				actual.setState(-1, WebStates.OPEN, false);
				legacy.setState(-1, WebStates.OPEN, false);
				// A direct OPEN change is not necessarily a layout publication.
				sameTargets(actual, legacy);
				actual.setFilter("does-not-match");
				legacy.setFilter("does-not-match");
				sameTargets(actual, legacy);
			}
		}
	}

	@Test
	public void segmentsModesRetainTheirRawIndexGeometryAndZOrder() {
		for (int mode = SegmentsModel.STEPPER; mode <= SegmentsModel.NAV; mode++) {
			SegmentsModel actual = new SegmentsModel("test", WebStates.role("navigation"),
					mode, WebStates.RECT, WebStates.V_DEFAULT);
			SegmentsModel legacy = new LegacySegments(mode);
			actual.setLogicalCount(512);
			legacy.setLogicalCount(512);
			actual.layout(320, 116);
			legacy.layout(320, 116);
			for (long position : new long[] { 0, 120, 12_345, 1_000_000, 0 }) {
				actual.scrollTo(position);
				legacy.scrollTo(position);
				sameTargets(actual, legacy);
			}
			actual.setRtl(true);
			legacy.setRtl(true);
			sameTargets(actual, legacy);
			actual.setRtl(false);
			legacy.setRtl(false);
			sameTargets(actual, legacy);
		}
	}

	@Test
	public void filteredOverlappingItemsUseHighestOriginalIndex() {
		InputModel actual = new InputModel("test", 0, InputModel.AUTOCOMPLETE,
				WebStates.RECT, WebStates.V_DEFAULT);
		actual.setLogicalCount(64);
		for (int item : new int[] { 3, 19, 51 }) {
			actual.setText(item, "hit");
		}
		actual.setFilter("hit");
		actual.setState(-1, WebStates.OPEN, true);
		actual.layout(320, 116);
		actual.setRect(3, 10, 45, 35, 25);
		actual.setRect(19, 10, 45, 35, 25);
		assertEquals(19, actual.hitTest(15, 50));
		actual.setState(19, WebStates.HIDDEN, true);
		// Hidden-state publication may recompute layout; restore the deliberate
		// overlap to verify that the higher-index hidden item is ignored.
		actual.setRect(3, 10, 45, 35, 25);
		actual.setRect(19, 10, 45, 35, 25);
		assertEquals(3, actual.hitTest(15, 50));
	}

	private static final class CustomInput extends InputModel {
		CustomInput() {
			super("test", 0, OTP, WebStates.RECT, WebStates.V_DEFAULT);
		}
		@Override
		protected void doLayout() {
			super.doLayout();
			if (count > 90) setRect(90, 4, 4, 24, 24);
		}
	}
	private static final class CustomSegments extends SegmentsModel {
		CustomSegments() {
			super("test", 0, PAGINATION, WebStates.RECT, WebStates.V_DEFAULT);
		}
		@Override
		protected void doLayout() {
			super.doLayout();
			if (count > 90) setRect(90, 4, 4, 24, 24);
		}
	}
	@Test
	public void externalSubclassMayTargetOffWindowItem() {
		CustomInput input = new CustomInput();
		input.setLogicalCount(120);
		input.layout(320, 116);
		assertEquals(90, input.hitTest(5, 5));
		CustomSegments seg = new CustomSegments();
		seg.setLogicalCount(120);
		seg.layout(320, 116);
		assertEquals(90, seg.hitTest(5, 5));
	}

	@Test
	public void hundredThousandLogicalItemsNeedNoItemObjectsForPointerQueries() {
		InputModel auto = new AutocompleteModel();
		auto.setLogicalCount(100_000);
		auto.setState(-1, WebStates.OPEN, true);
		auto.layout(320, 116);
		SegmentsModel pages = new SegmentsModel("test", 0, SegmentsModel.PAGINATION,
				WebStates.RECT, WebStates.V_DEFAULT);
		pages.setLogicalCount(100_000);
		pages.layout(320, 116);
		pages.scrollTo(320_000L);
		for (int k = 0; k < 500; k++) {
			assertEquals(-1, auto.hitTest(-100, -100));
			assertEquals(-1, pages.hitTest(-100, -100));
		}
		assertTrue(pages.residentCount() < 64);
		assertTrue(auto.residentCount() < 64);
	}
}
