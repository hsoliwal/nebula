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
 * including zero-sized bounds and long-coordinate endpoint semantics.</p>
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
					Boolean.valueOf(originalHeader(clip, client, header)),
					Boolean.valueOf(GridViewportDamage.intersectsHeader(clip, client, header)));
			assertEquals("footer sample " + sample,
					Boolean.valueOf(originalFooter(clip, client, footer)),
					Boolean.valueOf(GridViewportDamage.intersectsFooter(clip, client, footer)));
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
		assertEquals(Boolean.valueOf(originalHeader(new Rectangle(10, 36, 10, 1), client, 16)),
				Boolean.valueOf(GridViewportDamage.intersectsHeader(new Rectangle(10, 36, 10, 1), client, 16)));
		assertEquals(Boolean.valueOf(originalFooter(new Rectangle(10, 103, 10, 1), client, 16)),
				Boolean.valueOf(GridViewportDamage.intersectsFooter(new Rectangle(10, 103, 10, 1), client, 16)));
	}

	private static Rectangle rectangle(Random random) {
		return new Rectangle(
				COORDINATES[random.nextInt(COORDINATES.length)],
				COORDINATES[random.nextInt(COORDINATES.length)],
				EXTENTS[random.nextInt(EXTENTS.length)],
				EXTENTS[random.nextInt(EXTENTS.length)]);
	}

	private static boolean originalHeader(Rectangle clip, Rectangle client, int height) {
        return height > 0 && independentOverlap(clip, client.x, client.y,
                client.width, Math.min(height, client.height));
    }

    private static boolean originalFooter(Rectangle clip, Rectangle client, int height) {
        if (height <= 0) return false;
        long footer = Math.min(height, client.height);
        return independentOverlap(clip, client.x,
                (long) client.y + client.height - footer, client.width, footer);
    }

    private static int originalPlan(Rectangle clip, Rectangle client, int headerHeight,
            int footerHeight, boolean headerVisible, boolean footerVisible,
            boolean fixedOverlay, boolean transientOverlay) {
        if (!independentOverlap(clip, client.x, client.y, client.width, client.height)) {
            return 0;
        }
        int result = GridPaintDAG.BACKGROUND;
        long header = headerVisible ? Math.max(0, headerHeight) : 0;
        long footer = footerVisible ? Math.max(0, footerHeight) : 0;
        if (independentOverlap(clip, client.x, (long) client.y + header,
                client.width, client.height - header - footer)) {
            result |= GridPaintDAG.BODY;
            if (fixedOverlay) result |= GridPaintDAG.FIXED;
        }
        if (headerVisible && originalHeader(clip, client, headerHeight)) result |= GridPaintDAG.HEADER;
        if (footerVisible && originalFooter(clip, client, footerHeight)) result |= GridPaintDAG.FOOTER;
        if (transientOverlay) result |= GridPaintDAG.OVERLAY;
        return result;
    }

    /** Independently compare widened half-open endpoints without SWT Rectangle.intersects. */
    private static boolean independentOverlap(Rectangle clip, long x, long y, long width, long height) {
        return clip.width > 0 && clip.height > 0 && width > 0 && height > 0
                && (long) clip.x < x + width && x < (long) clip.x + clip.width
                && (long) clip.y < y + height && y < (long) clip.y + clip.height;
    }

    @Test
    public void extremeViewportScrollRectanglesNeverWrapTheSignedPixelAxis() {
        Random random = new Random(0x20261123L);
        for (int caseId = 0; caseId < 16384; caseId++) {
            int origin = COORDINATES[random.nextInt(COORDINATES.length)];
            int height = EXTENTS[random.nextInt(EXTENTS.length)];
            int header = EXTENTS[random.nextInt(EXTENTS.length)];
            int footer = EXTENTS[random.nextInt(EXTENTS.length)];
            Rectangle client = new Rectangle(20, origin, 120, height);
            Rectangle actual = GridViewportDamage.scrollDamage(client, header, footer, false);
            long top = (long) origin + Math.max(0, header);
            long bottom = (long) origin + height - Math.max(0, footer);
            long expectedTop = Math.min(top, Integer.MAX_VALUE);
            long expectedBottom = Math.min(bottom, (long) Integer.MAX_VALUE + 1);
            long expectedHeight = top > Integer.MAX_VALUE ? 0
                    : Math.max(0, expectedBottom - expectedTop);
            assertEquals("top " + caseId, expectedTop, actual.y);
            assertEquals("height " + caseId, expectedHeight, actual.height);
            assertEquals("width " + caseId, 120, actual.width);
        }
    }

    @Test
    public void positiveExtremaPlanesAgreeWithWideHalfOpenOracle() {
        Random random = new Random(0x574944454cL);
        for (int caseId = 0; caseId < 4096; caseId++) {
            Rectangle client = rectangle(random);
            Rectangle clip = rectangle(random);
            int header = EXTENTS[random.nextInt(EXTENTS.length)];
            int footer = EXTENTS[random.nextInt(EXTENTS.length)];
            for (int flags = 0; flags < 16; flags++) {
                int expected = originalPlan(clip, client, header, footer,
                        (flags & 1) != 0, (flags & 2) != 0,
                        (flags & 4) != 0, (flags & 8) != 0);
                int actual = GridPaintDAG.plan(clip, client, header, footer,
                        (flags & 1) != 0, (flags & 2) != 0,
                        (flags & 4) != 0, (flags & 8) != 0);
                assertEquals("wide plan " + caseId + " flags " + flags, expected, actual);
            }
        }
    }

}
