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

import java.util.Arrays;

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
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.HEADER, GridPaintDAG.BODY));
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.HEADER, GridPaintDAG.FIXED));
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.FIXED, GridPaintDAG.BODY));
		assertFalse(GridPaintDAG.dependsOn(GridPaintDAG.HEADER, GridPaintDAG.FOOTER));
	}

	@Test
	public void viewportPlanesHaveStableZOrder() {
		assertEquals(0, GridPaintDAG.zIndex(GridPaintDAG.BACKGROUND));
		assertEquals(1, GridPaintDAG.zIndex(GridPaintDAG.BODY));
		assertEquals(2, GridPaintDAG.zIndex(GridPaintDAG.FIXED));
		assertEquals(3, GridPaintDAG.zIndex(GridPaintDAG.HEADER));
		assertEquals(3, GridPaintDAG.zIndex(GridPaintDAG.FOOTER));
		assertEquals(4, GridPaintDAG.zIndex(GridPaintDAG.OVERLAY));

		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.OVERLAY, GridPaintDAG.BODY));
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.OVERLAY, GridPaintDAG.FIXED));
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.OVERLAY, GridPaintDAG.HEADER));
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.OVERLAY, GridPaintDAG.FOOTER));
		assertFalse(GridPaintDAG.dependsOn(GridPaintDAG.BODY, GridPaintDAG.OVERLAY));
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
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.OVERLAY, GridPaintDAG.BODY));
		assertTrue(GridPaintDAG.zIndex(GridPaintDAG.OVERLAY) > GridPaintDAG.zIndex(GridPaintDAG.FIXED));
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
	@Test
	public void lazyPriorityFrontierPeeksAndPollsHighestWithoutMutatingSource() {
		int[] source = {3, 7, 3, 1, 9, 9, 2};
		int[] original = source.clone();
		GridLazyIntPriorityFrontier frontier = GridLazyIntPriorityFrontier.maxHeap(source);

		assertArrayEquals(original, source);
		assertEquals(7, frontier.size());
		assertEquals(9, frontier.peek());
		assertEquals("peek must not remove the top priority", 7, frontier.size());

		int[] drained = new int[source.length];
		for (int index = 0; index < drained.length; index++) {
			drained[index] = frontier.poll();
		}
		assertArrayEquals(new int[] {9, 9, 7, 3, 3, 2, 1}, drained);
		assertTrue(frontier.isEmpty());
	}

	@Test
	public void lazyPriorityFrontierMatchesDescendingPrimitiveSortAndDuplicateSkipping() {
		int[] source = new int[4_096];
		for (int index = 0; index < source.length; index++) {
			source[index] = (index * 1_103_515_245 + 12_345) & 1_023;
		}
		int[] expected = source.clone();
		Arrays.sort(expected);

		GridLazyIntPriorityFrontier frontier = GridLazyIntPriorityFrontier.maxHeap(source);
		for (int index = expected.length - 1; index >= 0; index--) {
			assertEquals(expected[index], frontier.poll());
		}
		assertTrue(frontier.isEmpty());

		GridLazyIntPriorityFrontier duplicates =
				GridLazyIntPriorityFrontier.maxHeap(new int[] {3, 5, 3, 9, 5, 1, 9});
		int previous = -1;
		int[] unique = new int[7];
		int uniqueCount = 0;
		while (!duplicates.isEmpty()) {
			int value = duplicates.poll();
			if (value == previous) {
				continue;
			}
			unique[uniqueCount++] = value;
			previous = value;
		}
		assertArrayEquals(new int[] {9, 5, 3, 1}, Arrays.copyOf(unique, uniqueCount));
	}


}
