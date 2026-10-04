// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.search;

import com.synexia.job.IProgressMonitor;
import com.synexia.nativeinterop.JniLibraryLoader;
import com.synexia.nativeinterop.NativeLibrarySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Optional JNI provider with shared loader ownership and a Java correctness fallback. */
public final class JniSearchEngine implements SearchEngine {
    private static final String LIBRARY_PROPERTY = "m3.native.library";
    private volatile boolean loaded;
    private volatile NativeLibrarySpec selectedLibrary;

    @Override
    public String name() { return "JNI"; }

    @Override
    public int priority() { return 100; }

    @Override
    public boolean available() {
        if (!Boolean.getBoolean("m3.native.enabled")) return false;
        NativeLibrarySpec library =
                NativeLibrarySpec.fromProperties(LIBRARY_PROPERTY, "synexia_m3");
        if (loaded && library.cacheKey().equals(
                selectedLibrary == null ? "" : selectedLibrary.cacheKey())) return true;
        try {
            JniLibraryLoader.load(library);
            if (nativeAbi() != 1) return false;
            selectedLibrary = library;
            loaded = true;
            return true;
        } catch (LinkageError | SecurityException unavailable) {
            loaded = false;
            return false;
        }
    }

    @Override
    public CompiledSearch compile(byte[] needle) { return compile(needle, null); }

    @Override
    public CompiledSearch compile(byte[] needle, IProgressMonitor monitor) {
        IProgressMonitor checked = monitor == null ? IProgressMonitor.noop() : monitor;
        checked.checkCanceled();
        if (!available()) throw new IllegalStateException("JNI search provider is unavailable");
        if (needle == null || needle.length == 0) {
            throw new IllegalArgumentException("needle must not be empty");
        }
        byte[] copy = Arrays.copyOf(needle, needle.length);
        checked.checkCanceled();
        long handle = compileNative(copy);
        if (handle == 0L) throw new IllegalStateException("native pattern compilation returned null handle");
        try {
            checked.checkCanceled();
            return new NativePattern(handle, copy.length);
        } catch (RuntimeException | Error failure) {
            releaseNative(handle);
            throw failure;
        }
    }

    private static native int nativeAbi();
    private static native long compileNative(byte[] needle);
    private static native int indexOfNative(long handle, byte[] haystack, int fromIndex);
    /** Optional ABI-1 extension; an older library falls back to the interface implementation. */
    private static native int lastIndexOfNative(long handle, byte[] haystack, int fromIndex);
    /** Optional zero-copy ABI-1 extension for direct/mapped buffers. */
    private static native int indexOfDirectNative(
            long handle, ByteBuffer haystack, int offset, int length, int fromIndex);
    /** Optional zero-copy ABI-1 extension for reverse direct/mapped-buffer lookup. */
    private static native int lastIndexOfDirectNative(
            long handle, ByteBuffer haystack, int offset, int length, int fromIndex);
    /** Optional ABI-1 extension; an older library falls back to repeated indexOf calls. */
    private static native int countOverlappingNative(long handle, byte[] haystack, int fromIndex);
    /** Optional ABI-1 extension; bounded positions are copied once across JNI. */
    private static native int[] positionsOverlappingNative(
            long handle, byte[] haystack, int fromIndex, int maxMatches);
    private static native void releaseNative(long handle);

    static int countIfNative(CompiledSearch compiled, byte[] haystack, int fromIndex,
            IProgressMonitor monitor) {
        if (!(compiled instanceof NativePattern pattern)) return -1;
        return pattern.countOverlapping(haystack, fromIndex, monitor);
    }

    /** Null means the provider or additive native symbol is unavailable. */
    static int[] positionsIfNative(CompiledSearch compiled, byte[] haystack, int fromIndex,
            int maxMatches, IProgressMonitor monitor) {
        if (!(compiled instanceof NativePattern pattern)) return null;
        return pattern.positionsOverlapping(haystack, fromIndex, maxMatches, monitor);
    }

    private static final class NativePattern implements CompiledSearch {
        private final int needleLength;
        private long handle;

        private NativePattern(long handle, int needleLength) {
            this.handle = handle;
            this.needleLength = needleLength;
        }

        @Override
        public synchronized int indexOf(byte[] haystack, int fromIndex) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            if (haystack == null) throw new NullPointerException("haystack");
            return indexOfNative(handle, haystack, Math.max(0, fromIndex));
        }

        @Override
        public synchronized int indexOf(byte[] haystack, int fromIndex, IProgressMonitor monitor) {
            IProgressMonitor checked = monitor == null ? IProgressMonitor.noop() : monitor;
            checked.checkCanceled();
            int result = indexOf(haystack, fromIndex);
            checked.checkCanceled();
            return result;
        }

        @Override
        public synchronized int lastIndexOf(byte[] haystack, int fromIndex) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            if (haystack == null) throw new NullPointerException("haystack");
            try {
                return lastIndexOfNative(handle, haystack, fromIndex);
            } catch (UnsatisfiedLinkError oldLibrary) {
                return CompiledSearch.super.lastIndexOf(haystack, fromIndex);
            }
        }

        @Override
        public synchronized int indexOf(ByteBuffer haystack, int fromIndex) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            if (haystack == null) throw new NullPointerException("haystack");
            if (!haystack.isDirect()) {
                return CompiledSearch.super.indexOf(haystack, fromIndex);
            }
            ByteBuffer view = haystack.duplicate();
            try {
                int result = indexOfDirectNative(
                        handle, view, view.position(), view.remaining(), fromIndex);
                return result == Integer.MIN_VALUE
                        ? CompiledSearch.super.indexOf(haystack, fromIndex)
                        : result;
            } catch (UnsatisfiedLinkError oldLibrary) {
                return CompiledSearch.super.indexOf(haystack, fromIndex);
            }
        }

        @Override
        public synchronized int lastIndexOf(ByteBuffer haystack, int fromIndex) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            if (haystack == null) throw new NullPointerException("haystack");
            if (!haystack.isDirect()) {
                return CompiledSearch.super.lastIndexOf(haystack, fromIndex);
            }
            ByteBuffer view = haystack.duplicate();
            try {
                int result = lastIndexOfDirectNative(
                        handle, view, view.position(), view.remaining(), fromIndex);
                return result == Integer.MIN_VALUE
                        ? CompiledSearch.super.lastIndexOf(haystack, fromIndex)
                        : result;
            } catch (UnsatisfiedLinkError oldLibrary) {
                return CompiledSearch.super.lastIndexOf(haystack, fromIndex);
            }
        }

        @Override
        public int needleLength() { return needleLength; }

        private synchronized int countOverlapping(byte[] haystack, int fromIndex,
                IProgressMonitor monitor) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            monitor.checkCanceled();
            int count;
            try {
                count = countOverlappingNative(handle, haystack, Math.max(0, fromIndex));
            } catch (UnsatisfiedLinkError oldLibrary) {
                return -1;
            }
            monitor.checkCanceled();
            return count;
        }

        private synchronized int[] positionsOverlapping(byte[] haystack, int fromIndex,
                int maxMatches, IProgressMonitor monitor) {
            if (handle == 0L) throw new IllegalStateException("native search pattern is closed");
            monitor.checkCanceled();
            int[] positions;
            try {
                positions = positionsOverlappingNative(
                        handle, haystack, Math.max(0, fromIndex), maxMatches);
            } catch (UnsatisfiedLinkError oldLibrary) {
                return null;
            }
            monitor.checkCanceled();
            if (positions == null || positions.length > maxMatches) {
                throw new IllegalStateException("native positions returned an invalid batch");
            }
            int previous = Math.max(0, fromIndex) - 1;
            int lastStart = haystack.length - needleLength;
            for (int position : positions) {
                if (position <= previous || position > lastStart) {
                    throw new IllegalStateException("native positions returned an invalid byte offset");
                }
                previous = position;
            }
            return positions;
        }

        @Override
        public String strategy() { return "JNI_PRECOMPUTED_BMH"; }

        @Override
        public synchronized void close() {
            if (handle != 0L) {
                releaseNative(handle);
                handle = 0L;
            }
        }
    }
}
