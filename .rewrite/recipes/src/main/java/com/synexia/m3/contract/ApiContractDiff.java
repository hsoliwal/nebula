package com.synexia.m3.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Additive compatibility comparison. New members are allowed; removed external contracts fail closed. */
public final class ApiContractDiff {

    public Result compare(JavaApiSnapshot baseline, JavaApiSnapshot current, Set<String> allowedRemovalFingerprints) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(current, "current");
        Set<String> allow = Set.copyOf(Objects.requireNonNull(allowedRemovalFingerprints, "allowedRemovalFingerprints"));

        Map<String, ApiMember> before = mapByFingerprint(baseline);
        Map<String, ApiMember> after = mapByFingerprint(current);

        List<Change> changes = new ArrayList<>();
        for (ApiMember member : before.values()) {
            if (!after.containsKey(member.fingerprint())) {
                boolean allowed = allow.contains(member.fingerprint());
                Severity severity = member.externalContract() && !allowed
                        ? Severity.BREAKING
                        : Severity.REVIEW;
                changes.add(new Change(ChangeKind.REMOVED, severity, member, allowed));
            }
        }
        for (ApiMember member : after.values()) {
            if (!before.containsKey(member.fingerprint())) {
                changes.add(new Change(ChangeKind.ADDED, Severity.ADDITIVE, member, false));
            }
        }
        changes.sort(Comparator
                .comparing(Change::severity)
                .thenComparing(change -> change.member().sourcePath())
                .thenComparing(change -> change.member().owner())
                .thenComparing(change -> change.member().signature()));
        return new Result(
                changes,
                changes.stream().noneMatch(change -> change.severity() == Severity.BREAKING));
    }

    public static Set<String> readAllowlist(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) return Set.of();
        Set<String> result = new HashSet<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("sourcePath\t")) continue;
            String[] fields = line.split("\\t", -1);
            if (fields.length < 4 || fields[1].isBlank() || fields[2].isBlank() || fields[3].isBlank()) {
                throw new IllegalArgumentException(
                        "API removal allowlist requires sourcePath, fingerprint, replacement and evidence");
            }
            result.add(fields[1]);
        }
        return Set.copyOf(result);
    }

    private static Map<String, ApiMember> mapByFingerprint(JavaApiSnapshot snapshot) {
        Map<String, ApiMember> result = new HashMap<>();
        for (ApiMember member : snapshot.members()) {
            ApiMember previous = result.put(member.fingerprint(), member);
            if (previous != null) {
                throw new IllegalStateException("duplicate API member fingerprint: " + member.fingerprint());
            }
        }
        return result;
    }

    public enum ChangeKind {
        ADDED,
        REMOVED
    }

    public enum Severity {
        ADDITIVE,
        REVIEW,
        BREAKING
    }

    public record Change(ChangeKind kind, Severity severity, ApiMember member, boolean allowlisted) {
    }

    public record Result(List<Change> changes, boolean compatible) {
        public Result {
            changes = List.copyOf(changes);
        }
    }
}
