// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.corpus.CompetitiveProblemCategory;
import com.synexia.algorithms.corpus.NativeMechanicsDonorCatalog;
import com.synexia.algorithms.corpus.ProblemOptimizationCatalog;
import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Deterministic C/C++/GPU donor comparison receipt for one canonical algorithm shape. */
public final class M3NativeDonorSerialReview {
    public record DonorEvidence(
            int ordinal,
            String repository,
            String revision,
            List<String> matchedKernels,
            List<String> matchedMechanics,
            List<String> owners,
            String nativeUse,
            String donorRoot,
            String root) {
        public DonorEvidence {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            repository = text(repository, "repository");
            revision = sha1(revision, "revision");
            matchedKernels = stable(matchedKernels);
            matchedMechanics = stable(matchedMechanics);
            owners = stable(owners);
            nativeUse = text(nativeUse, "nativeUse");
            donorRoot = sha256(donorRoot, "donorRoot");
            String expected = digest(
                    "M3_NATIVE_DONOR_SERIAL_ROW_V1",
                    Integer.toString(ordinal),
                    repository,
                    revision,
                    String.join("\u001f", matchedKernels),
                    String.join("\u001f", matchedMechanics),
                    String.join("\u001f", owners),
                    nativeUse,
                    donorRoot);
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!expected.equals(root)) throw new IllegalArgumentException("native donor row root mismatch");
        }

        public String pin() {
            return repository + "@" + revision;
        }

        public boolean sourceCopyAuthority() { return false; }
        public boolean nativeExecutionAuthority() { return false; }
        public boolean replacementAuthority() { return false; }
    }

    public record Receipt(
            AlgorithmShape shape,
            CompetitiveProblemCategory category,
            ProblemOptimizationCatalog.NativeLane nativeLane,
            List<DonorEvidence> donors,
            String donorCatalogueRoot,
            String optimizationRoot,
            String root) {
        public Receipt {
            shape = Objects.requireNonNull(shape, "shape");
            category = Objects.requireNonNull(category, "category");
            nativeLane = Objects.requireNonNull(nativeLane, "nativeLane");
            donors = List.copyOf(Objects.requireNonNull(donors, "donors"));
            donorCatalogueRoot = sha256(donorCatalogueRoot, "donorCatalogueRoot");
            optimizationRoot = sha256(optimizationRoot, "optimizationRoot");
            if (category != CompetitiveProblemCategory.fromShape(shape)) {
                throw new IllegalArgumentException("shape/category drift");
            }
            for (int i = 0; i < donors.size(); i++) {
                if (donors.get(i).ordinal() != i) {
                    throw new IllegalArgumentException("non-serial native donor ordinal");
                }
            }
            String expected = digest(
                    "M3_NATIVE_DONOR_SERIAL_REVIEW_V1",
                    shape.name(),
                    category.name(),
                    nativeLane.name(),
                    donorCatalogueRoot,
                    optimizationRoot,
                    donors.stream().map(DonorEvidence::root).reduce("", M3NativeDonorSerialReview::join));
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!expected.equals(root)) throw new IllegalArgumentException("native donor review root mismatch");
        }

        public List<String> pins() {
            return donors.stream().map(DonorEvidence::pin).toList();
        }

        public boolean sourceCopyAuthority() { return false; }
        public boolean nativeExecutionAuthority() { return false; }
        public boolean replacementAuthority() { return false; }
    }

    private M3NativeDonorSerialReview() {}

    public static Receipt review(AlgorithmShape shape) {
        AlgorithmShape checked = Objects.requireNonNull(shape, "shape");
        ProblemOptimizationCatalog.Plan plan = ProblemOptimizationCatalog.plan(checked);
        ArrayList<NativeMechanicsDonorCatalog.Match> matches =
                new ArrayList<>(plan.nativeMechanicsDonors());
        matches.sort(Comparator
                .comparing((NativeMechanicsDonorCatalog.Match match) -> match.donor().repository())
                .thenComparing(match -> match.donor().revision()));
        ArrayList<DonorEvidence> rows = new ArrayList<>(matches.size());
        for (int ordinal = 0; ordinal < matches.size(); ordinal++) {
            NativeMechanicsDonorCatalog.Match match = matches.get(ordinal);
            NativeMechanicsDonorCatalog.Donor donor = match.donor();
            rows.add(new DonorEvidence(
                    ordinal,
                    donor.repository(),
                    donor.revision(),
                    match.matchedKernels().stream().map(Enum::name).sorted().toList(),
                    match.matchedMechanics().stream().map(Enum::name).sorted().toList(),
                    donor.owners().stream().map(Enum::name).sorted().toList(),
                    donor.nativeUse().name(),
                    donor.root(),
                    ""));
        }
        return new Receipt(
                checked,
                CompetitiveProblemCategory.fromShape(checked),
                plan.nativeLane(),
                rows,
                NativeMechanicsDonorCatalog.root(),
                plan.root(),
                "");
    }

    public static List<Receipt> reviewAll() {
        return java.util.Arrays.stream(AlgorithmShape.values())
                .sorted(Comparator.comparing(Enum::name))
                .map(M3NativeDonorSerialReview::review)
                .toList();
    }

    public static Receipt requireCanonical(
            AlgorithmShape shape,
            ProblemOptimizationCatalog.Plan optimization,
            List<String> nativeDonors) {
        Receipt receipt = review(shape);
        ProblemOptimizationCatalog.Plan checked =
                Objects.requireNonNull(optimization, "optimization");
        List<String> supplied = stable(nativeDonors);
        if (checked.shape() != shape
                || !checked.root().equals(receipt.optimizationRoot())
                || checked.nativeLane() != receipt.nativeLane()
                || !supplied.equals(receipt.pins())) {
            throw new IllegalArgumentException(
                    "native donor review does not match canonical shape/plan/pins");
        }
        return receipt;
    }

    private static List<String> stable(List<String> values) {
        return Objects.requireNonNull(values, "values").stream()
                .map(value -> text(value, "value"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String join(String left, String right) {
        return left.isEmpty() ? right : left + "\u001f" + right;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha1(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{40}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String sha256(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.requireNonNull(value, "digest value")
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
