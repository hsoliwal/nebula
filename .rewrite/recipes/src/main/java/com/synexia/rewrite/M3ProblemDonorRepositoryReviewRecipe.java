// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.synexia.algorithms.corpus.DonorLicensePromotionPolicy;
import com.synexia.algorithms.corpus.PinnedAlgorithmDonorCatalog;
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
 * Candidate-only review of the authoritative pinned algorithm donor manifest.
 *
 * <p>This recipe deliberately reuses {@link PinnedAlgorithmDonorCatalog}; it does not maintain a
 * second donor list. Every row is bound to an immutable Git commit and reviewed license blob.
 * Promotion policy is reported as evidence only. Donor rows never grant direct source-copy,
 * replacement, target-file edit, or promotion authority.</p>
 */
public final class M3ProblemDonorRepositoryReviewRecipe extends Recipe {
    @Option(
            displayName = "Corpus kind",
            description = "ALL, LEETCODE, HACKERRANK, GEEKS_FOR_GEEKS/GFG, or GENERAL.",
            example = "HACKERRANK",
            required = false)
    private final String platform;

    @Option(
            displayName = "Permissive promotion candidates only",
            description =
                    "When true, retain only donors admitted by the existing conservative license policy.",
            example = "true",
            required = false)
    private final Boolean promotionReadyOnly;

    @Option(
            displayName = "Result limit",
            description = "Maximum deterministic donor rows.",
            example = "100",
            required = false)
    private final Integer limit;

    private final transient DonorTable donorTable = new DonorTable(this);

    public M3ProblemDonorRepositoryReviewRecipe() {
        this("ALL", false, 1000);
    }

    @JsonCreator
    public M3ProblemDonorRepositoryReviewRecipe(
            String platform, Boolean promotionReadyOnly, Integer limit) {
        this.platform = normalizePlatform(platform);
        this.promotionReadyOnly = Objects.requireNonNullElse(promotionReadyOnly, false);
        this.limit = Objects.requireNonNullElse(limit, 1000);
        if (this.limit < 1 || this.limit > 100_000) {
            throw new IllegalArgumentException("limit must be between 1 and 100000");
        }
    }

    @Override
    public String getDisplayName() {
        return "M3 pinned problem donor repository review";
    }

    @Override
    public String getDescription() {
        return "Exports the existing pinned LeetCode/HackerRank/GeeksForGeeks/general Java donor "
                + "manifest with exact revisions, license blobs and conservative promotion policy.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "recipe-first",
                "donor",
                "provenance",
                "pinned-git",
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
                    plannedRows().forEach(row -> donorTable.insertRow(context, row));
                }
                return tree;
            }
        };
    }

    public String getPlatform() {
        return platform;
    }

    public Boolean getPromotionReadyOnly() {
        return promotionReadyOnly;
    }

    public Integer getLimit() {
        return limit;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    public String catalogueRoot() {
        return PinnedAlgorithmDonorCatalog.snapshot().root();
    }

    /** Stable rows projected from the one authoritative donor manifest. */
    public List<DonorRow> plannedRows() {
        List<PinnedAlgorithmDonorCatalog.Pin> candidates =
                platform.equals("ALL")
                        ? PinnedAlgorithmDonorCatalog.all()
                        : PinnedAlgorithmDonorCatalog.byKind(
                                PinnedAlgorithmDonorCatalog.CorpusKind.valueOf(platform));
        ArrayList<DonorRow> rows = new ArrayList<>();
        int ordinal = 0;
        for (PinnedAlgorithmDonorCatalog.Pin pin : candidates) {
            DonorLicensePromotionPolicy.Assessment assessment =
                    DonorLicensePromotionPolicy.assess(pin.license());
            if (promotionReadyOnly && !assessment.automaticCodePromotionAllowed()) {
                continue;
            }
            rows.add(
                    new DonorRow(
                            ordinal++,
                            pin.kind().name(),
                            pin.repository(),
                            pin.repositoryUrl(),
                            pin.revision(),
                            pin.license(),
                            pin.licenseFile(),
                            pin.licenseBlob(),
                            assessment.decision().name(),
                            assessment.rationale(),
                            assessment.automaticCodePromotionAllowed(),
                            PinnedAlgorithmDonorCatalog.snapshot().root()));
            if (rows.size() >= limit) break;
        }
        return List.copyOf(rows);
    }

    private boolean claimEmission(ExecutionContext context) {
        String key =
                M3ProblemDonorRepositoryReviewRecipe.class.getName()
                        + ".emitted."
                        + platform
                        + "."
                        + promotionReadyOnly
                        + "."
                        + limit;
        synchronized (context) {
            if (Boolean.TRUE.equals(context.getMessage(key))) return false;
            context.putMessage(key, Boolean.TRUE);
            return true;
        }
    }

    private static String normalizePlatform(String value) {
        String normalized =
                Objects.requireNonNullElse(value, "ALL")
                        .strip()
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_');
        if (normalized.isEmpty()) normalized = "ALL";
        if (normalized.equals("GFG") || normalized.equals("GEEKSFORGEEKS")) {
            normalized = "GEEKS_FOR_GEEKS";
        }
        if (normalized.equals("ALL")) return normalized;
        try {
            return PinnedAlgorithmDonorCatalog.CorpusKind.valueOf(normalized).name();
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("unsupported donor corpus kind: " + value, invalid);
        }
    }

    public static final class DonorTable extends DataTable<DonorRow> {
        DonorTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 pinned algorithm donor review",
                    "Authoritative pinned Git donor provenance and conservative license policy.");
        }
    }

    public static final class DonorRow {
        @Column(displayName = "Ordinal", description = "Stable review ordinal.")
        private final int ordinal;

        @Column(displayName = "Corpus kind", description = "LEETCODE, HACKERRANK, GEEKS_FOR_GEEKS, or GENERAL.")
        private final String corpusKind;

        @Column(displayName = "Repository", description = "GitHub owner/name.")
        private final String repository;

        @Column(displayName = "URL", description = "Canonical GitHub repository URL.")
        private final String url;

        @Column(displayName = "Revision", description = "Pinned full Git commit.")
        private final String revision;

        @Column(displayName = "Declared license", description = "Reviewed donor license.")
        private final String license;

        @Column(displayName = "License file", description = "License path bound to the pin.")
        private final String licenseFile;

        @Column(displayName = "License blob", description = "Exact Git blob SHA of the reviewed license file.")
        private final String licenseBlob;

        @Column(displayName = "Promotion decision", description = "Existing conservative donor-license policy decision.")
        private final String promotionDecision;

        @Column(displayName = "Promotion rationale", description = "Existing conservative policy rationale.")
        private final String promotionRationale;

        @Column(displayName = "Automatic promotion candidate", description = "License-policy result only; proof gates still apply.")
        private final boolean automaticPromotionCandidate;

        @Column(displayName = "Manifest root", description = "SHA-256 identity of the complete authoritative donor manifest.")
        private final String manifestRoot;

        @Column(displayName = "Replacement authority", description = "Always false.")
        private final boolean replacementAuthority;

        DonorRow(
                int ordinal,
                String corpusKind,
                String repository,
                String url,
                String revision,
                String license,
                String licenseFile,
                String licenseBlob,
                String promotionDecision,
                String promotionRationale,
                boolean automaticPromotionCandidate,
                String manifestRoot) {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            this.ordinal = ordinal;
            this.corpusKind = required(corpusKind, "corpusKind");
            this.repository = required(repository, "repository");
            this.url = required(url, "url");
            this.revision = required(revision, "revision");
            this.license = required(license, "license");
            this.licenseFile = required(licenseFile, "licenseFile");
            this.licenseBlob = required(licenseBlob, "licenseBlob");
            this.promotionDecision = required(promotionDecision, "promotionDecision");
            this.promotionRationale = required(promotionRationale, "promotionRationale");
            this.automaticPromotionCandidate = automaticPromotionCandidate;
            if (manifestRoot == null || !manifestRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("manifestRoot");
            }
            this.manifestRoot = manifestRoot;
            this.replacementAuthority = false;
        }

        public int getOrdinal() { return ordinal; }
        public String getCorpusKind() { return corpusKind; }
        public String getRepository() { return repository; }
        public String getUrl() { return url; }
        public String getRevision() { return revision; }
        public String getLicense() { return license; }
        public String getLicenseFile() { return licenseFile; }
        public String getLicenseBlob() { return licenseBlob; }
        public String getPromotionDecision() { return promotionDecision; }
        public String getPromotionRationale() { return promotionRationale; }
        public boolean isAutomaticPromotionCandidate() { return automaticPromotionCandidate; }
        public String getManifestRoot() { return manifestRoot; }
        public boolean isReplacementAuthority() { return replacementAuthority; }

        private static String required(String value, String field) {
            String checked = Objects.toString(value, "").strip();
            if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
                throw new IllegalArgumentException(field);
            }
            return checked;
        }
    }
}
