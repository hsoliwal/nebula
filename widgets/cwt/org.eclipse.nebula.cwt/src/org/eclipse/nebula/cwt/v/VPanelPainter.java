/****************************************************************************
 * Copyright (c) 2008, 2009 Jeremy Dowdall
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Jeremy Dowdall <jeremyd@aspencloud.com> - initial API and implementation
 *****************************************************************************/

package org.eclipse.nebula.cwt.v;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

public class VPanelPainter implements IControlPainter {

	/** Bound retained scratch to 256 references; larger panels keep the original path. */
	private static final int MAX_PAINT_BUFFER = 256;
	private final Object paintBufferLock = new Object();
	private boolean bufferDisposed;

	public void dispose() {
		synchronized (paintBufferLock) {
			bufferDisposed = true;
		}
	}
	
	public void paintBackground(VControl control, Event e) {
		if(control.background != null && !control.background.isDisposed()) {
			e.gc.setBackground(control.background);
			e.gc.fillRectangle(control.bounds);
		}
	}
	
	public void paintBorders(VControl control, Event e) {
		if(control.hasStyle(SWT.BORDER)) {
			if(control.foreground != null) {
				e.gc.setForeground(control.foreground);
			} else {
				e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_BORDER));
			}
			e.gc.drawRectangle(control.bounds.x, control.bounds.y, control.bounds.width-1, control.bounds.height-1);
		}
	}
	
	public void paintContent(VControl control, Event e) {
		VPanel panel = (VPanel) control;
		// Public getChildren overrides and custom list behavior remain authoritative.
		if (panel == null || panel.getClass() != VPanel.class || panel.children == null
				|| panel.children.getClass() != ArrayList.class) {
			paintSnapshot(panel.getChildren(), e);
			return;
		}
		VControl[] buffer = acquirePaintBuffer(panel, panel.children.size());
		if (buffer == null) {
			paintSnapshot(panel.getChildren(), e);
			return;
		}
		VControl[] snapshot = buffer;
		try {
			// Capture the whole existing child order before invoking any child callback.
			snapshot = panel.children.toArray(buffer);
			paintSnapshot(snapshot, e);
		} finally {
			Arrays.fill(buffer, null);
			if (snapshot != buffer) Arrays.fill(snapshot, null);
			releasePaintBuffer(panel, snapshot);
		}
	}

	private static void paintSnapshot(VControl[] snapshot, Event e) {
		for (VControl child : snapshot) child.paintControl(e);
	}

	private VControl[] acquirePaintBuffer(VPanel panel, int count) {
		synchronized (paintBufferLock) {
			if (bufferDisposed || count > MAX_PAINT_BUFFER) return null;
			return panel.acquirePaintSnapshot(count);
		}
	}

	private void releasePaintBuffer(VPanel panel, VControl[] buffer) {
		synchronized (paintBufferLock) {
			if (!bufferDisposed && buffer.length <= MAX_PAINT_BUFFER) panel.releasePaintSnapshot(buffer);
		}
	}

}
