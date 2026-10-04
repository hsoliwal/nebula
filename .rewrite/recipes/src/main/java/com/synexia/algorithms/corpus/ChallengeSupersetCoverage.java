// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic implementation queue over every logical pinned challenge.
 *
 * <p>A shape-classified challenge is executable, but it is not called source-supersetted until
 * multiple real donor bodies have been compared and recorded in {@link ChallengeSupersetCatalog}.
 * This keeps "we have an algorithm shape" distinct from "we consolidated this problem's observed
 * Java variants". The tri-platform review surface covers LeetCode, HackerRank and GeeksforGeeks
 * uniformly; none of those public problem sites grants source-copy authority.</p>
 */
public final class ChallengeSupersetCoverage {
    public enum Status {
        SOURCE_SUPERSETTED,
        SHAPE_EXECUTABLE_NOT_SUPERSETTED,
        CLASSIFICATION_CONFLICT,
        UNCLASSIFIED
    }

    public record Entry(
            String stableId,
            String title,
            ChallengePlatform platform,
            int sourceImplementationCount,
            Status status) {
        public Entry {
            stableId = required(stableId, "stableId");
            title = required(title, "title");
            platform = Objects.requireNonNull(platform, "platform");
            status = Objects.requireNonNull(status, "status");
            if (sourceImplementationCount < 1) {
                throw new IllegalArgumentException("sourceImplementationCount");
            }
        }
    }

    public record PlatformSummary(
            ChallengePlatform platform,
            int logicalProblems,
            int sourceImplementations,
            int sourceSupersetted,
            int shapeExecutableNotSupersetted,
            int classificationConflicts,
            int unclassified) {
        public PlatformSummary {
            platform = Objects.requireNonNull(platform, "platform");
            if (logicalProblems < 0
                    || sourceImplementations < 0
                    || sourceSupersetted < 0
                    || shapeExecutableNotSupersetted < 0
                    || classificationConflicts < 0
                    || unclassified < 0
                    || logicalProblems
                            != sourceSupersetted
                                    + shapeExecutableNotSupersetted
                                    + classificationConflicts
                                    + unclassified) {
                throw new IllegalArgumentException("platform summary partition");
            }
        }

        public int pending() {
            return logicalProblems - sourceSupersetted;
        }
    }

    private static final List<ChallengePlatform> REVIEW_PLATFORMS =
            List.of(
                    ChallengePlatform.LEETCODE,
                    ChallengePlatform.HACKERRANK,
                    ChallengePlatform.GEEKSFORGEEKS);

    private ChallengeSupersetCoverage() {}

    public static List<ChallengePlatform> reviewPlatforms() {
        return REVIEW_PLATFORMS;
    }

    public static List<Entry> all() {
        ArrayList<Entry> result = new ArrayList<>(ChallengeCatalog.all().size());
        for (ChallengeProblem problem : ChallengeCatalog.all()) {
            result.add(entry(problem));
        }
        result.sort(
                java.util.Comparator
                        .comparingInt((Entry value) -> priority(value.status()))
                        .thenComparing(
                                java.util.Comparator.comparingInt(Entry::sourceImplementationCount)
                                        .reversed())
                        .thenComparing(Entry::stableId));
        return List.copyOf(result);
    }

    /** Highest-value remaining problem-level consolidation queue. */
    public static List<Entry> pending() {
        return all().stream()
                .filter(entry -> entry.status() != Status.SOURCE_SUPERSETTED)
                .toList();
    }

    /** Platform-bounded queue preserving the canonical global review order. */
    public static List<Entry> pending(ChallengePlatform platform) {
        ChallengePlatform checked = reviewPlatform(platform);
        return pending().stream().filter(entry -> entry.platform() == checked).toList();
    }

    /**
     * Returns the next bounded review tranche for one challenge platform.
     *
     * <p>This is catalogue planning only. It does not fetch public-site source, admit a donor, or
     * authorize replacement.</p>
     */
    public static List<Entry> nextPending(ChallengePlatform platform, int limit) {
        if (limit < 1 || limit > 100_000) throw new IllegalArgumentException("limit");
        return pending(platform).stream().limit(limit).toList();
    }

    public static Map<Status, Integer> counts() {
        EnumMap<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Entry entry : all()) counts.merge(entry.status(), 1, Math::addExact);
        return Map.copyOf(counts);
    }

    public static PlatformSummary platformSummary(ChallengePlatform platform) {
        return platformSummaries().get(reviewPlatform(platform));
    }

    public static Map<ChallengePlatform, PlatformSummary> platformSummaries() {
        EnumMap<ChallengePlatform, int[]> counts = new EnumMap<>(ChallengePlatform.class);
        for (ChallengePlatform platform : REVIEW_PLATFORMS) counts.put(platform, new int[6]);
        for (Entry entry : all()) {
            int[] row = counts.get(entry.platform());
            if (row == null) continue;
            row[0] = Math.addExact(row[0], 1);
            row[1] = Math.addExact(row[1], entry.sourceImplementationCount());
            switch (entry.status()) {
                case SOURCE_SUPERSETTED -> row[2] = Math.addExact(row[2], 1);
                case SHAPE_EXECUTABLE_NOT_SUPERSETTED -> row[3] = Math.addExact(row[3], 1);
                case CLASSIFICATION_CONFLICT -> row[4] = Math.addExact(row[4], 1);
                case UNCLASSIFIED -> row[5] = Math.addExact(row[5], 1);
            }
        }
        EnumMap<ChallengePlatform, PlatformSummary> result =
                new EnumMap<>(ChallengePlatform.class);
        for (ChallengePlatform platform : REVIEW_PLATFORMS) {
            int[] row = counts.get(platform);
            result.put(
                    platform,
                    new PlatformSummary(
                            platform, row[0], row[1], row[2], row[3], row[4], row[5]));
        }
        return Map.copyOf(result);
    }

    public static int supersettedSourceImplementationCount() {
        int total = 0;
        for (ChallengeSupersetCatalog.ProblemSuperset problem : ChallengeSupersetCatalog.all()) {
            ChallengeProblem logical = ChallengeCatalog.require(problem.stableId());
            total = Math.addExact(total, logical.implementationCount());
        }
        return total;
    }

    /**
     * Deterministic tri-platform evidence report.
     *
     * <p>The report is deliberately metadata-only: problem identity, classification state,
     * implementation count, canonical shape and admitted source-superset provenance count.</p>
     */
    public static String renderTsv(IProgressMonitor suppliedMonitor) {
        IProgressMonitor monitor = ProgressMonitors.nonNull(suppliedMonitor);
        List<Entry> rows = all();
        monitor.beginTask("render-challenge-superset-coverage", rows.size());
        StringBuilder out = new StringBuilder(Math.max(4096, rows.size() * 176));
        out.append(
                "challengeId\tplatform\ttitle\timplementations\tstatus\tshape"
                        + "\tkernel\tdonorOrigins\n");
        try {
            for (Entry row : rows) {
                monitor.checkCanceled();
                ChallengeProblem problem = ChallengeCatalog.require(row.stableId());
                var admitted = ChallengeSupersetCatalog.find(row.stableId());
                out.append(clean(row.stableId())).append('\t')
                        .append(row.platform()).append('\t')
                        .append(clean(row.title())).append('\t')
                        .append(row.sourceImplementationCount()).append('\t')
                        .append(row.status()).append('\t')
                        .append(problem.shape().map(Enum::name).orElse("")).append('\t')
                        .append(admitted.map(ChallengeSupersetCatalog.ProblemSuperset::kernel)
                                .map(ChallengeSupersetCoverage::clean)
                                .orElse(""))
                        .append('\t')
                        .append(admitted.map(value -> value.donors().size()).orElse(0))
                        .append('\n');
                monitor.worked(1);
            }
            return out.toString();
        } finally {
            monitor.done();
        }
    }

    public static String root(IProgressMonitor monitor) {
        return sha256(renderTsv(monitor));
    }

    public static String root() {
        return root(IProgressMonitor.noop());
    }

    private static Entry entry(ChallengeProblem problem) {
        String stableId = problem.id().stableId();
        Status status;
        if (ChallengeSupersetCatalog.contains(stableId)) {
            status = Status.SOURCE_SUPERSETTED;
        } else {
            status =
                    switch (problem.status()) {
                        case EXECUTABLE -> Status.SHAPE_EXECUTABLE_NOT_SUPERSETTED;
                        case CLASSIFICATION_CONFLICT -> Status.CLASSIFICATION_CONFLICT;
                        case UNCLASSIFIED -> Status.UNCLASSIFIED;
                    };
        }
        return new Entry(
                stableId,
                problem.title(),
                problem.id().platform(),
                problem.implementationCount(),
                status);
    }

    private static int priority(Status status) {
        return switch (status) {
            case CLASSIFICATION_CONFLICT -> 0;
            case UNCLASSIFIED -> 1;
            case SHAPE_EXECUTABLE_NOT_SUPERSETTED -> 2;
            case SOURCE_SUPERSETTED -> 3;
        };
    }

    private static ChallengePlatform reviewPlatform(ChallengePlatform platform) {
        ChallengePlatform checked = Objects.requireNonNull(platform, "platform");
        if (!REVIEW_PLATFORMS.contains(checked)) {
            throw new IllegalArgumentException("unsupported review platform: " + checked);
        }
        return checked;
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String clean(String value) {
        return value.replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('|', '/');
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
