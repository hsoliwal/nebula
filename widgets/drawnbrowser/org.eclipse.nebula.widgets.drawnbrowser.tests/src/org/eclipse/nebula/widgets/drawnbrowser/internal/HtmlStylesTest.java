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

	@Test
	public void boxModelShorthandsSidesAndBorders() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet("p { margin: 10px 20px 30px 40px; padding: 5px; border: 2px solid red } div { margin: 4px 8px; padding: 1px 2px 3px }"
				+ " h1 { margin: 2em; border-width: 3px; border-color: #00f }");
		int[] root = HtmlStyles.root(), p = styles.compute(root, "p", null, null);
		assertEquals(10, p[HtmlStyles.MARGIN_TOP]);
		assertEquals(20, p[HtmlStyles.MARGIN_RIGHT]);
		assertEquals(30, p[HtmlStyles.MARGIN_BOTTOM]);
		assertEquals("the left margin is the indent", 40, p[INDENT]);
		for (int side : new int[] { HtmlStyles.PADDING_TOP, HtmlStyles.PADDING_RIGHT, HtmlStyles.PADDING_BOTTOM, HtmlStyles.PADDING_LEFT }) {
			assertEquals(5, p[side]);
		}
		assertEquals(2, p[HtmlStyles.BORDER_WIDTH]);
		assertEquals(0xFFFF0000, p[HtmlStyles.BORDER_COLOR]);
		int[] div = styles.compute(root, "div", null, null);
		assertEquals("two values: vertical", 4, div[HtmlStyles.MARGIN_BOTTOM]);
		assertEquals("and horizontal", 8, div[INDENT]);
		assertEquals("three values: the left repeats the right", 2, div[HtmlStyles.PADDING_LEFT]);
		assertEquals(3, div[HtmlStyles.PADDING_BOTTOM]);
		int[] h1 = styles.compute(root, "h1", null, null);
		assertEquals("em is not a px length: ignored", 0, h1[HtmlStyles.MARGIN_TOP]);
		assertEquals(3, h1[HtmlStyles.BORDER_WIDTH]);
		assertEquals(0xFF0000FF, h1[HtmlStyles.BORDER_COLOR]);
		int[] own = styles.compute(root, "p", null, "padding-left: 9px; border-style: none");
		assertEquals(9, own[HtmlStyles.PADDING_LEFT]);
		assertEquals(5, own[HtmlStyles.PADDING_TOP]);
		assertEquals("border-style none removes the border", 0, own[HtmlStyles.BORDER_WIDTH]);
		int[] child = styles.compute(p, "span", null, null);
		assertEquals("the box is not inherited", 0, child[HtmlStyles.MARGIN_TOP]);
		assertEquals(0, child[HtmlStyles.BORDER_WIDTH]);
		assertEquals(0, child[HtmlStyles.PADDING_LEFT]);
	}

	@Test
	public void positionOffsetsAndZIndex() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet(".a { position: absolute; top: -12px; left: 30px; z-index: 5 } .f { position: fixed; top: auto; z-index: -2 }"
				+ " .r { position: relative; left: 4em; z-index: x }");
		int[] root = HtmlStyles.root(), a = styles.compute(root, "div", "a", null);
		assertEquals(HtmlStyles.ABSOLUTE, a[HtmlStyles.POSITION]);
		assertEquals("negative offsets are allowed", -12, a[HtmlStyles.POS_TOP]);
		assertEquals(30, a[HtmlStyles.POS_LEFT]);
		assertEquals(5, a[HtmlStyles.Z_INDEX]);
		int[] f = styles.compute(root, "div", "f", null);
		assertEquals(HtmlStyles.FIXED, f[HtmlStyles.POSITION]);
		assertEquals(HtmlStyles.AUTO, f[HtmlStyles.POS_TOP]);
		assertEquals(HtmlStyles.AUTO, f[HtmlStyles.POS_LEFT]);
		assertEquals(-2, f[HtmlStyles.Z_INDEX]);
		int[] r = styles.compute(root, "div", "r", null);
		assertEquals(HtmlStyles.RELATIVE, r[HtmlStyles.POSITION]);
		assertEquals("em is not px: auto stays", HtmlStyles.AUTO, r[HtmlStyles.POS_LEFT]);
		assertEquals("a bad z-index is ignored", 0, r[HtmlStyles.Z_INDEX]);
		int[] child = styles.compute(a, "span", null, null);
		assertEquals("the position is not inherited", HtmlStyles.STATIC, child[HtmlStyles.POSITION]);
		assertEquals(HtmlStyles.AUTO, child[HtmlStyles.POS_TOP]);
		assertEquals(0, child[HtmlStyles.Z_INDEX]);
		assertEquals(HtmlStyles.STATIC, styles.compute(root, "div", "a", "position: static")[HtmlStyles.POSITION]);
	}
}
