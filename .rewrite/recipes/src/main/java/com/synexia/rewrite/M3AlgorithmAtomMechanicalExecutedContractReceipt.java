// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Additive custody receipt preserving mechanical donor evidence through executed Java contract proof.
 *
 * <p>The existing {@link M3AlgorithmAtomExecutedContractReceipt} remains the Java contract authority.
 * This receipt binds that proof to the exact source-bound algorithm atom and, when the shape is
 * catalogued, to the exact mechanical donor binding that was required before execution.</p>
 */
public record M3AlgorithmAtomMechanicalExecutedContractReceipt(
        AlgorithmShape shape,
        String algorithmAtomBindingRoot,
        String atomRoot,
        String crateRoot,
        String sourceBoundCandidateRoot,
        String executedContractRoot,
        boolean mechanicalReviewRequired,
        String mechanicalBindingRoot,
        String mechanicalCatalogueRoot,
        String mechanicalReviewRoot,
        String nativeDonorReviewRoot,
        String root) {

    public M3AlgorithmAtomMechanicalExecutedContractReceipt {
        shape = Objects.requireNonNull(shape, "shape");
        algorithmAtomBindingRoot = sha(algorithmAtomBindingRoot, "algorithmAtomBindingRoot");
        atomRoot = sha(atomRoot, "atomRoot");
        crateRoot = sha(crateRoot, "crateRoot");
        sourceBoundCandidateRoot = sha(sourceBoundCandidateRoot, "sourceBoundCandidateRoot");
        executedContractRoot = sha(executedContractRoot, "executedContractRoot");

        boolean required =
                M3AlgorithmAtomMechanicalDonorBinding.isMechanicalReviewRequired(shape);
        if (mechanicalReviewRequired != required) {
            throw new IllegalArgumentException("mechanical review requirement drift");
        }

        if (mechanicalReviewRequired) {
            mechanicalBindingRoot = sha(mechanicalBindingRoot, "mechanicalBindingRoot");
            mechanicalCatalogueRoot = sha(mechanicalCatalogueRoot, "mechanicalCatalogueRoot");
            mechanicalReviewRoot = sha(mechanicalReviewRoot, "mechanicalReviewRoot");
            nativeDonorReviewRoot = sha(nativeDonorReviewRoot, "nativeDonorReviewRoot");
        } else {
            mechanicalBindingRoot = none(mechanicalBindingRoot, "mechanicalBindingRoot");
            mechanicalCatalogueRoot = none(mechanicalCatalogueRoot, "mechanicalCatalogueRoot");
            mechanicalReviewRoot = none(mechanicalReviewRoot, "mechanicalReviewRoot");
            nativeDonorReviewRoot = none(nativeDonorReviewRoot, "nativeDonorReviewRoot");
        }

        String expected =
                digest(
                        "M3_ALGORITHM_ATOM_MECHANICAL_EXECUTED_CONTRACT_V1",
                        shape.name(),
                        algorithmAtomBindingRoot,
                        atomRoot,
                        crateRoot,
                        sourceBoundCandidateRoot,
                        executedContractRoot,
                        Boolean.toString(mechanicalReviewRequired),
                        mechanicalBindingRoot,
                        mechanicalCatalogueRoot,
                        mechanicalReviewRoot,
                        nativeDonorReviewRoot,
                        "sourceMutationAuthority=false",
                        "donorSourceCopyAuthority=false",
                        "recipePromotionAuthority=false",
                        "repositoryPromotionAuthority=false",
                        "mergeAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!root.equals(expected)) {
            throw new IllegalArgumentException(
                    "mechanical executed-contract receipt root mismatch");
        }
    }

    /**
     * Bind an already verified executed contract to the source-bound/mechanical custody that
     * preceded execution.
     */
    public static M3AlgorithmAtomMechanicalExecutedContractReceipt bind(
            AlgorithmShape shape,
            String algorithmAtomBindingRoot,
            String atomRoot,
            String crateRoot,
            String sourceBoundCandidateRoot,
            Optional<M3AlgorithmAtomMechanicalDonorBinding> mechanicalBinding,
            M3AlgorithmAtomExecutedContractReceipt executedContract) {
        AlgorithmShape checkedShape = Objects.requireNonNull(shape, "shape");
        String bindingRoot = sha(algorithmAtomBindingRoot, "algorithmAtomBindingRoot");
        String checkedAtomRoot = sha(atomRoot, "atomRoot");
        String checkedCrateRoot = sha(crateRoot, "crateRoot");
        String candidateRoot = sha(sourceBoundCandidateRoot, "sourceBoundCandidateRoot");
        Optional<M3AlgorithmAtomMechanicalDonorBinding> mechanical =
                Objects.requireNonNull(mechanicalBinding, "mechanicalBinding");
        M3AlgorithmAtomExecutedContractReceipt executed =
                Objects.requireNonNull(executedContract, "executedContract");

        if (!executed.bindingRoot().equals(bindingRoot)
                || !executed.atomRoot().equals(checkedAtomRoot)
                || !executed.crateRoot().equals(checkedCrateRoot)
                || !executed.fixedPoint()
                || !executed.javaContractVerified()
                || executed.sourceMutationAuthority()
                || executed.donorSourceCopyAuthority()
                || executed.recipePromotionAuthority()
                || executed.repositoryPromotionAuthority()
                || executed.mergeAuthority()) {
            throw new IllegalArgumentException(
                    "executed contract does not belong to exact authority-free algorithm atom");
        }

        boolean required =
                M3AlgorithmAtomMechanicalDonorBinding.isMechanicalReviewRequired(checkedShape);
        if (required != mechanical.isPresent()) {
            throw new IllegalArgumentException(
                    "catalogued shape requires exact mechanical binding through executed contract");
        }

        String mechanicalRoot = "NONE";
        String catalogueRoot = "NONE";
        String reviewRoot = "NONE";
        String nativeRoot = "NONE";
        if (mechanical.isPresent()) {
            M3AlgorithmAtomMechanicalDonorBinding bound = mechanical.orElseThrow();
            M3RecipeFirstInvariant.requireAlgorithmAtomMechanicalDonorBinding(bound);
            M3MechanicalDonorShapeReview.Receipt canonicalReview =
                    M3MechanicalDonorShapeReview.review(checkedShape.name());
            if (!bound.mechanicalCatalogueRoot().equals(canonicalReview.catalogueRoot())
                    || !bound.mechanicalReviewRoot().equals(canonicalReview.reviewRoot())) {
                throw new IllegalArgumentException(
                        "mechanical binding review roots are not canonical for shape");
            }
            if (bound.shape() != checkedShape
                    || !bound.algorithmAtomBindingRoot().equals(bindingRoot)
                    || !bound.atomRoot().equals(checkedAtomRoot)
                    || !bound.crateRoot().equals(checkedCrateRoot)
                    || bound.sourceMutationAuthority()
                    || bound.donorSourceCopyAuthority()
                    || bound.replacementAuthority()
                    || bound.promotionAuthority()) {
                throw new IllegalArgumentException(
                        "mechanical binding does not belong to exact algorithm atom execution");
            }
            mechanicalRoot = bound.root();
            catalogueRoot = bound.mechanicalCatalogueRoot();
            reviewRoot = bound.mechanicalReviewRoot();
            nativeRoot = bound.nativeDonorReviewRoot();
        }

        M3AlgorithmAtomMechanicalExecutedContractReceipt receipt =
                new M3AlgorithmAtomMechanicalExecutedContractReceipt(
                        checkedShape,
                        bindingRoot,
                        checkedAtomRoot,
                        checkedCrateRoot,
                        candidateRoot,
                        executed.root(),
                        required,
                        mechanicalRoot,
                        catalogueRoot,
                        reviewRoot,
                        nativeRoot,
                        "");
        M3RecipeFirstInvariant.requireMechanicalExecutedContractReceipt(receipt);
        return receipt;
    }

    /**
     * Canonical integration path from one source-bound algorithm atom review.
     *
     * <p>The existing executed-contract owner still validates the Java contract. This wrapper only
     * carries donor custody across that boundary.</p>
     */
    public static M3AlgorithmAtomMechanicalExecutedContractReceipt admit(
            M3RecipeCrateSerialAtomReview.AtomSourceBoundCandidate sourceBound) {
        M3RecipeCrateSerialAtomReview.AtomSourceBoundCandidate checked =
                Objects.requireNonNull(sourceBound, "sourceBound");
        M3RecipeFirstInvariant.requireAlgorithmAtomSourceBoundCandidate(checked);
        M3AlgorithmAtomExecutedContractReceipt executed =
                M3AlgorithmAtomExecutedContractReceipt.admit(
                        checked.atomBinding(),
                        checked.candidate());
        return bind(
                checked.atomBinding().shape(),
                checked.atomBinding().root(),
                checked.atomBinding().atom().root(),
                checked.atomBinding().crateRoot(),
                checked.root(),
                checked.mechanicalBinding(),
                executed);
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean recipePromotionAuthority() {
        return false;
    }

    public boolean repositoryPromotionAuthority() {
        return false;
    }

    public boolean mergeAuthority() {
        return false;
    }

    private static String none(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (!checked.equals("NONE")) {
            throw new IllegalArgumentException(field + " must be NONE when review is not required");
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
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
