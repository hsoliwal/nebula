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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The drawn browser's CSS subset: colours, the cascade, inheritance. */
public class HtmlStylesTest {

	@Test
	public void shrinkAcceptsFiniteCssNumbersAndDoesNotInherit() {
		HtmlStyles styles = new HtmlStyles();
		assertEquals(1, HtmlStyles.shrink(HtmlStyles.root()), 0);
		assertEquals(1, HtmlStyles.shrink(new int[32]), 0);
		String[] values = { "0", "+0", "-0", ".5", "+1.25", "2e-3", "1e308" };
		double[] expected = { 0, 0, 0, .5, 1.25, .002, 1e308 };
		for (int i = 0; i < values.length; i++) {
			int[] style = styles.compute(HtmlStyles.root(), "div", null, "flex-shrink:" + values[i]);
			assertEquals(values[i], expected[i], HtmlStyles.shrink(style), 0);
			assertEquals("not inherited: " + values[i], 1,
					HtmlStyles.shrink(styles.compute(style, "div", null, null)), 0);
		}
	}

	@Test
	public void invalidShrinkDeclarationsLeaveThePrecedingValue() {
		HtmlStyles styles = new HtmlStyles();
		for (String invalid : new String[] { "-0.01", "NaN", "Infinity", "1e309", "0x1.0p0", "1f", "1.", "1 2", "--1", "" }) {
			assertEquals(invalid, .25, HtmlStyles.shrink(styles.compute(HtmlStyles.root(), "div", null,
					"flex-shrink:.25;flex-shrink:" + invalid)), 0);
		}
		styles.addSheet(".panel {flex-shrink:.5} .panel {flex-shrink:2e-1}");
		assertEquals(.2, HtmlStyles.shrink(styles.compute(HtmlStyles.root(), "div", "panel", "flex-shrink:NaN")), 0);
		assertEquals(.75, HtmlStyles.shrink(styles.compute(HtmlStyles.root(), "div", "panel", "flex-shrink:.75")), 0);
	}

	@Test
	public void flexShorthandResetsAndCarriesTheShrinkFactor() {
		HtmlStyles styles = new HtmlStyles();
		String[] values = { "none", "auto", "initial", "0 0 240px", "1 0", "1 10px", "1", "2 .125 auto", "40px" };
		int[] grow = { 0, 1, 0, 0, 1, 1, 1, 2, 1 }, basis = { -1, -1, -1, 240, 0, 10, 0, -1, 40 };
		double[] shrink = { 0, 1, 1, 0, 0, 1, 1, .125, 1 };
		for (int i = 0; i < values.length; i++) {
			int[] style = styles.compute(HtmlStyles.root(), "div", null, "flex:3 .75 90px;flex:" + values[i]);
			assertEquals(values[i], grow[i], style[HtmlStyles.FLEX_GROW]);
			assertEquals(values[i], basis[i], style[HtmlStyles.FLEX_BASIS]);
			assertEquals(values[i], shrink[i], HtmlStyles.shrink(style), 0);
		}
	}

	@Test
	public void anInvalidFlexShorthandDoesNotPartiallyOverwriteTheCascade() {
		HtmlStyles styles = new HtmlStyles();
		for (String invalid : new String[] { "1 -1 20px", "1 NaN 20px", "1 1e309 20px", "1 .5 garbage", "1 2 30px extra" }) {
			int[] style = styles.compute(HtmlStyles.root(), "div", null, "flex:2 .25 90px;flex:" + invalid);
			assertEquals(invalid, 2, style[HtmlStyles.FLEX_GROW]);
			assertEquals(invalid, 90, style[HtmlStyles.FLEX_BASIS]);
			assertEquals(invalid, .25, HtmlStyles.shrink(style), 0);
		}
	}

	@Test
	public void shrinkStorageCannotAliasTheExistingPositionalMask() {
		HtmlStyles styles = new HtmlStyles();
		String css = "color:blue;background:red;position:relative;top:-3px;right:5px;z-index:0;width:70px;border:2px inset gray";
		int[] original = styles.compute(HtmlStyles.root(), "div", null, css);
		int[] extended = styles.compute(HtmlStyles.root(), "div", null, css + ";flex-shrink:4.9e-324");
		org.junit.Assert.assertArrayEquals(java.util.Arrays.copyOf(original, 32), java.util.Arrays.copyOf(extended, 32));
		assertEquals(Double.MIN_VALUE, HtmlStyles.shrink(extended), 0);
		assertEquals(0, extended[HtmlStyles.Z_AUTO]);
	}

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
	public void schemeBordersAreParsedAndNotInherited() {
		HtmlStyles styles = new HtmlStyles();
		for (String name : new String[] {"solid", "inset", "outset", "groove", "ridge", "double"}) {
			int expected = switch (name) {
				case "inset" -> HtmlStyles.BORDER_INSET;
				case "outset" -> HtmlStyles.BORDER_OUTSET;
				case "groove" -> HtmlStyles.BORDER_GROOVE;
				case "ridge" -> HtmlStyles.BORDER_RIDGE;
				case "double" -> HtmlStyles.BORDER_DOUBLE;
				default -> HtmlStyles.BORDER_SOLID;
			};
			int[] box = styles.compute(HtmlStyles.root(), "p", null, "border: 4px " + name + " #808080");
			assertEquals(name, expected, box[HtmlStyles.BORDER_STYLE]);
			assertEquals(4, box[HtmlStyles.BORDER_WIDTH]);
			assertEquals("style is not inherited", HtmlStyles.BORDER_SOLID,
					styles.compute(box, "span", null, null)[HtmlStyles.BORDER_STYLE]);
		}
		assertEquals(HtmlStyles.BORDER_OUTSET,
				styles.compute(HtmlStyles.root(), "p", null, "border: 4px solid red; border-style: outset")[HtmlStyles.BORDER_STYLE]);
		assertEquals(0, styles.compute(HtmlStyles.root(), "p", null, "border: 4px ridge red; border-style: none")[HtmlStyles.BORDER_WIDTH]);
		HtmlDocument document = HtmlDocument.parse("<p style='border:4px inset #808080'>a</p><p style='border:4px outset #808080'>b</p>");
		assertEquals(HtmlStyles.BORDER_INSET, document.blockBorderStyle[0]);
		assertEquals(HtmlStyles.BORDER_OUTSET, document.blockBorderStyle[1]);
	}

	@Test
	public void overflowClipsAreExplicitNotInheritedAndDoNotCollideWithPositionMask() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet(".panel { overflow: hidden; position: fixed; z-index: -3 } .visible { overflow: visible }");
		int[] root = HtmlStyles.root();
		int[] panel = styles.compute(root, "div", "panel", "overflow: nonsense");
		assertEquals(HtmlStyles.OVERFLOW_HIDDEN, panel[HtmlStyles.OVERFLOW]);
		assertEquals(HtmlStyles.FIXED, panel[HtmlStyles.POSITION]);
		assertEquals(-3, panel[HtmlStyles.Z_INDEX]);
		assertEquals("overflow is not inherited", HtmlStyles.OVERFLOW_VISIBLE,
				styles.compute(panel, "div", null, null)[HtmlStyles.OVERFLOW]);
		assertEquals(HtmlStyles.OVERFLOW_HIDDEN,
				styles.compute(root, "div", null, "overflow: clip")[HtmlStyles.OVERFLOW]);
		assertEquals(HtmlStyles.OVERFLOW_VISIBLE,
				styles.compute(panel, "div", "visible", "overflow: hidden; overflow: visible")[HtmlStyles.OVERFLOW]);
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

	@Test
	public void displayAndFlexProperties() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet(".row { display: flex; justify-content: space-between; gap: 12px } .grow { flex: 2 1 40px }"
				+ " .w { width: 90px; flex-grow: 1 } .gone { display: none }");
		int[] root = HtmlStyles.root(), row = styles.compute(root, "div", "row", null);
		assertEquals(HtmlStyles.DISPLAY_FLEX, row[HtmlStyles.DISPLAY]);
		assertEquals(FlexRow.SPACE_BETWEEN, row[HtmlStyles.JUSTIFY]);
		assertEquals(12, row[HtmlStyles.GAP]);
		int[] grow = styles.compute(root, "div", "grow", null);
		assertEquals("flex: grow shrink basis", 2, grow[HtmlStyles.FLEX_GROW]);
		assertEquals(40, grow[HtmlStyles.FLEX_BASIS]);
		int[] w = styles.compute(root, "div", "w", null);
		assertEquals("width is the basis", 90, w[HtmlStyles.FLEX_BASIS]);
		assertEquals(1, w[HtmlStyles.FLEX_GROW]);
		assertEquals(HtmlStyles.DISPLAY_NONE, styles.compute(root, "p", "gone", null)[HtmlStyles.DISPLAY]);
		int[] child = styles.compute(row, "span", null, null);
		assertEquals("display is not inherited", HtmlStyles.DISPLAY_DEFAULT, child[HtmlStyles.DISPLAY]);
		assertEquals(-1, child[HtmlStyles.FLEX_BASIS]);
		assertEquals(0, child[HtmlStyles.GAP]);
	}

	@Test
	public void gridProperties() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet(".g { display: grid; grid-template-columns: 100px repeat(2, 1fr); gap: 8px 12px }"
				+ " .h { display: inline-grid; grid-template-columns: 100px 1fr 1fr; row-gap: 3px; column-gap: 4px }"
				+ " .wide { grid-column: span 2 } .full { grid-column: 1 / -1 } .bad { grid-template-columns: 1em }");
		int[] root = HtmlStyles.root(), g = styles.compute(root, "div", "g", null), h = styles.compute(root, "div", "h", null);
		assertEquals(HtmlStyles.DISPLAY_GRID, g[HtmlStyles.DISPLAY]);
		assertArrayEquals(new int[] { 100, GridTracks.FR | 100, GridTracks.FR | 100 }, styles.tracks(g[HtmlStyles.TRACKS]));
		assertEquals("gap: the row gap, then the column gap", 8, g[HtmlStyles.ROW_GAP]);
		assertEquals(12, g[HtmlStyles.GAP]);
		assertEquals("the same list is kept once", g[HtmlStyles.TRACKS], h[HtmlStyles.TRACKS]);
		assertEquals(HtmlStyles.DISPLAY_GRID, h[HtmlStyles.DISPLAY]);
		assertEquals(3, h[HtmlStyles.ROW_GAP]);
		assertEquals(4, h[HtmlStyles.GAP]);
		assertEquals(2, styles.compute(root, "div", "wide", null)[HtmlStyles.COLUMN_SPAN]);
		assertEquals("1 / -1: the whole row", GridTracks.MAX_TRACKS, styles.compute(root, "div", "full", null)[HtmlStyles.COLUMN_SPAN]);
		assertEquals("a list not understood is ignored", 0, styles.compute(root, "div", "bad", null)[HtmlStyles.TRACKS]);
		assertEquals(0, styles.tracks(0).length);
		int[] child = styles.compute(g, "div", null, null);
		assertEquals("grid properties are not inherited", 0, child[HtmlStyles.TRACKS]);
		assertEquals(0, child[HtmlStyles.ROW_GAP]);
		assertEquals(0, child[HtmlStyles.COLUMN_SPAN]);
	}

	@Test
	public void positionSticky() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet("h2 { position: sticky; top: 3px } th { position: -webkit-sticky }");
		int[] root = HtmlStyles.root(), h2 = styles.compute(root, "h2", null, null);
		assertEquals(HtmlStyles.STICKY, h2[HtmlStyles.POSITION]);
		assertEquals(3, h2[HtmlStyles.POS_TOP]);
		assertEquals(HtmlStyles.STICKY, styles.compute(root, "th", null, null)[HtmlStyles.POSITION]);
	}
}
