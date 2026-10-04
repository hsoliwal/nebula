// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded round-robin challenge evidence across LeetCode, HackerRank and GeeksForGeeks.
 *
 * <p>Category/problem rows are discovery evidence only. They never grant source-copy,
 * replacement, execution or promotion authority. The bounded round-robin avoids a global
 * top-N query being accidentally dominated by one platform when multiple platforms contain
 * evidence for the same algorithm shape.</p>
 */
public final class ChallengeBalancedEvidence {

    public record PlatformEvidence(
            ChallengePlatform platform,
            List<String> categoryIds,
            List<String> problemIds,
            int sourceImplementations,
            String root) {

        public PlatformEvidence {
            platform = Objects.requireNonNull(platform, "platform");
            categoryIds = stable(categoryIds, "categoryIds");
            problemIds = stable(problemIds, "problemIds");
            if (sourceImplementations < problemIds.size()) {
                throw new IllegalArgumentException("sourceImplementations");
            }
            String expected =
                    ChallengeBalancedEvidence.root(
                            "SYNEXIA_CHALLENGE_BALANCED_PLATFORM_V1",
                            platform.name(),
                            String.join("\u001f", categoryIds),
                            String.join("\u001f", problemIds),
                            Integer.toString(sourceImplementations));
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("platform evidence root mismatch");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    public record Snapshot(
            AlgorithmShape shape,
            int limit,
            List<PlatformEvidence> platforms,
            List<String> categoryIds,
            List<String> problemIds,
            int sourceImplementations,
            int sourceAvailablePlatforms,
            int representedPlatforms,
            boolean balancedWithinLimit,
            String root) {

        public Snapshot {
            shape = Objects.requireNonNull(shape, "shape");
            CompetitiveCapabilityPlanIndex.Row capabilityPlan =
                    CompetitiveCapabilityPlanIndex.canonical().require(shape);
            if (limit < 1 || limit > 100_000) {
                throw new IllegalArgumentException("limit");
            }
            platforms =
                    Objects.requireNonNull(platforms, "platforms").stream()
                            .sorted(Comparator.comparing(value -> value.platform().name()))
                            .toList();
            categoryIds = stable(categoryIds, "categoryIds");
            problemIds = stable(problemIds, "problemIds");
            if (problemIds.size() > limit
                    || sourceImplementations < problemIds.size()
                    || sourceAvailablePlatforms < 0
                    || representedPlatforms < 0
                    || representedPlatforms > sourceAvailablePlatforms
                    || representedPlatforms != platforms.stream()
                            .filter(value -> !value.problemIds().isEmpty())
                            .map(PlatformEvidence::platform)
                            .distinct()
                            .count()) {
                throw new IllegalArgumentException("snapshot counts");
            }
            int required = Math.min(limit, sourceAvailablePlatforms);
            if (balancedWithinLimit != (representedPlatforms >= required)) {
                throw new IllegalArgumentException("balancedWithinLimit");
            }
            String expected =
                    ChallengeBalancedEvidence.root(
                            "SYNEXIA_CHALLENGE_BALANCED_SNAPSHOT_V1",
                            shape.name(),
                            capabilityPlan.root(),
                            Integer.toString(limit),
                            platforms.stream()
                                    .map(PlatformEvidence::root)
                                    .reduce("", (left, right) -> left + right + "\u001f"),
                            String.join("\u001f", categoryIds),
                            String.join("\u001f", problemIds),
                            Integer.toString(sourceImplementations),
                            Integer.toString(sourceAvailablePlatforms),
                            Integer.toString(representedPlatforms),
                            Boolean.toString(balancedWithinLimit));
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("snapshot root mismatch");
            }
        }

        public String capabilityPlanRoot() {
            return CompetitiveCapabilityPlanIndex.canonical().require(shape).root();
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private ChallengeBalancedEvidence() {}

    public static Snapshot forShape(AlgorithmShape shape, int limit) {
        AlgorithmShape checkedShape = Objects.requireNonNull(shape, "shape");
        if (limit < 1 || limit > 100_000) {
            throw new IllegalArgumentException("limit");
        }

        CompetitiveCapabilityPlanIndex.Row capabilityPlan =
                CompetitiveCapabilityPlanIndex.canonical().require(checkedShape);
        EnumMap<ChallengePlatform, List<ChallengeCategoryCatalog.Category>> categories =
                new EnumMap<>(ChallengePlatform.class);
        for (ChallengeCategoryCapabilityIndex.Row indexed :
                capabilityPlan.challengeCategories()) {
            ChallengeCategoryCatalog.Category category = indexed.category();
            if (ChallengeSupersetCoverage.reviewPlatforms().contains(category.platform())) {
                categories.computeIfAbsent(category.platform(), ignored -> new ArrayList<>())
                        .add(category);
            }
        }

        List<ChallengePlatform> platforms =
                ChallengeSupersetCoverage.reviewPlatforms().stream()
                        .filter(categories::containsKey)
                        .toList();

        EnumMap<ChallengePlatform, List<ChallengeSearchIndex.Hit>> available =
                new EnumMap<>(ChallengePlatform.class);
        for (ChallengePlatform platform : platforms) {
            ChallengeSearchIndex.Query query =
                    new ChallengeSearchIndex.Query(
                            java.util.Set.of(platform),
                            java.util.Set.of(checkedShape),
                            java.util.Set.of(),
                            java.util.Set.of(),
                            List.of(),
                            List.of(),
                            limit);
            available.put(platform, ChallengeSearchIndex.canonical().search(query));
        }

        EnumMap<ChallengePlatform, ArrayList<ChallengeSearchIndex.Hit>> selected =
                new EnumMap<>(ChallengePlatform.class);
        for (ChallengePlatform platform : platforms) {
            selected.put(platform, new ArrayList<>());
        }
        for (ChallengeSearchIndex.Hit hit : balance(available, limit)) {
            selected.computeIfAbsent(
                            hit.problem().id().platform(),
                            ignored -> new ArrayList<>())
                    .add(hit);
        }

        ArrayList<PlatformEvidence> platformEvidence = new ArrayList<>();
        ArrayList<String> allCategories = new ArrayList<>();
        ArrayList<String> allProblems = new ArrayList<>();
        int sourceImplementations = 0;
        int sourceAvailablePlatforms = 0;
        int representedPlatforms = 0;

        for (ChallengePlatform platform : platforms) {
            List<String> categoryIds =
                    categories.get(platform).stream()
                            .map(category -> platform.name() + ":" + category.id())
                            .sorted()
                            .toList();
            allCategories.addAll(categoryIds);

            List<ChallengeSearchIndex.Hit> selectedHits = selected.get(platform);
            List<String> problemIds =
                    selectedHits.stream()
                            .map(hit -> hit.problem().id().stableId())
                            .sorted()
                            .toList();
            allProblems.addAll(problemIds);
            int implementations =
                    selectedHits.stream()
                            .mapToInt(ChallengeSearchIndex.Hit::sourceImplementations)
                            .sum();
            sourceImplementations = Math.addExact(sourceImplementations, implementations);
            if (!available.getOrDefault(platform, List.of()).isEmpty()) {
                sourceAvailablePlatforms++;
            }
            if (!problemIds.isEmpty()) {
                representedPlatforms++;
            }
            platformEvidence.add(
                    new PlatformEvidence(
                            platform,
                            categoryIds,
                            problemIds,
                            implementations,
                            ""));
        }

        boolean balancedWithinLimit =
                representedPlatforms >= Math.min(limit, sourceAvailablePlatforms);
        return new Snapshot(
                checkedShape,
                limit,
                platformEvidence,
                allCategories,
                allProblems,
                sourceImplementations,
                sourceAvailablePlatforms,
                representedPlatforms,
                balancedWithinLimit,
                "");
    }

    /**
     * Canonical bounded round-robin over platform-local challenge hits.
     *
     * <p>LeetCode, HackerRank and GeeksForGeeks are visited in the same fixed review order used by
     * the source-superset pipeline. This is selection evidence only.</p>
     */
    public static List<ChallengeSearchIndex.Hit> balance(
            Map<ChallengePlatform, ? extends List<ChallengeSearchIndex.Hit>> local,
            int limit) {
        Objects.requireNonNull(local, "local");
        if (limit < 1 || limit > 100_000) {
            throw new IllegalArgumentException("limit");
        }
        ArrayList<ChallengeSearchIndex.Hit> selected = new ArrayList<>();
        int ordinal = 0;
        while (selected.size() < limit) {
            boolean progressed = false;
            for (ChallengePlatform platform : ChallengeSupersetCoverage.reviewPlatforms()) {
                List<ChallengeSearchIndex.Hit> rows =
                        local.containsKey(platform) ? local.get(platform) : List.of();
                if (ordinal < rows.size() && selected.size() < limit) {
                    selected.add(rows.get(ordinal));
                    progressed = true;
                }
            }
            if (!progressed) break;
            ordinal++;
        }
        return List.copyOf(selected);
    }

    private static List<String> stable(List<String> values, String field) {
        return Objects.requireNonNull(values, field).stream()
                .map(value -> required(value, field))
                .distinct()
                .sorted()
                .toList();
    }

    private static String root(String domain, String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, domain);
            for (String value : values) frame(digest, value);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static String required(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = required(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
