// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Deterministic metadata-only serialization for one LLM task recipe crate. */
public final class M3LlmTaskRecipeCrateCodec {
    public static final String VERSION = "M3_LLM_TASK_RECIPE_CRATE_V1";
    private static final String HEADER = "key\tvalue\n";

    private M3LlmTaskRecipeCrateCodec() {}

    public static String root(M3LlmTaskRecipeCrate crate) {
        M3LlmTaskRecipeCrate checked = Objects.requireNonNull(crate, "crate");
        MessageDigest digest = digest();
        frame(digest, VERSION);
        frame(digest, checked.taskId());
        frame(digest, checked.capabilityId());
        frame(digest, checked.requirementSha256());
        frame(digest, checked.catalogueSha256());
        frame(digest, checked.disposition().name());
        checked.recipeIds().forEach(value -> frame(digest, "recipe:" + value));
        checked.targetPaths().stream().sorted().forEach(value -> frame(digest, "target:" + value));
        checked.donorReferences().stream().sorted().forEach(value -> frame(digest, "donor:" + value));
        checked.requiredGates().stream()
                .sorted(Comparator.comparingInt(M3LlmTaskRecipeCrate.Gate::ordinal))
                .forEach(value -> frame(digest, "gate:" + value.name()));
        frame(digest, "directFileMutationAllowed=false");
        frame(digest, "canonicalPromotionAllowed=false");
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String tsv(M3LlmTaskRecipeCrate crate) {
        M3LlmTaskRecipeCrate checked = Objects.requireNonNull(crate, "crate");
        StringBuilder out = new StringBuilder(HEADER);
        row(out, "version", VERSION);
        row(out, "root", root(checked));
        row(out, "taskId", checked.taskId());
        row(out, "capabilityId", checked.capabilityId());
        row(out, "requirementSha256", checked.requirementSha256());
        row(out, "catalogueSha256", checked.catalogueSha256());
        row(out, "disposition", checked.disposition().name());
        checked.recipeIds().forEach(value -> row(out, "recipe", value));
        checked.targetPaths().stream().sorted().forEach(value -> row(out, "target", value));
        checked.donorReferences().stream().sorted().forEach(value -> row(out, "donor", value));
        checked.requiredGates().stream()
                .sorted(Comparator.comparingInt(M3LlmTaskRecipeCrate.Gate::ordinal))
                .forEach(value -> row(out, "gate", value.name()));
        row(out, "directFileMutationAllowed", "false");
        row(out, "canonicalPromotionAllowed", "false");
        return out.toString();
    }

    /**
     * Strictly parse one canonical crate TSV and recompute its semantic root.
     *
     * <p>CRLF is accepted as a transport line ending, but the parsed logical document must otherwise
     * be byte-canonical after CRLF -> LF normalization. The admission receipt separately binds the
     * exact raw UTF-8 document bytes.</p>
     */
    public static M3LlmTaskRecipeCrate parse(String tsv) {
        String raw = Objects.requireNonNull(tsv, "tsv");
        if (raw.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("crate TSV contains NUL");
        }
        String normalized = raw.replace("\r\n", "\n");
        if (normalized.indexOf('\r') >= 0 || !normalized.endsWith("\n")) {
            throw new IllegalArgumentException("crate TSV must use LF or CRLF lines and end with newline");
        }
        if (!normalized.startsWith(HEADER)) {
            throw new IllegalArgumentException("crate TSV header mismatch");
        }

        Map<String, String> singleton = new LinkedHashMap<>();
        List<String> recipes = new ArrayList<>();
        Set<String> targets = new LinkedHashSet<>();
        Set<String> donors = new LinkedHashSet<>();
        EnumSet<M3LlmTaskRecipeCrate.Gate> gates =
                EnumSet.noneOf(M3LlmTaskRecipeCrate.Gate.class);

        String[] lines = normalized.split("\n", -1);
        for (int index = 1; index < lines.length - 1; index++) {
            String line = lines[index];
            int tab = line.indexOf('\t');
            if (tab <= 0 || tab != line.lastIndexOf('\t')) {
                throw new IllegalArgumentException("invalid crate TSV row " + (index + 1));
            }
            String key = line.substring(0, tab);
            String value = line.substring(tab + 1);
            if (value.isEmpty()) {
                throw new IllegalArgumentException("empty crate TSV value for " + key);
            }
            switch (key) {
                case "recipe" -> recipes.add(value);
                case "target" -> {
                    if (!targets.add(value)) {
                        throw new IllegalArgumentException("duplicate target row");
                    }
                }
                case "donor" -> {
                    if (!donors.add(value)) {
                        throw new IllegalArgumentException("duplicate donor row");
                    }
                }
                case "gate" -> {
                    final M3LlmTaskRecipeCrate.Gate gate;
                    try {
                        gate = M3LlmTaskRecipeCrate.Gate.valueOf(value);
                    } catch (IllegalArgumentException invalid) {
                        throw new IllegalArgumentException("unknown crate gate: " + value, invalid);
                    }
                    if (!gates.add(gate)) {
                        throw new IllegalArgumentException("duplicate gate row");
                    }
                }
                case "version",
                        "root",
                        "taskId",
                        "capabilityId",
                        "requirementSha256",
                        "catalogueSha256",
                        "disposition",
                        "directFileMutationAllowed",
                        "canonicalPromotionAllowed" ->
                        putSingleton(singleton, key, value);
                default -> throw new IllegalArgumentException("unknown crate TSV key: " + key);
            }
        }

        require(singleton, "version", VERSION);
        require(singleton, "directFileMutationAllowed", "false");
        require(singleton, "canonicalPromotionAllowed", "false");

        final M3LlmTaskRecipeCrate.Disposition disposition;
        try {
            disposition =
                    M3LlmTaskRecipeCrate.Disposition.valueOf(
                            required(singleton, "disposition"));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("unknown crate disposition", invalid);
        }

        M3LlmTaskRecipeCrate crate =
                new M3LlmTaskRecipeCrate(
                        required(singleton, "taskId"),
                        required(singleton, "capabilityId"),
                        required(singleton, "requirementSha256"),
                        required(singleton, "catalogueSha256"),
                        disposition,
                        recipes,
                        targets,
                        donors,
                        gates);

        String declaredRoot = required(singleton, "root");
        String actualRoot = root(crate);
        if (!declaredRoot.equals(actualRoot)) {
            throw new IllegalArgumentException("crate TSV root mismatch");
        }
        if (!tsv(crate).equals(normalized)) {
            throw new IllegalArgumentException("crate TSV is not canonical");
        }
        return crate;
    }

    private static void putSingleton(Map<String, String> fields, String key, String value) {
        if (fields.putIfAbsent(key, value) != null) {
            throw new IllegalArgumentException("duplicate crate TSV singleton: " + key);
        }
    }

    private static void require(Map<String, String> fields, String key, String expected) {
        String actual = required(fields, key);
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("crate TSV " + key + " mismatch");
        }
    }

    private static String required(Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing crate TSV field: " + key);
        }
        return value;
    }

    private static void row(StringBuilder out, String key, String value) {
        if (key.indexOf('\t') >= 0
                || value.indexOf('\t') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("crate TSV fields must be single-line");
        }
        out.append(key).append('\t').append(value).append('\n');
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
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
