/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.browserfallback.internal;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/** Headless contract tests; native Browser/RWT execution is a separate gate. */
public class BrowserScriptsTest {

    @Test
    public void acceptsOnlyBoundedKnownEvents() {
        assertTrue(BrowserScripts.validEvent(new Object[] { "click", "save", "" }));
        assertTrue(BrowserScripts.validEvent(new Object[] { "input", "name", "Ada" }));
        assertTrue(BrowserScripts.validEvent(new Object[] { "change", "yes", "true" }));
        assertFalse(BrowserScripts.validEvent(null));
        assertFalse(BrowserScripts.validEvent(new Object[] { "click", "x" }));
        assertFalse(BrowserScripts.validEvent(new Object[] { "focus", "x", "" }));
        assertFalse(BrowserScripts.validEvent(new Object[] { "click", "", "" }));
        assertFalse(BrowserScripts.validEvent(new Object[] { "click", "x", 1 }));
        assertFalse(BrowserScripts.validEvent(new Object[] { "click", "x",
                "a".repeat(BrowserScripts.MAX_EVENT_VALUE + 1) }));
    }

    @Test
    public void escapesQuotesControlsAndMarkupInsideJavaScriptLiterals() {
        String value = "</script>\n\r\t\\\"\\\\&\u2028\u2029";
        String encoded = BrowserScripts.jsString(value);
        assertTrue(encoded.startsWith("\""));
        assertTrue(encoded.endsWith("\""));
        assertFalse(encoded.contains("</script>"));
        assertFalse(encoded.contains("\n"));
        assertFalse(encoded.contains("\r"));
        assertTrue(encoded.contains("\\u003c"));
        assertTrue(encoded.contains("\\u003e"));
        assertTrue(encoded.contains("\\u0026"));
        assertTrue(encoded.contains("\\u2028"));
        assertTrue(encoded.contains("\\u2029"));
        assertTrue(encoded.contains("\\\""));
        assertTrue(encoded.contains("\\\\"));
    }

    @Test
    public void patchesByExactIdWithoutDynamicSelectorsOrHtmlInterpretation() {
        Map<String, String> data = Map.of("selector\"] [data-nebula-id=\"other", "<b>literal</b>");
        String script = BrowserScripts.patch(data, false);
        assertTrue(script.contains("Object.create(null)"));
        assertTrue(script.contains("querySelectorAll('[data-nebula-id]')"));
        assertTrue(script.contains("hasOwnProperty.call(patch,id)"));
        assertTrue(script.contains("node.textContent=patch[id]"));
        assertFalse(script.contains("innerHTML"));
        assertFalse(script.contains("<b>literal</b>"));
        assertFalse(script.contains("document.querySelector("));
    }

    @Test
    public void inputPatchDoesNotSynthesizeEvents() {
        String script = BrowserScripts.patch(Map.of("field", "updated"), true);
        assertTrue(script.contains("node.value=patch[id]"));
        assertFalse(script.contains("dispatchEvent"));
        assertFalse(script.contains("node.textContent"));
    }

    @Test
    public void refusesInvalidAndOversizedPatches() {
        assertRejected(() -> BrowserScripts.patch(null, false));
        assertRejected(
                () -> BrowserScripts.patch(Collections.singletonMap("", "x"), false));
        assertRejected(
                () -> BrowserScripts.patch(Collections.singletonMap("x", null), false));
        assertRejected(
                () -> BrowserScripts.patch(Collections.singletonMap("x",
                        "x".repeat(BrowserScripts.MAX_EVENT_VALUE + 1)), false));
        Map<String, String> tooMany = new HashMap<>();
        for (int i = 0; i <= BrowserScripts.MAX_PATCH_ITEMS; i++) {
            tooMany.put("id" + i, "text");
        }
        assertRejected(() -> BrowserScripts.patch(tooMany, false));
        Map<String, String> oversizedTransport = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            oversizedTransport.put("f" + i, "z".repeat(BrowserScripts.MAX_EVENT_VALUE));
        }
        assertRejected(
                () -> BrowserScripts.patch(oversizedTransport, false));
        Map<String, String> multibyte = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            multibyte.put("unicode" + i, "\u00e9".repeat(60000));
        }
        // UTF-16 script size fits; real UTF-8 payload exceeds 256 KiB.
        assertRejected(() -> BrowserScripts.patch(multibyte, false));
        assertEquals("(function(){return 0;})()", BrowserScripts.patch(Map.of(), false));
    }

    @Test
    public void bootstrapInstallsExactlyOneDelegatedHandlerPerEventType() {
        String js = BrowserScripts.install();
        assertTrue(js.contains("document.__m3NebulaInstalled"));
        // Per-document idempotency: replacing document while retaining window installs anew.
        assertFalse(js.contains("window.__m3NebulaInstalled"));
        assertTrue(js.contains("document.addEventListener('click',relay,true)"));
        assertTrue(js.contains("document.addEventListener('input',relay,true)"));
        assertTrue(js.contains("document.addEventListener('change',relay,true)"));
        assertTrue(js.contains("node.getAttribute('data-nebula-id')"));
    }
    @Test
    public void malformedSurrogatesFailClosedBeforeBrowserTransport() {
        String high = String.valueOf(Character.MIN_HIGH_SURROGATE);
        String low = String.valueOf(Character.MIN_LOW_SURROGATE);
        assertFalse(BrowserScripts.validEvent(new Object[] { "input", high, "ok" }));
        assertFalse(BrowserScripts.validEvent(new Object[] { "input", low, "ok" }));
        assertFalse(BrowserScripts.validEvent(new Object[] { "change", "id", high }));
        assertFalse(BrowserScripts.validEvent(new Object[] { "click", "id", low }));
        assertRejected(() -> BrowserScripts.jsString(high));
        assertRejected(() -> BrowserScripts.jsString(low));
        assertRejected(() -> BrowserScripts.jsString("a" + high + "b"));
        assertRejected(() -> BrowserScripts.jsString(low + high));
        assertRejected(() -> BrowserScripts.patch(Map.of(high, "x"), false));
        assertRejected(() -> BrowserScripts.patch(Map.of("x", low), true));
    }

    @Test
    public void validSupplementaryPairsAndOriginalLiteralSemanticsRemain() {
        String emoji = new String(Character.toChars(0x1f642));
        assertEquals("\"" + emoji + "\"", BrowserScripts.jsString(emoji));
        assertTrue(BrowserScripts.validEvent(new Object[] { "input", "key" + emoji, emoji }));
        String script = BrowserScripts.patch(Map.of("key" + emoji, emoji), false);
        assertTrue(script.contains(emoji));
        assertFalse(script.contains("innerHTML"));
    }

    @Test
    public void refusesDetachedSnapshotNodesAfterAncestorTextPatch() {
        // Java generates one bounded script; JS execution is tested separately
        // against detached-child and replaced-document-root fixtures; real Browser separate.
        String script = BrowserScripts.patch(Map.of(
                "parent", "replacement", "child", "detached"), false);
        assertTrue(script.contains("var root=document.documentElement;"));
        assertTrue(script.contains("if(!root||document.documentElement!==root){return n;}"));
        assertTrue(script.contains("node.textContent=patch[id];n++;"));
        String values = BrowserScripts.patch(Map.of("parent", "x"), true);
        assertTrue(values.contains("root.contains(node)"));
        assertTrue(values.contains("node.value=patch[id];n++;"));
    }

    @Test
    public void rootReplacementShortCircuitsTheRemainingSnapshot() {
        String script = BrowserScripts.patch(Map.of("parent", "x"), false);
        assertTrue(script.contains("if(!root||document.documentElement!==root){return n;}"));
        assertTrue(script.contains("if(!root.contains(node)){continue;}"));
        assertFalse(script.contains("document.documentElement!==root||!root.contains(node)"));
        assertFalse(script.contains("innerHTML"));
    }

    @Test
    public void rechecksConnectedNodeAfterIdLookupBeforeMutation() {
        // Custom element method or a patched DOM method can synchronously
        // detach or replace the root during getAttribute/contains. Read it
        // again before either textContent or value mutation.
        String expected = "if(Object.prototype.hasOwnProperty.call(patch,id)){"
                + "if(document.documentElement!==root){return n;}"
                + "if(!root.contains(node)){continue;}"
                + "if(document.documentElement!==root){return n;}";
        assertTrue(BrowserScripts.patch(Map.of("parent", "x"), false).contains(expected));
        assertTrue(BrowserScripts.patch(Map.of("parent", "x"), true).contains(expected));
    }

    private static void assertRejected(Runnable operation) {
        boolean rejected = false;
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue("Expected IllegalArgumentException", rejected);
    }
}
