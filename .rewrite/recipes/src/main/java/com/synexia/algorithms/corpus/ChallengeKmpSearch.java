// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Prepared Knuth-Morris-Pratt binary literal search with optional JNI acceleration.
 *
 * <p>The immutable prefix/failure table is computed once and reused across scans. Java 21 remains
 * the semantic oracle. Native code consumes only the prepared byte/prefix images; challenge-site
 * and systems-donor source is not copied into this implementation.</p>
 */
public final class ChallengeKmpSearch {
    private static final int NATIVE_MIN_BYTES = 512;
    private static volatile boolean nativeVerified;
    private static volatile boolean nativeStreamVerified;

    private ChallengeKmpSearch() {}

    /** Immutable pattern plus canonical KMP prefix table in direct native-order storage. */
    public static final class Prepared {
        private final ByteBuffer pattern;
        private final IntBuffer failure;

        private Prepared(ByteBuffer pattern, IntBuffer failure) {
            this.pattern = pattern.asReadOnlyBuffer();
            this.failure = failure.asReadOnlyBuffer();
        }

        public int size() {
            return pattern.capacity();
        }

        public byte byteAt(int index) {
            return pattern.get(index);
        }

        public int failureAt(int index) {
            return failure.get(index);
        }

        public byte[] toArray() {
            byte[] copy = new byte[pattern.capacity()];
            pattern.duplicate().get(copy);
            return copy;
        }

        public int[] failureTable() {
            int[] copy = new int[failure.capacity()];
            failure.duplicate().get(copy);
            return copy;
        }

        ByteBuffer directPattern() {
            ByteBuffer view = pattern.duplicate();
            view.clear();
            return view;
        }

        IntBuffer directFailure() {
            IntBuffer view = failure.duplicate();
            view.clear();
            return view;
        }
    }

    public static Prepared prepare(byte[] pattern) {
        return prepare(pattern, null);
    }

    public static Prepared prepare(byte[] pattern, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        byte[] owned = Objects.requireNonNull(pattern, "pattern").clone();
        int[] prefix = prefix(owned, checked);
        ByteBuffer patternStorage = ByteBuffer.allocateDirect(owned.length);
        patternStorage.put(owned).flip();
        ByteBuffer failureStorage =
                ByteBuffer.allocateDirect(Math.multiplyExact(prefix.length, Integer.BYTES))
                        .order(ByteOrder.nativeOrder());
        IntBuffer failure = failureStorage.asIntBuffer();
        failure.put(prefix).flip();
        ProgressMonitors.checkCanceled(checked);
        return new Prepared(patternStorage, failure);
    }

    public static synchronized void enableNative(Path library) {
        ChallengeNativeSearch.enableNative(Objects.requireNonNull(library, "library"));
        if (!nativeVerified) {
            verifyNative();
            nativeVerified = true;
        }
    }

    public static boolean nativeEnabled() {
        return nativeVerified;
    }

    public static int indexOf(byte[] haystack, byte[] pattern) {
        return indexOf(haystack, pattern, null);
    }

    public static int indexOf(
            byte[] haystack, byte[] pattern, IProgressMonitor monitor) {
        return indexOf(
                ChallengeNativeSearch.prepareBytes(Objects.requireNonNull(haystack, "haystack")),
                prepare(Objects.requireNonNull(pattern, "pattern"), monitor),
                monitor);
    }

    public static int indexOf(
            ChallengeNativeSearch.PreparedBytes haystack, Prepared pattern) {
        return indexOf(haystack, pattern, null);
    }

    public static int indexOf(
            ChallengeNativeSearch.PreparedBytes haystack,
            Prepared pattern,
            IProgressMonitor monitor) {
        ChallengeNativeSearch.PreparedBytes text =
                Objects.requireNonNull(haystack, "haystack");
        Prepared needle = Objects.requireNonNull(pattern, "pattern");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (needle.size() == 0) return 0;
        if (needle.size() > text.size()) return -1;

        if (monitor == null
                && nativeVerified
                && text.size() >= NATIVE_MIN_BYTES
                && needle.size() >= 2) {
            int result =
                    nativeIndexOfDirect(
                            text.directBuffer(),
                            needle.directPattern(),
                            needle.directFailure());
            ProgressMonitors.checkCanceled(checked);
            validatePositive(text, needle, result);
            return result;
        }
        return indexOfJava(text, needle, checked);
    }

    public static int indexOfJava(byte[] haystack, byte[] pattern) {
        ChallengeNativeSearch.PreparedBytes text =
                ChallengeNativeSearch.prepareBytes(
                        Objects.requireNonNull(haystack, "haystack"));
        Prepared needle = prepare(Objects.requireNonNull(pattern, "pattern"));
        return indexOfJava(text, needle, IProgressMonitor.noop());
    }

    static int indexOfJava(
            ChallengeNativeSearch.PreparedBytes text,
            Prepared needle,
            IProgressMonitor monitor) {
        int patternLength = needle.size();
        if (patternLength == 0) return 0;
        if (patternLength > text.size()) return -1;
        int matched = 0;
        for (int index = 0; index < text.size(); index++) {
            if ((index & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            matched = advanceMatched(needle, matched, text.byteAt(index));
            if (matched == patternLength) {
                return index + 1 - patternLength;
            }
        }
        ProgressMonitors.checkCanceled(monitor);
        return -1;
    }

    private static int[] prefix(byte[] pattern, IProgressMonitor monitor) {
        int[] failure = new int[pattern.length];
        int matched = 0;
        for (int index = 1; index < pattern.length; index++) {
            if ((index & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            while (matched > 0 && pattern[index] != pattern[matched]) {
                matched = failure[matched - 1];
            }
            if (pattern[index] == pattern[matched]) matched++;
            failure[index] = matched;
        }
        return failure;
    }

    private static void validatePositive(
            ChallengeNativeSearch.PreparedBytes text,
            Prepared needle,
            int result) {
        if (result < -1) {
            throw new IllegalStateException("native KMP index outside contract");
        }
        if (result >= 0) {
            if (result > text.size() - needle.size()) {
                throw new IllegalStateException("native KMP index outside contract");
            }
            for (int index = 0; index < needle.size(); index++) {
                if (text.byteAt(result + index) != needle.byteAt(index)) {
                    throw new IllegalStateException("native KMP false positive");
                }
            }
        }
    }

    private static void verifyNative() {
        byte[][] texts = {
            {},
            {0},
            "aaaaabaaaaab".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "abcxabcdabxabcdabcdabcy".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            new byte[2048]
        };
        byte[][] patterns = {
            {},
            {0},
            "aaaaab".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "abcdabcy".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "nomatch".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            {(byte) 0xff, 0}
        };
        for (byte[] text : texts) {
            for (byte[] pattern : patterns) {
                Prepared prepared = prepare(pattern);
                ChallengeNativeSearch.PreparedBytes preparedText =
                        ChallengeNativeSearch.prepareBytes(text);
                int expected = indexOfJava(preparedText, prepared, IProgressMonitor.noop());
                int actual =
                        nativeIndexOfDirect(
                                preparedText.directBuffer(),
                                prepared.directPattern(),
                                prepared.directFailure());
                if (actual != expected
                        || !Arrays.equals(pattern, prepared.toArray())
                        || !Arrays.equals(prefix(pattern, IProgressMonitor.noop()),
                                prepared.failureTable())) {
                    throw new LinkageError("native KMP oracle probe failed");
                }
            }
        }
    }


    /** An explicit stream policy; existing array-search dispatch is unchanged. */
    public enum StreamBackend { JAVA, JNI_REQUIRED }

    /** BYTE_LIMIT is not an assertion of absence beyond the inspected prefix. */
    public enum StreamStop { MATCH, END_OF_INPUT, BYTE_LIMIT }

    /** Offsets are relative to the input's position on entry. bytesRead includes chunk read-ahead. */
    public record StreamResult(StreamStop stop, long firstOffset, long bytesRead) {
        public StreamResult {
            Objects.requireNonNull(stop, "stop");
            if (bytesRead < 0 || (stop == StreamStop.MATCH
                    ? firstOffset < 0 || firstOffset > bytesRead : firstOffset != -1)) {
                throw new IllegalArgumentException("invalid KMP stream result");
            }
        }
    }

    /** Scan with the Java reference path, without requiring or loading any native library. */
    public static StreamResult scanStream(InputStream input, Prepared pattern, long maxBytes,
            int chunkBytes, IProgressMonitor monitor) throws IOException {
        return scanStream(input, pattern, maxBytes, chunkBytes, StreamBackend.JAVA, monitor);
    }

    /**
     * Search caller-owned input with one reusable buffer, the existing immutable pattern, and
     * one private prefix state. Does not close, rewind, skip, decode or retain the input. Pattern
     * matches can span arbitrarily many chunks; offsets/byte limits are long, not array indices.
     *
     * <p>Reads at most maxBytes and stops at the first match, observed EOF, or the byte limit.
     * A chunk may read beyond a match, so bytesRead may exceed firstOffset + pattern.size().
     * Reaching the exact limit does not probe one extra byte to infer EOF. Empty patterns match
     * at zero without reading. Caller must serialize access to each input; separate scans may
     * share a Prepared concurrently. Scratch is O(chunkBytes), excluding the prepared pattern.
     *
     * <p>JNI_REQUIRED needs enableNative and the new chunk symbol, checked before input reads.
     * No silent native fallback. Java checks cooperatively while matching; JNI checks between
     * chunks. A blocking InputStream read itself is not forcibly interruptible. No result is
     * published after observed cancellation, including cancellation by the done callback.
     */
    public static StreamResult scanStream(InputStream input, Prepared pattern, long maxBytes,
            int chunkBytes, StreamBackend backend, IProgressMonitor monitor) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(backend, "backend");
        if (maxBytes < 0 || chunkBytes < 1 || chunkBytes > 1_048_576) {
            throw new IllegalArgumentException("stream byte limit/chunk size");
        }
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (backend == StreamBackend.JNI_REQUIRED) requireStreamNative(checked);
        checked.beginTask("scan prepared KMP byte stream", maxBytes);
        StreamResult result;
        try {
            result = scanStreamOwned(input, pattern, maxBytes, chunkBytes, backend, checked);
            ProgressMonitors.checkCanceled(checked);
        } finally {
            checked.done();
        }
        ProgressMonitors.checkCanceled(checked);
        return result;
    }

    private static void requireStreamNative(IProgressMonitor monitor) {
        if (!nativeVerified) throw new IllegalStateException("KMP native library must be explicitly enabled");
        if (!nativeStreamVerified) verifyStreamNative(monitor);
    }

    // Separate capability admission leaves legacy enableNative compatible with older libraries.
    private static synchronized void verifyStreamNative(IProgressMonitor monitor) {
        if (nativeStreamVerified) return;
        byte[] text = {'z', 'a', 'a', 'a', 'b', (byte) 0xff, 0, 'a', 'b'};
        for (byte[] value : new byte[][] {{'a', 'b'}, {'a', 'a', 'b'}, {(byte) 0xff, 0}, {'x'}}) {
            Prepared pattern = prepare(value, monitor);
            ByteBuffer needle = pattern.directPattern();
            IntBuffer failure = pattern.directFailure();
            int expected = indexOfJava(ChallengeNativeSearch.prepareBytes(text), pattern, monitor);
            for (int width : new int[] {1, 2, 5}) {
                ByteBuffer chunk = ByteBuffer.allocateDirect(width);
                if (nativeScanChunk(chunk, 0, needle, failure, 0) != -1L) {
                    throw new LinkageError("KMP stream native identity probe failed");
                }
                int state = 0, actual = -1;
                for (int at = 0; at < text.length; at += width) {
                    ProgressMonitors.checkCanceled(monitor);
                    int count = Math.min(width, text.length - at);
                    chunk.clear();
                    chunk.put(text, at, count);
                    long step = nativeScanChunk(chunk, count, needle, failure, state);
                    if (step > 0 && step <= count) { actual = at + (int) step - value.length; break; }
                    if (step >= 0 || step < -(long) value.length) throw new LinkageError("invalid KMP stream native probe result");
                    state = (int) (-step - 1);
                }
                if (actual != expected) throw new LinkageError("KMP stream native oracle probe failed");
            }
        }
        ProgressMonitors.checkCanceled(monitor);
        nativeStreamVerified = true;
    }

    private static StreamResult scanStreamOwned(InputStream input, Prepared pattern, long maxBytes,
            int chunkBytes, StreamBackend backend, IProgressMonitor monitor) throws IOException {
        ProgressMonitors.checkCanceled(monitor);
        if (pattern.size() == 0) return new StreamResult(StreamStop.MATCH, 0, 0);
        if (maxBytes == 0) return new StreamResult(StreamStop.BYTE_LIMIT, -1, 0);
        int capacity = (int) Math.min(maxBytes, chunkBytes);
        byte[] bytes = new byte[capacity];
        ByteBuffer direct = backend == StreamBackend.JNI_REQUIRED ? ByteBuffer.allocateDirect(capacity) : null;
        ByteBuffer needle = backend == StreamBackend.JNI_REQUIRED ? pattern.directPattern() : null;
        IntBuffer failure = backend == StreamBackend.JNI_REQUIRED ? pattern.directFailure() : null;
        if (backend == StreamBackend.JNI_REQUIRED
                && nativeScanChunk(direct, 0, needle, failure, 0) != -1L) {
            throw new LinkageError("native KMP chunk identity probe failed");
        }
        long read = 0;
        int matched = 0;
        while (read < maxBytes) {
            ProgressMonitors.checkCanceled(monitor);
            int request = (int) Math.min(bytes.length, maxBytes - read);
            int count = input.read(bytes, 0, request);
            ProgressMonitors.checkCanceled(monitor);
            if (count == -1) return new StreamResult(StreamStop.END_OF_INPUT, -1, read);
            if (count < 1 || count > request) throw new IOException("invalid InputStream read count");
            long before = read;
            read += count; // count <= maxBytes - read, so no signed overflow.
            long step;
            if (backend == StreamBackend.JNI_REQUIRED) {
                direct.clear();
                direct.put(bytes, 0, count);
                step = nativeScanChunk(direct, count, needle, failure, matched);
            } else {
                step = scanChunkJava(bytes, count, pattern, matched, monitor);
            }
            monitor.worked(count);
            ProgressMonitors.checkCanceled(monitor);
            if (step > 0 && step <= count) {
                long offset = before + step - pattern.size();
                if (offset < 0) throw new IllegalStateException("native KMP premature match");
                return new StreamResult(StreamStop.MATCH, offset, read);
            }
            if (step >= 0 || step < -(long) pattern.size()) {
                throw new IllegalStateException("KMP chunk result outside contract");
            }
            matched = (int) (-step - 1);
        }
        return new StreamResult(StreamStop.BYTE_LIMIT, -1, read);
    }

    // Positive: bytes consumed through first match. Negative: -(outgoing prefix length + 1).
    private static long scanChunkJava(byte[] input, int count, Prepared needle, int matched,
            IProgressMonitor monitor) {
        for (int index = 0; index < count; index++) {
            if ((index & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            matched = advanceMatched(needle, matched, input[index]);
            if (matched == needle.size()) return index + 1L;
        }
        return -(matched + 1L);
    }

    // Shared by existing whole-buffer Java matching and the additive stream operation.
    private static int advanceMatched(Prepared needle, int matched, byte value) {
        while (matched > 0 && value != needle.byteAt(matched)) matched = needle.failureAt(matched - 1);
        return value == needle.byteAt(matched) ? matched + 1 : matched;
    }

    private static native long nativeScanChunk(ByteBuffer input, int count,
            ByteBuffer pattern, IntBuffer failure, int matched);

    private static native int nativeIndexOfDirect(
            ByteBuffer haystack, ByteBuffer pattern, IntBuffer failure);
}
