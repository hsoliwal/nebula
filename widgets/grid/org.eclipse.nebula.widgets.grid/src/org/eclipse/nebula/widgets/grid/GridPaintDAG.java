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
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import org.eclipse.swt.graphics.Rectangle;

/**
 * Paint-plane dependency atom for Grid.
 *
 * <p>The mask is intentionally primitive: callers can decide plane residency
 * without allocating command objects on every SWT.Paint. The z-order is
 * background -> body -> fixed overlay -> chrome -> transient overlay.
 * Header and footer share a chrome rank; their actual extents are defined by
 * the Grid layout, and paint planning conservatively handles both even if
 * an unusually small client area lets those extents overlap.</p>
 */
final class GridPaintDAG {

	static final int BACKGROUND = 1 << 0;
	static final int BODY = 1 << 1;
	static final int FIXED = 1 << 2;
	static final int HEADER = 1 << 3;
	static final int FOOTER = 1 << 4;
	static final int OVERLAY = 1 << 5;

	private GridPaintDAG() {
	}

	static int plan(Rectangle clip, Rectangle clientArea, int headerHeight, int footerHeight,
			boolean headerVisible, boolean footerVisible, boolean fixedOverlayActive,
			boolean transientOverlayActive) {
        if (clip == null) {
            throw new IllegalArgumentException("clip");
        }
        if (clientArea == null) {
            throw new IllegalArgumentException("clientArea");
        }
        // The native Rectangle endpoints are int, but x + width and
        // y + height need wide arithmetic for signed coordinate extremes.
        if (clip.isEmpty() || clientArea.isEmpty()
                || !GridViewportDamage.intersectsRegion(clip, clientArea.x,
                        clientArea.y, clientArea.width, clientArea.height)) {
            return 0;
        }

        int plan = BACKGROUND;
        int header = headerVisible ? Math.max(0, headerHeight) : 0;
        int footer = footerVisible ? Math.max(0, footerHeight) : 0;
        long bodyTop = (long) clientArea.y + header;
        long bodyBottom = (long) clientArea.y + clientArea.height - footer;
        if (GridViewportDamage.intersectsRegion(clip, clientArea.x, bodyTop,
                clientArea.width, bodyBottom - bodyTop)) {
            plan |= BODY;
            if (fixedOverlayActive) {
                plan |= FIXED;
            }
        }
        if (headerVisible && GridViewportDamage.intersectsHeader(clip, clientArea, header)) {
            plan |= HEADER;
        }
        if (footerVisible && GridViewportDamage.intersectsFooter(clip, clientArea, footer)) {
            plan |= FOOTER;
        }
        if (transientOverlayActive) {
            plan |= OVERLAY;
        }
        return plan;
    }

	static boolean includes(int plan, int plane) {
		return (plan & plane) != 0;
	}

	static int zIndex(int plane) {
		return switch (plane) {
			case BACKGROUND -> 0;
			case BODY -> 1;
			case FIXED -> 2;
			case HEADER, FOOTER -> 3;
			case OVERLAY -> 4;
			default -> -1;
		};
	}

	static boolean dependsOn(int plane, int dependency) {
		int planeZ = zIndex(plane);
		int dependencyZ = zIndex(dependency);
		return planeZ >= 0 && dependencyZ >= 0 && planeZ > dependencyZ;
	}
}
