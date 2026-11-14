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
// Generated fromtemplates/family/TOKEN.java.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;


import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/**
 * Small token renderers (pattern TOKEN-1): one shape plus one content run per item, laid out
 * as a wrapping row. Modes cover avatar initials, count badges with overflow, icons, removable
 * chips, key caps, highlight marks, inline code, captcha text, LEDs, links and switches.
 * Members: avatar, badge, chip, kbd, highlight-mark, icon, code-block, captcha-security, led-gauge,
 * link, switch.
 */
public class TokenModel extends WebModel implements TokenApi {
	public static final int TEXT = 0, INITIALS = 1, COUNT = 2, ICON = 3, CHIP = 4, KEYCAP = 5, MARK = 6, CODE = 7, CAPTCHA = 8, LED = 9,
			LINK = 10, SWITCH = 11;
	private int max = 99;
	private boolean removable;

	public TokenModel(String concept, int role, int mode, int shape, int variant) {
		super(concept, role, mode, shape, variant);
	}

	@Override
	public int aspects() {
		return A_D;
	}

	@Override
	public void load(String sample) {
		clear();
		for (String s : sample.split(";")) {
			int i = add(s);
			if (mode == COUNT) {
				setValue(i, parse(s));
			} else if (mode == ICON) {
				setAux(i, WebGlyphs.iconId(s));
			}
		}
	}

	static int parse(String s) {
		return WebStates.parse(s);
	}

	@Override
	public void setMax(int m) {
		max = Math.max(1, m);
		relayout(-1, D_VALUE);
	}

	@Override
	public void setRemovable(boolean on) {
		removable = on;
		relayout(-1, D_LAYOUT);
	}

	@Override
	public boolean isRemovable() {
		return removable;
	}

	@Override
	public String display(int item) {
		switch (mode) {
		case INITIALS: return initials(text(item));
		case COUNT: return value(item) > max ? max + "+" : Integer.toString(value(item));
		case LED: case ICON: return "";
		default: return text(item);
		}
	}

	/** Upper-case first letters of the first two words. */
	public static String initials(String name) {
		StringBuilder b = new StringBuilder(2);
		for (String w : name.trim().split("\\s+")) {
			if (!w.isEmpty() && b.length() < 2) {
				b.append(Character.toUpperCase(w.charAt(0)));
			}
		}
		return b.toString();
	}

	@Override
	protected void doLayout() {
		int x = 0, y = 0, h = rowHeight();
		for (int i = 0; i < count; i++) {
			int w = shape == LINE ? Math.max(width, h) : itemWidth(i, h);
			if (x > 0 && x + w > width) {
				x = 0;
				y += h + gap();
			}
			setRect(i, x, y, w, h);
			x += w + gap();
		}
	}

	private int itemWidth(int i, int h) {
		switch (mode) {
		case INITIALS: case LED: case ICON: return h;
		case SWITCH: return 2 * h + gap() + textWidth(text[i], font());
		case LINK: case MARK: return textWidth(display(i), font()) + 2;
		default: return Math.max(h, textWidth(display(i), font()) + 2 * pad() + (removable ? h / 2 : 0));
		}
	}

	@Override
	protected void emit(WebPaint p) {
		for (int i = 0; i < count; i++) {
			if ((flags[i] & HIDDEN) == 0) {
				int b = i * 4;
				emitItem(p, i, rects[b], rects[b + 1], rects[b + 2], rects[b + 3]);
			}
		}
	}

	private void emitItem(WebPaint p, int i, int x, int y, int w, int h) {
		switch (mode) {
		case SWITCH: emitSwitch(p, i, x, y, h); break;
		case LED: emitLed(p, i, x, y, h); break;
		case ICON: WebGlyphs.glyph(p, aux[i], x, y, h, theme.ink(variant, flags[i])); break;
		case KEYCAP: emitKeycap(p, i, x, y, w, h); break;
		case MARK: p.rect(x, y + 2, w, h - 4, WebTheme.mix(theme.slot(WebTheme.WARNING), theme.slot(WebTheme.SURFACE), 35));
			p.text(x, y, w, h, theme.slot(WebTheme.FG), font() | WebPaint.CENTER, text[i]); break;
		case LINK: p.text(x, y, w, h, theme.slot((flags[i] & VISITED) != 0 ? WebTheme.ACCENT2 : WebTheme.ACCENT),
				font() | WebPaint.UNDERLINE | WebPaint.CENTER, text[i]); break;
		case CODE: emitCode(p, i, x, y, w, h); break;
		case CAPTCHA: emitCaptcha(p, i, x, y, w, h); break;
		default: emitShape(p, i, x, y, w, h);
		}
	}

	private void emitShape(WebPaint p, int i, int x, int y, int w, int h) {
		if (shape == LINE) {
			p.line(x, y + h / 2, x + w, y + h / 2, theme.slot(WebTheme.BORDER));
			muted(p, text[i], x, y, w, h, WebPaint.CENTER);
			return;
		}
		surface(p, i, x, y, w, h);
		int tw = removable && mode == CHIP ? w - h / 2 : w;
		label(p, i, display(i), x, y, tw, h, WebPaint.CENTER | (mode == INITIALS ? WebPaint.BOLD : 0));
		if (removable && mode == CHIP) {
			WebGlyphs.glyph(p, WebGlyphs.I_CLOSE, x + w - h * 3 / 4, y + h / 4, h / 2, theme.ink(variant, flags[i]));
		}
	}

	private void emitSwitch(WebPaint p, int i, int x, int y, int h) {
		boolean on = (flags[i] & CHECKED) != 0;
		int t = animating && i == focus ? phase : on ? 1000 : 0;
		int track = on ? theme.fill(variant == V_DEFAULT ? V_PRIMARY : variant, flags[i]) : theme.slot(WebTheme.BORDER);
		p.round(x, y + h / 6, 2 * h, h * 2 / 3, h / 3, track);
		int d = h * 2 / 3 - 4, tx = x + 2 + (2 * h - d - 4) * t / 1000;
		p.oval(tx, y + h / 6 + 2, d, d, theme.slot(WebTheme.SURFACE));
		label(p, i, text[i], x + 2 * h + gap(), y, Math.max(0, rects[i * 4 + 2] - 2 * h - gap()), h, 0);
	}

	private void emitLed(WebPaint p, int i, int x, int y, int h) {
		boolean on = (flags[i] & CHECKED) != 0 || value[i] > 0;
		int c = on ? theme.fill(variant, 0) : theme.slot(WebTheme.MUTED);
		p.oval(x + 2, y + 2, h - 4, h - 4, c);
		p.ovalLine(x, y, h, h, on ? WebTheme.mix(c, theme.slot(WebTheme.SURFACE), 40) : theme.slot(WebTheme.BORDER));
	}

	private void emitKeycap(WebPaint p, int i, int x, int y, int w, int h) {
		int line = theme.line(V_DEFAULT, flags[i]);
		p.round(x, y, w, h, 4, theme.slot(WebTheme.SURFACE));
		p.roundLine(x, y, w, h, 4, line);
		p.line(x + 2, y + h - 1, x + w - 2, y + h - 1, line);
		p.text(x, y, w, h - 2, theme.slot(WebTheme.FG), font() | WebPaint.MONO | WebPaint.CENTER, text[i]);
	}

	private void emitCode(WebPaint p, int i, int x, int y, int w, int h) {
		p.round(x, y, w, h, 6, WebTheme.mix(theme.slot(WebTheme.FG), theme.slot(WebTheme.SURFACE), 7));
		p.text(x + pad(), y, w - 2 * pad(), h, theme.slot(WebTheme.FG), font() | WebPaint.MONO, text[i]);
	}

	private void emitCaptcha(WebPaint p, int i, int x, int y, int w, int h) {
		surface(p, i, x, y, w, h);
		int seed = text[i].hashCode();
		for (int k = 0; k < 3; k++, seed = seed * 1103515245 + 12345) {
			p.line(x, y + Math.floorMod(seed >> 8, h), x + w, y + Math.floorMod(seed >> 16, h), theme.slot(WebTheme.MUTED));
		}
		label(p, i, text[i], x, y, w, h, WebPaint.CENTER | WebPaint.BOLD | WebPaint.MONO);
	}

	@Override
	protected void activate(int item) {
		switch (mode) {
		case CHIP: setState(item, SELECTED, !is(item, SELECTED)); break;
		case SWITCH: case LED: setState(item, CHECKED, !is(item, CHECKED)); focus = item; phase = is(item, CHECKED) ? 0 : 1000;
			animate(); break;
		case LINK: setState(item, VISITED, true); break;
		default: break;
		}
		fire(item, D_ACTION);
	}

	@Override
	protected boolean onKey(int key, int ch, int mods) {
		if (removable && focus >= 0 && (key == KEY_DELETE || key == KEY_BACKSPACE)) {
			remove(focus);
			return true;
		}
		return false;
	}

	@Override
	protected boolean advance(int ms) {
		boolean on = focus >= 0 && focus < count && (flags[focus] & CHECKED) != 0;
		phase = on ? Math.min(1000, phase + ms * 8) : Math.max(0, phase - ms * 8);
		return on ? phase < 1000 : phase > 0;
	}
}
