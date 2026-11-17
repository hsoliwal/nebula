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
import org.eclipse.nebula.widgets.drawnbrowser.internal.PageHistory;
import org.eclipse.nebula.widgets.drawnbrowser.internal.PageViewport;
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
 * The API keeps the names of {@link org.eclipse.swt.browser.Browser}: {@link #setText(String)}, {@link #setUrl(String)},
 * {@link #getText()}, {@link #getUrl()}, {@link #back()}, {@link #forward()}, {@link #stop()},
 * {@link #addLocationListener(LocationListener)}. A click on a link navigates to it, resolved against the page's
 * location: {@link LocationListener#changing} (a listener may veto it with {@code doit = false}), then, once the page
 * has loaded and is shown, {@link LocationListener#changed}. Supported HTML: p, div, h1-h6, br, b/strong, i/em, u, s/strike/del, a, ul, ol, li,
 * img (data:, file:, http: and https: sources, loaded off the UI thread when their block is shown); script is ignored;
 * style elements and style attributes give the CSS subset of colour, background, weight, style, decoration, alignment
 * and left margin.
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
	/* the stores this widget draws and takes input for: the scroll window, and the location with its history */
	private final PageViewport viewport = new PageViewport();
	private final PageHistory pages = new PageHistory();
	private final java.util.List<LocationListener> locationListeners = new java.util.ArrayList<>();
	private java.util.concurrent.ExecutorService pageLoader;

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

	/**
	 * Shows {@code text} as HTML at {@code about:blank}; answers true, as
	 * {@link org.eclipse.swt.browser.Browser#setText(String)} does. Relative links and images are then taken as they
	 * are written.
	 */
	public boolean setText(String text) {
		checkWidget();
		if (text == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		pages.setText();
		show(text);
		return true;
	}

	/**
	 * Loads the page at {@code url} off the UI thread: {@link LocationListener#changing} first (a listener may veto
	 * it), then, once the page is shown, {@link LocationListener#changed}. A page that cannot be loaded leaves the
	 * current one; a newer navigation replaces an older one still loading. URLs are those {@link java.net.URLConnection}
	 * opens (file:, http:, https:).
	 *
	 * @return whether the navigation started (false when a listener vetoed it)
	 */
	public boolean setUrl(String url) {
		checkWidget();
		if (url == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		return navigate(url, PageHistory.PUSH);
	}

	/** The location of the page shown: its URL, or {@code about:blank} for {@link #setText(String)}. */
	public String getUrl() {
		checkWidget();
		return pages.location();
	}

	public boolean isBackEnabled() {
		checkWidget();
		return pages.canBack();
	}

	public boolean isForwardEnabled() {
		checkWidget();
		return pages.canForward();
	}

	/** Goes to the previous page of the history; answers whether there was one. */
	public boolean back() {
		checkWidget();
		return isBackEnabled() && navigate(pages.backTarget(), PageHistory.BACK);
	}

	/** Goes to the next page of the history; answers whether there was one. */
	public boolean forward() {
		checkWidget();
		return isForwardEnabled() && navigate(pages.forwardTarget(), PageHistory.FORWARD);
	}

	/** Drops the page being loaded, if any. */
	public void stop() {
		checkWidget();
		pages.stop();
	}

	private void show(String text) {
		html = text;
		document = HtmlDocument.parse(text);
		layout.dispose();
		layout = newLayout();
		viewport.reset();
		relayout();
	}

	/** The HTML of the page shown. */
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
		return viewport.y();
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
			case SWT.MouseMove -> setCursor(layout.linkAt(e.x, e.y + viewport.y()) != null ? getDisplay().getSystemCursor(SWT.CURSOR_HAND) : null);
			case SWT.MouseWheel -> {
				scrollTo(viewport.y() - e.count * LINE);
				e.doit = false;
			}
			case SWT.KeyDown -> key(e);
			case SWT.Dispose -> {
				pages.stop();
				layout.dispose();
				images.dispose();
				if (pageLoader != null) pageLoader.shutdownNow();
			}
			default -> {
			}
		}
	}

	private void relayout() {
		Rectangle area = getClientArea();
		layout.setWidth(Math.max(1, area.width));
		layout.ensure(viewport.y() + area.height);
		sync();
		updateBar();
		redraw();
	}

	/* the viewport learns the view height and how far the page is laid out; the scroll position is clamped */
	private void sync() {
		viewport.resize(getClientArea().height, layout.totalHeight());
	}

	private void updateBar() {
		ScrollBar bar = getVerticalBar();
		int page = Math.max(1, getClientArea().height);
		bar.setValues(viewport.y(), 0, Math.max(page, layout.totalHeight()), page, LINE, page);
	}

	/*
	 * moves the window's pixels by the scroll delta; only the band that comes into view is damaged and repainted.
	 * Control.scroll repaints only the part of its source the destination does not cover: for a move of more than half
	 * the view that leaves rows between them stale, so the exposed band is damaged explicitly (as StyledText does).
	 */
	private void scrollTo(int y) {
		layout.ensure(Math.max(0, y) + getClientArea().height);
		sync();
		int delta = viewport.scrollTo(y);
		if (delta != 0) {
			if (viewport.isFullRepaint(delta)) {
				redraw();
			} else {
				int width = getClientArea().width;
				scroll(0, viewport.copyTo(delta), 0, viewport.copyFrom(delta), width, viewport.copyHeight(delta), false);
				redraw(0, viewport.exposedTop(delta), width, viewport.exposedHeight(delta), false);
			}
		}
		updateBar();
	}

	private void paint(GC gc, int x, int y, int width, int height) {
		gc.setBackground(getBackground());
		gc.fillRectangle(x, y, width, height);
		if (document.blockCount == 0) return;
		gc.setForeground(getForeground());
		int scrollY = viewport.y();
		int pageTop = y + scrollY, pageBottom = pageTop + height;
		for (int b = layout.blockAt(pageTop); b >= 0 && b < document.blockCount && layout.top(b) < pageBottom; b++) {
			int top = layout.top(b) - scrollY + HtmlLayout.BLOCK_GAP, left = layout.indent(b);
			if (document.blockBackground[b] != 0) {
				gc.setBackground(layout.color(document.blockBackground[b]));
				gc.fillRectangle(0, layout.top(b) - scrollY, getClientArea().width, layout.height(b));
				gc.setBackground(getBackground());
			}
			if (document.kind[b] == HtmlDocument.IMAGE) {
				int i = document.image[b], w = layout.shownWidth(i), h = layout.shownHeight(i);
				Image image = images.get(pages.resolve(document.source[i]));
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
			if (source.equals(pages.resolve(document.source[i]))) moved |= layout.setNaturalSize(i, size.width, size.height);
		}
		if (moved) relayout();
		else redraw();
	}

	private void click(int x, int y) {
		String target = layout.linkAt(x, y + viewport.y());
		if (target != null) navigate(pages.resolve(target), PageHistory.PUSH);
	}

	/* changing (vetoable), then the page loads off the UI thread, then it is shown and changed is sent */
	private boolean navigate(String url, int mode) {
		LocationEvent changing = new LocationEvent(this);
		changing.location = url;
		changing.top = true;
		changing.doit = true;
		for (LocationListener listener : locationListeners.toArray(new LocationListener[0])) listener.changing(changing);
		if (!changing.doit) return false;
		int expected = pages.begin();
		if (pageLoader == null) {
			pageLoader = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
				Thread thread = new Thread(r, "DrawnBrowser pages");
				thread.setDaemon(true);
				return thread;
			});
		}
		org.eclipse.swt.widgets.Display display = getDisplay();
		pageLoader.execute(() -> {
			String page = fetch(url);
			if (page != null && !display.isDisposed()) display.asyncExec(() -> arrived(url, mode, expected, page));
		});
		return true;
	}

	private void arrived(String url, int mode, int expected, String page) {
		if (isDisposed() || !pages.arrive(url, mode, expected)) return;
		show(page);
		LocationEvent changed = new LocationEvent(this);
		changed.location = url;
		changed.top = true;
		for (LocationListener listener : locationListeners.toArray(new LocationListener[0])) listener.changed(changed);
	}

	/* the page's text, or null; runs on the loader thread */
	private static String fetch(String url) {
		try {
			java.net.URLConnection connection = java.net.URI.create(url).toURL().openConnection();
			connection.setConnectTimeout(10_000);
			connection.setReadTimeout(10_000);
			String type = connection.getContentType();
			java.nio.charset.Charset charset = java.nio.charset.StandardCharsets.UTF_8;
			int at = type == null ? -1 : type.toLowerCase(java.util.Locale.ROOT).indexOf("charset=");
			if (at >= 0) {
				try {
					charset = java.nio.charset.Charset.forName(type.substring(at + 8).replace("\"", "").trim());
				} catch (IllegalArgumentException e) {
					// keep UTF-8
				}
			}
			try (java.io.InputStream in = connection.getInputStream()) {
				return new String(in.readAllBytes(), charset);
			}
		} catch (java.io.IOException | IllegalArgumentException e) {
			return null;
		}
	}

	private void key(Event e) {
		sync();
		int scrollY = viewport.y(), page = viewport.page(LINE);
		switch (e.keyCode) {
			case SWT.ARROW_DOWN -> scrollTo(scrollY + LINE);
			case SWT.ARROW_UP -> scrollTo(scrollY - LINE);
			case SWT.PAGE_DOWN -> scrollTo(scrollY + page);
			case SWT.PAGE_UP -> scrollTo(scrollY - page);
			case SWT.HOME -> scrollTo(0);
			case SWT.END -> {
				layout.ensure(Integer.MAX_VALUE);
				sync();
				scrollTo(viewport.maxY());
			}
			default -> {
			}
		}
	}
}
