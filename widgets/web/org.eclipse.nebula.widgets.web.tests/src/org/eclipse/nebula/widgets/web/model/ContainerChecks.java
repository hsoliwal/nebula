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
// Generated fromtemplates/checks/CONTAINER.checks.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import static org.eclipse.nebula.widgets.web.model.WebChecks.check;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/** Family contract of the containers (pattern CONTAINER-1). */
public final class ContainerChecks {
	private ContainerChecks() {
	}

	public static void run(ContainerModel m) {
		m.layout(360, 400);
		check(m.count() > 0 && m.plane(WebView.HEADER, 3) > 0, "sections and header plane");
		WebPaint p = new WebPaint();
		int n = m.count();
		m.add("Extra", "a long body text that wraps over more than one line in a narrow container of the family");
		m.setOpen(n, true);
		check(m.isOpen(n) && m.itemRect(n, 3) > m.itemRect(0, 3) - 1000, "open section has a body");
		m.setExclusive(true);
		check(m.isExclusive(), "exclusive");
		m.setOpen(0, true);
		check(!m.isOpen(n) || m.mode != ContainerModel.ACCORDION, "exclusive closes the others");
		m.setExclusive(false);
		m.setRtl(true);
		m.paint(p);
		m.setRtl(false);
		keys(m, p);
		dismiss(m);
	}

	private static void keys(ContainerModel m, WebPaint p) {
		m.setFocus(0);
		boolean before = m.isOpen(0);
		m.key(KEY_ENTER, 0, 0);
		check(m.mode != ContainerModel.ACCORDION || m.isOpen(0) != before, "enter toggles a section");
		m.key(KEY_DOWN, 0, 0);
		check(m.focus() == 1, "down moves to the next section");
		m.setValue(0, 42);
		m.setAux(0, -3);
		m.paint(p);
		m.setAux(0, 5);
		m.paint(p);
	}

	private static void dismiss(ContainerModel m) {
		m.layout(360, 400);
		int x = m.itemRect(0, 0) + m.itemRect(0, 2) - 4, y = m.itemRect(0, 1) + 4;
		m.pointer(P_DOWN, x, y, 0);
		m.pointer(P_UP, x, y, 0);
		check(m.mode != ContainerModel.ALERT || m.is(0, HIDDEN), "close glyph dismisses an alert");
		m.setRtl(true);
		m.pointer(P_DOWN, 2, m.itemRect(1, 1) + 4, 0);
		m.pointer(P_UP, 2, m.itemRect(1, 1) + 4, 0);
		m.setRtl(false);
		m.setFocus(1);
		check(m.key(KEY_ESCAPE, 0, 0) == (m.mode == ContainerModel.ALERT), "escape dismisses alerts only");
		m.dismiss(1);
		check(m.is(1, HIDDEN) && m.itemRect(1, 3) == 0, "dismissed section has no height");
	}
}
