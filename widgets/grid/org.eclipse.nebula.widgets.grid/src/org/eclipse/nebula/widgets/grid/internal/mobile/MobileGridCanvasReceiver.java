/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.grid.internal.mobile;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

/**
 * Bounded Grid row presenter on the original SWT Canvas API.
 *
 * The same Canvas/GC/Mouse event contracts are implemented by the initial
 * Android and iOS SWT platform fragments. No Android/iOS classes or alternate
 * SWT toolkit are introduced here; the original Grid public API is unchanged.
 * This is an internal row presenter, not a replacement for Grid columns,
 * editors, group rows, accessibility or full Grid behavior.
 *
 * All methods (except isClosed()) are confined to the SWT Display thread.
 */
public final class MobileGridCanvasReceiver implements AutoCloseable {
    private final Canvas canvas;
    private final MobileGridViewport viewport;
    private final LongConsumer selection;
    private final int columns;
    private final int rowHeight;
    private String[][] cells = new String[0][];
    private long[] rows = new long[0];
    private boolean[] selected = new boolean[0];
    private long scrollY;
    private int lastPointerY;
    private boolean pointerDown;
    private boolean dragged;
    private int lastPaintedRows;
    private volatile boolean closed;

    /** Binds a Grid viewport of itemCount logical rows to the canvas; selection receives the selected row. */
    public MobileGridCanvasReceiver(Canvas canvas, long itemCount, int columns,
            int rowHeight, int overscan, MobileGridViewport.CellSource data,
            LongConsumer selection) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.selection = Objects.requireNonNull(selection, "selection");
        if (canvas.isDisposed()) throw new IllegalArgumentException("disposed Canvas");
        if (columns <= 0 || rowHeight <= 0) throw new IllegalArgumentException("invalid geometry");
        this.columns = columns;
        this.rowHeight = rowHeight;
        viewport = new MobileGridViewport(itemCount, columns, rowHeight, overscan,
                data, new MobileGridViewport.RowSink() {
                    @Override public void reserve(int slots) {
                        // The viewport owns the exact modulo slot count. Preallocate
                        // all three receiver lanes once, never geometrically or per row.
                        ensureSlot(slots);
                    }
                    @Override public void bind(int slot, long row, String[] text, boolean active) {
                        ensureSlot(slot + 1);
                        rows[slot] = row;
                        cells[slot] = text;
                        selected[slot] = active;
                    }
                    @Override public void release(int slot, long oldRow) {
                        if (slot < rows.length) {
                            rows[slot] = -1;
                            cells[slot] = null;
                            selected[slot] = false;
                        }
                    }
                });
        canvas.addListener(SWT.Paint, event -> paint(event.gc));
        canvas.addListener(SWT.Resize, event -> { if (!closed) canvas.redraw(); });
        canvas.addListener(SWT.MouseWheel, event -> {
            if (closed) return;
            long delta;
            try {
                delta = Math.negateExact(Math.multiplyExact(3L * rowHeight, (long) event.count));
            } catch (ArithmeticException overflow) {
                delta = event.count < 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
            }
            scrollBy(delta);
        });
        canvas.addListener(SWT.MouseDown, event -> {
            if (closed || event.button != 1) return;
            pointerDown = true;
            dragged = false;
            lastPointerY = event.y;
        });
        canvas.addListener(SWT.MouseMove, event -> {
            if (closed || !pointerDown) return;
            // Subtract in long before narrowing: extreme native pointer coordinates
            // must not wrap and reverse the scroll direction.
            long delta = (long)event.y - lastPointerY;
            lastPointerY = event.y;
            if (delta != 0) {
                dragged = true;
                scrollBy(-delta);
            }
        });
        canvas.addListener(SWT.MouseUp, event -> {
            if (closed || event.button != 1 || !pointerDown) return;
            pointerDown = false;
            if (!dragged) selectAt(event.y);
        });
        // Android ACTION_CANCEL and UIKit touch cancellation map to MouseExit.
        // Never allow an abandoned drag to leak into a new pointer sequence.
        canvas.addListener(SWT.MouseExit, event -> pointerDown = false);
        canvas.addListener(SWT.Dispose, event -> close());
    }

    private void ensureSlot(int count) {
        if (count <= rows.length) return;
        int old = rows.length;
        rows = Arrays.copyOf(rows, count);
        Arrays.fill(rows, old, count, -1);
        cells = Arrays.copyOf(cells, count);
        selected = Arrays.copyOf(selected, count);
    }

    private void checkOpen() {
        if (closed || canvas.isDisposed()) throw new IllegalStateException("mobile Grid receiver disposed");
        canvas.getStyle(); // SWT Widget.checkWidget enforces owning Display thread
    }

    private long maxScroll() {
        long count = viewport.itemCount();
        if (count == 0) return 0;
        long last = count - 1;
        return last > Long.MAX_VALUE / rowHeight ? Long.MAX_VALUE : last * rowHeight;
    }

    /** Scrolls to a vertical pixel offset, clamped to the last row. */
    public void scrollTo(long pixels) {
        checkOpen();
        if (pixels < 0) throw new IllegalArgumentException("negative scroll");
        long next = Math.min(pixels, maxScroll());
        if (next == scrollY) return;
        long delta = next - scrollY;
        scrollY = next;
        Rectangle area = canvas.getClientArea();
        int distance = blitDistance(delta, area.height);
        // The retained frame moves with the scroll and only the band it uncovers
        // is painted. A jump of a viewport or more keeps nothing.
        if (distance != 0 && area.width > 0) canvas.scroll(0, -distance, 0, 0, area.width, area.height, false);
        else canvas.redraw();
    }
    /** The pixels a scroll of delta moves the retained frame by, or 0 when no part of it stays in view. */
    static int blitDistance(long delta, int viewportHeight) {
        return delta > -viewportHeight && delta < viewportHeight ? (int) delta : 0;
    }
    /** Redraws one row when it is in view; the rest of the frame is kept. */
    private void redrawRow(long row) {
        if (row < 0 || closed || canvas.isDisposed()) return;
        Rectangle area = canvas.getClientArea();
        long first = scrollY / rowHeight;
        if (row < first || row - first > (long) area.height / rowHeight + 1) return;
        int y = (int) ((row - first) * rowHeight - scrollY % rowHeight);
        canvas.redraw(0, y, area.width, rowHeight, false);
    }

    /** Scrolls by a pixel delta, saturating at both ends. */
    public void scrollBy(long delta) {
        checkOpen();
        long next = delta > 0 && scrollY > Long.MAX_VALUE - delta
                ? Long.MAX_VALUE
                : delta < 0 && scrollY < -1L - delta
                    ? 0 : scrollY + delta;
        scrollTo(Math.max(0, next));
    }

    /** The vertical scroll offset in pixels. */
    public long scrollY() { checkOpen(); return scrollY; }
    /** The last presented window of the viewport. */
    public MobileGridViewport.Window window() { checkOpen(); return viewport.window(); }
    /** Sets the number of logical rows and keeps the scroll offset in range. */
    public void setItemCount(long count) {
        checkOpen();
        viewport.setItemCount(count);
        scrollTo(Math.min(scrollY, maxScroll()));
        canvas.redraw();
    }
    /** Invalidates one retained row and redraws that row. */
    public void invalidate(long row) { checkOpen(); viewport.invalidate(row); redrawRow(row); }
    /** Invalidates every retained row and redraws. */
    public void invalidateAll() { checkOpen(); viewport.invalidateAll(); canvas.redraw(); }
    /** The selected row, or -1. */
    public long selectedRow() { checkOpen(); return viewport.selectedRow(); }
    /** Selects a row (-1 for none) and redraws the previous and the new selection. */
    public void select(long row) {
        checkOpen();
        long previous = viewport.selectedRow();
        viewport.select(row);
        redrawRow(previous);
        redrawRow(row);
    }

    /** Hit-test only the visible logical range; overscan rows cannot be selected. */
    public long rowAt(int y) {
        checkOpen();
        Rectangle area = canvas.getClientArea();
        if (y < 0 || y >= area.height || viewport.itemCount() == 0) return -1;
        long offset = (long)y + scrollY % rowHeight;
        long first = scrollY / rowHeight;
        long relative = offset / rowHeight;
        long row = first > Long.MAX_VALUE - relative ? Long.MAX_VALUE : first + relative;
        return row < viewport.itemCount() ? row : -1;
    }

    private void selectAt(int y) {
        long row = rowAt(y);
        if (row < 0) return;
        long previous = viewport.selectedRow();
        viewport.select(row);
        selection.accept(row);
        redrawRow(previous);
        redrawRow(row);
    }

    /** Paints the rows the clip of the GC touches; rows outside it keep their retained pixels. */
    void paint(GC gc) {
        if (closed || gc == null) return;
        Rectangle area = canvas.getClientArea();
        if (area.width <= 0 || area.height <= 0) return;
        MobileGridViewport.Window view = viewport.show(scrollY, area.height);
        int width = Math.max(1, area.width / columns);
        gc.setBackground(canvas.getBackground());
        gc.setForeground(canvas.getForeground());
        Rectangle clip = gc.getClipping();
        int painted = 0;
        for (long row = view.visibleStart(); row < view.visibleEnd(); row++) {
            int slot = (int)(row % Math.max(1, rows.length));
            if (slot >= rows.length || rows[slot] != row || cells[slot] == null) continue;
            long yValue = (row - view.visibleStart()) * rowHeight - (scrollY % rowHeight);
            if (yValue < -rowHeight || yValue >= area.height) continue;
            int y = (int)yValue;
            if (y + rowHeight <= clip.y || y >= clip.y + clip.height) continue;
            painted++;
            String[] values = cells[slot];
            if (selected[slot]) gc.drawRectangle(0, y, Math.max(0, area.width - 1), Math.max(0, rowHeight - 1));
            for (int col = 0; col < columns; col++) {
                int x = (int)Math.min(Integer.MAX_VALUE, (long)col * width);
                if (x >= area.width) break;
                gc.drawText(values[col], x + 3, y + 2, true);
                if (col != 0) gc.drawLine(x, y, x,
                        (int)Math.min(Integer.MAX_VALUE, (long)y + rowHeight));
            }
            int bottom = (int)Math.min(Integer.MAX_VALUE, (long)y + rowHeight - 1L);
            gc.drawLine(0, bottom, area.width, bottom);
        }
        lastPaintedRows = painted;
    }
    /** Rows the last paint drew (those its clip touched). */
    int lastPaintedRows() { return lastPaintedRows; }

    /** Whether close() has run. */
    public boolean isClosed() { return closed; }
    /** Releases the viewport and its retained cells; later calls do nothing. */
    @Override public void close() {
        if (closed) return;
        closed = true;
        pointerDown = false;
        viewport.close();
        Arrays.fill(cells, null);
        Arrays.fill(rows, -1);
    }
}
