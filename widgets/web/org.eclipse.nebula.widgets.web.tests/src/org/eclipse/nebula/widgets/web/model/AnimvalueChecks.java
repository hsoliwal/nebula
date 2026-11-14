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
// Generated fromtemplates/checks/ANIMVALUE.checks.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import static org.eclipse.nebula.widgets.web.model.WebChecks.check;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/** Family contract of the animated values (pattern ANIMVALUE-1). */
public final class AnimvalueChecks {
	private AnimvalueChecks() {
	}

	public static void run(AnimvalueModel m) {
		m.layout(360, 200);
		WebPaint p = new WebPaint();
		m.load("title;avatar;line;line");
		check(m.mode == AnimvalueModel.TRANSITION || m.isAnimating(), "loops start on load");
		for (int i = 0; i < 70; i++) {
			m.tick(16);
			m.paint(p);
		}
		m.setMax(0);
		check(m.max() == 1, "max is at least 1");
		m.setMax(100);
		m.setProgress(140);
		check(m.value(0) == 100, "progress clamps");
		m.paint(p);
		m.setProgress(0);
		transition(m, p);
		m.clear();
		m.setProgress(5);
	}

	private static void transition(AnimvalueModel m, WebPaint p) {
		check(m.eased() >= 0 && m.eased() <= 1000, "eased range");
		m.setFocus(0);
		m.key(KEY_ENTER, 0, 0);
		for (int i = 0; i < 100 && m.tick(16); i++) {
			m.paint(p);
		}
		check(m.mode != AnimvalueModel.TRANSITION || m.eased() == 1000, "transition reaches the end");
		m.play();
		for (int i = 0; i < 100 && m.tick(16); i++) {
			m.paint(p);
		}
		check(m.mode != AnimvalueModel.TRANSITION || m.eased() == 0, "transition runs back");
	}
}
