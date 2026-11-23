/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/** Own-element CSS binding for native controls, without replacing their form/value or parser owners. */
public class HtmlControlStylesTest {

	private static String control(String tag, String style) {
		String start = "<" + tag + " name=field style='" + style + "'>";
		return switch (tag) {
			case "input" -> start;
			case "select" -> start + "<option>one</option><option selected>two</option></select>";
			default -> start + "value</" + tag + ">";
		};
	}

	private static int ownBox(HtmlDocument doc, int block) {
		assertEquals(HtmlDocument.CONTROL, doc.kind[block]);
		int box = doc.blockBox[block];
		assertTrue("a native control must own its element box", box > 0);
		assertEquals("the control role is appended after the existing grid role", HtmlDocument.GRID_BOX + 1, doc.boxRole[box]);
		assertEquals(block, doc.boxFirstBlock[box]);
		assertEquals(block + 1, doc.boxEndBlock[box]);
		return box;
	}

	@Test
	public void eachNativeControlOwnsItsPositionSizeAndStackLevel() {
		for (String tag : new String[] { "input", "textarea", "select", "button" }) {
			HtmlDocument doc = HtmlDocument.parse(control(tag,
					"position:fixed;left:31px;top:17px;right:2px;bottom:3px;width:90px;height:25px;z-index:4"));
			assertEquals(tag, 1, doc.controlCount);
			assertEquals(tag, 1, doc.blockCount);
			int box = ownBox(doc, 0);
			assertEquals(0, doc.boxParent[box]);
			assertEquals(HtmlStyles.FIXED, doc.boxPosition[box]);
			assertEquals(31, doc.boxLeft[box]);
			assertEquals(17, doc.boxTop[box]);
			assertEquals(2, doc.boxRight[box]);
			assertEquals(3, doc.boxBottom[box]);
			assertEquals(90, doc.boxWidth[box]);
			assertEquals(25, doc.boxHeight[box]);
			assertEquals(4, doc.boxZIndex[box]);
			assertFalse(doc.boxZAuto[box]);
			assertTrue(doc.positioned);
			assertTrue(doc.boxLayout);
		}
	}

	@Test
	public void everyNonstaticControlUsesTheResolvedSceneWithoutAnExplicitSize() {
		String[] positions = { "relative", "absolute", "fixed", "sticky" };
		for (String tag : new String[] { "input", "textarea", "select", "button" }) {
			for (int i = 0; i < positions.length; i++) {
				HtmlDocument doc = HtmlDocument.parse(control(tag, "position:" + positions[i] + ";top:5px;left:7px"));
				int box = ownBox(doc, 0);
				assertEquals(i + 1, doc.boxPosition[box]);
				assertTrue(tag + " " + positions[i], doc.boxLayout);
				assertEquals(HtmlStyles.AUTO, doc.boxWidth[box]);
				assertEquals(HtmlStyles.AUTO, doc.boxHeight[box]);
			}
		}
	}

	@Test
	public void ordinaryStaticFormsKeepTheLazyPathAndNativeDefaults() {
		HtmlDocument doc = HtmlDocument.parse("<form action=/send><input name=text size=12><textarea rows=3 cols=7>value</textarea>"
				+ "<select><option>first</option></select><button>go</button></form>");
		assertEquals(4, doc.controlCount);
		assertEquals(4, doc.blockCount);
		assertEquals(5, doc.boxCount);
		assertFalse(doc.positioned);
		assertFalse(doc.boxLayout);
		for (int b = 0; b < 4; b++) {
			int box = ownBox(doc, b);
			assertEquals(0, doc.boxParent[box]);
			assertEquals(HtmlStyles.STATIC, doc.boxPosition[box]);
			assertEquals(HtmlStyles.AUTO, doc.boxWidth[box]);
			assertEquals(HtmlStyles.AUTO, doc.boxHeight[box]);
			assertTrue(doc.boxZAuto[box]);
			assertEquals(0, doc.controlForm[b]);
		}
		assertEquals(12, doc.controlCols[0]);
		assertEquals(3, doc.controlRows[1]);
		assertEquals(7, doc.controlCols[1]);
		assertEquals("/send", doc.formAction[0]);
	}

	@Test
	public void parentGeometryIsNotCopiedIntoAControlOwnBox() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;left:17px;width:240px;height:120px;"
				+ "padding:4px;background:red;z-index:7'><input name=field></div>");
		int box = ownBox(doc, 0), parent = doc.boxParent[box];
		assertEquals(1, parent);
		assertEquals(240, doc.boxWidth[parent]);
		assertEquals(120, doc.boxHeight[parent]);
		assertEquals(HtmlStyles.AUTO, doc.boxWidth[box]);
		assertEquals(HtmlStyles.AUTO, doc.boxHeight[box]);
		assertEquals(HtmlStyles.AUTO, doc.boxLeft[box]);
		assertEquals(HtmlStyles.STATIC, doc.boxPosition[box]);
		assertEquals(0, doc.boxPadLeft[box]);
		assertEquals(0, doc.boxBackground[box]);
		assertTrue(doc.boxZAuto[box]);
		assertTrue(doc.boxLayout);
	}

	@Test
	public void parentDecorationSurvivesWhenAnUnstyledControlGainsItsOwnBox() {
		HtmlDocument doc = HtmlDocument.parse("<div style='background:red;padding:4px;border:2px solid blue;margin:3px'>"
				+ "<input value=plain></div>");
		int box = ownBox(doc, 0), parent = doc.boxParent[box];
		assertEquals(0xFFFF0000, doc.boxBackground[parent]);
		assertEquals(4, doc.boxPadLeft[parent]);
		assertEquals(2, doc.boxBorder[parent]);
		assertEquals(0, doc.boxBackground[box]);
		assertEquals(0, doc.boxBorder[box]);
		assertTrue("the original parent decoration belongs to its container", doc.boxLayout);
	}

	@Test
	public void inputIsAVoidSiblingForSelectorsAndNeverAnAncestor() {
		HtmlDocument doc = HtmlDocument.parse("<style>form > input:first-child{width:41px} input + textarea{width:42px} "
				+ "input textarea{width:999px} form > button{height:11px}</style><form action=/go>"
				+ "<input name=a><textarea name=b>one</textarea><button name=c>go</button></form><p>after</p>");
		assertEquals(41, doc.boxWidth[ownBox(doc, 0)]);
		assertEquals(42, doc.boxWidth[ownBox(doc, 1)]);
		assertEquals(11, doc.boxHeight[ownBox(doc, 2)]);
		assertEquals(0, doc.boxParent[doc.blockBox[1]]);
		assertEquals(0, doc.controlForm[0]);
		assertEquals(0, doc.controlForm[1]);
		assertEquals(0, doc.controlForm[2]);
		assertEquals("after", doc.blockText(3));
	}

	@Test
	public void hiddenInputsStaySubmissionOnlyWithoutAStyleBox() {
		HtmlDocument doc = HtmlDocument.parse("<form><input type=HIDDEN name=secret value=x "
				+ "style='position:fixed;width:500px'><input name=visible value=y></form>");
		assertEquals(2, doc.controlCount);
		assertEquals(HtmlDocument.HIDDEN, doc.controlType[0]);
		assertEquals("secret", doc.controlName[0]);
		assertEquals("x", doc.controlValue[0]);
		assertEquals(0, doc.controlForm[0]);
		assertEquals(1, doc.blockCount);
		assertEquals(1, doc.blockControl[0]);
		assertEquals(2, doc.boxCount);
		ownBox(doc, 0);
		assertFalse(doc.positioned);
		assertFalse(doc.boxLayout);
	}

	@Test
	public void displayNoneControlsDoNotLeakFramesOrSuppressFollowingSiblings() {
		HtmlDocument doc = HtmlDocument.parse("<style>input + input{width:33px}</style>"
				+ "<input name=hidden style='display:none;position:fixed'><input name=shown>"
				+ "<textarea style='display:none'>hidden</textarea><select style='display:none'><option>hidden</option></select>"
				+ "<button style='display:none'>hidden</button><p>after</p>");
		assertEquals(1, doc.controlCount);
		assertEquals("shown", doc.controlName[0]);
		assertEquals(2, doc.blockCount);
		assertEquals(33, doc.boxWidth[ownBox(doc, 0)]);
		assertEquals("after", doc.blockText(1));
	}

	@Test
	public void invisibleNonvoidControlsPreserveTheSurroundingTextFragment() {
		for (String tag : new String[] { "textarea", "select", "button" }) {
			for (String declaration : new String[] { " style='display:none'", " class=hidden" }) {
				HtmlDocument doc = HtmlDocument.parse("<style>.hidden{display:none}</style><p>before<" + tag
						+ declaration + ">hidden</" + tag + ">after</p>");
				assertEquals(tag + declaration, 1, doc.blockCount);
				assertEquals("beforeafter", doc.blockText(0));
				assertEquals(0, doc.controlCount);
				assertEquals(2, doc.boxCount);
				assertEquals(1, doc.blockBox[0]);
				assertEquals(0, doc.boxFirstBlock[1]);
				assertEquals(1, doc.boxEndBlock[1]);
				assertFalse(doc.boxLayout);
			}
		}
	}

	@Test
	public void controlValuesRowsOptionsAndButtonLabelsRetainTheirOriginalLanes() {
		HtmlDocument doc = HtmlDocument.parse("<form action=/send><input type=checkbox checked name=check>"
				+ "<textarea rows=4 cols=9 name=message style='position:relative'>\none\r\ntwo</textarea>"
				+ "<select name=choice style='position:relative'><option>first</option><option selected>second</option></select>"
				+ "<button name=send value=go style='position:relative'> Send <b>now</b> </button></form>");
		assertEquals(4, doc.controlCount);
		assertEquals("on", doc.controlValue[0]);
		assertEquals(1, doc.controlChecked[0]);
		assertEquals("one\ntwo", doc.controlValue[1]);
		assertEquals(4, doc.controlRows[1]);
		assertEquals(9, doc.controlCols[1]);
		assertArrayEquals(new String[] { "first", "second" }, doc.controlOptions[2]);
		assertEquals(1, doc.controlChecked[2]);
		assertEquals("go", doc.controlValue[3]);
		assertEquals("Send now", doc.controlLabel[3]);
		for (int b = 0; b < 4; b++) {
			ownBox(doc, b);
			assertEquals(0, doc.controlForm[b]);
		}
	}

	@Test
	public void controlsInsideTableCellsRemainFlattenedAndDoNotActivatePositioning() {
		HtmlDocument doc = HtmlDocument.parse("<table><tr><td><input style='position:fixed;width:100px'>"
				+ "<textarea style='position:fixed'>cell text</textarea></td></tr></table>");
		assertEquals(0, doc.controlCount);
		assertEquals(1, doc.blockCount);
		assertEquals(HtmlDocument.CELL, doc.kind[0]);
		assertEquals("cell text", doc.blockText(0));
		assertEquals(4, doc.boxCount);
		assertFalse(doc.positioned);
		assertFalse(doc.boxLayout);
	}

	@Test
	public void theExistingDirectInputAndFlattenedTextareaInFlexArePreserved() {
		HtmlDocument input = HtmlDocument.parse("<div style='display:flex'><input name=field value=x></div>");
		assertEquals(1, input.controlCount);
		assertEquals(1, input.blockCount);
		assertEquals(HtmlDocument.CONTROL, input.kind[0]);
		assertEquals("x", input.controlValue[0]);
		assertFalse(input.boxLayout);
		HtmlDocument textarea = HtmlDocument.parse("<div style='display:flex'><textarea name=field>x</textarea></div>");
		assertEquals(0, textarea.controlCount);
		assertEquals(1, textarea.blockCount);
		assertEquals(HtmlDocument.CELL, textarea.kind[0]);
		assertEquals("x", textarea.blockText(0));
		assertFalse(textarea.boxLayout);
	}

	@Test
	public void adjacentAndSelfClosingInputsKeepDocumentOrderAndExactRanges() {
		HtmlDocument doc = HtmlDocument.parse("<p>before</p><input value=one/><input value=two><p>after</p>");
		assertEquals(4, doc.blockCount);
		assertEquals("before", doc.blockText(0));
		assertEquals("after", doc.blockText(3));
		int first = ownBox(doc, 1), second = ownBox(doc, 2);
		assertEquals(second, doc.boxNextSibling[first]);
		assertEquals(doc.blockBox[3], doc.boxNextSibling[second]);
		assertEquals(0, doc.boxParent[first]);
		assertEquals(0, doc.boxParent[second]);
		assertEquals("one", doc.controlValue[0]);
		assertEquals("two", doc.controlValue[1]);
	}

	@Test
	public void growingControlAndBoxLanesKeepIndependentDimensionsAndValues() {
		StringBuilder html = new StringBuilder("<form>");
		for (int i = 0; i < 40; i++) html.append("<input value=v").append(i).append(" style='width:").append(100 + i).append("px'>");
		HtmlDocument doc = HtmlDocument.parse(html.append("</form>").toString());
		assertEquals(40, doc.controlCount);
		assertEquals(40, doc.blockCount);
		assertEquals(41, doc.boxCount);
		for (int b = 0; b < 40; b++) {
			int box = ownBox(doc, b);
			assertEquals(100 + b, doc.boxWidth[box]);
			assertEquals(b, doc.blockControl[b]);
			assertEquals("v" + b, doc.controlValue[b]);
			assertEquals(0, doc.controlForm[b]);
		}
	}

	@Test
	public void malformedLaterControlDeclarationsKeepEarlierValidValuesAndAutoResets() {
		HtmlDocument doc = HtmlDocument.parse("<input style='left:8;left:9em;top:-7;top:999999;"
				+ "width:13px;width:-2px;height:4px;height:1%;z-index:2;z-index:1.5'>"
				+ "<input style='position:fixed;position:static;width:13px;width:auto;z-index:3;z-index:auto'>");
		int first = ownBox(doc, 0), second = ownBox(doc, 1);
		assertEquals(8, doc.boxLeft[first]);
		assertEquals(-7, doc.boxTop[first]);
		assertEquals(13, doc.boxWidth[first]);
		assertEquals(4, doc.boxHeight[first]);
		assertEquals(2, doc.boxZIndex[first]);
		assertFalse(doc.boxZAuto[first]);
		assertEquals(HtmlStyles.STATIC, doc.boxPosition[second]);
		assertEquals(HtmlStyles.AUTO, doc.boxWidth[second]);
		assertTrue(doc.boxZAuto[second]);
	}

	@Test
	public void controlStyleAndFrameWorkRetainsDetachedCancellation() {
		String html = "<form>" + control("input", "position:fixed;width:90px").repeat(80) + "</form>";
		AtomicInteger visits = new AtomicInteger();
		assertNull(HtmlDocument.parse(html, () -> visits.incrementAndGet() > 20));
		HtmlDocument doc = HtmlDocument.parse(html, () -> false);
		assertEquals(80, doc.controlCount);
		assertEquals(81, doc.boxCount);
		assertTrue(doc.boxLayout);
	}
}
