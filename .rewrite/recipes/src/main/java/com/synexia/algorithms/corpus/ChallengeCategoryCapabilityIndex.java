// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShapeMask;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Precomputed two-word capability projection for LeetCode, HackerRank and GeeksforGeeks categories.
 *
 * <p>This is taxonomy/search evidence only. A mask match never grants source-copy, rewrite,
 * execution or promotion authority.</p>
 */
public final class ChallengeCategoryCapabilityIndex {
    public record Row(
            ChallengeCategoryCatalog.Category category,
            AlgorithmShapeMask capabilityMask) {
        public Row {
            category = Objects.requireNonNull(category, "category");
            capabilityMask = Objects.requireNonNull(capabilityMask, "capabilityMask");
            if (!Set.copyOf(capabilityMask.shapes()).equals(Set.copyOf(category.shapes()))) {
                throw new IllegalArgumentException("category capability mask drift");
            }
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    private static final ChallengeCategoryCapabilityIndex CANONICAL =
            compile(ChallengeCategoryCatalog.categories());

    private final List<Row> rows;
    private final Map<String, Row> byKey;
    private final Map<ChallengePlatform, List<Row>> byPlatform;
    private final String root;

    private ChallengeCategoryCapabilityIndex(List<Row> rows, String root) {
        this.rows = List.copyOf(rows);
        LinkedHashMap<String, Row> keyed = new LinkedHashMap<>();
        EnumMap<ChallengePlatform, ArrayList<Row>> platforms =
                new EnumMap<>(ChallengePlatform.class);
        for (ChallengePlatform platform : ChallengePlatform.values()) {
            platforms.put(platform, new ArrayList<>());
        }
        for (Row row : rows) {
            String key = key(row.category());
            if (keyed.putIfAbsent(key, row) != null) {
                throw new IllegalArgumentException("duplicate category capability row: " + key);
            }
            platforms.get(row.category().platform()).add(row);
        }
        this.byKey = Map.copyOf(keyed);
        EnumMap<ChallengePlatform, List<Row>> frozen =
                new EnumMap<>(ChallengePlatform.class);
        platforms.forEach((platform, values) -> frozen.put(platform, List.copyOf(values)));
        this.byPlatform = java.util.Collections.unmodifiableMap(frozen);
        this.root = sha(root, "root");
    }

    public static ChallengeCategoryCapabilityIndex canonical() {
        return CANONICAL;
    }

    public static ChallengeCategoryCapabilityIndex compile(
            List<ChallengeCategoryCatalog.Category> categories) {
        ArrayList<ChallengeCategoryCatalog.Category> stable =
                new ArrayList<>(Objects.requireNonNull(categories, "categories"));
        stable.sort(
                Comparator.comparing((ChallengeCategoryCatalog.Category row) -> row.platform().name())
                        .thenComparing(ChallengeCategoryCatalog.Category::id));
        ArrayList<Row> rows = new ArrayList<>(stable.size());
        for (ChallengeCategoryCatalog.Category category : stable) {
            rows.add(new Row(category, AlgorithmShapeMask.of(category.shapes())));
        }
        return new ChallengeCategoryCapabilityIndex(rows, root(rows));
    }

    public List<Row> rows() {
        return rows;
    }

    public List<Row> byPlatform(ChallengePlatform platform) {
        return byPlatform.getOrDefault(
                Objects.requireNonNull(platform, "platform"), List.of());
    }

    public Optional<Row> find(ChallengePlatform platform, String label) {
        Objects.requireNonNull(platform, "platform");
        return ChallengeCategoryCatalog.find(platform, label)
                .map(
                        category -> {
                            Row row = byKey.get(key(category));
                            if (row == null) {
                                throw new IllegalStateException("category capability index drift");
                            }
                            return row;
                        });
    }

    public Row require(ChallengePlatform platform, String label) {
        return find(platform, label)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "unknown challenge category: "
                                                + platform
                                                + ":"
                                                + label));
    }

    public List<Row> covering(AlgorithmShapeMask required) {
        AlgorithmShapeMask checked = Objects.requireNonNull(required, "required");
        if (checked.isEmpty()) return List.of();
        return rows.stream()
                .filter(row -> row.capabilityMask().containsAll(checked))
                .toList();
    }

    public List<Row> intersecting(AlgorithmShapeMask required) {
        AlgorithmShapeMask checked = Objects.requireNonNull(required, "required");
        if (checked.isEmpty()) return List.of();
        return rows.stream()
                .filter(row -> row.capabilityMask().intersects(checked))
                .toList();
    }

    public String root() {
        return root;
    }

    public String toTsv() {
        StringBuilder out =
                new StringBuilder(
                        "platform\tcategory_id\tdisplay_name\tcapability_mask_128"
                                + "\tshape_count\tshapes\tsource\tauthority\tshape_schema_root\n");
        for (Row row : rows) {
            ChallengeCategoryCatalog.Category category = row.category();
            out.append(category.platform().name())
                    .append('\t')
                    .append(safe(category.id()))
                    .append('\t')
                    .append(safe(category.displayName()))
                    .append('\t')
                    .append(row.capabilityMask().hex())
                    .append('\t')
                    .append(row.capabilityMask().bitCount())
                    .append('\t')
                    .append(
                            String.join(
                                    ",",
                                    row.capabilityMask().shapes().stream()
                                            .map(Enum::name)
                                            .toList()))
                    .append('\t')
                    .append(safe(category.source()))
                    .append('\t')
                    .append(safe(category.authority()))
                    .append('\t')
                    .append(AlgorithmShapeMask.schemaRoot())
                    .append('\n');
        }
        return out.toString();
    }

    private static String key(ChallengeCategoryCatalog.Category category) {
        return category.platform().name() + "/" + category.id();
    }

    private static String root(List<Row> rows) {
        MessageDigest digest = sha256();
        frame(digest, "CHALLENGE-CATEGORY-CAPABILITY-INDEX/1");
        frame(digest, AlgorithmShapeMask.schemaRoot());
        frame(digest, ChallengeCategoryCatalog.root());
        frame(digest, Integer.toString(rows.size()));
        for (Row row : rows) {
            ChallengeCategoryCatalog.Category category = row.category();
            frame(digest, category.platform().name());
            frame(digest, category.id());
            frame(digest, category.displayName());
            frame(digest, row.capabilityMask().hex());
            frame(digest, category.source());
            frame(digest, category.authority());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String safe(String value) {
        return Objects.toString(value, "")
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private static String sha(String value, String field) {
        String checked = Objects.toString(value, "").strip().toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
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
