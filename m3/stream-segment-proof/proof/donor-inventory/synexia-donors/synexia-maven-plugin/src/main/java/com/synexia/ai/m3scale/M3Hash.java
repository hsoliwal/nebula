// SPDX-License-Identifier: Apache-2.0
package com.synexia.ai.m3scale;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Domain-separated canonical SHA-256 and deterministic SimHash helpers. */
public final class M3Hash {
    private M3Hash() {}

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String canonical(String domain, List<String> values) {
        StringBuilder out = new StringBuilder();
        append(out, domain);
        for (String value : values) {
            append(out, value);
        }
        return sha256(out.toString());
    }

    public static void append(StringBuilder out, String value) {
        String safe = value == null ? "" : value;
        out.append(safe.length()).append(':').append(safe).append(';');
    }

    public static long simHash(List<String> features) {
        long[] weights = new long[64];
        java.util.TreeMap<String, Integer> counts = new java.util.TreeMap<>();
        for (String feature : features) {
            counts.merge(feature, 1, Integer::sum);
        }
        counts.forEach((feature, count) -> {
            byte[] digest;
            try {
                digest = MessageDigest.getInstance("SHA-256").digest(feature.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException(exception);
            }
            long bits = java.nio.ByteBuffer.wrap(digest, 0, 8).getLong();
            int weight = Math.min(64, count);
            for (int bit = 0; bit < 64; bit++) {
                weights[bit] += ((bits >>> bit) & 1L) == 1L ? weight : -weight;
            }
        });
        long result = 0L;
        for (int bit = 0; bit < 64; bit++) {
            if (weights[bit] >= 0L) {
                result |= (1L << bit);
            }
        }
        return result;
    }

    public static int hamming(long left, long right) {
        return Long.bitCount(left ^ right);
    }
}
