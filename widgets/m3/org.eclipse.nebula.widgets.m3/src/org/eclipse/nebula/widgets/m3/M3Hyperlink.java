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
package org.eclipse.nebula.widgets.m3;

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.M3HiddenGate;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A hyperlink: drawn link text that underlines on hover, notifies Selection on click, Enter or Space, and remembers
 * whether it was visited (the SWT counterpart of SwingX <code>JXHyperlink</code>, re-derived from its documented
 * behaviour; SWT's Link covers links inside a sentence, this covers a link as a stand-alone action).
 * <p>
 * Every change is a delta: hover, focus and visited repaint the text rectangle only; a text change repaints the union
 * of the old and new text rectangles; a resize repaints only the newly exposed strip (the text is left-aligned).
 * While hidden, changes do no native work and the link paints once when shown.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Events: <code>Selection</code>. Startup switch:
 * <code>-Dswt.widget.mode.M3Hyperlink=original</code> repaints the whole control on every change.
 * </p>
 */
public class M3Hyperlink extends Canvas {

	static final boolean VIEWPORT = WidgetMode.of("M3Hyperlink", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final int PAD = 2;

	private final M3HiddenGate gate;
	private String text = ""; //$NON-NLS-1$
	private int textWidth, textHeight = -1;
	private boolean visited, hover, pressed;
	private RGB unvisitedRgb, visitedRgb = new RGB(85, 26, 139);
	private Color unvisitedColor, visitedColor;

	/**
	 * Creates an unvisited link with empty text.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3Hyperlink(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | (VIEWPORT ? SWT.NO_REDRAW_RESIZE : 0));
		gate = new M3HiddenGate(this, null);
		unvisitedRgb = getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND).getRGB();
		setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		addListener(SWT.Paint, this::paint);
		addListener(SWT.MouseEnter, e -> setHover(true));
		addListener(SWT.MouseExit, e -> setHover(false));
		addListener(SWT.MouseDown, e -> pressed = e.button == 1);
		addListener(SWT.MouseUp, e -> {
			if (pressed && e.button == 1 && getTextBounds().contains(e.x, e.y)) {
				activate(e);
			}
			pressed = false;
		});
		addListener(SWT.KeyDown, e -> {
			if (e.character == '\r' || e.character == ' ') {
				activate(e);
			}
		});
		addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
				e.doit = true;
			}
		});
		addListener(SWT.FocusIn, e -> repaintText());
		addListener(SWT.FocusOut, e -> repaintText());
		addListener(SWT.Dispose, e -> disposeColors());
		getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent event) {
				event.result = text;
			}
		});
	}

	/**
	 * Sets the link text; repaints the union of the old and new text rectangles.
	 *
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the text is null
	 */
	public void setText(String text) {
		checkWidget();
		if (text == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (this.text.equals(text)) {
			return;
		}
		Rectangle before = textWidth >= 0 && textHeight >= 0 ? getTextBounds() : null;
		this.text = text;
		textWidth = -1;
		if (showing()) {
			measure();
			Rectangle after = getTextBounds();
			repaint(before == null ? after : before.union(after));
		}
	}

	/** @return the link text, never null */
	public String getText() {
		checkWidget();
		return text;
	}

	/** Marks the link visited or unvisited and repaints its text. */
	public void setVisited(boolean visited) {
		checkWidget();
		if (this.visited != visited) {
			this.visited = visited;
			repaintText();
		}
	}

	/** @return true after the link was activated (or set visited) */
	public boolean isVisited() {
		checkWidget();
		return visited;
	}

	/**
	 * Sets the colours of unvisited and visited text.
	 *
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if a colour is null
	 */
	public void setLinkColors(RGB unvisited, RGB visited) {
		checkWidget();
		if (unvisited == null || visited == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		unvisitedRgb = unvisited;
		visitedRgb = visited;
		disposeColors();
		repaintText();
	}

	/** @return the text rectangle in client coordinates */
	public Rectangle getTextBounds() {
		checkWidget();
		if (textWidth < 0 || textHeight < 0) {
			measure();
		}
		Rectangle client = getClientArea();
		return new Rectangle(client.x, client.y + Math.max(0, (client.height - textHeight - 2 * PAD) / 2), textWidth + 2 * PAD,
				textHeight + 2 * PAD);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		measure();
		Rectangle trim = computeTrim(0, 0, wHint == SWT.DEFAULT ? textWidth + 2 * PAD : wHint,
				hHint == SWT.DEFAULT ? textHeight + 2 * PAD : hHint);
		return new Point(trim.width, trim.height);
	}

	private void activate(Event cause) {
		setVisited(true);
		Event event = new Event();
		event.stateMask = cause.stateMask;
		notifyListeners(SWT.Selection, event);
	}

	private void setHover(boolean value) {
		if (hover != value) {
			hover = value;
			repaintText();
		}
	}

	private void measure() {
		GC gc = new GC(this);
		Point extent = gc.textExtent(text, SWT.DRAW_TRANSPARENT);
		gc.dispose();
		textWidth = extent.x;
		textHeight = extent.y;
		M3Counters.measures++;
	}

	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	private void repaintText() {
		if (showing()) {
			repaint(getTextBounds());
		}
	}

	private void repaint(Rectangle area) {
		if (VIEWPORT) {
			redraw(area.x, area.y, area.width, area.height, false);
		} else {
			M3Counters.fullRepaints++;
			redraw();
		}
	}

	private void disposeColors() {
		if (unvisitedColor != null) {
			unvisitedColor.dispose();
			visitedColor.dispose();
			unvisitedColor = visitedColor = null;
		}
	}

	private void paint(Event event) {
		GC gc = event.gc;
		gc.setBackground(getBackground());
		gc.fillRectangle(event.x, event.y, event.width, event.height);
		if (textWidth < 0) {
			measure();
		}
		if (unvisitedColor == null) {
			unvisitedColor = new Color(getDisplay(), unvisitedRgb);
			visitedColor = new Color(getDisplay(), visitedRgb);
		}
		Rectangle bounds = getTextBounds();
		gc.setForeground(visited ? visitedColor : unvisitedColor);
		gc.drawText(text, bounds.x + PAD, bounds.y + PAD, true);
		if (hover) {
			int baseline = bounds.y + PAD + gc.getFontMetrics().getAscent() + 1;
			gc.drawLine(bounds.x + PAD, baseline, bounds.x + PAD + textWidth - 1, baseline);
		}
		if (isFocusControl()) {
			gc.drawFocus(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}
}
