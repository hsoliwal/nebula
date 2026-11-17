/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Observable geometry and lifecycle contracts shared by the existing viewport families. */
public class ResidentGeometry_Test {
	@Test
	public void segmentsRetireOldWindowsAcrossAllModes() {
		for (int mode = SegmentsModel.STEPPER; mode <= SegmentsModel.NAV; mode++) {
			SegmentsModel model = new SegmentsModel("segments", 0, mode, ROUND, 0);
			model.setLogicalCount(10_000);
			model.layout(320, 120);
			model.scrollTo((long) model.itemExtent() * 8_000);
			assertTrue(model.residentCount() < 32);
			assertEquals(0, model.itemRect(0, 2));
			int retired = model.window(2);
			model.scrollTo(0);
			assertEquals(0, model.itemRect(retired, 2));
			assertTrue(model.itemRect(0, 2) > 0);
			model.layout(0, 0);
			assertEquals(0, model.residentCount());
			assertEquals(0, model.itemRect(0, 2));
		}
	}

	@Test
	public void segmentsPreserveZeroRectCoordinatesThroughRtlTransitions() {
		NavigationModel model = new NavigationModel();
		model.setLogicalCount(500);
		model.layout(180, 80);
		model.setRtl(true);
		assertEquals(180, model.itemRect(499, 0));
		model.scrollTo(4_000);
		assertEquals(180, model.itemRect(499, 0));
		model.setRtl(false);
		assertEquals(0, model.itemRect(499, 0));
		model.scrollTo(0);
		assertEquals(0, model.itemRect(499, 0));
	}

	@Test
	public void segmentsShrinkClearAndRegrowWithoutStaleGeometry() {
		NavigationModel model = new NavigationModel();
		model.setLogicalCount(2_000);
		model.layout(200, 120);
		model.scrollTo(50_000);
		model.setLogicalCount(1);
		assertEquals(0, model.scrollOffset());
		assertTrue(model.itemRect(0, 2) > 0);
		model.clear();
		model.setLogicalCount(2_000);
		assertEquals(0, model.itemRect(1_250, 2));
		assertTrue(model.itemRect(0, 2) > 0);
	}

	@Test
	public void segmentSubclassRetainsEverySetRectHookInOriginalOrder() {
		class Observed extends SegmentsModel {
			final List<Integer> calls = new ArrayList<>();
			Observed() { super("observed", 0, NAV, ROUND, 0); }
			@Override protected void setRect(int item, int x, int y, int w, int h) {
				calls.add(Integer.valueOf(item));
				super.setRect(item, x, y, w, h);
			}
		}
		Observed model = new Observed();
		model.setLogicalCount(100);
		model.layout(200, 80);
		model.calls.clear();
		model.scrollTo(800);
		assertEquals(100, model.calls.size());
		for (int i = 0; i < 100; i++) assertEquals(Integer.valueOf(i), model.calls.get(i));
	}

	@Test
	public void autocompleteRetiresFilteredIdentitiesOnCloseAndReplacement() {
		AutocompleteModel model = new AutocompleteModel();
		for (int i = 0; i < 250; i++) model.add((i % 2 == 0 ? "even" : "odd") + i);
		model.layout(200, 160);
		model.setFilter("even");
		model.setState(-1, OPEN, true);
		model.scrollTo(2_000);
		int retired = model.itemAt(model.window(2));
		assertTrue(model.itemRect(retired, 2) > 0);
		model.setFilter("odd");
		assertEquals(0, model.itemRect(retired, 2));
		int shown = model.itemAt(model.window(2));
		assertTrue(model.itemRect(shown, 2) > 0);
		assertTrue(model.key(KEY_ESCAPE, 0, 0));
		assertEquals(0, model.itemRect(shown, 2));
		model.setFilter("missing");
		model.setState(-1, OPEN, true);
		assertEquals(0, model.residentCount());
		for (int i = 0; i < model.count(); i++) assertEquals(0, model.itemRect(i, 2));
	}

	@Test
	public void autocompleteRtlShrinkRemoveAndRegrowKeepRectContracts() {
		AutocompleteModel model = new AutocompleteModel();
		model.setLogicalCount(1_000);
		model.layout(220, 140);
		model.setState(-1, OPEN, true);
		model.scrollTo(12_000);
		model.setRtl(true);
		assertEquals(220, model.itemRect(999, 0));
		model.setRtl(false);
		assertEquals(0, model.itemRect(999, 0));
		model.setLogicalCount(3);
		model.remove(0);
		model.clear();
		model.setLogicalCount(1_000);
		assertEquals(0, model.itemRect(999, 2));
		assertTrue(model.itemRect(0, 2) > 0);
	}

	@Test
	public void otpKeepsItsExistingSixtyFourCellContract() {
		OtpInputModel model = new OtpInputModel();
		model.setLogicalCount(1_000);
		model.layout(200, 80);
		assertTrue(model.itemRect(63, 2) > 0);
		assertEquals(0, model.itemRect(64, 2));
		model.setRtl(true);
		assertEquals(200, model.itemRect(999, 0));
		model.setRtl(false);
		assertEquals(0, model.itemRect(999, 0));
		model.setLogicalCount(2);
		model.setLogicalCount(1_000);
		assertTrue(model.itemRect(63, 2) > 0);
		assertEquals(0, model.itemRect(64, 2));
	}

	@Test
	public void inputSubclassRetainsFullClearBeforeDropdownHooks() {
		class Observed extends InputModel {
			final List<Integer> calls = new ArrayList<>();
			Observed() { super("observed", 0, AUTOCOMPLETE, ROUND, 0); }
			@Override protected void setRect(int item, int x, int y, int w, int h) {
				calls.add(Integer.valueOf(item));
				super.setRect(item, x, y, w, h);
			}
		}
		Observed model = new Observed();
		model.setLogicalCount(100);
		model.layout(200, 150);
		model.setState(-1, OPEN, true);
		model.calls.clear();
		model.scrollTo(800);
		assertEquals(100 + model.residentCount(), model.calls.size());
		for (int i = 0; i < 100; i++) assertEquals(Integer.valueOf(i), model.calls.get(i));
		for (int i = 0; i < model.residentCount(); i++)
			assertEquals(Integer.valueOf(model.itemAt(model.window(2) + i)), model.calls.get(100 + i));
	}
}
