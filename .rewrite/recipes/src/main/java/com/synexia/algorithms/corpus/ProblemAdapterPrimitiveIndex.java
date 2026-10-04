// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.corpus.CompetitiveProblemCategoryCatalog.Platform;
import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.algorithms.shapes.AlgorithmShapeMask;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Precomputed platform/category -> executable search primitive join over the adapter corpus.
 *
 * <p>The adapter index and primitive catalogue remain authoritative. This table is derived once
 * during catalog initialization so repeated tri-platform review never rescans adapter rows or
 * primitive shapes. Projection rows reuse the canonical primitive objects and grant no source
 * copy, execution, replacement, or promotion authority.</p>
 */
public final class ProblemAdapterPrimitiveIndex {

    public record Projection(
            Platform platform,
            CompetitiveProblemCategory category,
            AlgorithmShapeMask adapterShapeMask,
            int adapterCount,
            List<ChallengeSearchPrimitiveCatalog.Primitive> primitives,
            String root) {

        public Projection {
            platform = Objects.requireNonNull(platform, "platform");
            category = Objects.requireNonNull(category, "category");
            adapterShapeMask = Objects.requireNonNull(adapterShapeMask, "adapterShapeMask");
            if (adapterCount < 0) throw new IllegalArgumentException("adapterCount");

            primitives = Objects.requireNonNull(primitives, "primitives").stream()
                    .sorted(Comparator.comparing(ChallengeSearchPrimitiveCatalog.Primitive::id))
                    .toList();
            if (primitives.stream()
                    .map(ChallengeSearchPrimitiveCatalog.Primitive::id)
                    .distinct()
                    .count() != primitives.size()) {
                throw new IllegalArgumentException("duplicate adapter primitive");
            }
            TreeMap<String, ChallengeSearchPrimitiveCatalog.Primitive> expected =
                    new TreeMap<>();
            for (AlgorithmShape shape : adapterShapeMask.shapes()) {
                for (ChallengeSearchPrimitiveCatalog.Primitive primitive :
                        ChallengeSearchPrimitiveCatalog.forShape(shape)) {
                    expected.putIfAbsent(primitive.id(), primitive);
                }
            }
            if (!primitives.equals(List.copyOf(expected.values()))) {
                throw new IllegalArgumentException("adapter primitive projection drift");
            }
            if ((adapterCount == 0) != adapterShapeMask.isEmpty()) {
                throw new IllegalArgumentException("adapter shape mask/count drift");
            }

            CatalogueDigest digest =
                    new CatalogueDigest("SYNEXIA_PROBLEM_ADAPTER_PRIMITIVE_PROJECTION_V1")
                            .text(platform.name())
                            .text(category.name())
                            .text(adapterShapeMask.hex())
                            .number(adapterCount);
            primitives.forEach(primitive -> digest.text(primitive.root()));
            String expectedRoot = digest.finish();
            root = root == null || root.isBlank() ? expectedRoot : sha(root, "root");
            if (!root.equals(expectedRoot)) {
                throw new IllegalArgumentException("adapter primitive projection root mismatch");
            }
        }

        public boolean sourceCopyAuthority() { return false; }
        public boolean executionAuthority() { return false; }
        public boolean replacementAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    public record Snapshot(
            int projections,
            long reviewedAdapterPostings,
            long primitivePostings,
            String adapterIndexRoot,
            String primitiveCatalogRoot,
            String root) {

        public Snapshot {
            int expectedProjections =
                    Math.multiplyExact(
                            Platform.values().length,
                            CompetitiveProblemCategory.values().length);
            if (projections != expectedProjections
                    || reviewedAdapterPostings < 0
                    || primitivePostings < 0) {
                throw new IllegalArgumentException("adapter primitive index counts");
            }
            adapterIndexRoot = sha(adapterIndexRoot, "adapterIndexRoot");
            primitiveCatalogRoot = sha(primitiveCatalogRoot, "primitiveCatalogRoot");
            root = sha(root, "root");
        }

        public boolean sourceCopyAuthority() { return false; }
        public boolean executionAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    private final Projection[][] table;
    private final Snapshot snapshot;

    private ProblemAdapterPrimitiveIndex(Projection[][] table, Snapshot snapshot) {
        Platform[] platforms = Platform.values();
        CompetitiveProblemCategory[] categories = CompetitiveProblemCategory.values();
        if (table.length != platforms.length) {
            throw new IllegalArgumentException("platform table");
        }
        this.table = new Projection[platforms.length][categories.length];
        for (int platform = 0; platform < platforms.length; platform++) {
            if (table[platform].length != categories.length) {
                throw new IllegalArgumentException("category table");
            }
            System.arraycopy(table[platform], 0, this.table[platform], 0, categories.length);
            for (Projection projection : this.table[platform]) {
                Objects.requireNonNull(projection, "projection");
            }
        }
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public static ProblemAdapterPrimitiveIndex compile(ProblemAdapterIndex adapterIndex) {
        ProblemAdapterIndex checked = Objects.requireNonNull(adapterIndex, "adapterIndex");
        Platform[] platforms = Platform.values();
        CompetitiveProblemCategory[] categories = CompetitiveProblemCategory.values();
        Projection[][] table = new Projection[platforms.length][categories.length];

        long adapterPostings = 0L;
        long primitivePostings = 0L;
        CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_PROBLEM_ADAPTER_PRIMITIVE_INDEX_V1")
                        .text(checked.snapshot().root())
                        .text(ChallengeSearchPrimitiveCatalog.root())
                        .number(Math.multiplyExact(platforms.length, categories.length));

        for (Platform platform : platforms) {
            for (CompetitiveProblemCategory category : categories) {
                List<ProblemAdapter> adapters =
                        checked.byPlatformCategory(platform.name(), category);
                EnumSet<AlgorithmShape> shapes = EnumSet.noneOf(AlgorithmShape.class);
                for (ProblemAdapter adapter : adapters) {
                    if (!adapter.executable()
                            || !adapter.classification().classified()
                            || !adapter.source().platform().equalsIgnoreCase(platform.name())
                            || CompetitiveProblemCategory.fromShape(
                                            adapter.classification().shape())
                                    != category) {
                        throw new IllegalStateException(
                                "adapter primitive source posting drift");
                    }
                    shapes.add(adapter.classification().shape());
                }

                AlgorithmShapeMask shapeMask = AlgorithmShapeMask.of(shapes);
                TreeMap<String, ChallengeSearchPrimitiveCatalog.Primitive> primitives =
                        new TreeMap<>();
                for (AlgorithmShape shape : shapeMask.shapes()) {
                    for (ChallengeSearchPrimitiveCatalog.Primitive primitive :
                            ChallengeSearchPrimitiveCatalog.forShape(shape)) {
                        primitives.putIfAbsent(primitive.id(), primitive);
                    }
                }

                Projection projection =
                        new Projection(
                                platform,
                                category,
                                shapeMask,
                                adapters.size(),
                                List.copyOf(primitives.values()),
                                "");
                table[platform.ordinal()][category.ordinal()] = projection;
                adapterPostings = Math.addExact(adapterPostings, adapters.size());
                primitivePostings =
                        Math.addExact(primitivePostings, projection.primitives().size());
                digest.text(projection.root());
            }
        }

        String root = digest.finish();
        Snapshot snapshot =
                new Snapshot(
                        Math.multiplyExact(platforms.length, categories.length),
                        adapterPostings,
                        primitivePostings,
                        checked.snapshot().root(),
                        ChallengeSearchPrimitiveCatalog.root(),
                        root);
        return new ProblemAdapterPrimitiveIndex(table, snapshot);
    }

    public Projection require(Platform platform, CompetitiveProblemCategory category) {
        Platform checkedPlatform = Objects.requireNonNull(platform, "platform");
        CompetitiveProblemCategory checkedCategory =
                Objects.requireNonNull(category, "category");
        return table[checkedPlatform.ordinal()][checkedCategory.ordinal()];
    }

    public Projection require(
            ChallengePlatform platform, CompetitiveProblemCategory category) {
        ChallengePlatform checked = Objects.requireNonNull(platform, "platform");
        if (checked == ChallengePlatform.OTHER) {
            throw new IllegalArgumentException(
                    "OTHER has no problem-adapter primitive lane");
        }
        return require(Platform.valueOf(checked.name()), category);
    }

    public Projection require(String platform, CompetitiveProblemCategory category) {
        String checked = Objects.requireNonNull(platform, "platform")
                .strip()
                .toUpperCase(Locale.ROOT);
        try {
            return require(Platform.valueOf(checked), category);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(
                    "unknown adapter primitive platform: " + platform, unknown);
        }
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public String toTsv() {
        StringBuilder out =
                new StringBuilder(
                        "platform\tcategory\tadapter_shape_mask_128\tadapter_count"
                                + "\tprimitive_count\tprimitive_ids\trow_root\n");
        for (Platform platform : Platform.values()) {
            for (CompetitiveProblemCategory category : CompetitiveProblemCategory.values()) {
                Projection projection = require(platform, category);
                out.append(platform.name())
                        .append('\t').append(category.name())
                        .append('\t').append(projection.adapterShapeMask().hex())
                        .append('\t').append(projection.adapterCount())
                        .append('\t').append(projection.primitives().size())
                        .append('\t').append(String.join(",", projection.primitives().stream()
                                .map(ChallengeSearchPrimitiveCatalog.Primitive::id).toList()))
                        .append('\t').append(projection.root())
                        .append('\n');
            }
        }
        return out.toString();
    }

    private static String sha(String value, String field) {
        String checked =
                Objects.toString(value, "").strip().toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
