/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.internal;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Small, dependency-free scripts for the opt-in SWT/RWT Browser fallback.
 * Native SWT/Nebula controls are the preferred receiver of admitted UI
 * translations. The scripts never evaluate application-supplied JavaScript.
 */
public final class M3BrowserScripts {

    public static final int MAX_EVENT_ID = 256;
    public static final int MAX_EVENT_VALUE = 65536;
    public static final int MAX_PATCH_ITEMS = 1024;
    public static final int MAX_PATCH_SCRIPT_CHARS = 262144;
    public static final int MAX_PATCH_UTF8_BYTES = 262144;

    private M3BrowserScripts() {
    }

    /** Delegates ordinary DOM events; the BrowserFunction belongs to one Browser/session. */
    public static String install() {
        return "(function(){"
                + "if(document.__m3NebulaInstalled){return true;}"
                + "if(typeof window.m3NebulaEvent!=='function'){return false;}"
                + "var relay=function(event){var node=event.target;"
                + "while(node&&node.nodeType===1&&!node.hasAttribute('data-nebula-id')){node=node.parentElement;}"
                + "if(!node||node.nodeType!==1){return;}"
                + "var value='';"
                + "if(node.type==='checkbox'||node.type==='radio'){value=node.checked?'true':'false';}"
                + "else if(typeof node.value==='string'){value=node.value;}"
                + "window.m3NebulaEvent(event.type,node.getAttribute('data-nebula-id'),value);"
                + "};"
                + "document.addEventListener('click',relay,true);"
                + "document.addEventListener('input',relay,true);"
                + "document.addEventListener('change',relay,true);"
                + "document.__m3NebulaInstalled=true;return true;"
                + "})()";
    }

    /** Rejects malformed, oversize, or unsupported BrowserFunction messages. */
    public static boolean validEvent(Object[] arguments) {
        if (arguments == null || arguments.length != 3
                || !(arguments[0] instanceof String)
                || !(arguments[1] instanceof String)
                || !(arguments[2] instanceof String)) {
            return false;
        }
        String type = (String) arguments[0];
        String id = (String) arguments[1];
        String value = (String) arguments[2];
        return ("click".equals(type) || "input".equals(type) || "change".equals(type))
                && !id.isEmpty() && id.length() <= MAX_EVENT_ID
                && value.length() <= MAX_EVENT_VALUE
                && validUtf16(id) && validUtf16(value);
    }

    /**
     * One browser request for a batch, matched by exact data-nebula-id, never
     * by a caller-built selector. Returns the number of updated DOM elements.
     */
    public static String patch(Map<String, String> values, boolean inputValues) {
        if (values == null || values.size() > MAX_PATCH_ITEMS) {
            throw new IllegalArgumentException("Null or oversized widget patch");
        }
        if (values.isEmpty()) {
            return "(function(){return 0;})()";
        }
        StringBuilder out = new StringBuilder(128 + values.size() * 64);
        out.append("(function(){var patch=Object.create(null);");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String id = entry.getKey();
            String value = entry.getValue();
            if (id == null || id.isEmpty() || id.length() > MAX_EVENT_ID
                    || value == null || value.length() > MAX_EVENT_VALUE) {
                throw new IllegalArgumentException("Invalid widget id or value");
            }
            out.append("patch[").append(jsString(id)).append("]=")
                    .append(jsString(value)).append(';');
            if (out.length() > MAX_PATCH_SCRIPT_CHARS) {
                throw new IllegalArgumentException("Browser patch exceeds transport budget");
            }
        }
        // NodeList is a static snapshot. An earlier ancestor textContent patch
        // may detach descendants; once the document root changes, stop the entire scan.
        out.append("var root=document.documentElement;")
                .append("var nodes=document.querySelectorAll('[data-nebula-id]');")
                .append("var n=0;for(var i=0;i<nodes.length;i++){")
                .append("var node=nodes[i];if(!root||document.documentElement!==root){return n;}")
                .append("if(!root.contains(node)){continue;}")
                .append("var id=node.getAttribute('data-nebula-id');")
                .append("if(Object.prototype.hasOwnProperty.call(patch,id)){")
                .append("if(document.documentElement!==root){return n;}")
                .append("if(!root.contains(node)){continue;}")
                .append("if(document.documentElement!==root){return n;}");
        if (inputValues) {
            out.append("if('value' in node){node.value=patch[id];n++;}");
        } else {
            out.append("node.textContent=patch[id];n++;");
        }
        String script = out.append("}}return n;})()").toString();
        if (script.getBytes(StandardCharsets.UTF_8).length > MAX_PATCH_UTF8_BYTES) {
            throw new IllegalArgumentException("Browser patch exceeds UTF-8 transport budget");
        }
        return script;
    }

    /** Produces a single safe JavaScript string literal, not executable input. */
    public static String jsString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Null string");
        }
        StringBuilder out = new StringBuilder(text.length() + 2).append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\' || ch == '"') {
                out.append('\\').append(ch);
            } else if (ch < 0x20 || ch == 0x2028 || ch == 0x2029
                    || ch == '<' || ch == '>' || ch == '&') {
                out.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) {
                    out.append(Character.forDigit((ch >>> shift) & 0xf, 16));
                }
            } else if (Character.isHighSurrogate(ch)) {
                if (i + 1 >= text.length()
                        || !Character.isLowSurrogate(text.charAt(i + 1))) {
                    throw new IllegalArgumentException("Malformed UTF-16 string");
                }
                out.append(ch).append(text.charAt(++i));
            } else if (Character.isLowSurrogate(ch)) {
                throw new IllegalArgumentException("Malformed UTF-16 string");
            } else {
                out.append(ch);
            }
        }
        return out.append('"').toString();
    }

    /** Reject unpaired UTF-16 units without changing valid browser text. */
    private static boolean validUtf16(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                if (i + 1 >= text.length()
                        || !Character.isLowSurrogate(text.charAt(++i))) {
                    return false;
                }
            } else if (Character.isLowSurrogate(ch)) {
                return false;
            }
        }
        return true;
    }
}
