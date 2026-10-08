/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Rectangle;
import org.junit.Test;

/**
 * Behavioral distillation proof for viewport/chrome/graphics mechanics.
 *
 * <p>The scenarios are derived from public SWT/Swing example categories, not
 * copied example implementations.  They keep the donor observations executable
 * against Grid's retained atoms.</p>
 */
public class GridViewportDistillation_Test {

	@Test
	public void verticalScrollDamagesBodyWithoutHeaderOrFooter() {
		Rectangle client = new Rectangle(0, 0, 640, 480);
		Rectangle body = GridViewportDamage.scrollDamage(client, 32, 24, false);

		assertEquals(new Rectangle(0, 32, 640, 424), body);
		assertFalse(GridViewportDamage.intersectsHeader(body, client, 32));
		assertFalse(GridViewportDamage.intersectsFooter(body, client, 24));

		int plan = GridPaintDAG.plan(
				body, client, 32, 24, true, true, true, false);
		assertTrue(GridPaintDAG.includes(plan, GridPaintDAG.BACKGROUND));
		assertTrue(GridPaintDAG.includes(plan, GridPaintDAG.BODY));
		assertTrue(GridPaintDAG.includes(plan, GridPaintDAG.FIXED));
		assertFalse(GridPaintDAG.includes(plan, GridPaintDAG.HEADER));
		assertFalse(GridPaintDAG.includes(plan, GridPaintDAG.FOOTER));
	}

	@Test
	public void headerAndBodyRemainIndependentPaintPlanes() {
		Rectangle client = new Rectangle(0, 0, 640, 480);
		Rectangle header = new Rectangle(0, 0, 640, 32);
		int headerPlan = GridPaintDAG.plan(
				header, client, 32, 24, true, true, true, false);

		assertTrue(GridPaintDAG.includes(headerPlan, GridPaintDAG.BACKGROUND));
		assertTrue(GridPaintDAG.includes(headerPlan, GridPaintDAG.HEADER));
		assertFalse(GridPaintDAG.includes(headerPlan, GridPaintDAG.BODY));
		assertFalse(GridPaintDAG.includes(headerPlan, GridPaintDAG.FIXED));

		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.HEADER, GridPaintDAG.BACKGROUND));
		assertFalse(GridPaintDAG.dependsOn(GridPaintDAG.HEADER, GridPaintDAG.BODY));
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.FIXED, GridPaintDAG.BODY));
	}

	@Test
	public void horizontalScrollKeepsScrollCoupledChromeInSharedDamageDomain() {
		Rectangle client = new Rectangle(5, 7, 640, 480);
		assertEquals(client, GridViewportDamage.scrollDamage(client, 32, 24, true));
	}

	@Test
	public void transientOverlayIsComposedAboveOrdinaryViewportPlanes() {
		Rectangle client = new Rectangle(0, 0, 640, 480);
		Rectangle clip = new Rectangle(0, 64, 200, 120);
		int plan = GridPaintDAG.plan(
				clip, client, 32, 0, true, false, false, true);

		assertTrue(GridPaintDAG.includes(plan, GridPaintDAG.BACKGROUND));
		assertTrue(GridPaintDAG.includes(plan, GridPaintDAG.BODY));
		assertTrue(GridPaintDAG.includes(plan, GridPaintDAG.OVERLAY));
		assertFalse(GridPaintDAG.includes(plan, GridPaintDAG.HEADER));
	}

	@Test
	public void affineCompositionRetainsOrderedViewportCoordinates() {
		GridTransform translation = GridTransform.translate(10, 20);
		GridTransform scale = GridTransform.scale(2, 3);
		GridTransform composed = translation.then(scale);

		assertArrayEquals(
				new float[] {2, 0, 0, 3, 20, 60},
				composed.elements(),
				0.0001f);
		assertEquals(
				new Rectangle(20, 60, 40, 60),
				composed.mapBounds(new Rectangle(0, 0, 20, 20)));
	}

	@Test
	public void canonicalQuadrantRotationKeepsExactIntegerBounds() {
		GridTransform quarterTurn = GridTransform.rotate((float)(Math.PI / 2d));
		assertArrayEquals(
				new float[] {0, 1, -1, 0, 0, 0},
				quarterTurn.elements(),
				0.0001f);
		assertEquals(
				new Rectangle(-20, 0, 20, 10),
				quarterTurn.mapBounds(new Rectangle(0, 0, 10, 20)));
	}

	@Test
	public void inverseTranslationRoundTripsViewportBounds() {
		GridTransform translation = GridTransform.translate(137, -29);
		Rectangle original = new Rectangle(11, 17, 83, 41);
		Rectangle mapped = translation.mapBounds(original);
		assertEquals(original, translation.inverse().mapBounds(mapped));
	}

	@Test
	public void graphicsStateDagCollapsesEquivalentStrokeAndAlphaAtoms() {
		LineAttributes stroke = new LineAttributes(
				2f, SWT.CAP_ROUND, SWT.JOIN_BEVEL, SWT.LINE_DASH,
				new float[] {3f, 5f}, 1f, 8f);
		GridGCStateDAG dag = new GridGCStateDAG(stroke, 255);

		assertEquals(0, dag.planStroke(new LineAttributes(
				2f, SWT.CAP_ROUND, SWT.JOIN_BEVEL, SWT.LINE_DASH,
				new float[] {3f, 5f}, 1f, 8f)));
		assertEquals(GridGCStateDAG.STROKE, dag.planStroke(new LineAttributes(
				3f, SWT.CAP_ROUND, SWT.JOIN_BEVEL, SWT.LINE_DASH,
				new float[] {3f, 5f}, 1f, 8f)));
		assertEquals(0, dag.planAlpha(255));
		assertEquals(GridGCStateDAG.ALPHA, dag.planAlpha(128));
		assertEquals(2, dag.nativeTransitions());
	}
}
