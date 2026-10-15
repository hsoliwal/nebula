/*******************************************************************************
 * Copyright (c) 2020 Laurent CARON.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Laurent CARON (laurent.caron at gmail dot com) - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.carousel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

class ImageSelector extends Canvas {
	private static final int CIRCLE_DIAMETER = 20;
	private static final int CIRCLE_STEP = CIRCLE_DIAMETER + 5;
	private static final int ARROW_SIZE = 12;

	Color arrowColor, circleBackground, circleForeground, circleHoverColor;
	private final List<Rectangle> rects = new ArrayList<>();
	private int indexHover = -1;
	/** The previous paint's hit geometry, without one Rectangle per image. */
	private boolean coordinateViewport;
	private int paintedCircleCount;
	private int paintedCircleY;
	private Rectangle arrowLeftArea, arrowRightArea;
	private boolean hoverLeftArrow, hoverRightArrow;
	private final Carousel carousel;

	public ImageSelector(final Carousel parent, final int none) {
		super(parent, SWT.DOUBLE_BUFFERED);
		carousel = parent;

		addListener(SWT.Paint, e -> {
			final GC gc = e.gc;
			gc.setFont(getFont());
			gc.setAdvanced(true);
			gc.setTextAntialias(SWT.ON);
			gc.setAntialias(SWT.ON);
			final Color previousForeground = gc.getForeground();
			final Color previousBackground = gc.getBackground();

			drawContent(gc);

			gc.setBackground(previousBackground);
			gc.setForeground(previousForeground);
		});

		addListener(SWT.MouseExit, e -> {
			indexHover = -1;
			setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		});

		addListener(SWT.MouseMove, e -> {
			Point pt = getDisplay().getCursorLocation();
			pt = getDisplay().map(null, this, pt.x, pt.y);

			hoverLeftArrow = arrowLeftArea.contains(pt);
			hoverRightArrow = arrowRightArea.contains(pt);

			Cursor expectedCursor = hoverLeftArrow || hoverRightArrow ? getDisplay().getSystemCursor(SWT.CURSOR_HAND) : getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
			indexHover = -1;
			if (coordinateViewport) {
				indexHover = paintedCircleAt(pt);
				if (indexHover >= 0) {
					expectedCursor = getDisplay().getSystemCursor(SWT.CURSOR_HAND);
				}
			} else {
				for (int i = 0; i < rects.size(); i++) {
					final Rectangle rect = rects.get(i);
					if (rect.contains(pt)) {
						expectedCursor = getDisplay().getSystemCursor(SWT.CURSOR_HAND);
						indexHover = i;
						break;
					}
				}
			}

			if (!expectedCursor.equals(getCursor())) {
				setCursor(expectedCursor);
			}
			redraw();
		});

		addListener(SWT.MouseUp, e -> {
			final ImageContainer imageContainer = parent.imageContainer;
			if (hoverLeftArrow) {
				imageContainer.movePrevious();
			} else if (hoverRightArrow) {
				imageContainer.moveNext();
			} else if (indexHover != -1) {
				imageContainer.moveTo(indexHover);
			}
		});
	}

	private void drawContent(final GC gc) {
		final int x = drawCircles(gc);
		drawArrows(x, gc);
	}

	/**
	 * CPU hit-test of rectangles last painted by this very Canvas. A parent
	 * image-list mutation before the next Paint must not change those bounds.
	 */
	private int paintedCircleAt(final Point point) {
		if (point.x < 0 || point.y < paintedCircleY
				|| (long) point.y >= (long) paintedCircleY + CIRCLE_DIAMETER) return -1;
		int index = point.x / CIRCLE_STEP;
		if (index >= paintedCircleCount || point.x % CIRCLE_STEP >= CIRCLE_DIAMETER) return -1;
		return index;
	}

	/** Preserve the original path for subclass overrides and invalid resources. */
	private int drawCircles(final GC gc) {
		if (getClass() != ImageSelector.class || carousel.getClass() != Carousel.class
				|| circleBackground == null || circleForeground == null || circleHoverColor == null
				|| circleBackground.isDisposed() || circleForeground.isDisposed()
				|| circleHoverColor.isDisposed()) {
			coordinateViewport = false;
			return drawCirclesLegacy(gc);
		}
		final int count = carousel.getImages().size();
		if ((long) count * CIRCLE_STEP + CIRCLE_DIAMETER > Integer.MAX_VALUE) {
			coordinateViewport = false;
			return drawCirclesLegacy(gc);
		}
		final Rectangle clip = gc.getClipping();
		if (clip.width < 0) {
			coordinateViewport = false;
			return drawCirclesLegacy(gc);
		}
		final Rectangle clientArea = getClientArea();
		paintedCircleCount = count;
		paintedCircleY = (clientArea.height - CIRCLE_DIAMETER) / 2;
		coordinateViewport = true;
		rects.clear();

		// Conservative one-circle halo: even GC stroke edges are not excluded.
		long from = Math.floorDiv((long) clip.x - CIRCLE_STEP, CIRCLE_STEP);
		long until = Math.floorDiv((long) clip.x + clip.width + CIRCLE_STEP, CIRCLE_STEP) + 1;
		int first = (int) Math.max(0L, Math.min((long) count, from));
		int after = (int) Math.max((long) first, Math.min((long) count, until));
		for (int i = first; i < after; i++) {
			int x = i * CIRCLE_STEP;
			if (i == carousel.getSelection()) {
				gc.setBackground(circleBackground);
				gc.fillOval(x, paintedCircleY, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
			} else if (i == indexHover) {
				gc.setForeground(circleHoverColor);
				gc.drawOval(x, paintedCircleY, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
			} else {
				gc.setForeground(circleForeground);
				gc.drawOval(x, paintedCircleY, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
			}
		}
		return (int) ((long) count * CIRCLE_STEP + CIRCLE_DIAMETER);
	}

	/** Exact preimage loop retained for fallback, including its effects. */
	private int drawCirclesLegacy(final GC gc) {
		int x = 0;
		final Rectangle clientArea = getClientArea();
		rects.clear();
		for (int i = 0; i < carousel.getImages().size(); i++) {
			final Rectangle rect = new Rectangle(x, (clientArea.height - CIRCLE_DIAMETER) / 2, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
			rects.add(rect);
			if (i == carousel.getSelection()) {
				gc.setBackground(circleBackground);
				gc.fillOval(rect.x, rect.y, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
			} else if (i == indexHover) {
				//
				gc.setForeground(circleHoverColor);
				gc.drawOval(rect.x, rect.y, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
			} else {
				gc.setForeground(circleForeground);
				gc.drawOval(rect.x, rect.y, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
			}
			x += CIRCLE_DIAMETER + 5;
		}
		return x + CIRCLE_DIAMETER;

	}


	private void drawArrows(int x, final GC gc) {
		gc.setForeground(arrowColor);

		final Rectangle clientArea = getClientArea();
		final int topY = (clientArea.height - ARROW_SIZE) / 2;
		final int width = (int) (ARROW_SIZE * .5f);

		gc.setLineWidth(hoverLeftArrow ? 3 : 1);

		gc.drawPolyline(new int[] { x + width, topY, //
				x, topY + ARROW_SIZE / 2, //
				x + width, topY + ARROW_SIZE });

		arrowLeftArea = new Rectangle(x, topY, ARROW_SIZE, ARROW_SIZE);

		x += ARROW_SIZE * 3;

		gc.setLineWidth(hoverRightArrow ? 3 : 1);
		gc.drawPolyline(new int[] { x, topY, //
				x + width, topY + ARROW_SIZE / 2, //
				x, topY + ARROW_SIZE });
		arrowRightArea = new Rectangle(x, topY, ARROW_SIZE, ARROW_SIZE);
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {
		final Point superSize = super.computeSize(wHint, hHint, changed);
		final int width = 70 + carousel.getImages().size() * (CIRCLE_DIAMETER + 5);
		final int height = 40;
		return new Point(Math.max(superSize.x, width), Math.max(superSize.y, height));
	}

}
