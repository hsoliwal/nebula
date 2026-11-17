/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Product-bundle contracts for React-like variable-height Masonry tiles. */
public class MasonryModel_Test {
	@Test
	public void existingCoreAndCollectionContracts() {
		MasonryModel model = MasonryModel.sample();
		assertEquals("grid", WebStates.roleName(model.role()));
		model.layout(360, 260);
		WebChecks.paints(model);
		WebChecks.deltas(model);
		WebChecks.themes(model);
		WebChecks.mirror(model);
		WebChecks.keys(model);
		WebChecks.pointer(model);
		WebChecks.damage(model);
		WebChecks.localize(model);
		WebChecks.removal(model);
		// WebChecks.resident() assumes one item per row, so Masonry validates
		// its variable-height multi-column window in deepScrollUsesBoundedResidentPaint.
		CollectionChecks.run(MasonryModel.sample());
	}

	@Test
	public void shortestColumnPackingAndThemeProjection() {
		MasonryModel model = MasonryModel.sample();
		int[] heights = {112, 224, 56, 336, 168, 112};
		for (int i = 0; i < heights.length; i++) {
			model.setTileHeight(i, heights[i]);
			assertEquals(heights[i], model.tileHeight(i));
		}
		model.layout(336, 192);
		assertEquals(392L, model.logicalExtent());
		assertEquals(56, model.itemRect(3, 1));
		assertEquals(112, model.itemRect(4, 1));
		assertEquals(224, model.itemRect(5, 1));
		assertEquals(model.itemRect(0, 0), model.itemRect(4, 0));
		assertEquals(model.itemRect(1, 0), model.itemRect(5, 0));
		assertEquals(model.itemRect(2, 0), model.itemRect(3, 0));

		int initialX = model.itemRect(0, 0);
		model.setRtl(true);
		assertTrue(model.itemRect(0, 0) > model.itemRect(1, 0));
		assertTrue(model.itemRect(0, 0) > initialX);
		model.setDark(true);
		assertTrue(model.isDark());
		WebPaint paint = new WebPaint();
		model.paint(paint);
		assertTrue(paint.count() > 0);
		assertTrue(paint.count() <= model.residentCount() * 8 + 8);
	}

	@Test
	public void filteredGeometryAndHiddenItems() {
		MasonryModel model = MasonryModel.sample();
		int[] heights = {112, 224, 56, 336, 168, 112};
		for (int i = 0; i < heights.length; i++) {
			model.setTileHeight(i, heights[i]);
		}
		model.layout(336, 192);
		model.setFilter("card");
		assertEquals(3, model.logicalCount());
		assertEquals(336L, model.logicalExtent());
		model.reveal(5); // Excluded by the filter; must not move the viewport.
		assertEquals(0L, model.scrollOffset());
		model.setFilter("no-hit");
		assertEquals(0, model.logicalCount());
		assertEquals(0L, model.logicalExtent());
		assertEquals(0, model.residentCount());
		WebPaint paint = new WebPaint();
		model.paint(paint);
		assertEquals(0, paint.count());
		model.setFilter("");
		assertEquals(6, model.logicalCount());
	}

	@Test
	public void deepScrollUsesBoundedResidentPaint() {
		MasonryModel model = new MasonryModel();
		model.setLogicalCount(100_000);
		model.layout(896, 192);
		assertEquals(1_400_000L, model.logicalExtent());
		model.scrollTo(900_000L);
		assertEquals(900_000L, model.scrollOffset());
		assertTrue(model.residentCount() > 0);
		assertTrue(model.residentCount() < 100);
		assertEquals(0, model.itemRect(0, 2));
		WebPaint paint = new WebPaint();
		model.paint(paint);
		assertTrue(paint.count() <= model.residentCount() * 8 + 8);
		model.reveal(90_000);
		assertTrue(model.scrollOffset() > 900_000L);
		assertTrue(model.residentCount() < 100);
	}

	@Test
	public void subclassesMayPaintOutsideTheStandardResidentWindow() {
		class CustomLayout extends MasonryModel {
			@Override
			protected void doLayout() {
				super.doLayout();
				setRect(0, 0, 0, 60, 60);
			}
		}
		CustomLayout model = new CustomLayout();
		model.setLogicalCount(10_000);
		model.layout(336, 192);
		model.scrollTo(80_000);
		assertFalse(model.window(2) == 0);
		WebPaint paint = new WebPaint();
		model.paint(paint);
		assertTrue("custom off-window layout must retain painting", paint.count() > 0);
	}
}
