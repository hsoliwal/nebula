// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.contract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable M3 admission invariant: execution context, external interface, and environment
 * remain locked while file-local behavior atoms are replaced serially.
 */
public record M3Invariant(
        Scope scope,
        String contextRoot,
        String interfaceRoot,
        String environmentRoot,
        String root) {

    public enum Scope { FILE, PACKAGE, MODULE, REACTOR, AST_LEAF }

    public M3Invariant {
        scope = Objects.requireNonNull(scope, "scope");
        contextRoot = sha(contextRoot, "contextRoot");
        interfaceRoot = sha(interfaceRoot, "interfaceRoot");
        environmentRoot = sha(environmentRoot, "environmentRoot");
        String expected = digest(
                "SYNEXIA-M3-INVARIANT/1",
                scope.name(), contextRoot, interfaceRoot, environmentRoot);
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) {
            throw new IllegalArgumentException("M3_INVARIANT_ROOT_MISMATCH");
        }
    }

    public static M3Invariant of(
            Scope scope, String contextRoot, String interfaceRoot, String environmentRoot) {
        return new M3Invariant(scope, contextRoot, interfaceRoot, environmentRoot, "");
    }

    /** Exact admission: source behavior may change, but these three locks may not. */
    public boolean compatibleWith(M3Invariant next) {
        Objects.requireNonNull(next, "next");
        return scope == next.scope
                && contextRoot.equals(next.contextRoot)
                && interfaceRoot.equals(next.interfaceRoot)
                && environmentRoot.equals(next.environmentRoot);
    }

    public boolean sameExecutionContext(M3Invariant next) {
        Objects.requireNonNull(next, "next");
        return contextRoot.equals(next.contextRoot)
                && environmentRoot.equals(next.environmentRoot);
    }

    public boolean sameInterface(M3Invariant next) {
        Objects.requireNonNull(next, "next");
        return interfaceRoot.equals(next.interfaceRoot);
    }

    private static String sha(String value, String field) {
        Objects.requireNonNull(value, field);
        String checked = value.toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.requireNonNull(value, "value")
                        .getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length)
                        .getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) ':');
                digest.update(bytes);
                digest.update((byte) '\n');
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
