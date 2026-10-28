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

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.M3HiddenGate;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * An image view: shows an image at a scale, panned by dragging and zoomed around the pointer with Ctrl+wheel (the SWT
 * counterpart of SwingX <code>JXImageView</code>, re-derived from its documented behaviour). The image is the
 * caller's (not disposed by the view).
 * <p>
 * The model is a scale and an integer screen offset of the image (client point p shows image point (p + offset) /
 * scale), so a pan by n pixels moves the drawn image by exactly n pixels.
 * A paint draws only the part of the image under the damaged rectangle with one scaled drawImage (native work
 * proportional to the window, never to the image). Panning moves the window's pixels (Control.scroll) and paints only
 * the exposed strips; a resize paints only the newly exposed strips (the image is anchored at the top-left); zooming
 * repaints the client area once. While hidden, changes only update the scale and origin and the view paints once when
 * shown.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Events: <code>Selection</code> after the scale or origin changed. Startup switch:
 * <code>-Dswt.widget.mode.M3ImageView=original</code> repaints the whole view on every pan step and draws the whole
 * image on every paint. RWT: Control.scroll and MouseMove are not available on a stock RWT server.
 * </p>
 */
public class M3ImageView extends Canvas {

	static final boolean VIEWPORT = WidgetMode.of("M3ImageView", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private final M3HiddenGate gate;
	private Image image;
	private int imageWidth, imageHeight;
	private double scale = 1, minScale = 1.0 / 64, maxScale = 64;
	/* the screen offset of the image: client point p shows image point (p + origin) / scale; integer, so a pan by n
	 * pixels moves the drawn image by exactly n pixels */
	private int originX, originY;
	private int dragX = Integer.MIN_VALUE, dragY;

	/**
	 * Creates an empty view at scale 1.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3ImageView(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND | (VIEWPORT ? SWT.NO_REDRAW_RESIZE : 0));
		gate = new M3HiddenGate(this, null);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.MouseDown, e -> {
			if (e.button == 1) {
				dragX = e.x;
				dragY = e.y;
			}
		});
		addListener(SWT.MouseMove, e -> {
			if (dragX != Integer.MIN_VALUE && (e.stateMask & SWT.BUTTON1) != 0) {
				panBy(dragX - e.x, dragY - e.y);
				dragX = e.x;
				dragY = e.y;
			}
		});
		addListener(SWT.MouseUp, e -> dragX = Integer.MIN_VALUE);
		addListener(SWT.MouseVerticalWheel, e -> {
			if ((e.stateMask & SWT.MOD1) != 0) {
				zoomAt(e.count > 0 ? 1.25 : 0.8, e.x, e.y);
				e.doit = false;
			}
		});
	}

	/**
	 * Shows an image (not disposed by the view) at the current scale from its top-left corner.
	 *
	 * @param image the image, or null
	 */
	public void setImage(Image image) {
		checkWidget();
		this.image = image;
		Rectangle bounds = image == null ? new Rectangle(0, 0, 0, 0) : image.getBounds();
		imageWidth = bounds.width;
		imageHeight = bounds.height;
		originX = originY = 0;
		fullRepaint();
	}

	/** @return the image, or null */
	public Image getImage() {
		checkWidget();
		return image;
	}

	/**
	 * Sets the scale, keeping the image point at the client's centre where it is.
	 *
	 * @param scale screen pixels per image pixel, clamped to the scale limits
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if not a positive number
	 */
	public void setScale(double scale) {
		checkWidget();
		if (!(scale > 0) || Double.isInfinite(scale)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		Rectangle client = getClientArea();
		zoomAt(scale / this.scale, client.width / 2, client.height / 2);
	}

	/** @return screen pixels per image pixel */
	public double getScale() {
		checkWidget();
		return scale;
	}

	/**
	 * Sets the scale limits.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if not 0 &lt; min &lt;= max
	 */
	public void setScaleLimits(double min, double max) {
		checkWidget();
		if (!(min > 0) || !(max >= min) || Double.isInfinite(max)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		minScale = min;
		maxScale = max;
		setScale(Math.max(min, Math.min(max, scale)));
	}

	/**
	 * Multiplies the scale by a factor, keeping the image point under a client point fixed.
	 *
	 * @param factor the zoom factor, greater than 0
	 * @param x client x of the fixed point
	 * @param y client y of the fixed point
	 */
	public void zoomAt(double factor, int x, int y) {
		checkWidget();
		if (!(factor > 0)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		double next = Math.max(minScale, Math.min(maxScale, scale * factor));
		if (next == scale) {
			return;
		}
		double imageX = (originX + x) / scale, imageY = (originY + y) / scale;
		scale = next;
		originX = (int) Math.round(imageX * scale - x);
		originY = (int) Math.round(imageY * scale - y);
		fullRepaint();
		changed();
	}

	/** Scales the image to fit the client area and shows it from its top-left corner. */
	public void fitToWindow() {
		checkWidget();
		if (imageWidth == 0 || imageHeight == 0) {
			return;
		}
		Rectangle client = getClientArea();
		scale = Math.max(minScale, Math.min(maxScale, Math.min(client.width / (double) imageWidth, client.height / (double) imageHeight)));
		originX = originY = 0;
		fullRepaint();
		changed();
	}

	/**
	 * Sets the image point shown at the client's top-left corner.
	 *
	 * @param x image x (may be negative or beyond the image: the area outside the image is background)
	 * @param y image y
	 */
	public void setOrigin(double x, double y) {
		checkWidget();
		int nextX = (int) Math.round(x * scale), nextY = (int) Math.round(y * scale);
		int dx = originX - nextX, dy = originY - nextY;
		originX = nextX;
		originY = nextY;
		moveContent(dx, dy);
		changed();
	}

	/** @return the image point at the client's top-left corner */
	public double[] getOrigin() {
		checkWidget();
		return new double[] { originX / scale, originY / scale };
	}

	/**
	 * Pans by a number of screen pixels (positive: the content moves left and up).
	 */
	public void panBy(int dx, int dy) {
		checkWidget();
		if (dx == 0 && dy == 0) {
			return;
		}
		originX += dx;
		originY += dy;
		moveContent(-dx, -dy);
		changed();
	}

	/** @return the client point of an image point */
	public Point toClient(double imageX, double imageY) {
		checkWidget();
		return new Point((int) Math.floor(imageX * scale) - originX, (int) Math.floor(imageY * scale) - originY);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		int width = (int) Math.ceil(imageWidth * scale), height = (int) Math.ceil(imageHeight * scale);
		Rectangle trim = computeTrim(0, 0, wHint == SWT.DEFAULT ? Math.max(1, width) : wHint, hHint == SWT.DEFAULT ? Math.max(1, height) : hHint);
		return new Point(trim.width, trim.height);
	}

	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	private void fullRepaint() {
		if (showing()) {
			M3Counters.fullRepaints++;
			redraw();
		}
	}

	/* moves the window's pixels by (dx, dy) and paints only the exposed strips (viewport), or repaints everything */
	private void moveContent(int dx, int dy) {
		if (!showing()) {
			return;
		}
		if (!VIEWPORT) {
			M3Counters.fullRepaints++;
			redraw();
			return;
		}
		Rectangle client = getClientArea();
		if (Math.abs(dx) >= client.width || Math.abs(dy) >= client.height) {
			redraw();
			return;
		}
		scroll(client.x + dx, client.y + dy, client.x, client.y, client.width, client.height, false);
	}

	private void changed() {
		notifyListeners(SWT.Selection, new Event());
	}

	private void paint(Event event) {
		GC gc = event.gc;
		gc.setBackground(getBackground());
		gc.fillRectangle(event.x, event.y, event.width, event.height);
		if (image == null || image.isDisposed()) {
			return;
		}
		Rectangle client = getClientArea();
		int ex = VIEWPORT ? event.x : client.x, ey = VIEWPORT ? event.y : client.y;
		int ew = VIEWPORT ? event.width : client.width, eh = VIEWPORT ? event.height : client.height;
		// the image pixels whose cells touch the damaged rectangle, clipped to the image
		int sx0 = Math.max(0, (int) Math.floor((ex + originX) / scale)), sy0 = Math.max(0, (int) Math.floor((ey + originY) / scale));
		int sx1 = Math.min(imageWidth, (int) Math.ceil((ex + ew + originX) / scale));
		int sy1 = Math.min(imageHeight, (int) Math.ceil((ey + eh + originY) / scale));
		if (sx0 >= sx1 || sy0 >= sy1) {
			return;
		}
		Point topLeft = toClient(sx0, sy0), bottomRight = toClient(sx1, sy1);
		gc.setInterpolation(SWT.NONE);
		gc.drawImage(image, sx0, sy0, sx1 - sx0, sy1 - sy0, topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
	}
}
