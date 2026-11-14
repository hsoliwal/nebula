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
// Generated fromtemplates/family/CONTAINER.java.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;


import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/**
 * Containers with a header (pattern CONTAINER-1): vertical sections, each a header text, a body
 * text and optionally a value with a delta; an open bit per section; dismissible alerts; elevation.
 * The header strip of the first section is the HEADER plane. Members: card, accordion, alert-banner,
 * empty-state, stat-kpi.
 */
public class ContainerModel extends WebModel implements ContainerApi {
	public static final int CARD = 0, ACCORDION = 1, ALERT = 2, EMPTY = 3, STAT = 4;
	private boolean exclusive;

	public ContainerModel(String concept, int role, int mode, int shape, int variant) {
		super(concept, role, mode, shape, variant);
	}

	@Override
	public int aspects() {
		return A_D | A_P;
	}

	@Override
	public void load(String sample) {
		clear();
		for (String s : sample.split(";")) {
			String[] f = s.split("~", -1);
			int i = add(f[0], f.length > 1 ? f[1] : null);
			if (mode == STAT && f.length > 2) {
				setValue(i, WebStates.parse(f[1]));
				setAux(i, WebStates.parse(f[2]));
			}
			if (mode != ACCORDION) {
				setState(i, OPEN, true);
			}
		}
	}

	@Override
	public void setExclusive(boolean on) {
		exclusive = on;
	}

	@Override
	public boolean isExclusive() {
		return exclusive;
	}

	@Override
	public boolean isOpen(int item) {
		return is(item, OPEN);
	}

	@Override
	public void setOpen(int item, boolean open) {
		if (open && exclusive) {
			for (int i = 0; i < count; i++) {
				flags[i] &= i == item ? ~0 : ~OPEN;
			}
		}
		setState(item, OPEN, open);
		relayout(item, D_LAYOUT);
	}

	@Override
	public void dismiss(int item) {
		setState(item, HIDDEN, true);
		relayout(item, D_ACTION);
	}

	@Override
	protected boolean vertical() {
		return true;
	}

	/** Header height of a section. */
	protected int header() {
		return mode == STAT ? rowHeight() * 3 : mode == EMPTY ? rowHeight() * 3 : rowHeight() + pad();
	}

	/** Body height of an open section: wrapped lines of the detail text. */
	protected int body(int i) {
		if ((flags[i] & OPEN) == 0 || detail[i] == null || mode == STAT) {
			return 0;
		}
		int perLine = Math.max(1, (width - 2 * pad()) / Math.max(1, font() * 55 / 100));
		return ((detail[i].length() + perLine - 1) / perLine) * (font() + 6) + pad();
	}

	@Override
	protected void doLayout() {
		int y = 0;
		for (int i = 0; i < count; i++) {
			int h = (flags[i] & HIDDEN) != 0 ? 0 : header() + body(i);
			setRect(i, 0, y, width, h);
			y += h == 0 ? 0 : h + gap();
		}
		setPlane(HEADER, 0, 0, width, count > 0 ? header() : 0);
	}

	@Override
	protected void emit(WebPaint p) {
		for (int i = 0; i < count; i++) {
			if ((flags[i] & HIDDEN) == 0) {
				int b = i * 4;
				section(p, i, rects[b], rects[b + 1], rects[b + 2], rects[b + 3]);
			}
		}
	}

	private void section(WebPaint p, int i, int x, int y, int w, int h) {
		if (mode == CARD || mode == STAT || mode == EMPTY) {
			p.round(x, y + 2, w, h, radius(), theme.shadow());
		}
		if (mode == ALERT) {
			alert(p, i, x, y, w, h);
		} else {
			surface(p, i, x, y, w, h);
		}
		switch (mode) {
		case STAT: stat(p, i, x, y, w); break;
		case EMPTY: empty(p, i, x, y, w); break;
		default: titled(p, i, x, y, w, h);
		}
	}

	private void alert(WebPaint p, int i, int x, int y, int w, int h) {
		int c = theme.fill(variant == V_DEFAULT ? V_INFO : variant, 0);
		p.round(x, y, w, h, radius(), WebTheme.mix(c, theme.slot(WebTheme.SURFACE), 14));
		p.rect(x, y, 4, h, c);
		WebGlyphs.glyph(p, WebGlyphs.I_CLOSE, x + w - rowHeight(), y + pad() / 2, rowHeight() - pad() / 2, theme.slot(WebTheme.MUTED));
	}

	private void titled(WebPaint p, int i, int x, int y, int w, int h) {
		int hh = header();
		p.text(x + pad(), y, w - 2 * pad() - rowHeight(), hh, theme.slot(WebTheme.FG), font() | WebPaint.BOLD | (rtl ? WebPaint.RIGHT : 0), text[i]);
		if (mode == ACCORDION) {
			int g = (flags[i] & OPEN) != 0 ? WebGlyphs.I_DOWN : rtl ? WebGlyphs.I_PREV : WebGlyphs.I_NEXT;
			WebGlyphs.glyph(p, g, rtl ? x + pad() / 2 : x + w - rowHeight(), y + (hh - rowHeight()) / 2, rowHeight(), theme.slot(WebTheme.MUTED));
			p.line(x, y + hh, x + w, y + hh, theme.slot(WebTheme.BORDER));
		}
		if (h > hh) {
			muted(p, detail[i], x + pad(), y + hh, w - 2 * pad(), h - hh, 0);
		}
	}

	private void stat(WebPaint p, int i, int x, int y, int w) {
		int r = rowHeight();
		muted(p, text[i], x + pad(), y + pad() / 2, w - 2 * pad(), r, 0);
		p.text(x + pad(), y + r, w - 2 * pad(), r, theme.slot(WebTheme.FG), Math.min(255, font() * 2) | WebPaint.BOLD, Integer.toString(value[i]));
		boolean up = aux[i] >= 0;
		int c = theme.slot(up ? WebTheme.SUCCESS : WebTheme.DANGER);
		WebGlyphs.glyph(p, up ? WebGlyphs.I_UP : WebGlyphs.I_DOWN, x + pad(), y + 2 * r, r * 2 / 3, c);
		p.text(x + pad() + r, y + 2 * r, w - 2 * pad() - r, r * 2 / 3, c, font(), (up ? "+" : "") + aux[i] + "%");
	}

	private void empty(WebPaint p, int i, int x, int y, int w) {
		int r = rowHeight();
		p.oval(x + w / 2 - r / 2, y + pad() / 2, r, r, WebTheme.mix(theme.slot(WebTheme.ACCENT), theme.slot(WebTheme.SURFACE), 15));
		WebGlyphs.glyph(p, WebGlyphs.I_SEARCH, x + w / 2 - r / 2, y + pad() / 2, r, theme.slot(WebTheme.ACCENT));
		p.text(x, y + r + pad() / 2, w, r, theme.slot(WebTheme.FG), font() | WebPaint.BOLD | WebPaint.CENTER, text[i]);
		muted(p, detail[i], x, y + 2 * r, w, r, WebPaint.CENTER);
	}

	@Override
	protected void activate(int item) {
		if (mode == ACCORDION) {
			setOpen(item, !is(item, OPEN));
		}
		fire(item, D_ACTION);
	}

	@Override
	protected void activateAt(int item, int x, int y) {
		boolean onClose = rtl ? x < rowHeight() : x > rects[item * 4 + 2] - rowHeight();
		if (mode == ALERT && onClose && y < rowHeight() + pad()) {
			dismiss(item);
		} else {
			activate(item);
		}
	}

	@Override
	protected boolean onEscape() {
		if (mode == ALERT && focus >= 0) {
			dismiss(focus);
			return true;
		}
		return false;
	}
}
