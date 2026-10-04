// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Reusable native-search primitives with Java 21 reference semantics.
 *
 * <p>The byte operation is deliberately binary/literal search, not Unicode or regex semantics.
 * Regex engines and MIndex planners may use it only as a necessary candidate/prefilter primitive.
 * Java remains the oracle and fallback. JNI is optional and explicitly loaded by the host.</p>
 */
public final class ChallengeNativeSearch {
    private static final int NATIVE_MIN_VALUES = 256;
    private static final int NATIVE_MIN_BYTES = 512;
    private static volatile boolean nativeEnabled;

    private ChallengeNativeSearch() {}


    /** Immutable validated sorted int32 snapshot for repeated O(log n) rank lookups. */
    public static final class PreparedInt32 {
        private final int[] values;

        private PreparedInt32(int[] values) {
            this.values = values;
        }

        public int size() {
            return values.length;
        }

        public int valueAt(int index) {
            return values[index];
        }

        public int[] toArray() {
            return values.clone();
        }
    }

    /** Immutable direct int32 image: one preparation copy, repeated JNI/JNA pointer access. */
    public static final class PreparedDirectInt32 {
        private final IntBuffer values;

        private PreparedDirectInt32(IntBuffer values) {
            this.values = values.asReadOnlyBuffer();
        }

        public int size() {
            return values.capacity();
        }

        public int valueAt(int index) {
            return values.get(index);
        }

        public int[] toArray() {
            int[] copy = new int[values.capacity()];
            values.duplicate().get(copy);
            return copy;
        }

        IntBuffer directBuffer() {
            IntBuffer view = values.duplicate();
            view.clear();
            return view;
        }
    }

    /** Immutable direct byte image for repeated large literal scans. */
    public static final class PreparedBytes {
        private final ByteBuffer bytes;

        private PreparedBytes(ByteBuffer bytes) {
            this.bytes = bytes.asReadOnlyBuffer();
        }

        public int size() {
            return bytes.capacity();
        }

        public byte byteAt(int index) {
            return bytes.get(index);
        }

        public byte[] toArray() {
            byte[] copy = new byte[bytes.capacity()];
            bytes.duplicate().get(copy);
            return copy;
        }

        ByteBuffer directBuffer() {
            ByteBuffer view = bytes.duplicate();
            view.clear();
            return view;
        }
    }

    /** Direct literal pattern with Boyer-Moore-Horspool shifts precomputed once. */
    public static final class PreparedBytePattern {
        private final ByteBuffer bytes;
        private final int[] shifts;

        private PreparedBytePattern(ByteBuffer bytes, int[] shifts) {
            this.bytes = bytes.asReadOnlyBuffer();
            this.shifts = shifts;
        }

        public int size() {
            return bytes.capacity();
        }

        public byte byteAt(int index) {
            return bytes.get(index);
        }

        public byte[] toArray() {
            byte[] copy = new byte[bytes.capacity()];
            bytes.duplicate().get(copy);
            return copy;
        }

        ByteBuffer directBuffer() {
            ByteBuffer view = bytes.duplicate();
            view.clear();
            return view;
        }

        int shift(int unsignedByte) {
            return shifts[unsignedByte];
        }
    }

    public static PreparedInt32 prepareSorted(int[] values, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        int[] owned = Objects.requireNonNull(values, "values").clone();
        requireSorted(owned, checked);
        ProgressMonitors.checkCanceled(checked);
        return new PreparedInt32(owned);
    }

    public static PreparedInt32 prepareSorted(int[] values) {
        return prepareSorted(values, null);
    }

    public static PreparedDirectInt32 prepareSortedDirect(
            int[] values, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        int[] owned = Objects.requireNonNull(values, "values").clone();
        requireSorted(owned, checked);
        ByteBuffer storage =
                ByteBuffer.allocateDirect(Math.multiplyExact(owned.length, Integer.BYTES))
                        .order(ByteOrder.nativeOrder());
        IntBuffer ints = storage.asIntBuffer();
        ints.put(owned).flip();
        ProgressMonitors.checkCanceled(checked);
        return new PreparedDirectInt32(ints);
    }

    public static PreparedDirectInt32 prepareSortedDirect(int[] values) {
        return prepareSortedDirect(values, null);
    }

    public static PreparedBytes prepareBytes(byte[] values) {
        byte[] owned = Objects.requireNonNull(values, "values").clone();
        ByteBuffer storage = ByteBuffer.allocateDirect(owned.length);
        storage.put(owned).flip();
        return new PreparedBytes(storage);
    }

    public static PreparedBytePattern preparePattern(byte[] values) {
        byte[] owned = Objects.requireNonNull(values, "values").clone();
        ByteBuffer storage = ByteBuffer.allocateDirect(owned.length);
        storage.put(owned).flip();
        int[] shifts = new int[256];
        Arrays.fill(shifts, Math.max(1, owned.length));
        for (int index = 0; index + 1 < owned.length; index++) {
            shifts[owned[index] & 0xff] = owned.length - 1 - index;
        }
        return new PreparedBytePattern(storage, shifts);
    }

    public static synchronized void enableNative(Path library) {
        Path selected = Objects.requireNonNull(library, "library").toAbsolutePath().normalize();
        if (!Files.isRegularFile(selected)) {
            throw new IllegalArgumentException("native library does not exist: " + selected);
        }
        if (!nativeEnabled) {
            System.load(selected.toString());
            if (nativeAbi() != 1) throw new LinkageError("challenge search ABI mismatch");
            verifyNative();
            nativeEnabled = true;
        }
    }

    public static boolean nativeEnabled() {
        return nativeEnabled;
    }

    public static int lowerBound(int[] values, int target) {
        return lowerBound(values, target, null);
    }

    public static int lowerBound(int[] values, int target, IProgressMonitor monitor) {
        return lowerBound(prepareSorted(values, monitor), target, monitor);
    }

    public static int lowerBound(PreparedInt32 prepared, int target) {
        return lowerBound(prepared, target, null);
    }

    public static int lowerBound(
            PreparedInt32 prepared, int target, IProgressMonitor monitor) {
        PreparedInt32 stable = Objects.requireNonNull(prepared, "prepared");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (monitor == null && nativeEnabled && stable.values.length >= NATIVE_MIN_VALUES) {
            int result = nativeLowerBound(stable.values, target);
            ProgressMonitors.checkCanceled(checked);
            requireLowerBoundResult(stable.values, target, result);
            return result;
        }
        return lowerBoundJavaPrepared(stable.values, target, checked);
    }

    public static int lowerBound(PreparedDirectInt32 prepared, int target) {
        return lowerBound(prepared, target, null);
    }

    public static int lowerBound(
            PreparedDirectInt32 prepared, int target, IProgressMonitor monitor) {
        PreparedDirectInt32 stable = Objects.requireNonNull(prepared, "prepared");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        int result;
        if (monitor == null && nativeEnabled && stable.size() >= NATIVE_MIN_VALUES) {
            result = nativeLowerBoundDirect(stable.directBuffer(), target);
            ProgressMonitors.checkCanceled(checked);
            requireLowerBoundResult(stable.values, target, result);
        } else {
            result = lowerBoundJavaPrepared(stable.values, target, checked);
        }
        return result;
    }

    /**
     * Exponential lower-bound over a prepared ascending snapshot.
     *
     * <p>This is exactly galloping lower-bound from index zero; no separate representation or
     * kernel is retained.</p>
     */
    public static int exponentialLowerBound(PreparedInt32 prepared, int target) {
        return exponentialLowerBound(prepared, target, null);
    }

    public static int exponentialLowerBound(
            PreparedInt32 prepared, int target, IProgressMonitor monitor) {
        return gallopingLowerBound(prepared, 0, target, monitor);
    }

    public static int exponentialLowerBound(PreparedDirectInt32 prepared, int target) {
        return exponentialLowerBound(prepared, target, null);
    }

    public static int exponentialLowerBound(
            PreparedDirectInt32 prepared, int target, IProgressMonitor monitor) {
        return gallopingLowerBound(prepared, 0, target, monitor);
    }

    public static int exponentialLowerBound(int[] values, int target) {
        return exponentialLowerBound(values, target, null);
    }

    public static int exponentialLowerBound(
            int[] values, int target, IProgressMonitor monitor) {
        return gallopingLowerBound(prepareSorted(values, monitor), 0, target, monitor);
    }

    /**
     * Galloping lower-bound in the suffix beginning at {@code fromIndex}.
     *
     * <p>The result is the first index at or after {@code fromIndex} whose value is greater than
     * or equal to {@code target}, or {@code size()} when no such value exists.</p>
     */
    public static int gallopingLowerBound(
            int[] values, int fromIndex, int target) {
        return gallopingLowerBound(values, fromIndex, target, null);
    }

    public static int gallopingLowerBound(
            int[] values, int fromIndex, int target, IProgressMonitor monitor) {
        return gallopingLowerBound(
                prepareSorted(values, monitor), fromIndex, target, monitor);
    }

    public static int gallopingLowerBound(
            PreparedInt32 prepared, int fromIndex, int target) {
        return gallopingLowerBound(prepared, fromIndex, target, null);
    }

    public static int gallopingLowerBound(
            PreparedInt32 prepared,
            int fromIndex,
            int target,
            IProgressMonitor monitor) {
        PreparedInt32 stable = Objects.requireNonNull(prepared, "prepared");
        int from = checkFromIndex(fromIndex, stable.values.length);
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (monitor == null && nativeEnabled && stable.values.length >= NATIVE_MIN_VALUES) {
            try {
                int result = nativeGallopingLowerBound(stable.values, from, target);
                ProgressMonitors.checkCanceled(checked);
                requireGallopingLowerBoundResult(stable.values, from, target, result);
                return result;
            } catch (UnsatisfiedLinkError olderLibrary) {
                // ABI v1 libraries predating this additive symbol retain the Java oracle path.
            }
        }
        return gallopingLowerBoundJavaPrepared(stable.values, from, target, checked);
    }

    public static int gallopingLowerBound(
            PreparedDirectInt32 prepared, int fromIndex, int target) {
        return gallopingLowerBound(prepared, fromIndex, target, null);
    }

    public static int gallopingLowerBound(
            PreparedDirectInt32 prepared,
            int fromIndex,
            int target,
            IProgressMonitor monitor) {
        PreparedDirectInt32 stable = Objects.requireNonNull(prepared, "prepared");
        int from = checkFromIndex(fromIndex, stable.size());
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (monitor == null && nativeEnabled && stable.size() >= NATIVE_MIN_VALUES) {
            try {
                int result =
                        nativeGallopingLowerBoundDirect(stable.directBuffer(), from, target);
                ProgressMonitors.checkCanceled(checked);
                requireGallopingLowerBoundResult(stable.values, from, target, result);
                return result;
            } catch (UnsatisfiedLinkError olderLibrary) {
                // Keep old explicitly loaded libraries compatible through the Java oracle.
            }
        }
        return gallopingLowerBoundJavaPrepared(stable.values, from, target, checked);
    }

    public static int gallopingLowerBoundJava(
            int[] values, int fromIndex, int target) {
        IProgressMonitor monitor = IProgressMonitor.noop();
        int[] stable = Objects.requireNonNull(values, "values");
        requireSorted(stable, monitor);
        int from = checkFromIndex(fromIndex, stable.length);
        return gallopingLowerBoundJavaPrepared(stable, from, target, monitor);
    }

    /**
     * Exact interpolation search over a prepared strictly ascending snapshot.
     *
     * <p>Interpolation is Java-only: it is useful for near-uniform integer domains but has no
     * native promotion until benchmark evidence justifies another ABI symbol.</p>
     */
    public static int interpolationIndexOf(PreparedInt32 prepared, int target) {
        return interpolationIndexOf(prepared, target, null);
    }

    public static int interpolationIndexOf(
            PreparedInt32 prepared, int target, IProgressMonitor monitor) {
        PreparedInt32 stable = Objects.requireNonNull(prepared, "prepared");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        return interpolationIndexOfJavaPrepared(stable.values, target, checked);
    }

    public static int interpolationIndexOf(PreparedDirectInt32 prepared, int target) {
        return interpolationIndexOf(prepared, target, null);
    }

    public static int interpolationIndexOf(
            PreparedDirectInt32 prepared, int target, IProgressMonitor monitor) {
        PreparedDirectInt32 stable = Objects.requireNonNull(prepared, "prepared");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        return interpolationIndexOfJavaPrepared(stable.values, target, checked);
    }

    public static int interpolationIndexOf(int[] values, int target) {
        return interpolationIndexOf(values, target, null);
    }

    public static int interpolationIndexOf(
            int[] values, int target, IProgressMonitor monitor) {
        return interpolationIndexOf(prepareSorted(values, monitor), target, monitor);
    }

    public static int interpolationIndexOfJava(int[] values, int target) {
        IProgressMonitor monitor = IProgressMonitor.noop();
        int[] stable = Objects.requireNonNull(values, "values");
        requireSorted(stable, monitor);
        return interpolationIndexOfJavaPrepared(stable, target, monitor);
    }

    public static int lowerBoundJava(int[] values, int target) {
        IProgressMonitor monitor = IProgressMonitor.noop();
        int[] stable = Objects.requireNonNull(values, "values");
        requireSorted(stable, monitor);
        return lowerBoundJavaPrepared(stable, target, monitor);
    }

    private static int lowerBoundJavaPrepared(
            int[] values, int target, IProgressMonitor monitor) {
        int low = 0;
        int high = values.length;
        int polls = 0;
        while (low < high) {
            if ((polls++ & 255) == 0) ProgressMonitors.checkCanceled(monitor);
            int middle = low + ((high - low) >>> 1);
            if (values[middle] < target) low = middle + 1;
            else high = middle;
        }
        ProgressMonitors.checkCanceled(monitor);
        return low;
    }

    private static int lowerBoundJavaPrepared(
            IntBuffer values, int target, IProgressMonitor monitor) {
        int low = 0;
        int high = values.capacity();
        int polls = 0;
        while (low < high) {
            if ((polls++ & 255) == 0) ProgressMonitors.checkCanceled(monitor);
            int middle = low + ((high - low) >>> 1);
            if (values.get(middle) < target) low = middle + 1;
            else high = middle;
        }
        ProgressMonitors.checkCanceled(monitor);
        return low;
    }

    private static int gallopingLowerBoundJavaPrepared(
            int[] values, int fromIndex, int target, IProgressMonitor monitor) {
        if (fromIndex == values.length || values[fromIndex] >= target) return fromIndex;

        int low = fromIndex + 1;
        long span = 1L;
        int polls = 0;
        while ((long) fromIndex + span < values.length
                && values[(int) ((long) fromIndex + span)] < target) {
            if ((polls++ & 255) == 0) ProgressMonitors.checkCanceled(monitor);
            low = (int) ((long) fromIndex + span) + 1;
            span <<= 1;
        }
        long bound = (long) fromIndex + span + 1L;
        int high = bound < values.length ? (int) bound : values.length;
        while (low < high) {
            if ((polls++ & 255) == 0) ProgressMonitors.checkCanceled(monitor);
            int middle = low + ((high - low) >>> 1);
            if (values[middle] < target) low = middle + 1;
            else high = middle;
        }
        ProgressMonitors.checkCanceled(monitor);
        return low;
    }

    private static int gallopingLowerBoundJavaPrepared(
            IntBuffer values, int fromIndex, int target, IProgressMonitor monitor) {
        int length = values.capacity();
        if (fromIndex == length || values.get(fromIndex) >= target) return fromIndex;

        int low = fromIndex + 1;
        long span = 1L;
        int polls = 0;
        while ((long) fromIndex + span < length
                && values.get((int) ((long) fromIndex + span)) < target) {
            if ((polls++ & 255) == 0) ProgressMonitors.checkCanceled(monitor);
            low = (int) ((long) fromIndex + span) + 1;
            span <<= 1;
        }
        long bound = (long) fromIndex + span + 1L;
        int high = bound < length ? (int) bound : length;
        while (low < high) {
            if ((polls++ & 255) == 0) ProgressMonitors.checkCanceled(monitor);
            int middle = low + ((high - low) >>> 1);
            if (values.get(middle) < target) low = middle + 1;
            else high = middle;
        }
        ProgressMonitors.checkCanceled(monitor);
        return low;
    }

    private static int interpolationIndexOfJavaPrepared(
            int[] values, int target, IProgressMonitor monitor) {
        int low = 0;
        int high = values.length - 1;
        int polls = 0;
        while (low <= high && target >= values[low] && target <= values[high]) {
            if ((polls++ & 63) == 0) ProgressMonitors.checkCanceled(monitor);
            if (low == high) return values[low] == target ? low : -1;

            long numerator =
                    ((long) target - values[low]) * (long) (high - low);
            long denominator = (long) values[high] - values[low];
            int position = low + (int) (numerator / denominator);
            int value = values[position];
            if (value == target) return position;
            if (value < target) low = position + 1;
            else high = position - 1;
        }
        ProgressMonitors.checkCanceled(monitor);
        return -1;
    }

    private static int interpolationIndexOfJavaPrepared(
            IntBuffer values, int target, IProgressMonitor monitor) {
        int low = 0;
        int high = values.capacity() - 1;
        int polls = 0;
        while (low <= high
                && target >= values.get(low)
                && target <= values.get(high)) {
            if ((polls++ & 63) == 0) ProgressMonitors.checkCanceled(monitor);
            if (low == high) return values.get(low) == target ? low : -1;

            int lowValue = values.get(low);
            int highValue = values.get(high);
            long numerator =
                    ((long) target - lowValue) * (long) (high - low);
            long denominator = (long) highValue - lowValue;
            int position = low + (int) (numerator / denominator);
            int value = values.get(position);
            if (value == target) return position;
            if (value < target) low = position + 1;
            else high = position - 1;
        }
        ProgressMonitors.checkCanceled(monitor);
        return -1;
    }

    private static int checkFromIndex(int fromIndex, int length) {
        if (fromIndex < 0 || fromIndex > length) {
            throw new IndexOutOfBoundsException(
                    "fromIndex=" + fromIndex + ", length=" + length);
        }
        return fromIndex;
    }

    public static int indexOfBytes(byte[] haystack, byte[] needle) {
        return indexOfBytes(haystack, needle, null);
    }

    public static int indexOfBytes(
            byte[] haystack, byte[] needle, IProgressMonitor monitor) {
        byte[] text = Objects.requireNonNull(haystack, "haystack");
        byte[] pattern = Objects.requireNonNull(needle, "needle");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (monitor == null
                && nativeEnabled
                && text.length >= NATIVE_MIN_BYTES
                && pattern.length >= 2) {
            int result = nativeIndexOfBytes(text, pattern);
            ProgressMonitors.checkCanceled(checked);
            requireByteSearchResult(text, pattern, result);
            return result;
        }
        return indexOfBytesJava(text, pattern, checked);
    }

    public static int indexOfBytes(
            PreparedBytes haystack, PreparedBytePattern needle) {
        return indexOfBytes(haystack, needle, null);
    }

    public static int indexOfBytes(
            PreparedBytes haystack,
            PreparedBytePattern needle,
            IProgressMonitor monitor) {
        PreparedBytes text = Objects.requireNonNull(haystack, "haystack");
        PreparedBytePattern pattern = Objects.requireNonNull(needle, "needle");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        int result;
        if (monitor == null
                && nativeEnabled
                && text.size() >= NATIVE_MIN_BYTES
                && pattern.size() >= 2) {
            result = nativeIndexOfDirect(text.directBuffer(), pattern.directBuffer());
            ProgressMonitors.checkCanceled(checked);
            requireByteSearchResult(text.bytes, pattern.bytes, result);
        } else {
            result = indexOfBytesJava(text, pattern, checked);
        }
        return result;
    }

    public static int indexOfBytesJava(byte[] haystack, byte[] needle) {
        return indexOfBytesJava(
                Objects.requireNonNull(haystack, "haystack"),
                Objects.requireNonNull(needle, "needle"),
                IProgressMonitor.noop());
    }

    /** Boyer-Moore-Horspool binary literal search with exact first-occurrence semantics. */
    private static int indexOfBytesJava(
            byte[] haystack, byte[] needle, IProgressMonitor monitor) {
        if (needle.length == 0) return 0;
        if (needle.length > haystack.length) return -1;
        if (needle.length == 1) {
            for (int index = 0; index < haystack.length; index++) {
                if ((index & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
                if (haystack[index] == needle[0]) return index;
            }
            return -1;
        }
        int[] shift = new int[256];
        Arrays.fill(shift, needle.length);
        for (int index = 0; index < needle.length - 1; index++) {
            shift[needle[index] & 0xff] = needle.length - 1 - index;
        }
        int position = 0;
        int maximum = haystack.length - needle.length;
        int last = needle.length - 1;
        int polls = 0;
        while (position <= maximum) {
            if ((polls++ & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            if (haystack[position + last] == needle[last]) {
                int matched = 0;
                while (matched < last
                        && haystack[position + matched] == needle[matched]) {
                    matched++;
                }
                if (matched == last) return position;
            }
            position += shift[haystack[position + last] & 0xff];
        }
        ProgressMonitors.checkCanceled(monitor);
        return -1;
    }

    private static int indexOfBytesJava(
            PreparedBytes haystack,
            PreparedBytePattern needle,
            IProgressMonitor monitor) {
        int patternLength = needle.size();
        int textLength = haystack.size();
        if (patternLength == 0) return 0;
        if (patternLength > textLength) return -1;
        if (patternLength == 1) {
            byte wanted = needle.byteAt(0);
            for (int index = 0; index < textLength; index++) {
                if ((index & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
                if (haystack.byteAt(index) == wanted) return index;
            }
            return -1;
        }
        int position = 0;
        int maximum = textLength - patternLength;
        int last = patternLength - 1;
        int polls = 0;
        while (position <= maximum) {
            if ((polls++ & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            byte tail = haystack.byteAt(position + last);
            if (tail == needle.byteAt(last)) {
                int matched = 0;
                while (matched < last
                        && haystack.byteAt(position + matched) == needle.byteAt(matched)) {
                    matched++;
                }
                if (matched == last) return position;
            }
            position += needle.shift(tail & 0xff);
        }
        ProgressMonitors.checkCanceled(monitor);
        return -1;
    }

    private static void requireLowerBoundResult(
            int[] values, int target, int result) {
        if (result < 0
                || result > values.length
                || (result > 0 && values[result - 1] >= target)
                || (result < values.length && values[result] < target)) {
            throw new IllegalStateException("native lower bound violated contract");
        }
    }

    private static void requireLowerBoundResult(
            IntBuffer values, int target, int result) {
        if (result < 0
                || result > values.capacity()
                || (result > 0 && values.get(result - 1) >= target)
                || (result < values.capacity() && values.get(result) < target)) {
            throw new IllegalStateException("native direct lower bound violated contract");
        }
    }

    /**
     * Validate only geometry and a reported positive match. Native absence is trusted after the
     * explicit load-time/randomized differential gates so the hot path does not re-scan in Java.
     */
    private static void requireGallopingLowerBoundResult(
            int[] values, int fromIndex, int target, int result) {
        if (result < fromIndex
                || result > values.length
                || (result > fromIndex && values[result - 1] >= target)
                || (result < values.length && values[result] < target)) {
            throw new IllegalStateException(
                    "native galloping lower bound violated suffix contract");
        }
    }

    private static void requireGallopingLowerBoundResult(
            IntBuffer values, int fromIndex, int target, int result) {
        if (result < fromIndex
                || result > values.capacity()
                || (result > fromIndex && values.get(result - 1) >= target)
                || (result < values.capacity() && values.get(result) < target)) {
            throw new IllegalStateException(
                    "native direct galloping lower bound violated suffix contract");
        }
    }

    private static void requireByteSearchResult(
            byte[] haystack, byte[] needle, int result) {
        if (result < -1) {
            throw new IllegalStateException("native byte-search index outside contract");
        }
        if (result >= 0) {
            if (needle.length > haystack.length
                    || result > haystack.length - needle.length) {
                throw new IllegalStateException("native byte-search index outside contract");
            }
            for (int index = 0; index < needle.length; index++) {
                if (haystack[result + index] != needle[index]) {
                    throw new IllegalStateException("native byte-search false positive");
                }
            }
        }
    }

    private static void requireByteSearchResult(
            ByteBuffer haystack, ByteBuffer needle, int result) {
        int textLength = haystack.capacity();
        int patternLength = needle.capacity();
        if (result < -1) {
            throw new IllegalStateException(
                    "native direct byte-search index outside contract");
        }
        if (result >= 0) {
            if (patternLength > textLength || result > textLength - patternLength) {
                throw new IllegalStateException(
                        "native direct byte-search index outside contract");
            }
            for (int index = 0; index < patternLength; index++) {
                if (haystack.get(result + index) != needle.get(index)) {
                    throw new IllegalStateException(
                            "native direct byte-search false positive");
                }
            }
        }
    }

    private static void requireSorted(int[] values, IProgressMonitor monitor) {
        for (int index = 1; index < values.length; index++) {
            if ((index & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            if (values[index] <= values[index - 1]) {
                throw new IllegalArgumentException("values must be strictly ascending");
            }
        }
    }

    private static void verifyNative() {
        boolean gallopingAvailable = true;
        try {
            if (nativeGallopingLowerBound(new int[0], 0, 0) != 0) {
                throw new LinkageError("native galloping lower-bound empty probe failed");
            }
        } catch (UnsatisfiedLinkError olderLibrary) {
            gallopingAvailable = false;
        }

        int[][] arrays = {
            {},
            {Integer.MIN_VALUE, -1, 0, 2, Integer.MAX_VALUE},
            java.util.stream.IntStream.range(0, 513).map(value -> value * 3 - 700).toArray()
        };
        for (int[] values : arrays) {
            for (int target : new int[] {
                    Integer.MIN_VALUE, -701, -700, -1, 0, 1, 836, Integer.MAX_VALUE}) {
                int expected = lowerBoundJava(values, target);
                int[] copy = values.clone();
                int actual = nativeLowerBound(copy, target);
                if (actual != expected || !Arrays.equals(copy, values)) {
                    throw new LinkageError("native lower-bound oracle probe failed");
                }
                if (gallopingAvailable) {
                    for (int from : new int[] {0, values.length / 2, values.length}) {
                        int suffixExpected =
                                gallopingLowerBoundJava(values, from, target);
                        int galloping =
                                nativeGallopingLowerBound(copy, from, target);
                        if (galloping != suffixExpected) {
                            throw new LinkageError(
                                    "native galloping lower-bound oracle probe failed");
                        }
                    }
                }
            }
        }
        byte[][] texts = {
            {},
            {0},
            "abc abc abcd".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            new byte[1024]
        };
        byte[][] needles = {
            {},
            {0},
            "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "abcd".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            {(byte) 0xff, 0}
        };
        for (byte[] text : texts) {
            for (byte[] needle : needles) {
                int expected = indexOfBytesJava(text, needle);
                byte[] textCopy = text.clone();
                byte[] needleCopy = needle.clone();
                int actual = nativeIndexOfBytes(textCopy, needleCopy);
                PreparedBytes directText = prepareBytes(text);
                PreparedBytePattern directNeedle = preparePattern(needle);
                int direct =
                        nativeIndexOfDirect(
                                directText.directBuffer(), directNeedle.directBuffer());
                if (actual != expected
                        || direct != expected
                        || !Arrays.equals(textCopy, text)
                        || !Arrays.equals(needleCopy, needle)) {
                    throw new LinkageError("native byte-search oracle probe failed");
                }
            }
        }
        for (int[] values : arrays) {
            PreparedDirectInt32 directValues = prepareSortedDirect(values);
            for (int target : new int[] {
                    Integer.MIN_VALUE, -701, -700, -1, 0, 1, 836, Integer.MAX_VALUE}) {
                if (nativeLowerBoundDirect(directValues.directBuffer(), target)
                        != lowerBoundJava(values, target)) {
                    throw new LinkageError("native direct lower-bound oracle probe failed");
                }
                if (gallopingAvailable) {
                    for (int from : new int[] {0, values.length / 2, values.length}) {
                        int suffixExpected =
                                gallopingLowerBoundJava(values, from, target);
                        int galloping =
                                nativeGallopingLowerBoundDirect(
                                        directValues.directBuffer(), from, target);
                        if (galloping != suffixExpected) {
                            throw new LinkageError(
                                    "native direct galloping oracle probe failed");
                        }
                    }
                }
            }
        }
        ChallengeRabinKarpSearch.verifyNative();
        ChallengeZSearch.verifyNativeIfPresent();
        ChallengeMultiPatternSearch.verifyNative();
    }

    private static native int nativeAbi();
    private static native int nativeLowerBound(int[] values, int target);
    private static native int nativeGallopingLowerBound(
            int[] values, int fromIndex, int target);
    private static native int nativeIndexOfBytes(byte[] haystack, byte[] needle);
    private static native int nativeLowerBoundDirect(IntBuffer values, int target);
    private static native int nativeGallopingLowerBoundDirect(
            IntBuffer values, int fromIndex, int target);
    private static native int nativeIndexOfDirect(ByteBuffer haystack, ByteBuffer needle);
}
