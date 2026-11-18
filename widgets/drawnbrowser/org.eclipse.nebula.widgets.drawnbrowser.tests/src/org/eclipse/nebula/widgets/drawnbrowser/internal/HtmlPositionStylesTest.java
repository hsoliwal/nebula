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

import org.junit.Test;

/** Supported positioning values, cascade resets and invalid-declaration preservation. */
public class HtmlPositionStylesTest {

	private static int[] computed(String declarations) {
		return new HtmlStyles().compute(HtmlStyles.root(), "div", null, declarations);
	}

	private static void assertAutoGeometry(int[] style) {
		assertEquals(HtmlStyles.AUTO, style[HtmlStyles.OFFSET_TOP]);
		assertEquals(HtmlStyles.AUTO, style[HtmlStyles.OFFSET_LEFT]);
		assertEquals(HtmlStyles.AUTO, style[HtmlStyles.OFFSET_RIGHT]);
		assertEquals(HtmlStyles.AUTO, style[HtmlStyles.OFFSET_BOTTOM]);
		assertEquals(HtmlStyles.AUTO, style[HtmlStyles.WIDTH]);
		assertEquals(HtmlStyles.AUTO, style[HtmlStyles.HEIGHT]);
	}

	@Test
	public void rootHasAutoGeometryAndDistinctAutoZ() {
		int[] root = HtmlStyles.root();
		assertEquals(HtmlStyles.SLOTS, root.length);
		assertEquals(HtmlStyles.STATIC, root[HtmlStyles.POSITION]);
		assertAutoGeometry(root);
		assertEquals(0, root[HtmlStyles.Z_INDEX]);
		assertEquals(1, root[HtmlStyles.Z_AUTO]);
	}

	@Test
	public void signedPixelsAndUnitlessZeroAreAccepted() {
		int[] style = computed("position: absolute; left: -12px; top: +8px; right: 0; bottom: -0; width: 25px; height: +0");
		assertEquals(HtmlStyles.ABSOLUTE, style[HtmlStyles.POSITION]);
		assertEquals(-12, style[HtmlStyles.OFFSET_LEFT]);
		assertEquals(8, style[HtmlStyles.OFFSET_TOP]);
		assertEquals(0, style[HtmlStyles.OFFSET_RIGHT]);
		assertEquals(0, style[HtmlStyles.OFFSET_BOTTOM]);
		assertEquals(25, style[HtmlStyles.WIDTH]);
		assertEquals(0, style[HtmlStyles.HEIGHT]);
		assertEquals(HtmlStyles.RELATIVE, computed("position:relative")[HtmlStyles.POSITION]);
		assertEquals(HtmlStyles.FIXED, computed("position:fixed")[HtmlStyles.POSITION]);
	}

	@Test
	public void invalidLaterValuesNeverEraseEarlierDeclarations() {
		int[] style = computed("position:fixed; position:sticky; left:7px; left:2em; top:8px; top:9em; right:9px; right:1.2px;"
				+ "bottom:10px; bottom:20%; width:11px; width:-1px; height:12px; height:2147483648px;"
				+ "z-index:-3; z-index:999999999999; color:red; color:bogus; background:blue; background:bogus");
		assertEquals(HtmlStyles.FIXED, style[HtmlStyles.POSITION]);
		assertEquals(7, style[HtmlStyles.OFFSET_LEFT]);
		assertEquals(8, style[HtmlStyles.OFFSET_TOP]);
		assertEquals(9, style[HtmlStyles.OFFSET_RIGHT]);
		assertEquals(10, style[HtmlStyles.OFFSET_BOTTOM]);
		assertEquals(11, style[HtmlStyles.WIDTH]);
		assertEquals(12, style[HtmlStyles.HEIGHT]);
		assertEquals(-3, style[HtmlStyles.Z_INDEX]);
		assertEquals(0, style[HtmlStyles.Z_AUTO]);
		assertEquals(0xFFFF0000, style[HtmlStyles.COLOR]);
		assertEquals(0xFF0000FF, style[HtmlStyles.BACKGROUND]);
	}

	@Test
	public void legacyUnitlessTopAndLeftOverrideEarlierLengthsWithinTheirExistingGrammar() {
		int[] style = computed("top:8px;top:9;left:6px;left:-7");
		assertEquals(9, style[HtmlStyles.POS_TOP]);
		assertEquals(-7, style[HtmlStyles.POS_LEFT]);
		style = computed("top:-99999;top:100000;top:+4;left:99999;left:-100000;left:+5");
		assertEquals(-99999, style[HtmlStyles.POS_TOP]);
		assertEquals(99999, style[HtmlStyles.POS_LEFT]);
		style = computed("top:00009;left:-00007");
		assertEquals(9, style[HtmlStyles.POS_TOP]);
		assertEquals(-7, style[HtmlStyles.POS_LEFT]);
	}

	@Test
	public void integerZIncludesEveryValueWhileGeometryReservesTheExistingAutoSentinel() {
		int[] style = computed("left:17px; left:-2147483647px; top:-2147483648px; right:2147483647px; z-index:-2147483648");
		assertEquals(17, style[HtmlStyles.OFFSET_LEFT]);
		assertEquals(Integer.MIN_VALUE, style[HtmlStyles.OFFSET_TOP]);
		assertEquals(Integer.MAX_VALUE, style[HtmlStyles.OFFSET_RIGHT]);
		assertEquals(Integer.MIN_VALUE, style[HtmlStyles.Z_INDEX]);
		assertEquals(0, style[HtmlStyles.Z_AUTO]);
		assertEquals(Integer.MAX_VALUE, computed("z-index:+2147483647")[HtmlStyles.Z_INDEX]);
		style = computed("z-index:-2147483647");
		assertEquals(Integer.MIN_VALUE + 1, style[HtmlStyles.Z_INDEX]);
		assertEquals(0, style[HtmlStyles.Z_AUTO]);
	}

	@Test
	public void explicitAutoAndStaticResetEarlierRules() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet("div { position:absolute; left:1px; top:2px; right:3px; bottom:4px; width:20px; height:30px; z-index:-9 }"
				+ ".reset { position:static; left:auto; top:auto; right:auto; bottom:auto; width:auto; height:auto; z-index:auto }");
		int[] reset = styles.compute(HtmlStyles.root(), "div", "reset", null);
		assertEquals(HtmlStyles.STATIC, reset[HtmlStyles.POSITION]);
		assertAutoGeometry(reset);
		assertEquals(1, reset[HtmlStyles.Z_AUTO]);
		int[] explicitZero = styles.compute(HtmlStyles.root(), "div", "reset", "position: relative; left:0; z-index:0");
		assertEquals(HtmlStyles.RELATIVE, explicitZero[HtmlStyles.POSITION]);
		assertEquals(0, explicitZero[HtmlStyles.OFFSET_LEFT]);
		assertEquals(0, explicitZero[HtmlStyles.Z_INDEX]);
		assertEquals(0, explicitZero[HtmlStyles.Z_AUTO]);
	}

	@Test
	public void invalidHigherSpecificityAndInlineLeaveValidLowerRule() {
		HtmlStyles styles = new HtmlStyles();
		styles.addSheet("div {position:relative; left:4px; width:20px; z-index:2} .bad {position:sticky; left:4%; width:-1px; z-index:1.5}");
		int[] style = styles.compute(HtmlStyles.root(), "div", "bad", "left:bogus; width:1em; z-index:999999999999");
		assertEquals(HtmlStyles.RELATIVE, style[HtmlStyles.POSITION]);
		assertEquals(4, style[HtmlStyles.OFFSET_LEFT]);
		assertEquals(20, style[HtmlStyles.WIDTH]);
		assertEquals(2, style[HtmlStyles.Z_INDEX]);
	}

	@Test
	public void geometryAndZDoNotInherit() {
		int[] parent = computed("position:fixed; left:1px; top:2px; right:3px; bottom:4px; width:5px; height:6px; z-index:7; color:red");
		int[] child = new HtmlStyles().compute(parent, "p", null, null);
		assertEquals(HtmlStyles.STATIC, child[HtmlStyles.POSITION]);
		assertAutoGeometry(child);
		assertEquals(0, child[HtmlStyles.Z_INDEX]);
		assertEquals(1, child[HtmlStyles.Z_AUTO]);
		assertEquals("text still inherits", 0xFFFF0000, child[HtmlStyles.COLOR]);
	}

	@Test
	public void existingBoxSlotsRemainStableBesidePositioning() {
		assertEquals(5, HtmlStyles.MARGIN_TOP);
		assertEquals(6, HtmlStyles.MARGIN_RIGHT);
		assertEquals(7, HtmlStyles.MARGIN_BOTTOM);
		assertEquals(8, HtmlStyles.PADDING_TOP);
		assertEquals(9, HtmlStyles.PADDING_RIGHT);
		assertEquals(10, HtmlStyles.PADDING_BOTTOM);
		assertEquals(11, HtmlStyles.PADDING_LEFT);
		assertEquals(12, HtmlStyles.BORDER_WIDTH);
		assertEquals(13, HtmlStyles.BORDER_COLOR);
		assertEquals(14, HtmlStyles.POSITION);
		assertEquals(15, HtmlStyles.POS_TOP);
		assertEquals(16, HtmlStyles.POS_LEFT);
		assertEquals(17, HtmlStyles.Z_INDEX);
		assertEquals(HtmlStyles.POS_TOP, HtmlStyles.OFFSET_TOP);
		assertEquals(HtmlStyles.POS_LEFT, HtmlStyles.OFFSET_LEFT);
		assertEquals(Integer.MIN_VALUE + 1, HtmlStyles.AUTO);
		int[] style = computed("margin:1px 2px 3px 4px;padding:5px 6px 7px 8px;border:9px solid red;"
				+ "position:relative;left:-10px;top:11px;right:12px;bottom:13px;width:14px;height:15px;z-index:-16");
		for (int slot : new int[] { HtmlStyles.MARGIN_TOP, HtmlStyles.MARGIN_RIGHT, HtmlStyles.MARGIN_BOTTOM,
				HtmlStyles.PADDING_TOP, HtmlStyles.PADDING_RIGHT, HtmlStyles.PADDING_BOTTOM, HtmlStyles.PADDING_LEFT }) {
			assertEquals(slot - 4 + (slot >= HtmlStyles.PADDING_TOP ? 1 : 0), style[slot]);
		}
		assertEquals(4, style[HtmlStyles.INDENT]);
		assertEquals(9, style[HtmlStyles.BORDER_WIDTH]);
		assertEquals(0xFFFF0000, style[HtmlStyles.BORDER_COLOR]);
		assertEquals(HtmlStyles.RELATIVE, style[HtmlStyles.POSITION]);
		assertEquals(-10, style[HtmlStyles.OFFSET_LEFT]);
		assertEquals(11, style[HtmlStyles.OFFSET_TOP]);
		assertEquals(12, style[HtmlStyles.OFFSET_RIGHT]);
		assertEquals(13, style[HtmlStyles.OFFSET_BOTTOM]);
		assertEquals(14, style[HtmlStyles.WIDTH]);
		assertEquals(15, style[HtmlStyles.HEIGHT]);
		assertEquals(-16, style[HtmlStyles.Z_INDEX]);
		assertEquals(0, style[HtmlStyles.Z_AUTO]);
		int[] child = new HtmlStyles().compute(style, "span", null, null);
		for (int slot = HtmlStyles.MARGIN_TOP; slot <= HtmlStyles.BORDER_COLOR; slot++) assertEquals(0, child[slot]);
	}
}
