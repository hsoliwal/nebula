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
 *******************************************************************************/
package org.eclipse.nebula.visualization.internal.xygraph.toolbar;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget D leaf of the XYGraph toolbar button: setEnabled with the state the button already shows keeps its
 * contents (no new figure, relayout or repaint); a new state shows the enabled or the gray image.
 */
public class GrayableButtonEnabledTest {

	private Image image;

	@Before
	public void setUp() {
		Display display = Display.getDefault();
		image = new Image(display, 16, 16);
		GC gc = new GC(image);
		gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
		gc.fillRectangle(0, 0, 16, 16);
		gc.dispose();
	}

	@After
	public void tearDown() {
		image.dispose();
	}

	@Test
	public void theStateShownKeepsTheContents() {
		GrayableButton button = new GrayableButton(image);
		button.setEnabled(false);
		IFigure gray = contents(button);
		assertSame(button.grayImage, ((ImageFigure) gray).getImage());

		button.setEnabled(false);
		assertSame("the state shown keeps the contents", gray, contents(button));

		button.setEnabled(true);
		IFigure enabled = contents(button);
		assertNotSame(gray, enabled);
		assertSame(image, ((ImageFigure) enabled).getImage());
		button.setEnabled(true);
		assertSame("the state shown keeps the contents", enabled, contents(button));
	}

	private static IFigure contents(GrayableButton button) {
		return (IFigure) button.getChildren().get(0);
	}
}
