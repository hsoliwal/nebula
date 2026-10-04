// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One production adapter per indexed Java implementation plus precomputed immutable projections.
 */
public final class ProblemAdapterCatalog {

    private static final List<ProblemAdapter> ADAPTERS = build();
    private static final ProblemAdapterIndex INDEX = ProblemAdapterIndex.compile(ADAPTERS);
    private static final ProblemAdapterPrimitiveIndex PRIMITIVE_INDEX =
            ProblemAdapterPrimitiveIndex.compile(INDEX);

    private ProblemAdapterCatalog() {}

    /** File-level inventory: one adapter for every pinned Java implementation path. */
    public static List<ProblemAdapter> all() {
        return INDEX.all();
    }

    /** Shared immutable index over the complete adapter corpus. */
    public static ProblemAdapterIndex index() {
        return INDEX;
    }

    public static ProblemAdapterIndex.Snapshot indexSnapshot() {
        return INDEX.snapshot();
    }

    /** Shared precomputed platform/category -> executable primitive index. */
    public static ProblemAdapterPrimitiveIndex primitiveIndex() {
        return PRIMITIVE_INDEX;
    }

    public static ProblemAdapterPrimitiveIndex.Snapshot primitiveIndexSnapshot() {
        return PRIMITIVE_INDEX.snapshot();
    }

    public static ProblemAdapter require(String id) {
        return INDEX.require(id);
    }

    public static List<ProblemAdapter> executable() {
        return INDEX.executable();
    }

    public static List<ProblemAdapter> unresolved() {
        return INDEX.unresolved();
    }

    public static List<ProblemAdapter> byShape(AlgorithmShape shape) {
        return INDEX.byShape(shape);
    }

    public static List<ProblemAdapter> byTemplate(EnterpriseTemplateKind kind) {
        return INDEX.byTemplate(kind);
    }

    public static List<ProblemAdapter> byRepository(String repository) {
        return INDEX.byRepository(repository);
    }

    public static List<ProblemAdapter> byPlatform(String platform) {
        return INDEX.byPlatform(platform);
    }

    /** Classified adapters for one normalized problem category, in corpus order. */
    public static List<ProblemAdapter> byCategory(CompetitiveProblemCategory category) {
        return INDEX.byCategory(category);
    }

    /** Classified adapters for one platform and normalized category, in corpus order. */
    public static List<ProblemAdapter> byPlatformCategory(
            String platform, CompetitiveProblemCategory category) {
        return INDEX.byPlatformCategory(platform, category);
    }

    public static List<ProblemAdapter> byProblem(String problemKey) {
        return INDEX.byProblem(problemKey);
    }

    /**
     * Deduplicated logical problem inventory.
     *
     * <p>Multiple repositories/solutions for the same platform problem collapse to one immutable
     * postings list only when their normalized problem identities agree.
     */
    public static Map<String, List<ProblemAdapter>> implementationsByProblem() {
        return INDEX.implementationsByProblem();
    }

    private static List<ProblemAdapter> build() {
        final List<ProblemAdapter> result = new ArrayList<>(JavaProblemCorpus.entries().size());
        for (CorpusSourceEntry source : JavaProblemCorpus.entries()) {
            final ProblemShape classification = CrossDonorShapeResolver.resolve(source);
            final TemplateStyle style = classification.classified()
                    ? ProblemTemplateCatalog.style(classification.shape())
                    : new TemplateStyle(
                            EnterpriseTemplateKind.CUSTOM,
                            List.of("classify", "adapt"),
                            "encode -> canonical donor -> decode");
            result.add(new ProblemAdapter(source, classification, style));
        }
        return List.copyOf(result);
    }
}
