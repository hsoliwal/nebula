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

import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A busy indicator: a ring of points of which a head and a fading trail turn while the label is busy, followed by an
 * optional text (the SWT counterpart of SwingX <code>JXBusyLabel</code>, re-derived from its documented behaviour).
 * <p>
 * The model is a few ints (frame, point count, trail length, delay, icon size) and two int lanes with the point
 * centres; the trail shades are computed once per colour change. A frame step repaints the icon square only and
 * allocates nothing; a text change repaints the text strip only.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Startup switch: <code>-Dswt.widget.mode.M3BusyLabel=original</code> repaints the whole
 * control on every frame, as a plain implementation would.
 * </p>
 */
public class M3BusyLabel extends Canvas {

	static final boolean VIEWPORT = WidgetMode.of("M3BusyLabel", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3BusyLabel"); //$NON-NLS-1$

	private static final int GAP = 4;

	private String text = ""; //$NON-NLS-1$
	private boolean busy;
	private int points = 8, trail = 4, delay = 100, frame, iconSize = 16;
	private int[] xs = new int[0], ys = new int[0];
	private int dot;
	/* shades[0] is the head, shades[trail] the idle point colour; null until the next paint after a colour change */
	private Color[] shades;
	private final Runnable tick = this::tick;

	/**
	 * Creates an idle busy label with eight points.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3BusyLabel(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		geometry();
		addListener(SWT.Paint, this::paint);
		addListener(SWT.Dispose, e -> {
			getDisplay().timerExec(-1, tick);
			disposeShades();
		});
	}

	/**
	 * Starts or stops the animation.
	 *
	 * @param busy true to turn the trail every {@link #getDelay()} milliseconds
	 */
	public void setBusy(boolean busy) {
		checkWidget();
		if (this.busy == busy) {
			return;
		}
		this.busy = busy;
		LOG.debug(() -> "busy " + busy); //$NON-NLS-1$
		getDisplay().timerExec(busy ? delay : -1, tick);
		redrawIcon();
	}

	/** @return true while the trail turns */
	public boolean isBusy() {
		checkWidget();
		return busy;
	}

	private void tick() {
		if (isDisposed() || !busy) {
			return;
		}
		setFrame((frame + 1) % points);
		getDisplay().timerExec(delay, tick);
	}

	/**
	 * Moves the head of the trail to a point and repaints the icon.
	 *
	 * @param frame the index of the head point
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the frame is not between 0 and the point count - 1
	 */
	public void setFrame(int frame) {
		checkWidget();
		if (frame < 0 || frame >= points) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
		if (this.frame != frame) {
			this.frame = frame;
			redrawIcon();
		}
	}

	/** @return the index of the head point */
	public int getFrame() {
		checkWidget();
		return frame;
	}

	/**
	 * Sets the number of points on the ring; the frame restarts at 0.
	 *
	 * @param points at least 3
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if fewer than 3
	 */
	public void setPointCount(int points) {
		checkWidget();
		if (points < 3) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.points = points;
		trail = Math.min(trail, points);
		frame = 0;
		geometry();
		disposeShades();
		redrawIcon();
	}

	/** @return the number of points */
	public int getPointCount() {
		checkWidget();
		return points;
	}

	/**
	 * Sets how many points, head included, are drawn brighter than the idle points.
	 *
	 * @param trail between 1 and the point count
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if out of range
	 */
	public void setTrailLength(int trail) {
		checkWidget();
		if (trail < 1 || trail > points) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.trail = trail;
		disposeShades();
		redrawIcon();
	}

	/** @return the trail length */
	public int getTrailLength() {
		checkWidget();
		return trail;
	}

	/**
	 * Sets the time between two frames; takes effect at the next frame.
	 *
	 * @param milliseconds at least 1
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if less than 1
	 */
	public void setDelay(int milliseconds) {
		checkWidget();
		if (milliseconds < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		delay = milliseconds;
	}

	/** @return the time between two frames in milliseconds */
	public int getDelay() {
		checkWidget();
		return delay;
	}

	/**
	 * Sets the side of the icon square.
	 *
	 * @param pixels at least 8
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if less than 8
	 */
	public void setIconSize(int pixels) {
		checkWidget();
		if (pixels < 8) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		iconSize = pixels;
		geometry();
		redraw();
	}

	/** @return the side of the icon square */
	public int getIconSize() {
		checkWidget();
		return iconSize;
	}

	/**
	 * Sets the text after the icon and repaints the text strip.
	 *
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the text is null
	 */
	public void setText(String text) {
		checkWidget();
		if (text == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (!this.text.equals(text)) {
			this.text = text;
			Rectangle strip = getTextBounds();
			redraw(strip.x, strip.y, strip.width, strip.height, false);
		}
	}

	/** @return the text, never null */
	public String getText() {
		checkWidget();
		return text;
	}

	/** @return the icon square in client coordinates */
	public Rectangle getIconBounds() {
		checkWidget();
		Rectangle client = getClientArea();
		return new Rectangle(client.x, client.y + Math.max(0, (client.height - iconSize) / 2), iconSize, iconSize);
	}

	/** @return the strip right of the icon, where the text is drawn, in client coordinates */
	public Rectangle getTextBounds() {
		checkWidget();
		Rectangle client = getClientArea();
		int x = client.x + iconSize + GAP;
		return new Rectangle(x, client.y, Math.max(0, client.x + client.width - x), client.height);
	}

	@Override
	public void setForeground(Color color) {
		super.setForeground(color);
		disposeShades();
		redraw();
	}

	@Override
	public void setBackground(Color color) {
		super.setBackground(color);
		disposeShades();
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		int width = iconSize, height = iconSize;
		if (!text.isEmpty()) {
			GC gc = new GC(this);
			Point extent = gc.textExtent(text, SWT.DRAW_TRANSPARENT);
			gc.dispose();
			width += GAP + extent.x;
			height = Math.max(height, extent.y);
		}
		if (wHint != SWT.DEFAULT) {
			width = wHint;
		}
		if (hHint != SWT.DEFAULT) {
			height = hHint;
		}
		Rectangle trim = computeTrim(0, 0, width, height);
		return new Point(trim.width, trim.height);
	}

	private void redrawIcon() {
		if (VIEWPORT) {
			Rectangle icon = getIconBounds();
			redraw(icon.x, icon.y, icon.width, icon.height, false);
		} else {
			redraw();
		}
	}

	/* point centres on a ring inside the icon square; computed when the point count or icon size changes */
	private void geometry() {
		dot = Math.max(2, iconSize / 5);
		double radius = (iconSize - dot) / 2.0, centre = iconSize / 2.0;
		xs = new int[points];
		ys = new int[points];
		for (int i = 0; i < points; i++) {
			double angle = 2 * Math.PI * i / points - Math.PI / 2;
			xs[i] = (int) Math.round(centre + radius * Math.cos(angle));
			ys[i] = (int) Math.round(centre + radius * Math.sin(angle));
		}
	}

	/* head = foreground; the trail fades linearly to the idle shade, 25% foreground over the background */
	private Color[] shades() {
		if (shades == null) {
			RGB fg = getForeground().getRGB(), bg = getBackground().getRGB();
			shades = new Color[trail + 1];
			for (int d = 0; d <= trail; d++) {
				double t = d == trail ? 0.25 : 1.0 - 0.75 * d / trail;
				shades[d] = new Color(getDisplay(), mix(fg.red, bg.red, t), mix(fg.green, bg.green, t), mix(fg.blue, bg.blue, t));
			}
		}
		return shades;
	}

	private static int mix(int a, int b, double t) {
		return (int) Math.round(a * t + b * (1 - t));
	}

	private void disposeShades() {
		if (shades != null) {
			for (Color color : shades) {
				color.dispose();
			}
			shades = null;
		}
	}

	private void paint(Event event) {
		GC gc = event.gc;
		gc.setBackground(getBackground());
		gc.fillRectangle(event.x, event.y, event.width, event.height);
		Rectangle icon = getIconBounds();
		if (icon.intersects(event.x, event.y, event.width, event.height)) {
			Color[] colors = shades();
			for (int i = 0; i < points; i++) {
				int distance = (frame - i + points) % points;
				gc.setBackground(busy && distance < trail ? colors[distance] : colors[trail]);
				gc.fillOval(icon.x + xs[i] - dot / 2, icon.y + ys[i] - dot / 2, dot, dot);
			}
		}
		Rectangle strip = getTextBounds();
		if (!text.isEmpty() && strip.intersects(event.x, event.y, event.width, event.height)) {
			int height = gc.getFontMetrics().getHeight();
			gc.setForeground(getForeground());
			gc.drawText(text, strip.x, strip.y + Math.max(0, (strip.height - height) / 2), true);
		}
	}
}
