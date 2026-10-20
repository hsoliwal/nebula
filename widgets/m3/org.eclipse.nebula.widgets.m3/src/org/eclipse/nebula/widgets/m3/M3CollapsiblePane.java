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
import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;

/**
 * A pane that collapses and expands its content, optionally animated (the SWT counterpart of SwingX
 * <code>JXCollapsiblePane</code>, re-derived from its documented behaviour). Fill {@link #getContent()}; the pane's
 * preferred size along its axis is the visible extent, from 0 (collapsed) to the content's preferred size.
 * <p>
 * The state is a few ints (extent, target, full width and height measured once per expansion) and two flags. While
 * collapsed the content is hidden, so it is neither laid out nor painted. An animation frame changes only the extent
 * and slides the content (a move, never a resize): the content is measured once when the animation starts and is not
 * laid out again until the pane's own size across the axis changes. A resize of the pane moves or resizes the content
 * only; nothing is measured on a resize.
 * </p>
 * <p>
 * Styles: <code>VERTICAL</code> (default, collapses the height) or <code>HORIZONTAL</code> (collapses the width).
 * Events: <code>Expand</code> and <code>Collapse</code> when a state change completes. Startup switch:
 * <code>-Dswt.widget.mode.M3CollapsiblePane=original</code> measures and lays out the content on every frame and
 * keeps a collapsed content visible at zero size, as a plain implementation would. RWT: timerExec, setBounds and
 * layout only; no drawing.
 * </p>
 */
public class M3CollapsiblePane extends Composite {

	static final boolean VIEWPORT = WidgetMode.of("M3CollapsiblePane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3CollapsiblePane"); //$NON-NLS-1$

	private final Composite content;
	private final boolean vertical;
	private boolean collapsed, animated = true, animating;
	private int extent = -1, target, fullWidth = -1, fullHeight = -1, steps = 8, delay = 15;
	private final Runnable frame = this::frame;

	/**
	 * Creates an expanded pane with an empty content composite.
	 *
	 * @param parent the parent
	 * @param style VERTICAL or HORIZONTAL, BORDER
	 */
	public M3CollapsiblePane(Composite parent, int style) {
		super(parent, style & ~(SWT.VERTICAL | SWT.HORIZONTAL));
		vertical = (style & SWT.HORIZONTAL) == 0;
		content = new Composite(this, SWT.NONE);
		super.setLayout(new PaneLayout());
		addListener(SWT.Dispose, e -> getDisplay().timerExec(-1, frame));
	}

	/** @return the composite to put the collapsible controls in */
	public Composite getContent() {
		checkWidget();
		return content;
	}

	/** The pane lays out its content itself; a layout cannot be set. */
	@Override
	public void setLayout(Layout layout) {
		checkWidget();
	}

	/**
	 * Collapses or expands the content, animated when {@link #isAnimated()} and the pane is visible.
	 *
	 * @param collapsed true to collapse
	 */
	public void setCollapsed(boolean collapsed) {
		checkWidget();
		if (this.collapsed == collapsed && !animating) {
			return;
		}
		this.collapsed = collapsed;
		LOG.debug(() -> (collapsed ? "collapse" : "expand") + (animated ? " animated" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		measure();
		int full = vertical ? fullHeight : fullWidth;
		if (extent < 0) {
			extent = collapsed ? full : 0;
		}
		target = collapsed ? 0 : full;
		if (animated && isVisible() && extent != target) {
			content.setVisible(true);
			if (!animating) {
				animating = true;
				getDisplay().timerExec(delay, frame);
			}
		} else {
			finish();
		}
	}

	/** @return true when collapsed, or collapsing */
	public boolean isCollapsed() {
		checkWidget();
		return collapsed;
	}

	/** @return true while an animation runs */
	public boolean isAnimating() {
		checkWidget();
		return animating;
	}

	/** Turns the animation on or off. */
	public void setAnimated(boolean animated) {
		checkWidget();
		this.animated = animated;
	}

	/** @return true when state changes are animated */
	public boolean isAnimated() {
		checkWidget();
		return animated;
	}

	/**
	 * Sets the number of frames of an animation and the time between two frames.
	 *
	 * @param steps at least 1
	 * @param milliseconds at least 1
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if a value is less than 1
	 */
	public void setAnimation(int steps, int milliseconds) {
		checkWidget();
		if (steps < 1 || milliseconds < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.steps = steps;
		delay = milliseconds;
	}

	/** @return the visible extent along the axis in pixels (the content's preferred size when expanded) */
	public int getExtent() {
		checkWidget();
		if (extent < 0) {
			measure();
			return vertical ? fullHeight : fullWidth;
		}
		return extent;
	}

	/* measures the content's preferred size once per state change (never on a frame or a resize) */
	private void measure() {
		Rectangle client = getClientArea();
		int wHint = vertical && client.width > 0 ? client.width : SWT.DEFAULT;
		int hHint = !vertical && client.height > 0 ? client.height : SWT.DEFAULT;
		Point size = content.computeSize(wHint, hHint);
		fullWidth = size.x;
		fullHeight = size.y;
		M3Counters.fullLayouts++;
	}

	private void frame() {
		if (isDisposed() || !animating) {
			return;
		}
		int full = Math.max(1, vertical ? fullHeight : fullWidth);
		int step = Math.max(1, (full + steps - 1) / steps);
		extent = extent < target ? Math.min(target, extent + step) : Math.max(target, extent - step);
		if (extent == target) {
			finish();
			return;
		}
		if (!VIEWPORT) {
			measure();
		}
		relayoutParent();
		getDisplay().timerExec(delay, frame);
	}

	private void finish() {
		animating = false;
		getDisplay().timerExec(-1, frame);
		extent = collapsed ? 0 : -1;
		if (VIEWPORT) {
			content.setVisible(!collapsed);
		}
		relayoutParent();
		Event event = new Event();
		notifyListeners(collapsed ? SWT.Collapse : SWT.Expand, event);
	}

	private void relayoutParent() {
		Composite parent = getParent();
		parent.layout(new Control[] { this });
	}

	/*
	 * Along the axis the pane shows `extent` pixels of the content, whose far edge is anchored to the pane's far edge
	 * (the content slides); across the axis the content fills the pane.
	 */
	private final class PaneLayout extends Layout {

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			if (fullWidth < 0 || !animating && !collapsed) {
				Point size = content.computeSize(vertical ? wHint : SWT.DEFAULT, vertical ? SWT.DEFAULT : hHint, flushCache);
				fullWidth = size.x;
				fullHeight = size.y;
			}
			int along = extent >= 0 ? extent : vertical ? fullHeight : fullWidth;
			return vertical ? new Point(wHint != SWT.DEFAULT ? wHint : fullWidth, along)
					: new Point(along, hHint != SWT.DEFAULT ? hHint : fullHeight);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			M3Counters.layouts++;
			Rectangle client = getClientArea();
			if (collapsed && !animating && VIEWPORT) {
				return;
			}
			if (vertical) {
				int height = Math.max(fullHeight, client.height);
				setContentBounds(client.x, client.y + client.height - height, client.width, height);
			} else {
				int width = Math.max(fullWidth, client.width);
				setContentBounds(client.x + client.width - width, client.y, width, client.height);
			}
		}

		private void setContentBounds(int x, int y, int width, int height) {
			Rectangle bounds = content.getBounds();
			if (bounds.width != width || bounds.height != height) {
				content.setBounds(x, y, width, height);
				M3Counters.moves++;
			} else if (bounds.x != x || bounds.y != y) {
				content.setLocation(x, y);
				M3Counters.moves++;
			}
		}
	}
}
