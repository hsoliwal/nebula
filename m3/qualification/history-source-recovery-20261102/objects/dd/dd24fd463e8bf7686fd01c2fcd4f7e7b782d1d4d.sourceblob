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

/** Package-private invalidation atom for Grid viewport body/header/footer planes. */
final class GridViewportDamage {

	private GridViewportDamage() {
	}

	static Rectangle scrollDamage(
			Rectangle clientArea, int headerHeight, int footerHeight, boolean horizontal) {
		if (clientArea == null) throw new IllegalArgumentException("clientArea");
		if (horizontal) {
			return new Rectangle(clientArea.x, clientArea.y, clientArea.width, clientArea.height);
		}
		int top = clientArea.y + Math.max(0, headerHeight);
		int bottom = clientArea.y + clientArea.height - Math.max(0, footerHeight);
		return new Rectangle(clientArea.x, top, clientArea.width, Math.max(0, bottom - top));
	}

	static boolean intersectsHeader(Rectangle clip, Rectangle clientArea, int headerHeight) {
		if (headerHeight <= 0) return false;
		Rectangle header = new Rectangle(
				clientArea.x, clientArea.y, clientArea.width, Math.min(headerHeight, clientArea.height));
		return clip.intersects(header);
	}

	static boolean intersectsFooter(Rectangle clip, Rectangle clientArea, int footerHeight) {
		if (footerHeight <= 0) return false;
		int height = Math.min(footerHeight, clientArea.height);
		Rectangle footer = new Rectangle(
				clientArea.x, clientArea.y + clientArea.height - height, clientArea.width, height);
		return clip.intersects(footer);
	}
}
