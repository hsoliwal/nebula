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

/** Package-private invalidation atom for Grid viewport body/header/footer planes. */
final class GridViewportDamage {

	private GridViewportDamage() {
	}

	static Rectangle scrollDamage(
			Rectangle clientArea, int headerHeight, int footerHeight, boolean horizontal) {
        if (clientArea == null) {
            throw new IllegalArgumentException("clientArea");
        }
		if (horizontal) {
			return new Rectangle(clientArea.x, clientArea.y, clientArea.width, clientArea.height);
		}
        long top = (long) clientArea.y + Math.max(0, headerHeight);
        long bottom = (long) clientArea.y + clientArea.height - Math.max(0, footerHeight);
        // The body may extend beyond the representable SWT Rectangle pixel axis.
        // Return only the representable part; never wrap into a negative y.
        if (top > Integer.MAX_VALUE) {
            return new Rectangle(clientArea.x, Integer.MAX_VALUE, clientArea.width, 0);
        }
        long representedBottom = Math.min((long) Integer.MAX_VALUE + 1, bottom);
        return new Rectangle(clientArea.x, (int) top, clientArea.width,
                (int) Math.max(0, representedBottom - top));
	}

    /** Strict half-open intersection with endpoints widened before addition. */
    static boolean intersectsRegion(Rectangle clip, long x, long y, long width, long height) {
        return clip.width > 0 && clip.height > 0 && width > 0 && height > 0
                && Math.max((long) clip.x, x) < Math.min((long) clip.x + clip.width, x + width)
                && Math.max((long) clip.y, y) < Math.min((long) clip.y + clip.height, y + height);
    }

	static boolean intersectsHeader(Rectangle clip, Rectangle clientArea, int headerHeight) {
        if (headerHeight <= 0) {
            return false;
        }
        return intersectsRegion(clip, clientArea.x, clientArea.y, clientArea.width,
                Math.min(headerHeight, clientArea.height));
    }

	static boolean intersectsFooter(Rectangle clip, Rectangle clientArea, int footerHeight) {
        if (footerHeight <= 0) {
            return false;
        }
        int height = Math.min(footerHeight, clientArea.height);
        return intersectsRegion(clip, clientArea.x,
                (long) clientArea.y + clientArea.height - height, clientArea.width, height);
    }
}
