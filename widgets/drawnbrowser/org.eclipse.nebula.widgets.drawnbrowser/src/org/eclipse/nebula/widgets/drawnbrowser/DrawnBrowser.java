/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser;

import org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument;
import org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlLayout;
import org.eclipse.nebula.widgets.drawnbrowser.internal.ImageStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * A browser that SWT draws itself: HTML becomes one drawn, scrolled view instead of a control per element. Only
 * the blocks of the damaged area are painted. Scrolling moves the window's own pixels ({@link #scroll}) and repaints
 * only the band that comes into view; the page is laid out lazily, as far down as it is shown.
 * <p>
 * The API keeps the names of {@link org.eclipse.swt.browser.Browser}: {@link #setText(String)},
 * {@link #getText()}, {@link #addLocationListener(LocationListener)}. A click on a link sends
 * {@link LocationListener#changing} (a listener may veto it with {@code doit = false}), then
 * {@link LocationListener#changed}. Supported HTML: p, div, h1-h6, br, b/strong, i/em, u, s/strike/del, a, ul, ol, li,
 * img (data:, file:, http: and https: sources, loaded off the UI thread when their block is shown); script and style
 * are ignored.
 * </p>
 * <p>
 * Pattern: prasser/swtbrowser (Apache-2.0, 92120bef61) builds a StyledText per block; this widget draws the same
 * tags in one viewport.
 * </p>
 */
public class DrawnBrowser extends Canvas {

	private static final int LINE = 16;

	private String html = "";
	private HtmlDocument document = HtmlDocument.parse("");
	private HtmlLayout layout;
	/* images of every page shown, loaded when their block is painted */
	private final ImageStore images;
	private int scrollY;
	private final java.util.List<LocationListener> locationListeners = new java.util.ArrayList<>();

	/**
	 * @param parent a composite
	 * @param style  the Canvas styles; {@link SWT#V_SCROLL} is always added
	 */
	@SuppressWarnings("this-escape") // an SWT widget registers its listeners while it is constructed
	public DrawnBrowser(Composite parent, int style) {
		super(parent, style | SWT.V_SCROLL | SWT.NO_BACKGROUND);
		layout = newLayout();
		images = new ImageStore(getDisplay(), 64, this::imageLoaded);
		Listener listener = this::handle;
		for (int type : new int[] { SWT.Paint, SWT.Resize, SWT.MouseUp, SWT.MouseMove, SWT.MouseWheel, SWT.KeyDown, SWT.Dispose }) {
			addListener(type, listener);
		}
		getVerticalBar().addListener(SWT.Selection, e -> scrollTo(getVerticalBar().getSelection()));
	}

	/** Shows {@code text} as HTML; answers true, as {@link org.eclipse.swt.browser.Browser#setText(String)} does. */
	public boolean setText(String text) {
		checkWidget();
		if (text == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		html = text;
		document = HtmlDocument.parse(text);
		layout.dispose();
		layout = newLayout();
		scrollY = 0;
		relayout();
		return true;
	}

	/** The HTML last given to {@link #setText(String)}. */
	public String getText() {
		checkWidget();
		return html;
	}

	public void addLocationListener(LocationListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		locationListeners.add(listener);
	}

	public void removeLocationListener(LocationListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		locationListeners.remove(listener);
	}

	/** The vertical scroll position in pixels. */
	public int getScrollY() {
		checkWidget();
		return scrollY;
	}

	/** Scrolls so that page y {@code y} is at the top (clamped). */
	public void setScrollY(int y) {
		checkWidget();
		scrollTo(y);
	}

	private HtmlLayout newLayout() {
		Color link = getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND);
		return new HtmlLayout(getDisplay(), document, getFont(), link);
	}

	private void handle(Event e) {
		switch (e.type) {
			case SWT.Paint -> paint(e.gc, e.x, e.y, e.width, e.height);
			case SWT.Resize -> relayout();
			case SWT.MouseUp -> {
				if (e.button == 1) click(e.x, e.y);
			}
			case SWT.MouseMove -> setCursor(layout.linkAt(e.x, e.y + scrollY) != null ? getDisplay().getSystemCursor(SWT.CURSOR_HAND) : null);
			case SWT.MouseWheel -> {
				scrollTo(scrollY - e.count * LINE);
				e.doit = false;
			}
			case SWT.KeyDown -> key(e);
			case SWT.Dispose -> {
				layout.dispose();
				images.dispose();
			}
			default -> {
			}
		}
	}

	private void relayout() {
		Rectangle area = getClientArea();
		layout.setWidth(Math.max(1, area.width));
		layout.ensure(scrollY + area.height);
		updateBar();
		scrollY = Math.max(0, Math.min(scrollY, maxScroll()));
		redraw();
	}

	private int maxScroll() {
		return Math.max(0, layout.totalHeight() - getClientArea().height);
	}

	private void updateBar() {
		ScrollBar bar = getVerticalBar();
		int page = Math.max(1, getClientArea().height);
		bar.setValues(scrollY, 0, Math.max(page, layout.totalHeight()), page, LINE, page);
	}

	/* moves the window's pixels by the scroll delta; only the band that comes into view is damaged and repainted */
	private void scrollTo(int y) {
		layout.ensure(Math.max(0, y) + getClientArea().height);
		int target = Math.max(0, Math.min(y, maxScroll()));
		int delta = target - scrollY;
		if (delta == 0) {
			updateBar();
			return;
		}
		scrollY = target;
		Rectangle area = getClientArea();
		if (Math.abs(delta) >= area.height) {
			redraw();
		} else if (delta > 0) {
			scroll(0, 0, 0, delta, area.width, area.height - delta, false);
		} else {
			scroll(0, -delta, 0, 0, area.width, area.height + delta, false);
		}
		updateBar();
	}

	private void paint(GC gc, int x, int y, int width, int height) {
		gc.setBackground(getBackground());
		gc.fillRectangle(x, y, width, height);
		if (document.blockCount == 0) return;
		gc.setForeground(getForeground());
		int pageTop = y + scrollY, pageBottom = pageTop + height;
		for (int b = layout.blockAt(pageTop); b >= 0 && b < document.blockCount && layout.top(b) < pageBottom; b++) {
			int top = layout.top(b) - scrollY + HtmlLayout.BLOCK_GAP, left = layout.indent(b);
			if (document.kind[b] == HtmlDocument.IMAGE) {
				int i = document.image[b], w = layout.shownWidth(i), h = layout.shownHeight(i);
				Image image = images.get(document.source[i]);
				if (image != null && !image.isDisposed()) {
					Rectangle size = image.getBounds();
					gc.drawImage(image, 0, 0, size.width, size.height, left, top, w, h);
				} else {
					gc.drawRectangle(left, top, w - 1, h - 1);
				}
				continue;
			}
			TextLayout text = layout.textLayout(b);
			if (document.kind[b] == HtmlDocument.LIST_ITEM) {
				String marker = document.marker[b] == HtmlDocument.BULLET ? "\u2022" : document.marker[b] + ".";
				gc.drawString(marker, left - gc.stringExtent(marker).x - 6, top, true);
			}
			text.draw(gc, left, top);
		}
	}

	/* an image arrived: its natural size may change its block, and the blocks below move */
	private void imageLoaded(String source, Image image) {
		if (isDisposed()) return;
		Rectangle size = image.getBounds();
		boolean moved = false;
		for (int i = 0; i < document.imageCount; i++) {
			if (source.equals(document.source[i])) moved |= layout.setNaturalSize(i, size.width, size.height);
		}
		if (moved) relayout();
		else redraw();
	}

	private void click(int x, int y) {
		String location = layout.linkAt(x, y + scrollY);
		if (location == null) return;
		LocationEvent changing = new LocationEvent(this);
		changing.location = location;
		changing.top = true;
		changing.doit = true;
		for (LocationListener listener : locationListeners.toArray(new LocationListener[0])) listener.changing(changing);
		if (!changing.doit) return;
		LocationEvent changed = new LocationEvent(this);
		changed.location = location;
		changed.top = true;
		for (LocationListener listener : locationListeners.toArray(new LocationListener[0])) listener.changed(changed);
	}

	private void key(Event e) {
		int page = Math.max(LINE, getClientArea().height - LINE);
		switch (e.keyCode) {
			case SWT.ARROW_DOWN -> scrollTo(scrollY + LINE);
			case SWT.ARROW_UP -> scrollTo(scrollY - LINE);
			case SWT.PAGE_DOWN -> scrollTo(scrollY + page);
			case SWT.PAGE_UP -> scrollTo(scrollY - page);
			case SWT.HOME -> scrollTo(0);
			case SWT.END -> {
				layout.ensure(Integer.MAX_VALUE);
				scrollTo(maxScroll());
			}
			default -> {
			}
		}
	}
}
