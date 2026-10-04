// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.synexia.algorithms.corpus.ChallengeDonorReviewPlan;
import com.synexia.algorithms.corpus.ChallengePlatform;
import com.synexia.fastsearch.problem.ProblemCatalogue;
import com.synexia.fastsearch.problem.ProblemCatalogueTsv;
import com.synexia.fastsearch.problem.ProblemDescriptor;
import com.synexia.fastsearch.problem.ProblemSource;
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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/**
 * Candidate-only M3 recipe that exports the serial challenge/native donor review as a DataTable.
 *
 * <p>The visitor returns every input tree unchanged. This recipe is a repeatable review/evidence
 * producer, not a source transformation and not replacement/native-execution authority.</p>
 */
public final class M3ChallengeDonorReviewRecipe extends Recipe {
    public enum Scope {
        BOTH,
        CATEGORIES,
        FILES,
        PROBLEMS
    }

    @Option(
            displayName = "Challenge platform",
            description =
                    "ALL, LEETCODE, HACKERRANK, or GEEKSFORGEEKS. "
                            + "Filters the existing pinned challenge inventory only.",
            example = "LEETCODE",
            required = false)
    private final String platform;

    @Option(
            displayName = "Review scope",
            description = "BOTH, CATEGORIES, FILES, or PROBLEMS.",
            example = "FILES",
            required = false)
    private final String scope;

    private final transient ReviewTable reviewTable = new ReviewTable(this);
    private final transient ProblemReviewTable problemReviewTable = new ProblemReviewTable(this);

    public M3ChallengeDonorReviewRecipe() {
        this("ALL", "BOTH");
    }

    @JsonCreator
    public M3ChallengeDonorReviewRecipe(String platform, String scope) {
        this.platform = normalizePlatform(platform);
        this.scope = normalizeScope(scope).name();
    }

    @Override
    public String getDisplayName() {
        return "M3 challenge/native donor serial review";
    }

    @Override
    public String getDescription() {
        return "Exports candidate-only serial LeetCode, HackerRank, and GeeksforGeeks review "
                + "evidence joining challenge categories, canonical Java shapes, existing "
                + "Synexia primitives, pinned native donors, and metadata-only problem rows.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "donor",
                "challenge",
                "leetcode",
                "hackerrank",
                "geeksforgeeks",
                "review",
                "candidate-only",
                "class-backed");
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
                Scope selectedScope = Scope.valueOf(scope);
                if (selectedScope == Scope.PROBLEMS) {
                    if (tree instanceof SourceFile sourceFile
                            && isProblemCataloguePath(sourceFile)) {
                        for (ProblemReviewRow row : problemRows(sourceFile.printAll())) {
                            problemReviewTable.insertRow(context, row);
                        }
                    }
                    return tree;
                }
                if (claimEmission(context)) {
                    for (ReviewRow row : plannedRows()) {
                        reviewTable.insertRow(context, row);
                    }
                }
                return tree;
            }
        };
    }

    private boolean claimEmission(ExecutionContext context) {
        String key =
                M3ChallengeDonorReviewRecipe.class.getName()
                        + ".emitted."
                        + platform
                        + "."
                        + scope;
        synchronized (context) {
            Boolean already = context.getMessage(key);
            if (Boolean.TRUE.equals(already)) {
                return false;
            }
            context.putMessage(key, Boolean.TRUE);
            return true;
        }
    }

    public String getPlatform() {
        return platform;
    }

    public String getScope() {
        return scope;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public String reviewRoot() {
        if (Scope.valueOf(scope) == Scope.PROBLEMS) {
            throw new IllegalStateException(
                    "PROBLEMS scope is bound to an input problem-catalogue.tsv");
        }
        return selectedReport().root();
    }

    /** Deterministic SHA-256 identity of a strict metadata-only problem catalogue. */
    public String problemReviewRoot(String tsv) {
        return new ProblemCatalogue(ProblemCatalogueTsv.parse(tsv)).root();
    }

    /**
     * Strict metadata-only problem rows in fixed LeetCode -> HackerRank -> GeeksforGeeks order.
     * No statement, editorial, test, answer, or solution body is accepted by the TSV codec.
     */
    public List<ProblemReviewRow> problemRows(String tsv) {
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException(
                    "M3 problem catalogue review interrupted");
        }
        ProblemCatalogue catalogue =
                new ProblemCatalogue(ProblemCatalogueTsv.parse(tsv));
        List<ProblemSource> sources = platform.equals("ALL")
                ? List.of(
                        ProblemSource.LEETCODE,
                        ProblemSource.HACKERRANK,
                        ProblemSource.GEEKSFORGEEKS)
                : List.of(problemSource(ChallengePlatform.valueOf(platform)));
        ArrayList<ProblemReviewRow> rows = new ArrayList<>();
        int ordinal = 0;
        for (ProblemSource source : sources) {
            for (ProblemDescriptor problem : catalogue.bySource(source)) {
                rows.add(new ProblemReviewRow(
                        ordinal++,
                        problem.source().name(),
                        problem.externalId(),
                        problem.title(),
                        problem.uri().toString(),
                        problem.categories().stream()
                                .map(Enum::name)
                                .sorted()
                                .reduce("", M3ChallengeDonorReviewRecipe::comma),
                        problem.asymptoticTarget(),
                        problem.evidenceOnly(),
                        problem.licenseNote(),
                        catalogue.root()));
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException(
                    "M3 problem catalogue review interrupted");
        }
        return List.copyOf(rows);
    }

    /** Deterministic row projection used by both the OpenRewrite DataTable and focused tests. */
    public List<ReviewRow> plannedRows() {
        Scope selectedScope = Scope.valueOf(scope);
        if (selectedScope == Scope.PROBLEMS) {
            return List.of();
        }
        ChallengeDonorReviewPlan.Report report = selectedReport();
        ArrayList<ReviewRow> rows = new ArrayList<>(
                report.categories().size() + report.files().size());

        if (selectedScope != Scope.FILES) {
            for (ChallengeDonorReviewPlan.CategoryRow row : report.categories()) {
                rows.add(
                        ReviewRow.category(
                                row.ordinal(),
                                row.platform().name(),
                                row.categoryId(),
                                row.shapes().stream()
                                        .map(Enum::name)
                                        .reduce("", M3ChallengeDonorReviewRecipe::comma),
                                String.join(",", row.localPrimitiveIds()),
                                String.join(",", row.nativeDonors()),
                                String.join(",", row.nativeMechanics()),
                                row.root()));
            }
        }

        if (selectedScope != Scope.CATEGORIES) {
            for (ChallengeDonorReviewPlan.FileRow row : report.files()) {
                rows.add(
                        ReviewRow.file(
                                row.ordinal(),
                                row.adapterId(),
                                row.platform().name(),
                                row.category().name(),
                                row.shape(),
                                String.join(",", row.platformCategoryIds()),
                                String.join(",", row.localPrimitiveIds()),
                                String.join(",", row.nativeDonors()),
                                String.join(",", row.nativeMechanics()),
                                row.nativeLane().name(),
                                row.disposition().name(),
                                row.rationale(),
                                row.root()));
            }
        }
        return List.copyOf(rows);
    }

    private ChallengeDonorReviewPlan.Report selectedReport() {
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("M3 donor review interrupted");
        }
        ChallengePlatform selected =
                platform.equals("ALL") ? null : ChallengePlatform.valueOf(platform);
        ChallengeDonorReviewPlan.Report report =
                ChallengeDonorReviewPlan.review(selected, null);
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("M3 donor review interrupted");
        }
        return report;
    }

    private static boolean isProblemCataloguePath(SourceFile sourceFile) {
        String path =
                sourceFile.getSourcePath().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return path.equals("problem-catalogue.tsv")
                || path.endsWith("/problem-catalogue.tsv");
    }

    private static ProblemSource problemSource(ChallengePlatform platform) {
        return switch (Objects.requireNonNull(platform, "platform")) {
            case LEETCODE -> ProblemSource.LEETCODE;
            case HACKERRANK -> ProblemSource.HACKERRANK;
            case GEEKSFORGEEKS -> ProblemSource.GEEKSFORGEEKS;
            case OTHER -> throw new IllegalArgumentException(
                    "OTHER has no public problem catalogue lane");
        };
    }

    private static String normalizePlatform(String value) {
        String normalized = Objects.requireNonNullElse(value, "ALL").strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) normalized = "ALL";
        if (normalized.equals("GFG")) normalized = "GEEKSFORGEEKS";
        if (!normalized.equals("ALL")) {
            ChallengePlatform parsed = ChallengePlatform.from(normalized);
            if (parsed == ChallengePlatform.OTHER) {
                throw new IllegalArgumentException("unsupported challenge platform: " + value);
            }
            normalized = parsed.name();
        }
        return normalized;
    }

    private static Scope normalizeScope(String value) {
        String normalized = Objects.requireNonNullElse(value, "BOTH").strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) normalized = "BOTH";
        try {
            return Scope.valueOf(normalized);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(
                    "scope must be BOTH, CATEGORIES, FILES, or PROBLEMS", invalid);
        }
    }

    private static String comma(String left, String right) {
        return left.isEmpty() ? right : left + "," + right;
    }

    /** OpenRewrite export surface. Every row remains candidate-only. */
    public static final class ReviewTable extends DataTable<ReviewRow> {
        ReviewTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 challenge/native donor review",
                    "Serial candidate-only review joining challenge corpus shapes, "
                            + "local primitives, and native mechanics donors.");
        }
    }

    /** Metadata-only problem catalogue export; every row remains evidence only. */
    public static final class ProblemReviewTable extends DataTable<ProblemReviewRow> {
        ProblemReviewTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 problem catalogue review",
                    "Strict metadata-only LeetCode/HackerRank/GeeksforGeeks evidence. "
                            + "No problem or solution bodies are admitted.");
        }
    }

    public static final class ProblemReviewRow {
        @Column(displayName = "Ordinal", description = "Fixed serial review ordinal.")
        private final int ordinal;

        @Column(displayName = "Source", description = "LEETCODE, HACKERRANK, or GEEKSFORGEEKS.")
        private final String source;

        @Column(displayName = "External ID", description = "Public problem metadata identifier.")
        private final String externalId;

        @Column(displayName = "Title", description = "Problem title metadata only.")
        private final String title;

        @Column(displayName = "URL", description = "Host-validated public metadata URL.")
        private final String url;

        @Column(displayName = "Categories", description = "Normalized Synexia problem categories.")
        private final String categories;

        @Column(displayName = "Asymptotic target", description = "Optional metadata target.")
        private final String asymptoticTarget;

        @Column(
                displayName = "Evidence only",
                description = "Must be true for the three public challenge sites.")
        private final boolean evidenceOnly;

        @Column(displayName = "License note", description = "Metadata/source rights note.")
        private final String licenseNote;

        @Column(
                displayName = "Catalogue root",
                description = "SHA-256 identity of the complete admitted metadata snapshot.")
        private final String catalogueRoot;

        @Column(
                displayName = "Replacement authority",
                description = "Always false; problem similarity never authorizes source replacement.")
        private final boolean replacementAuthority;

        private ProblemReviewRow(
                int ordinal,
                String source,
                String externalId,
                String title,
                String url,
                String categories,
                String asymptoticTarget,
                boolean evidenceOnly,
                String licenseNote,
                String catalogueRoot) {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            this.ordinal = ordinal;
            this.source = ReviewRow.required(source, "source");
            this.externalId = ReviewRow.required(externalId, "externalId");
            this.title = ReviewRow.required(title, "title");
            this.url = ReviewRow.required(url, "url");
            this.categories = Objects.requireNonNull(categories, "categories");
            this.asymptoticTarget =
                    Objects.requireNonNull(asymptoticTarget, "asymptoticTarget");
            if (!evidenceOnly) {
                throw new IllegalArgumentException("public problem row must remain evidence-only");
            }
            this.evidenceOnly = true;
            this.licenseNote = ReviewRow.required(licenseNote, "licenseNote");
            if (catalogueRoot == null || !catalogueRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("catalogueRoot");
            }
            this.catalogueRoot = catalogueRoot;
            this.replacementAuthority = false;
        }

        public int getOrdinal() { return ordinal; }
        public String getSource() { return source; }
        public String getExternalId() { return externalId; }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getCategories() { return categories; }
        public String getAsymptoticTarget() { return asymptoticTarget; }
        public boolean isEvidenceOnly() { return evidenceOnly; }
        public String getLicenseNote() { return licenseNote; }
        public String getCatalogueRoot() { return catalogueRoot; }
        public boolean isReplacementAuthority() { return replacementAuthority; }
    }

    public static final class ReviewRow {
        @Column(displayName = "Scope", description = "CATEGORY or FILE.")
        private final String scope;

        @Column(displayName = "Ordinal", description = "Stable serial ordinal within this scope.")
        private final int ordinal;

        @Column(displayName = "Identity", description = "Category ID or pinned file adapter ID.")
        private final String identity;

        @Column(displayName = "Platform", description = "Challenge platform.")
        private final String platform;

        @Column(
                displayName = "Canonical category",
                description = "Cross-site canonical category for file rows; blank for taxonomy rows.")
        private final String canonicalCategory;

        @Column(
                displayName = "Shape",
                description = "Canonical shape or comma-separated taxonomy shape candidates.")
        private final String shape;

        @Column(
                displayName = "Platform categories",
                description = "Platform taxonomy categories supporting a file's classified shape.")
        private final String platformCategories;

        @Column(
                displayName = "Local primitives",
                description = "Already registered executable Synexia primitive IDs.")
        private final String localPrimitives;

        @Column(
                displayName = "Native donors",
                description = "Pinned native donor repository@revision evidence.")
        private final String nativeDonors;

        @Column(
                displayName = "Native mechanics",
                description = "Matched mechanics from the existing native donor catalogue.")
        private final String nativeMechanics;

        @Column(
                displayName = "Native lane",
                description = "Existing ProblemOptimizationCatalog native-lane policy.")
        private final String nativeLane;

        @Column(
                displayName = "Disposition",
                description = "Review disposition; never source replacement authority.")
        private final String disposition;

        @Column(
                displayName = "Rationale",
                description = "Deterministic rationale for the review disposition.")
        private final String rationale;

        @Column(displayName = "Review root", description = "SHA-256 identity of this review row.")
        private final String reviewRoot;

        @Column(
                displayName = "Replacement authority",
                description = "Always false; later M3 proof gates own mutation admission.")
        private final boolean replacementAuthority;

        private ReviewRow(
                String scope,
                int ordinal,
                String identity,
                String platform,
                String canonicalCategory,
                String shape,
                String platformCategories,
                String localPrimitives,
                String nativeDonors,
                String nativeMechanics,
                String nativeLane,
                String disposition,
                String rationale,
                String reviewRoot) {
            this.scope = required(scope, "scope");
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            this.ordinal = ordinal;
            this.identity = required(identity, "identity");
            this.platform = required(platform, "platform");
            this.canonicalCategory = Objects.requireNonNull(canonicalCategory, "canonicalCategory");
            this.shape = Objects.requireNonNull(shape, "shape");
            this.platformCategories = Objects.requireNonNull(platformCategories, "platformCategories");
            this.localPrimitives = Objects.requireNonNull(localPrimitives, "localPrimitives");
            this.nativeDonors = Objects.requireNonNull(nativeDonors, "nativeDonors");
            this.nativeMechanics = Objects.requireNonNull(nativeMechanics, "nativeMechanics");
            this.nativeLane = Objects.requireNonNull(nativeLane, "nativeLane");
            this.disposition = Objects.requireNonNull(disposition, "disposition");
            this.rationale = Objects.requireNonNull(rationale, "rationale");
            if (reviewRoot == null || !reviewRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("reviewRoot");
            }
            this.reviewRoot = reviewRoot;
            this.replacementAuthority = false;
        }

        static ReviewRow category(
                int ordinal,
                String platform,
                String categoryId,
                String shapes,
                String localPrimitives,
                String nativeDonors,
                String nativeMechanics,
                String root) {
            return new ReviewRow(
                    "CATEGORY",
                    ordinal,
                    categoryId,
                    platform,
                    "",
                    shapes,
                    "",
                    localPrimitives,
                    nativeDonors,
                    nativeMechanics,
                    "",
                    "TAXONOMY_REVIEW",
                    "taxonomy/shape evidence only; no source replacement authority",
                    root);
        }

        static ReviewRow file(
                int ordinal,
                String adapterId,
                String platform,
                String canonicalCategory,
                String shape,
                String platformCategories,
                String localPrimitives,
                String nativeDonors,
                String nativeMechanics,
                String nativeLane,
                String disposition,
                String rationale,
                String root) {
            return new ReviewRow(
                    "FILE",
                    ordinal,
                    adapterId,
                    platform,
                    canonicalCategory,
                    shape,
                    platformCategories,
                    localPrimitives,
                    nativeDonors,
                    nativeMechanics,
                    nativeLane,
                    disposition,
                    rationale,
                    root);
        }

        public String getScope() { return scope; }
        public int getOrdinal() { return ordinal; }
        public String getIdentity() { return identity; }
        public String getPlatform() { return platform; }
        public String getCanonicalCategory() { return canonicalCategory; }
        public String getShape() { return shape; }
        public String getPlatformCategories() { return platformCategories; }
        public String getLocalPrimitives() { return localPrimitives; }
        public String getNativeDonors() { return nativeDonors; }
        public String getNativeMechanics() { return nativeMechanics; }
        public String getNativeLane() { return nativeLane; }
        public String getDisposition() { return disposition; }
        public String getRationale() { return rationale; }
        public String getReviewRoot() { return reviewRoot; }
        public boolean isReplacementAuthority() { return replacementAuthority; }

        private static String required(String value, String field) {
            String checked = Objects.requireNonNull(value, field).strip();
            if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
                throw new IllegalArgumentException(field);
            }
            return checked;
        }
    }
}
