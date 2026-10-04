// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.donor.DonorMechanicalShapeCatalog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Content-addressed evidence joining one admitted LLM recipe crate to one mechanical donor review.
 *
 * <p>This record adds custody only. It never grants source mutation, donor source copy, replacement
 * or canonical promotion authority. Donor references absent from the mechanical catalogue remain
 * governed by the existing pinned-donor admission receipt.</p>
 */
public record M3MechanicalDonorRecipeCrateBinding(
        String taskId,
        String capabilityId,
        String shape,
        String crateRoot,
        String admissionRoot,
        String mechanicalCatalogueRoot,
        String mechanicalReviewRoot,
        List<String> reviewedRepositories,
        List<String> crateMechanicalRepositories,
        String root) {

    public M3MechanicalDonorRecipeCrateBinding {
        taskId = text(taskId, "taskId");
        capabilityId = text(capabilityId, "capabilityId");
        shape = text(shape, "shape");
        crateRoot = sha(crateRoot, "crateRoot");
        admissionRoot = sha(admissionRoot, "admissionRoot");
        mechanicalCatalogueRoot = sha(mechanicalCatalogueRoot, "mechanicalCatalogueRoot");
        mechanicalReviewRoot = sha(mechanicalReviewRoot, "mechanicalReviewRoot");
        reviewedRepositories = stable(reviewedRepositories);
        crateMechanicalRepositories = stable(crateMechanicalRepositories);

        if (!reviewedRepositories.containsAll(crateMechanicalRepositories)) {
            throw new IllegalArgumentException(
                    "selected mechanical review does not cover every mechanical crate donor");
        }

        String expected =
                digest(
                        "M3_MECHANICAL_DONOR_RECIPE_CRATE_BINDING_V1",
                        taskId,
                        capabilityId,
                        shape,
                        crateRoot,
                        admissionRoot,
                        mechanicalCatalogueRoot,
                        mechanicalReviewRoot,
                        String.join("\u001f", reviewedRepositories),
                        String.join("\u001f", crateMechanicalRepositories),
                        "sourceMutationAuthority=false",
                        "donorSourceCopyAuthority=false",
                        "replacementAuthority=false",
                        "promotionAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!root.equals(expected)) {
            throw new IllegalArgumentException("mechanical donor recipe-crate binding root mismatch");
        }
    }

    public static M3MechanicalDonorRecipeCrateBinding bind(
            M3LlmTaskRecipeCrate crate,
            M3LlmTaskAdmissionReceipt admission,
            String mechanicalShape) {
        M3LlmTaskRecipeCrate checkedCrate = Objects.requireNonNull(crate, "crate");
        M3LlmTaskAdmissionReceipt checkedAdmission =
                Objects.requireNonNull(admission, "admission");

        M3RecipeFirstInvariant.requireLlmTaskRecipeCrate(checkedCrate);
        String checkedCrateRoot = M3LlmTaskRecipeCrateCodec.root(checkedCrate);

        if (!checkedCrateRoot.equals(checkedAdmission.crateRoot())
                || !checkedCrate.taskId().equals(checkedAdmission.taskId())
                || !checkedCrate.capabilityId().equals(checkedAdmission.capabilityId())
                || !checkedCrate.requirementSha256().equals(checkedAdmission.requirementSha256())
                || !checkedCrate.catalogueSha256().equals(checkedAdmission.catalogueSha256())
                || checkedCrate.disposition() != checkedAdmission.disposition()
                || checkedAdmission.donorRepositoryCount()
                        != checkedCrate.donorReferences().size()
                || !checkedAdmission.requiredStages()
                        .equals(M3RecipeFirstInvariant.canonicalStages())
                || !checkedAdmission.recipeCrateIsWorkUnit()
                || !checkedAdmission.candidateOnly()
                || !checkedAdmission.serialPromotionRequired()
                || checkedAdmission.targetFileEditAuthority()
                || checkedAdmission.donorSourceCopyAuthority()
                || checkedAdmission.promotionAuthority()) {
            throw new IllegalArgumentException(
                    "admission receipt does not belong to the exact candidate-only recipe crate");
        }

        M3MechanicalDonorShapeReview.Receipt review =
                M3MechanicalDonorShapeReview.review(mechanicalShape);
        if (review.sourceCopyAuthority() || review.replacementAuthority()) {
            throw new IllegalStateException("mechanical review authority widened");
        }

        List<String> reviewed =
                review.donors().stream()
                        .map(M3MechanicalDonorShapeReview.DonorEvidence::repository)
                        .distinct()
                        .sorted()
                        .toList();

        List<String> mechanicalReferences =
                checkedCrate.donorReferences().stream()
                        .filter(
                                repository ->
                                        DonorMechanicalShapeCatalog.byRepository(repository)
                                                .isPresent())
                        .sorted()
                        .toList();

        if (!reviewed.containsAll(mechanicalReferences)) {
            throw new IllegalArgumentException(
                    "crate mechanical donors are not covered by selected shape " + review.shape());
        }

        M3MechanicalDonorRecipeCrateBinding binding =
                new M3MechanicalDonorRecipeCrateBinding(
                        checkedCrate.taskId(),
                        checkedCrate.capabilityId(),
                        review.shape(),
                        checkedCrateRoot,
                        checkedAdmission.root(),
                        review.catalogueRoot(),
                        review.reviewRoot(),
                        reviewed,
                        mechanicalReferences,
                        "");
        M3RecipeFirstInvariant.requireMechanicalDonorRecipeCrateBinding(binding);
        return binding;
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static List<String> stable(List<String> values) {
        return Objects.requireNonNull(values, "values").stream()
                .map(value -> text(value, "repository"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes =
                        Objects.requireNonNull(value, "digest value")
                                .getBytes(StandardCharsets.UTF_8);
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
