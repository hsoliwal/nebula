// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Strict persisted codec for exact post-promotion canonical target Git-blob evidence. */
public final class M3CanonicalTreeReadbackTsv {
    public static final String HEADER =
            "canonical_branch\tpromotion_commit_sha\tcanonical_head_sha\tpath"
                    + "\texpected_git_blob\tactual_git_blob";

    private M3CanonicalTreeReadbackTsv() {
        throw new AssertionError("No instances");
    }

    public static M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence parse(String text) {
        String source = Objects.requireNonNull(text, "text");
        if (source.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("canonical readback must use LF");
        }
        String[] raw = source.split("\n", -1);
        int count = raw.length;
        if (count > 0 && raw[count - 1].isEmpty()) count--;
        if (count < 2 || !HEADER.equals(raw[0])) {
            throw new IllegalArgumentException("canonical readback header/rows");
        }

        String branch = null;
        String promotion = null;
        String head = null;
        String previousPath = null;
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        LinkedHashMap<String, String> actual = new LinkedHashMap<>();

        for (int line = 1; line < count; line++) {
            if (raw[line].isEmpty()) {
                throw new IllegalArgumentException("blank canonical readback row");
            }
            String[] fields = raw[line].split("\t", -1);
            if (fields.length != 6) {
                throw new IllegalArgumentException("canonical readback column count");
            }
            if (branch == null) {
                branch = fields[0];
                promotion = fields[1];
                head = fields[2];
            } else if (!branch.equals(fields[0])
                    || !promotion.equals(fields[1])
                    || !head.equals(fields[2])) {
                throw new IllegalArgumentException("canonical readback identity drift");
            }

            String path = fields[3];
            if (previousPath != null && previousPath.compareTo(path) >= 0) {
                throw new IllegalArgumentException(
                        "canonical readback paths must be strictly sorted");
            }
            previousPath = path;
            if (expected.putIfAbsent(path, fields[4]) != null
                    || actual.putIfAbsent(path, fields[5]) != null) {
                throw new IllegalArgumentException("duplicate canonical readback path");
            }
        }

        M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence evidence =
                new M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence(
                        branch, promotion, head, expected, actual);
        M3RecipeFirstInvariant.requirePostMergeCanonicalTreeReadback(evidence);
        return evidence;
    }

    public static String encode(
            M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence evidence) {
        M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence checked =
                Objects.requireNonNull(evidence, "evidence");
        M3RecipeFirstInvariant.requirePostMergeCanonicalTreeReadback(checked);

        ArrayList<String> paths = new ArrayList<>(checked.expectedTargetBlobs().keySet());
        paths.sort(String::compareTo);
        StringBuilder out = new StringBuilder(HEADER).append('\n');
        for (String path : paths) {
            row(
                    out,
                    checked.canonicalBranch(),
                    checked.promotionCommitSha(),
                    checked.canonicalHeadSha(),
                    path,
                    checked.expectedTargetBlobs().get(path),
                    checked.actualTargetBlobs().get(path));
        }
        return out.toString();
    }

    public static String root(
            M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence evidence) {
        M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence checked =
                Objects.requireNonNull(evidence, "evidence");
        M3RecipeFirstInvariant.requirePostMergeCanonicalTreeReadback(checked);
        ArrayList<String> paths = new ArrayList<>(checked.expectedTargetBlobs().keySet());
        paths.sort(String::compareTo);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "M3-CANONICAL-TREE-READBACK/1");
            frame(digest, checked.canonicalBranch());
            frame(digest, checked.promotionCommitSha());
            frame(digest, checked.canonicalHeadSha());
            for (String path : paths) {
                frame(digest, path);
                frame(digest, checked.expectedTargetBlobs().get(path));
                frame(digest, checked.actualTargetBlobs().get(path));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void row(StringBuilder out, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) out.append('\t');
            out.append(cell(values[index]));
        }
        out.append('\n');
    }

    private static String cell(String value) {
        String checked = Objects.toString(value, "");
        if (checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("canonical readback TSV cell");
        }
        return checked;
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
