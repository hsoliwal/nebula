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
	/** Writable declarative bits; pointer, focus, dragging and loading stay owned by SWT. */
	private static final int CONTROLLED = DISABLED | SELECTED | ERROR | OPEN | CHECKED | MIXED
			| EXPANDED | VISITED | HIDDEN | DONE | READONLY;
	private int pull, loadedAt = -1, threshold = 5, columns = 1, dragFrom = -1, dragX, dragY;
	private final boolean residentOnlyLayout;
	// Only the previous paint window is retired on a warm left-to-right layout.
	private int[] residentIds = new int[0];
	private int previousResidents;
	private boolean mirroredRects;
	/** React-style identities are references owned by this collection, never per-item proxies. */
	private String[] stableKeys;
	// The parent owns primitive geometry for every masonry item; no per-tile objects.
	private long[] masonryTop;
	private short[] masonryColumn;
	private int[] masonryHeights;
	private long masonryExtent;
	// Reusable, source-index-ordered resident ids for paint and pointer targeting.
	// Source order preserves overlapping-cell z-order when filter positions differ.
	private int[] residentOrder = new int[8];
	// Optional Flexbox-like zero-basis gallery weights, indexed by existing item id.
	// Unsigned 16-bit weights travel with stable keys; no per-tile style objects.
	private short[] galleryFlexWeights;

	public CollectionModel(String concept, int role, int mode, int shape, int variant) {
		super(concept, role, mode, shape, variant);
		residentOnlyLayout = originalLayoutOwner(getClass());
		itemExtent = mode == MESSAGES ? 56 : mode == GALLERY || mode == MASONRY ? 112 : 40;
	}

	/** Exact built-in owners alone can rely on resident-only rectangles and pure layout hooks. */
	private static boolean originalLayoutOwner(Class<?> leaf) {
		return leaf == CollectionModel.class || leaf == SwipeActionsModel.class
				|| leaf == PullToRefreshModel.class || leaf == InfiniteScrollModel.class
				|| leaf == ReorderDragModel.class || leaf == MessageListModel.class
				|| leaf == CommandPaletteModel.class || leaf == MasonryModel.class;
	}

	/**
	 * Reconciles a declarative sequence in one layout/delta publication. Stable keys preserve
	 * per-row primitive state, value, group, aux and focus across reorder and relabel; new
	 * keys start empty. The caller's arrays are copied before validation and never retained.
	 * This is a native row-state bridge, not a JavaScript/React hook interpreter.
	 */
	@Override
	public void reconcile(String[] keys, String[] labels) {
		applySnapshot(keys, labels, null, null);
	}

	/**
	 * Applies React-style controlled boolean properties to the existing native lanes.
	 * Uncontrolled event state is retained, even while the declarative order changes.
	 * The unsigned short masks and values are cloned and validated before mutation.
	 */
	@Override
	public void reconcileProps(String[] keys, String[] labels, short[] masks, short[] values) {
		if (masks == null || values == null || keys == null
				|| masks.length != keys.length || values.length != keys.length) {
			throw new IllegalArgumentException("property masks and values must match key count");
		}
		applySnapshot(keys, labels, masks.clone(), values.clone());
	}

	private void applySnapshot(String[] keys, String[] labels, short[] masks, short[] props) {
		if (keys == null || labels == null || keys.length != labels.length) {
			throw new IllegalArgumentException("key and label arrays must have equal non-null lengths");
		}
		String[] incoming = keys.clone(), words = labels.clone();
		int[] unique = keyTable(incoming.length);
		indexKeys(incoming, unique);
		if (masks != null) {
			for (int i = 0; i < masks.length; i++) {
				int mask = masks[i] & 0xffff, value = props[i] & 0xffff;
				if ((mask & ~CONTROLLED) != 0 || (value & ~mask) != 0) {
					throw new IllegalArgumentException("invalid controlled state at " + i);
				}
			}
		}
		boolean sameRows = sameSnapshot(incoming, words);
		if (sameRows && masks == null) {
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
		short[] stagedFlex = galleryFlexWeights == null ? null : new short[n];
		int nextFocus = -1, nextHover = -1, nextPressed = -1;
		boolean changedProps = false;
		for (int i = 0; i < n; i++) {
			int old = previous == null ? -1 : lookup(incoming[i], stableKeys, previous);
			if (old >= 0) {
				stagedFlags[i] = flags[old];
				stagedValue[i] = value[old];
				stagedAux[i] = aux[old];
				stagedGroup[i] = group[old];
				stagedDetail[i] = detail[old];
				if (old == focus) nextFocus = i;
				if (old == hover) nextHover = i;
				if (old == pressed) nextPressed = i;
			}
			if (stagedFlex != null) {
				stagedFlex[i] = old < 0 ? (short) 1 : galleryFlexWeights[old];
			}
			if (masks != null) {
				int mask = masks[i] & 0xffff;
				int controlled = (stagedFlags[i] & ~mask) | (props[i] & 0xffff);
				changedProps |= controlled != stagedFlags[i];
				stagedFlags[i] = controlled;
			}
		}
		if (sameRows && !changedProps) {
			return;
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
		if (stagedFlex != null) {
			System.arraycopy(stagedFlex, 0, galleryFlexWeights, 0, n);
			Arrays.fill(galleryFlexWeights, n, Math.max(n, retired), (short) 1);
		}
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

	@Override
	public void setGalleryFlexWeights(short[] weights) {
		if (mode != GALLERY) {
			throw new UnsupportedOperationException("flex weights require gallery mode");
		}
		short[] staged = weights == null ? null : weights.clone();
		if (staged != null) {
			if (staged.length != count) {
				throw new IllegalArgumentException("flex weight count must equal gallery item count");
			}
			for (int i = 0; i < staged.length; i++) {
				if ((staged[i] & 0xffff) == 0) {
					throw new IllegalArgumentException("zero flex weight at " + i);
				}
			}
		}
		if (staged == null && galleryFlexWeights == null) {
			return;
		}
		if (staged != null && galleryFlexWeights != null) {
			boolean unchanged = true;
			for (int i = 0; i < count; i++) {
				unchanged &= staged[i] == galleryFlexWeights[i];
			}
			if (unchanged) {
				return;
			}
		}
		galleryFlexWeights = staged == null ? null : Arrays.copyOf(staged, flags.length);
		if (galleryFlexWeights != null) {
			Arrays.fill(galleryFlexWeights, count, flags.length, (short) 1);
		}
		relayout(-1, D_LAYOUT);
	}

	@Override
	public int galleryFlexWeight(int item) {
		if (mode != GALLERY) {
			throw new UnsupportedOperationException("flex weights require gallery mode");
		}
		check(item);
		return galleryFlexWeights == null ? 1 : galleryFlexWeights[item] & 0xffff;
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
		if (galleryFlexWeights != null && count < galleryFlexWeights.length) {
			galleryFlexWeights[count] = 1;
		}
		return super.add(label, detail);
	}

	@Override
	protected void grow(int cap) {
		super.grow(cap);
		if (galleryFlexWeights != null) {
			int old = galleryFlexWeights.length;
			galleryFlexWeights = Arrays.copyOf(galleryFlexWeights, cap);
			Arrays.fill(galleryFlexWeights, old, cap, (short) 1);
		}
	}

	@Override
	public void remove(int item) {
		check(item);
		if (galleryFlexWeights != null) {
			System.arraycopy(galleryFlexWeights, item + 1, galleryFlexWeights, item,
					count - item - 1);
			galleryFlexWeights[count - 1] = 1;
		}
		super.remove(item);
		stableKeys = null;
	}

	@Override
	public void clear() {
		stableKeys = null;
		galleryFlexWeights = null;
		super.clear();
	}

	@Override
	public void setLogicalCount(int n) {
		if (galleryFlexWeights != null && n >= 0) {
			if (n > count) {
				Arrays.fill(galleryFlexWeights, count, Math.min(n, galleryFlexWeights.length), (short) 1);
			} else {
				Arrays.fill(galleryFlexWeights, n, count, (short) 1);
			}
		}
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
		boolean bounded = residentOnlyLayout && mode != KANBAN;
		columns = 1;
		if (bounded && !rtl && !mirroredRects) {
			for (int k = 0; k < previousResidents; k++) {
				setRect(residentIds[k], 0, 0, 0, 0);
			}
		} else {
			// RTL exposes mirrored zero-width rectangles through itemRect/snapshot.
			// Keep their exact coordinates, including the first layout back in LTR.
			// Unknown subclasses retain every original setRect hook invocation.
			for (int i = 0; i < count; i++) {
				setRect(i, 0, 0, 0, 0);
				columns = mode == KANBAN ? Math.max(columns, group[i] + 1) : 1;
			}
		}
		if (mode == KANBAN) {
			board();
		} else if (mode == MASONRY) {
			masonry();
		} else {
			rows();
		}
		if (bounded) {
			int n = window[3] - window[2];
			if (n > residentIds.length) {
				residentIds = Arrays.copyOf(residentIds, Math.max(n, Math.max(8, residentIds.length * 2)));
			}
			for (int k = 0; k < n; k++) {
				residentIds[k] = itemAt(window[2] + k);
			}
			previousResidents = n;
			mirroredRects = rtl;
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
		if (mode == GALLERY && galleryFlexWeights != null) {
			flexRows();
			return;
		}
		int per = perRow(), tile = width / per;
		for (int k = window[2]; k < window[3]; k++) {
			int i = itemAt(k), row = k / per, col = k % per;
			int y = (int) ((long) row * itemExtent - scroll) + pull;
			int x = mode == MESSAGES ? (group[i] == 1 ? width / 4 : 0) : col * tile;
			int w = mode == MESSAGES ? width * 3 / 4 : mode == GALLERY ? tile : width;
			setRect(i, x + (mode == SWIPE ? aux[i] : 0), y, w, itemExtent - (mode == GALLERY || mode == MESSAGES ? gap() : 1));
		}
	}

	/**
	 * A zero-basis, weighted Flexbox-row subset. Each row consumes its available
	 * width, including a short final row, while retaining logical row height,
	 * viewport residency, hit geometry and existing per-item identity.
	 */
	private void flexRows() {
		int per = perRow(), n = logicalCount();
		int rowStart = window[2] / per * per;
		while (rowStart < window[3]) {
			int end = (int) Math.min((long) n, (long) rowStart + per);
			long total = 0;
			for (int k = rowStart; k < end; k++) {
				total += galleryFlexWeights[itemAt(k)] & 0xffff;
			}
			long preceding = 0;
			int y = (int) ((long) (rowStart / per) * itemExtent - scroll) + pull;
			for (int k = rowStart; k < end; k++) {
				int i = itemAt(k), x = flexEdge(preceding, total);
				preceding += galleryFlexWeights[i] & 0xffff;
				if (k >= window[2] && k < window[3]) {
					setRect(i, x, y, flexEdge(preceding, total) - x, itemExtent - gap());
				}
			}
			rowStart = end;
		}
	}

	/** Integer pixel edges avoid cumulative rounding; enormous products fall back safely. */
	private int flexEdge(long prefix, long total) {
		if (prefix == 0 || width == 0) {
			return 0;
		}
		if (prefix <= Long.MAX_VALUE / width) {
			return (int) (prefix * width / total);
		}
		return java.math.BigInteger.valueOf(prefix).multiply(java.math.BigInteger.valueOf(width))
				.divide(java.math.BigInteger.valueOf(total)).intValueExact();
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

	/**
	 * Only built-in row/tile owners admit resident-only traversal. A subclass may
	 * set other rectangles or paint outside its projected resident window;
	 * Kanban's independently scrolling columns do not form a contiguous window.
	 */
	private boolean residentOnlyOwner() {
		return mode != KANBAN && (getClass() == CollectionModel.class || getClass() == MasonryModel.class);
	}

	/** Rebuilds a primitive projection in the original source-index z-order. */
	private int residentOrder() {
		int n = window[3] - window[2];
		if (n > residentOrder.length) {
			residentOrder = Arrays.copyOf(residentOrder, n);
		}
		for (int k = 0; k < n; k++) {
			residentOrder[k] = itemAt(window[2] + k);
		}
		Arrays.sort(residentOrder, 0, n);
		return n;
	}

	@Override
	public int hitTest(int x, int y) {
		if (!residentOnlyOwner()) {
			return super.hitTest(x, y);
		}
		int n = residentOrder();
		for (int k = n - 1; k >= 0; k--) {
			int i = residentOrder[k], b = i * 4;
			if ((flags[i] & HIDDEN) == 0 && x >= rects[b] && y >= rects[b + 1]
					&& x < rects[b] + rects[b + 2] && y < rects[b + 1] + rects[b + 3]) {
				return i;
			}
		}
		return -1;
	}

	@Override
	protected void emit(WebPaint p) {
		if (mode == KANBAN) {
			headers(p);
		}
		if (mode == PULL && pull > 0) {
			indicator(p);
		}
		if (mode == MASONRY && getClass() == MasonryModel.class) {
			// This owner's preexisting bounded painter draws in filtered
			// viewport order, which differs from source order on some filters.
			for (int k = window[2]; k < window[3]; k++) {
				emitItem(p, itemAt(k));
			}
		} else if (residentOnlyOwner()) {
			int n = residentOrder();
			for (int k = 0; k < n; k++) {
				emitItem(p, residentOrder[k]);
			}
		} else {
			// Preserve subclass full scans and Kanban's independent column order.
			for (int i = 0; i < count; i++) {
				emitItem(p, i);
			}
		}
	}

	private void emitItem(WebPaint p, int i) {
		int b = i * 4;
		if (rects[b + 2] > 0 && (flags[i] & HIDDEN) == 0) {
			cell(p, i, rects[b], rects[b + 1], rects[b + 2], rects[b + 3]);
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
		short flex = galleryFlexWeights == null ? (short) 1 : galleryFlexWeights[from];
		remove(from);
		insertAt(to, t, d, f, v, a, g, flex);
		setFocus(to);
	}

	private void insertAt(int to, String t, String d, int f, int v, int a, int g, short flex) {
		add(t, d);
		for (int i = count - 1; i > to; i--) {
			text[i] = text[i - 1]; detail[i] = detail[i - 1]; flags[i] = flags[i - 1];
			value[i] = value[i - 1]; aux[i] = aux[i - 1]; group[i] = group[i - 1];
			if (galleryFlexWeights != null) galleryFlexWeights[i] = galleryFlexWeights[i - 1];
		}
		text[to] = t; detail[to] = d; flags[to] = f; value[to] = v; aux[to] = a; group[to] = g;
		if (galleryFlexWeights != null) galleryFlexWeights[to] = flex;
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
