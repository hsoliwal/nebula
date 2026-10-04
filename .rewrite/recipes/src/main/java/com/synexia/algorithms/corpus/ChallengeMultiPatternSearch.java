// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reusable Aho-Corasick byte-pattern search with immutable precomputation.
 *
 * <p>This owner exists because the competitive-problem catalogue already maps
 * {@code AlgorithmShape.AHO_CORASICK} to a JNI-candidate multi-pattern kernel. Java remains the
 * semantic oracle. The optional native backend is a differential acceleration lane only and does
 * not define Unicode or regex semantics.</p>
 */
public final class ChallengeMultiPatternSearch {
    public static final int MAX_PATTERNS = 4_096;
    public static final int MAX_TOTAL_PATTERN_BYTES = 1 << 20;
    private static final int NATIVE_MIN_TEXT_BYTES = 1_024;
    private static final int NATIVE_MIN_PATTERNS = 4;

    private ChallengeMultiPatternSearch() {}

    /** Immutable compiled byte automaton. Duplicates are retained and report independently. */
    public static final class Prepared {
        private final byte[][] patterns;
        private final int[] fail;
        private final int[] outputLink;
        private final int[] edgeStart;
        private final int[] edgeCount;
        private final byte[] edgeLabels;
        private final int[] edgeTargets;
        private final int[] outputStart;
        private final int[] outputCount;
        private final int[] outputs;
        private final int[] prefixSubtreeCount;
        private final int emptyPatternCount;
        private final byte[] flatPatterns;
        private final int[] patternOffsets;
        private final int[] patternLengths;
        private final ByteBuffer directPatterns;
        private final IntBuffer directOffsets;
        private final IntBuffer directLengths;

        private Prepared(
                byte[][] patterns,
                int[] fail,
                int[] outputLink,
                int[] edgeStart,
                int[] edgeCount,
                byte[] edgeLabels,
                int[] edgeTargets,
                int[] outputStart,
                int[] outputCount,
                int[] outputs,
                int[] prefixSubtreeCount,
                int emptyPatternCount,
                byte[] flatPatterns,
                int[] patternOffsets,
                int[] patternLengths) {
            this.patterns = patterns;
            this.fail = fail;
            this.outputLink = outputLink;
            this.edgeStart = edgeStart;
            this.edgeCount = edgeCount;
            this.edgeLabels = edgeLabels;
            this.edgeTargets = edgeTargets;
            this.outputStart = outputStart;
            this.outputCount = outputCount;
            this.outputs = outputs;
            this.prefixSubtreeCount = prefixSubtreeCount;
            this.emptyPatternCount = emptyPatternCount;
            this.flatPatterns = flatPatterns;
            this.patternOffsets = patternOffsets;
            this.patternLengths = patternLengths;

            ByteBuffer patternStorage = ByteBuffer.allocateDirect(flatPatterns.length);
            patternStorage.put(flatPatterns).flip();
            directPatterns = patternStorage.asReadOnlyBuffer();

            ByteBuffer offsetStorage =
                    ByteBuffer.allocateDirect(Math.multiplyExact(patternOffsets.length, Integer.BYTES))
                            .order(ByteOrder.nativeOrder());
            IntBuffer offsetInts = offsetStorage.asIntBuffer();
            offsetInts.put(patternOffsets).flip();
            directOffsets = offsetInts.asReadOnlyBuffer();

            ByteBuffer lengthStorage =
                    ByteBuffer.allocateDirect(Math.multiplyExact(patternLengths.length, Integer.BYTES))
                            .order(ByteOrder.nativeOrder());
            IntBuffer lengthInts = lengthStorage.asIntBuffer();
            lengthInts.put(patternLengths).flip();
            directLengths = lengthInts.asReadOnlyBuffer();
        }

        public int patternCount() {
            return patterns.length;
        }

        public int totalPatternBytes() {
            return flatPatterns.length;
        }

        public byte[] pattern(int index) {
            return patterns[index].clone();
        }

        /** Number of prepared patterns, including duplicates, under the supplied byte prefix. */
        public int prefixCount(byte[] prefix) {
            return ChallengeMultiPatternSearch.prefixCount(this, prefix);
        }

        /** True when at least one prepared pattern begins with the supplied prefix. */
        public boolean startsWith(byte[] prefix) {
            return ChallengeMultiPatternSearch.startsWith(this, prefix);
        }

        /** True when at least one prepared pattern exactly equals the supplied bytes. */
        public boolean containsExact(byte[] pattern) {
            return ChallengeMultiPatternSearch.containsExact(this, pattern);
        }

        /** Exact prepared-pattern multiplicity, preserving duplicate input rows. */
        public int exactCount(byte[] pattern) {
            return ChallengeMultiPatternSearch.exactCount(this, pattern);
        }

        byte[] flatPatterns() {
            return flatPatterns.clone();
        }

        int[] patternOffsets() {
            return patternOffsets.clone();
        }

        int[] patternLengths() {
            return patternLengths.clone();
        }

        ByteBuffer directPatterns() {
            ByteBuffer view = directPatterns.duplicate();
            view.clear();
            return view;
        }

        IntBuffer directOffsets() {
            IntBuffer view = directOffsets.duplicate();
            view.clear();
            return view;
        }

        IntBuffer directLengths() {
            IntBuffer view = directLengths.duplicate();
            view.clear();
            return view;
        }
    }

    public static Prepared prepare(List<byte[]> patterns) {
        return prepare(patterns, null);
    }

    public static Prepared prepare(List<byte[]> patterns, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        List<byte[]> source = List.copyOf(Objects.requireNonNull(patterns, "patterns"));
        if (source.size() > MAX_PATTERNS) {
            throw new IllegalArgumentException("pattern count exceeds " + MAX_PATTERNS);
        }

        byte[][] owned = new byte[source.size()][];
        int totalBytes = 0;
        for (int index = 0; index < source.size(); index++) {
            if ((index & 255) == 0) ProgressMonitors.checkCanceled(checked);
            byte[] value = Objects.requireNonNull(source.get(index), "pattern").clone();
            totalBytes = Math.addExact(totalBytes, value.length);
            if (totalBytes > MAX_TOTAL_PATTERN_BYTES) {
                throw new IllegalArgumentException(
                        "pattern bytes exceed " + MAX_TOTAL_PATTERN_BYTES);
            }
            owned[index] = value;
        }

        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(new Node());
        int emptyPatternCount = 0;
        for (int pattern = 0; pattern < owned.length; pattern++) {
            if ((pattern & 255) == 0) ProgressMonitors.checkCanceled(checked);
            byte[] bytes = owned[pattern];
            if (bytes.length == 0) {
                emptyPatternCount++;
                continue;
            }
            int state = 0;
            for (byte value : bytes) {
                int label = value & 0xff;
                Integer next = nodes.get(state).next.get(label);
                if (next == null) {
                    next = nodes.size();
                    nodes.get(state).next.put(label, next);
                    nodes.add(new Node());
                }
                state = next;
            }
            nodes.get(state).outputs.add(pattern);
        }

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        nodes.get(0).next.values().stream().sorted().forEach(queue::addLast);
        while (!queue.isEmpty()) {
            ProgressMonitors.checkCanceled(checked);
            int parent = queue.removeFirst();
            Node parentNode = nodes.get(parent);
            for (Map.Entry<Integer, Integer> edge :
                    parentNode.next.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .toList()) {
                int label = edge.getKey();
                int child = edge.getValue();
                int fallback = parentNode.fail;
                Integer transition = nodes.get(fallback).next.get(label);
                while (fallback != 0 && transition == null) {
                    fallback = nodes.get(fallback).fail;
                    transition = nodes.get(fallback).next.get(label);
                }
                int fail = transition == null || transition == child ? 0 : transition;
                Node childNode = nodes.get(child);
                childNode.fail = fail;
                childNode.outputLink =
                        nodes.get(fail).outputs.isEmpty()
                                ? nodes.get(fail).outputLink
                                : fail;
                queue.addLast(child);
            }
        }

        int nodeCount = nodes.size();
        int edgeTotal = nodes.stream().mapToInt(node -> node.next.size()).sum();
        int outputTotal = nodes.stream().mapToInt(node -> node.outputs.size()).sum();
        int[] fail = new int[nodeCount];
        int[] outputLink = new int[nodeCount];
        int[] edgeStart = new int[nodeCount];
        int[] edgeCount = new int[nodeCount];
        byte[] edgeLabels = new byte[edgeTotal];
        int[] edgeTargets = new int[edgeTotal];
        int[] outputStart = new int[nodeCount];
        int[] outputCount = new int[nodeCount];
        int[] outputs = new int[outputTotal];
        int edgeCursor = 0;
        int outputCursor = 0;
        for (int state = 0; state < nodeCount; state++) {
            Node node = nodes.get(state);
            fail[state] = node.fail;
            outputLink[state] = node.outputLink;
            edgeStart[state] = edgeCursor;
            edgeCount[state] = node.next.size();
            for (Map.Entry<Integer, Integer> edge :
                    node.next.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .toList()) {
                edgeLabels[edgeCursor] = (byte) edge.getKey().intValue();
                edgeTargets[edgeCursor] = edge.getValue();
                edgeCursor++;
            }
            outputStart[state] = outputCursor;
            outputCount[state] = node.outputs.size();
            for (int pattern : node.outputs) outputs[outputCursor++] = pattern;
        }

        int[] prefixSubtreeCount = outputCount.clone();
        for (int state = nodeCount - 1; state >= 0; state--) {
            int start = edgeStart[state];
            int end = start + edgeCount[state];
            for (int edge = start; edge < end; edge++) {
                prefixSubtreeCount[state] =
                        Math.addExact(
                                prefixSubtreeCount[state],
                                prefixSubtreeCount[edgeTargets[edge]]);
            }
        }
        prefixSubtreeCount[0] =
                Math.addExact(prefixSubtreeCount[0], emptyPatternCount);

        ByteArrayOutputStream flat = new ByteArrayOutputStream(totalBytes);
        int[] offsets = new int[owned.length];
        int[] lengths = new int[owned.length];
        for (int index = 0; index < owned.length; index++) {
            offsets[index] = flat.size();
            lengths[index] = owned[index].length;
            flat.writeBytes(owned[index]);
        }
        ProgressMonitors.checkCanceled(checked);
        return new Prepared(
                owned,
                fail,
                outputLink,
                edgeStart,
                edgeCount,
                edgeLabels,
                edgeTargets,
                outputStart,
                outputCount,
                outputs,
                prefixSubtreeCount,
                emptyPatternCount,
                flat.toByteArray(),
                offsets,
                lengths);
    }

    /** Returns prepared pattern multiplicity below one byte prefix, including duplicates. */
    public static int prefixCount(Prepared prepared, byte[] prefix) {
        return prefixCount(prepared, prefix, null);
    }

    public static int prefixCount(
            Prepared prepared, byte[] prefix, IProgressMonitor monitor) {
        Prepared stable = Objects.requireNonNull(prepared, "prepared");
        int state = trieState(stable, Objects.requireNonNull(prefix, "prefix"), monitor);
        return state < 0 ? 0 : stable.prefixSubtreeCount[state];
    }

    public static boolean startsWith(Prepared prepared, byte[] prefix) {
        return prefixCount(prepared, prefix) != 0;
    }

    public static boolean containsExact(Prepared prepared, byte[] pattern) {
        return exactCount(prepared, pattern) != 0;
    }

    public static int exactCount(Prepared prepared, byte[] pattern) {
        return exactCount(prepared, pattern, null);
    }

    public static int exactCount(
            Prepared prepared, byte[] pattern, IProgressMonitor monitor) {
        Prepared stable = Objects.requireNonNull(prepared, "prepared");
        byte[] query = Objects.requireNonNull(pattern, "pattern");
        int state = trieState(stable, query, monitor);
        if (state < 0) return 0;
        return query.length == 0 ? stable.emptyPatternCount : stable.outputCount[state];
    }

    /**
     * Returns the first byte offset for every prepared pattern, or {@code -1} if absent.
     *
     * <p>An empty pattern matches at byte offset zero, matching literal-search conventions.</p>
     */
    public static int[] firstOffsets(Prepared prepared, byte[] haystack) {
        return firstOffsets(prepared, haystack, null);
    }

    public static int[] firstOffsets(
            Prepared prepared, ChallengeNativeSearch.PreparedBytes haystack) {
        return firstOffsets(prepared, haystack, null);
    }

    public static int[] firstOffsets(
            Prepared prepared,
            ChallengeNativeSearch.PreparedBytes haystack,
            IProgressMonitor monitor) {
        Prepared stable = Objects.requireNonNull(prepared, "prepared");
        ChallengeNativeSearch.PreparedBytes text =
                Objects.requireNonNull(haystack, "haystack");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (monitor == null
                && ChallengeNativeSearch.nativeEnabled()
                && text.size() >= NATIVE_MIN_TEXT_BYTES
                && stable.patternCount() >= NATIVE_MIN_PATTERNS) {
            int[] result =
                    nativeFirstOffsetsDirect(
                            text.directBuffer(),
                            stable.directPatterns(),
                            stable.directOffsets(),
                            stable.directLengths(),
                            stable.patternCount());
            validateNativeResult(stable, text, result);
            return result;
        }
        return firstOffsetsJava(stable, text, checked);
    }

    public static int[] firstOffsets(
            Prepared prepared, byte[] haystack, IProgressMonitor monitor) {
        Prepared stable = Objects.requireNonNull(prepared, "prepared");
        byte[] text = Objects.requireNonNull(haystack, "haystack");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (monitor == null
                && ChallengeNativeSearch.nativeEnabled()
                && text.length >= NATIVE_MIN_TEXT_BYTES
                && stable.patternCount() >= NATIVE_MIN_PATTERNS) {
            int[] result =
                    nativeFirstOffsets(
                            text,
                            stable.flatPatterns,
                            stable.patternOffsets,
                            stable.patternLengths);
            validateNativeResult(stable, text, result);
            return result;
        }
        return firstOffsetsJava(stable, text, checked);
    }

    public static int[] firstOffsetsJava(
            Prepared prepared, ChallengeNativeSearch.PreparedBytes haystack) {
        return firstOffsetsJava(
                Objects.requireNonNull(prepared, "prepared"),
                Objects.requireNonNull(haystack, "haystack"),
                IProgressMonitor.noop());
    }

    public static int[] firstOffsetsJava(Prepared prepared, byte[] haystack) {
        return firstOffsetsJava(
                Objects.requireNonNull(prepared, "prepared"),
                Objects.requireNonNull(haystack, "haystack"),
                IProgressMonitor.noop());
    }

    private static int[] firstOffsetsJava(
            Prepared prepared,
            ChallengeNativeSearch.PreparedBytes text,
            IProgressMonitor monitor) {
        int[] result = initialResult(prepared);
        int unresolved = unresolved(prepared);
        int state = 0;
        for (int position = 0; position < text.size() && unresolved > 0; position++) {
            if ((position & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            int label = text.byteAt(position) & 0xff;
            int next = transition(prepared, state, label);
            while (state != 0 && next < 0) {
                state = prepared.fail[state];
                next = transition(prepared, state, label);
            }
            state = next < 0 ? 0 : next;
            unresolved -= emit(prepared, state, position, result);
            int linked = prepared.outputLink[state];
            while (linked != 0) {
                unresolved -= emit(prepared, linked, position, result);
                linked = prepared.outputLink[linked];
            }
        }
        ProgressMonitors.checkCanceled(monitor);
        return result;
    }

    private static int[] firstOffsetsJava(
            Prepared prepared, byte[] text, IProgressMonitor monitor) {
        int[] result = new int[prepared.patternCount()];
        Arrays.fill(result, -1);
        int unresolved = 0;
        for (int pattern = 0; pattern < result.length; pattern++) {
            if (prepared.patterns[pattern].length == 0) result[pattern] = 0;
            else unresolved++;
        }
        int state = 0;
        for (int position = 0; position < text.length && unresolved > 0; position++) {
            if ((position & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            int label = text[position] & 0xff;
            int next = transition(prepared, state, label);
            while (state != 0 && next < 0) {
                state = prepared.fail[state];
                next = transition(prepared, state, label);
            }
            state = next < 0 ? 0 : next;
            unresolved -= emit(prepared, state, position, result);
            int linked = prepared.outputLink[state];
            while (linked != 0) {
                unresolved -= emit(prepared, linked, position, result);
                linked = prepared.outputLink[linked];
            }
        }
        ProgressMonitors.checkCanceled(monitor);
        return result;
    }

    private static int[] initialResult(Prepared prepared) {
        int[] result = new int[prepared.patternCount()];
        Arrays.fill(result, -1);
        for (int pattern = 0; pattern < result.length; pattern++) {
            if (prepared.patterns[pattern].length == 0) result[pattern] = 0;
        }
        return result;
    }

    private static int unresolved(Prepared prepared) {
        int count = 0;
        for (byte[] pattern : prepared.patterns) {
            if (pattern.length != 0) count++;
        }
        return count;
    }

    private static int trieState(
            Prepared prepared, byte[] query, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        int state = 0;
        for (int index = 0; index < query.length; index++) {
            if ((index & 255) == 0) ProgressMonitors.checkCanceled(checked);
            state = transition(prepared, state, query[index] & 0xff);
            if (state < 0) return -1;
        }
        ProgressMonitors.checkCanceled(checked);
        return state;
    }

    private static int transition(Prepared prepared, int state, int label) {
        int low = prepared.edgeStart[state];
        int high = low + prepared.edgeCount[state];
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            int candidate = prepared.edgeLabels[middle] & 0xff;
            if (candidate < label) low = middle + 1;
            else high = middle;
        }
        int end = prepared.edgeStart[state] + prepared.edgeCount[state];
        return low < end && (prepared.edgeLabels[low] & 0xff) == label
                ? prepared.edgeTargets[low]
                : -1;
    }

    private static int emit(
            Prepared prepared, int state, int position, int[] result) {
        int newlyResolved = 0;
        int start = prepared.outputStart[state];
        int end = start + prepared.outputCount[state];
        for (int cursor = start; cursor < end; cursor++) {
            int pattern = prepared.outputs[cursor];
            if (result[pattern] >= 0) continue;
            int offset = position + 1 - prepared.patterns[pattern].length;
            if (offset < 0) throw new IllegalStateException("automaton emitted before pattern start");
            result[pattern] = offset;
            newlyResolved++;
        }
        return newlyResolved;
    }

    static void verifyNative() {
        Prepared prepared =
                prepare(
                        List.of(
                                new byte[0],
                                "he".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                "she".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                "hers".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                "his".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                "he".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        byte[] text = "ahishers".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int[] expected = firstOffsetsJava(prepared, text);
        int[] actual =
                nativeFirstOffsets(
                        text,
                        prepared.flatPatterns,
                        prepared.patternOffsets,
                        prepared.patternLengths);
        if (!Arrays.equals(expected, actual)) {
            throw new LinkageError("native Aho-Corasick oracle probe failed");
        }
        ChallengeNativeSearch.PreparedBytes directText =
                ChallengeNativeSearch.prepareBytes(text);
        int[] direct =
                nativeFirstOffsetsDirect(
                        directText.directBuffer(),
                        prepared.directPatterns(),
                        prepared.directOffsets(),
                        prepared.directLengths(),
                        prepared.patternCount());
        if (!Arrays.equals(expected, direct)) {
            throw new LinkageError("native direct Aho-Corasick oracle probe failed");
        }
    }

    static void validateNativeResult(
            Prepared prepared,
            ChallengeNativeSearch.PreparedBytes text,
            int[] result) {
        if (result == null || result.length != prepared.patternCount()) {
            throw new IllegalStateException("native multi-pattern result shape");
        }
        for (int pattern = 0; pattern < result.length; pattern++) {
            int offset = result[pattern];
            byte[] needle = prepared.patterns[pattern];
            if (needle.length == 0) {
                if (offset != 0) throw new IllegalStateException("empty pattern contract");
                continue;
            }
            if (offset < -1 || offset > text.size() - needle.length) {
                throw new IllegalStateException("native multi-pattern offset");
            }
            if (offset >= 0) {
                for (int index = 0; index < needle.length; index++) {
                    if (text.byteAt(offset + index) != needle[index]) {
                        throw new IllegalStateException("native multi-pattern false positive");
                    }
                }
            }
        }
    }

    static void validateNativeResult(Prepared prepared, byte[] text, int[] result) {
        if (result == null || result.length != prepared.patternCount()) {
            throw new IllegalStateException("native multi-pattern result shape");
        }
        for (int pattern = 0; pattern < result.length; pattern++) {
            int offset = result[pattern];
            byte[] needle = prepared.patterns[pattern];
            if (needle.length == 0) {
                if (offset != 0) throw new IllegalStateException("empty pattern contract");
                continue;
            }
            if (offset < -1
                    || offset > text.length - needle.length) {
                throw new IllegalStateException("native multi-pattern offset");
            }
            if (offset >= 0) {
                for (int index = 0; index < needle.length; index++) {
                    if (text[offset + index] != needle[index]) {
                        throw new IllegalStateException("native multi-pattern false positive");
                    }
                }
            }
        }
    }

    private static final class Node {
        final HashMap<Integer, Integer> next = new HashMap<>();
        final ArrayList<Integer> outputs = new ArrayList<>();
        int fail;
        int outputLink;
    }

    private static native int[] nativeFirstOffsets(
            byte[] haystack,
            byte[] flatPatterns,
            int[] patternOffsets,
            int[] patternLengths);

    private static native int[] nativeFirstOffsetsDirect(
            ByteBuffer haystack,
            ByteBuffer flatPatterns,
            IntBuffer patternOffsets,
            IntBuffer patternLengths,
            int patternCount);
}
