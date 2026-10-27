// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.m3.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

final class WebWidgetAdmissionTest {
    @Test void exactStaticShapesRemainCandidates() {
        Object[][] cases = {
            {WebWidgetAdmission.Framework.RAP_RWT, "org.eclipse.swt.widgets.Label", "g:Label"},
            {WebWidgetAdmission.Framework.RAP_RWT, "org.eclipse.swt.widgets.Button", "g:Button"},
            {WebWidgetAdmission.Framework.GWT, "com.google.gwt.user.client.ui.Label", "g:Label"},
            {WebWidgetAdmission.Framework.GWT, "com.google.gwt.user.client.ui.Button", "g:Button"},
            {WebWidgetAdmission.Framework.JWT, "eu.webtoolkit.jwt.WText", "g:Label"},
            {WebWidgetAdmission.Framework.JWT, "eu.webtoolkit.jwt.WPushButton", "g:Button"},
            {WebWidgetAdmission.Framework.APACHE_WICKET, "org.apache.wicket.markup.html.basic.Label", "g:Label"},
            {WebWidgetAdmission.Framework.SPRING_THYMELEAF, "html:label", "g:Label"},
            {WebWidgetAdmission.Framework.SPRING_THYMELEAF, "html:button", "g:Button"},
            {WebWidgetAdmission.Framework.VAADIN_FLOW, "com.vaadin.flow.component.html.Span", "g:Label"},
            {WebWidgetAdmission.Framework.VAADIN_FLOW, "com.vaadin.flow.component.button.Button", "g:Button"}
        };
        for (Object[] sample : cases) {
            var decision = WebWidgetAdmission.classify((WebWidgetAdmission.Framework) sample[0],
                    (String) sample[1], 0);
            assertEquals(WebWidgetAdmission.Status.STATIC_SHAPE_CANDIDATE, decision.status());
            assertEquals(sample[2], decision.uiBinderElement());
            assertNotNull(decision.swtReceiver());
            assertTrue(decision.needsQualification());
        }
    }

    @Test void neverMapsCollectionSemanticsAsChildControls() {
        Object[][] cases = {
            {WebWidgetAdmission.Framework.RAP_RWT, "org.eclipse.swt.widgets.Table"},
            {WebWidgetAdmission.Framework.GWT, "com.google.gwt.user.cellview.client.CellTable"},
            {WebWidgetAdmission.Framework.JWT, "eu.webtoolkit.jwt.WTable"},
            {WebWidgetAdmission.Framework.APACHE_WICKET, "org.apache.wicket.markup.html.list.ListView"},
            {WebWidgetAdmission.Framework.APACHE_TAPESTRY, "org.apache.tapestry5.corelib.components.Loop"},
            {WebWidgetAdmission.Framework.VAADIN_FLOW, "com.vaadin.flow.component.grid.Grid"}
        };
        for (Object[] sample : cases) {
            var decision = WebWidgetAdmission.classify((WebWidgetAdmission.Framework) sample[0],
                    (String) sample[1], 0);
            assertEquals(WebWidgetAdmission.Status.HOLD_INTRINSIC_VIEWPORT, decision.status());
            assertTrue(decision.uiBinderElement().isEmpty());
            assertTrue(decision.needsQualification());
        }
    }

    @Test void everyEffectAndUnknownBitFailsClosed() {
        int[] masks = {WebWidgetAdmission.DATA_BINDING, WebWidgetAdmission.EVENT_CALLBACK,
                WebWidgetAdmission.BROWSER_API, WebWidgetAdmission.SESSION_STATE,
                WebWidgetAdmission.STYLE_OR_LAYOUT, WebWidgetAdmission.VIRTUAL_ITEMS,
                WebWidgetAdmission.ACCESSIBILITY, 1 << 29, -1};
        for (int mask : masks) {
            var decision = WebWidgetAdmission.classify(WebWidgetAdmission.Framework.GWT,
                    "com.google.gwt.user.client.ui.Button", mask);
            assertEquals(WebWidgetAdmission.Status.HOLD_RUNTIME_EFFECTS, decision.status());
            assertTrue(decision.needsQualification());
        }
    }

    @Test void intrinsicFormSemanticsCannotBeErasedByAnEmptyEffectMask() {
        Object[][] cases = {
            {WebWidgetAdmission.Framework.APACHE_WICKET, "org.apache.wicket.markup.html.form.Button"},
            {WebWidgetAdmission.Framework.APACHE_TAPESTRY, "org.apache.tapestry5.corelib.components.Label"},
            {WebWidgetAdmission.Framework.APACHE_TAPESTRY, "org.apache.tapestry5.corelib.components.Submit"}
        };
        for (Object[] sample : cases) {
            var decision = WebWidgetAdmission.classify((WebWidgetAdmission.Framework) sample[0],
                    (String) sample[1], 0);
            assertEquals(WebWidgetAdmission.Status.HOLD_REQUIRED_SEMANTICS, decision.status());
            assertTrue(decision.uiBinderElement().isEmpty());
        }
    }

    @Test void sourceIdentityIsExactAndUnmappedDoesNotGuess() {
        String[] untrusted = {"Button", "button", "com.google.gwt.user.client.ui.button",
                "org.eclipse.swt.widgets.Button ", "<button>", "g:Button", "th:each",
                "org.eclipse.swt.widgets.Table", "com.vaadin.flow.component.grid.GridView"};
        for (String type : untrusted) {
            var decision = WebWidgetAdmission.classify(WebWidgetAdmission.Framework.GWT, type, 0);
            assertEquals(WebWidgetAdmission.Status.HOLD_UNSUPPORTED_SOURCE, decision.status());
        }
    }

    @Test void rejectsMalformedArguments() {
        assertThrows(NullPointerException.class,
                () -> WebWidgetAdmission.classify(null, "x", 0));
        assertThrows(NullPointerException.class,
                () -> WebWidgetAdmission.classify(WebWidgetAdmission.Framework.GWT, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> WebWidgetAdmission.classify(WebWidgetAdmission.Framework.GWT, " ", 0));
        assertThrows(IllegalArgumentException.class,
                () -> WebWidgetAdmission.classify(WebWidgetAdmission.Framework.GWT, "x".repeat(257), 0));
    }

    @Test void deterministicAcrossThreadsAndInputOrders() throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = IntStream.range(0, 512).<java.util.concurrent.Callable<WebWidgetAdmission.Decision>>mapToObj(i ->
                    () -> WebWidgetAdmission.classify(WebWidgetAdmission.Framework.VAADIN_FLOW,
                            "com.vaadin.flow.component.button.Button", (i & 1) == 0 ? 0 : 1)).toList();
            var results = pool.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) {
                assertEquals((i & 1) == 0
                        ? WebWidgetAdmission.Status.STATIC_SHAPE_CANDIDATE
                        : WebWidgetAdmission.Status.HOLD_RUNTIME_EFFECTS, results.get(i).get().status());
            }
        }
    }
}
