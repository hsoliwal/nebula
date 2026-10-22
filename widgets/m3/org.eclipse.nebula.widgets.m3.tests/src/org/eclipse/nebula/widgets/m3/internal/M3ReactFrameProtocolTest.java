/*******************************************************************************
 * Copyright (c) 2026 Synexia and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.internal;

/** Dependency-free Java 11 protocol and hostile web-text regression entry. */
public final class M3ReactFrameProtocolTest {
    private static int checks;

    private M3ReactFrameProtocolTest() {
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("unexpected parameters");
        }
        compareFrame("{}", 1);
        compareFrame("{\"version\":1,\"rows\":[]}", 1);
        compareFrame("{\"text\":\"a\\nb\\t\\\"quoted\\\"\\\\slash\"}", 12);
        compareFrame("{\"text\":\"</script><script>alert(1)</script>\"}", 25);
        compareFrame("{\"text\":\"\\u2028 & \\u2029\"}", 47);
        compareFrame("{\"text\":\"𐐀 café λ\"}", 128);
        check(M3ReactFrameProtocol.quote("a\u0000b").contains("\\u0000"), "NUL escaped");
        check(M3ReactFrameProtocol.quote("<&>").contains("\\u003c\\u0026\\u003e"),
                "HTML delimiters escaped");
        check(M3ReactFrameProtocol.quote("\uD800").contains("\\ud800"),
                "isolated surrogate escaped");
        for (long rev : new long[] {1, 2, 10_000_000, M3ReactFrameProtocol.MAX_JS_INTEGER}) {
            M3ReactFrameProtocol.validateFrame(rev, "{}");
            checks++;
        }
        mustReject(() -> M3ReactFrameProtocol.validateFrame(0, "{}"));
        mustReject(() -> M3ReactFrameProtocol.validateFrame(
                M3ReactFrameProtocol.MAX_JS_INTEGER + 1, "{}"));
        mustReject(() -> M3ReactFrameProtocol.validateFrame(1, "[]"));
        mustReject(() -> M3ReactFrameProtocol.validateFrame(1, "{"));
        mustReject(() -> M3ReactFrameProtocol.validateFrame(1, "{" + " ".repeat(
                M3ReactFrameProtocol.MAX_FRAME_CHARS) + "}"));
        mustReject(() -> M3ReactFrameProtocol.validateFrame(1, null));

        check(M3ReactFrameProtocol.exactInteger(Double.valueOf(18), 100) == 18, "JS double");
        check(M3ReactFrameProtocol.exactInteger(Integer.valueOf(12), 100) == 12, "int");
        for (Object invalid : new Object[] {null, "1", Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.valueOf(-1), Double.valueOf(1.5), Double.valueOf(101)}) {
            check(M3ReactFrameProtocol.exactInteger(invalid, 100) < 0, "invalid JS index");
        }
        check(M3ReactFrameProtocol.allowedAction("ready"), "ready");
        check(M3ReactFrameProtocol.allowedAction("select"), "select");
        check(M3ReactFrameProtocol.allowedAction("activate"), "activate");
        check(M3ReactFrameProtocol.allowedAction("scroll"), "scroll");
        check(M3ReactFrameProtocol.allowedAction("focus"), "focus");
        check(!M3ReactFrameProtocol.allowedAction("eval"), "no arbitrary JS action");
        check(!M3ReactFrameProtocol.allowedAction(""), "no empty action");
        System.out.println("PASS M3ReactFrameProtocolTest assertions=" + checks);
    }

    private static void compareFrame(String json, long revision) {
        String script = M3ReactFrameProtocol.frameScript(revision, json);
        check(script.startsWith("if (typeof window.m3NebulaReceive === 'function') "),
                "fixed named receiver");
        check(script.contains("window.m3NebulaReceive(" + revision + ", \""),
                "generation argument");
        check(script.endsWith("\");"), "string literal closed");
        check(!script.contains("</script>"), "no HTML end tag injection");
        check(!script.contains("alert(1)</script>"), "hostile JavaScript inert");
    }

    private static void mustReject(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | NullPointerException expected) {
            checks++;
            return;
        }
        throw new AssertionError("expected fail-closed input");
    }

    private static void check(boolean ok, String description) {
        checks++;
        if (!ok) {
            throw new AssertionError(description);
        }
    }
}
