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
// Generated fromtemplates/checks/OVERLAY.checks.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import static org.eclipse.nebula.widgets.web.model.WebChecks.check;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/** Family contract of the overlay planes (pattern OVERLAY-1): open/close slide on the OVERLAY plane only. */
public final class OverlayChecks {
	private OverlayChecks() {
	}

	public static void run(OverlayModel m) {
		m.layout(400, 300);
		WebPaint p = new WebPaint();
		m.setTarget(30, 30, 60, 20);
		m.setExtent(200);
		m.open();
		check(m.isOpen() && m.isAnimating(), "open animates");
		slideDamage(m);
		m.paint(p);
		check(p.count() > 0, "open plane paints");
		keys(m);
		m.setRtl(true);
		m.open();
		run(m, 80);
		m.setRtl(false);
		m.close();
		run(m, 80);
		check(!m.isOpen() && !m.isAnimating(), "closed");
		m.paint(p);
		outside(m);
	}

	/** P: while sliding, every frame damages at most the plane (plus the backdrop of modal planes). */
	private static void slideDamage(OverlayModel m) {
		int[] d = new int[4];
		m.takeDamage(d);
		m.tick(16);
		if (m.takeDamage(d) && !m.modal()) {
			check(d[2] <= m.plane(WebView.OVERLAY, 2) + Math.abs(m.plane(WebView.OVERLAY, 4)) + 1, "slide damages only the plane");
		}
		run(m, 200);
		check(m.plane(WebView.OVERLAY, 4) == 0 && m.plane(WebView.OVERLAY, 5) == 0, "open plane at rest");
	}

	private static void run(OverlayModel m, int frames) {
		for (int i = 0; i < frames && m.tick(16); i++) {
			check(m.isAnimating(), "running");
		}
	}

	private static void keys(OverlayModel m) {
		m.setTrap(true);
		m.key(KEY_TAB, 0, 0);
		m.key(KEY_TAB, 0, MOD_SHIFT);
		m.setStep(0);
		m.key(KEY_RIGHT, 0, 0);
		check(m.mode != OverlayModel.TOUR || m.step() == 1, "tour next");
		m.key(KEY_LEFT, 0, 0);
		m.setFocus(0);
		m.key(KEY_ENTER, 0, 0);
		m.setStep(m.count() - 1);
		m.key(KEY_ENTER, 0, 0);
		m.setTrap(false);
		m.key(KEY_TAB, 0, 0);
		m.open();
		check(m.key(KEY_ESCAPE, 0, 0) && !m.isOpen(), "escape closes");
		check(!m.key(KEY_ESCAPE, 0, 0), "escape on a closed plane");
		run(m, 200);
	}

	private static void outside(OverlayModel m) {
		m.open();
		run(m, 200);
		m.pointer(P_UP, 1, 1, 0);
		check(!m.modal() || m.hitTest(1, 1) >= 0 || !m.isOpen(), "a click on the backdrop closes modal planes");
		m.open();
		m.close();
		m.close();
	}
}
