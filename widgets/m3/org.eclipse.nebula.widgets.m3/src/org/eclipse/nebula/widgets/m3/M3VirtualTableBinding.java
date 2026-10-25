/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * Original SWT/RWT Table receiver for a virtual, keyed row provider.
 *
 * <p>The Table owns its viewport, scrollbars, items and SWT.SetData
 * lifecycle. This binding owns only four cached 64-row pages, a compact
 * selection-key array and three listeners; it never constructs one Control
 * per row or cell. Pages are fetched synchronously on the UI thread:
 * providers MUST use already-resident/indexed data and MUST NOT make
 * blocking database, HTTP or network calls here. Fetch asynchronously
 * into a snapshot outside this binding, then publish on the UI thread.
 * No RWT browser/session parity is implied by SWT API compilation.</p>
 *
 * <p>Web input bindings (Vaadin Grid/DataProvider, Wicket IDataProvider,
 * Spring Pageable or GWT CellTable) are build-time or application
 * adaptations to {@link Rows}; none is imported as a runtime dependency.
 * Stable keys, not view positions, preserve selection after replacement,
 * sorting or filtering. Original Nebula Grid/RAP Grid remains the
 * appropriate receiver when rich spans, frozen columns or cell editors
 * are needed.</p>
 */
public final class M3VirtualTableBinding implements AutoCloseable {

    /** Exact, already-indexed view of a potentially very large logical table. */
    public interface Rows {
        /** Logical rows after sorting/filtering; never materialize this count as Controls. */
        int size();

        /** One column for a Table with no TableColumns; otherwise exact TableColumn count. */
        int columns();

        /**
         * Fetch precisely {@code length} rows beginning at {@code offset}.
         * Must be bounded, non-blocking and safe on the SWT UI thread.
         */
        Page fetch(int offset, int length);

        /** Current visible index of stable key, or -1 when filtered/removed. */
        int indexOfKey(String key);
    }

    /**
     * Immutable, entry-less row page. Cell text is row-major in one String[]
     * rather than one object or array per cell; keys are stored separately.
     */
    public static final class Page {
        private final int offset;
        private final int columns;
        private final String[] keys;
        private final String[] text;

        public Page(int offset, int columns, String[] keys, String[] text) {
            if (offset < 0 || columns < 1 || columns > MAX_COLUMNS || keys == null
                    || text == null || keys.length < 1 || keys.length > PAGE_ROWS
                    || text.length != keys.length * columns) {
                throw new IllegalArgumentException("Invalid virtual table page geometry");
            }
            int units = 0;
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if (key == null || key.isEmpty() || key.length() > MAX_KEY_UNITS) {
                    throw new IllegalArgumentException("Invalid stable row key");
                }
                for (int j = 0; j < i; j++) {
                    if (key.equals(keys[j])) {
                        throw new IllegalArgumentException("Duplicate key in row page");
                    }
                }
            }
            for (String cell : text) {
                if (cell == null || cell.length() > MAX_CELL_UNITS) {
                    throw new IllegalArgumentException("Null or oversized table cell");
                }
                units += cell.length();
                if (units > MAX_PAGE_TEXT_UNITS) {
                    throw new IllegalArgumentException("Table page exceeds memory budget");
                }
            }
            this.offset = offset;
            this.columns = columns;
            this.keys = keys.clone();
            this.text = text.clone();
        }

        public int offset() { return offset; }
        public int size() { return keys.length; }
        public int columns() { return columns; }
        public String keyAt(int relativeRow) { return keys[relativeRow]; }
        public String textAt(int relativeRow, int column) {
            return text[relativeRow * columns + column];
        }

        private String[] cellTexts(int relativeRow) {
            return Arrays.copyOfRange(text, relativeRow * columns,
                    (relativeRow + 1) * columns);
        }
    }

    public static final int PAGE_ROWS = 64;
    public static final int HOT_PAGES = 4;
    public static final int MAX_SELECTION_KEYS = 4096;
    public static final int MAX_COLUMNS = 64;
    public static final int MAX_KEY_UNITS = 256;
    public static final int MAX_CELL_UNITS = 16384;
    public static final int MAX_PAGE_TEXT_UNITS = 262144;
    public static final String ROW_KEY = "m3.web.id";

    private static final String[] EMPTY = {};

    private final Table table;
    private final Listener setData;
    private final Listener selected;
    private final Listener onDispose;
    private final Page[] pages = new Page[HOT_PAGES];
    private final long[] ages = new long[HOT_PAGES];
    private Rows rows;
    private String[] selectedKeys = EMPTY;
    private long clock;
    private boolean fetching;
    private boolean closed;

    private M3VirtualTableBinding(Table table, Rows rows) {
        this.table = table;
        this.rows = rows;
        setData = this::onSetData;
        selected = event -> captureSelection();
        onDispose = event -> {
            closed = true;
            clearCache();
            selectedKeys = EMPTY;
        };
        table.addListener(SWT.SetData, setData);
        table.addListener(SWT.Selection, selected);
        table.addListener(SWT.DefaultSelection, selected);
        table.addListener(SWT.Dispose, onDispose);
    }

    /** Bind one original virtual Table; the caller creates its columns first. */
    public static M3VirtualTableBinding bind(Table table, Rows rows) {
        if (table == null || rows == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        if ((table.getStyle() & SWT.VIRTUAL) == 0) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
        verifyColumns(table, rows);
        int size = checkedSize(rows);
        M3VirtualTableBinding result = new M3VirtualTableBinding(table, rows);
        try {
            table.setItemCount(size);
        } catch (RuntimeException failed) {
            result.close();
            throw failed;
        }
        return result;
    }

    private static void verifyColumns(Table table, Rows rows) {
        int columns = Math.max(1, table.getColumnCount());
        if (columns > MAX_COLUMNS || rows.columns() != columns) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
    }

    private static int checkedSize(Rows rows) {
        int count = rows.size();
        if (count < 0) {
            throw new IllegalArgumentException("Negative logical row count");
        }
        return count;
    }

    private void onSetData(Event event) {
        if (closed || fetching || !(event.item instanceof TableItem)) {
            throw new IllegalStateException("Invalid or reentrant SWT.SetData");
        }
        int index = event.index;
        int count = table.getItemCount();
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Stale virtual table item");
        }
        Page page = pageAt(index);
        int relative = index - page.offset();
        TableItem item = (TableItem) event.item;
        item.setData(ROW_KEY, page.keyAt(relative));
        item.setText(page.cellTexts(relative));
    }

    private Page pageAt(int index) {
        int offset = (index / PAGE_ROWS) * PAGE_ROWS;
        long now = ++clock;
        if (now <= 0) {
            // Extremely rare clock wrap: preserve deterministic recency order.
            Arrays.fill(ages, 0);
            now = clock = 1;
        }
        for (int i = 0; i < HOT_PAGES; i++) {
            if (pages[i] != null && pages[i].offset() == offset) {
                ages[i] = now;
                return pages[i];
            }
        }
        int count = table.getItemCount();
        int length = Math.min(PAGE_ROWS, count - offset);
        Page page;
        fetching = true;
        try {
            page = rows.fetch(offset, length);
        } finally {
            fetching = false;
        }
        if (page == null || page.offset() != offset || page.size() != length
                || page.columns() != Math.max(1, table.getColumnCount())) {
            throw new IllegalArgumentException("Provider returned an incorrect page");
        }
        int slot = 0;
        for (int i = 1; i < HOT_PAGES; i++) {
            if (pages[slot] != null && (pages[i] == null || ages[i] < ages[slot])) {
                slot = i;
            }
        }
        pages[slot] = page;
        ages[slot] = now;
        return page;
    }

    /** Return stable keys captured from SWT.Selection, not unstable view indices. */
    public String[] getSelectedKeys() {
        checkOpen();
        return selectedKeys.clone();
    }

    /** Update the selected stable keys without materializing the complete view. */
    public void setSelectedKeys(String... keys) {
        checkOpen();
        if (keys == null || keys.length > MAX_SELECTION_KEYS) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
        for (String key : keys) {
            if (key == null || key.isEmpty() || key.length() > MAX_KEY_UNITS) {
                SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            }
        }
        selectedKeys = keys.clone();
        restoreSelection();
    }

    private void captureSelection() {
        if (closed) {
            return;
        }
        int[] selected = table.getSelectionIndices();
        if (selected.length > MAX_SELECTION_KEYS) {
            throw new IllegalStateException("Selection exceeds bounded key budget");
        }
        String[] keys = new String[selected.length];
        for (int i = 0; i < selected.length; i++) {
            TableItem item = table.getItem(selected[i]);
            Object key = item.getData(ROW_KEY);
            if (!(key instanceof String)) {
                Page page = pageAt(selected[i]);
                key = page.keyAt(selected[i] - page.offset());
            }
            keys[i] = (String) key;
        }
        selectedKeys = keys;
    }

    private void restoreSelection() {
        int count = table.getItemCount();
        int[] indices = new int[selectedKeys.length];
        int kept = 0;
        for (String key : selectedKeys) {
            int index = rows.indexOfKey(key);
            if (index < -1 || index >= count) {
                throw new IllegalArgumentException("Invalid key-to-index mapping");
            }
            if (index >= 0) {
                indices[kept++] = index;
            }
        }
        table.deselectAll();
        for (int i = 0; i < kept; i++) {
            table.select(indices[i]);
        }
    }

    /**
     * Replace the source after sorting/filtering or a snapshot refresh.
     * Re-resolves the selected stable keys using the new provider; filters
     * may temporarily hide a key without forgetting it.
     */
    public void replaceRows(Rows next) {
        checkOpen();
        if (next == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        if (fetching) {
            throw new IllegalStateException("Cannot mutate virtual rows during SetData");
        }
        verifyColumns(table, next);
        int count = checkedSize(next);
        rows = next;
        clearCache();
        table.setItemCount(count);
        table.clearAll();
        restoreSelection();
    }

    /** Invalidate a damaged inclusive logical range, without clearing other pages. */
    public void invalidate(int first, int last) {
        checkOpen();
        if (fetching) {
            throw new IllegalStateException("Cannot invalidate during SetData");
        }
        int count = table.getItemCount();
        if (first < 0 || last < first || last >= count) {
            SWT.error(SWT.ERROR_INVALID_RANGE);
        }
        for (int i = 0; i < HOT_PAGES; i++) {
            Page page = pages[i];
            if (page != null && page.offset() <= last
                    && (long) page.offset() + page.size() > first) {
                pages[i] = null;
                ages[i] = 0;
            }
        }
        table.clear(first, last);
    }

    /** Number of resident page objects, always in [0,4]. */
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
            throw new IllegalStateException("Virtual Table binding is closed");
        }
        // Also enforces SWT's UI-thread and disposed-widget contracts.
        table.getItemCount();
    }

    /** Stops observing the widget. The original Table and its items remain owned by SWT. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        clearCache();
        selectedKeys = EMPTY;
        if (!table.isDisposed()) {
            table.removeListener(SWT.SetData, setData);
            table.removeListener(SWT.Selection, selected);
            table.removeListener(SWT.DefaultSelection, selected);
            table.removeListener(SWT.Dispose, onDispose);
        }
    }
}
