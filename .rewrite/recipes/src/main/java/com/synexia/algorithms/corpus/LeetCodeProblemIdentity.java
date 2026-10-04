// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Repository-aware LeetCode problem identity without generic trailing-digit guessing. */
public final class LeetCodeProblemIdentity {

    public enum Evidence {
        TRUSTED_NUMBER,
        VALIDATED_NUMBER,
        UNIQUE_TITLE_ALIAS
    }

    public record Identity(int number, Evidence evidence, String titleKey) {
        public Identity {
            if (number < 1) throw new IllegalArgumentException("problem number must be positive");
            Objects.requireNonNull(evidence, "evidence");
            titleKey = titleKey == null ? "" : titleKey;
        }
    }

    private static final Pattern QIYUAN = Pattern.compile("^(\\d{3,4})_");
    private static final Pattern BLANKJ = Pattern.compile("/_(\\d{4})/");
    private static final Pattern FISHER = Pattern.compile("^_(\\d{1,4})(?:Test)?$");
    private static final Pattern DOOCS =
            Pattern.compile("^solution/\\d{4}-\\d{4}/(\\d{4})\\.[^/]+/");
    private static final Pattern DOOCS_TITLE =
            Pattern.compile("^solution/\\d{4}-\\d{4}/\\d{4}\\.([^/]+)/");
    private static final Pattern LIWEIWEI =
            Pattern.compile("/(\\d{4})-[^/]+/src/");
    private static final Pattern LIWEIWEI_TITLE =
            Pattern.compile("/\\d{4}-([^/]+)/src/");
    private static final Pattern FLUENCY =
            Pattern.compile("[A-Za-z](\\d{1,4})$");

    private static final Pattern JAVADEV = Pattern.compile(
            "^src/(?:main|test)/java/g(\\d{4})_(\\d{4})/s(\\d{4})_([a-z0-9_]+)/[^/]+\\.java$");

    private LeetCodeProblemIdentity() {}

    /** Resolve all identities together so title aliases are globally ambiguity-checked. */
    public static Map<CorpusSourceEntry, Identity> resolveAll(List<CorpusSourceEntry> sources) {
        Objects.requireNonNull(sources, "sources");

        Map<String, Integer> aliases = uniqueStrongTitleAliases(sources);
        LinkedHashMap<CorpusSourceEntry, Identity> identities = new LinkedHashMap<>();

        for (CorpusSourceEntry source : sources) {
            if (!isLeetCode(source)) continue;

            OptionalInt trusted = strongNumber(source);
            String key = titleKey(source);
            if (trusted.isPresent()) {
                identities.put(
                        source,
                        new Identity(trusted.getAsInt(), Evidence.TRUSTED_NUMBER, key));
                continue;
            }

            if (source.repository().equals("fluency03/leetcode-java")) {
                OptionalInt candidate = fluencyCandidate(source.path());
                Integer aliased = key.isEmpty() ? null : aliases.get(key);
                if (candidate.isPresent()
                        && aliased != null
                        && aliased == candidate.getAsInt()) {
                    identities.put(
                            source,
                            new Identity(candidate.getAsInt(), Evidence.VALIDATED_NUMBER, key));
                    continue;
                }
            }

            Integer alias = key.isEmpty() ? null : aliases.get(key);
            if (alias != null) {
                identities.put(
                        source,
                        new Identity(alias, Evidence.UNIQUE_TITLE_ALIAS, key));
            }
        }
        return Collections.unmodifiableMap(identities);
    }

    /** Trusted repository-specific numeric encoding, before descriptive alias expansion. */
    public static OptionalInt trustedNumber(CorpusSourceEntry source) {
        Objects.requireNonNull(source, "source");
        return isLeetCode(source) ? strongNumber(source) : OptionalInt.empty();
    }

    /** Normalized descriptive title used only for ambiguity-checked cross-repository aliases. */
    public static String titleKey(CorpusSourceEntry source) {
        Objects.requireNonNull(source, "source");
        if (!isLeetCode(source)) return "";

        String title = switch (source.repository()) {
            case "qiyuangong/leetcode" -> {
                String stem = stem(source.path());
                Matcher matcher = QIYUAN.matcher(stem);
                yield matcher.find()
                        ? stem.substring(matcher.end()).replace('_', ' ')
                        : "";
            }
            case "doocs/leetcode" -> group(DOOCS_TITLE, source.path());
            case "javadev/LeetCode-in-Java" -> {
                Matcher matcher = javadevPath(source.path());
                yield matcher == null ? "" : matcher.group(4).replace('_', ' ');
            }
            case "liweiwei1419/LeetCode-Solutions-in-Good-Style" ->
                    group(LIWEIWEI_TITLE, source.path()).replace('-', ' ');
            case "fluency03/leetcode-java" ->
                    stem(source.path()).replaceFirst("\\d{1,4}$", "");
            case "varunu28/LeetCode-Java-Solutions",
                    "gouthampradhan/leetcode",
                    "leetcoders/LeetCode-Java" -> stem(source.path());
            default -> "";
        };
        String normalized = normalize(title);
        if (normalized.length() < 4) return "";
        boolean generic = switch (normalized) {
            case "solution", "main", "treenode", "listnode", "interval", "node" -> true;
            default -> false;
        };
        return generic ? "" : normalized;
    }

    private static Map<String, Integer> uniqueStrongTitleAliases(List<CorpusSourceEntry> sources) {
        LinkedHashMap<String, Integer> aliases = new LinkedHashMap<>();
        java.util.HashSet<String> ambiguous = new java.util.HashSet<>();

        for (CorpusSourceEntry source : sources) {
            if (!isStrongDescriptiveRepository(source.repository())) continue;
            OptionalInt number = strongNumber(source);
            String key = titleKey(source);
            if (number.isEmpty() || key.isEmpty()) continue;

            Integer previous = aliases.putIfAbsent(key, number.getAsInt());
            if (previous != null && previous != number.getAsInt()) {
                ambiguous.add(key);
            }
        }
        ambiguous.forEach(aliases::remove);
        return Collections.unmodifiableMap(aliases);
    }

    private static OptionalInt strongNumber(CorpusSourceEntry source) {
        String repository = source.repository();
        String path = source.path();
        return switch (repository) {
            case "qiyuangong/leetcode" -> match(QIYUAN, stem(path));
            case "Blankj/awesome-java-leetcode" -> match(BLANKJ, path);
            case "fishercoder1534/Leetcode" -> match(FISHER, stem(path));
            case "doocs/leetcode" -> match(DOOCS, path);
            case "javadev/LeetCode-in-Java" -> {
                Matcher matcher = javadevPath(path);
                yield matcher == null ? OptionalInt.empty()
                        : OptionalInt.of(Integer.parseInt(matcher.group(3)));
            }
            case "liweiwei1419/LeetCode-Solutions-in-Good-Style" -> match(LIWEIWEI, path);
            default -> OptionalInt.empty();
        };
    }

    /** A malformed bucket must not become a trusted number or an alias seed. */
    private static Matcher javadevPath(String path) {
        Matcher matcher = JAVADEV.matcher(path);
        if (!matcher.matches()) return null;
        int first = Integer.parseInt(matcher.group(1));
        int last = Integer.parseInt(matcher.group(2));
        int number = Integer.parseInt(matcher.group(3));
        if (first < 1 || (first - 1) % 100 != 0 || last != first + 99
                || number < first || number > last) return null;
        return matcher;
    }

    private static OptionalInt fluencyCandidate(String path) {
        return match(FLUENCY, stem(path));
    }

    private static OptionalInt match(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) return OptionalInt.empty();
        try {
            int number = Integer.parseInt(matcher.group(1));
            return number > 0 ? OptionalInt.of(number) : OptionalInt.empty();
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    private static String group(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static boolean isStrongDescriptiveRepository(String repository) {
        return repository.equals("qiyuangong/leetcode")
                || repository.equals("doocs/leetcode")
                || repository.equals("javadev/LeetCode-in-Java")
                || repository.equals("liweiwei1419/LeetCode-Solutions-in-Good-Style");
    }

    private static boolean isLeetCode(CorpusSourceEntry source) {
        return source.platform().equalsIgnoreCase("LEETCODE");
    }

    private static String stem(String path) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.endsWith(".java") ? name.substring(0, name.length() - 5) : name;
    }

    private static String normalize(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) out.append(c);
        }
        return out.toString();
    }
}
