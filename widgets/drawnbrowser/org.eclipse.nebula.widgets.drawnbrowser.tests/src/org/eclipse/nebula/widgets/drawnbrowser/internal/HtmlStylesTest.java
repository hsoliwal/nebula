/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.ALIGN;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.BACKGROUND;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.BITS;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.CENTER;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.COLOR;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.INDENT;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlStyles.RIGHT;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The drawn browser's CSS subset: colours, the cascade, inheritance. */
public class HtmlStylesTest {

	@Test
	public void coloursParseAndOthersAreNone() {
		assertEquals(0xFFFF0000, HtmlStyles.color("#f00"));
		assertEquals(0xFF1A2B3C, HtmlStyles.color("#1A2B3C"));
		assertEquals(0xFF0A141E, HtmlStyles.color("rgb(10, 20, 30)"));
		assertEquals(0xFF008080, HtmlStyles.color("Teal"));
		assertEquals(0, HtmlStyles.color("#12"));
		assertEquals(0, HtmlStyles.color("hsl(1,2%,3%)"));
		assertEquals(0, HtmlStyles.color("chartreuse"));
	}

	@Test
	public void classBeatsTagAndTagClassBeatsBothWhateverTheOrder() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet("p.note { color: #00f } .note { color: #0f0 } p { color: #f00 }");
		int[] root = HtmlStyles.root();
		assertEquals(0xFFFF0000, styles.compute(root, "p", null, null)[COLOR]);
		assertEquals(0xFF00FF00, styles.compute(root, "div", "note", null)[COLOR]);
		assertEquals(0xFF0000FF, styles.compute(root, "p", "other note", null)[COLOR]);
		assertEquals("the style attribute wins", 0xFF808080, styles.compute(root, "p", "note", "color: gray")[COLOR]);
	}

	@Test
	public void textPropertiesAndAlignmentInheritButBackgroundAndIndentDoNot() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet("/* page */ div { color: navy; font-weight: bold; text-align: center; background-color: yellow; margin-left: 30px }");
		int[] div = styles.compute(HtmlStyles.root(), "div", null, null);
		assertEquals(0xFFFFFF00, div[BACKGROUND]);
		assertEquals(30, div[INDENT]);
		int[] span = styles.compute(div, "span", null, "font-style: italic");
		assertEquals(0xFF000080, span[COLOR]);
		assertEquals(HtmlDocument.BOLD | HtmlDocument.ITALIC, span[BITS]);
		assertEquals(CENTER, span[ALIGN]);
		assertEquals(0, span[BACKGROUND]);
		assertEquals(0, span[INDENT]);
		int[] plain = styles.compute(span, "em", null, "font-weight: normal; text-decoration: underline line-through; text-align: right");
		assertEquals(HtmlDocument.ITALIC | HtmlDocument.UNDERLINE | HtmlDocument.STRIKE, plain[BITS]);
		assertEquals(RIGHT, plain[ALIGN]);
	}

	@Test
	public void whatIsNotUnderstoodIsIgnored() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet("div > p { color: red } #id { color: red } p { colour: red; margin-left: 2em; color: bogus } broken {");
		int[] p = styles.compute(HtmlStyles.root(), "p", null, "color:;;: red; text-align: justify");
		assertEquals(0, p[COLOR]);
		assertEquals(0, p[INDENT]);
		assertEquals(HtmlStyles.LEFT, p[ALIGN]);
	}
}
