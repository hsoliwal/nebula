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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;

/**
 * A task pane: a drawn title bar (image, title, chevron) over an {@link M3CollapsiblePane}; clicking the title bar
 * collapses or expands the content (the SWT counterpart of SwingX <code>JXTaskPane</code>, re-derived from its
 * documented behaviour). Stack task panes in an {@link M3TaskPaneContainer}.
 * <p>
 * Every change is a delta: a title or image change repaints the title bar only; a hover repaints the title bar only;
 * collapsing slides the content (measured once, never laid out per frame) and repaints only the chevron; a collapsed
 * content is hidden, so the viewports inside it fold their deltas until it expands. A width change of the title bar
 * repaints the chevron at its old and new place and the newly exposed strip. While the title bar is hidden it does no
 * native work and paints once when shown.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Events: <code>Expand</code> and <code>Collapse</code> when a state change completes.
 * Startup switch: <code>-Dswt.widget.mode.M3TaskPane=original</code> repaints the whole title bar on every change.
 * </p>
 */
public class M3TaskPane extends Composite {

	static final boolean VIEWPORT = WidgetMode.of("M3TaskPane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final int PAD = 6, CHEVRON = 12;

	private final Canvas header;
	private final M3CollapsiblePane body;
	private final M3HiddenGate gate;
	private String title = ""; //$NON-NLS-1$
	private Image image;
	private boolean special, hover;
	private int headerHeight = -1, headerWidth = -1;

	/**
	 * Creates an expanded task pane with an empty title.
	 *
	 * @param parent the parent, usually an {@link M3TaskPaneContainer}
	 * @param style BORDER
	 */
	public M3TaskPane(Composite parent, int style) {
		super(parent, style);
		header = new Canvas(this, SWT.DOUBLE_BUFFERED | (VIEWPORT ? SWT.NO_REDRAW_RESIZE : 0));
		gate = new M3HiddenGate(header, null);
		body = new M3CollapsiblePane(this, SWT.VERTICAL);
		super.setLayout(new PaneLayout());
		header.addListener(SWT.Paint, this::paintHeader);
		header.addListener(SWT.Resize, e -> headerResized());
		header.addListener(SWT.MouseDown, e -> {
			if (e.button == 1) {
				setCollapsed(!isCollapsed());
			}
		});
		header.addListener(SWT.MouseEnter, e -> setHover(true));
		header.addListener(SWT.MouseExit, e -> setHover(false));
		body.addListener(SWT.Expand, e -> stateChanged(SWT.Expand));
		body.addListener(SWT.Collapse, e -> stateChanged(SWT.Collapse));
	}

	/** The pane lays out its title bar and content itself; a layout cannot be set. */
	@Override
	public void setLayout(Layout layout) {
		checkWidget();
	}

	/** @return the composite to put the task controls in */
	public Composite getContent() {
		checkWidget();
		return body.getContent();
	}

	/** @return the drawn title bar (clicking it toggles the pane) */
	public Control getTitleBar() {
		checkWidget();
		return header;
	}

	/**
	 * Sets the title and repaints the title bar.
	 *
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the title is null
	 */
	public void setTitle(String title) {
		checkWidget();
		if (title == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (!this.title.equals(title)) {
			this.title = title;
			repaintHeader();
		}
	}

	/** @return the title, never null */
	public String getTitle() {
		checkWidget();
		return title;
	}

	/** Sets the image left of the title (not disposed by the pane), or null. */
	public void setImage(Image image) {
		checkWidget();
		this.image = image;
		headerHeight = -1;
		repaintHeader();
	}

	/** @return the image, or null */
	public Image getImage() {
		checkWidget();
		return image;
	}

	/** Emphasizes the title bar (selection colours), as a primary task group. */
	public void setSpecial(boolean special) {
		checkWidget();
		if (this.special != special) {
			this.special = special;
			repaintHeader();
		}
	}

	/** @return true when the title bar is emphasized */
	public boolean isSpecial() {
		checkWidget();
		return special;
	}

	/** Collapses or expands the content (animated when {@link #isAnimated()}). */
	public void setCollapsed(boolean collapsed) {
		checkWidget();
		body.setCollapsed(collapsed);
		repaintChevron();
	}

	/** @return true when collapsed or collapsing */
	public boolean isCollapsed() {
		checkWidget();
		return body.isCollapsed();
	}

	/** Turns the collapse animation on or off. */
	public void setAnimated(boolean animated) {
		checkWidget();
		body.setAnimated(animated);
	}

	/** @return true when collapsing is animated */
	public boolean isAnimated() {
		checkWidget();
		return body.isAnimated();
	}

	/** @return true while the content slides */
	public boolean isAnimating() {
		checkWidget();
		return body.isAnimating();
	}

	/** @return the chevron square in title bar coordinates */
	public Rectangle getChevronBounds() {
		checkWidget();
		Point size = header.getSize();
		return new Rectangle(Math.max(0, size.x - PAD - CHEVRON), Math.max(0, (size.y - CHEVRON) / 2), CHEVRON, CHEVRON);
	}

	private void stateChanged(int type) {
		repaintChevron();
		notifyListeners(type, new Event());
	}

	private void setHover(boolean value) {
		if (hover != value) {
			hover = value;
			repaintHeader();
		}
	}

	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	private void repaintHeader() {
		if (showing()) {
			M3Counters.fullRepaints += VIEWPORT ? 0 : 1;
			header.redraw();
		}
	}

	private void repaintChevron() {
		if (!showing()) {
			return;
		}
		if (VIEWPORT) {
			Rectangle chevron = getChevronBounds();
			header.redraw(chevron.x, chevron.y, chevron.width, chevron.height, false);
		} else {
			M3Counters.fullRepaints++;
			header.redraw();
		}
	}

	/* a width change moves only the right-aligned chevron: damage its old and new place (the exposed strip is damaged by
	 * the window system) */
	private void headerResized() {
		Point size = header.getSize();
		int old = headerWidth;
		headerWidth = size.x;
		if (!VIEWPORT || old < 0 || !gate.visible()) {
			return;
		}
		int y = Math.max(0, (size.y - CHEVRON) / 2);
		header.redraw(Math.max(0, old - PAD - CHEVRON), y, CHEVRON, CHEVRON, false);
		header.redraw(Math.max(0, size.x - PAD - CHEVRON), y, CHEVRON, CHEVRON, false);
	}

	private int headerHeight() {
		if (headerHeight < 0) {
			GC gc = new GC(header);
			int text = gc.getFontMetrics().getHeight();
			gc.dispose();
			int icon = image == null ? 0 : image.getBounds().height;
			headerHeight = Math.max(Math.max(text, icon), CHEVRON) + 2 * PAD;
			M3Counters.measures++;
		}
		return headerHeight;
	}

	private void paintHeader(Event event) {
		GC gc = event.gc;
		Point size = header.getSize();
		gc.setBackground(getDisplay().getSystemColor(special ? SWT.COLOR_LIST_SELECTION : SWT.COLOR_WIDGET_LIGHT_SHADOW));
		gc.fillRectangle(event.x, event.y, event.width, event.height);
		Color foreground = getDisplay().getSystemColor(special ? SWT.COLOR_LIST_SELECTION_TEXT : SWT.COLOR_WIDGET_FOREGROUND);
		gc.setForeground(foreground);
		int x = PAD;
		if (image != null) {
			Rectangle bounds = image.getBounds();
			gc.drawImage(image, x, Math.max(0, (size.y - bounds.height) / 2));
			x += bounds.width + PAD;
		}
		int textHeight = gc.getFontMetrics().getHeight();
		gc.drawText(title, x, Math.max(0, (size.y - textHeight) / 2), true);
		Rectangle chevron = getChevronBounds();
		int cx = chevron.x + CHEVRON / 2, cy = chevron.y + CHEVRON / 2, d = isCollapsed() ? 3 : -3;
		gc.setLineWidth(hover ? 2 : 1);
		gc.drawLine(cx - 4, cy - d, cx, cy + d);
		gc.drawLine(cx, cy + d, cx + 4, cy - d);
		gc.setLineWidth(1);
	}

	/* title bar on top at its measured height, the collapsible content below; the content is measured by the body */
	private final class PaneLayout extends Layout {

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			Point content = body.computeSize(wHint, SWT.DEFAULT, flushCache);
			int width = wHint != SWT.DEFAULT ? wHint : Math.max(content.x, 120);
			return new Point(width, headerHeight() + content.y);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle client = getClientArea();
			int top = headerHeight();
			header.setBounds(client.x, client.y, client.width, top);
			body.setBounds(client.x, client.y + top, client.width, Math.max(0, client.height - top));
		}
	}
}
