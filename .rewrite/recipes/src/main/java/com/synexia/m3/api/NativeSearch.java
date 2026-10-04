// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** Optional JNI edit-distance acceleration with Java as the exact correctness oracle. */
public final class NativeSearch {
    private static final int MAX_BATCH = 100_000;
    private static final int MAX_NORMALIZED_CHARS = 4_096;
    private static final long MAX_TOTAL_NORMALIZED_CHARS = 4_194_304L;
    private static final long MAX_EDIT_CELLS = 100_000_000L;
    private static volatile boolean loadAttempted;
    private static volatile boolean loaded;

    public enum Backend { JAVA, JNI_PARITY_VERIFIED }

    public record VerifiedBatch(int[] distances, Backend backend, String evidenceSha256) {
        public VerifiedBatch {
            distances = Objects.requireNonNull(distances, "distances").clone();
            backend = Objects.requireNonNull(backend, "backend");
            if (evidenceSha256 == null || !evidenceSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("evidenceSha256");
            }
        }
        @Override public int[] distances() { return distances.clone(); }
    }

    private NativeSearch() {}

    public static boolean isAvailable() { ensureLoaded(); return loaded; }

    public static int[] levenshteinBatch(String query, String[] candidates) {
        return verifiedLevenshteinBatch(query, candidates).distances();
    }

    public static VerifiedBatch verifiedLevenshteinBatch(String query, String[] candidates) {
        String q = FuzzyScorer.normalize(query);
        String[] normalized = normalizeCandidates(candidates);
        validateBounds(q, normalized);
        int[] oracle = javaBatch(q, normalized);
        ensureLoaded();
        if (!loaded) return new VerifiedBatch(oracle, Backend.JAVA, evidence(q, normalized, oracle));

        int[] nativeResult = nativeLevenshteinBatch(q, normalized);
        if (nativeResult == null || nativeResult.length != oracle.length
                || !Arrays.equals(nativeResult, oracle)) {
            throw new IllegalStateException("M3 JNI search parity mismatch; native result rejected");
        }
        return new VerifiedBatch(nativeResult, Backend.JNI_PARITY_VERIFIED,
                evidence(q, normalized, nativeResult));
    }

    private static String[] normalizeCandidates(String[] candidates) {
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.length > MAX_BATCH) {
            throw new IllegalArgumentException("candidate batch exceeds " + MAX_BATCH);
        }
        String[] normalized = new String[candidates.length];
        for (int i = 0; i < candidates.length; i++) normalized[i] = FuzzyScorer.normalize(candidates[i]);
        return normalized;
    }

    private static void validateBounds(String query, String[] candidates) {
        if (query.length() > MAX_NORMALIZED_CHARS) throw new IllegalArgumentException("query too large");
        long total = query.length();
        long cells = 0L;
        for (String candidate : candidates) {
            if (candidate.length() > MAX_NORMALIZED_CHARS) {
                throw new IllegalArgumentException("candidate too large");
            }
            total = Math.addExact(total, candidate.length());
            cells = Math.addExact(cells,
                    Math.multiplyExact((long) query.length(), (long) candidate.length()));
            if (total > MAX_TOTAL_NORMALIZED_CHARS) throw new IllegalArgumentException("batch too large");
            if (cells > MAX_EDIT_CELLS) throw new IllegalArgumentException("edit work budget exceeded");
        }
    }

    private static int[] javaBatch(String query, String[] candidates) {
        int[] result = new int[candidates.length];
        for (int i = 0; i < candidates.length; i++) result[i] = FuzzyScorer.distance(query, candidates[i]);
        return result;
    }

    private static String evidence(String query, String[] candidates, int[] distances) {
        MessageDigest digest = digest();
        frame(digest, "SYNEXIA_M3_NATIVE_SEARCH_PARITY_V1");
        frame(digest, query);
        for (int i = 0; i < candidates.length; i++) {
            frame(digest, candidates[i]);
            frame(digest, Integer.toString(distances[i]));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new ExceptionInInitializerError(impossible); }
    }

    private static void ensureLoaded() {
        if (loadAttempted) return;
        synchronized (NativeSearch.class) {
            if (loadAttempted) return;
            try { System.loadLibrary("synexia_m3_search"); loaded = true; }
            catch (UnsatisfiedLinkError | SecurityException unavailable) { loaded = false; }
            finally { loadAttempted = true; }
        }
    }

    private static native int[] nativeLevenshteinBatch(String query, String[] candidates);
}
