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
// Generated fromtemplates/checks/TOKEN.checks.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import static org.eclipse.nebula.widgets.web.model.WebChecks.check;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/** Family contract of the token renderers (pattern TOKEN-1), run on every member's sample model. */
public final class TokenChecks {
	private TokenChecks() {
	}

	public static void run(TokenModel m) {
		m.layout(360, 120);
		check(m.count() > 0, "sample items");
		check(TokenModel.initials("ada  lovelace byron").equals("AL") && TokenModel.initials(" ").isEmpty(), "initials");
		check(WebGlyphs.iconId("search") == WebGlyphs.I_SEARCH && WebGlyphs.iconId("nope") == WebGlyphs.I_DOT, "icon ids");
		check(TokenModel.parse(" 12 ") == 12 && TokenModel.parse("x") == 0, "count parse");
		WebPaint p = new WebPaint();
		for (int id = WebGlyphs.I_DOT; id <= WebGlyphs.I_DOWN; id++) {
			WebGlyphs.glyph(p, id, 0, 0, 16, 0);
		}
		m.setMax(1);
		m.setValue(0, 5);
		check(m.mode != TokenModel.COUNT || m.display(0).equals("1+"), "count overflow");
		m.setMax(99);
		activation(m);
		removable(m);
	}

	private static void activation(TokenModel m) {
		m.setFocus(0);
		m.key(KEY_ENTER, 0, 0);
		for (int i = 0; i < 200 && m.tick(16); i++) {
			check(m.isAnimating() || !m.isAnimating(), "tick");
		}
		m.key(KEY_SPACE, 0, 0);
		for (int i = 0; i < 200 && m.tick(16); i++) {
			check(true, "tick back");
		}
		m.setState(0, CHECKED, true);
		WebPaint p = new WebPaint();
		m.paint(p);
		check(p.count() > 0 || m.mode == TokenModel.ICON, "checked paint");
		m.setState(0, CHECKED, false);
		m.setValue(0, 1);
		m.paint(p);
	}

	private static void removable(TokenModel m) {
		int n = m.count();
		m.setRemovable(true);
		check(m.isRemovable(), "removable flag");
		m.layout(40, 200);
		WebPaint p = new WebPaint();
		m.paint(p);
		m.setFocus(0);
		check(m.key(KEY_DELETE, 0, 0) && m.count() == n - 1, "delete removes the focused token");
		m.setRemovable(false);
		check(!m.key(KEY_BACKSPACE, 0, 0), "not removable");
		m.load("ada lovelace;7;search");
		m.layout(360, 120);
		m.paint(p);
	}
}
