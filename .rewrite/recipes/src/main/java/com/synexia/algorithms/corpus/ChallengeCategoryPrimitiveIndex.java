// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.algorithms.shapes.AlgorithmShapeMask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Precomputed category -> executable search-primitive projection for public challenge taxonomies.
 *
 * <p>The category capability index and executable primitive catalogue remain the canonical owners.
 * This index joins them once at class initialization so repeated LeetCode/HackerRank/GeeksforGeeks
 * review does not rescan primitive shapes. Rows are evidence only and never grant source-copy,
 * execution, replacement, or promotion authority.</p>
 */
public final class ChallengeCategoryPrimitiveIndex {
    public record Row(
            ChallengeCategoryCatalog.Category category,
            AlgorithmShapeMask categoryCapabilityMask,
            AlgorithmShapeMask executableCapabilityMask,
            List<ChallengeSearchPrimitiveCatalog.Primitive> primitives,
            String root) {

        public Row {
            category = Objects.requireNonNull(category, "category");
            categoryCapabilityMask =
                    Objects.requireNonNull(categoryCapabilityMask, "categoryCapabilityMask");
            executableCapabilityMask =
                    Objects.requireNonNull(executableCapabilityMask, "executableCapabilityMask");
            primitives = Objects.requireNonNull(primitives, "primitives").stream()
                    .sorted(Comparator.comparing(ChallengeSearchPrimitiveCatalog.Primitive::id))
                    .toList();
            if (!AlgorithmShapeMask.of(category.shapes()).equals(categoryCapabilityMask)) {
                throw new IllegalArgumentException("category capability mask drift");
            }
            if (!categoryCapabilityMask.containsAll(executableCapabilityMask)) {
                throw new IllegalArgumentException("executable mask escapes category capabilities");
            }
            if (primitives.stream()
                    .map(ChallengeSearchPrimitiveCatalog.Primitive::id)
                    .distinct().count() != primitives.size()) {
                throw new IllegalArgumentException("duplicate category primitive");
            }

            EnumSet<AlgorithmShape> executableShapes = EnumSet.noneOf(AlgorithmShape.class);
            for (ChallengeSearchPrimitiveCatalog.Primitive primitive : primitives) {
                if (!ChallengeSearchPrimitiveCatalog.require(primitive.id()).equals(primitive)) {
                    throw new IllegalArgumentException("non-canonical category primitive");
                }
                boolean intersects = false;
                for (AlgorithmShape shape : primitive.shapes()) {
                    if (categoryCapabilityMask.contains(shape)) {
                        executableShapes.add(shape);
                        intersects = true;
                    }
                }
                if (!intersects) {
                    throw new IllegalArgumentException("primitive does not serve category");
                }
            }
            if (!AlgorithmShapeMask.of(executableShapes).equals(executableCapabilityMask)) {
                throw new IllegalArgumentException("category executable mask drift");
            }

            CatalogueDigest digest = new CatalogueDigest("CHALLENGE-CATEGORY-PRIMITIVE-ROW/1")
                    .text(category.platform().name())
                    .text(category.id())
                    .text(categoryCapabilityMask.hex())
                    .text(executableCapabilityMask.hex());
            primitives.forEach(primitive -> digest.text(primitive.root()));
            String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("category primitive row root mismatch");
            }
        }

        public boolean sourceCopyAuthority() { return false; }
        public boolean executionAuthority() { return false; }
        public boolean replacementAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    private static final ChallengeCategoryPrimitiveIndex CANONICAL = buildCanonical();

    private final List<Row> rows;
    private final Map<String, Row> byKey;
    private final Map<ChallengePlatform, List<Row>> byPlatform;
    private final String root;

    private ChallengeCategoryPrimitiveIndex(List<Row> rows, String root) {
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
                throw new IllegalArgumentException("duplicate category primitive row: " + key);
            }
            platforms.get(row.category().platform()).add(row);
        }
        byKey = Collections.unmodifiableMap(keyed);
        EnumMap<ChallengePlatform, List<Row>> frozen = new EnumMap<>(ChallengePlatform.class);
        platforms.forEach((platform, values) -> frozen.put(platform, List.copyOf(values)));
        byPlatform = Collections.unmodifiableMap(frozen);
        this.root = sha(root, "root");
    }

    public static ChallengeCategoryPrimitiveIndex canonical() { return CANONICAL; }

    public List<Row> rows() { return rows; }

    public List<Row> byPlatform(ChallengePlatform platform) {
        return byPlatform.getOrDefault(
                Objects.requireNonNull(platform, "platform"), List.of());
    }

    public Row require(ChallengePlatform platform, String label) {
        ChallengeCategoryCapabilityIndex.Row capability =
                ChallengeCategoryCapabilityIndex.canonical()
                        .require(Objects.requireNonNull(platform, "platform"), label);
        Row row = byKey.get(key(capability.category()));
        if (row == null) throw new IllegalStateException("category primitive index drift");
        return row;
    }

    public String root() { return root; }

    public String toTsv() {
        StringBuilder out =
                new StringBuilder(
                        "platform\tcategory_id\tdisplay_name\tcategory_mask_128"
                                + "\texecutable_mask_128\tprimitive_count\tprimitive_ids"
                                + "\tcategory_capability_root\tprimitive_catalog_root\trow_root\n");
        for (Row row : rows) {
            out.append(row.category().platform().name())
                    .append('\t').append(safe(row.category().id()))
                    .append('\t').append(safe(row.category().displayName()))
                    .append('\t').append(row.categoryCapabilityMask().hex())
                    .append('\t').append(row.executableCapabilityMask().hex())
                    .append('\t').append(row.primitives().size())
                    .append('\t').append(String.join(",", row.primitives().stream()
                            .map(ChallengeSearchPrimitiveCatalog.Primitive::id).toList()))
                    .append('\t').append(ChallengeCategoryCapabilityIndex.canonical().root())
                    .append('\t').append(ChallengeSearchPrimitiveCatalog.root())
                    .append('\t').append(row.root())
                    .append('\n');
        }
        return out.toString();
    }

    private static ChallengeCategoryPrimitiveIndex buildCanonical() {
        ChallengeCategoryCapabilityIndex capabilities =
                ChallengeCategoryCapabilityIndex.canonical();
        ArrayList<Row> rows = new ArrayList<>(capabilities.rows().size());
        for (ChallengeCategoryCapabilityIndex.Row capability : capabilities.rows()) {
            LinkedHashMap<String, ChallengeSearchPrimitiveCatalog.Primitive> primitives =
                    new LinkedHashMap<>();
            EnumSet<AlgorithmShape> executableShapes = EnumSet.noneOf(AlgorithmShape.class);
            for (AlgorithmShape shape : capability.capabilityMask().shapes()) {
                for (ChallengeSearchPrimitiveCatalog.Primitive primitive :
                        ChallengeSearchPrimitiveCatalog.forShape(shape)) {
                    primitives.putIfAbsent(primitive.id(), primitive);
                    executableShapes.add(shape);
                }
            }
            List<ChallengeSearchPrimitiveCatalog.Primitive> stable =
                    primitives.values().stream()
                            .sorted(Comparator.comparing(
                                    ChallengeSearchPrimitiveCatalog.Primitive::id))
                            .toList();
            rows.add(new Row(
                    capability.category(),
                    capability.capabilityMask(),
                    AlgorithmShapeMask.of(executableShapes),
                    stable,
                    ""));
        }
        rows.sort(
                Comparator.comparing((Row row) -> row.category().platform().name())
                        .thenComparing(row -> row.category().id()));
        CatalogueDigest digest = new CatalogueDigest("CHALLENGE-CATEGORY-PRIMITIVE-INDEX/1")
                .text(capabilities.root())
                .text(ChallengeSearchPrimitiveCatalog.root())
                .number(rows.size());
        rows.forEach(row -> digest.text(row.root()));
        return new ChallengeCategoryPrimitiveIndex(rows, digest.finish());
    }

    private static String key(ChallengeCategoryCatalog.Category category) {
        return category.platform().name() + "/" + category.id();
    }

    private static String safe(String value) {
        return Objects.toString(value, "")
                .replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static String sha(String value, String field) {
        String checked = Objects.toString(value, "").strip().toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }
}
