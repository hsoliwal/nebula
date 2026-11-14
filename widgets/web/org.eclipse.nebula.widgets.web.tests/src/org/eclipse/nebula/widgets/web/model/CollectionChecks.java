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
// Generated fromtemplates/checks/COLLECTION.checks.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import static org.eclipse.nebula.widgets.web.model.WebChecks.check;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/** Family contract of the virtual collections (pattern COLLECTION-1). */
public final class CollectionChecks {
	private CollectionChecks() {
	}

	public static void run(CollectionModel m) {
		m.layout(360, 240);
		WebPaint p = new WebPaint();
		for (int i = 0; i < 40; i++) {
			m.add("row " + i);
		}
		m.setGroup(1, 1);
		m.layout(360, 240);
		m.paint(p);
		keys(m, p);
		drag(m, p);
		loading(m, p);
		m.setFilter("row 1");
		check(m.logicalCount() > 0 && m.logicalCount() < m.count(), "filter compacts positions");
		m.paint(p);
		m.setFilter("");
		m.setRtl(true);
		m.paint(p);
		m.setRtl(false);
	}

	private static void keys(CollectionModel m, WebPaint p) {
		m.setFocus(0);
		m.key(KEY_DOWN, 0, MOD_ALT);
		check(m.focus() == 1 || m.mode == CollectionModel.GALLERY, "alt+down reorders");
		m.key(KEY_UP, 0, MOD_ALT);
		m.key(KEY_UP, 0, MOD_ALT);
		m.key(KEY_RIGHT, 0, MOD_CTRL);
		m.key(KEY_LEFT, 0, MOD_CTRL);
		m.key(KEY_PAGE_DOWN, 0, 0);
		m.key(KEY_PAGE_UP, 0, 0);
		m.key(KEY_DELETE, 0, 0);
		m.paint(p);
		m.key(KEY_ENTER, 0, 0);
		m.key(KEY_DELETE, 0, 0);
		m.setFocus(m.count() - 1);
		m.paint(p);
		m.move(m.count() - 1, 0);
		check(m.focus() == 0, "move keeps focus on the moved item");
		m.moveToColumn(0, -2);
		check(m.group(0) == 0, "column clamps");
		m.setFocus(-1);
		String first = m.text(0);
		m.key(KEY_DOWN, 0, MOD_ALT);
		check(first.equals(m.text(0)), "no focus, no reorder (the key only moves the focus)");
	}

	private static void drag(CollectionModel m, WebPaint p) {
		m.scrollTo(0);
		m.layout(360, 240);
		int x = m.itemRect(0, 0) + 10, y = m.itemRect(0, 1) + 5;
		m.pointer(P_DOWN, x, y, 0);
		m.pointer(P_DRAG, x - 200, y + 50, 0);
		m.paint(p);
		m.pointer(P_UP, x - 200, y + 50, 0);
		m.pointer(P_DOWN, x, y, 0);
		m.pointer(P_DRAG, x - 20, y, 0);
		m.pointer(P_UP, x - 20, y, 0);
		m.swipe(0, -1000);
		check(m.mode != CollectionModel.SWIPE || m.aux(0) < 0, "swipe opens");
		m.paint(p);
		m.pointer(P_DOWN, 300, 230, 0);
		m.pointer(P_UP, 300, 230, 0);
		m.pointer(P_DOWN, -5, -5, 0);
		m.pointer(P_DRAG, 0, 0, 0);
		m.pointer(P_UP, -5, -5, 0);
		m.swipe(0, 0);
	}

	private static void loading(CollectionModel m, WebPaint p) {
		int[] loads = new int[1];
		m.setDelta((item, mask) -> loads[0] += (mask & WebLanes.D_LOAD) != 0 ? 1 : 0);
		m.setThreshold(3);
		m.scrollTo(Long.MAX_VALUE);
		int once = loads[0];
		m.scrollTo(Long.MAX_VALUE - 1);
		check(m.mode != CollectionModel.INFINITE || (m.needsMore() && once <= 1 && loads[0] == once), "one load per item count");
		m.add("more");
		m.scrollTo(Long.MAX_VALUE);
		check(m.mode != CollectionModel.INFINITE || loads[0] == once + 1, "a new item count loads again");
		m.scrollTo(0);
		m.pointer(P_WHEEL, 0, -4, 0);
		m.pointer(P_WHEEL, 0, -4, 0);
		check(m.mode != CollectionModel.PULL || (m.pull() > 0 && m.is(-1, LOADING)), "pull to refresh");
		m.paint(p);
		for (int i = 0; i < 5; i++) {
			m.tick(16);
		}
		m.endRefresh();
		check(m.pull() == 0 && !m.is(-1, LOADING), "refresh ended");
		int y = m.itemRect(0, 1) + 2;
		m.pointer(P_DOWN, 20, y, 0);
		m.pointer(P_DRAG, 20, y + 200, 0);
		m.pointer(P_UP, 20, y + 200, 0);
		m.endRefresh();
		m.setDelta(null);
	}
}
