// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Full M3Scale authority receipt for a task-local, class-backed IOP operator recipe.
 *
 * <p>This receipt is deliberately non-authoritative for canonical mutation. It proves only that the
 * reusable recipe operator remains bound to the exact recipe-first work order, challenge/donor
 * evidence, canonical IOP class hooks, and complete FILE-to-UNIVERSE proof path. OpenRewrite still
 * produces candidates; serial M3 proof owns promotion.</p>
 */
public final class M3IopOperatorFullScaleAuthority {
    private static final String DOMAIN = "M3_IOP_OPERATOR_FULL_SCALE_AUTHORITY_V1";

    private static final List<String> PROBLEM_PLATFORMS = List.of(
            "LEETCODE",
            "HACKERRANK",
            "GEEKSFORGEEKS");

    public record Receipt(
            String capabilityId,
            String operatorId,
            String operatorWriterRoot,
            String mutationFenceRoot,
            String challengeSearchRoot,
            String donorReviewRoot,
            List<String> requiredRecipeFirstStages,
            List<String> requiredClassHookIds,
            List<String> fullScaleProofPath,
            List<String> problemPlatforms,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            boolean targetFileEditAuthority,
            boolean donorSourceCopyAuthority,
            boolean promotionAuthority,
            String root) {

        public Receipt {
            capabilityId = text(capabilityId, "capabilityId");
            operatorId = text(operatorId, "operatorId");
            operatorWriterRoot = sha(operatorWriterRoot, "operatorWriterRoot");
            mutationFenceRoot = sha(mutationFenceRoot, "mutationFenceRoot");
            challengeSearchRoot = sha(challengeSearchRoot, "challengeSearchRoot");
            donorReviewRoot = sha(donorReviewRoot, "donorReviewRoot");
            requiredRecipeFirstStages = List.copyOf(
                    Objects.requireNonNull(requiredRecipeFirstStages, "requiredRecipeFirstStages"));
            requiredClassHookIds = stable(requiredClassHookIds);
            fullScaleProofPath = List.copyOf(
                    Objects.requireNonNull(fullScaleProofPath, "fullScaleProofPath"));
            problemPlatforms = List.copyOf(
                    Objects.requireNonNull(problemPlatforms, "problemPlatforms"));

            if (!requiredRecipeFirstStages.equals(M3RecipeFirstInvariant.canonicalStages())) {
                throw new IllegalArgumentException("operator escaped canonical recipe-first stage order");
            }
            if (!requiredClassHookIds.equals(M3IopRecipeMutationFence.canonicalClassHookIds())) {
                throw new IllegalArgumentException("operator escaped canonical IOP class hooks");
            }
            if (!fullScaleProofPath.equals(M3IopOperatorFullScaleAuthority.fullScaleProofPath())) {
                throw new IllegalArgumentException("operator escaped FILE-to-UNIVERSE M3 proof path");
            }
            if (!problemPlatforms.equals(M3IopOperatorFullScaleAuthority.problemPlatforms())) {
                throw new IllegalArgumentException("operator escaped canonical challenge-platform catalogue");
            }
            if (!candidateOnly || !serialPromotionRequired) {
                throw new IllegalArgumentException(
                        "operator must remain candidate-only with serial promotion");
            }
            if (targetFileEditAuthority || donorSourceCopyAuthority || promotionAuthority) {
                throw new IllegalArgumentException(
                        "operator full-scale receipt cannot own target-file edit, donor-copy, or promotion authority");
            }

            String expected = digest(
                    DOMAIN,
                    capabilityId,
                    operatorId,
                    operatorWriterRoot,
                    mutationFenceRoot,
                    challengeSearchRoot,
                    donorReviewRoot,
                    String.join("\u001f", requiredRecipeFirstStages),
                    String.join("\u001f", requiredClassHookIds),
                    String.join("\u001f", fullScaleProofPath),
                    String.join("\u001f", problemPlatforms),
                    Boolean.toString(candidateOnly),
                    Boolean.toString(serialPromotionRequired),
                    Boolean.toString(targetFileEditAuthority),
                    Boolean.toString(donorSourceCopyAuthority),
                    Boolean.toString(promotionAuthority));
            if (root == null || root.isBlank()) {
                root = expected;
            }
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("operator full-scale authority root mismatch");
            }
        }
    }

    private M3IopOperatorFullScaleAuthority() {}

    public static Receipt from(M3IopOperatorRecipeWriter.WrittenOperator written) {
        Objects.requireNonNull(written, "written");
        M3IopOperatorRecipeWriter.Receipt writer = written.receipt();
        M3IopRecipeMutationFence.Receipt fence = written.mutationAuthorityReceipt();

        if (!writer.candidateOnly()
                || !writer.serialPromotionRequired()
                || writer.targetFileEditAuthority()
                || writer.donorSourceCopyAuthority()
                || writer.promotionAuthority()) {
            throw new IllegalArgumentException("operator writer authority escaped M3 candidate-only fence");
        }
        if (!fence.candidateOnly()
                || !fence.serialPromotionRequired()
                || !fence.fullScaleProofRequired()
                || fence.targetFileEditAuthority()
                || fence.promotionAuthority()) {
            throw new IllegalArgumentException("operator mutation fence escaped full-scale M3 authority");
        }

        return new Receipt(
                writer.capabilityId(),
                writer.operatorId(),
                writer.root(),
                fence.root(),
                writer.challengeSearchRoot(),
                writer.donorReviewRoot(),
                writer.requiredStages(),
                writer.requiredClassHookIds(),
                fullScaleProofPath(),
                problemPlatforms(),
                true,
                true,
                false,
                false,
                false,
                "");
    }

    public static List<String> fullScaleProofPath() {
        return M3IopFullScaleMutationAuthority.fullScaleProofPath();
    }

    public static List<String> problemPlatforms() {
        return PROBLEM_PLATFORMS;
    }

    public static boolean writesCanonicalSource() {
        return false;
    }

    public static boolean copiesDonorSource() {
        return false;
    }

    public static boolean promotesCanonicalState() {
        return false;
    }

    private static List<String> stable(List<String> values) {
        return Objects.requireNonNull(values, "values").stream()
                .map(value -> text(value, "value"))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNullElse(value, "").strip();
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return checked;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.requireNonNull(value, "value")
                        .getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
