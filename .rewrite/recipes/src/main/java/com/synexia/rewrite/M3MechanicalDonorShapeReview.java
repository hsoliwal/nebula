// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.donor.DonorMechanicalShapeCatalog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Content-addressed read-only review over the canonical mechanical donor-shape catalogue.
 *
 * <p>The receipt is discovery/provenance evidence only. It grants no donor source-copy,
 * replacement, mutation, native-execution or promotion authority.</p>
 */
public final class M3MechanicalDonorShapeReview {
    private static final String CATALOGUE_ROOT =
            sha256("mechanical-donor-catalogue-v1\0"
                    + DonorMechanicalShapeCatalog.manifest());

    private M3MechanicalDonorShapeReview() {}

    public static Receipt review(String shape) {
        String canonical = canonicalShape(shape);
        List<DonorMechanicalShapeCatalog.Entry> entries =
                DonorMechanicalShapeCatalog.byShape(canonical);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("unknown mechanical donor shape: " + canonical);
        }

        ArrayList<DonorEvidence> donors = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            donors.add(evidence(canonical, index + 1, entries.get(index)));
        }

        List<DonorEvidence> frozen = List.copyOf(donors);
        StringBuilder rootInput =
                new StringBuilder("mechanical-donor-review-v1\0")
                        .append(canonical)
                        .append('\0')
                        .append(CATALOGUE_ROOT)
                        .append('\0')
                        .append(frozen.size());
        for (DonorEvidence donor : frozen) {
            rootInput.append('\0').append(donor.rowRoot());
        }

        return new Receipt(
                canonical,
                CATALOGUE_ROOT,
                frozen,
                sha256(rootInput.toString()),
                false,
                false);
    }

    public static List<Receipt> reviewAll() {
        return DonorMechanicalShapeCatalog.shapes().stream()
                .map(M3MechanicalDonorShapeReview::review)
                .toList();
    }

    public static String catalogueRoot() {
        return CATALOGUE_ROOT;
    }

    private static DonorEvidence evidence(
            String shape,
            int ordinal,
            DonorMechanicalShapeCatalog.Entry entry) {
        String root =
                sha256(
                        String.join(
                                "\0",
                                "mechanical-donor-row-v1",
                                shape,
                                Integer.toString(ordinal),
                                entry.repository(),
                                entry.revision(),
                                entry.license(),
                                entry.licensePath(),
                                entry.licenseBlob(),
                                entry.sourceClass().name(),
                                entry.family().name(),
                                entry.decision().name(),
                                entry.note()));
        return new DonorEvidence(
                ordinal,
                entry.repository(),
                entry.revision(),
                entry.license(),
                entry.licensePath(),
                entry.licenseBlob(),
                entry.sourceClass().name(),
                entry.family().name(),
                entry.decision().name(),
                entry.note(),
                root,
                false,
                false);
    }

    private static String canonicalShape(String shape) {
        String value =
                Objects.requireNonNull(shape, "shape")
                        .strip()
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_');
        if (value.isEmpty()) throw new IllegalArgumentException("shape required");
        return value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public record Receipt(
            String shape,
            String catalogueRoot,
            List<DonorEvidence> donors,
            String reviewRoot,
            boolean sourceCopyAuthority,
            boolean replacementAuthority) {
        public Receipt {
            shape = Objects.requireNonNull(shape, "shape");
            catalogueRoot = requireSha(catalogueRoot, "catalogueRoot");
            donors = List.copyOf(Objects.requireNonNull(donors, "donors"));
            if (donors.isEmpty()) throw new IllegalArgumentException("donors required");
            reviewRoot = requireSha(reviewRoot, "reviewRoot");
            if (sourceCopyAuthority || replacementAuthority) {
                throw new IllegalArgumentException("review receipt cannot grant source authority");
            }
        }
    }

    public record DonorEvidence(
            int ordinal,
            String repository,
            String revision,
            String license,
            String licensePath,
            String licenseBlob,
            String sourceClass,
            String family,
            String decision,
            String note,
            String rowRoot,
            boolean sourceCopyAuthority,
            boolean replacementAuthority) {
        public DonorEvidence {
            if (ordinal <= 0) throw new IllegalArgumentException("ordinal must be > 0");
            repository = requireText(repository, "repository");
            revision = requireGitId(revision, "revision");
            license = requireText(license, "license");
            licensePath = requireText(licensePath, "licensePath");
            licenseBlob = requireGitId(licenseBlob, "licenseBlob");
            sourceClass = requireText(sourceClass, "sourceClass");
            family = requireText(family, "family");
            decision = requireText(decision, "decision");
            note = requireText(note, "note");
            rowRoot = requireSha(rowRoot, "rowRoot");
            if (sourceCopyAuthority || replacementAuthority) {
                throw new IllegalArgumentException("donor evidence cannot grant source authority");
            }
        }
    }

    private static String requireGitId(String value, String field) {
        String checked = requireText(value, field);
        if (!checked.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException(field + " must be a pinned lowercase Git object ID");
        }
        return checked;
    }

    private static String requireSha(String value, String field) {
        String checked = requireText(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return checked;
    }

    private static String requireText(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()) throw new IllegalArgumentException(field + " required");
        return checked;
    }
}
