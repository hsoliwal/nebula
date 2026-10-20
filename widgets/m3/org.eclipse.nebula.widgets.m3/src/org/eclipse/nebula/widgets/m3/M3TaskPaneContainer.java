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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * A vertical stack of {@link M3TaskPane}s (or any controls) with margins and spacing (the SWT counterpart of SwingX
 * <code>JXTaskPaneContainer</code>, re-derived from its documented behaviour). Put it in a ScrolledComposite for
 * scrolling.
 * <p>
 * The stack keeps the children's heights and tops in int lanes. When one pane changes size (expand, collapse, a frame
 * of the animation, new content) SWT flushes only that child, so only that child is measured again; panes above it are
 * not touched and panes below it are moved, not resized, so their content is not laid out again. A width change
 * re-measures every child (their content may wrap) but moves no child whose bounds stay the same.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Startup switch: <code>-Dswt.widget.mode.M3TaskPaneContainer=original</code> measures
 * every child on every layout, as a plain stacking layout would.
 * </p>
 */
public class M3TaskPaneContainer extends Composite {

	static final boolean VIEWPORT = WidgetMode.of("M3TaskPaneContainer", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private int margin = 8, spacing = 8;

	/**
	 * Creates an empty container.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3TaskPaneContainer(Composite parent, int style) {
		super(parent, style);
		super.setLayout(new Stack());
		setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}

	/** The container stacks its children itself; a layout cannot be set. */
	@Override
	public void setLayout(Layout layout) {
		checkWidget();
	}

	/**
	 * Sets the margin around the stack and the spacing between children.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if a value is negative
	 */
	public void setSpacing(int margin, int spacing) {
		checkWidget();
		if (margin < 0 || spacing < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.margin = margin;
		this.spacing = spacing;
		layout(true);
	}

	/** @return the spacing between children */
	public int getSpacing() {
		checkWidget();
		return spacing;
	}

	/** @return the margin around the stack */
	public int getMargin() {
		checkWidget();
		return margin;
	}

	/*
	 * Height and top lanes per child, by child order; dirty children (flushed by SWT along a layout(Control[]) path,
	 * or all after a width change) are measured again; tops are a prefix sum.
	 */
	private final class Stack extends Layout {

		private Control[] children = new Control[0];
		private int[] heights = new int[0], tops = new int[0];
		private int width = -1;
		private final List<Control> dirty = new ArrayList<>();

		@Override
		protected boolean flushCache(Control control) {
			if (!VIEWPORT) {
				return false;
			}
			if (!dirty.contains(control)) {
				dirty.add(control);
			}
			return true;
		}

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			Control[] list = getChildren();
			int w = 0, h = 2 * margin + Math.max(0, list.length - 1) * spacing;
			int childHint = wHint == SWT.DEFAULT ? SWT.DEFAULT : Math.max(0, wHint - 2 * margin);
			for (Control child : list) {
				Point size = child.computeSize(childHint, SWT.DEFAULT, flushCache);
				w = Math.max(w, size.x);
				h += size.y;
			}
			return new Point(wHint != SWT.DEFAULT ? wHint : w + 2 * margin, h);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			M3Counters.layouts++;
			Rectangle client = getClientArea();
			Control[] list = getChildren();
			int inner = Math.max(0, client.width - 2 * margin);
			boolean all = !VIEWPORT || flushCache && dirty.isEmpty() || inner != width || !Arrays.equals(list, children);
			if (list.length != heights.length) {
				heights = Arrays.copyOf(heights, list.length);
				tops = Arrays.copyOf(tops, list.length);
			}
			children = list;
			width = inner;
			int y = client.y + margin;
			for (int i = 0; i < list.length; i++) {
				Control child = list[i];
				if (all || dirty.contains(child)) {
					heights[i] = child.computeSize(inner, SWT.DEFAULT, true).y;
					M3Counters.measures++;
				}
				Rectangle bounds = child.getBounds();
				int x = client.x + margin;
				if (bounds.width != inner || bounds.height != heights[i]) {
					child.setBounds(x, y, inner, heights[i]);
					M3Counters.moves++;
				} else if (bounds.x != x || bounds.y != y) {
					child.setLocation(x, y);
					M3Counters.moves++;
				}
				tops[i] = y;
				y += heights[i] + spacing;
			}
			dirty.clear();
		}
	}
}
