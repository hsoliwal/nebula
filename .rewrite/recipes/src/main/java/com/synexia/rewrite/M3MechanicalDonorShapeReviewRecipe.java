// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Read-only OpenRewrite surface for deterministic contest/native mechanical donor-shape evidence.
 */
public final class M3MechanicalDonorShapeReviewRecipe extends Recipe {
    @Option(
            displayName = "Mechanical shape",
            description = "ALL or one canonical mechanical donor shape.",
            example = "PAGE_CACHE",
            required = false)
    private final String shape;

    private final transient SummaryTable summaryTable = new SummaryTable(this);
    private final transient DonorTable donorTable = new DonorTable(this);

    public M3MechanicalDonorShapeReviewRecipe() {
        this("ALL");
    }

    @JsonCreator
    public M3MechanicalDonorShapeReviewRecipe(String shape) {
        String normalized =
                Objects.requireNonNullElse(shape, "ALL")
                        .strip()
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_');
        if (normalized.isEmpty()) normalized = "ALL";
        if (!normalized.equals("ALL")) M3MechanicalDonorShapeReview.review(normalized);
        this.shape = normalized;
    }

    @Override
    public String getDisplayName() {
        return "M3 mechanical donor shape review";
    }

    @Override
    public String getDescription() {
        return "Exports deterministic candidate-only donor evidence across contest, knowledge/reasoning, and "
                + "pinned native C/C++ systems before recipe-crate implementation.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "donor",
                "mechanical-shape",
                "recipe-first",
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

    public String getShape() {
        return shape;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public List<M3MechanicalDonorShapeReview.Receipt> plannedReceipts() {
        if (shape.equals("ALL")) return M3MechanicalDonorShapeReview.reviewAll();
        return List.of(M3MechanicalDonorShapeReview.review(shape));
    }

    public List<SummaryRow> summaryRows() {
        return plannedReceipts().stream().map(SummaryRow::new).toList();
    }

    public List<DonorRow> donorRows() {
        ArrayList<DonorRow> rows = new ArrayList<>();
        for (M3MechanicalDonorShapeReview.Receipt receipt : plannedReceipts()) {
            for (M3MechanicalDonorShapeReview.DonorEvidence donor : receipt.donors()) {
                rows.add(new DonorRow(receipt, donor));
            }
        }
        return List.copyOf(rows);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                String key = getClass().getName() + ".emitted." + shape;
                synchronized (context) {
                    if (Boolean.TRUE.equals(context.getMessage(key))) return tree;
                    context.putMessage(key, Boolean.TRUE);
                }
                summaryRows().forEach(row -> summaryTable.insertRow(context, row));
                donorRows().forEach(row -> donorTable.insertRow(context, row));
                return tree;
            }
        };
    }

    public static final class SummaryTable extends DataTable<SummaryRow> {
        SummaryTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 mechanical donor shape review summary",
                    "One content-addressed candidate-only review receipt per mechanical shape.");
        }
    }

    public static final class DonorTable extends DataTable<DonorRow> {
        DonorTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 mechanical donor shape comparison",
                    "Serial pinned donor evidence across contest and native systems.");
        }
    }

    public static final class SummaryRow {
        @Column(displayName = "Shape", description = "Canonical mechanical/problem shape.")
        private final String shape;

        @Column(displayName = "Donor count", description = "Pinned donor rows for this shape.")
        private final int donorCount;

        @Column(displayName = "Catalogue root", description = "Full mechanical catalogue SHA-256.")
        private final String catalogueRoot;

        @Column(displayName = "Review root", description = "Shape-specific review SHA-256.")
        private final String reviewRoot;

        @Column(displayName = "Source copy authority", description = "Always false.")
        private final boolean sourceCopyAuthority;

        @Column(displayName = "Replacement authority", description = "Always false.")
        private final boolean replacementAuthority;

        SummaryRow(M3MechanicalDonorShapeReview.Receipt receipt) {
            shape = receipt.shape();
            donorCount = receipt.donors().size();
            catalogueRoot = receipt.catalogueRoot();
            reviewRoot = receipt.reviewRoot();
            sourceCopyAuthority = false;
            replacementAuthority = false;
        }

        public String getShape() { return shape; }
        public int getDonorCount() { return donorCount; }
        public String getCatalogueRoot() { return catalogueRoot; }
        public String getReviewRoot() { return reviewRoot; }
        public boolean isSourceCopyAuthority() { return sourceCopyAuthority; }
        public boolean isReplacementAuthority() { return replacementAuthority; }
    }

    public static final class DonorRow {
        @Column(displayName = "Shape", description = "Canonical mechanical/problem shape.")
        private final String shape;

        @Column(displayName = "Ordinal", description = "Stable serial ordinal within the shape.")
        private final int ordinal;

        @Column(displayName = "Repository", description = "Pinned GitHub donor repository.")
        private final String repository;

        @Column(displayName = "Revision", description = "Pinned full Git revision.")
        private final String revision;

        @Column(displayName = "License", description = "Pinned donor license label.")
        private final String license;

        @Column(displayName = "License path", description = "Pinned donor license path.")
        private final String licensePath;

        @Column(displayName = "License blob", description = "Pinned donor license Git blob.")
        private final String licenseBlob;

        @Column(displayName = "Source class", description = "Contest/native/reasoning source class.")
        private final String sourceClass;

        @Column(displayName = "Family", description = "Mechanical donor family.")
        private final String family;

        @Column(displayName = "Decision", description = "Wrap/adapt/reference-only decision.")
        private final String decision;

        @Column(displayName = "Note", description = "Catalogue review note.")
        private final String note;

        @Column(displayName = "Row root", description = "Content-addressed donor row SHA-256.")
        private final String rowRoot;

        @Column(displayName = "Source copy authority", description = "Always false.")
        private final boolean sourceCopyAuthority;

        @Column(displayName = "Replacement authority", description = "Always false.")
        private final boolean replacementAuthority;

        DonorRow(
                M3MechanicalDonorShapeReview.Receipt receipt,
                M3MechanicalDonorShapeReview.DonorEvidence donor) {
            shape = receipt.shape();
            ordinal = donor.ordinal();
            repository = donor.repository();
            revision = donor.revision();
            license = donor.license();
            licensePath = donor.licensePath();
            licenseBlob = donor.licenseBlob();
            sourceClass = donor.sourceClass();
            family = donor.family();
            decision = donor.decision();
            note = donor.note();
            rowRoot = donor.rowRoot();
            sourceCopyAuthority = false;
            replacementAuthority = false;
        }

        public String getShape() { return shape; }
        public int getOrdinal() { return ordinal; }
        public String getRepository() { return repository; }
        public String getRevision() { return revision; }
        public String getLicense() { return license; }
        public String getLicensePath() { return licensePath; }
        public String getLicenseBlob() { return licenseBlob; }
        public String getSourceClass() { return sourceClass; }
        public String getFamily() { return family; }
        public String getDecision() { return decision; }
        public String getNote() { return note; }
        public String getRowRoot() { return rowRoot; }
        public boolean isSourceCopyAuthority() { return sourceCopyAuthority; }
        public boolean isReplacementAuthority() { return replacementAuthority; }
    }
}
