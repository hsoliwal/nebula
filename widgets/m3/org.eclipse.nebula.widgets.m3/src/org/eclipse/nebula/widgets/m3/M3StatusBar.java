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
package org.eclipse.nebula.widgets.m3;

import java.util.Arrays;

import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * A status bar: a row of slots separated by drawn lines, ending with an optional resize grip (the SWT counterpart of
 * SwingX <code>JXStatusBar</code>, re-derived from its documented behaviour). A slot shows drawn text or hosts a child
 * control (a progress bar, a button); its width is fixed, its preferred width, or a weighted share of the rest.
 * <p>
 * Slots are int lanes (mode, value, measured text width, x, width) plus a text lane and a control lane; there is no
 * object per slot. Positions are prefix sums; weighted shares are split by cumulative rounding so the slots tile the
 * bar without gaps. A text change repaints its slot only; when a preferred-width slot changes width the bar repaints
 * from that slot to its end and moves only the controls whose slot moved. Text of fixed and weighted slots is not
 * measured at all.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Startup switch: <code>-Dswt.widget.mode.M3StatusBar=original</code> lays out every
 * slot and repaints the whole bar on every change, as a plain implementation would.
 * </p>
 */
public class M3StatusBar extends Canvas {

	/** Slot width mode: the value is the width in pixels. */
	public static final int FIXED = 0;
	/** Slot width mode: the value is a weight; weighted slots share what fixed and preferred slots leave. */
	public static final int FILL = 1;
	/** Slot width mode: the width of the text or the control's preferred width, at least the value. */
	public static final int PREFERRED = 2;

	static final boolean VIEWPORT = WidgetMode.of("M3StatusBar", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3StatusBar"); //$NON-NLS-1$

	private static final int PAD = 4, SEPARATOR = 5, GRIP = 14;

	private int count;
	private int[] mode = new int[4], value = new int[4], textWidth = new int[4], xs = new int[4], widths = new int[4];
	private String[] texts = new String[4];
	private Control[] controls = new Control[4];
	private boolean grip = true;
	private int fontHeight = -1;
	/* left edge of the area the last layout changed, or Integer.MAX_VALUE when nothing moved */
	private int changedFrom;
	private int dragX = -1, dragY, dragWidth, dragHeight;

	/**
	 * Creates an empty status bar with a resize grip.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3StatusBar(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.Resize, e -> {
			layoutSlots();
			redraw();
		});
		addListener(SWT.MouseDown, this::mouse);
		addListener(SWT.MouseMove, this::mouse);
		addListener(SWT.MouseUp, this::mouse);
	}

	/**
	 * Appends a slot.
	 *
	 * @param mode {@link #FIXED}, {@link #FILL} or {@link #PREFERRED}
	 * @param value pixels for FIXED, a weight for FILL, a minimum width for PREFERRED
	 * @return the index of the new slot
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if the mode is unknown or the value negative
	 */
	public int addSlot(int mode, int value) {
		checkWidget();
		if (mode < FIXED || mode > PREFERRED || value < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		if (count == this.mode.length) {
			int n = count * 2;
			this.mode = Arrays.copyOf(this.mode, n);
			this.value = Arrays.copyOf(this.value, n);
			textWidth = Arrays.copyOf(textWidth, n);
			xs = Arrays.copyOf(xs, n);
			widths = Arrays.copyOf(widths, n);
			texts = Arrays.copyOf(texts, n);
			controls = Arrays.copyOf(controls, n);
		}
		int slot = count++;
		this.mode[slot] = mode;
		this.value[slot] = value;
		texts[slot] = ""; //$NON-NLS-1$
		xs[slot] = widths[slot] = textWidth[slot] = 0;
		controls[slot] = null;
		relayout(xs[Math.max(0, slot - 1)]);
		return slot;
	}

	/**
	 * Removes a slot; the slot's control, if any, is left as it is (not disposed).
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the slot does not exist
	 */
	public void removeSlot(int slot) {
		checkSlot(slot);
		int from = xs[slot], tail = count - slot - 1;
		System.arraycopy(mode, slot + 1, mode, slot, tail);
		System.arraycopy(value, slot + 1, value, slot, tail);
		System.arraycopy(textWidth, slot + 1, textWidth, slot, tail);
		System.arraycopy(xs, slot + 1, xs, slot, tail);
		System.arraycopy(widths, slot + 1, widths, slot, tail);
		System.arraycopy(texts, slot + 1, texts, slot, tail);
		System.arraycopy(controls, slot + 1, controls, slot, tail);
		count--;
		texts[count] = null;
		controls[count] = null;
		relayout(from);
	}

	/** @return the number of slots */
	public int getSlotCount() {
		checkWidget();
		return count;
	}

	/** @return the width mode of a slot */
	public int getSlotMode(int slot) {
		checkSlot(slot);
		return mode[slot];
	}

	/**
	 * Sets the text of a slot and repaints the slot, or, when a preferred-width slot changes width, the bar from that
	 * slot on.
	 *
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_RANGE - if the slot does not exist</li>
	 *                <li>ERROR_NULL_ARGUMENT - if the text is null</li>
	 *                </ul>
	 */
	public void setText(int slot, String text) {
		checkSlot(slot);
		if (text == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (texts[slot].equals(text)) {
			return;
		}
		texts[slot] = text;
		if (mode[slot] == PREFERRED) {
			int width = measure(text);
			if (width != textWidth[slot]) {
				textWidth[slot] = width;
				if (controls[slot] == null) {
					relayout(xs[slot]);
					return;
				}
			}
		}
		if (VIEWPORT) {
			Rectangle bounds = getSlotBounds(slot);
			redraw(bounds.x, bounds.y, bounds.width, bounds.height, false);
		} else {
			layoutSlots();
			redraw();
		}
	}

	/** @return the text of a slot, never null */
	public String getText(int slot) {
		checkSlot(slot);
		return texts[slot];
	}

	/**
	 * Places a child control in a slot (its text is then not drawn), or clears the slot's control.
	 *
	 * @param control a child of this bar, or null
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_RANGE - if the slot does not exist</li>
	 *                <li>ERROR_INVALID_PARENT - if the control is not a child of this bar</li>
	 *                </ul>
	 */
	public void setControl(int slot, Control control) {
		checkSlot(slot);
		if (control != null && control.getParent() != this) {
			SWT.error(SWT.ERROR_INVALID_PARENT);
		}
		controls[slot] = control;
		relayout(xs[slot]);
		Rectangle bounds = getSlotBounds(slot);
		if (control != null) {
			control.setBounds(bounds);
		}
		redraw(bounds.x, bounds.y, bounds.width, bounds.height, false);
	}

	/** @return the control of a slot, or null */
	public Control getControl(int slot) {
		checkSlot(slot);
		return controls[slot];
	}

	/** @return the bounds of a slot in client coordinates */
	public Rectangle getSlotBounds(int slot) {
		checkSlot(slot);
		Rectangle client = getClientArea();
		return new Rectangle(xs[slot], client.y, widths[slot], client.height);
	}

	/** Shows or hides the resize grip at the right end; dragging the grip resizes the shell. */
	public void setGrip(boolean grip) {
		checkWidget();
		if (this.grip != grip) {
			this.grip = grip;
			layoutSlots();
			redraw();
		}
	}

	/** @return true when the resize grip is shown */
	public boolean getGrip() {
		checkWidget();
		return grip;
	}

	/** @return the grip square in client coordinates (empty when there is no grip) */
	public Rectangle getGripBounds() {
		checkWidget();
		Rectangle client = getClientArea();
		return grip ? new Rectangle(client.x + client.width - GRIP, client.y + client.height - GRIP, GRIP, GRIP)
				: new Rectangle(0, 0, 0, 0);
	}

	@Override
	public void setFont(Font font) {
		super.setFont(font);
		fontHeight = -1;
		for (int i = 0; i < count; i++) {
			textWidth[i] = mode[i] == PREFERRED ? measure(texts[i]) : 0;
		}
		layoutSlots();
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		int width = 2 * PAD + (grip ? GRIP : 0) + Math.max(0, count - 1) * SEPARATOR;
		int height = fontHeight() + 6;
		for (int i = 0; i < count; i++) {
			width += mode[i] == FILL ? 0 : minimum(i);
			if (controls[i] != null) {
				height = Math.max(height, controls[i].computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			}
		}
		Rectangle trim = computeTrim(0, 0, wHint == SWT.DEFAULT ? width : wHint, hHint == SWT.DEFAULT ? height : hHint);
		return new Point(trim.width, trim.height);
	}

	private void checkSlot(int slot) {
		checkWidget();
		if (slot < 0 || slot >= count) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
	}

	private int measure(String text) {
		if (text.isEmpty()) {
			return 0;
		}
		GC gc = new GC(this);
		int width = gc.textExtent(text, SWT.DRAW_TRANSPARENT).x;
		gc.dispose();
		return width;
	}

	private int fontHeight() {
		if (fontHeight < 0) {
			GC gc = new GC(this);
			fontHeight = gc.getFontMetrics().getHeight();
			gc.dispose();
		}
		return fontHeight;
	}

	private int minimum(int slot) {
		if (mode[slot] == FIXED) {
			return value[slot];
		}
		int preferred = controls[slot] != null ? controls[slot].computeSize(SWT.DEFAULT, SWT.DEFAULT).x : textWidth[slot] + 2 * PAD;
		return mode[slot] == PREFERRED ? Math.max(value[slot], preferred) : 0;
	}

	/* lays out and repaints from the first slot that moved (viewport) or everything (original) */
	private void relayout(int from) {
		layoutSlots();
		if (!VIEWPORT) {
			redraw();
			return;
		}
		int left = Math.min(from, changedFrom);
		if (left != Integer.MAX_VALUE) {
			Rectangle client = getClientArea();
			left = Math.max(client.x, left);
			redraw(left, client.y, client.x + client.width - left, client.height, false);
		}
	}

	/*
	 * Prefix-sum layout: fixed and preferred widths first, then the rest split among weighted slots by cumulative
	 * rounding (slot i ends at rest * (w0 + .. + wi) / W), so the slots tile the bar exactly. Records the left edge of
	 * the first slot whose bounds changed and moves only the controls whose slot changed.
	 */
	private void layoutSlots() {
		Rectangle client = getClientArea();
		int available = client.width - 2 * PAD - (grip ? GRIP : 0) - Math.max(0, count - 1) * SEPARATOR;
		long weights = 0;
		int used = 0;
		for (int i = 0; i < count; i++) {
			if (mode[i] == FILL) {
				weights += value[i];
			} else {
				used += minimum(i);
			}
		}
		int rest = Math.max(0, available - used), x = client.x + PAD, given = 0;
		long seen = 0;
		changedFrom = Integer.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			int width;
			if (mode[i] == FILL) {
				seen += value[i];
				int end = weights == 0 ? 0 : (int) (rest * seen / weights);
				width = end - given;
				given = end;
			} else {
				width = minimum(i);
			}
			if (x != xs[i] || width != widths[i]) {
				changedFrom = Math.min(changedFrom, Math.min(x, xs[i]));
				xs[i] = x;
				widths[i] = width;
				if (controls[i] != null && !controls[i].isDisposed()) {
					controls[i].setBounds(x, client.y, width, client.height);
				}
			}
			x += width + SEPARATOR;
		}
		if (LOG.isTraceEnabled()) {
			final int from = changedFrom;
			LOG.trace(() -> "layout " + count + " slots, changed from x=" + from); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void mouse(Event event) {
		switch (event.type) {
		case SWT.MouseDown:
			if (event.button == 1 && getGripBounds().contains(event.x, event.y)) {
				Point at = toDisplay(event.x, event.y);
				Point size = getShell().getSize();
				dragX = at.x;
				dragY = at.y;
				dragWidth = size.x;
				dragHeight = size.y;
			}
			break;
		case SWT.MouseMove:
			if (dragX >= 0 && (event.stateMask & SWT.BUTTON1) != 0) {
				Point at = toDisplay(event.x, event.y);
				Shell shell = getShell();
				shell.setSize(Math.max(64, dragWidth + at.x - dragX), Math.max(32, dragHeight + at.y - dragY));
			}
			break;
		case SWT.MouseUp:
			dragX = -1;
			break;
		default:
			break;
		}
	}

	private void paint(Event event) {
		GC gc = event.gc;
		Rectangle clip = new Rectangle(event.x, event.y, event.width, event.height);
		Rectangle client = getClientArea();
		gc.setBackground(getBackground());
		gc.fillRectangle(clip);
		gc.setForeground(getForeground());
		if (fontHeight < 0) {
			fontHeight = gc.getFontMetrics().getHeight();
		}
		int textY = client.y + Math.max(0, (client.height - fontHeight()) / 2);
		for (int i = 0; i < count; i++) {
			if (controls[i] == null && !texts[i].isEmpty() && xs[i] < clip.x + clip.width && xs[i] + widths[i] > clip.x) {
				gc.setClipping(new Rectangle(xs[i], client.y, widths[i], client.height).intersection(clip));
				gc.drawText(texts[i], xs[i] + PAD, textY, true);
				gc.setClipping(clip);
			}
		}
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		for (int i = 0; i + 1 < count; i++) {
			int line = xs[i] + widths[i] + SEPARATOR / 2;
			if (line >= clip.x && line < clip.x + clip.width) {
				gc.drawLine(line, client.y + 3, line, client.y + client.height - 4);
			}
		}
		Rectangle box = getGripBounds();
		if (grip && box.intersects(clip)) {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
			for (int row = 0; row < 3; row++) {
				for (int column = 2 - row; column < 3; column++) {
					gc.fillRectangle(box.x + 2 + column * 4, box.y + 2 + row * 4, 2, 2);
				}
			}
		}
	}
}
