// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime view of the exact pinned algorithm donor manifest.
 *
 * <p>The authoritative rows are packaged directly from {@code synexia-algo/corpus/donors.tsv};
 * this class does not maintain another independent list of repository revisions. Exact duplicate
 * rows collapse to one pin while the snapshot records duplicate-row count for audit.</p>
 */
public final class PinnedAlgorithmDonorCatalog {
    public enum CorpusKind {
        LEETCODE,
        HACKERRANK,
        GEEKS_FOR_GEEKS,
        GENERAL
    }

    public record Pin(
            String repository,
            String revision,
            String license,
            String licenseFile,
            String licenseBlob,
            CorpusKind kind) {

        public Pin {
            repository = requiredRepository(repository);
            revision = sha1(revision, "revision");
            license = required(license, "license");
            licenseFile = required(licenseFile, "licenseFile");
            licenseBlob = sha1(licenseBlob, "licenseBlob");
            kind = Objects.requireNonNull(kind, "kind");
        }

        public String repositoryUrl() {
            return "https://github.com/" + repository;
        }

        public String stableKey() {
            return repository + "\u0000" + revision;
        }
    }

    public record Snapshot(
            List<Pin> pins,
            int rawRows,
            int duplicateRows,
            String root) {

        public Snapshot {
            pins = pins.stream()
                    .sorted(Comparator.comparing(Pin::repository).thenComparing(Pin::revision))
                    .toList();
            if (rawRows < pins.size() || duplicateRows != rawRows - pins.size()) {
                throw new IllegalArgumentException("donor manifest counts");
            }
            String expected = digest(pins, rawRows, duplicateRows);
            root = root == null || root.isBlank() ? expected : root;
            if (!root.equals(expected)) throw new IllegalArgumentException("donor manifest root mismatch");
        }
    }

    private static final String RESOURCE =
            "/com/synexia/algorithms/corpus/donors.tsv";
    private static final Snapshot SNAPSHOT = load();
    private static final Map<String, Pin> BY_KEY = indexByKey();
    private static final Map<String, List<Pin>> BY_REPOSITORY = indexByRepository();
    private static final Map<CorpusKind, List<Pin>> BY_KIND = indexByKind();

    private PinnedAlgorithmDonorCatalog() {}

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static List<Pin> all() {
        return SNAPSHOT.pins();
    }

    public static List<Pin> byKind(CorpusKind kind) {
        return BY_KIND.get(Objects.requireNonNull(kind, "kind"));
    }

    public static List<Pin> revisions(String repository) {
        return BY_REPOSITORY.getOrDefault(requiredRepository(repository), List.of());
    }

    public static boolean contains(String repository, String revision) {
        return BY_KEY.containsKey(key(repository, revision));
    }

    public static boolean containsRepository(String repository) {
        return BY_REPOSITORY.containsKey(requiredRepository(repository));
    }

    public static Pin require(String repository, String revision) {
        Pin pin = BY_KEY.get(key(repository, revision));
        if (pin == null) {
            throw new IllegalArgumentException(
                    "unpinned algorithm donor: " + repository + "@" + revision);
        }
        return pin;
    }

    private static Snapshot load() {
        InputStream stream = PinnedAlgorithmDonorCatalog.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new ExceptionInInitializerError("missing packaged donor manifest: " + RESOURCE);
        }

        LinkedHashMap<String, Pin> unique = new LinkedHashMap<>();
        int rawRows = 0;
        int duplicates = 0;
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] columns = line.split("\t", -1);
                if (columns.length != 5) {
                    throw new IllegalArgumentException(
                            "donor manifest line " + lineNumber + " must contain five columns");
                }
                rawRows++;
                Pin pin = new Pin(
                        columns[0],
                        columns[1],
                        columns[2],
                        columns[3],
                        columns[4],
                        kind(columns[0]));
                Pin prior = unique.putIfAbsent(pin.stableKey(), pin);
                if (prior != null) {
                    if (!prior.equals(pin)) {
                        throw new IllegalArgumentException(
                                "conflicting donor manifest rows for " + pin.stableKey());
                    }
                    duplicates++;
                }
            }
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
        return new Snapshot(new ArrayList<>(unique.values()), rawRows, duplicates, "");
    }

    private static Map<String, Pin> indexByKey() {
        LinkedHashMap<String, Pin> values = new LinkedHashMap<>();
        for (Pin pin : SNAPSHOT.pins()) {
            if (values.putIfAbsent(pin.stableKey(), pin) != null) {
                throw new ExceptionInInitializerError("duplicate donor pin: " + pin.stableKey());
            }
        }
        return Map.copyOf(values);
    }

    private static Map<String, List<Pin>> indexByRepository() {
        LinkedHashMap<String, ArrayList<Pin>> mutable = new LinkedHashMap<>();
        for (Pin pin : SNAPSHOT.pins()) {
            mutable.computeIfAbsent(pin.repository(), ignored -> new ArrayList<>()).add(pin);
        }
        LinkedHashMap<String, List<Pin>> frozen = new LinkedHashMap<>();
        mutable.forEach((repository, pins) -> frozen.put(repository, List.copyOf(pins)));
        return Map.copyOf(frozen);
    }

    private static Map<CorpusKind, List<Pin>> indexByKind() {
        EnumMap<CorpusKind, ArrayList<Pin>> mutable = new EnumMap<>(CorpusKind.class);
        for (CorpusKind kind : CorpusKind.values()) mutable.put(kind, new ArrayList<>());
        for (Pin pin : SNAPSHOT.pins()) mutable.get(pin.kind()).add(pin);

        EnumMap<CorpusKind, List<Pin>> frozen = new EnumMap<>(CorpusKind.class);
        mutable.forEach((kind, pins) -> frozen.put(kind, List.copyOf(pins)));
        return Map.copyOf(frozen);
    }

    private static CorpusKind kind(String repository) {
        String lower = repository.toLowerCase(Locale.ROOT);
        if (lower.contains("leetcode")) return CorpusKind.LEETCODE;
        if (lower.contains("hackerrank") || lower.contains("interview-preparation-kit")) {
            return CorpusKind.HACKERRANK;
        }
        if (lower.contains("geeksforgeeks") || lower.contains("gfg")) {
            return CorpusKind.GEEKS_FOR_GEEKS;
        }
        return CorpusKind.GENERAL;
    }

    private static String key(String repository, String revision) {
        return requiredRepository(repository) + "\u0000" + sha1(revision, "revision");
    }

    private static String requiredRepository(String value) {
        String checked = required(value, "repository");
        if (!checked.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("repository");
        }
        return checked;
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String sha1(String value, String field) {
        String checked = required(value, field);
        if (!checked.matches("[0-9a-f]{40}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String digest(List<Pin> pins, int rawRows, int duplicateRows) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "SYNEXIA_ALGORITHM_DONOR_MANIFEST_V1");
            update(digest, Integer.toString(rawRows));
            update(digest, Integer.toString(duplicateRows));
            for (Pin pin : pins) {
                update(digest, pin.repository());
                update(digest, pin.revision());
                update(digest, pin.license());
                update(digest, pin.licenseFile());
                update(digest, pin.licenseBlob());
                update(digest, pin.kind().name());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
