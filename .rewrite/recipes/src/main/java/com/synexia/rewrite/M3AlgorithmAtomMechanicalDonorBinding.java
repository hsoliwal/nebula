// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.donor.DonorMechanicalShapeCatalog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Content-addressed custody joining one exact algorithm atom to an admitted mechanical donor review.
 *
 * <p>This is additive evidence only. The existing algorithm atom binding remains the owner of file,
 * method, challenge, Java-donor, native-donor and execution-lane evidence.</p>
 */
public record M3AlgorithmAtomMechanicalDonorBinding(
        AlgorithmShape shape,
        String algorithmAtomBindingRoot,
        String atomRoot,
        String methodKey,
        String crateRoot,
        String admissionRoot,
        String mechanicalCrateBindingRoot,
        String mechanicalCatalogueRoot,
        String mechanicalReviewRoot,
        String nativeDonorReviewRoot,
        String root) {

    public M3AlgorithmAtomMechanicalDonorBinding {
        shape = Objects.requireNonNull(shape, "shape");
        algorithmAtomBindingRoot = sha(algorithmAtomBindingRoot, "algorithmAtomBindingRoot");
        atomRoot = sha(atomRoot, "atomRoot");
        methodKey = text(methodKey, "methodKey");
        crateRoot = sha(crateRoot, "crateRoot");
        admissionRoot = sha(admissionRoot, "admissionRoot");
        mechanicalCrateBindingRoot = sha(mechanicalCrateBindingRoot, "mechanicalCrateBindingRoot");
        mechanicalCatalogueRoot = sha(mechanicalCatalogueRoot, "mechanicalCatalogueRoot");
        mechanicalReviewRoot = sha(mechanicalReviewRoot, "mechanicalReviewRoot");
        nativeDonorReviewRoot = sha(nativeDonorReviewRoot, "nativeDonorReviewRoot");

        if (!isMechanicalReviewRequired(shape)) {
            throw new IllegalArgumentException(
                    "algorithm shape has no canonical mechanical donor review: " + shape);
        }

        String expected =
                digest(
                        "M3_ALGORITHM_ATOM_MECHANICAL_DONOR_BINDING_V1",
                        shape.name(),
                        algorithmAtomBindingRoot,
                        atomRoot,
                        methodKey,
                        crateRoot,
                        admissionRoot,
                        mechanicalCrateBindingRoot,
                        mechanicalCatalogueRoot,
                        mechanicalReviewRoot,
                        nativeDonorReviewRoot,
                        "sourceMutationAuthority=false",
                        "donorSourceCopyAuthority=false",
                        "replacementAuthority=false",
                        "promotionAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!root.equals(expected)) {
            throw new IllegalArgumentException(
                    "algorithm atom mechanical donor binding root mismatch");
        }
    }

    public static Optional<M3AlgorithmAtomMechanicalDonorBinding> bindIfCatalogued(
            M3AlgorithmAtomRecipeCrateBinding atomBinding,
            M3LlmTaskAdmissionReceipt admission) {
        M3AlgorithmAtomRecipeCrateBinding atom =
                Objects.requireNonNull(atomBinding, "atomBinding");
        M3LlmTaskAdmissionReceipt admitted =
                Objects.requireNonNull(admission, "admission");

        if (!isMechanicalReviewRequired(atom.shape())) {
            return Optional.empty();
        }
        if (atom.sourceMutationAuthority()
                || atom.donorSourceCopyAuthority()
                || atom.promotionAuthority()) {
            throw new IllegalStateException("algorithm atom authority widened before review binding");
        }

        M3MechanicalDonorRecipeCrateBinding mechanical =
                M3MechanicalDonorRecipeCrateBinding.bind(
                        atom.crate(),
                        admitted,
                        atom.shape().name());

        if (!atom.crateRoot().equals(mechanical.crateRoot())
                || !admitted.root().equals(mechanical.admissionRoot())
                || !atom.shape().name().equals(mechanical.shape())) {
            throw new IllegalArgumentException(
                    "algorithm atom and mechanical recipe-crate evidence do not share one identity");
        }

        M3NativeDonorSerialReview.Receipt nativeReview =
                atom.nativeDonorReviewReceipt();

        M3AlgorithmAtomMechanicalDonorBinding binding =
                new M3AlgorithmAtomMechanicalDonorBinding(
                        atom.shape(),
                        atom.root(),
                        atom.atom().root(),
                        atom.atom().methodKey(),
                        atom.crateRoot(),
                        admitted.root(),
                        mechanical.root(),
                        mechanical.mechanicalCatalogueRoot(),
                        mechanical.mechanicalReviewRoot(),
                        nativeReview.root(),
                        "");

        M3RecipeFirstInvariant.requireAlgorithmAtomMechanicalDonorBinding(binding);
        return Optional.of(binding);
    }

    public static boolean isMechanicalReviewRequired(AlgorithmShape shape) {
        AlgorithmShape checked = Objects.requireNonNull(shape, "shape");
        return !DonorMechanicalShapeCatalog.byShape(checked.name()).isEmpty();
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
