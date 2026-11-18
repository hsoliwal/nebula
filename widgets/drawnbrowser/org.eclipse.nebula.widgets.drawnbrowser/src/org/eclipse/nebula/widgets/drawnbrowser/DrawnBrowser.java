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
import org.eclipse.nebula.widgets.drawnbrowser.internal.PageFind;
import org.eclipse.nebula.widgets.drawnbrowser.internal.PageHistory;
import org.eclipse.nebula.widgets.drawnbrowser.internal.PageSelection;
import org.eclipse.nebula.widgets.drawnbrowser.internal.PageViewport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
 * pre (white space kept, no wrap, monospace), code/tt/kbd/samp (monospace), blockquote (indented), hr,
 * table/caption/tr/td/th (automatic column widths, the border attribute; text only in cells, no spans), img (data:, file:, http: and https: sources, loaded off the UI thread when their block is shown); script is ignored;
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
	/* the selection in the page's text, made by find or the mouse */
	private final PageSelection selection = new PageSelection();
	/* scrolls while a drag holds the mouse above or below the view; re-armed while it moves the page */
	private final Runnable autoScroll = this::autoScroll;
	private static final int AUTO_SCROLL_MS = 40;
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
		for (int type : new int[] { SWT.Paint, SWT.Resize, SWT.MouseDown, SWT.MouseUp, SWT.MouseMove, SWT.MouseWheel, SWT.KeyDown,
				SWT.Dispose }) {
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
		selection.clear();
		relayout();
	}

	/**
	 * Finds {@code findString} in the page's text (the text drawn, not the HTML) and selects it, as JFace's
	 * {@code IFindReplaceTarget.findAndSelect} does: searching forward finds the first match that starts at or after
	 * {@code offset}, backward the last one that starts at or before it; -1 searches the whole page. There is no wrap.
	 * The match is scrolled into view; only the blocks of the old and the new selection are painted again.
	 *
	 * @return the match's offset in the page's text, or -1 (the selection is then unchanged)
	 */
	public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord) {
		checkWidget();
		if (findString == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		int at = PageFind.find(document, offset, findString, searchForward, caseSensitive, wholeWord);
		if (at < 0) return -1;
		redrawRange(selection.start(), selection.end());
		selection.select(at, at + findString.length());
		reveal(at);
		redrawRange(selection.start(), selection.end());
		return at;
	}

	/** The selection in the page's text: {@code x} is its start offset, {@code y} its end (as StyledText answers). */
	public Point getSelection() {
		checkWidget();
		return new Point(selection.start(), selection.end());
	}

	/**
	 * The selected text, or the empty string. A selection across blocks has a line break between their texts (the
	 * page's text has none), so it can be longer than the selection's range.
	 */
	public String getSelectionText() {
		checkWidget();
		return selection.text(document);
	}

	/**
	 * Selects the range {@code [start, end)} of the page's text (clamped to it; in either order), as
	 * {@code StyledText.setSelection(int, int)} does, without scrolling.
	 */
	public void setSelection(int start, int end) {
		checkWidget();
		int length = document.text.length();
		redrawRange(selection.start(), selection.end());
		selection.select(Math.max(0, Math.min(start, length)), Math.max(0, Math.min(end, length)));
		redrawRange(selection.start(), selection.end());
	}

	/** Selects the page's whole text. */
	public void selectAll() {
		checkWidget();
		setSelection(0, document.text.length());
	}

	/**
	 * Copies the selected text to the clipboard as text, blocks separated by line breaks; nothing happens when nothing
	 * is selected. Ctrl+C (Command+C) does the same, Ctrl+A selects all.
	 */
	public void copy() {
		checkWidget();
		String text = selection.text(document);
		if (text.isEmpty()) return;
		org.eclipse.swt.dnd.Clipboard clipboard = new org.eclipse.swt.dnd.Clipboard(getDisplay());
		try {
			clipboard.setContents(new Object[] { text }, new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	/* one auto-scroll step of a drag held above or below the view: the page moves, the selection follows the mouse */
	private void autoScroll() {
		if (isDisposed() || !selection.isDragging()) return;
		int step = viewport.edgeStep(selection.pointerY(), LINE);
		if (step == 0) return;
		int before = viewport.y();
		scrollTo(before + step);
		int oldStart = selection.start(), oldEnd = selection.end();
		if (selection.drag(layout.offsetAt(selection.pointerX(), selection.pointerY() + viewport.y()))) {
			redrawRange(Math.min(oldStart, selection.start()), Math.max(oldEnd, selection.end()));
		}
		if (viewport.y() != before) getDisplay().timerExec(AUTO_SCROLL_MS, autoScroll);
	}

	/* damages the blocks that hold [start, end) of the page's text, where they are in view */
	private void redrawRange(int start, int end) {
		if (start >= end) return;
		int first = PageFind.blockOf(document, start), last = PageFind.blockOf(document, end - 1);
		if (first < 0 || last < 0) return;
		int width = getClientArea().width;
		for (int b = first; b <= last; b++) {
			redraw(0, layout.top(b) - viewport.y(), width, layout.height(b), false);
		}
	}

	/* scrolls the line holding the page text offset into view, a third of the way down, unless it is in view already */
	private void reveal(int offset) {
		int b = PageFind.blockOf(document, offset);
		TextLayout text = b < 0 ? null : layout.textLayout(b);
		if (text == null) return;
		int local = offset - document.textStart[b];
		Rectangle line = text.getLineBounds(text.getLineIndex(local));
		int top = layout.top(b) + layout.contentTop(b) + line.y, view = getClientArea().height;
		if (top < viewport.y() || top + line.height > viewport.y() + view) scrollTo(top - view / 3);
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
			case SWT.MouseDown -> {
				if (e.button == 1) {
					redrawRange(selection.start(), selection.end());
					selection.press(layout.offsetAt(e.x, e.y + viewport.y()));
					selection.pointer(e.x, e.y);
				}
			}
			case SWT.MouseUp -> {
				if (e.button == 1) {
					selection.release();
					if (selection.isEmpty()) click(e.x, e.y); // a drag that selected text does not follow the link under it
				}
			}
			case SWT.MouseMove -> {
				if (selection.isDragging() && (e.stateMask & SWT.BUTTON1) != 0) {
					selection.pointer(e.x, e.y);
					int oldStart = selection.start(), oldEnd = selection.end();
					if (selection.drag(layout.offsetAt(e.x, e.y + viewport.y()))) {
						redrawRange(Math.min(oldStart, selection.start()), Math.max(oldEnd, selection.end()));
					}
					if (viewport.edgeStep(e.y, LINE) != 0) getDisplay().timerExec(AUTO_SCROLL_MS, autoScroll);
				}
				setCursor(layout.linkAt(e.x, e.y + viewport.y()) != null ? getDisplay().getSystemCursor(SWT.CURSOR_HAND) : null);
			}
			case SWT.MouseWheel -> {
				scrollTo(viewport.y() - e.count * LINE);
				e.doit = false;
			}
			case SWT.KeyDown -> key(e);
			case SWT.Dispose -> {
				getDisplay().timerExec(-1, autoScroll);
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
			int top = layout.top(b) - scrollY + layout.contentTop(b), left = layout.indent(b);
			boolean cell = document.kind[b] == HtmlDocument.CELL, box = !cell && layout.hasBox(b);
			// a cell's box is its column; a CSS box its border box; any other block keeps its whole row
			int boxX = cell ? layout.cellX(b) : box ? layout.boxLeft(b) : 0;
			int boxWidth = cell ? layout.cellWidth(b) : box ? layout.boxWidth(b) : getClientArea().width;
			int boxY = (box ? layout.boxTop(b) : layout.top(b)) - scrollY, boxHeight = box ? layout.boxHeight(b) : layout.height(b);
			if (document.blockBackground[b] != 0) {
				gc.setBackground(layout.color(document.blockBackground[b]));
				gc.fillRectangle(boxX, boxY, boxWidth, boxHeight);
				gc.setBackground(getBackground());
			}
			int border = cell ? 0 : document.blockBorder[b];
			if (border > 0) {
				int argb = document.blockBorderColor[b];
				gc.setBackground(argb != 0 ? layout.color(argb) : getForeground());
				gc.fillRectangle(boxX, boxY, boxWidth, border);
				gc.fillRectangle(boxX, boxY + boxHeight - border, boxWidth, border);
				gc.fillRectangle(boxX, boxY, border, boxHeight);
				gc.fillRectangle(boxX + boxWidth - border, boxY, border, boxHeight);
				gc.setBackground(getBackground());
			}
			if (cell && document.tableBorder[document.cellTable[b]] > 0) {
				gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				gc.drawRectangle(boxX, layout.top(b) - scrollY, boxWidth - 1, layout.height(b) - 1);
				gc.setForeground(getForeground());
			}
			if (document.kind[b] == HtmlDocument.RULE) {
				gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				gc.fillRectangle(left, top, Math.max(0, getClientArea().width - left - HtmlLayout.BLOCK_GAP), HtmlLayout.RULE_HEIGHT);
				gc.setBackground(getBackground());
				continue;
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
			int selectionStart = Math.max(selection.start(), document.textStart[b]) - document.textStart[b];
			int selectionEnd = Math.min(selection.end(), document.textEnd[b]) - document.textStart[b] - 1;
			if (selectionStart <= selectionEnd) {
				text.draw(gc, left, top, selectionStart, selectionEnd, getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT),
						getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
			} else {
				text.draw(gc, left, top);
			}
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
		if ((e.stateMask & SWT.MOD1) != 0 && (e.keyCode == 'c' || e.keyCode == 'a')) {
			if (e.keyCode == 'c') copy();
			else selectAll();
			return;
		}
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
