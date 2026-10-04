/* Copyright 2026 Synexia. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.algorithms.corpus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Length-framed UTF-16 code-unit metadata receipt. Preserves every Java String exactly, including
 * unpaired surrogates. Not semantic equivalence or external-source authentication. */
final class CatalogueDigest {
    private final MessageDigest digest;

    CatalogueDigest(String domain) {
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        text(domain);
    }

    CatalogueDigest number(int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
        return this;
    }

    CatalogueDigest text(String value) {
        number(value.length());
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
        return this;
    }

    String finish() { return HexFormat.of().formatHex(digest.digest()); }
}
