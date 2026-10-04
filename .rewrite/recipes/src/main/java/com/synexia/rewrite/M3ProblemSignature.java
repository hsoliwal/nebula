// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic candidate signature for one source file.
 *
 * <p>The classifier uses only bounded lexical/API evidence. A shape is a search hint, never proof
 * that the file implements the corresponding algorithm and never replacement authority.</p>
 */
public record M3ProblemSignature(
        String sourcePath,
        List<Candidate> candidates,
        List<String> searchTerms,
        String root) {

    private static final Pattern IDENTIFIER =
            Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]{2,63}");
    private static final int MAX_SOURCE_CHARS = 2_000_000;
    private static final int MAX_TERMS = 24;
    private static final int MAX_CANDIDATES = 8;

    public record Candidate(
            AlgorithmShape shape,
            int score,
            List<String> evidence) {
        public Candidate {
            shape = Objects.requireNonNull(shape, "shape");
            if (score < 1 || score > 100) throw new IllegalArgumentException("score");
            evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
            if (evidence.isEmpty()) throw new IllegalArgumentException("evidence");
        }
    }

    public M3ProblemSignature {
        sourcePath = path(sourcePath);
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        searchTerms = List.copyOf(Objects.requireNonNull(searchTerms, "searchTerms"));
        root = sha(root, "root");
    }

    public static M3ProblemSignature fromSource(String sourcePath, String source) {
        String path = path(sourcePath);
        String text = Objects.requireNonNull(source, "source");
        if (text.length() > MAX_SOURCE_CHARS) {
            throw new IllegalArgumentException("source too large for candidate classifier");
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Map<AlgorithmShape, MutableCandidate> found = new LinkedHashMap<>();

        cue(found, lower, AlgorithmShape.BINARY_SEARCH, 100,
                "arrays.binarysearch", "collections.binarysearch", "binarysearch", "binary_search");
        cue(found, lower, AlgorithmShape.PRIORITY_QUEUE, 90,
                "priorityqueue", "priority_queue");
        cue(found, lower, AlgorithmShape.DIJKSTRA, 98,
                "dijkstra", "shortestpath", "shortest_path");
        if (contains(lower, "priorityqueue") && containsAny(lower, "distance", " dist", "shortest")) {
            add(found, AlgorithmShape.DIJKSTRA, 88, "priority-queue + distance cue");
        }

        cue(found, lower, AlgorithmShape.TOPOLOGICAL_SORT, 98,
                "topological", "indegree", "in_degree");
        cue(found, lower, AlgorithmShape.STRONGLY_CONNECTED_COMPONENTS, 98,
                "tarjan", "kosaraju", "lowlink", "low_link", "stronglyconnected");
        if (containsAny(lower, "unionfind", "disjointset", "disjoint_set")
                || (contains(lower, "parent") && contains(lower, "union(") && contains(lower, "find("))) {
            add(found, AlgorithmShape.UNION_FIND, 94, "union/find parent-set cue");
        }

        cue(found, lower, AlgorithmShape.BFS, 90, "bfs", "breadthfirst", "breadth_first");
        if (containsAny(lower, "arraydeque", "queue<", "queue ")
                && containsAny(lower, "visited", "seen")
                && containsAny(lower, "adj", "neighbor", "neighbour", "edge")) {
            add(found, AlgorithmShape.BFS, 82, "queue + visited + graph cue");
        }

        cue(found, lower, AlgorithmShape.DFS, 90, "dfs", "depthfirst", "depth_first");
        if (containsAny(lower, "visited", "seen")
                && containsAny(lower, "adj", "neighbor", "neighbour", "edge")
                && containsAny(lower, "recursive", "recurse")) {
            add(found, AlgorithmShape.DFS, 72, "visited + graph + recursion cue");
        }

        cue(found, lower, AlgorithmShape.TRIE, 96, "trie", "prefixtree", "prefix_tree");
        cue(found, lower, AlgorithmShape.AHO_CORASICK, 100, "ahocorasick", "aho_corasick");
        cue(found, lower, AlgorithmShape.KMP, 100, "knuthmorrispratt", "kmp", "prefixfunction", "lps[");
        cue(found, lower, AlgorithmShape.RABIN_KARP, 100, "rabinkarp", "rabin_karp", "rollinghash");
        cue(found, lower, AlgorithmShape.SEGMENT_TREE, 100, "segmenttree", "segment_tree");
        cue(found, lower, AlgorithmShape.FENWICK_TREE, 100, "fenwick", "binaryindexedtree", "binary_indexed_tree");

        cue(found, lower, AlgorithmShape.SLIDING_WINDOW, 94, "slidingwindow", "sliding_window");
        if (contains(lower, "window")
                && containsAny(lower, "left", "start")
                && containsAny(lower, "right", "end")) {
            add(found, AlgorithmShape.SLIDING_WINDOW, 78, "window + boundary pointers");
        }
        cue(found, lower, AlgorithmShape.TWO_POINTER, 88, "twopointer", "two_pointer");
        if (!contains(lower, "window")
                && containsAny(lower, "left", "lo")
                && containsAny(lower, "right", "hi")) {
            add(found, AlgorithmShape.TWO_POINTER, 58, "paired boundary identifiers");
        }

        cue(found, lower, AlgorithmShape.PREFIX_SCAN, 94,
                "prefixsum", "prefix_sum", "prefixscan", "prefix_scan");
        cue(found, lower, AlgorithmShape.DYNAMIC_PROGRAMMING, 92,
                "dynamicprogramming", "dynamic_programming", "dp[", "memo[", "memoization");
        cue(found, lower, AlgorithmShape.BACKTRACKING, 92,
                "backtrack", "backtracking");
        cue(found, lower, AlgorithmShape.KADANE, 100, "kadane");
        cue(found, lower, AlgorithmShape.LONGEST_COMMON_SUBSEQUENCE, 100,
                "longestcommonsubsequence", "longest_common_subsequence", "lcs[");
        cue(found, lower, AlgorithmShape.KNAPSACK_01, 100, "knapsack");
        cue(found, lower, AlgorithmShape.COIN_CHANGE, 100, "coinchange", "coin_change");

        if (containsAny(lower, "hashmap", "hashset", "concurrenthashmap")) {
            add(found, AlgorithmShape.HASH_MEMBERSHIP, 74, "hash collection cue");
        }
        if (containsAny(lower, "frequency", "counts", "counter")
                && containsAny(lower, "hashmap", "map<")) {
            add(found, AlgorithmShape.FREQUENCY_COUNT, 80, "frequency map cue");
        }
        if (containsAny(lower, "arrays.sort(", "collections.sort(", ".sorted(")) {
            add(found, AlgorithmShape.SORT_UNIQUE, 45, "generic ordered-data cue");
        }
        if (found.isEmpty() && containsAny(lower, "for(", "for (", "while(", "while (")) {
            add(found, AlgorithmShape.LINEAR_SCAN, 30, "bounded loop fallback");
        }

        List<Candidate> candidates =
                found.values().stream()
                        .map(MutableCandidate::freeze)
                        .sorted(Comparator.comparingInt(Candidate::score).reversed()
                                .thenComparing(candidate -> candidate.shape().name()))
                        .limit(MAX_CANDIDATES)
                        .toList();

        TreeSet<String> terms = new TreeSet<>();
        Matcher matcher = IDENTIFIER.matcher(text);
        while (matcher.find() && terms.size() < MAX_TERMS * 4) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (!stop(token)) terms.add(token);
        }
        addPathTerms(path, terms);
        List<String> searchTerms = terms.stream().limit(MAX_TERMS).toList();

        String root = root(path, candidates, searchTerms);
        return new M3ProblemSignature(path, candidates, searchTerms, root);
    }

    public boolean replacementAuthority() {
        return false;
    }

    private static void cue(
            Map<AlgorithmShape, MutableCandidate> found,
            String source,
            AlgorithmShape shape,
            int score,
            String... needles) {
        for (String needle : needles) {
            if (contains(source, needle)) add(found, shape, score, "lexical cue:" + needle);
        }
    }

    private static void add(
            Map<AlgorithmShape, MutableCandidate> found,
            AlgorithmShape shape,
            int score,
            String evidence) {
        found.computeIfAbsent(shape, MutableCandidate::new).add(score, evidence);
    }

    private static boolean contains(String value, String needle) {
        return value.contains(needle);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static void addPathTerms(String path, TreeSet<String> terms) {
        Matcher matcher = IDENTIFIER.matcher(path);
        while (matcher.find()) terms.add(matcher.group().toLowerCase(Locale.ROOT));
    }

    private static boolean stop(String token) {
        return switch (token) {
            case "public", "private", "protected", "static", "final", "class", "interface",
                    "record", "enum", "void", "return", "new", "null", "true", "false",
                    "this", "super", "string", "integer", "long", "double", "float",
                    "boolean", "object", "override", "throws", "throw", "package", "import",
                    "java", "util", "get", "set" -> true;
            default -> false;
        };
    }

    private static String root(
            String path,
            List<Candidate> candidates,
            List<String> terms) {
        MessageDigest digest = digest();
        frame(digest, "SYNEXIA_M3_PROBLEM_SIGNATURE_V1");
        frame(digest, path);
        for (Candidate candidate : candidates) {
            frame(digest, candidate.shape().name());
            frame(digest, Integer.toString(candidate.score()));
            candidate.evidence().forEach(value -> frame(digest, value));
        }
        terms.forEach(value -> frame(digest, value));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static String path(String value) {
        String result = Objects.requireNonNull(value, "sourcePath").replace('\\', '/').strip();
        if (result.isEmpty() || result.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourcePath");
        }
        return result;
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }

    private static final class MutableCandidate {
        private final AlgorithmShape shape;
        private int score;
        private final TreeSet<String> evidence = new TreeSet<>();

        private MutableCandidate(AlgorithmShape shape) {
            this.shape = shape;
        }

        private void add(int value, String why) {
            score = Math.max(score, value);
            evidence.add(why);
        }

        private Candidate freeze() {
            return new Candidate(shape, score, List.copyOf(evidence));
        }
    }
}
