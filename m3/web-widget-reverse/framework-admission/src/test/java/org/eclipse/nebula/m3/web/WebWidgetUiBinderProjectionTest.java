// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.m3.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

final class WebWidgetUiBinderProjectionTest {
    private static final byte[] SOURCE = "class Orders { /* pinned Java source */ }"
            .getBytes(StandardCharsets.UTF_8);
    private static final String PATH = "src/main/java/demo/Orders.java";

    private static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static WebWidgetUiBinderProjection.SourceReceipt receipt() {
        return new WebWidgetUiBinderProjection.SourceReceipt(PATH, sha(SOURCE));
    }

    private static WebWidgetUiBinderProjection.Declaration label(String id, String text) {
        return new WebWidgetUiBinderProjection.Declaration(WebWidgetAdmission.Framework.GWT,
                "com.google.gwt.user.client.ui.Label", id, text, true, 0);
    }

    private static WebWidgetUiBinderProjection.Declaration button(String id, String text,
                                                                   boolean enabled) {
        return new WebWidgetUiBinderProjection.Declaration(WebWidgetAdmission.Framework.GWT,
                "com.google.gwt.user.client.ui.Button", id, text, enabled, 0);
    }

    private static WebWidgetUiBinderProjection.Projection project(
            WebWidgetUiBinderProjection.Declaration... rows) {
        return WebWidgetUiBinderProjection.project(receipt(), SOURCE, List.of(rows));
    }

    @Test void producesExactExistingUiBinderGrammarWithoutASecondModel() {
        var result = project(label("title", "Fish & chips <all>"), button("save", "Save", false));
        String xml = "<ui:UiBinder xmlns:ui=\"urn:ui:com.google.gwt.uibinder\""
                + " xmlns:g=\"urn:import:com.google.gwt.user.client.ui\"><g:VerticalPanel>"
                + "<g:Label ui:field=\"title\" text=\"Fish &amp; chips &lt;all&gt;\"/>"
                + "<g:Button ui:field=\"save\" text=\"Save\" enabled=\"false\"/>"
                + "</g:VerticalPanel></ui:UiBinder>";
        assertEquals(xml, result.uiBinderXml());
        assertEquals(sha(xml.getBytes(StandardCharsets.UTF_8)), result.xmlSha256());
        assertEquals(sha(SOURCE), result.sourceSha256());
        assertEquals(2, result.rowCount());
        assertTrue(result.runtimeQualificationRequired());
    }

    @Test void admitsOnlyExactPreviouslyClassifiedStaticSymbols() {
        Object[][] allowed = {
            {WebWidgetAdmission.Framework.RAP_RWT, "org.eclipse.swt.widgets.Label"},
            {WebWidgetAdmission.Framework.GWT, "com.google.gwt.user.client.ui.Label"},
            {WebWidgetAdmission.Framework.JWT, "eu.webtoolkit.jwt.WText"},
            {WebWidgetAdmission.Framework.APACHE_WICKET, "org.apache.wicket.markup.html.basic.Label"},
            {WebWidgetAdmission.Framework.SPRING_THYMELEAF, "html:label"},
            {WebWidgetAdmission.Framework.VAADIN_FLOW, "com.vaadin.flow.component.html.Span"}
        };
        for (Object[] row : allowed) {
            var declaration = new WebWidgetUiBinderProjection.Declaration(
                    (WebWidgetAdmission.Framework) row[0], (String) row[1], "title", "Static", true, 0);
            assertTrue(project(declaration).uiBinderXml().contains("<g:Label"));
        }
    }

    @Test void refusesUnknownSymbolsAndForeignDefaultSemantics() {
        Object[][] held = {
            {WebWidgetAdmission.Framework.GWT, "Button"},
            {WebWidgetAdmission.Framework.APACHE_WICKET, "org.apache.wicket.markup.html.form.Button"},
            {WebWidgetAdmission.Framework.APACHE_TAPESTRY,
                    "org.apache.tapestry5.corelib.components.Label"},
            {WebWidgetAdmission.Framework.RAP_RWT, "org.eclipse.swt.widgets.Table"},
            {WebWidgetAdmission.Framework.VAADIN_FLOW, "com.vaadin.flow.component.grid.Grid"},
            {WebWidgetAdmission.Framework.GWT, "com.google.gwt.user.cellview.client.CellTable"}
        };
        for (Object[] item : held) {
            var row = new WebWidgetUiBinderProjection.Declaration(
                    (WebWidgetAdmission.Framework) item[0], (String) item[1], "title", "Static", true, 0);
            assertThrows(IllegalArgumentException.class, () -> project(row));
        }
    }

    @Test void refusesEveryEffectMaskIncludingUnknownBits() {
        int[] effects = {WebWidgetAdmission.DATA_BINDING, WebWidgetAdmission.EVENT_CALLBACK,
                WebWidgetAdmission.BROWSER_API, WebWidgetAdmission.SESSION_STATE,
                WebWidgetAdmission.STYLE_OR_LAYOUT, WebWidgetAdmission.VIRTUAL_ITEMS,
                WebWidgetAdmission.ACCESSIBILITY, 1 << 30, -1};
        for (int mask : effects) {
            var dynamic = new WebWidgetUiBinderProjection.Declaration(WebWidgetAdmission.Framework.GWT,
                    "com.google.gwt.user.client.ui.Button", "save", "Save", true, mask);
            assertThrows(IllegalArgumentException.class, () -> project(dynamic));
        }
    }

    @Test void refusesSourceDriftInvalidUtf8AndResourceBudgets() {
        assertThrows(IllegalArgumentException.class, () -> WebWidgetUiBinderProjection.project(
                receipt(), "different".getBytes(StandardCharsets.UTF_8), List.of(label("x", "Hello"))));
        assertThrows(IllegalArgumentException.class, () -> WebWidgetUiBinderProjection.project(
                receipt(), new byte[] {(byte) 0xc3, (byte) 0x28}, List.of(label("x", "Hello"))));
        assertThrows(IllegalArgumentException.class, () -> WebWidgetUiBinderProjection.project(
                receipt(), new byte[0], List.of(label("x", "Hello"))));
        assertThrows(IllegalArgumentException.class, () -> WebWidgetUiBinderProjection.project(
                receipt(), new byte[1_048_577], List.of(label("x", "Hello"))));
    }

    @Test void validatesIdsAndRefusesDuplicateFields() {
        for (String bad : new String[] {"class", "_hidden", "", "x-y", "x y", "x".repeat(65)}) {
            assertThrows(IllegalArgumentException.class, () -> project(label(bad, "Text")));
        }
        assertThrows(IllegalArgumentException.class, () -> project(
                label("dup", "First"), button("dup", "Second", true)));
    }

    @Test void refusesUnrepresentableLiteralTextAndPreservesUnicode() {
        for (String bad : new String[] {"", " ", " leading", "trailing ", "double  space",
                "{dynamic}", "control\ntext", "\u0000", "\uD800", "\uDC00", "\uFFFE",
                "x".repeat(4097)}) {
            assertThrows(IllegalArgumentException.class, () -> project(label("x", bad)));
        }
        var good = project(label("poem", "Café 日本語 🚀"));
        assertTrue(good.uiBinderXml().contains("Café 日本語 🚀"));
        assertEquals(1, good.rowCount());
    }

    @Test void disabledLabelRefusesAndButtonFlagIsExplicit() {
        var bad = new WebWidgetUiBinderProjection.Declaration(WebWidgetAdmission.Framework.GWT,
                "com.google.gwt.user.client.ui.Label", "x", "Disabled", false, 0);
        assertThrows(IllegalArgumentException.class, () -> project(bad));
        assertTrue(project(button("save", "Save", true)).uiBinderXml().contains("enabled=\"true\""));
        assertTrue(project(button("save", "Save", false)).uiBinderXml().contains("enabled=\"false\""));
    }

    @Test void rejectsUntrustedSourceReceiptsAndZeroOrExcessiveRows() {
        for (String bad : new String[] {"/tmp/a.java", "../a.java", "src/../a.java",
                "src/./a.java", "src//a.java", "src\\a.java", "", "."}) {
            assertThrows(IllegalArgumentException.class, () ->
                    new WebWidgetUiBinderProjection.SourceReceipt(bad, sha(SOURCE)));
        }
        assertThrows(IllegalArgumentException.class, () ->
                new WebWidgetUiBinderProjection.SourceReceipt(PATH, "not-sha"));
        assertThrows(IllegalArgumentException.class, () -> WebWidgetUiBinderProjection.project(
                receipt(), SOURCE, List.of()));
        var rows = new ArrayList<WebWidgetUiBinderProjection.Declaration>();
        for (int i = 0; i < 257; i++) rows.add(label("item" + i, "Value" + i));
        assertThrows(IllegalArgumentException.class, () -> WebWidgetUiBinderProjection.project(
                receipt(), SOURCE, rows));
    }

    @Test void serialFixedPointAndConcurrentCandidateDeterminism() throws Exception {
        var rows = new ArrayList<WebWidgetUiBinderProjection.Declaration>();
        for (int i = 0; i < 256; i++) rows.add(label("item" + i, "Value" + i));
        var first = WebWidgetUiBinderProjection.project(receipt(), SOURCE, rows);
        assertEquals(first, WebWidgetUiBinderProjection.project(receipt(), SOURCE, rows));
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<java.util.concurrent.Callable<WebWidgetUiBinderProjection.Projection>>();
            for (int i = 0; i < 128; i++) tasks.add(() ->
                    WebWidgetUiBinderProjection.project(receipt(), SOURCE, rows));
            for (var future : executor.invokeAll(tasks)) assertEquals(first, future.get());
        }
    }
}
