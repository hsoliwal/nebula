/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.grid.internal.mobile;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;
import org.eclipse.swt.internal.mobile.MobileWidgetRuntime;
import org.eclipse.swt.internal.mobile.MobileWidgetRuntime.*;

/**
 * Mobile native-row receiver for Nebula Grid's bounded projection. Supplies
 * clickable platform-native row controls through SWT's internal mobile SPI.
 * Not an alternative public Grid class and not yet wired to original Grid.
 */
public final class MobileGridViewBinding implements AutoCloseable, MobileGridViewport.RowSink {
	private final MobileWidgetRuntime.Node surface;
	private final MobileGridViewport projection;
	private final int rowHeight;
	private final LongConsumer onSelection;
	private MobileWidgetRuntime.Node[] nativeRows = new MobileWidgetRuntime.Node[0];
	private long[] logicalRows = new long[0];
	private long offsetPixels;
	private int width;
	private int height;
	private boolean disposed;

	public MobileGridViewBinding(MobileWidgetRuntime.Node parent, long totalRows,
			int columns, int rowHeight, int overscan, MobileGridViewport.CellSource data,
			LongConsumer onSelection) {
		if (rowHeight <= 0) throw new IllegalArgumentException("positive row height required");
		this.rowHeight = rowHeight;
		this.onSelection = Objects.requireNonNull(onSelection);
		surface = Objects.requireNonNull(parent).createChild(Kind.COMPOSITE);
		projection = new MobileGridViewport(totalRows, columns, rowHeight, overscan, data, this);
	}
	public MobileWidgetRuntime.Node nativeControl() { requireLive(); return surface; }
	public MobileGridViewport projection() { requireLive(); return projection; }
	public MobileGridViewport.Window show(long scrollY, int displayWidth, int displayHeight) {
		requireLive();
		if (displayWidth < 0 || displayHeight < 0 || scrollY < 0)
			throw new IllegalArgumentException("negative viewport extent");
		long total = projection.itemCount();
		long maximum = total == 0 ? 0 : total - 1;
		// Keep the coordinate origin representable even if a caller overscrolls.
		// Do not multiply until the row count is known to fit in long pixels.
		offsetPixels = maximum <= Long.MAX_VALUE / rowHeight
				? Math.min(scrollY, maximum * rowHeight) : scrollY;
		width = displayWidth;
		height = displayHeight;
		surface.setBounds(new Bounds(0, 0, width, height));
		MobileGridViewport.Window window = projection.show(offsetPixels, height);
		// Even when the same rows remain resident, a fractional-row scroll
		// changes their screen coordinates without changing model contents.
		for (int slot = 0; slot < logicalRows.length; slot++) {
			if (logicalRows[slot] >= 0 && nativeRows[slot] != null)
				position(nativeRows[slot], logicalRows[slot]);
		}
		return window;
	}
	@Override public void bind(int slot, long row, String[] cells, boolean selected) {
		requireLive();
		ensureSlot(slot + 1);
		MobileWidgetRuntime.Node view = nativeRows[slot];
		if (view == null) {
			view = surface.createChild(Kind.BUTTON);
			nativeRows[slot] = view;
			final int ownedSlot = slot;
			view.on(EventType.SELECT, event -> {
				long selectedRow = logicalRows[ownedSlot];
				if (selectedRow < 0 || disposed) return;
				projection.select(selectedRow);
				try {
					onSelection.accept(selectedRow);
				} finally {
					// A selection listener may dispose or replace the Grid.
					// Never repaint native rows after their owner is closed.
					if (!disposed && !surface.isDisposed())
						projection.show(offsetPixels, height);
				}
			});
		}
		logicalRows[slot] = row;
		StringBuilder title = new StringBuilder(selected ? "[*] " : "[ ] ");
		for (int i = 0; i < cells.length; i++) {
			if (i > 0) title.append(" | ");
			title.append(cells[i]);
		}
		view.setText(title.toString());
		position(view, row);
		view.setAccessibilityLabel("Grid row " + row);
		view.setVisible(true);
	}
	private void position(MobileWidgetRuntime.Node view, long row) {
		long delta = row - offsetPixels / rowHeight;
		long position = delta * rowHeight - (offsetPixels % rowHeight);
		if (position < Integer.MIN_VALUE || position > Integer.MAX_VALUE)
			throw new IllegalStateException("projected row coordinate overflow");
		view.setBounds(new Bounds(0, (int)position, width, rowHeight));
	}
	@Override public void release(int slot, long oldRow) {
		if (slot >= logicalRows.length) return;
		logicalRows[slot] = -1;
		MobileWidgetRuntime.Node widget = nativeRows[slot];
		if (widget != null && !widget.isDisposed()) widget.setVisible(false);
	}
	private void ensureSlot(int count) {
		if (count <= nativeRows.length) return;
		int old = nativeRows.length;
		nativeRows = Arrays.copyOf(nativeRows, count);
		logicalRows = Arrays.copyOf(logicalRows, count);
		Arrays.fill(logicalRows, old, count, -1);
	}
	private void requireLive() {
		if (disposed || surface.isDisposed()) throw new IllegalStateException("disposed mobile Grid presentation");
	}
	@Override public void close() {
		if (disposed) return;
		disposed = true;
		projection.close();
		surface.close();
		Arrays.fill(nativeRows, null);
		Arrays.fill(logicalRows, -1);
	}
}
