// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Frozen deterministic snapshot of the OpenRewrite recipes visible on one runtime classpath.
 *
 * <p>Capture performs classpath discovery once. Subsequent lookups are binary searches over the
 * sorted immutable entry list and the SHA-256 fingerprint can key caches/evidence receipts.</p>
 */
public final class M3OpenRewriteInventorySnapshot {
    private final List<M3OpenRewriteInventory.Entry> entries;
    private final String sha256;

    private M3OpenRewriteInventorySnapshot(
            List<M3OpenRewriteInventory.Entry> entries, String sha256) {
        this.entries = entries;
        this.sha256 = sha256;
    }

    public static M3OpenRewriteInventorySnapshot capture(String... acceptedPackages) {
        List<M3OpenRewriteInventory.Entry> entries =
                M3OpenRewriteInventory.discover(acceptedPackages);
        String tsv = M3OpenRewriteInventory.toTsv(entries);
        return new M3OpenRewriteInventorySnapshot(entries, sha256(tsv));
    }

    public int size() {
        return entries.size();
    }

    public String sha256() {
        return sha256;
    }

    public List<M3OpenRewriteInventory.Entry> entries() {
        return entries;
    }

    public boolean contains(String recipeName) {
        return indexOf(recipeName) >= 0;
    }

    public M3OpenRewriteInventory.Entry require(String recipeName) {
        int index = indexOf(recipeName);
        if (index < 0) {
            throw new IllegalArgumentException("recipe not found: " + recipeName);
        }
        return entries.get(index);
    }

    public String toTsv() {
        return M3OpenRewriteInventory.toTsv(entries);
    }

    private int indexOf(String recipeName) {
        String checked = text(recipeName);
        int low = 0;
        int high = entries.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int compared = entries.get(mid).name().compareTo(checked);
            if (compared < 0) {
                low = mid + 1;
            } else if (compared > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    private static String text(String value) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("recipeName required");
        }
        return value.strip();
    }

    private static String sha256(String value) {
        Objects.requireNonNull(value, "value");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
