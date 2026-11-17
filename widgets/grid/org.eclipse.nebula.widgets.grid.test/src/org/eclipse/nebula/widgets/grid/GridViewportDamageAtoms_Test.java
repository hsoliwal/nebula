/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.eclipse.swt.graphics.Rectangle;
import org.junit.Test;

/**
 * Differential oracle for the allocation-free viewport paint atoms.
 *
 * <p>Original rectangle-based expressions are intentionally retained only in
 * this test. They must agree with the primitive-coordinate production path,
 * including zero-sized bounds and the integer overflow semantics of SWT.</p>
 */
public class GridViewportDamageAtoms_Test {

	private static final int[] COORDINATES = {
			Integer.MIN_VALUE, -2048, -1, 0, 1, 37, 512, Integer.MAX_VALUE
	};
	private static final int[] EXTENTS = {
			-16, -1, 0, 1, 15, 480, Integer.MAX_VALUE
	};

	@Test
	public void headerFooterAndPaintPlaneAtomsMatchOriginalExpressions() {
		Random random = new Random(0x4d334e4542554c41L);
		for (int sample = 0; sample < 4096; sample++) {
			Rectangle client = rectangle(random);
			Rectangle clip = sample % 31 == 0 ? client : rectangle(random);
			int header = EXTENTS[random.nextInt(EXTENTS.length)];
			int footer = EXTENTS[random.nextInt(EXTENTS.length)];
			assertEquals("header sample " + sample,
					originalHeader(clip, client, header),
					GridViewportDamage.intersectsHeader(clip, client, header));
			assertEquals("footer sample " + sample,
					originalFooter(clip, client, footer),
					GridViewportDamage.intersectsFooter(clip, client, footer));
			for (int bits = 0; bits < 16; bits++) {
				boolean headerVisible = (bits & 1) != 0;
				boolean footerVisible = (bits & 2) != 0;
				boolean fixed = (bits & 4) != 0;
				boolean overlay = (bits & 8) != 0;
				assertEquals("plan sample " + sample + " flags " + bits,
						originalPlan(clip, client, header, footer,
								headerVisible, footerVisible, fixed, overlay),
						GridPaintDAG.plan(clip, client, header, footer,
								headerVisible, footerVisible, fixed, overlay));
			}
		}
	}

	@Test
	public void visibleHeaderFooterBoundariesRemainExclusive() {
		Rectangle client = new Rectangle(10, 20, 200, 100);
		assertEquals(originalHeader(new Rectangle(10, 36, 10, 1), client, 16),
				GridViewportDamage.intersectsHeader(new Rectangle(10, 36, 10, 1), client, 16));
		assertEquals(originalFooter(new Rectangle(10, 103, 10, 1), client, 16),
				GridViewportDamage.intersectsFooter(new Rectangle(10, 103, 10, 1), client, 16));
	}

	private static Rectangle rectangle(Random random) {
		return new Rectangle(
				COORDINATES[random.nextInt(COORDINATES.length)],
				COORDINATES[random.nextInt(COORDINATES.length)],
				EXTENTS[random.nextInt(EXTENTS.length)],
				EXTENTS[random.nextInt(EXTENTS.length)]);
	}

	private static boolean originalHeader(Rectangle clip, Rectangle client, int height) {
		return height > 0 && clip.intersects(new Rectangle(client.x, client.y,
				client.width, Math.min(height, client.height)));
	}

	private static boolean originalFooter(Rectangle clip, Rectangle client, int height) {
		if (height <= 0) {
			return false;
		}
		int visible = Math.min(height, client.height);
		return clip.intersects(new Rectangle(client.x, client.y + client.height - visible,
				client.width, visible));
	}

	private static int originalPlan(Rectangle clip, Rectangle client, int headerHeight,
			int footerHeight, boolean headerVisible, boolean footerVisible,
			boolean fixedOverlay, boolean transientOverlay) {
		if (clip.isEmpty() || client.isEmpty() || !clip.intersects(client)) {
			return 0;
		}
		int result = GridPaintDAG.BACKGROUND;
		int header = headerVisible ? Math.max(0, headerHeight) : 0;
		int footer = footerVisible ? Math.max(0, footerHeight) : 0;
		int top = client.y + Math.max(0, header);
		int bottom = client.y + client.height - Math.max(0, footer);
		Rectangle body = new Rectangle(client.x, top, client.width, Math.max(0, bottom - top));
		if (body.width > 0 && body.height > 0 && clip.intersects(body)) {
			result |= GridPaintDAG.BODY;
			if (fixedOverlay) {
				result |= GridPaintDAG.FIXED;
			}
		}
		if (headerVisible && originalHeader(clip, client, header)) {
			result |= GridPaintDAG.HEADER;
		}
		if (footerVisible && originalFooter(clip, client, footer)) {
			result |= GridPaintDAG.FOOTER;
		}
		if (transientOverlay) {
			result |= GridPaintDAG.OVERLAY;
		}
		return result;
	}
}
