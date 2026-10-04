// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Exact permissive donor pins for optimization mechanics.
 *
 * <p>These pins authorize review/attribution only. A donor match never grants automatic source
 * copy, replacement, execution, or promotion authority.</p>
 */
public final class OptimizationDonorCatalog {
    public enum Role {
        ALGORITHM_REFERENCE,
        PRIMITIVE_COLLECTIONS,
        BITMAP_POSTINGS,
        BOUNDED_REGEX
    }

    public record Donor(
            String repository,
            String revision,
            String license,
            String licenseFile,
            String licenseBlob,
            List<Role> roles,
            String root) {

        public Donor {
            repository = repositoryName(repository);
            revision = sha1(revision, "revision");
            license = text(license, "license");
            licenseFile = text(licenseFile, "licenseFile");
            licenseBlob = sha1(licenseBlob, "licenseBlob");
            final EnumSet<Role> ordered = EnumSet.noneOf(Role.class);
            ordered.addAll(Objects.requireNonNull(roles, "roles"));
            if (ordered.isEmpty()) {
                throw new IllegalArgumentException("DONOR_ROLES_REQUIRED");
            }
            roles = List.copyOf(ordered);
            final CatalogueDigest digest = new CatalogueDigest("SYNEXIA_OPTIMIZATION_DONOR_V1")
                    .text(repository)
                    .text(revision)
                    .text(license)
                    .text(licenseFile)
                    .text(licenseBlob);
            roles.forEach(role -> digest.text(role.name()));
            final String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("OPTIMIZATION_DONOR_ROOT_MISMATCH");
            }
        }

        public boolean implementationAuthority() {
            return false;
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public String repositoryUrl() {
            return "https://github.com/" + repository;
        }
    }

    private static final List<Donor> DONORS = List.of(
            new Donor(
                    "TheAlgorithms/Java",
                    "743ff5eb0944d8a3ed25d73fc992f5a69a36134d",
                    "MIT",
                    "LICENSE",
                    "f6bcf04e7773b3bfca7a30836672dc7322e38944",
                    List.of(Role.ALGORITHM_REFERENCE),
                    ""),
            new Donor(
                    "RoaringBitmap/RoaringBitmap",
                    "f2289086f4204df51a1d9c4e494d50ffa3ee8988",
                    "Apache-2.0",
                    "LICENSE",
                    "a890d4a062fad2e14e91762d46f1e966c5799e5c",
                    List.of(Role.BITMAP_POSTINGS),
                    ""),
            new Donor(
                    "vigna/fastutil",
                    "cbf3c2ec706d16c56e6896b13ef0b5361be981c8",
                    "Apache-2.0",
                    "LICENSE-2.0",
                    "d645695673349e3947e8e5ae42332d0ac3164cd7",
                    List.of(Role.PRIMITIVE_COLLECTIONS),
                    ""),
            new Donor(
                    "google/re2j",
                    "951a6159fdc0a4931593c70442926f86031edf40",
                    "BSD-3-Clause",
                    "LICENSE",
                    "b620ae68fe332112e50d1fc5da4a32cd6191681a",
                    List.of(Role.BOUNDED_REGEX),
                    ""));

    private static final String ROOT = root(DONORS);

    private OptimizationDonorCatalog() {}

    public static List<Donor> all() {
        return DONORS;
    }

    public static List<Donor> byRole(final Role role) {
        Objects.requireNonNull(role, "role");
        return DONORS.stream().filter(donor -> donor.roles().contains(role)).toList();
    }

    public static Donor require(final String repository) {
        final String checked = repositoryName(repository);
        return DONORS.stream()
                .filter(donor -> donor.repository().equals(checked))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "UNPINNED_OPTIMIZATION_DONOR:" + checked));
    }

    /**
     * Native systems-mechanics donor tier attached to this optimization evidence owner.
     */
    public static List<NativeMechanicsDonorCatalog.Donor> nativeSystems() {
        return NativeMechanicsDonorCatalog.all();
    }

    public static String root() {
        return ROOT;
    }

    private static String root(final List<Donor> donors) {
        final ArrayList<Donor> sorted = new ArrayList<>(donors);
        sorted.sort(Comparator.comparing(Donor::repository).thenComparing(Donor::revision));
        final CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_OPTIMIZATION_DONOR_CATALOG_V1");
        sorted.forEach(donor -> digest.text(donor.root()));
        return digest.finish();
    }

    private static String repositoryName(final String value) {
        final String checked = text(value, "repository");
        if (!checked.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("INVALID_DONOR_REPOSITORY");
        }
        return checked;
    }

    private static String text(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("INVALID_" + field.toUpperCase(java.util.Locale.ROOT));
        }
        return checked;
    }

    private static String sha1(final String value, final String field) {
        final String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("INVALID_" + field.toUpperCase(java.util.Locale.ROOT));
        }
        return checked;
    }

    private static String sha256(final String value, final String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("INVALID_" + field.toUpperCase(java.util.Locale.ROOT));
        }
        return value;
    }
}
