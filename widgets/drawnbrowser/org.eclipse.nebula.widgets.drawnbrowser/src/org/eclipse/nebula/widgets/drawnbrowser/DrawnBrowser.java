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
import org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlPaintOrder;
import org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlPositionedLayout;
import org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
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
	/* The existing flat positioned-block order; box scenes use their context-aware fragment plan. */
	private int[] positionedOrder;
	/* the page's form controls: their widgets (made when first in view), the values and checked states they show */
	private Control[] widgets = new Control[0];
	private String[] formValues = new String[0];
	private int[] formChecked = new int[0], placed = new int[0];
	private int placeStamp;
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

	/** A completed, exclusively owned backend result; publication transfers its document to the UI thread. */
	static final class PreparedPage {
		final String text;
		private final HtmlDocument document;
		private final Throwable failure;

		private PreparedPage(String text, HtmlDocument document, Throwable failure) {
			this.text = text;
			this.document = document;
			this.failure = failure;
		}

		static PreparedPage prepare(String text) {
			try {
				return new PreparedPage(text, HtmlDocument.parse(text), null);
			} catch (RuntimeException | Error failure) {
				// Preserve the original UI-dispatch failure policy after stale-arrival admission.
				return new PreparedPage(text, null, failure);
			}
		}

		HtmlDocument document() {
			if (failure instanceof RuntimeException exception) throw exception;
			if (failure instanceof Error error) throw error;
			return document;
		}
	}

	/**
	 * @param parent a composite
	 * @param style  the Canvas styles; {@link SWT#V_SCROLL} is always added
	 */
	@SuppressWarnings("this-escape") // an SWT widget registers its listeners while it is constructed
	public DrawnBrowser(Composite parent, int style) {
		// radios of a page group by their name and form, not by their parent: SWT's automatic group is off
		super(parent, style | SWT.V_SCROLL | SWT.NO_BACKGROUND | SWT.NO_RADIO_GROUP);
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
	 * Loads and parses the page at {@code url} off the UI thread: {@link LocationListener#changing} first (a listener may veto
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
		show(PreparedPage.prepare(text));
	}

	/* UI-thread installation only: parsing has finished, and the published document has no remaining writer. */
	private void show(PreparedPage page) {
		html = page.text;
		document = page.document();
		positionedOrder = null;
		// the page's form state, apart from the widgets that show it; the old page's widgets go
		for (Control widget : widgets) if (widget != null) widget.dispose();
		widgets = new Control[document.controlCount];
		placed = new int[document.controlCount];
		formValues = java.util.Arrays.copyOf(document.controlValue, document.controlCount);
		formChecked = java.util.Arrays.copyOf(document.controlChecked, document.controlCount);
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
				setCursor(linkAt(e.x, e.y) != null ? getDisplay().getSystemCursor(SWT.CURSOR_HAND) : null);
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
		layout.setViewport(area.height, viewport.y());
		layout.ensure(viewport.y() + area.height);
		sync();
		updateBar();
		redraw();
		placeWidgets();
	}

	/* the viewport learns the view height and how far the page is laid out; the scroll position is clamped */
	private void sync() {
		viewport.resize(getClientArea().height, layout.totalHeight());
		layout.setViewport(getClientArea().height, viewport.y());
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
		layout.setViewport(getClientArea().height, viewport.y());
		if (delta != 0) {
			HtmlPositionedLayout scene = layout.positioned();
			if (viewport.isFullRepaint(delta) || scene != null && (scene.hasFixed() || scene.hasSticky())) {
				redraw();
			} else {
				int width = getClientArea().width;
				scroll(0, viewport.copyTo(delta), 0, viewport.copyFrom(delta), width, viewport.copyHeight(delta), false);
				redraw(0, viewport.exposedTop(delta), width, viewport.exposedHeight(delta), false);
				if (layout.positioned() == null) redrawFixed(delta, width);
			}
		}
		updateBar();
		placeWidgets();
	}

	private void paint(GC gc, int x, int y, int width, int height) {
		gc.setBackground(getBackground());
		gc.fillRectangle(x, y, width, height);
		gc.setForeground(getForeground());
		HtmlPositionedLayout scene = layout.positioned();
		if (scene != null) {
			paintPositioned(gc, scene, x, y, width, height);
			return;
		}
		if (document.blockCount == 0) return;
		int scrollY = viewport.y();
		int pageTop = y + scrollY, pageBottom = pageTop + height;
		paintPositioned(gc, scrollY, pageTop, pageBottom, true);
		for (int b = layout.paintStart(pageTop); b >= 0 && b < document.blockCount && layout.top(b) < pageBottom; b++) {
			if (!layout.positioned(b)) paintLegacyBlock(gc, b, 0, -scrollY);
		}
		paintPositioned(gc, scrollY, pageTop, pageBottom, false);
	}

	/* Context backgrounds and content are separate fragments: negative descendants paint between them. */
	private void paintPositioned(GC gc, HtmlPositionedLayout scene, int damageX, int damageY, int damageWidth, int damageHeight) {
		HtmlPaintOrder order = scene.order();
		int scroll = viewport.y();
		int count = scene.collectVisible(damageY + scroll, damageHeight, scroll);
		for (int v = 0; v < count; v++) {
			int s = scene.visibleStep(v);
			int i = order.index(s);
			if (order.isBackground(s)) {
				int color = document.boxBackground[i];
				int border = document.boxBorder[i];
				if (color == 0 && border == 0) continue;
				int x = scene.boxX(i), y = scene.boxY(i, scroll) - scroll, w = scene.boxWidth(i), h = scene.boxHeight(i);
				if (!intersects(x, y, w, h, damageX, damageY, damageWidth, damageHeight)) continue;
				paintBox(gc, x, y, w, h, color, border, document.boxBorderColor[i], document.boxBorderStyle[i]);
			} else {
				int y = scene.blockY(i, scroll) - scroll, box = document.blockBox[i];
				// PRE, list markers and images may overflow their box; let the widget's damage clip bound drawing.
				if ((long) y + scene.blockHeight(i) <= damageY || y >= (long) damageY + damageHeight) continue;
				paintBlock(gc, i, y + layout.contentTop(i), scene.blockX(i), scene.boxX(box), scene.boxWidth(box));
			}
		}
	}

	private static boolean intersects(int x, int y, int width, int height, int dx, int dy, int dw, int dh) {
		return width > 0 && height > 0 && x < (long) dx + dw && y < (long) dy + dh
				&& (long) x + width > dx && (long) y + height > dy;
	}

	/* A box's background and border paint together, before its negative-z descendants and content. */
	private void paintBox(GC gc, int x, int y, int width, int height, int background, int border, int borderColor, int borderStyle) {
		if (width <= 0 || height <= 0) return;
		if (background != 0) {
			gc.setBackground(layout.color(background));
			gc.fillRectangle(x, y, width, height);
		}
		if (border > 0) paintBorder(gc, x, y, width, height, border, borderColor, borderStyle);
		gc.setBackground(getBackground());
	}

	/* Retain the flat owner's coordinate contract while sharing the existing content drawing atoms. */
	private void paintLegacyBlock(GC gc, int b, int dx, int dy) {
		int top = layout.top(b) + dy + layout.contentTop(b), left = layout.indent(b) + dx;
		boolean cell = document.kind[b] == HtmlDocument.CELL, box = !cell && layout.hasBox(b);
		int boxX = (cell ? layout.cellX(b) : box ? layout.boxLeft(b) : 0) + dx;
		int boxWidth = cell ? layout.cellWidth(b) : box ? layout.boxWidth(b) : getClientArea().width;
		int boxY = (box ? layout.boxTop(b) : layout.top(b)) + dy;
		int boxHeight = box ? layout.boxHeight(b) : layout.height(b);
		paintBox(gc, boxX, boxY, boxWidth, boxHeight, document.blockBackground[b],
				cell && !layout.boxedItem(b) ? 0 : document.blockBorder[b], document.blockBorderColor[b],
				document.blockBorderStyle[b]);
		paintBlock(gc, b, top, left, boxX, boxWidth);
	}

	private void paintBlock(GC gc, int b, int top, int left, int boxX, int boxWidth) {
		int scrollY = viewport.y();
		boolean cell = document.kind[b] == HtmlDocument.CELL;

		if (cell && document.tableBorder[document.cellTable[b]] > 0) {
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
			gc.drawRectangle(boxX, layout.top(b) - scrollY, boxWidth - 1, layout.height(b) - 1);
			gc.setForeground(getForeground());
		}
		if (document.kind[b] == HtmlDocument.RULE) {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
			gc.fillRectangle(left, top, Math.max(0, boxX + boxWidth - left - HtmlLayout.BLOCK_GAP), HtmlLayout.RULE_HEIGHT);
			gc.setBackground(getBackground());
			return;
		}
		if (document.kind[b] == HtmlDocument.IMAGE) {
			int i = document.image[b], w = layout.shownWidth(i), h = layout.shownHeight(i);
			if (w <= 0 || h <= 0) return;
			Image image = images.get(pages.resolve(document.source[i]));
			if (image != null && !image.isDisposed()) {
				Rectangle size = image.getBounds();
				gc.drawImage(image, 0, 0, size.width, size.height, left, top, w, h);
			} else {
				gc.drawRectangle(left, top, w - 1, h - 1);
			}
			return;
		}
		if (document.kind[b] == HtmlDocument.CONTROL) return; // the original native widget owns its pixels
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
		String target = linkAt(x, y);
		if (target != null) navigate(pages.resolve(target), PageHistory.PUSH);
	}

	/* changing (vetoable), then loading and parsing off the UI thread, then UI installation and changed */
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
			if (page == null || display.isDisposed()) return;
			PreparedPage prepared = PreparedPage.prepare(page);
			if (display.isDisposed()) return;
			try {
				display.asyncExec(() -> arrived(url, mode, expected, prepared));
			} catch (org.eclipse.swt.SWTException failure) {
				// The Display may close between the check above and queue admission.
				if (failure.code != SWT.ERROR_DEVICE_DISPOSED) throw failure;
			}
		});
		return true;
	}

	private void arrived(String url, int mode, int expected, PreparedPage page) {
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

	/** One CSS border scheme in the original SWT GC; no Figure or Control per box. */
	private void paintBorder(GC gc, int x, int y, int width, int height, int thickness, int argb, int style) {
		Color base = argb != 0 ? layout.color(argb) : getForeground();
		if (style == HtmlStyles.BORDER_SOLID || (style == HtmlStyles.BORDER_DOUBLE && thickness < 3)) {
			// Exact original solid-border pixels.
			gc.setBackground(base);
			int horizontal = Math.min(thickness, height), vertical = Math.min(thickness, width);
			gc.fillRectangle(x, y, width, horizontal);
			gc.fillRectangle(x, y + height - horizontal, width, horizontal);
			gc.fillRectangle(x, y, vertical, height);
			gc.fillRectangle(x + width - vertical, y, vertical, height);
		} else if (style == HtmlStyles.BORDER_DOUBLE) {
			int stripe = Math.max(1, thickness / 3);
			borderBand(gc, x, y, width, height, 0, stripe, base, base);
			borderBand(gc, x, y, width, height, thickness - stripe, stripe, base, base);
		} else {
			// Draw2D SchemeBorder: light/dark top-left vs bottom-right side colours.
			int color = argb;
			if (color == 0) {
				org.eclipse.swt.graphics.RGB rgb = getForeground().getRGB();
				color = 0xFF000000 | rgb.red << 16 | rgb.green << 8 | rgb.blue;
			}
			Color light = layout.color(shadeBorder(color, true));
			Color dark = layout.color(shadeBorder(color, false));
			boolean lower = style == HtmlStyles.BORDER_INSET || style == HtmlStyles.BORDER_GROOVE;
			Color tl = lower ? dark : light, br = lower ? light : dark;
			if (style == HtmlStyles.BORDER_GROOVE || style == HtmlStyles.BORDER_RIDGE) {
				int outer = (thickness + 1) / 2;
				borderBand(gc, x, y, width, height, 0, outer, tl, br);
				borderBand(gc, x, y, width, height, outer, thickness - outer, br, tl);
			} else {
				borderBand(gc, x, y, width, height, 0, thickness, tl, br);
			}
		}
		gc.setBackground(getBackground());
	}

	private static int shadeBorder(int color, boolean lighter) {
		int red = color >>> 16 & 255, green = color >>> 8 & 255, blue = color & 255;
		red = lighter ? red + (255 - red) / 2 : red / 2;
		green = lighter ? green + (255 - green) / 2 : green / 2;
		blue = lighter ? blue + (255 - blue) / 2 : blue / 2;
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	private static void borderBand(GC gc, int x, int y, int width, int height,
			int inset, int thickness, Color topLeft, Color bottomRight) {
		int w = width - 2 * inset, h = height - 2 * inset;
		if (w <= 0 || h <= 0 || thickness <= 0) return;
		int t = Math.min(thickness, Math.min(w, h));
		int xx = x + inset, yy = y + inset;
		gc.setBackground(topLeft);
		gc.fillRectangle(xx, yy, w, t);
		gc.fillRectangle(xx, yy, t, h);
		gc.setBackground(bottomRight);
		gc.fillRectangle(xx, yy + h - t, w, t);
		gc.fillRectangle(xx + w - t, yy, t, h);
	}

	/*
	 * the positioned boxes in z-index order (document order among equals): those with a negative z-index when below,
	 * the others otherwise; only boxes that reach the damaged page band are painted
	 */
	private void paintPositioned(GC gc, int scrollY, int pageTop, int pageBottom, boolean below) {
		for (int b : positionedOrder()) {
			if ((document.blockZ[b] < 0) != below) continue;
			int dy = layout.offsetY(b, scrollY), top = layout.top(b) + dy;
			if (top >= pageBottom || top + layout.height(b) <= pageTop) continue;
			paintLegacyBlock(gc, b, layout.offsetX(b), dy - scrollY);
		}
	}

	/* the positioned blocks sorted by z-index, stable (document order among equal z-indexes); made once per page */
	private int[] positionedOrder() {
		if (positionedOrder != null) return positionedOrder;
		int n = 0;
		for (int b = 0; b < document.blockCount; b++) if (document.blockPosition[b] != 0) n++;
		int[] order = new int[n];
		n = 0;
		for (int b = 0; b < document.blockCount; b++) {
			if (document.blockPosition[b] == 0) continue;
			int at = n++;
			while (at > 0 && document.blockZ[order[at - 1]] > document.blockZ[b]) {
				order[at] = order[at - 1]; // insertion sort: stable
				at--;
			}
			order[at] = b;
		}
		return positionedOrder = order;
	}

	/* the link under (x, y) in the view: positioned boxes from the topmost down, then the flow */
	private String linkAt(int x, int y) {
		if (layout.positioned() != null) return layout.linkAt(x, y + viewport.y());
		int scrollY = viewport.y();
		int[] order = positionedOrder();
		for (int i = order.length - 1; i >= 0; i--) {
			int b = order[i], dx = layout.offsetX(b), dy = layout.offsetY(b, scrollY);
			int top = layout.top(b) + dy - scrollY;
			if (y < top || y >= top + layout.height(b)) continue;
			String link = layout.linkIn(b, x - dx, y + scrollY - dy);
			if (link != null) return link;
		}
		return layout.linkAt(x, y + scrollY);
	}

	/*
	 * a fixed box stays where it is in the view, and a stuck sticky box moves less than the page, but the scroll moved
	 * their pixels with the page: where such a box is now and where its pixels were moved to are damaged
	 */
	private void redrawFixed(int delta, int width) {
		int scrollY = viewport.y(), before = scrollY - delta;
		for (int b : positionedOrder()) {
			int position = document.blockPosition[b];
			if (position != org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.FIXED
					&& position != org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.STICKY) continue;
			int height = layout.height(b), top = layout.top(b) + layout.offsetY(b, scrollY) - scrollY;
			int moved = layout.top(b) + layout.offsetY(b, before) - before - delta;
			if (moved == top) continue; // it moved with the page
			redraw(0, top, width, height, false);
			redraw(0, moved, width, height, false);
		}
	}

	/* the form controls of the blocks in view, made on first sight; those out of view are hidden */
	private void placeWidgets() {
		if (widgets.length == 0) return;
		int stamp = ++placeStamp, scrollY = viewport.y(), viewBottom = scrollY + getClientArea().height;
		HtmlPositionedLayout scene = layout.positioned();
		int visible = scene == null ? 0 : scene.collectVisible(scrollY, getClientArea().height, scrollY);
		for (int step = scene == null ? layout.blockAt(scrollY) : 0;
				step >= 0 && (scene == null ? step < document.blockCount && layout.top(step) < viewBottom : step < visible); step++) {
			int b;
			if (scene == null) b = step;
			else {
				int paint = scene.visibleStep(step);
				if (scene.order().isBackground(paint)) continue;
				b = scene.order().index(paint);
			}
			int c = document.blockControl[b];
			if (c < 0 || document.kind[b] != HtmlDocument.CONTROL) continue;
			Control widget = widget(c);
			widget.setBounds(layout.indent(b), layout.top(b) + layout.contentTop(b) - scrollY, layout.controlWidth(b),
					layout.controlHeight(b));
			widget.setVisible(true);
			placed[c] = stamp;
		}
		for (int c = 0; c < widgets.length; c++) {
			if (widgets[c] != null && placed[c] != stamp && widgets[c].getVisible()) widgets[c].setVisible(false);
		}
	}

	/* the native widget of form control c, made once */
	private Control widget(int c) {
		if (widgets[c] != null) return widgets[c];
		int type = document.controlType[c];
		Control made;
		switch (type) {
			case HtmlDocument.CHECKBOX, HtmlDocument.RADIO -> {
				Button box = new Button(this, type == HtmlDocument.CHECKBOX ? SWT.CHECK : SWT.RADIO);
				box.setSelection(formChecked[c] == 1);
				box.addListener(SWT.Selection, e -> checked(c, box.getSelection()));
				made = box;
			}
			case HtmlDocument.SUBMIT, HtmlDocument.BUTTON -> {
				Button button = new Button(this, SWT.PUSH);
				button.setText(document.controlLabel[c]);
				if (type == HtmlDocument.SUBMIT) button.addListener(SWT.Selection, e -> submit(c));
				made = button;
			}
			case HtmlDocument.SELECT -> {
				Combo combo = new Combo(this, SWT.READ_ONLY | SWT.DROP_DOWN);
				combo.setItems(document.controlOptions[c]);
				if (document.controlOptions[c].length > 0) combo.select(formChecked[c]);
				combo.addListener(SWT.Selection, e -> formChecked[c] = combo.getSelectionIndex());
				made = combo;
			}
			default -> {
				int style = SWT.BORDER | (type == HtmlDocument.TEXTAREA ? SWT.MULTI | SWT.WRAP | SWT.V_SCROLL : SWT.SINGLE)
						| (type == HtmlDocument.PASSWORD ? SWT.PASSWORD : 0);
				Text text = new Text(this, style);
				text.setText(formValues[c]);
				text.addListener(SWT.Modify, e -> formValues[c] = text.getText());
				made = text;
			}
		}
		return widgets[c] = made;
	}

	/* a check box or a radio changes; a radio clears the other radios of its name in its form */
	private void checked(int c, boolean on) {
		formChecked[c] = on ? 1 : 0;
		if (!on || document.controlType[c] != HtmlDocument.RADIO) return;
		for (int o = 0; o < document.controlCount; o++) {
			if (o == c || document.controlType[o] != HtmlDocument.RADIO || document.controlForm[o] != document.controlForm[c]
					|| !java.util.Objects.equals(document.controlName[o], document.controlName[c])) continue;
			formChecked[o] = 0;
			if (widgets[o] != null) ((Button) widgets[o]).setSelection(false);
		}
	}

	/*
	 * a submit button sends its form: the named values of the form's controls (checked boxes and radios, the selected
	 * option, the clicked button), URL encoded, as the query of the form's action (GET)
	 */
	private void submit(int button) {
		int form = document.controlForm[button];
		StringBuilder query = new StringBuilder();
		for (int c = 0; c < document.controlCount; c++) {
			String name = document.controlName[c];
			if (document.controlForm[c] != form || name == null) continue;
			String value = switch (document.controlType[c]) {
				case HtmlDocument.CHECKBOX, HtmlDocument.RADIO -> formChecked[c] == 1 ? document.controlValue[c] : null;
				case HtmlDocument.SELECT -> document.controlOptions[c].length == 0 ? null : document.controlOptions[c][formChecked[c]];
				case HtmlDocument.SUBMIT -> c == button ? document.controlValue[c] : null;
				case HtmlDocument.BUTTON -> null;
				default -> formValues[c];
			};
			if (value == null) continue;
			if (query.length() > 0) query.append('&');
			query.append(java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)).append('=')
					.append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
		}
		String action = form >= 0 ? document.formAction[form] : null;
		String target = pages.resolve(action != null ? action : pages.location());
		int cut = target.indexOf('#');
		if (cut >= 0) target = target.substring(0, cut);
		cut = target.indexOf('?');
		if (cut >= 0) target = target.substring(0, cut);
		navigate(target + "?" + query, PageHistory.PUSH);
	}
}
