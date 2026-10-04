// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.corpus.ChallengePlatform;
import com.synexia.fastsearch.problem.ProblemCategory;
import com.synexia.fastsearch.problem.ProblemDescriptor;
import com.synexia.m3.search.CompetitiveProblemReview;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * A task-bound, fixed-order review of independently indexed public problem metadata.
 *
 * <p>Each site is reviewed even when its category has no matching problems. This is a search
 * transcript for recipe selection, never an equivalence proof or donor-source license.</p>
 */
public record M3CompetitiveProblemCrateEvidence(
        M3LlmTaskRecipeCratePlanner.ProblemEvidence problemEvidence,
        CompetitiveProblemReview.ProblemEvidenceReview review,
        String root) {
    private static final List<ChallengePlatform> ORDER = List.of(
            ChallengePlatform.LEETCODE,
            ChallengePlatform.HACKERRANK,
            ChallengePlatform.GEEKSFORGEEKS);

    public M3CompetitiveProblemCrateEvidence {
        problemEvidence = Objects.requireNonNull(problemEvidence, "problemEvidence");
        review = Objects.requireNonNull(review, "review");
        if (!review.lanes().stream()
                .map(CompetitiveProblemReview.ProblemEvidenceLane::platform)
                .toList().equals(ORDER)) {
            throw new IllegalArgumentException("competitive problem review order");
        }
        final var checkedReview = review;
        if (review.lanes().stream().anyMatch(lane ->
                !checkedReview.catalogueRoot().equals(lane.catalogueRoot())
                        || lane.category() != checkedReview.category()
                        || !lane.query().equals(checkedReview.query()))) {
            throw new IllegalArgumentException("competitive problem review drift");
        }
        String expected = digest(
                "M3_COMPETITIVE_PROBLEM_CRATE_EVIDENCE_V1",
                M3LlmTaskRecipeCrateCodec.root(problemEvidence.crate()),
                problemEvidence.root(),
                review.root(),
                review.catalogueRoot(),
                "sourceCopyAuthority=false",
                "replacementAuthority=false",
                "promotionAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root);
        if (!expected.equals(root)) {
            throw new IllegalArgumentException("competitive problem evidence root mismatch");
        }
    }

    public ProblemCategory category() {
        return review.category();
    }

    public boolean sourceCopyAuthority() { return false; }
    public boolean replacementAuthority() { return false; }
    public boolean promotionAuthority() { return false; }

    /** One row per problem, or an explicit empty lane, preserving the serial site order. */
    public String tsv() {
        StringBuilder out = new StringBuilder(
                "pass\tplatform\tcategory\tquery\tlaneMatches\tproblemKey\turl\tlaneRoot\tcatalogueRoot\treviewRoot\n");
        for (int pass = 0; pass < ORDER.size(); pass++) {
            CompetitiveProblemReview.ProblemEvidenceLane lane = review.lanes().get(pass);
            if (lane.problems().isEmpty()) {
                row(out, pass + 1, lane, "", "");
            } else {
                for (ProblemDescriptor problem : lane.problems()) {
                    row(out, pass + 1, lane, problem.canonicalKey(), problem.uri().toString());
                }
            }
        }
        return out.toString();
    }

    private void row(
            StringBuilder out, int pass,
            CompetitiveProblemReview.ProblemEvidenceLane lane, String key, String url) {
        out.append(pass).append('\t')
                .append(lane.platform().name()).append('\t')
                .append(lane.category().name()).append('\t')
                .append(cell(lane.query())).append('\t')
                .append(lane.problems().size()).append('\t')
                .append(cell(key)).append('\t')
                .append(cell(url)).append('\t')
                .append(lane.root()).append('\t')
                .append(lane.catalogueRoot()).append('\t')
                .append(review.root()).append('\n');
    }

    private static String cell(String value) {
        if (value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("problem metadata is not a single TSV cell");
        }
        return value;
    }

    private static String sha(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("competitive evidence root");
        }
        return value;
    }

    private static String digest(String... values) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.requireNonNull(value, "digest field")
                        .getBytes(StandardCharsets.UTF_8);
                hash.update((byte) (bytes.length >>> 24));
                hash.update((byte) (bytes.length >>> 16));
                hash.update((byte) (bytes.length >>> 8));
                hash.update((byte) bytes.length);
                hash.update(bytes);
            }
            return HexFormat.of().formatHex(hash.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
