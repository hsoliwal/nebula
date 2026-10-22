/*******************************************************************************
 * Copyright (c) 2026 Synexia and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.internal;

import java.util.Objects;

/**
 * Bounded, presentation-only transport for one CPU-owned viewport frame.
 *
 * <p>This class does not own data, sorting, a scroll model, selection or SWT
 * resources. The receiver must interpret each frame as a disposable projection.
 */
public final class M3ReactFrameProtocol {
    public static final int MAX_FRAME_CHARS = 262_144;
    public static final long MAX_JS_INTEGER = 9_007_199_254_740_991L;

    private M3ReactFrameProtocol() {
    }

    public static void validateFrame(long revision, String json) {
        if (revision < 1 || revision > MAX_JS_INTEGER) {
            throw new IllegalArgumentException("viewport revision");
        }
        Objects.requireNonNull(json, "json");
        if (json.length() < 2 || json.length() > MAX_FRAME_CHARS
                || json.charAt(0) != '{' || json.charAt(json.length() - 1) != '}') {
            throw new IllegalArgumentException("bounded JSON frame object required");
        }
    }

    /** Never treat a source JSON frame as executable JavaScript source. */
    public static String frameScript(long revision, String json) {
        validateFrame(revision, json);
        return "if (typeof window.m3NebulaReceive === 'function') "
                + "window.m3NebulaReceive(" + revision + ", " + quote(json) + ");";
    }

    /**
     * JS string literal escaping, including script delimiters, control codes
     * and isolated UTF-16 surrogates. The renderer parses JSON separately.
     */
    public static String quote(String raw) {
        Objects.requireNonNull(raw, "raw");
        StringBuilder result = new StringBuilder(raw.length() + 16).append('"');
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '"' || ch == '\\') {
                result.append('\\').append(ch);
            } else if (ch < 0x20 || ch == 0x7f || ch == '<' || ch == '>'
                    || ch == '&' || ch == 0x2028 || ch == 0x2029
                    || Character.isSurrogate(ch)) {
                result.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) {
                    result.append(Character.forDigit((ch >>> shift) & 15, 16));
                }
            } else {
                result.append(ch);
            }
        }
        return result.append('"').toString();
    }

    public static boolean allowedAction(String value) {
        return "ready".equals(value) || "select".equals(value)
                || "activate".equals(value) || "scroll".equals(value)
                || "focus".equals(value);
    }

    /** SWT BrowserFunction supplies JS numbers as boxed Java Number values. */
    public static long exactInteger(Object value, long maximum) {
        if (!(value instanceof Number)) {
            return -1L;
        }
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number) || number < 0 || number > maximum
                || Math.rint(number) != number) {
            return -1L;
        }
        return (long) number;
    }
}
