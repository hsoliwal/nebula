// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.fastsearch.problem.ProblemCatalogue;
import com.synexia.fastsearch.problem.ProblemCategory;
import com.synexia.m3.search.CompetitiveProblemReview;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Turns an LLM task into a recipe-first M3 work crate by consulting the admitted recipe catalogue.
 */
public final class M3LlmTaskRecipeCratePlanner {
    public record ProblemEvidence(
            M3LlmTaskRecipeCrate crate,
            M3ChallengeCategorySearchEvidence categoryEvidence,
            M3PinnedDonorEvidence donorEvidence,
            String challengeSearchRoot,
            String root) {
        public ProblemEvidence {
            crate = Objects.requireNonNull(crate, "crate");
            categoryEvidence = Objects.requireNonNull(categoryEvidence, "categoryEvidence");
            donorEvidence = Objects.requireNonNull(donorEvidence, "donorEvidence");
            if (challengeSearchRoot == null || !challengeSearchRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("challengeSearchRoot");
            }
            String expected = digest(
                    M3LlmTaskRecipeCrateCodec.root(crate),
                    categoryEvidence.root(),
                    donorEvidence.root(),
                    challengeSearchRoot);
            root = root == null || root.isBlank() ? expected : root;
            if (!expected.equals(root)) throw new IllegalArgumentException("problem evidence root mismatch");
        }
    }

    public M3LlmTaskRecipeCrate plan(
            String taskId,
            String capabilityId,
            String requirementSha256,
            M3RecipeCatalogue catalogue,
            Set<String> targetPaths,
            Set<String> donorReferences) {
        Objects.requireNonNull(catalogue, "catalogue");
        M3RecipeCatalogue.Resolution resolution = catalogue.resolveForLlmTask(capabilityId);

        List<String> recipes = resolution.candidates().stream()
                .map(M3RecipeDescriptor::recipeClassName)
                .sorted()
                .toList();

        return new M3LlmTaskRecipeCrate(
                taskId,
                capabilityId,
                requirementSha256,
                catalogueDigest(catalogue),
                resolution.disposition()
                        == M3RecipeCatalogue.Disposition.REUSE_OR_COMPOSE_RECIPE
                        ? M3LlmTaskRecipeCrate.Disposition.REUSE_OR_COMPOSE_RECIPE
                        : M3LlmTaskRecipeCrate.Disposition.CREATE_OR_IMPROVE_RECIPE,
                recipes,
                targetPaths,
                donorReferences,
                Set.of(M3LlmTaskRecipeCrate.Gate.values()));
    }

    /**
     * Adds donor references from the existing precomputed challenge index before creating the
     * recipe crate. Challenge solution bodies are not read or copied.
     */
    public M3LlmTaskRecipeCrate planWithProblemEvidence(
            String taskId,
            String capabilityId,
            String requirementSha256,
            M3RecipeCatalogue catalogue,
            Set<String> targetPaths,
            Set<String> donorReferences,
            String platform,
            String shape,
            String category,
            String term,
            int limit) {
        return planWithProblemEvidenceDetailed(
                taskId, capabilityId, requirementSha256, catalogue, targetPaths, donorReferences,
                platform, shape, category, term, limit).crate();
    }

    public ProblemEvidence planWithProblemEvidenceDetailed(
            String taskId,
            String capabilityId,
            String requirementSha256,
            M3RecipeCatalogue catalogue,
            Set<String> targetPaths,
            Set<String> donorReferences,
            String platform,
            String shape,
            String category,
            String term,
            int limit) {
        M3ChallengeCategorySearchEvidence categoryEvidence =
                M3ChallengeCategorySearchEvidence.search(platform, category, limit);
        M3ChallengeFastSearchReviewRecipe search =
                new M3ChallengeFastSearchReviewRecipe(
                        platform, shape, category, term, "", limit);
        java.util.TreeSet<String> donors =
                new java.util.TreeSet<>(Objects.requireNonNull(donorReferences, "donorReferences"));
        search.plannedRows().stream()
                .flatMap(row -> java.util.Arrays.stream(row.getDonorRepositories().split(",")))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .forEach(donors::add);
        M3PinnedDonorEvidence donorEvidence = M3PinnedDonorEvidence.from(Set.copyOf(donors));
        M3LlmTaskRecipeCrate crate = plan(
                taskId, capabilityId, requirementSha256, catalogue, targetPaths, Set.copyOf(donors));
        return new ProblemEvidence(
                crate, categoryEvidence, donorEvidence, search.searchRoot(), "");
    }

    /**
     * Adds a fixed LeetCode, HackerRank, GeeksforGeeks metadata pass to the existing recipe
     * crate. The indexed catalogue is supplied by the caller and contributes its content root;
     * public problem pages and solution bodies are never fetched.
     */
    public M3CompetitiveProblemCrateEvidence planWithCompetitiveEvidenceDetailed(
            String taskId,
            String capabilityId,
            String requirementSha256,
            M3RecipeCatalogue catalogue,
            Set<String> targetPaths,
            Set<String> donorReferences,
            String platform,
            String shape,
            String category,
            String term,
            int limit,
            ProblemCatalogue problemCatalogue,
            ProblemCategory metadataCategory) {
        ProblemEvidence evidence = planWithProblemEvidenceDetailed(
                taskId, capabilityId, requirementSha256, catalogue, targetPaths,
                donorReferences, platform, shape, category, term, limit);
        CompetitiveProblemReview.ProblemEvidenceReview review =
                CompetitiveProblemReview.reviewProblems(
                        "m3.llm", Objects.requireNonNull(problemCatalogue, "problemCatalogue"),
                        Objects.requireNonNull(metadataCategory, "metadataCategory"), term, limit);
        return new M3CompetitiveProblemCrateEvidence(evidence, review, "");
    }

    public boolean directFileMutationAllowed() {
        return false;
    }

    public boolean canonicalPromotionAllowed() {
        return false;
    }

    private static String digest(String... values) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String catalogueDigest(M3RecipeCatalogue catalogue) {
        String canonical = catalogue.entries().stream()
                .map(entry -> entry.capabilityId()
                        + "|" + entry.recipeClassName()
                        + "|" + entry.recipeArtifactSha256()
                        + "|" + entry.fixtureRootSha256())
                .sorted()
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte value : bytes) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
