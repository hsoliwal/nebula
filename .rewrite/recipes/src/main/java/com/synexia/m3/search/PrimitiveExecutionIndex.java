// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.search;

import com.synexia.algorithms.corpus.ChallengeSearchPrimitiveCatalog;
import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Canonical precomputed execution-step projection for registered search primitives.
 *
 * <p>The primitive catalogue remains the authority. This index only materializes immutable
 * Java/JNI execution metadata once so repeated category/problem reviews do not rebuild the same
 * validated step records.</p>
 */
public final class PrimitiveExecutionIndex {
    private static final PrimitiveExecutionIndex CANONICAL =
            new PrimitiveExecutionIndex(ChallengeSearchPrimitiveCatalog.all());

    private final Map<String, CompetitiveProblemReview.PrimitiveExecutionStep> byId;
    private final Map<AlgorithmShape, List<CompetitiveProblemReview.PrimitiveExecutionStep>> byShape;
    private final String primitiveCatalogRoot;
    private final String root;

    private PrimitiveExecutionIndex(List<ChallengeSearchPrimitiveCatalog.Primitive> primitives) {
        ArrayList<ChallengeSearchPrimitiveCatalog.Primitive> ordered =
                new ArrayList<>(Objects.requireNonNull(primitives, "primitives"));
        ordered.sort(Comparator.comparing(ChallengeSearchPrimitiveCatalog.Primitive::id));

        LinkedHashMap<String, CompetitiveProblemReview.PrimitiveExecutionStep> ids =
                new LinkedHashMap<>();
        EnumMap<AlgorithmShape, ArrayList<CompetitiveProblemReview.PrimitiveExecutionStep>> shapes =
                new EnumMap<>(AlgorithmShape.class);
        for (ChallengeSearchPrimitiveCatalog.Primitive primitive : ordered) {
            ChallengeSearchPrimitiveCatalog.Primitive canonical =
                    ChallengeSearchPrimitiveCatalog.require(primitive.id());
            if (!canonical.equals(primitive)) {
                throw new IllegalArgumentException("non-canonical primitive: " + primitive.id());
            }
            CompetitiveProblemReview.PrimitiveExecutionStep step = step(canonical);
            if (ids.putIfAbsent(step.primitiveId(), step) != null) {
                throw new IllegalArgumentException("duplicate primitive: " + step.primitiveId());
            }
            for (AlgorithmShape shape : canonical.shapes()) {
                shapes.computeIfAbsent(shape, ignored -> new ArrayList<>()).add(step);
            }
        }
        this.byId = Collections.unmodifiableMap(ids);
        EnumMap<AlgorithmShape, List<CompetitiveProblemReview.PrimitiveExecutionStep>> frozen =
                new EnumMap<>(AlgorithmShape.class);
        shapes.forEach((shape, values) -> frozen.put(shape, List.copyOf(values)));
        this.byShape = Collections.unmodifiableMap(frozen);
        this.primitiveCatalogRoot = ChallengeSearchPrimitiveCatalog.root();
        this.root = root(primitiveCatalogRoot, List.copyOf(ids.values()));
    }

    public static PrimitiveExecutionIndex canonical() {
        return CANONICAL;
    }

    public CompetitiveProblemReview.PrimitiveExecutionStep require(String primitiveId) {
        String checked = Objects.requireNonNull(primitiveId, "primitiveId").strip();
        CompetitiveProblemReview.PrimitiveExecutionStep step = byId.get(checked);
        if (step == null) throw new IllegalArgumentException("unknown primitive: " + checked);
        return step;
    }

    public List<CompetitiveProblemReview.PrimitiveExecutionStep> forShape(AlgorithmShape shape) {
        return byShape.getOrDefault(Objects.requireNonNull(shape, "shape"), List.of());
    }

    public List<CompetitiveProblemReview.PrimitiveExecutionStep> forPrimitives(
            Collection<ChallengeSearchPrimitiveCatalog.Primitive> primitives) {
        TreeMap<String, CompetitiveProblemReview.PrimitiveExecutionStep> selected =
                new TreeMap<>();
        for (ChallengeSearchPrimitiveCatalog.Primitive primitive :
                Objects.requireNonNull(primitives, "primitives")) {
            ChallengeSearchPrimitiveCatalog.Primitive canonical =
                    ChallengeSearchPrimitiveCatalog.require(
                            Objects.requireNonNull(primitive, "primitive").id());
            if (!canonical.equals(primitive)) {
                throw new IllegalArgumentException("non-canonical primitive: " + primitive.id());
            }
            selected.putIfAbsent(canonical.id(), require(canonical.id()));
        }
        return List.copyOf(selected.values());
    }

    public String primitiveCatalogRoot() {
        return primitiveCatalogRoot;
    }

    public String root() {
        return root;
    }

    private static CompetitiveProblemReview.PrimitiveExecutionStep step(
            ChallengeSearchPrimitiveCatalog.Primitive primitive) {
        CompetitiveProblemReview.PrimitiveExecutionLane lane =
                primitive.nativeOptional()
                        ? CompetitiveProblemReview.PrimitiveExecutionLane.JAVA_PRIMARY_JNI_OPTIONAL
                        : CompetitiveProblemReview.PrimitiveExecutionLane.JAVA_ONLY;
        return new CompetitiveProblemReview.PrimitiveExecutionStep(
                primitive.id(),
                primitive.root(),
                primitive.representation(),
                primitive.prepared(),
                primitive.javaOwner(),
                primitive.javaEntryPoint(),
                primitive.nativeSymbol(),
                lane,
                "");
    }

    private static String root(
            String primitiveCatalogRoot,
            List<CompetitiveProblemReview.PrimitiveExecutionStep> steps) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "M3-PRIMITIVE-EXECUTION-INDEX/1");
            frame(digest, primitiveCatalogRoot);
            frame(digest, Integer.toString(steps.size()));
            for (CompetitiveProblemReview.PrimitiveExecutionStep step : steps) {
                frame(digest, step.root());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
