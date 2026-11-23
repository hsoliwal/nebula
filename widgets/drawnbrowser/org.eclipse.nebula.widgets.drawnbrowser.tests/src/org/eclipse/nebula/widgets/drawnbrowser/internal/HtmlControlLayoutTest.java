/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;

/** Explicit native-control sizes are distinct from intrinsic metrics and ancestor dimensions. */
public class HtmlControlLayoutTest {

	private HtmlLayout layout(String html) {
		Display display = Display.getDefault();
		HtmlLayout layout = new HtmlLayout(display, HtmlDocument.parse(html), display.getSystemFont(),
				display.getSystemColor(SWT.COLOR_BLUE));
		layout.setWidth(300);
		layout.setViewport(200, 0);
		return layout;
	}

	@Test
	public void ownCssDimensionsRemainRequestedMetricsBeforeNativeSizeConstraints() {
		String style = " style='width:110px;height:28px'";
		for (String html : new String[] { "<input" + style + ">", "<textarea" + style + ">area</textarea>",
				"<select" + style + "><option>one</option></select>", "<button" + style + ">button</button>" }) {
			HtmlLayout layout = layout(html);
			try {
				assertEquals(html, 110, layout.controlWidth(0));
				assertEquals(html, 28, layout.controlHeight(0));
			} finally {
				layout.dispose();
			}
		}
	}

	@Test
	public void zeroCssDimensionsDoNotFallBackToIntrinsicControlSize() {
		HtmlLayout layout = layout("<input style='width:0;height:0'>");
		try {
			assertEquals(0, layout.controlWidth(0));
			assertEquals(0, layout.controlHeight(0));
			layout.ensure(Integer.MAX_VALUE);
			assertNotNull(layout.positioned());
			assertTrue(layout.totalHeight() >= 0);
		} finally {
			layout.dispose();
		}
	}

	@Test
	public void resettingOwnDimensionsToAutoPreservesIntrinsicRowsAndColumns() {
		HtmlLayout ordinary = layout("<textarea rows=4 cols=12>value</textarea>");
		HtmlLayout reset = layout("<div style='width:260px;height:180px'><textarea rows=4 cols=12 "
				+ "style='width:77px;width:auto;height:31px;height:auto'>value</textarea></div>");
		try {
			assertEquals(ordinary.controlWidth(0), reset.controlWidth(0));
			assertEquals(ordinary.controlHeight(0), reset.controlHeight(0));
		} finally {
			ordinary.dispose();
			reset.dispose();
		}
	}

	@Test
	public void largestCssControlHeightSaturatesItsGapAllocation() {
		HtmlLayout layout = layout("<input style='width:2147483647px;height:2147483647px'>");
		try {
			assertEquals(Integer.MAX_VALUE, layout.controlWidth(0));
			assertEquals(Integer.MAX_VALUE, layout.controlHeight(0));
			layout.ensure(Integer.MAX_VALUE);
			assertEquals(Integer.MAX_VALUE, layout.height(0));
			assertEquals(Integer.MAX_VALUE, layout.totalHeight());
		} finally {
			layout.dispose();
		}
	}
}
