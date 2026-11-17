/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Contract tests of the React-style sparkline inside Nebula's existing viewport. */
public class SparklineModel_Test {
	@Test
	public void valuesAccessibilityAndPrimitivePaint() {
		SparklineModel model = SparklineModel.sample();
		assertEquals(7, model.count());
		assertEquals("img", WebStates.roleName(model.role()));
		assertEquals(2, model.minValue());
		assertEquals(9, model.maxValue());
		assertEquals("Point 3: 2", model.accessibleName(2));
		model.layout(96, 40);
		assertEquals(2, model.itemRect(0, 0));
		assertEquals(27, model.itemRect(0, 1));
		assertEquals(0, model.itemRect(5, 1));
		WebPaint paint = new WebPaint();
		model.paint(paint);
		assertEquals(2, paint.count());
		assertEquals(WebPaint.POLY, paint.get(0, WebPaint.F_OP));
		assertEquals(7, paint.get(0, WebPaint.F_ARG));
		int first = paint.get(0, WebPaint.F_REF);
		assertEquals(6, paint.point(first));
		assertEquals(31, paint.point(first + 1));
		assertEquals(WebPaint.OVAL, paint.get(1, WebPaint.F_OP));
	}

	@Test
	public void millionScaleLogicalGeometryWithBoundedPainting() {
		SparklineModel model = new SparklineModel();
		model.setLogicalCount(100_000);
		model.layout(240, 48);
		assertEquals(1_200_000L, model.logicalExtent());
		model.scrollTo(800_000L);
		assertEquals(800_000L, model.scrollOffset());
		assertTrue(model.residentCount() > 0);
		assertTrue(model.residentCount() < 32);
		assertEquals(0, model.itemRect(0, 2));
		assertEquals(66_667, model.hitTest(12, 18));
		WebPaint paint = new WebPaint();
		model.paint(paint);
		assertTrue(paint.count() <= model.residentCount() + 2);
		assertTrue(paint.count() > 0);
		model.setFocus(90_000);
		assertTrue(model.scrollOffset() > 800_000L);
		assertTrue(model.residentCount() < 32);
		assertEquals(0, model.itemRect(66_667, 2));
	}

	@Test
	public void filterRtlAndRetiredResidentRectangles() {
		SparklineModel model = SparklineModel.sample();
		for (int i = 0; i < model.count(); i++) {
			model.setText(i, i == 1 || i == 4 ? "keep" : "drop");
		}
		model.layout(36, 40);
		model.setFilter("keep");
		assertEquals(2, model.logicalCount());
		assertEquals(24L, model.logicalExtent());
		assertEquals(1, model.hitTest(6, 14));
		assertEquals(4, model.hitTest(18, 14));
		assertEquals(0, model.itemRect(0, 2));
		int left = model.itemRect(1, 0);
		model.setRtl(true);
		assertEquals(0, model.aspects() & SparklineModel.A_D);
		assertEquals(36 - left - 8, model.itemRect(1, 0));
		assertEquals(4, model.hitTest(18, 14));
		model.setFocus(6); // Excluded from the filter; must not scroll to a phantom point.
		assertEquals(0L, model.scrollOffset());
		model.setFilter("missing");
		assertEquals(0, model.residentCount());
		assertEquals(0, model.itemRect(1, 2));
		WebPaint paint = new WebPaint();
		model.paint(paint);
		assertEquals(0, paint.count());
	}

	@Test
	public void hiddenPointsSplitTheLineAndSinglePointStillPaints() {
		SparklineModel model = SparklineModel.sample();
		model.layout(96, 40);
		model.setState(3, WebStates.HIDDEN, true);
		WebPaint paint = new WebPaint();
		model.paint(paint);
		assertEquals(4, paint.count()); // two independent lines and their terminal points
		assertEquals(3, paint.get(0, WebPaint.F_ARG));
		assertEquals(3, paint.get(2, WebPaint.F_ARG));
		model.load("-5");
		model.paint(paint);
		assertEquals(1, paint.count());
		assertEquals(WebPaint.OVAL, paint.get(0, WebPaint.F_OP));
		assertEquals(-5, model.minValue());
		assertEquals(-5, model.maxValue());
	}

	@Test
	public void signedExtremesLoadReplacementAndFocusNavigation() {
		SparklineModel model = new SparklineModel();
		model.load(Integer.MIN_VALUE + ";0;" + Integer.MAX_VALUE);
		model.layout(36, 40);
		assertEquals(Integer.MIN_VALUE, model.minValue());
		assertEquals(Integer.MAX_VALUE, model.maxValue());
		for (int i = 0; i < model.count(); i++) {
			assertTrue(model.itemRect(i, 1) >= -4);
			assertTrue(model.itemRect(i, 1) <= 35);
		}
		assertTrue(model.key(WebStates.KEY_RIGHT, 0, 0));
		assertEquals(0, model.focus());
		assertTrue(model.key(WebStates.KEY_END, 0, 0));
		assertEquals(2, model.focus());
		model.setDark(true);
		assertTrue(model.isDark());
		model.setValue(0, 3);
		assertEquals(0, model.minValue());
		model.load("");
		assertEquals(0, model.count());
		assertEquals(0, model.minValue());
		assertFalse(model.hitTest(5, 5) >= 0);
		int[] samples = { 7, -3, 42 };
		model.load(samples);
		samples[0] = 999;
		assertEquals(7, model.value(0));
		assertEquals(-3, model.minValue());
		assertEquals(42, model.maxValue());
	}

	@Test
	public void horizontalDamageBlitAndRtlFallback() {
		SparklineModel model = new SparklineModel();
		model.setLogicalCount(100);
		model.layout(72, 40);
		int[] damage = new int[4];
		assertTrue(model.takeDamage(damage));
		model.scrollTo(12);
		assertEquals(-12, model.takeBlit());
		assertTrue(model.takeDamage(damage));
		assertEquals(60, damage[0]);
		assertEquals(12, damage[2]);
		model.setRtl(true);
		assertTrue(model.takeDamage(damage));
		model.scrollTo(24);
		assertEquals(0, model.takeBlit());
		assertTrue(model.takeDamage(damage));
		assertEquals(0, damage[0]);
		assertEquals(72, damage[2]);
	}
}
