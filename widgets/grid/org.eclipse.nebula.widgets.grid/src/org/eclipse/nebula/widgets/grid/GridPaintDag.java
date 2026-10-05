/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import org.eclipse.swt.graphics.Rectangle;

/**
 * Paint-plane dependency atom for Grid.
 *
 * <p>The mask is intentionally primitive: callers can decide plane residency
 * without allocating command objects on every SWT.Paint. The dependency order
 * is background -> body -> fixed overlay -> chrome -> transient overlay.</p>
 */
final class GridPaintDag {

	static final int BACKGROUND = 1 << 0;
	static final int BODY = 1 << 1;
	static final int FIXED = 1 << 2;
	static final int HEADER = 1 << 3;
	static final int FOOTER = 1 << 4;
	static final int OVERLAY = 1 << 5;

	private GridPaintDag() {
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
        if (!clip.intersects(clientArea)) {
            return 0;
        }

		int plan = BACKGROUND;
		int header = headerVisible ? Math.max(0, headerHeight) : 0;
		int footer = footerVisible ? Math.max(0, footerHeight) : 0;
		Rectangle body = GridViewportDamage.scrollDamage(clientArea, header, footer, false);
		if (body.width > 0 && body.height > 0 && clip.intersects(body)) {
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

	static boolean dependsOn(int plane, int dependency) {
        if (plane == BODY) {
            return dependency == BACKGROUND;
        }
        if (plane == FIXED) {
            return dependency == BODY || dependency == BACKGROUND;
        }
        if (plane == HEADER || plane == FOOTER) {
            return dependency == BACKGROUND;
        }
        if (plane == OVERLAY) {
            return dependency == BACKGROUND;
        }
		return false;
	}
}
