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
// Generated fromtemplates/family/COLLECTION.java.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import java.util.Arrays;

import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/**
 * Virtual collections (pattern COLLECTION-1, aspects R+G+D+P): rows or tiles over a long logical
 * extent; only the resident window has rects. Per-row swipe offset (aux lane), board columns (group
 * lane), overscroll pull with a refresh indicator on the OVERLAY plane, a load threshold, keyboard and
 * drag reorder, message bubbles, a filtered command palette, a tile grid and variable-height
 * masonry columns. Members: kanban-board, swipe-actions, pull-to-refresh, infinite-scroll,
 * reorder-drag, message-list, command-palette, gallery, masonry.
 */
public class CollectionModel extends WebModel implements CollectionApi {
	public static final int KANBAN = 0, SWIPE = 1, PULL = 2, INFINITE = 3, REORDER = 4, MESSAGES = 5, PALETTE = 6, GALLERY = 7, MASONRY = 8;
	private int pull, loadedAt = -1, threshold = 5, columns = 1, dragFrom = -1, dragX, dragY;
	/** React-style identities are references owned by this collection, never per-item proxies. */
	private String[] stableKeys;
	// The parent owns primitive geometry for every masonry item; no per-tile objects.
	private long[] masonryTop;
	private short[] masonryColumn;
	private int[] masonryHeights;
	private long masonryExtent;

	public CollectionModel(String concept, int role, int mode, int shape, int variant) {
		super(concept, role, mode, shape, variant);
		itemExtent = mode == MESSAGES ? 56 : mode == GALLERY || mode == MASONRY ? 112 : 40;
	}

	/**
	 * Reconciles a declarative sequence in one layout/delta publication. Stable keys preserve
	 * per-row primitive state, value, group, aux and focus across reorder and relabel; new
	 * keys start empty. The caller's arrays are copied before validation and never retained.
	 * This is a native row-state bridge, not a JavaScript/React hook interpreter.
	 */
	@Override
	public void reconcile(String[] keys, String[] labels) {
		if (keys == null || labels == null || keys.length != labels.length) {
			throw new IllegalArgumentException("key and label arrays must have equal non-null lengths");
		}
		String[] incoming = keys.clone(), words = labels.clone();
		int[] unique = keyTable(incoming.length);
		indexKeys(incoming, unique);
		if (sameSnapshot(incoming, words)) {
			return;
		}
		int n = incoming.length;
		int[] previous = stableKeys == null ? null : keyTable(stableKeys.length);
		if (previous != null) {
			indexKeys(stableKeys, previous);
		}
		int[] stagedFlags = new int[n], stagedValue = new int[n], stagedAux = new int[n],
				stagedGroup = new int[n];
		String[] stagedDetail = new String[n];
		int nextFocus = -1, nextHover = -1, nextPressed = -1;
		for (int i = 0; i < n; i++) {
			int old = previous == null ? -1 : lookup(incoming[i], stableKeys, previous);
			if (old < 0) {
				continue;
			}
			stagedFlags[i] = flags[old];
			stagedValue[i] = value[old];
			stagedAux[i] = aux[old];
			stagedGroup[i] = group[old];
			stagedDetail[i] = detail[old];
			if (old == focus) nextFocus = i;
			if (old == hover) nextHover = i;
			if (old == pressed) nextPressed = i;
		}
		int retired = count;
		if (n > flags.length) {
			grow(n);
		}
		System.arraycopy(stagedFlags, 0, flags, 0, n);
		System.arraycopy(stagedValue, 0, value, 0, n);
		System.arraycopy(stagedAux, 0, aux, 0, n);
		System.arraycopy(stagedGroup, 0, group, 0, n);
		System.arraycopy(words, 0, text, 0, n);
		System.arraycopy(stagedDetail, 0, detail, 0, n);
		java.util.Arrays.fill(rects, 0, Math.max(n, retired) * 4, 0);
		if (retired > n) {
			blank(n, retired);
		}
		count = n;
		stableKeys = incoming;
		forgetLocalizationKeys();
		focus = nextFocus;
		hover = nextHover;
		pressed = nextPressed;
		loadedAt = -1;
		rebuildFilter();
		scroll = Math.max(0L, Math.min(scroll, Math.max(0L, logicalExtent() - viewLength())));
		relayout(-1, D_ITEMS);
	}

	@Override
	public String stableKey(int item) {
		check(item);
		return stableKeys == null ? null : stableKeys[item];
	}

	private boolean sameSnapshot(String[] keys, String[] labels) {
		if (stableKeys == null || count != keys.length) {
			return false;
		}
		for (int i = 0; i < count; i++) {
			if (!stableKeys[i].equals(keys[i]) || !java.util.Objects.equals(text[i], labels[i])) {
				return false;
			}
		}
		return true;
	}

	/** A primitive open-addressed index: collision resolution checks complete String keys. */
	private static int[] keyTable(int n) {
		if (n > (1 << 28)) {
			throw new IllegalArgumentException("keyed snapshot exceeds addressable index limit");
		}
		int size = 8;
		while (size < n * 2) {
			size <<= 1;
		}
		return new int[size];
	}

	private static void indexKeys(String[] keys, int[] table) {
		int mask = table.length - 1;
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			if (key == null) {
				throw new IllegalArgumentException("null stable key at " + i);
			}
			int hash = key.hashCode();
			int slot = (hash ^ (hash >>> 16)) & mask;
			while (table[slot] != 0) {
				if (keys[table[slot] - 1].equals(key)) {
					throw new IllegalArgumentException("duplicate stable key: " + key);
				}
				slot = (slot + 1) & mask;
			}
			table[slot] = i + 1;
		}
	}

	private static int lookup(String key, String[] keys, int[] table) {
		int mask = table.length - 1;
		int hash = key.hashCode(), slot = (hash ^ (hash >>> 16)) & mask;
		while (table[slot] != 0) {
			int i = table[slot] - 1;
			if (keys[i].equals(key)) {
				return i;
			}
			slot = (slot + 1) & mask;
		}
		return -1;
	}

	@Override
	public int add(String label, String detail) {
		stableKeys = null;
		return super.add(label, detail);
	}

	@Override
	public void remove(int item) {
		super.remove(item);
		stableKeys = null;
	}

	@Override
	public void clear() {
		stableKeys = null;
		super.clear();
	}

	@Override
	public void setLogicalCount(int n) {
		super.setLogicalCount(n);
		stableKeys = null;
	}

	@Override
	public int aspects() {
		return A_R | A_G | A_D | A_P;
	}

	@Override
	public void load(String sample) {
		clear();
		for (String s : sample.split(";")) {
			String[] f = s.split("~", -1);
			boolean grouped = (mode == KANBAN || mode == MESSAGES) && f.length > 1;
			int i = add(grouped ? f[1] : f[0]);
			setGroup(i, grouped ? WebStates.parse(f[0]) : 0);
		}
	}

	/** Positions per row: equal gallery tiles or at most eight shortest-column masonry lanes. */
	protected int perRow() {
		return mode == GALLERY ? Math.max(1, width / itemExtent)
				: mode == MASONRY ? Math.max(1, Math.min(8, width / itemExtent)) : 1;
	}

	@Override
	public long logicalExtent() {
		if (mode == MASONRY) {
			planMasonry();
			return masonryExtent;
		}
		return mode == KANBAN ? (long) columnDepth() * itemExtent : (((long) logicalCount() + perRow() - 1) / perRow()) * itemExtent;
	}

	@Override
	protected void project() {
		if (mode == MASONRY) {
			planMasonry();
			int n = logicalCount();
			if (viewLength() <= 0 || n == 0) {
				Arrays.fill(window, 0);
				return;
			}
			// Greedy shortest-column placement has non-decreasing start positions.
			// Include tall tiles starting before the visible interval.
			int first = masonryLowerBound(Math.max(0L, scroll - (long) itemExtent * 4));
			int end = masonryLowerBound(scroll + viewLength());
			window[0] = first;
			window[1] = end;
			window[2] = Math.max(0, first - overscan);
			window[3] = (int) Math.min(n, (long) end + overscan);
			return;
		}
		super.project();
		if (mode == GALLERY) {
			int n = logicalCount(), per = perRow();
			window[0] = (int) Math.min((long) n, (long) window[0] * per);
			window[1] = (int) Math.min((long) n, (long) window[1] * per);
			window[2] = (int) Math.min((long) n, (long) window[2] * per);
			window[3] = (int) Math.min((long) n, (long) window[3] * per);
		}
	}

	private void planMasonry() {
		int n = logicalCount(), per = perRow();
		if (masonryTop == null || masonryTop.length < n) {
			masonryTop = new long[n];
			masonryColumn = new short[n];
			masonryHeights = new int[n];
		}
		long[] depth = new long[per];
		for (int k = 0; k < n; k++) {
			int col = 0;
			for (int c = 1; c < per; c++) {
				if (depth[c] < depth[col]) {
					col = c;
				}
			}
			int i = itemAt(k);
			int requested = value[i];
			int h = requested <= 0 ? itemExtent : (int) Math.min((long) requested, (long) itemExtent * 4);
			masonryTop[k] = depth[col];
			masonryColumn[k] = (short) col;
			masonryHeights[k] = h;
			depth[col] += h;
		}
		masonryExtent = 0;
		for (long d : depth) {
			masonryExtent = Math.max(masonryExtent, d);
		}
	}

	private int masonryLowerBound(long target) {
		int lo = 0, hi = logicalCount();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (masonryTop[mid] < target) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	private int columnDepth() {
		int[] depth = new int[Math.max(1, columns)];
		int max = 0;
		for (int i = 0; i < count; i++) {
			max = Math.max(max, ++depth[Math.max(0, Math.min(depth.length - 1, group[i]))]);
		}
		return max;
	}

	@Override
	protected void doLayout() {
		columns = 1;
		for (int i = 0; i < count; i++) {
			setRect(i, 0, 0, 0, 0);
			columns = mode == KANBAN ? Math.max(columns, group[i] + 1) : 1;
		}
		if (mode == KANBAN) {
			board();
		} else if (mode == MASONRY) {
			masonry();
		} else {
			rows();
		}
		setPlane(OVERLAY, 0, 0, width, rowHeight() + pad());
	}

	private void masonry() {
		int per = perRow(), tile = width / per;
		for (int k = window[2]; k < window[3]; k++) {
			int i = itemAt(k);
			int x = masonryColumn[k] * tile + gap();
			int y = (int) (masonryTop[k] - scroll);
			setRect(i, x, y, Math.max(0, tile - 2 * gap()), Math.max(0, masonryHeights[k] - gap()));
		}
	}

	private void rows() {
		int per = perRow(), tile = width / per;
		for (int k = window[2]; k < window[3]; k++) {
			int i = itemAt(k), row = k / per, col = k % per;
			int y = (int) ((long) row * itemExtent - scroll) + pull;
			int x = mode == MESSAGES ? (group[i] == 1 ? width / 4 : 0) : col * tile;
			int w = mode == MESSAGES ? width * 3 / 4 : mode == GALLERY ? tile : width;
			setRect(i, x + (mode == SWIPE ? aux[i] : 0), y, w, itemExtent - (mode == GALLERY || mode == MESSAGES ? gap() : 1));
		}
	}

	private void board() {
		int colW = width / Math.max(1, columns);
		int[] depth = new int[Math.max(1, columns)];
		for (int i = 0; i < count; i++) {
			int c = Math.max(0, Math.min(columns - 1, group[i]));
			int y = rowHeight() + (int) ((long) depth[c]++ * itemExtent - scroll);
			if (y + itemExtent > rowHeight() && y < height) {
				setRect(i, c * colW + gap(), y, colW - 2 * gap(), itemExtent - gap());
			}
		}
	}

	@Override
	protected void emit(WebPaint p) {
		if (mode == KANBAN) {
			headers(p);
		}
		if (mode == PULL && pull > 0) {
			indicator(p);
		}
		// The existing modes keep their complete scan and subclass painting
		// semantics. Only the exact built-in Masonry owner has resident-only rects.
		boolean bounded = mode == MASONRY && getClass() == MasonryModel.class;
		int first = bounded ? window[2] : 0, end = bounded ? window[3] : count;
		for (int k = first; k < end; k++) {
			int i = bounded ? itemAt(k) : k;
			int b = i * 4;
			if (rects[b + 2] > 0 && (flags[i] & HIDDEN) == 0) {
				cell(p, i, rects[b], rects[b + 1], rects[b + 2], rects[b + 3]);
			}
		}
	}

	private void headers(WebPaint p) {
		int colW = width / Math.max(1, columns);
		for (int c = 0; c < columns; c++) {
			p.round(c * colW + 2, 0, colW - 4, height, radius(), WebTheme.mix(theme.slot(WebTheme.FG), theme.slot(WebTheme.BG), 4));
			muted(p, "#" + (c + 1), c * colW + gap(), 0, colW - 2 * gap(), rowHeight(), WebPaint.BOLD);
		}
	}

	private void indicator(WebPaint p) {
		int r = rowHeight(), x = width / 2 - r / 2;
		p.translate(plane(OVERLAY, 4), plane(OVERLAY, 5));
		p.oval(x, Math.max(0, pull - r) / 2, r, r, theme.slot(WebTheme.SURFACE));
		p.arc(x + 4, Math.max(0, pull - r) / 2 + 4, r - 8, r - 8, phase * 360 / 1000, Math.min(300, pull * 3), theme.slot(WebTheme.ACCENT));
		p.translate(0, 0);
	}

	private void cell(WebPaint p, int i, int x, int y, int w, int h) {
		if (mode == SWIPE && aux[i] < 0) {
			p.rect(x + w + aux[i], y, -aux[i], h, theme.slot(WebTheme.DANGER));
			p.text(x + w + aux[i], y, -aux[i], h, 0xffffff, font() | WebPaint.CENTER, "Delete");
		}
		boolean bubble = mode == MESSAGES || mode == KANBAN || mode == GALLERY || mode == MASONRY;
		int st = flags[i] | (i == focus ? SELECTED : 0);
		if (bubble) {
			p.round(x, y, w, h, radius(), mode == MESSAGES && group[i] == 1 ? theme.fill(V_PRIMARY, flags[i]) : theme.fill(V_DEFAULT, st));
			p.roundLine(x, y, w, h, radius(), theme.line(V_DEFAULT, st));
		} else {
			p.rect(x, y, w, h, theme.fill(V_DEFAULT, st));
			p.line(x, y + h, x + w, y + h, theme.slot(WebTheme.BORDER));
		}
		int ink = mode == MESSAGES && group[i] == 1 ? 0xffffff : theme.ink(V_DEFAULT, st);
		String s = text[i] == null ? Integer.toString(i + 1) : text[i];
		p.text(x + pad(), y, w - 2 * pad(), h, ink, font() | (mode == GALLERY || mode == MASONRY ? WebPaint.CENTER : rtl ? WebPaint.RIGHT : 0), s);
		if ((flags[i] & DRAGGING) != 0) {
			p.roundLine(x + 1, y + 1, w - 2, h - 2, radius(), theme.slot(WebTheme.ACCENT));
		}
	}

	@Override
	protected boolean vertical() {
		return mode != GALLERY;
	}

	@Override
	public void scrollTo(long offset) {
		super.scrollTo(offset);
		if (mode == INFINITE && window[1] >= logicalCount() - threshold && loadedAt != count) {
			loadedAt = count;
			fire(-1, D_LOAD);
		}
	}

	@Override
	public void setThreshold(int items) {
		threshold = Math.max(0, items);
	}

	@Override
	public boolean needsMore() {
		return mode == INFINITE && loadedAt == count;
	}

	@Override
	public int pull() {
		return pull;
	}

	@Override
	public void endRefresh() {
		setState(-1, LOADING, false);
		pull = 0;
		animating = false;
		relayout(-1, D_LOAD);
	}

	@Override
	public void move(int from, int to) {
		check(from);
		check(to);
		String t = text[from], d = detail[from];
		int f = flags[from], v = value[from], a = aux[from], g = group[from];
		remove(from);
		insertAt(to, t, d, f, v, a, g);
		setFocus(to);
	}

	private void insertAt(int to, String t, String d, int f, int v, int a, int g) {
		add(t, d);
		for (int i = count - 1; i > to; i--) {
			text[i] = text[i - 1]; detail[i] = detail[i - 1]; flags[i] = flags[i - 1];
			value[i] = value[i - 1]; aux[i] = aux[i - 1]; group[i] = group[i - 1];
		}
		text[to] = t; detail[to] = d; flags[to] = f; value[to] = v; aux[to] = a; group[to] = g;
		relayout(to, D_ITEMS);
	}

	@Override
	public void moveToColumn(int item, int column) {
		setGroup(item, Math.max(0, column));
	}

	@Override
	public void swipe(int item, int offset) {
		setAux(item, Math.max(-actionWidth(), Math.min(0, offset)));
	}

	/** Width of the revealed swipe action. */
	protected int actionWidth() {
		return rowHeight() * 3;
	}

	@Override
	protected boolean onKey(int key, int ch, int mods) {
		if (focus < 0) {
			return false;
		}
		if ((mods & MOD_ALT) != 0 && (key == KEY_UP || key == KEY_DOWN) && mode != GALLERY) {
			int to = focus + (key == KEY_DOWN ? 1 : -1);
			if (to >= 0 && to < count) {
				move(focus, to);
			}
			return true;
		}
		if (mode == KANBAN && (mods & MOD_CTRL) != 0 && (key == KEY_LEFT || key == KEY_RIGHT)) {
			moveToColumn(focus, group[focus] + ((key == KEY_RIGHT) != rtl ? 1 : -1));
			return true;
		}
		return listKey(key);
	}

	private boolean listKey(int key) {
		switch (key) {
		case KEY_DELETE:
			if (mode == SWIPE) {
				swipe(focus, aux[focus] < 0 ? 0 : -actionWidth());
			}
			return mode == SWIPE;
		case KEY_PAGE_DOWN: case KEY_PAGE_UP:
			int n = Math.max(1, viewLength() / itemExtent) * (key == KEY_PAGE_DOWN ? 1 : -1);
			setFocus(Math.max(0, Math.min(count - 1, focus + n)));
			reveal(focus);
			return true;
		default: return false;
		}
	}

	@Override
	public void setFocus(int item) {
		super.setFocus(item);
		if (item >= 0) {
			reveal(item);
		}
	}

	/** Scrolls the minimum so the item's row is inside the viewport. */
	public void reveal(int item) {
		int pos = item;
		for (int k = 0; k < logicalCount(); k++) {
			pos = itemAt(k) == item ? k : pos;
		}
		if (mode == MASONRY) {
			if (pos >= logicalCount() || itemAt(pos) != item) {
				return; // Filtered out: do not scroll to a nonresident logical position.
			}
			planMasonry();
		}
		long top = mode == MASONRY ? masonryTop[pos] : (long) (pos / perRow()) * itemExtent;
		int extent = mode == MASONRY ? masonryHeights[pos] : itemExtent;
		if (mode != KANBAN && (top < scroll || top + extent > scroll + viewLength())) {
			scrollTo(top < scroll ? top : top + extent - viewLength());
		}
	}

	@Override
	protected void activate(int item) {
		if (mode == SWIPE && aux[item] < 0) {
			remove(item);
			fire(-1, D_ACTION);
			return;
		}
		fire(item, D_ACTION);
	}

	@Override
	protected void onWheel(int delta) {
		if (mode == PULL && scroll == 0 && delta < 0) {
			overscroll(-delta * 16);
		} else {
			scrollTo(scroll + (long) delta * itemExtent);
		}
	}

	private void overscroll(int px) {
		pull = Math.min(rowHeight() * 3, pull + px);
		if (pull >= rowHeight() * 2 && !is(-1, LOADING)) {
			setState(-1, LOADING, true);
			animate();
			fire(-1, D_LOAD);
		}
		movePlane(OVERLAY, 0, pull - rowHeight());
		relayout(-1, D_SCROLL);
	}

	@Override
	public void pointer(int kind, int x, int y, int mods) {
		if (kind == P_DOWN) {
			dragFrom = hitTest(x, y);
			dragX = x;
			dragY = y;
		}
		if (kind == P_UP && dragFrom >= 0) {
			drop(x, y);
		}
		super.pointer(kind, x, y, mods);
	}

	@Override
	protected void onDrag(int x, int y) {
		if (dragFrom < 0 || dragFrom >= count) {
			return;
		}
		if (mode == SWIPE) {
			swipe(dragFrom, x - dragX);
		} else if (mode == PULL && scroll == 0 && y > dragY) {
			overscroll(Math.max(0, (y - dragY) / 2 - pull));
		} else {
			setState(dragFrom, DRAGGING, true);
		}
	}

	private void drop(int x, int y) {
		int from = dragFrom;
		dragFrom = -1;
		if (from >= count) {
			return;
		}
		if (mode == SWIPE) {
			swipe(from, aux[from] < -actionWidth() / 2 ? -actionWidth() : 0);
		} else if (is(from, DRAGGING)) {
			setState(from, DRAGGING, false);
			int to = hitTest(x, y);
			if (mode == KANBAN) {
				moveToColumn(from, x * Math.max(1, columns) / Math.max(1, width));
			} else if (to >= 0 && to != from) {
				move(from, to);
			}
		}
	}

	@Override
	protected boolean advance(int ms) {
		phase = (phase + ms) % 1000;
		return is(-1, LOADING);
	}
}
