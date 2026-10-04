// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable donor-license/provenance audit over resolved competitive-programming Java evidence.
 *
 * <p>Challenge-source rows remain evidence even when a repository is not license-admitted. A pinned
 * permissive donor may advance to behavioral/benchmark proof, but this audit never grants direct
 * source-copy, replacement or promotion authority.</p>
 */
public final class ChallengeDonorEvidenceAudit {

    public enum Disposition {
        EVIDENCE_ONLY_UNPINNED,
        PINNED_INSPECTION_ONLY,
        PINNED_PERMISSIVE_CANDIDATE
    }

    public record EvidenceRow(
            String platform,
            String repository,
            String revision,
            String path,
            String evidenceId,
            boolean pinned,
            String declaredLicense,
            DonorLicensePromotionPolicy.Decision licenseDecision,
            boolean automaticPromotionCandidate,
            Disposition disposition,
            String root) {

        public EvidenceRow {
            platform = text(platform, "platform").toUpperCase(Locale.ROOT);
            repository = text(repository, "repository");
            revision = sha1(revision, "revision");
            path = text(path, "path");
            evidenceId = text(evidenceId, "evidenceId");
            declaredLicense = text(declaredLicense, "declaredLicense");
            licenseDecision = Objects.requireNonNull(licenseDecision, "licenseDecision");
            disposition = Objects.requireNonNull(disposition, "disposition");

            if (!pinned
                    && (automaticPromotionCandidate
                            || disposition != Disposition.EVIDENCE_ONLY_UNPINNED)) {
                throw new IllegalArgumentException("unpinned evidence cannot be promotion-ready");
            }
            if (automaticPromotionCandidate
                    != (disposition == Disposition.PINNED_PERMISSIVE_CANDIDATE)) {
                throw new IllegalArgumentException("promotion/disposition mismatch");
            }

            String expected =
                    digest(
                            "SYNEXIA_CHALLENGE_DONOR_EVIDENCE_ROW_V1",
                            platform,
                            repository,
                            revision,
                            path,
                            evidenceId,
                            Boolean.toString(pinned),
                            declaredLicense,
                            licenseDecision.name(),
                            Boolean.toString(automaticPromotionCandidate),
                            disposition.name());
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("evidence row root mismatch");
            }
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
    }

    public record Snapshot(
            AlgorithmShape shape,
            int limit,
            List<EvidenceRow> rows,
            int representedPlatforms,
            int pinnedRows,
            int permissiveCandidateRows,
            int inspectionOnlyRows,
            int unpinnedEvidenceRows,
            String corpusRoot,
            String donorManifestRoot,
            String root) {

        public Snapshot {
            shape = Objects.requireNonNull(shape, "shape");
            if (limit < 1 || limit > 1000) throw new IllegalArgumentException("limit");
            rows =
                    Objects.requireNonNull(rows, "rows").stream()
                            .sorted(
                                    Comparator.comparing(EvidenceRow::platform)
                                            .thenComparing(EvidenceRow::repository)
                                            .thenComparing(EvidenceRow::revision)
                                            .thenComparing(EvidenceRow::path))
                            .toList();
            representedPlatforms =
                    Math.toIntExact(rows.stream().map(EvidenceRow::platform).distinct().count());
            int expectedPinned = (int) rows.stream().filter(EvidenceRow::pinned).count();
            int expectedPermissive =
                    (int)
                            rows.stream()
                                    .filter(EvidenceRow::automaticPromotionCandidate)
                                    .count();
            int expectedInspection =
                    (int)
                            rows.stream()
                                    .filter(
                                            row ->
                                                    row.disposition()
                                                            == Disposition.PINNED_INSPECTION_ONLY)
                                    .count();
            int expectedUnpinned =
                    (int)
                            rows.stream()
                                    .filter(
                                            row ->
                                                    row.disposition()
                                                            == Disposition.EVIDENCE_ONLY_UNPINNED)
                                    .count();
            if (pinnedRows != expectedPinned
                    || permissiveCandidateRows != expectedPermissive
                    || inspectionOnlyRows != expectedInspection
                    || unpinnedEvidenceRows != expectedUnpinned) {
                throw new IllegalArgumentException("snapshot counts");
            }
            corpusRoot = sha256(corpusRoot, "corpusRoot");
            donorManifestRoot = sha256(donorManifestRoot, "donorManifestRoot");

            String expected =
                    digest(
                            "SYNEXIA_CHALLENGE_DONOR_EVIDENCE_SNAPSHOT_V1",
                            shape.name(),
                            Integer.toString(limit),
                            Integer.toString(representedPlatforms),
                            Integer.toString(pinnedRows),
                            Integer.toString(permissiveCandidateRows),
                            Integer.toString(inspectionOnlyRows),
                            Integer.toString(unpinnedEvidenceRows),
                            corpusRoot,
                            donorManifestRoot,
                            rows.stream()
                                    .map(EvidenceRow::root)
                                    .reduce("", (left, right) -> left + right + "\u001f"));
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("snapshot root mismatch");
            }
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
    }

    private ChallengeDonorEvidenceAudit() {}

    public static EvidenceRow assess(CorpusSourceEntry entry) {
        CorpusSourceEntry checked = Objects.requireNonNull(entry, "entry");
        if (!PinnedAlgorithmDonorCatalog.contains(checked.repository(), checked.commit())) {
            return new EvidenceRow(
                    checked.platform(),
                    checked.repository(),
                    checked.commit(),
                    checked.path(),
                    checked.evidenceId(),
                    false,
                    "UNVERIFIED",
                    DonorLicensePromotionPolicy.Decision.UNKNOWN_REVIEW_REQUIRED,
                    false,
                    Disposition.EVIDENCE_ONLY_UNPINNED,
                    "");
        }

        PinnedAlgorithmDonorCatalog.Pin pin =
                PinnedAlgorithmDonorCatalog.require(checked.repository(), checked.commit());
        DonorLicensePromotionPolicy.Assessment license =
                DonorLicensePromotionPolicy.assess(pin.license());
        boolean promotionCandidate = license.automaticCodePromotionAllowed();
        return new EvidenceRow(
                checked.platform(),
                checked.repository(),
                checked.commit(),
                checked.path(),
                checked.evidenceId(),
                true,
                pin.license(),
                license.decision(),
                promotionCandidate,
                promotionCandidate
                        ? Disposition.PINNED_PERMISSIVE_CANDIDATE
                        : Disposition.PINNED_INSPECTION_ONLY,
                "");
    }

    public static Snapshot forShape(AlgorithmShape shape, int limit) {
        AlgorithmShape checked = Objects.requireNonNull(shape, "shape");
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("limit");

        List<EvidenceRow> rows =
                JavaProblemCorpus.resolvedEvidenceByShape(checked, limit).stream()
                        .map(ChallengeDonorEvidenceAudit::assess)
                        .toList();
        int pinned = (int) rows.stream().filter(EvidenceRow::pinned).count();
        int permissive = (int) rows.stream().filter(EvidenceRow::automaticPromotionCandidate).count();
        int inspection =
                (int)
                        rows.stream()
                                .filter(
                                        row ->
                                                row.disposition()
                                                        == Disposition.PINNED_INSPECTION_ONLY)
                                .count();
        int unpinned =
                (int)
                        rows.stream()
                                .filter(
                                        row ->
                                                row.disposition()
                                                        == Disposition.EVIDENCE_ONLY_UNPINNED)
                                .count();

        return new Snapshot(
                checked,
                limit,
                rows,
                0,
                pinned,
                permissive,
                inspection,
                unpinned,
                JavaProblemCorpus.resolvedIndexSnapshot().root(),
                PinnedAlgorithmDonorCatalog.snapshot().root(),
                "");
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha1(String value, String field) {
        String checked = text(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{40}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String sha256(String value, String field) {
        String checked = text(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
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
