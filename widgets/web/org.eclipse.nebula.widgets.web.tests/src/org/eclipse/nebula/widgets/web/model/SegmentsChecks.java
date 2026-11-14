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
// Generated fromtemplates/checks/SEGMENTS.checks.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import static org.eclipse.nebula.widgets.web.model.WebChecks.check;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/** Family contract of the ordered segments (pattern SEGMENTS-1). */
public final class SegmentsChecks {
	private SegmentsChecks() {
	}

	public static void run(SegmentsModel m) {
		m.layout(360, 240);
		WebPaint p = new WebPaint();
		check(m.count() > 0, "segments");
		m.select(m.count() - 1);
		check(m.current() == m.count() - 1 && m.is(m.current(), SELECTED), "select");
		check(m.mode != SegmentsModel.STEPPER || m.is(0, DONE), "earlier steps done");
		check(m.labelOf(0) != null, "label");
		m.paint(p);
		m.setRtl(true);
		m.paint(p);
		m.setRtl(false);
		keys(m, p);
		pages(m, p);
	}

	private static void keys(SegmentsModel m, WebPaint p) {
		m.setFocus(0);
		m.key(m.isRtl() ? KEY_LEFT : KEY_RIGHT, 0, 0);
		m.key(KEY_DOWN, 0, 0);
		m.key(KEY_ENTER, 0, 0);
		m.key(KEY_ENTER, 0, 0);
		m.key(KEY_PAGE_DOWN, 0, 0);
		m.key(KEY_PAGE_UP, 0, 0);
		m.pointer(P_MOVE, m.itemRect(0, 0) + 2, m.itemRect(0, 1) + 2, 0);
		m.paint(p);
		m.pointer(P_WHEEL, 0, 1, 0);
		m.pointer(P_WHEEL, 0, -1, 0);
		m.select(-1);
		check(m.current() == -1, "no selection");
		m.paint(p);
	}

	/** R at family level: a long run of text-less segments shows 1-based numbers and only a resident window. */
	private static void pages(SegmentsModel m, WebPaint p) {
		m.load("50");
		m.layout(360, 240);
		check(m.mode != SegmentsModel.PAGINATION || (m.count() == 50 && "50".equals(m.labelOf(49))), "pages");
		m.select(m.count() - 1);
		check(m.window(1) >= m.count() || m.window(1) > m.current(), "selection revealed");
		m.paint(p);
		m.reveal(0);
		check(m.scrollOffset() == 0, "reveal first");
	}
}
