// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Content-addressed custody joining one algorithm atom's mechanical executed contract to a
 * promotable recipe receipt and exact post-promotion canonical Git-tree readback.
 *
 * <p>The existing {@link M3RecipePromotionReceipt} remains promotion authority and
 * {@link M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence} remains canonical tree authority.
 * This receipt only preserves provenance across those boundaries.</p>
 */
public record M3AlgorithmAtomMechanicalPromotionReadbackReceipt(
        AlgorithmShape shape,
        String sourcePath,
        String algorithmAtomBindingRoot,
        String atomRoot,
        String crateRoot,
        String sourceBoundCandidateRoot,
        String executedMechanicalContractRoot,
        boolean mechanicalReviewRequired,
        String mechanicalBindingRoot,
        String mechanicalCatalogueRoot,
        String mechanicalReviewRoot,
        String nativeDonorReviewRoot,
        String promotionReceiptRoot,
        String promotionWorkRoot,
        String promotionRecipeCatalogueRoot,
        String promotionRecipePassRoot,
        String canonicalReadbackRoot,
        String promotionCommitSha,
        String canonicalHeadSha,
        String expectedGitBlob,
        String actualGitBlob,
        String root) {

    public M3AlgorithmAtomMechanicalPromotionReadbackReceipt {
        shape = Objects.requireNonNull(shape, "shape");
        sourcePath = path(sourcePath);
        algorithmAtomBindingRoot = sha(algorithmAtomBindingRoot, "algorithmAtomBindingRoot");
        atomRoot = sha(atomRoot, "atomRoot");
        crateRoot = sha(crateRoot, "crateRoot");
        sourceBoundCandidateRoot = sha(sourceBoundCandidateRoot, "sourceBoundCandidateRoot");
        executedMechanicalContractRoot =
                sha(executedMechanicalContractRoot, "executedMechanicalContractRoot");

        boolean required = M3AlgorithmAtomMechanicalDonorBinding.isMechanicalReviewRequired(shape);
        if (mechanicalReviewRequired != required) {
            throw new IllegalArgumentException("mechanical review requirement drift");
        }

        if (required) {
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

        promotionReceiptRoot = sha(promotionReceiptRoot, "promotionReceiptRoot");
        promotionWorkRoot = sha(promotionWorkRoot, "promotionWorkRoot");
        promotionRecipeCatalogueRoot =
                sha(promotionRecipeCatalogueRoot, "promotionRecipeCatalogueRoot");
        promotionRecipePassRoot = sha(promotionRecipePassRoot, "promotionRecipePassRoot");
        canonicalReadbackRoot = sha(canonicalReadbackRoot, "canonicalReadbackRoot");
        promotionCommitSha = gitSha(promotionCommitSha, "promotionCommitSha");
        canonicalHeadSha = gitSha(canonicalHeadSha, "canonicalHeadSha");
        expectedGitBlob = gitSha(expectedGitBlob, "expectedGitBlob");
        actualGitBlob = gitSha(actualGitBlob, "actualGitBlob");
        if (!expectedGitBlob.equals(actualGitBlob)) {
            throw new IllegalArgumentException("canonical atom source blob mismatch");
        }

        String expected =
                digest(
                        "M3_ALGORITHM_ATOM_MECHANICAL_PROMOTION_READBACK_V1",
                        shape.name(),
                        sourcePath,
                        algorithmAtomBindingRoot,
                        atomRoot,
                        crateRoot,
                        sourceBoundCandidateRoot,
                        executedMechanicalContractRoot,
                        Boolean.toString(mechanicalReviewRequired),
                        mechanicalBindingRoot,
                        mechanicalCatalogueRoot,
                        mechanicalReviewRoot,
                        nativeDonorReviewRoot,
                        promotionReceiptRoot,
                        promotionWorkRoot,
                        promotionRecipeCatalogueRoot,
                        promotionRecipePassRoot,
                        canonicalReadbackRoot,
                        promotionCommitSha,
                        canonicalHeadSha,
                        expectedGitBlob,
                        actualGitBlob,
                        "sourceMutationAuthority=false",
                        "donorSourceCopyAuthority=false",
                        "recipePromotionAuthority=false",
                        "repositoryPromotionAuthority=false",
                        "mergeAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!root.equals(expected)) {
            throw new IllegalArgumentException(
                    "mechanical promotion/readback custody root mismatch");
        }
    }

    /**
     * Bind exact atom, executed contract, promotion and canonical tree evidence.
     *
     * <p>The generic promotion receipt is capability-level; this wrapper additionally requires the
     * same recipe catalogue as the atom's admitted task crate and exact readback of the atom source
     * path after promotion.</p>
     */
    public static M3AlgorithmAtomMechanicalPromotionReadbackReceipt bind(
            M3AlgorithmAtomRecipeCrateBinding atomBinding,
            M3AlgorithmAtomMechanicalExecutedContractReceipt executedContract,
            M3RecipePromotionReceipt promotion,
            M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence readback) {
        M3AlgorithmAtomRecipeCrateBinding atom =
                Objects.requireNonNull(atomBinding, "atomBinding");
        M3AlgorithmAtomMechanicalExecutedContractReceipt executed =
                Objects.requireNonNull(executedContract, "executedContract");
        M3RecipePromotionReceipt promoted = Objects.requireNonNull(promotion, "promotion");
        M3RecipeFirstInvariant.CanonicalTreeReadbackEvidence canonical =
                Objects.requireNonNull(readback, "readback");

        atom.requireSerialReviewReady();
        M3RecipeFirstInvariant.requireMechanicalExecutedContractReceipt(executed);
        M3RecipeFirstInvariant.requirePostMergeCanonicalTreeReadback(canonical);

        if (executed.shape() != atom.shape()
                || !executed.algorithmAtomBindingRoot().equals(atom.root())
                || !executed.atomRoot().equals(atom.atom().root())
                || !executed.crateRoot().equals(atom.crateRoot())) {
            throw new IllegalArgumentException(
                    "mechanical executed contract belongs to another algorithm atom");
        }

        if (!promoted.promotable()) {
            throw new IllegalArgumentException("algorithm atom requires promotable recipe receipt");
        }
        if (!promoted.capabilityId().equals(atom.crate().capabilityId())
                || !promoted.recipeCatalogueRoot().equals(atom.crate().catalogueSha256())) {
            throw new IllegalArgumentException(
                    "promotion receipt belongs to another capability or recipe catalogue");
        }

        String sourcePath = atom.atom().sourcePath();
        String expectedBlob = canonical.expectedTargetBlobs().get(sourcePath);
        String actualBlob = canonical.actualTargetBlobs().get(sourcePath);
        if (expectedBlob == null || actualBlob == null) {
            throw new IllegalArgumentException(
                    "canonical readback does not contain exact algorithm atom source path");
        }
        if (!expectedBlob.equals(actualBlob)) {
            throw new IllegalArgumentException(
                    "canonical readback does not preserve exact algorithm atom source blob");
        }

        M3AlgorithmAtomMechanicalPromotionReadbackReceipt receipt =
                new M3AlgorithmAtomMechanicalPromotionReadbackReceipt(
                        atom.shape(),
                        sourcePath,
                        atom.root(),
                        atom.atom().root(),
                        atom.crateRoot(),
                        executed.sourceBoundCandidateRoot(),
                        executed.root(),
                        executed.mechanicalReviewRequired(),
                        executed.mechanicalBindingRoot(),
                        executed.mechanicalCatalogueRoot(),
                        executed.mechanicalReviewRoot(),
                        executed.nativeDonorReviewRoot(),
                        promoted.root(),
                        promoted.workRoot(),
                        promoted.recipeCatalogueRoot(),
                        promoted.recipePassRoot(),
                        M3CanonicalTreeReadbackTsv.root(canonical),
                        canonical.promotionCommitSha(),
                        canonical.canonicalHeadSha(),
                        expectedBlob,
                        actualBlob,
                        "");
        M3RecipeFirstInvariant.requireMechanicalPromotionReadbackReceipt(receipt);
        return receipt;
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

    private static String path(String value) {
        String checked = Objects.requireNonNull(value, "sourcePath").strip().replace('\\', '/');
        if (checked.isEmpty()
                || checked.startsWith("/")
                || checked.equals("..")
                || checked.startsWith("../")
                || checked.endsWith("/..")
                || checked.contains("/../")
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException("sourcePath");
        }
        return checked;
    }

    private static String none(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (!checked.equals("NONE")) {
            throw new IllegalArgumentException(field + " must be NONE");
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

    private static String gitSha(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (!checked.matches("[0-9a-f]{40}")) {
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
