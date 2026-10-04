// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Pinned Git revision/license evidence for donor repositories nominated by a recipe task. */
public record M3PinnedDonorEvidence(
        List<Row> rows,
        List<String> unresolvedRepositories,
        String manifestRoot,
        String root) {

    public record Row(
            String corpusKind,
            String repository,
            String url,
            String revision,
            String license,
            String licenseFile,
            String licenseBlob,
            String promotionDecision,
            boolean automaticPromotionCandidate) {
        public Row {
            corpusKind = text(corpusKind, "corpusKind");
            repository = text(repository, "repository");
            url = text(url, "url");
            revision = text(revision, "revision");
            license = text(license, "license");
            licenseFile = text(licenseFile, "licenseFile");
            licenseBlob = text(licenseBlob, "licenseBlob");
            promotionDecision = text(promotionDecision, "promotionDecision");
        }
    }

    public M3PinnedDonorEvidence {
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        unresolvedRepositories = List.copyOf(
                Objects.requireNonNull(unresolvedRepositories, "unresolvedRepositories"));
        manifestRoot = sha(manifestRoot, "manifestRoot");
        String expected = digest(
                "M3_PINNED_DONOR_EVIDENCE_V1",
                manifestRoot,
                rows.stream()
                        .map(row -> row.repository() + "|" + row.revision() + "|" + row.licenseBlob()
                                + "|" + row.promotionDecision() + "|"
                                + row.automaticPromotionCandidate())
                        .reduce("", (a, b) -> a + b + "\n"),
                String.join("\u001f", unresolvedRepositories),
                "donorSourceCopyAuthority=false",
                "replacementAuthority=false",
                "promotionAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) throw new IllegalArgumentException("donor evidence root mismatch");
    }

    public static M3PinnedDonorEvidence from(Set<String> repositories) {
        TreeSet<String> requested = new TreeSet<>(
                Objects.requireNonNull(repositories, "repositories"));
        M3ProblemDonorRepositoryReviewRecipe review =
                new M3ProblemDonorRepositoryReviewRecipe("ALL", false, 100_000);
        ArrayList<Row> rows = new ArrayList<>();
        TreeSet<String> resolved = new TreeSet<>();
        for (M3ProblemDonorRepositoryReviewRecipe.DonorRow row : review.plannedRows()) {
            if (!requested.contains(row.getRepository())) continue;
            resolved.add(row.getRepository());
            rows.add(new Row(
                    row.getCorpusKind(),
                    row.getRepository(),
                    row.getUrl(),
                    row.getRevision(),
                    row.getLicense(),
                    row.getLicenseFile(),
                    row.getLicenseBlob(),
                    row.getPromotionDecision(),
                    row.isAutomaticPromotionCandidate()));
        }
        rows.sort(Comparator.comparing(Row::repository).thenComparing(Row::revision));
        requested.removeAll(resolved);
        return new M3PinnedDonorEvidence(
                rows, List.copyOf(requested), review.catalogueRoot(), "");
    }

    public boolean allRepositoriesPinned() { return unresolvedRepositories.isEmpty(); }
    public boolean donorSourceCopyAuthority() { return false; }
    public boolean replacementAuthority() { return false; }
    public boolean promotionAuthority() { return false; }

    public String tsv() {
        StringBuilder out = new StringBuilder(
                "repository\tcorpusKind\trevision\tlicense\tlicenseFile\tlicenseBlob\tpromotionDecision\tautomaticPromotionCandidate\n");
        for (Row row : rows) {
            out.append(row.repository()).append('\t')
                    .append(row.corpusKind()).append('\t')
                    .append(row.revision()).append('\t')
                    .append(row.license()).append('\t')
                    .append(row.licenseFile()).append('\t')
                    .append(row.licenseBlob()).append('\t')
                    .append(row.promotionDecision()).append('\t')
                    .append(row.automaticPromotionCandidate()).append('\n');
        }
        for (String unresolved : unresolvedRepositories) {
            out.append(unresolved).append("\tUNRESOLVED\t\t\t\t\tHOLD\tfalse\n");
        }
        return out.toString();
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0 || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return value;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
