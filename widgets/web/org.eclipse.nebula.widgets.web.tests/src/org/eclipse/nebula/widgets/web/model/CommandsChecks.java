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
// Generated fromtemplates/checks/COMMANDS.checks.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import static org.eclipse.nebula.widgets.web.model.WebChecks.check;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/** Family contract of the grouped command buttons (pattern COMMANDS-1). */
public final class CommandsChecks {
	private CommandsChecks() {
	}

	public static void run(CommandsModel m) {
		m.layout(480, 160);
		WebPaint p = new WebPaint();
		m.setCaptions("Clipboard", "Font");
		m.setKind(m.count() - 1, CommandsModel.TOGGLE);
		m.setFocus(m.count() - 1);
		m.key(KEY_ENTER, 0, 0);
		check(m.is(m.count() - 1, CHECKED), "toggle command");
		m.paint(p);
		m.setRtl(true);
		m.paint(p);
		clickArrow(m, p);
		m.setRtl(false);
		clickArrow(m, p);
		m.setKind(0, CommandsModel.DROPDOWN);
		click(m, 0, 2);
		m.setKind(0, CommandsModel.PUSH);
		click(m, 0, 2);
		menuKeys(m, p);
	}

	private static void clickArrow(CommandsModel m, WebPaint p) {
		m.setKind(0, CommandsModel.SPLIT_KIND);
		int w = m.itemRect(0, 2), h = m.itemRect(0, 3);
		click(m, 0, m.isRtl() ? 2 : w - 2);
		check(m.isMenuOpen() || h == 0, "arrow opens the menu");
		m.paint(p);
		check(m.key(KEY_ESCAPE, 0, 0) && !m.isMenuOpen(), "escape closes the menu");
		click(m, 0, m.isRtl() ? w - 2 : 2);
		m.paint(p);
	}

	private static void click(CommandsModel m, int item, int dx) {
		int x = m.itemRect(item, 0) + dx, y = m.itemRect(item, 1) + 2;
		m.pointer(P_DOWN, x, y, 0);
		m.pointer(P_UP, x, y, 0);
	}

	private static void menuKeys(CommandsModel m, WebPaint p) {
		m.setMenuOpen(false);
		m.setFocus(0);
		boolean opened = m.key(KEY_DOWN, 0, 0);
		check(opened == (m.mode == CommandsModel.SPLIT), "down opens a split menu");
		m.paint(p);
		if (m.count() > 1 && m.isMenuOpen()) {
			m.key(KEY_ENTER, 0, 0);
			check(!m.isMenuOpen(), "choosing a menu item closes the menu");
		}
		check(!m.key(KEY_ESCAPE, 0, 0), "escape without a menu");
	}
}
