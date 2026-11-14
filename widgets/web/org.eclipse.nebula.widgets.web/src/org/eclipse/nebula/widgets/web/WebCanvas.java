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
// Generated fromtemplates/nebula/WebCanvas.java.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web;

import org.eclipse.nebula.widgets.web.model.WebModel;
import org.eclipse.nebula.widgets.web.model.WebPaint;
import org.eclipse.nebula.widgets.web.model.WebStates;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * One drawn Canvas per web widget (pattern CORE-1, SWT side). The control owns no widget
 * state: it maps SWT events to neutral model keys and pointer kinds, repaints only the model's
 * damage, records damage without painting while the part is hidden ({@code SWT.PartVisible} /
 * {@code SWT.PartHidden} when the SWT build has them, else Show/Hide), and replays the model's
 * display list through the GC. Colours and fonts are cached per widget and disposed with it.
 *
 * @param <M> the lane model
 */
public class WebCanvas<M extends WebModel> extends Canvas {
	static final int PART_VISIBLE = eventType("PartVisible"), PART_HIDDEN = eventType("PartHidden");
	private static final int[] EVENTS = { SWT.Paint, SWT.Resize, SWT.KeyDown, SWT.MouseMove, SWT.MouseDown, SWT.MouseUp, SWT.MouseExit,
			SWT.MouseVerticalWheel, SWT.FocusIn, SWT.FocusOut, SWT.Traverse, SWT.Dispose, SWT.Show, SWT.Hide };
	private static final int[] SWT_KEYS = { SWT.ARROW_LEFT, SWT.ARROW_RIGHT, SWT.ARROW_UP, SWT.ARROW_DOWN, SWT.HOME, SWT.END, SWT.PAGE_UP,
			SWT.PAGE_DOWN, SWT.CR, ' ', SWT.ESC, SWT.TAB, SWT.DEL, SWT.BS };
	protected final M model;
	private final WebPaint paint = new WebPaint();
	private final int[] damage = new int[4];
	private final WebCanvasCache cache;
	private final Runnable ticker = this::tick;
	private boolean partVisible = true, pending, ticking;

	public WebCanvas(Composite parent, int style, M model) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		this.model = model;
		this.cache = new WebCanvasCache(getDisplay());
		model.setDelta(this::changed);
		for (int type : EVENTS) {
			addListener(type, this::handle);
		}
		if (PART_VISIBLE != 0) {
			addListener(PART_VISIBLE, this::handle);
			addListener(PART_HIDDEN, this::handle);
		}
		initAccessible();
	}

	static int eventType(String name) {
		try {
			return SWT.class.getField(name).getInt(null);
		} catch (ReflectiveOperationException e) {
			return 0;
		}
	}

	/** The lane model; all widget behaviour is model API. */
	public M getModel() {
		checkWidget();
		return model;
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		model.layout(wHint == SWT.DEFAULT ? 320 : wHint, hHint == SWT.DEFAULT ? 0 : hHint);
		Point p = new Point(wHint == SWT.DEFAULT ? model.prefWidth() : wHint, hHint == SWT.DEFAULT ? model.prefHeight() : hHint);
		model.layout(getSize().x, getSize().y);
		return p;
	}

	private void handle(Event e) {
		switch (e.type) {
		case SWT.Paint: onPaint(e); break;
		case SWT.Resize: model.layout(getSize().x, getSize().y); break;
		case SWT.KeyDown: onKey(e); break;
		case SWT.Traverse: onTraverse(e); break;
		case SWT.FocusIn: case SWT.FocusOut: model.setState(-1, WebStates.FOCUS, e.type == SWT.FocusIn); break;
		case SWT.Dispose: cache.dispose(); break;
		case SWT.Show: setPartVisible(true); break;
		case SWT.Hide: setPartVisible(false); break;
		default: onOther(e);
		}
	}

	private void onOther(Event e) {
		if (e.type == PART_VISIBLE || e.type == PART_HIDDEN) {
			setPartVisible(e.type == PART_VISIBLE);
		} else {
			onMouse(e);
		}
	}

	private void onMouse(Event e) {
		int mods = mods(e.stateMask);
		switch (e.type) {
		case SWT.MouseMove: model.pointer((e.stateMask & SWT.BUTTON1) != 0 ? WebStates.P_DRAG : WebStates.P_MOVE, e.x, e.y, mods); break;
		case SWT.MouseDown: forceFocus(); model.pointer(WebStates.P_DOWN, e.x, e.y, mods); break;
		case SWT.MouseUp: model.pointer(WebStates.P_UP, e.x, e.y, mods); break;
		case SWT.MouseExit: model.pointer(WebStates.P_EXIT, e.x, e.y, mods); break;
		default: model.pointer(WebStates.P_WHEEL, e.x, -e.count, mods);
		}
	}

	private void onKey(Event e) {
		int key = WebStates.KEY_CHAR;
		for (int i = 0; i < SWT_KEYS.length; i++) {
			if (e.keyCode == SWT_KEYS[i]) {
				key = i + 1;
			}
		}
		if (key != WebStates.KEY_CHAR || e.character >= ' ') {
			e.doit = !model.key(key, e.character, mods(e.stateMask));
		}
	}

	private void onTraverse(Event e) {
		switch (e.detail) {
		case SWT.TRAVERSE_ARROW_NEXT: case SWT.TRAVERSE_ARROW_PREVIOUS: case SWT.TRAVERSE_RETURN: case SWT.TRAVERSE_ESCAPE:
			e.doit = false; break;
		case SWT.TRAVERSE_TAB_NEXT: case SWT.TRAVERSE_TAB_PREVIOUS:
			e.doit = !model.key(WebStates.KEY_TAB, 0, e.detail == SWT.TRAVERSE_TAB_PREVIOUS ? WebStates.MOD_SHIFT : 0); break;
		default: break;
		}
	}

	private static int mods(int stateMask) {
		return ((stateMask & SWT.SHIFT) != 0 ? WebStates.MOD_SHIFT : 0) | ((stateMask & SWT.MOD1) != 0 ? WebStates.MOD_CTRL : 0)
				| ((stateMask & SWT.ALT) != 0 ? WebStates.MOD_ALT : 0);
	}

	/** Model delta: damage-only redraw while visible; while hidden only remember that damage exists. */
	private void changed(int item, int mask) {
		if (isDisposed()) {
			return;
		}
		if (!partVisible) {
			pending = true;
			return;
		}
		int dy = model.takeBlit();
		if (dy != 0) {
			Point size = getSize();
			boolean h = model.scrollsHorizontally();
			int o = model.viewOrigin(), len = model.viewLength();
			int source = o + Math.max(0, -dy), span = len - Math.abs(dy);
			if (h) {
				scroll(source + dy, 0, source, 0, span, size.y, false);
			} else {
				scroll(0, source + dy, 0, source, size.x, span, false);
			}
		}
		if (model.takeDamage(damage)) {
			redraw(damage[0], damage[1], damage[2], damage[3], false);
		}
		if (model.isAnimating() && !ticking) {
			ticking = true;
			getDisplay().timerExec(16, ticker);
		}
	}

	/** PartVisible-aware hook: one flush and one paint when the part becomes visible. */
	protected void setPartVisible(boolean visible) {
		partVisible = visible;
		if (visible && pending) {
			pending = false;
			changed(-1, 0);
		}
	}

	public boolean isPartVisible() {
		return partVisible;
	}

	private void tick() {
		ticking = false;
		if (!isDisposed() && partVisible && model.tick(16)) {
			ticking = true;
			getDisplay().timerExec(16, ticker);
		}
	}

	private void onPaint(Event e) {
		GC gc = e.gc;
		gc.setAntialias(SWT.ON);
		gc.setBackground(cache.color(model.theme().slot(0)));
		gc.fillRectangle(e.x, e.y, e.width, e.height);
		model.paint(paint);
		try (PaintClip clipping = new PaintClip(gc)) {
			for (int i = 0; i < paint.count(); i++) {
				if (paint.intersects(i, e.x, e.y, e.width, e.height)) {
					replay(gc, i, e, clipping);
				}
			}
		}
	}

	private void replay(GC gc, int i, Event e, PaintClip clipping) {
		int x = paint.get(i, WebPaint.F_X), y = paint.get(i, WebPaint.F_Y), w = paint.get(i, WebPaint.F_W), h = paint.get(i, WebPaint.F_H);
		int arg = paint.get(i, WebPaint.F_ARG);
		Color c = cache.color(paint.get(i, WebPaint.F_RGB));
		gc.setBackground(c);
		gc.setForeground(c);
		switch (paint.get(i, WebPaint.F_OP)) {
		case WebPaint.RECT: gc.fillRectangle(x, y, w, h); break;
		case WebPaint.ROUND: gc.fillRoundRectangle(x, y, w, h, 2 * arg, 2 * arg); break;
		case WebPaint.ROUND_LINE: gc.drawRoundRectangle(x, y, w - 1, h - 1, 2 * arg, 2 * arg); break;
		case WebPaint.OVAL: gc.fillOval(x, y, w, h); break;
		case WebPaint.OVAL_LINE: gc.drawOval(x, y, w - 1, h - 1); break;
		case WebPaint.LINE: gc.drawLine(x, arg == 0 ? y : y + h, x + w, arg == 0 ? y + h : y); break;
		default: replayRest(gc, i, x, y, w, h, arg, e, clipping);
		}
	}

	private void replayRest(GC gc, int i, int x, int y, int w, int h, int arg, Event e, PaintClip clipping) {
		switch (paint.get(i, WebPaint.F_OP)) {
		case WebPaint.TEXT: text(gc, paint.text(paint.get(i, WebPaint.F_REF)), x, y, w, h, arg, e, clipping); break;
		case WebPaint.GRADIENT: gc.setBackground(cache.color(arg)); gc.fillGradientRectangle(x, y, w, h, false); break;
		case WebPaint.ARC: gc.drawArc(x, y, w, h, arg, paint.get(i, WebPaint.F_REF)); break;
		default: gc.drawPolyline(points(i, arg));
		}
	}

	private int[] points(int i, int n) {
		int[] xy = new int[2 * n]; // TODO CORE-1: a reusable scratch array per point count
		int off = paint.get(i, WebPaint.F_REF);
		for (int k = 0; k < xy.length; k++) {
			xy[k] = paint.point(off + k);
		}
		return xy;
	}

	private void text(GC gc, String s, int x, int y, int w, int h, int style, Event e, PaintClip clipping) {
		gc.setFont(cache.font(getFont(), style));
		Point ext = gc.textExtent(s);
		int tx = (style & WebPaint.CENTER) != 0 ? x + (w - ext.x) / 2 : (style & WebPaint.RIGHT) != 0 ? x + w - ext.x : x;
		int ty = y + (h - ext.y) / 2;
		Throwable primary = null;
		try {
			clipping.apply(x, y, w, h);
			gc.drawText(s, tx, ty, true);
			if ((style & WebPaint.UNDERLINE) != 0) {
				gc.drawLine(tx, ty + ext.y - 1, tx + ext.x, ty + ext.y - 1);
			}
		} catch (RuntimeException | Error failure) {
			primary = failure;
			throw failure;
		} finally {
			try {
				clipping.restore();
			} catch (RuntimeException | Error cleanup) {
				if (primary == null) {
					throw cleanup;
				}
				if (primary != cleanup) {
					primary.addSuppressed(cleanup);
				}
			}
		}
	}

	/** One frame scope; native scratch is lazy and shared by its text records only. */
	private static final class PaintClip implements AutoCloseable {
		private final GC gc;
		private org.eclipse.swt.graphics.Region originalClip, workingClip;
		private org.eclipse.swt.graphics.Transform originalTransform;
		private boolean captured, originallyClipped;

		PaintClip(GC gc) {
			this.gc = gc;
		}

		void apply(int x, int y, int w, int h) {
			captured = false;
			if (originalClip == null) {
				originalClip = new org.eclipse.swt.graphics.Region(gc.getDevice());
			}
			if (workingClip == null) {
				workingClip = new org.eclipse.swt.graphics.Region(gc.getDevice());
			}
			if (originalTransform == null) {
				originalTransform = new org.eclipse.swt.graphics.Transform(gc.getDevice());
			}
			originallyClipped = gc.isClipped();
			gc.getTransform(originalTransform);
			if (originalTransform.isIdentity()) {
				gc.getClipping(originalClip);
			} else {
				try {
					gc.setTransform(null);
					gc.getClipping(originalClip);
				} finally {
					gc.setTransform(originalTransform);
				}
			}
			captured = true;
			gc.setClipping(x, y, w, h);
			if (originalTransform.isIdentity()) {
				gc.getClipping(workingClip);
				workingClip.intersect(originalClip);
				gc.setClipping(workingClip);
			} else {
				try {
					gc.setTransform(null);
					gc.getClipping(workingClip);
					workingClip.intersect(originalClip);
					gc.setClipping(workingClip);
				} finally {
					gc.setTransform(originalTransform);
				}
			}
		}

		void restore() {
			if (!captured) {
				return;
			}
			try {
				if (originalTransform.isIdentity()) {
					gc.setClipping(originallyClipped ? originalClip : null);
				} else {
					try {
						gc.setTransform(null);
						gc.setClipping(originallyClipped ? originalClip : null);
					} finally {
						gc.setTransform(originalTransform);
					}
				}
			} finally {
				captured = false;
			}
		}

		void dispose() {
			try {
				restore();
			} finally {
				try {
					if (originalTransform != null) {
						originalTransform.dispose();
					}
				} finally {
					try {
						if (workingClip != null) {
							workingClip.dispose();
						}
					} finally {
						if (originalClip != null) {
							originalClip.dispose();
						}
					}
				}
			}
		}

		@Override
		public void close() {
			dispose();
		}
	}

	private void initAccessible() {
		getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result = model.accessibleName(e.childID == ACC.CHILDID_SELF ? -1 : e.childID);
			}
		});
		getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getRole(AccessibleControlEvent e) {
				e.detail = WebCanvasCache.accRole(WebStates.roleName(model.role()));
			}
		});
	}

	/** Font data of the widget font scaled to a pixel size (used by the cache). */
	static FontData scaled(Font base, int px, int style) {
		FontData fd = base.getFontData()[0];
		String name = (style & WebPaint.MONO) != 0 ? "Consolas" : fd.getName();
		return new FontData(name, Math.max(6, px * 3 / 4), (style & WebPaint.BOLD) != 0 ? SWT.BOLD : SWT.NORMAL);
	}
}
