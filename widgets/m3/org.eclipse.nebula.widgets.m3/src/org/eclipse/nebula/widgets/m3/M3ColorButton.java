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
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A colour button: a drawn swatch of the current colour that opens the system colour dialog on click, Enter or Space
 * (the SWT counterpart of SwingX <code>JXColorSelectionButton</code>, re-derived from its documented behaviour).
 * <p>
 * The colour is one packed RGB int; a colour change repaints the swatch only and keeps one Color (replaced on change).
 * While hidden, a colour change only updates the int and the swatch paints once when shown.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Events: <code>Selection</code> when the colour changed (<code>Event.index</code> the
 * packed 0xRRGGBB value). Startup switch: <code>-Dswt.widget.mode.M3ColorButton=original</code> repaints the whole
 * button on every change.
 * </p>
 */
public class M3ColorButton extends Canvas {

	static final boolean VIEWPORT = WidgetMode.of("M3ColorButton", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final int INSET = 4, SIZE = 24;

	private final M3HiddenGate gate;
	private int rgb = 0x000000;
	private Color swatch;
	private boolean pressed, hover;
	private int width = -1, height = -1;

	/**
	 * Creates a black colour button.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3ColorButton(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | (VIEWPORT ? SWT.NO_REDRAW_RESIZE : 0));
		gate = new M3HiddenGate(this, null);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.MouseDown, e -> pressed = e.button == 1);
		addListener(SWT.MouseUp, e -> {
			if (pressed && e.button == 1 && getClientArea().contains(e.x, e.y)) {
				chooseColor();
			}
			pressed = false;
		});
		addListener(SWT.MouseEnter, e -> setHover(true));
		addListener(SWT.MouseExit, e -> setHover(false));
		addListener(SWT.KeyDown, e -> {
			if (e.character == '\r' || e.character == ' ') {
				chooseColor();
			}
		});
		addListener(SWT.Traverse, e -> e.doit = e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS);
		addListener(SWT.FocusIn, e -> repaintButton());
		addListener(SWT.FocusOut, e -> repaintButton());
		addListener(SWT.Dispose, e -> disposeSwatch());
		addListener(SWT.Resize, e -> resized());
		getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent event) {
				event.result = String.format("colour #%06X", Integer.valueOf(rgb)); //$NON-NLS-1$
			}
		});
	}

	/**
	 * Sets the colour; repaints the swatch.
	 *
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the colour is null
	 */
	public void setColor(RGB color) {
		checkWidget();
		if (color == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		setPacked((color.red << 16) | (color.green << 8) | color.blue, false);
	}

	/** @return the colour */
	public RGB getColor() {
		checkWidget();
		return new RGB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
	}

	/** @return the colour as 0xRRGGBB */
	public int getPackedColor() {
		checkWidget();
		return rgb;
	}

	/** @return the swatch rectangle in client coordinates */
	public Rectangle getSwatchBounds() {
		checkWidget();
		Rectangle client = getClientArea();
		return new Rectangle(client.x + INSET, client.y + INSET, Math.max(0, client.width - 2 * INSET), Math.max(0, client.height - 2 * INSET));
	}

	/** Opens the system colour dialog and takes its result. */
	public void chooseColor() {
		checkWidget();
		ColorDialog dialog = new ColorDialog(getShell());
		dialog.setRGB(getColor());
		RGB chosen = dialog.open();
		if (chosen != null) {
			setPacked((chosen.red << 16) | (chosen.green << 8) | chosen.blue, true);
		}
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		Rectangle trim = computeTrim(0, 0, wHint == SWT.DEFAULT ? SIZE + 2 * INSET : wHint, hHint == SWT.DEFAULT ? SIZE : hHint);
		return new Point(trim.width, trim.height);
	}

	private void setPacked(int value, boolean notify) {
		if (value == rgb) {
			return;
		}
		rgb = value;
		disposeSwatch();
		if (showing()) {
			if (VIEWPORT) {
				Rectangle area = getSwatchBounds();
				redraw(area.x, area.y, area.width, area.height, false);
			} else {
				M3Counters.fullRepaints++;
				redraw();
			}
		}
		Event event = new Event();
		event.index = value;
		notifyListeners(SWT.Selection, event);
	}

	private void setHover(boolean value) {
		if (hover != value) {
			hover = value;
			repaintButton();
		}
	}

	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	private void repaintButton() {
		if (showing()) {
			M3Counters.fullRepaints += VIEWPORT ? 0 : 1;
			redraw();
		}
	}

	/*
	 * The swatch and the border follow the right and bottom edges: a resize damages only the strips from the nearer old
	 * or new edge (minus the inset and border) to the end; the rest of the swatch keeps its pixels (NO_REDRAW_RESIZE).
	 */
	private void resized() {
		Rectangle client = getClientArea();
		int oldWidth = width, oldHeight = height;
		width = client.width;
		height = client.height;
		if (!VIEWPORT || oldWidth < 0 || !gate.visible()) {
			return;
		}
		int x = Math.max(0, Math.min(oldWidth, width) - INSET - 1), y = Math.max(0, Math.min(oldHeight, height) - INSET - 1);
		if (width != oldWidth) {
			redraw(client.x + x, client.y, client.width - x, client.height, false);
		}
		if (height != oldHeight) {
			redraw(client.x, client.y + y, client.width, client.height - y, false);
		}
	}

	private void disposeSwatch() {
		if (swatch != null) {
			swatch.dispose();
			swatch = null;
		}
	}

	private void paint(Event event) {
		GC gc = event.gc;
		Rectangle client = getClientArea();
		gc.setBackground(getDisplay().getSystemColor(hover ? SWT.COLOR_WIDGET_LIGHT_SHADOW : SWT.COLOR_WIDGET_BACKGROUND));
		gc.fillRectangle(event.x, event.y, event.width, event.height);
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawRectangle(client.x, client.y, client.width - 1, client.height - 1);
		if (swatch == null) {
			swatch = new Color(getDisplay(), (rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
		}
		Rectangle area = getSwatchBounds();
		gc.setBackground(swatch);
		gc.fillRectangle(area);
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		gc.drawRectangle(area.x, area.y, Math.max(0, area.width - 1), Math.max(0, area.height - 1));
		if (isFocusControl()) {
			gc.drawFocus(client.x + 1, client.y + 1, client.width - 2, client.height - 2);
		}
	}
}
