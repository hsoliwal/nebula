// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deduplicated production catalog over pinned LeetCode, HackerRank and GeeksforGeeks Java files.
 *
 * <p>File-level provenance remains in {@link ProblemAdapterCatalog}. This layer groups equivalent
 * challenge implementations without copying donor source and exposes one stable logical problem
 * identity for application code.</p>
 */
public final class ChallengeCatalog {

    record ParsedSource(String numericId, String slug, String title) {}

    private static final Pattern NUMBERED_TITLE =
            Pattern.compile("^\\s*0*([0-9]+)[._ :\\-]+(.+?)\\s*$");
    private static final Pattern PREFIXED_NUMBER =
            Pattern.compile(
                    "(?i)^\\s*(?:leetcode|lc|problem)[._ :\\-]*0*([0-9]+)"
                            + "[._ :\\-]+(.+?)\\s*$");
    private static final Pattern RANGE = Pattern.compile("^\\d+\\s*-\\s*\\d+$");

    private static final List<ChallengeProblem> CHALLENGES = build();
    private static final Map<String, ChallengeProblem> BY_ID = byId();

    private ChallengeCatalog() {}

    public static List<ChallengeProblem> all() {
        return CHALLENGES;
    }

    public static ChallengeProblem require(String stableId) {
        ChallengeProblem result = BY_ID.get(Objects.requireNonNull(stableId, "stableId"));
        if (result == null) throw new IllegalArgumentException("unknown challenge: " + stableId);
        return result;
    }

    public static List<ChallengeProblem> byPlatform(ChallengePlatform platform) {
        Objects.requireNonNull(platform, "platform");
        return CHALLENGES.stream()
                .filter(problem -> problem.id().platform() == platform)
                .toList();
    }

    public static List<ChallengeProblem> executable() {
        return CHALLENGES.stream().filter(ChallengeProblem::executable).toList();
    }

    public static List<ChallengeProblem> unresolved() {
        return CHALLENGES.stream()
                .filter(problem -> problem.status() == ChallengeProblem.Status.UNCLASSIFIED)
                .toList();
    }

    public static List<ChallengeProblem> conflicts() {
        return CHALLENGES.stream()
                .filter(problem ->
                        problem.status() == ChallengeProblem.Status.CLASSIFICATION_CONFLICT)
                .toList();
    }

    public static int sourceImplementationCount() {
        return ProblemAdapterCatalog.all().size();
    }

    public static int sourceImplementationCount(ChallengePlatform platform) {
        Objects.requireNonNull(platform, "platform");
        return Math.toIntExact(
                ProblemAdapterCatalog.all().stream()
                        .filter(adapter ->
                                ChallengePlatform.from(adapter.source().platform()) == platform)
                        .count());
    }

    public static Map<ChallengePlatform, Integer> logicalProblemCounts() {
        EnumMap<ChallengePlatform, Integer> counts = new EnumMap<>(ChallengePlatform.class);
        for (ChallengeProblem problem : CHALLENGES) {
            counts.merge(problem.id().platform(), 1, Math::addExact);
        }
        return Collections.unmodifiableMap(counts);
    }

    private static List<ChallengeProblem> build() {
        List<ProblemAdapter> adapters = ProblemAdapterCatalog.all().stream()
                .sorted(Comparator.comparing(ProblemAdapter::id))
                .toList();

        Map<String, Set<String>> numericIdsBySlug = new LinkedHashMap<>();
        Map<String, ParsedSource> parsedByAdapter = new LinkedHashMap<>();
        for (ProblemAdapter adapter : adapters) {
            ParsedSource parsed = parse(adapter.source());
            parsedByAdapter.put(adapter.id(), parsed);
            if (ChallengePlatform.from(adapter.source().platform()) == ChallengePlatform.LEETCODE
                    && parsed.numericId() != null) {
                numericIdsBySlug
                        .computeIfAbsent(parsed.slug(), ignored -> new LinkedHashSet<>())
                        .add(parsed.numericId());
            }
        }

        Map<String, Group> groups = new LinkedHashMap<>();
        for (ProblemAdapter adapter : adapters) {
            ChallengePlatform platform = ChallengePlatform.from(adapter.source().platform());
            ParsedSource parsed = parsedByAdapter.get(adapter.id());
            String externalId = parsed.numericId();
            if (platform == ChallengePlatform.LEETCODE && externalId == null) {
                Set<String> aliases = numericIdsBySlug.get(parsed.slug());
                if (aliases != null && aliases.size() == 1) externalId = aliases.iterator().next();
            }
            if (externalId == null) externalId = parsed.slug();
            String resolvedExternalId = externalId;
            String stableId =
                    platform.name().toLowerCase(Locale.ROOT) + ":" + resolvedExternalId;
            groups.computeIfAbsent(
                            stableId,
                            ignored -> new Group(platform, resolvedExternalId))
                    .add(adapter, parsed);
        }

        ArrayList<ChallengeProblem> result = new ArrayList<>(groups.size());
        for (Group group : groups.values()) result.add(group.freeze());
        result.sort(Comparator.comparing(ChallengeProblem::id));
        return List.copyOf(result);
    }

    private static Map<String, ChallengeProblem> byId() {
        LinkedHashMap<String, ChallengeProblem> result = new LinkedHashMap<>();
        for (ChallengeProblem problem : CHALLENGES) {
            ChallengeProblem old = result.putIfAbsent(problem.id().stableId(), problem);
            if (old != null) {
                throw new ExceptionInInitializerError(
                        "duplicate logical challenge id: " + problem.id().stableId());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    static ParsedSource parse(CorpusSourceEntry source) {
        Objects.requireNonNull(source, "source");
        String problemName = source.problemName();
        ChallengePlatform platform = ChallengePlatform.from(source.platform());
        // GeeksforGeeks titles such as "0 - 1 Knapsack Problem" contain numbers that
        // are part of the title, not a stable platform problem number.
        ParsedSource direct = platform == ChallengePlatform.GEEKSFORGEEKS
                ? new ParsedSource(null, slug(problemName), problemName)
                : parseSegment(problemName);
        String numericId = direct.numericId();
        String title = direct.title();

        if (numericId == null && platform == ChallengePlatform.LEETCODE) {
            String[] segments = source.path().replace('\\', '/').split("/");
            for (int i = segments.length - 1; i >= 0; i--) {
                String segment = stripJavaSuffix(segments[i]);
                if (segment.isBlank() || RANGE.matcher(segment).matches()) continue;
                ParsedSource candidate = parseSegment(segment);
                if (candidate.numericId() != null) {
                    numericId = candidate.numericId();
                    if (isGenericName(problemName)) title = candidate.title();
                    break;
                }
            }
        }

        String slug = slug(title);
        if (slug.isEmpty()) slug = slug(problemName);
        if (slug.isEmpty()) slug = slug(source.path());
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("cannot derive challenge slug from " + source.path());
        }
        return new ParsedSource(numericId, slug, title);
    }

    private static ParsedSource parseSegment(String raw) {
        String value = stripJavaSuffix(Objects.requireNonNull(raw, "raw")).trim();
        Matcher numbered = NUMBERED_TITLE.matcher(value);
        if (numbered.matches()) {
            return new ParsedSource(
                    ChallengeId.stripLeadingZeros(numbered.group(1)),
                    slug(numbered.group(2)),
                    numbered.group(2).trim());
        }
        Matcher prefixed = PREFIXED_NUMBER.matcher(value);
        if (prefixed.matches()) {
            return new ParsedSource(
                    ChallengeId.stripLeadingZeros(prefixed.group(1)),
                    slug(prefixed.group(2)),
                    prefixed.group(2).trim());
        }
        return new ParsedSource(null, slug(value), value);
    }

    private static String slug(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = Character.toLowerCase(value.charAt(i));
            if (Character.isLetterOrDigit(current)) out.append(current);
        }
        return out.toString();
    }

    static String normalizeSlug(String titleOrSlug) {
        String value = Objects.requireNonNull(titleOrSlug, "titleOrSlug").trim();
        String normalized = slug(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("titleOrSlug must contain letters/digits");
        }
        return normalized;
    }

    private static String stripJavaSuffix(String value) {
        return value.endsWith(".java") ? value.substring(0, value.length() - 5) : value;
    }

    private static boolean isGenericName(String value) {
        return value.matches("(?i)(solution|main)(?:\\d+|[_-].*)?");
    }

    private static final class Group {
        private final ChallengePlatform platform;
        private final String externalId;
        private final ArrayList<ProblemAdapter> implementations = new ArrayList<>();
        private final ArrayList<ParsedSource> parsed = new ArrayList<>();

        private Group(ChallengePlatform platform, String externalId) {
            this.platform = platform;
            this.externalId = externalId;
        }

        private void add(ProblemAdapter adapter, ParsedSource source) {
            implementations.add(adapter);
            parsed.add(source);
        }

        private ChallengeProblem freeze() {
            int titleIndex = 0;
            for (int i = 1; i < implementations.size(); i++) {
                if (implementations.get(i).id().compareTo(implementations.get(titleIndex).id()) < 0) {
                    titleIndex = i;
                }
            }
            ParsedSource display = parsed.get(titleIndex);
            String title = display.title().isBlank() ? display.slug() : display.title();
            ChallengeId id = new ChallengeId(platform, externalId, display.slug());
            return new ChallengeProblem(id, title, implementations);
        }
    }
}
