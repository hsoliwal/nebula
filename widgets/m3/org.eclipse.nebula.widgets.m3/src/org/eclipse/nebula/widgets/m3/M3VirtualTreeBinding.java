/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Original SWT/RWT VIRTUAL Tree receiver for a keyed, already-resident hierarchy.
 *
 * <p>Tree, TreeItem, expansion, scrollbars, item realization and painting stay
 * owned by SWT/RWT. No extra Control is created for any logical node. This
 * binding caches only four 64-sibling pages. A source-specific adapter may
 * read a published immutable snapshot but MUST NOT do blocking HTTP, SQL,
 * JPA or other network I/O on the UI thread, including from SetData.</p>
 *
 * <p>This is a product receiving contract, NOT a universal GWT Tree,
 * Vaadin TreeGrid, Wicket tree or MyFaces Trinidad source transpiler.
 * Those source/model/lifecycle transformations belong in Synexia.
 * Real RAP browser behavior, native SWT pixels, accessibility and resource
 * ownership require independent platform qualification.</p>
 */
public final class M3VirtualTreeBinding implements AutoCloseable {

    public static final int PAGE_ROWS = 64;
    public static final int HOT_PAGES = 4;
    public static final int MAX_COLUMNS = 64;
    public static final int MAX_KEY_UNITS = 256;
    public static final int MAX_CELL_UNITS = 16384;
    public static final int MAX_PAGE_TEXT_UNITS = 262144;
    public static final int MAX_SELECTION_KEYS = 256;
    public static final int MAX_KEY_DEPTH = 64;
    public static final String ROW_KEY = "m3.web.id";

    private static final String[] EMPTY = {};

    /**
     * All operations are synchronous, bounded and UI-thread safe.
     *
     * <p>The root parent key is null. Every node key MUST be globally unique.
     * {@code path(key)} returns root-to-leaf view indices for a visible key,
     * or null if filtered/removed. No hierarchy walk is performed by this
     * binding to discover a key: indexed lookup belongs to the source.</p>
     */
    public interface Nodes {
        int rootCount();
        int columns();
        Page fetch(String parentKey, int offset, int length);
        int[] path(String stableKey);
    }

    /** Flat, immutable sibling-page lanes. No per-node entry objects. */
    public static final class Page {
        private final String parentKey;
        private final int offset;
        private final int columns;
        private final String[] keys;
        private final int[] children;
        private final String[] text;

        public Page(String parentKey, int offset, int columns,
                String[] keys, int[] children, String[] text) {
            if (offset < 0 || columns < 1 || columns > MAX_COLUMNS
                    || keys == null || children == null || text == null
                    || keys.length < 1 || keys.length > PAGE_ROWS
                    || children.length != keys.length
                    || text.length != keys.length * columns) {
                throw new IllegalArgumentException("Invalid virtual tree page geometry");
            }
            if (parentKey != null && (parentKey.isEmpty()
                    || parentKey.length() > MAX_KEY_UNITS)) {
                throw new IllegalArgumentException("Invalid parent key");
            }
            int units = 0;
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if (key == null || key.isEmpty() || key.length() > MAX_KEY_UNITS
                        || key.equals(parentKey) || children[i] < 0) {
                    throw new IllegalArgumentException("Invalid node key or child count");
                }
                for (int j = 0; j < i; j++) {
                    if (key.equals(keys[j])) {
                        throw new IllegalArgumentException("Duplicate sibling key");
                    }
                }
            }
            for (String cell : text) {
                if (cell == null || cell.length() > MAX_CELL_UNITS) {
                    throw new IllegalArgumentException("Null or oversized tree text");
                }
                units += cell.length();
                if (units > MAX_PAGE_TEXT_UNITS) {
                    throw new IllegalArgumentException("Tree page exceeds text budget");
                }
            }
            this.parentKey = parentKey;
            this.offset = offset;
            this.columns = columns;
            this.keys = keys.clone();
            this.children = children.clone();
            this.text = text.clone();
        }

        public String parentKey() { return parentKey; }
        public int offset() { return offset; }
        public int size() { return keys.length; }
        public int columns() { return columns; }
        public String keyAt(int row) { return keys[row]; }
        public int childCountAt(int row) { return children[row]; }
        public String textAt(int row, int column) {
            return text[row * columns + column];
        }
    }

    private final Tree tree;
    private final Listener setData;
    private final Listener selection;
    private final Listener dispose;
    private final Page[] pages = new Page[HOT_PAGES];
    private final long[] ages = new long[HOT_PAGES];
    private Nodes nodes;
    private String[] selectedKeys = EMPTY;
    private long clock;
    private boolean fetching;
    private boolean closed;

    private M3VirtualTreeBinding(Tree tree, Nodes nodes) {
        this.tree = tree;
        this.nodes = nodes;
        setData = this::onSetData;
        selection = event -> captureSelection();
        dispose = event -> release();
        tree.addListener(SWT.SetData, setData);
        tree.addListener(SWT.Selection, selection);
        tree.addListener(SWT.DefaultSelection, selection);
        tree.addListener(SWT.Dispose, dispose);
    }

    /** Binds the existing Tree; TreeColumns must be created by the caller first. */
    public static M3VirtualTreeBinding bind(Tree tree, Nodes nodes) {
        if (tree == null || nodes == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        if ((tree.getStyle() & SWT.VIRTUAL) == 0) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
        verifyColumns(tree, nodes);
        int count = checkedCount(nodes.rootCount());
        M3VirtualTreeBinding result = new M3VirtualTreeBinding(tree, nodes);
        try {
            tree.setItemCount(count);
        } catch (RuntimeException failure) {
            result.close();
            throw failure;
        }
        return result;
    }

    private static int checkedCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative hierarchy count");
        }
        return count;
    }

    private static void verifyColumns(Tree tree, Nodes nodes) {
        int columns = Math.max(1, tree.getColumnCount());
        if (columns > MAX_COLUMNS || nodes.columns() != columns) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
    }

    private void onSetData(Event event) {
        if (closed || fetching || !(event.item instanceof TreeItem)) {
            throw new IllegalStateException("Invalid or reentrant Tree.SetData");
        }
        TreeItem item = (TreeItem) event.item;
        TreeItem parent = item.getParentItem();
        String parentKey = null;
        int siblingCount;
        if (parent == null) {
            siblingCount = tree.getItemCount();
        } else {
            Object key = parent.getData(ROW_KEY);
            if (!(key instanceof String)) {
                throw new IllegalStateException("Parent must be initialized before child");
            }
            parentKey = (String) key;
            siblingCount = parent.getItemCount();
        }
        int index = event.index; // always relative to parent, not flattened row.
        if (index < 0 || index >= siblingCount) {
            throw new IllegalArgumentException("Stale sibling index");
        }
        Page page = pageFor(parentKey, index, siblingCount);
        int relative = index - page.offset();
        item.setData(ROW_KEY, page.keyAt(relative));
        for (int column = 0; column < page.columns(); column++) {
            item.setText(column, page.textAt(relative, column));
        }
        item.setItemCount(page.childCountAt(relative));
    }

    private Page pageFor(String parentKey, int index, int siblingCount) {
        int offset = (index / PAGE_ROWS) * PAGE_ROWS;
        long now = ++clock;
        if (now <= 0) {
            Arrays.fill(ages, 0);
            now = clock = 1;
        }
        for (int i = 0; i < pages.length; i++) {
            Page page = pages[i];
            if (page != null && page.offset() == offset
                    && Objects.equals(page.parentKey(), parentKey)) {
                ages[i] = now;
                return page;
            }
        }
        int length = Math.min(PAGE_ROWS, siblingCount - offset);
        Page candidate;
        fetching = true;
        try {
            candidate = nodes.fetch(parentKey, offset, length);
        } finally {
            fetching = false;
        }
        if (candidate == null || candidate.offset() != offset
                || candidate.size() != length
                || candidate.columns() != Math.max(1, tree.getColumnCount())
                || !Objects.equals(candidate.parentKey(), parentKey)) {
            throw new IllegalArgumentException("Provider returned a different sibling page");
        }
        int slot = 0;
        for (int i = 1; i < pages.length; i++) {
            if (pages[slot] != null
                    && (pages[i] == null || ages[i] < ages[slot])) {
                slot = i;
            }
        }
        pages[slot] = candidate;
        ages[slot] = now;
        return candidate;
    }

    /** Current selected source keys; a defensive copy with no TreeItem handles. */
    public String[] getSelectedKeys() {
        checkOpen();
        return selectedKeys.clone();
    }

    /** Preserve keys for temporarily filtered or collapsed nodes. */
    public void setSelectedKeys(String... keys) {
        checkOpen();
        validateKeys(keys);
        String[] candidate = keys.clone();
        verifyPaths(nodes, tree.getItemCount(), candidate);
        TreeItem[] visible = findVisible(candidate);
        selectedKeys = candidate;
        tree.setSelection(visible);
    }

    private void captureSelection() {
        if (closed) {
            return;
        }
        TreeItem[] selected = tree.getSelection();
        if (selected.length > MAX_SELECTION_KEYS) {
            throw new IllegalStateException("Too many selected tree nodes");
        }
        String[] keys = new String[selected.length];
        for (int i = 0; i < selected.length; i++) {
            // TreeItem.getData does not guarantee fresh application metadata
            // after Tree.clear(index,true). Resolve the virtual item's
            // content first so SWT.SetData repopulates a cleared item.
            selected[i].getText(0);
            Object key = selected[i].getData(ROW_KEY);
            if (!(key instanceof String)) {
                throw new IllegalStateException("Selected TreeItem has no stable key");
            }
            keys[i] = (String) key;
        }
        selectedKeys = keys;
    }

    private void validateKeys(String[] keys) {
        if (keys == null || keys.length > MAX_SELECTION_KEYS
                || (keys.length > 1 && (tree.getStyle() & SWT.MULTI) == 0)) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (key == null || key.isEmpty() || key.length() > MAX_KEY_UNITS) {
                SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            }
            for (int j = 0; j < i; j++) {
                if (key.equals(keys[j])) {
                    SWT.error(SWT.ERROR_INVALID_ARGUMENT);
                }
            }
        }
    }

    private static void verifyPaths(Nodes source, int roots, String[] keys) {
        for (String key : keys) {
            int[] path = source.path(key);
            if (path == null) {
                continue; // filtered or absent, keep key for later reappearance.
            }
            if (path.length < 1 || path.length > MAX_KEY_DEPTH
                    || path[0] < 0 || path[0] >= roots) {
                throw new IllegalArgumentException("Invalid root-to-node path");
            }
            for (int index : path) {
                if (index < 0) {
                    throw new IllegalArgumentException("Negative descendant index");
                }
            }
        }
    }

    private TreeItem[] findVisible(String[] keys) {
        TreeItem[] found = new TreeItem[keys.length];
        int count = 0;
        for (String key : keys) {
            int[] path = nodes.path(key);
            if (path == null) {
                continue;
            }
            TreeItem item = tree.getItem(path[0]);
            for (int depth = 1; item != null && depth < path.length; depth++) {
                // Ensure parent has received SetData before requesting a child;
                // otherwise its stable key and child count may still be absent.
                item.getText(0);
                if (!item.getExpanded() || path[depth] >= item.getItemCount()) {
                    item = null; // collapsed or removed: retain logical selection.
                } else {
                    item = item.getItem(path[depth]);
                }
            }
            if (item != null) {
                // Reading a virtual item's text may cause SWT.SetData. Do not
                // trust a stale source path unless its stable key matches.
                item.getText(0);
                if (!key.equals(item.getData(ROW_KEY))) {
                    throw new IllegalArgumentException("Path no longer resolves to key");
                }
                found[count++] = item;
            }
        }
        return Arrays.copyOf(found, count);
    }

    /**
     * Apply a published indexed snapshot. Missing/filtered selections remain
     * stored, but are not expanded to force them visible.
     */
    public void replaceNodes(Nodes next) {
        checkOpen();
        if (next == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        if (fetching) {
            throw new IllegalStateException("Cannot replace tree during SetData");
        }
        verifyColumns(tree, next);
        int count = checkedCount(next.rootCount());
        verifyPaths(next, count, selectedKeys); // preflight before Tree mutation
        nodes = next;
        clearCache();
        // Clear original TreeItem identity/expansion state, not merely text.
        // Reusing old item indices after a sorted snapshot can incorrectly
        // expand a *different* logical node in the new model.
        tree.setItemCount(0);
        tree.setItemCount(count);
        tree.setSelection(findVisible(selectedKeys));
    }

    /**
     * Invalidate bounded root damage; descendants of cleared roots are
     * re-requested lazily. Large/structural changes require replaceNodes().
     */
    public void invalidateRoots(int first, int last) {
        checkOpen();
        if (fetching) {
            throw new IllegalStateException("Cannot invalidate during SetData");
        }
        if (first < 0 || last < first || last >= tree.getItemCount()
                || (long) last - first >= PAGE_ROWS * HOT_PAGES) {
            SWT.error(SWT.ERROR_INVALID_RANGE);
        }
        for (int slot = 0; slot < pages.length; slot++) {
            Page page = pages[slot];
            if (page != null && page.parentKey() == null
                    && page.offset() <= last
                    && (long) page.offset() + page.size() > first) {
                pages[slot] = null;
                ages[slot] = 0;
            }
        }
        for (int i = first; i <= last; i++) {
            tree.clear(i, true);
        }
    }

    public int getResidentPageCount() {
        checkOpen();
        int count = 0;
        for (Page page : pages) {
            if (page != null) {
                count++;
            }
        }
        return count;
    }

    private void clearCache() {
        Arrays.fill(pages, null);
        Arrays.fill(ages, 0);
        clock = 0;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Virtual Tree binding closed");
        }
        tree.getItemCount(); // enforces SWT thread/disposal contract
    }

    private void release() {
        closed = true;
        clearCache();
        selectedKeys = EMPTY;
        nodes = null;
    }

    /** Detach listeners; never dispose the SWT/RWT Tree itself. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        release();
        if (!tree.isDisposed()) {
            tree.removeListener(SWT.SetData, setData);
            tree.removeListener(SWT.Selection, selection);
            tree.removeListener(SWT.DefaultSelection, selection);
            tree.removeListener(SWT.Dispose, dispose);
        }
    }
}
