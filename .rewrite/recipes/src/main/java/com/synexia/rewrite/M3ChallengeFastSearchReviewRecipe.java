// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.synexia.algorithms.corpus.ChallengeCategoryCapabilityIndex;
import com.synexia.algorithms.corpus.ChallengePlatform;
import com.synexia.algorithms.corpus.ChallengeProblem;
import com.synexia.algorithms.corpus.ChallengeSearchIndex;
import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.algorithms.shapes.AlgorithmShapeMask;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/**
 * Candidate-only fast search over the existing immutable challenge posting index.
 *
 * <p>This recipe reuses {@link ChallengeSearchIndex}; it does not build another corpus index,
 * download challenge sites, copy solution bodies, execute donor code, or grant replacement
 * authority.</p>
 */
public final class M3ChallengeFastSearchReviewRecipe extends Recipe {
    @Option(
            displayName = "Challenge platform",
            description = "ALL, LEETCODE, HACKERRANK, GEEKSFORGEEKS, or GFG.",
            example = "GFG",
            required = false)
    private final String platform;

    @Option(
            displayName = "Algorithm shape",
            description = "ALL or an AlgorithmShape enum value such as BINARY_SEARCH or KADANE.",
            example = "BINARY_SEARCH",
            required = false)
    private final String shape;

    @Option(
            displayName = "Platform category",
            description =
                    "Optional LeetCode/HackerRank/GeeksforGeeks category id, display name, or alias. "
                            + "Resolved through ChallengeCategoryCatalog before posting lookup.",
            example = "binary-search",
            required = false)
    private final String category;

    @Option(
            displayName = "Search term",
            description = "Optional term matched through the existing precomputed token postings.",
            example = "anagram",
            required = false)
    private final String term;

    @Option(
            displayName = "Donor repository",
            description = "Optional exact donor repository filter such as cvalingam/GeeksforGeeks.",
            example = "cvalingam/GeeksforGeeks",
            required = false)
    private final String repository;

    @Option(
            displayName = "Result limit",
            description = "Maximum number of logical challenge hits, from 1 through 100000.",
            example = "1000",
            required = false)
    private final Integer limit;

    private final transient SearchTable searchTable = new SearchTable(this);

    public M3ChallengeFastSearchReviewRecipe() {
        this("ALL", "ALL", "", "", "", 1000);
    }

    public M3ChallengeFastSearchReviewRecipe(
            String platform, String shape, String term, String repository, Integer limit) {
        this(platform, shape, "", term, repository, limit);
    }

    @JsonCreator
    public M3ChallengeFastSearchReviewRecipe(
            String platform,
            String shape,
            String category,
            String term,
            String repository,
            Integer limit) {
        this.platform = normalizePlatform(platform);
        this.shape = normalizeShape(shape);
        this.category = Objects.requireNonNullElse(category, "").strip();
        validateCategoryShape(this.platform, this.shape, this.category);
        this.term = Objects.requireNonNullElse(term, "").strip();
        this.repository = Objects.requireNonNullElse(repository, "").strip();
        this.limit = Objects.requireNonNullElse(limit, 1000);
        if (this.limit < 1 || this.limit > 100_000) {
            throw new IllegalArgumentException("limit must be between 1 and 100000");
        }
    }

    @Override
    public String getDisplayName() {
        return "M3 challenge fast-search review";
    }

    @Override
    public String getDescription() {
        return "Searches the existing immutable LeetCode/HackerRank/GeeksforGeeks challenge "
                + "posting index by platform, category, algorithm shape, term, or donor repository. "
                + "Category mechanics are resolved through the precomputed two-word capability mask "
                + "index before posting lookup; results remain candidate-only evidence.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "challenge",
                "search",
                "precomputed",
                "indexed",
                "capability-mask-128",
                "leetcode",
                "hackerrank",
                "geeksforgeeks",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (claimEmission(context)) {
                    plannedRows().forEach(row -> searchTable.insertRow(context, row));
                }
                return tree;
            }
        };
    }

    public String getPlatform() {
        return platform;
    }

    public String getShape() {
        return shape;
    }

    public String getCategory() {
        return category;
    }

    public String getTerm() {
        return term;
    }

    public String getRepository() {
        return repository;
    }

    public Integer getLimit() {
        return limit;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public String searchRoot() {
        return scopedEvaluation().root();
    }

    /** Deterministic projection from the existing prepared-posting evaluation. */
    public List<SearchRow> plannedRows() {
        M3PlatformScopedCategorySearch.Result evaluation = scopedEvaluation();
        ArrayList<SearchRow> rows = new ArrayList<>(evaluation.hits().size());
        int ordinal = 0;
        for (ChallengeSearchIndex.Hit hit : evaluation.hits()) {
            ChallengeProblem problem = hit.problem();
            rows.add(
                    new SearchRow(
                            ordinal++,
                            problem.id().stableId(),
                            problem.title(),
                            problem.id().platform().name(),
                            problem.shape().map(Enum::name).orElse(""),
                            problem.status().name(),
                            String.join(",", hit.donorRepositories()),
                            hit.sourceImplementations(),
                            evaluation.root()));
        }
        return List.copyOf(rows);
    }

    private ChallengeSearchIndex.Evaluation evaluation() {
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("M3 challenge search interrupted");
        }
        ChallengeSearchIndex.Query query =
                new ChallengeSearchIndex.Query(
                        platform.equals("ALL")
                                ? Set.of()
                                : Set.of(ChallengePlatform.valueOf(platform)),
                        selectedShapes(platform, shape, category),
                        Set.of(),
                        Set.of(),
                        term.isEmpty() ? List.of() : List.of(term),
                        repository.isEmpty() ? List.of() : List.of(repository),
                        limit);
        ChallengeSearchIndex.Evaluation result = ChallengeSearchIndex.canonical().evaluate(query);
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("M3 challenge search interrupted");
        }
        return result;
    }

    private M3PlatformScopedCategorySearch.Result scopedEvaluation() {
        if (platform.equals("ALL") && !category.isEmpty()) {
            return M3PlatformScopedCategorySearch.evaluate(
                    shape, category, term, repository, limit);
        }
        if (platform.equals("ALL") && !shape.equals("ALL")) {
            return M3PlatformScopedCategorySearch.evaluateShape(
                    shape, term, repository, limit);
        }
        ChallengeSearchIndex.Evaluation direct = evaluation();
        return new M3PlatformScopedCategorySearch.Result(direct.hits(), direct.root());
    }

    private boolean claimEmission(ExecutionContext context) {
        String key =
                M3ChallengeFastSearchReviewRecipe.class.getName()
                        + ".emitted."
                        + platform
                        + "."
                        + shape
                        + "."
                        + category
                        + "."
                        + term
                        + "."
                        + repository
                        + "."
                        + limit;
        synchronized (context) {
            if (Boolean.TRUE.equals(context.getMessage(key))) {
                return false;
            }
            context.putMessage(key, Boolean.TRUE);
            return true;
        }
    }

    private static String normalizePlatform(String value) {
        String normalized = Objects.requireNonNullElse(value, "ALL").strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) normalized = "ALL";
        if (normalized.equals("GFG")) normalized = "GEEKSFORGEEKS";
        if (normalized.equals("ALL")) return normalized;
        ChallengePlatform parsed = ChallengePlatform.from(normalized);
        if (parsed == ChallengePlatform.OTHER) {
            throw new IllegalArgumentException("unsupported challenge platform: " + value);
        }
        return parsed.name();
    }

    private static Set<AlgorithmShape> selectedShapes(
            String platform, String shape, String category) {
        if (category.isEmpty()) {
            return shape.equals("ALL")
                    ? Set.of()
                    : Set.of(AlgorithmShape.valueOf(shape));
        }

        ChallengeCategoryCapabilityIndex index =
                ChallengeCategoryCapabilityIndex.canonical();
        AlgorithmShapeMask mask = AlgorithmShapeMask.empty();
        if (platform.equals("ALL")) {
            for (ChallengePlatform candidate :
                    List.of(
                            ChallengePlatform.LEETCODE,
                            ChallengePlatform.HACKERRANK,
                            ChallengePlatform.GEEKSFORGEEKS)) {
                var resolved = index.find(candidate, category);
                if (resolved.isPresent()) {
                    mask = mask.union(resolved.orElseThrow().capabilityMask());
                }
            }
        } else {
            mask =
                    index.find(ChallengePlatform.valueOf(platform), category)
                            .map(ChallengeCategoryCapabilityIndex.Row::capabilityMask)
                            .orElse(AlgorithmShapeMask.empty());
        }
        if (mask.isEmpty()) {
            throw new IllegalArgumentException(
                    "unknown challenge category for selected platform: " + category);
        }
        if (!shape.equals("ALL")) {
            AlgorithmShape selected = AlgorithmShape.valueOf(shape);
            if (!mask.contains(selected)) {
                throw new IllegalArgumentException(
                        "algorithm shape is not mapped by selected challenge category");
            }
            return Set.of(selected);
        }
        return Set.copyOf(mask.shapes());
    }

    private static void validateCategoryShape(
            String platform, String shape, String category) {
        if (!category.isEmpty()) selectedShapes(platform, shape, category);
    }

    private static String normalizeShape(String value) {
        String normalized = Objects.requireNonNullElse(value, "ALL").strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) normalized = "ALL";
        normalized = normalized.replace('-', '_').replace(' ', '_');
        if (normalized.equals("ALL")) return normalized;
        try {
            return AlgorithmShape.valueOf(normalized).name();
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("unsupported algorithm shape: " + value, invalid);
        }
    }

    public static final class SearchTable extends DataTable<SearchRow> {
        SearchTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 precomputed challenge search",
                    "Candidate-only hits from the existing immutable challenge posting index.");
        }
    }

    public static final class SearchRow {
        @Column(displayName = "Ordinal", description = "Stable result ordinal.")
        private final int ordinal;

        @Column(displayName = "Stable ID", description = "Logical challenge identity.")
        private final String stableId;

        @Column(displayName = "Title", description = "Logical challenge title.")
        private final String title;

        @Column(displayName = "Platform", description = "Challenge platform.")
        private final String platform;

        @Column(displayName = "Shape", description = "Resolved AlgorithmShape when executable.")
        private final String shape;

        @Column(displayName = "Status", description = "Challenge classification/execution status.")
        private final String status;

        @Column(displayName = "Donor repositories", description = "Pinned donor repositories.")
        private final String donorRepositories;

        @Column(
                displayName = "Source implementations",
                description = "Number of pinned source implementations retained as provenance.")
        private final int sourceImplementations;

        @Column(
                displayName = "Search root",
                description = "SHA-256 root of the complete deterministic search evaluation.")
        private final String searchRoot;

        @Column(
                displayName = "Replacement authority",
                description = "Always false; search evidence cannot replace source.")
        private final boolean replacementAuthority;

        SearchRow(
                int ordinal,
                String stableId,
                String title,
                String platform,
                String shape,
                String status,
                String donorRepositories,
                int sourceImplementations,
                String searchRoot) {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            if (sourceImplementations < 1) {
                throw new IllegalArgumentException("sourceImplementations");
            }
            this.ordinal = ordinal;
            this.stableId = required(stableId, "stableId");
            this.title = required(title, "title");
            this.platform = required(platform, "platform");
            this.shape = Objects.requireNonNull(shape, "shape");
            this.status = required(status, "status");
            this.donorRepositories = required(donorRepositories, "donorRepositories");
            if (searchRoot == null || !searchRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("searchRoot");
            }
            this.sourceImplementations = sourceImplementations;
            this.searchRoot = searchRoot;
            this.replacementAuthority = false;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public String getStableId() {
            return stableId;
        }

        public String getTitle() {
            return title;
        }

        public String getPlatform() {
            return platform;
        }

        public String getShape() {
            return shape;
        }

        public String getStatus() {
            return status;
        }

        public String getDonorRepositories() {
            return donorRepositories;
        }

        public int getSourceImplementations() {
            return sourceImplementations;
        }

        public String getSearchRoot() {
            return searchRoot;
        }

        public boolean isReplacementAuthority() {
            return replacementAuthority;
        }

        private static String required(String value, String field) {
            String checked = Objects.requireNonNull(value, field).strip();
            if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
                throw new IllegalArgumentException(field);
            }
            return checked;
        }
    }
}
