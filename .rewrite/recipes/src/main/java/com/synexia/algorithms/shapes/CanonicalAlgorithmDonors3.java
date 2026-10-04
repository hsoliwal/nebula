// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Canonical non-duplicate donors: sorting, range/indexing, strings and trees.
 *
 * <p>Every donor is monitor-first. Inputs are primitive/compact where practical and callers may
 * project story-level problems over these kernels without copying the implementation.
 */
public final class CanonicalAlgorithmDonors3 {
    private CanonicalAlgorithmDonors3() {}

    public record ThreeWay(long[] values, long pivot) {
        public ThreeWay { values = Objects.requireNonNull(values, "values").clone(); }
        @Override public long[] values() { return values.clone(); }
    }

    public record RangeQueries(long[] values, int[] left, int[] right) {
        public RangeQueries {
            values = Objects.requireNonNull(values, "values").clone();
            left = Objects.requireNonNull(left, "left").clone();
            right = Objects.requireNonNull(right, "right").clone();
            if (left.length != right.length) throw new IllegalArgumentException("query lengths");
            for (int i = 0; i < left.length; i++) {
                if (left[i] < 0 || right[i] < left[i] || right[i] >= values.length) {
                    throw new IllegalArgumentException("query " + i);
                }
            }
        }
        @Override public long[] values() { return values.clone(); }
        @Override public int[] left() { return left.clone(); }
        @Override public int[] right() { return right.clone(); }
    }

    public record TrieBatch(String[] words, String[] queries) {
        public TrieBatch {
            words = Objects.requireNonNull(words, "words").clone();
            queries = Objects.requireNonNull(queries, "queries").clone();
            for (String s : words) validateLower(s);
            for (String s : queries) validateLower(s);
        }
        @Override public String[] words() { return words.clone(); }
        @Override public String[] queries() { return queries.clone(); }
    }

    public record PatternText(String text, String pattern) {
        public PatternText {
            text = Objects.requireNonNull(text, "text");
            pattern = Objects.requireNonNull(pattern, "pattern");
        }
    }

    public record AhoInput(String text, String[] patterns) {
        public AhoInput {
            text = Objects.requireNonNull(text, "text");
            validateLower(text);
            patterns = Objects.requireNonNull(patterns, "patterns").clone();
            for (String s : patterns) validateLower(s);
        }
        @Override public String[] patterns() { return patterns.clone(); }
    }

    public record IntTree(int[] offsets, int[] edges, int root) {
        public IntTree {
            offsets = Objects.requireNonNull(offsets, "offsets").clone();
            edges = Objects.requireNonNull(edges, "edges").clone();
            if (offsets.length < 2 || offsets[0] != 0 || offsets[offsets.length - 1] != edges.length) {
                throw new IllegalArgumentException("csr");
            }
            int n = offsets.length - 1;
            if (root < 0 || root >= n) throw new IllegalArgumentException("root");
            for (int i = 1; i < offsets.length; i++) if (offsets[i] < offsets[i - 1]) throw new IllegalArgumentException("csr");
            for (int e : edges) if (e < 0 || e >= n) throw new IllegalArgumentException("edge");
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
    }

    public record LcaQueries(int[] parent, int[] a, int[] b) {
        public LcaQueries {
            parent = Objects.requireNonNull(parent, "parent").clone();
            a = Objects.requireNonNull(a, "a").clone();
            b = Objects.requireNonNull(b, "b").clone();
            if (a.length != b.length) throw new IllegalArgumentException("query lengths");
            validateParent(parent);
            for (int x : a) if (x < 0 || x >= parent.length) throw new IllegalArgumentException("a");
            for (int x : b) if (x < 0 || x >= parent.length) throw new IllegalArgumentException("b");
        }
        @Override public int[] parent() { return parent.clone(); }
        @Override public int[] a() { return a.clone(); }
        @Override public int[] b() { return b.clone(); }
    }

    public record AncestorQueries(int[] parent, int[] node, int[] k) {
        public AncestorQueries {
            parent = Objects.requireNonNull(parent, "parent").clone();
            node = Objects.requireNonNull(node, "node").clone();
            k = Objects.requireNonNull(k, "k").clone();
            if (node.length != k.length) throw new IllegalArgumentException("query lengths");
            validateParent(parent);
            for (int i = 0; i < node.length; i++) {
                if (node[i] < 0 || node[i] >= parent.length || k[i] < 0) throw new IllegalArgumentException("query");
            }
        }
        @Override public int[] parent() { return parent.clone(); }
        @Override public int[] node() { return node.clone(); }
        @Override public int[] k() { return k.clone(); }
    }

    public static CanonicalAlgorithmDonors.Donor<long[], long[]> insertionSort() {
        return donor("sort.insertion.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.INSERTION_SORT, "O(n^2)", "O(n) clone", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "insertion-sort", Math.max(1L, (long)a.length * Math.max(1, a.length - 1) / 2));
            try {
                for (int i = 1; i < a.length; i++) {
                    long v = a[i]; int j = i - 1;
                    while (j >= 0 && a[j] > v) {
                        m.checkCanceled(); a[j + 1] = a[j--]; m.worked(1);
                    }
                    a[j + 1] = v;
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], long[]> mergeSort() {
        return donor("sort.merge.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.MERGE_SORT, "O(n log n)", "O(n)", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone(), tmp = new long[input.length];
            IProgressMonitor m = start(supplied, "merge-sort", Math.max(1, a.length));
            try {
                for (int width = 1; width < a.length; width <<= 1) {
                    m.checkCanceled();
                    for (int lo = 0; lo < a.length; lo += width << 1) {
                        int mid = Math.min(lo + width, a.length), hi = Math.min(lo + (width << 1), a.length);
                        int i = lo, j = mid, k = lo;
                        while (i < mid || j < hi) {
                            if (j >= hi || (i < mid && a[i] <= a[j])) tmp[k++] = a[i++]; else tmp[k++] = a[j++];
                            m.worked(1);
                        }
                    }
                    long[] swap = a; a = tmp; tmp = swap;
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], long[]> quickSort() {
        return donor("sort.quick.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.QUICK_SORT, "O(n log n) average", "O(log n) stack", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "quick-sort", IProgressMonitor.UNKNOWN);
            try {
                if (a.length < 2) return a;
                int[] stack = new int[a.length << 1]; int sp = 0;
                stack[sp++] = 0; stack[sp++] = a.length - 1;
                while (sp > 0) {
                    m.checkCanceled();
                    int hi = stack[--sp], lo = stack[--sp];
                    while (lo < hi) {
                        long pivot = a[(lo + hi) >>> 1]; int i = lo, j = hi;
                        while (i <= j) {
                            while (a[i] < pivot) i++;
                            while (a[j] > pivot) j--;
                            if (i <= j) { long t = a[i]; a[i++] = a[j]; a[j--] = t; }
                            m.worked(1);
                        }
                        if (j - lo < hi - i) {
                            if (i < hi) { stack[sp++] = i; stack[sp++] = hi; }
                            hi = j;
                        } else {
                            if (lo < j) { stack[sp++] = lo; stack[sp++] = j; }
                            lo = i;
                        }
                    }
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], long[]> heapSort() {
        return donor("sort.heap.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.HEAP_SORT, "O(n log n)", "O(n) clone", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "heap-sort", IProgressMonitor.UNKNOWN);
            try {
                for (int i = (a.length >>> 1) - 1; i >= 0; i--) siftDown(a, a.length, i, m);
                for (int end = a.length - 1; end > 0; end--) {
                    m.checkCanceled(); long t = a[0]; a[0] = a[end]; a[end] = t; siftDown(a, end, 0, m);
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<int[], int[]> countingSort() {
        return donor("sort.counting.int", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.COUNTING_SORT, "O(n + range)", "O(range)", (input, supplied) -> {
            int[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "counting-sort", Math.max(1, a.length));
            try {
                if (a.length < 2) return a;
                int min = a[0], max = a[0];
                for (int v : a) { m.checkCanceled(); min = Math.min(min, v); max = Math.max(max, v); m.worked(1); }
                long rangeLong = (long)max - min + 1;
                if (rangeLong > 10_000_000L) throw new IllegalArgumentException("counting range too large");
                int[] count = new int[(int)rangeLong];
                for (int v : a) count[v - min]++;
                int k = 0;
                for (int i = 0; i < count.length; i++) while (count[i]-- > 0) a[k++] = i + min;
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], long[]> radixSort() {
        return donor("sort.radix-signed.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.RADIX_SORT, "O(8n)", "O(n+256)", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone(), b = new long[input.length];
            IProgressMonitor m = start(supplied, "radix-sort", Math.max(1L, 8L * a.length));
            try {
                for (int shift = 0; shift < 64; shift += 8) {
                    int[] count = new int[256];
                    for (long v : a) count[(int)(((v ^ Long.MIN_VALUE) >>> shift) & 0xffL)]++;
                    int sum = 0; for (int i = 0; i < 256; i++) { int c = count[i]; count[i] = sum; sum += c; }
                    for (long v : a) { m.checkCanceled(); int d = (int)(((v ^ Long.MIN_VALUE) >>> shift) & 0xffL); b[count[d]++] = v; m.worked(1); }
                    long[] t = a; a = b; b = t;
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<ThreeWay, long[]> dutchFlag() {
        return donor("partition.dutch-flag.long", AlgorithmPurpose.PARTITIONING, AlgorithmShape.DUTCH_FLAG, "O(n)", "O(n) clone", (in, supplied) -> {
            long[] a = in.values(); int lo = 0, mid = 0, hi = a.length - 1;
            IProgressMonitor m = start(supplied, "dutch-flag", Math.max(1, a.length));
            try {
                while (mid <= hi) {
                    m.checkCanceled();
                    if (a[mid] < in.pivot()) { long t = a[lo]; a[lo++] = a[mid]; a[mid++] = t; }
                    else if (a[mid] > in.pivot()) { long t = a[mid]; a[mid] = a[hi]; a[hi--] = t; }
                    else mid++;
                    m.worked(1);
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], Long> majorityVote() {
        return donor("selection.boyer-moore-majority.long", AlgorithmPurpose.FIND_ONE, AlgorithmShape.BOYER_MOORE_MAJORITY, "O(n)", "O(1)", (a, supplied) -> {
            Objects.requireNonNull(a, "input");
            IProgressMonitor m = start(supplied, "majority-vote", Math.max(1, a.length * 2L));
            try {
                long candidate = 0; int count = 0;
                for (long v : a) { m.checkCanceled(); if (count == 0) candidate = v; count += v == candidate ? 1 : -1; m.worked(1); }
                int seen = 0; for (long v : a) { if (v == candidate) seen++; m.worked(1); }
                if (seen <= a.length / 2) throw new IllegalArgumentException("no strict majority");
                return candidate;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<RangeQueries, long[]> fenwickRangeSum() {
        return donor("range.fenwick.sum.long", AlgorithmPurpose.RUNNING_RANGE, AlgorithmShape.FENWICK_TREE, "O((n+q) log n)", "O(n)", (in, supplied) -> {
            long[] v = in.values(); long[] bit = new long[v.length + 1], out = new long[in.left().length];
            IProgressMonitor m = start(supplied, "fenwick-range-sum", Math.max(1, v.length + out.length));
            try {
                for (int i = 0; i < v.length; i++) { m.checkCanceled(); for (int x = i + 1; x < bit.length; x += x & -x) bit[x] += v[i]; m.worked(1); }
                int[] l = in.left(), r = in.right();
                for (int i = 0; i < out.length; i++) { m.checkCanceled(); out[i] = prefix(bit, r[i] + 1) - prefix(bit, l[i]); m.worked(1); }
                return out;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<RangeQueries, long[]> segmentTreeRangeMin() {
        return donor("range.segment-tree.min.long", AlgorithmPurpose.RUNNING_RANGE, AlgorithmShape.SEGMENT_TREE, "O(n + q log n)", "O(n)", (in, supplied) -> {
            long[] a = in.values(); int size = 1; while (size < a.length) size <<= 1;
            long[] tree = new long[size << 1]; Arrays.fill(tree, Long.MAX_VALUE);
            IProgressMonitor m = start(supplied, "segment-tree-min", Math.max(1, a.length + in.left().length));
            try {
                System.arraycopy(a, 0, tree, size, a.length);
                for (int i = size - 1; i > 0; i--) tree[i] = Math.min(tree[i << 1], tree[i << 1 | 1]);
                int[] lq = in.left(), rq = in.right(); long[] out = new long[lq.length];
                for (int q = 0; q < out.length; q++) {
                    m.checkCanceled(); int l = lq[q] + size, r = rq[q] + size + 1; long best = Long.MAX_VALUE;
                    while (l < r) {
                        if ((l & 1) != 0) best = Math.min(best, tree[l++]);
                        if ((r & 1) != 0) best = Math.min(best, tree[--r]);
                        l >>>= 1; r >>>= 1;
                    }
                    out[q] = best; m.worked(1);
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<RangeQueries, long[]> sparseTableRangeMin() {
        return donor("range.sparse-table.min.long", AlgorithmPurpose.RUNNING_RANGE, AlgorithmShape.SPARSE_TABLE, "O(n log n + q)", "O(n log n)", (in, supplied) -> {
            long[] a = in.values(); int n = a.length, levels = n == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(n);
            IProgressMonitor m = start(supplied, "sparse-table-min", Math.max(1, (long)n * Math.max(1, levels)));
            try {
                long[][] st = new long[levels][];
                if (levels > 0) {
                    st[0] = a.clone();
                    for (int k = 1; k < levels; k++) {
                        int len = 1 << k, half = len >>> 1, count = n - len + 1; st[k] = new long[Math.max(0, count)];
                        for (int i = 0; i < count; i++) { m.checkCanceled(); st[k][i] = Math.min(st[k - 1][i], st[k - 1][i + half]); m.worked(1); }
                    }
                }
                int[] l = in.left(), r = in.right(); long[] out = new long[l.length];
                for (int i = 0; i < out.length; i++) {
                    int len = r[i] - l[i] + 1, k = 31 - Integer.numberOfLeadingZeros(len), span = 1 << k;
                    out[i] = Math.min(st[k][l[i]], st[k][r[i] - span + 1]);
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<TrieBatch, boolean[]> trieMembership() {
        return donor("string.trie.membership.lowercase", AlgorithmPurpose.MEMBERSHIP, AlgorithmShape.TRIE, "O(total characters)", "O(total characters * alphabet)", (in, supplied) -> {
            String[] words = in.words(), queries = in.queries(); int maxNodes = 1; for (String s : words) maxNodes += s.length();
            int[][] next = new int[maxNodes][26]; for (int[] row : next) Arrays.fill(row, -1);
            byte[] terminal = new byte[maxNodes]; int nodes = 1;
            IProgressMonitor m = start(supplied, "trie-membership", IProgressMonitor.UNKNOWN);
            try {
                for (String s : words) {
                    int node = 0;
                    for (int i = 0; i < s.length(); i++) { m.checkCanceled(); int c = s.charAt(i) - 'a'; if (next[node][c] < 0) next[node][c] = nodes++; node = next[node][c]; m.worked(1); }
                    terminal[node] = 1;
                }
                boolean[] out = new boolean[queries.length];
                for (int q = 0; q < queries.length; q++) {
                    int node = 0; boolean ok = true; String s = queries[q];
                    for (int i = 0; i < s.length(); i++) { m.checkCanceled(); node = next[node][s.charAt(i) - 'a']; m.worked(1); if (node < 0) { ok = false; break; } }
                    out[q] = ok && terminal[node] != 0;
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<PatternText, Integer> horspool() {
        return donor(
                "string.horspool.first",
                AlgorithmPurpose.SEQUENCE_MATCH,
                AlgorithmShape.BOYER_MOORE_HORSPOOL,
                "O(65536 + n + m) worst; sublinear skips common",
                "O(65536)",
                (in, supplied) -> {
                    String text = in.text(), pattern = in.pattern();
                    int n = text.length(), mlen = pattern.length();
                    IProgressMonitor monitor =
                            start(supplied, "boyer-moore-horspool", Math.max(1, n));
                    try {
                        if (mlen == 0) return 0;
                        if (mlen > n) return -1;
                        int[] shift = new int[Character.MAX_VALUE + 1];
                        Arrays.fill(shift, mlen);
                        for (int index = 0; index + 1 < mlen; index++) {
                            shift[pattern.charAt(index)] = mlen - 1 - index;
                        }
                        int end = mlen - 1;
                        while (end < n) {
                            monitor.checkCanceled();
                            int patternIndex = mlen - 1;
                            int textIndex = end;
                            while (patternIndex >= 0
                                    && text.charAt(textIndex) == pattern.charAt(patternIndex)) {
                                patternIndex--;
                                textIndex--;
                            }
                            if (patternIndex < 0) return end - mlen + 1;
                            end = Math.addExact(end, shift[text.charAt(end)]);
                            monitor.worked(1);
                        }
                        return -1;
                    } finally {
                        monitor.done();
                    }
                });
    }
    public static CanonicalAlgorithmDonors.Donor<PatternText, Integer> rabinKarp() {
        return donor("string.rabin-karp.first", AlgorithmPurpose.SEQUENCE_MATCH, AlgorithmShape.RABIN_KARP, "O(n+m) expected", "O(1)", (in, supplied) -> {
            String t = in.text(), p = in.pattern(); int n = t.length(), mlen = p.length();
            IProgressMonitor mon = start(supplied, "rabin-karp", Math.max(1, n));
            try {
                if (mlen == 0) return 0; if (mlen > n) return -1;
                long base = 911382323L, hp = 0, hw = 0, pow = 1;
                for (int i = 0; i < mlen; i++) { hp = hp * base + p.charAt(i) + 1; hw = hw * base + t.charAt(i) + 1; if (i + 1 < mlen) pow *= base; }
                for (int i = 0; i <= n - mlen; i++) {
                    mon.checkCanceled(); if (hp == hw && t.regionMatches(i, p, 0, mlen)) return i; mon.worked(1);
                    if (i < n - mlen) hw = (hw - (t.charAt(i) + 1) * pow) * base + t.charAt(i + mlen) + 1;
                }
                return -1;
            } finally { mon.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<String, int[]> zAlgorithm() {
        return donor("string.z-array", AlgorithmPurpose.SEQUENCE_MATCH, AlgorithmShape.Z_ALGORITHM, "O(n)", "O(n)", (s, supplied) -> {
            Objects.requireNonNull(s, "input"); int n = s.length(); int[] z = new int[n]; int l = 0, r = 0;
            IProgressMonitor m = start(supplied, "z-algorithm", Math.max(1, n));
            try {
                for (int i = 1; i < n; i++) {
                    m.checkCanceled(); if (i <= r) z[i] = Math.min(r - i + 1, z[i - l]);
                    while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
                    if (i + z[i] - 1 > r) { l = i; r = i + z[i] - 1; }
                    m.worked(1);
                }
                if (n > 0) z[0] = n; return z;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<String, int[]> manacher() {
        return donor("string.manacher.longest-palindrome", AlgorithmPurpose.SEQUENCE_MATCH, AlgorithmShape.MANACHER, "O(n)", "O(n)", (s, supplied) -> {
            Objects.requireNonNull(s, "input"); int n = s.length(), len = n * 2 + 1, center = 0, right = 0, bestC = 0, bestR = 0; int[] p = new int[len];
            IProgressMonitor m = start(supplied, "manacher", Math.max(1, len));
            try {
                for (int i = 0; i < len; i++) {
                    m.checkCanceled(); int mirror = (center << 1) - i; if (i < right && mirror >= 0) p[i] = Math.min(right - i, p[mirror]);
                    while (i - p[i] - 1 >= 0 && i + p[i] + 1 < len && transformedEqual(s, i - p[i] - 1, i + p[i] + 1)) p[i]++;
                    if (i + p[i] > right) { center = i; right = i + p[i]; }
                    if (p[i] > bestR) { bestR = p[i]; bestC = i; }
                    m.worked(1);
                }
                int start = (bestC - bestR) >>> 1; return new int[]{start, bestR};
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<AhoInput, int[]> ahoCorasickCounts() {
        return donor("string.aho-corasick.counts.lowercase", AlgorithmPurpose.SEQUENCE_MATCH, AlgorithmShape.AHO_CORASICK, "O(pattern chars + text + matches)", "O(pattern chars * alphabet)", (in, supplied) -> {
            String[] patterns = in.patterns(); int maxNodes = 1; for (String s : patterns) maxNodes += s.length();
            int[][] next = new int[maxNodes][26]; for (int[] row : next) Arrays.fill(row, -1);
            int[] fail = new int[maxNodes], terminal = new int[patterns.length]; List<int[]> outputs = new ArrayList<>(); for (int i = 0; i < maxNodes; i++) outputs.add(new int[0]);
            int nodes = 1;
            IProgressMonitor m = start(supplied, "aho-corasick", IProgressMonitor.UNKNOWN);
            try {
                for (int pi = 0; pi < patterns.length; pi++) {
                    int node = 0; String s = patterns[pi];
                    for (int i = 0; i < s.length(); i++) { int c = s.charAt(i) - 'a'; if (next[node][c] < 0) next[node][c] = nodes++; node = next[node][c]; m.worked(1); }
                    terminal[pi] = node;
                }
                ArrayDeque<Integer> q = new ArrayDeque<>();
                for (int c = 0; c < 26; c++) { int x = next[0][c]; if (x >= 0) { fail[x] = 0; q.add(x); } else next[0][c] = 0; }
                while (!q.isEmpty()) {
                    m.checkCanceled(); int v = q.remove();
                    for (int c = 0; c < 26; c++) { int x = next[v][c]; if (x >= 0) { fail[x] = next[fail[v]][c]; q.add(x); } else next[v][c] = next[fail[v]][c]; }
                }
                int[] visits = new int[nodes]; int state = 0;
                for (int i = 0; i < in.text().length(); i++) { m.checkCanceled(); state = next[state][in.text().charAt(i) - 'a']; visits[state]++; m.worked(1); }
                int[] order = new int[nodes]; int oi = 0; q.clear(); q.add(0);
                while (!q.isEmpty()) { int v = q.remove(); order[oi++] = v; for (int c = 0; c < 26; c++) { int x = next[v][c]; if (x != 0 && fail[x] == v) q.add(x); } }
                for (int i = oi - 1; i > 0; i--) visits[fail[order[i]]] += visits[order[i]];
                int[] out = new int[patterns.length]; for (int i = 0; i < out.length; i++) out[i] = visits[terminal[i]];
                return out;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<IntTree, int[]> treePreorder() {
        return donor("tree.preorder.csr-int", AlgorithmPurpose.TRAVERSE, AlgorithmShape.TREE_TRAVERSAL, "O(V+E)", "O(V)", (in, supplied) -> {
            int[] off = in.offsets(), edges = in.edges(), out = new int[off.length - 1], stack = new int[off.length - 1]; byte[] seen = new byte[out.length];
            int sp = 0, count = 0; stack[sp++] = in.root(); seen[in.root()] = 1;
            IProgressMonitor m = start(supplied, "tree-preorder", Math.max(1, out.length + edges.length));
            try {
                while (sp > 0) {
                    m.checkCanceled(); int v = stack[--sp]; out[count++] = v; m.worked(1);
                    for (int i = off[v + 1] - 1; i >= off[v]; i--) { int x = edges[i]; if (seen[x] == 0) { seen[x] = 1; stack[sp++] = x; } m.worked(1); }
                }
                return Arrays.copyOf(out, count);
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<LcaQueries, int[]> lowestCommonAncestor() {
        return donor("tree.lca.parent-depth", AlgorithmPurpose.FIND_ONE, AlgorithmShape.LOWEST_COMMON_ANCESTOR, "O(n + qh)", "O(n)", (in, supplied) -> {
            int[] parent = in.parent(), depth = depths(parent), a = in.a(), b = in.b(), out = new int[a.length];
            IProgressMonitor m = start(supplied, "lca", IProgressMonitor.UNKNOWN);
            try {
                for (int i = 0; i < out.length; i++) {
                    m.checkCanceled(); int x = a[i], y = b[i];
                    while (depth[x] > depth[y]) { x = parent[x]; m.worked(1); }
                    while (depth[y] > depth[x]) { y = parent[y]; m.worked(1); }
                    while (x != y) { x = parent[x]; y = parent[y]; m.worked(1); }
                    out[i] = x;
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<AncestorQueries, int[]> binaryLifting() {
        return donor("tree.binary-lifting.kth-ancestor", AlgorithmPurpose.FIND_ONE, AlgorithmShape.BINARY_LIFTING, "O(n log n + q log n)", "O(n log n)", (in, supplied) -> {
            int[] parent = in.parent(); int maxK = 0; for (int x : in.k()) maxK = Math.max(maxK, x);
            int levels = Math.max(1, 32 - Integer.numberOfLeadingZeros(Math.max(1, maxK))); int[][] up = new int[levels][parent.length];
            System.arraycopy(parent, 0, up[0], 0, parent.length);
            for (int j = 1; j < levels; j++) for (int i = 0; i < parent.length; i++) up[j][i] = up[j - 1][i] < 0 ? -1 : up[j - 1][up[j - 1][i]];
            int[] nodes = in.node(), ks = in.k(), out = new int[nodes.length];
            IProgressMonitor m = start(supplied, "binary-lifting", Math.max(1, nodes.length));
            try {
                for (int q = 0; q < out.length; q++) {
                    m.checkCanceled(); int v = nodes[q], k = ks[q], bit = 0;
                    while (k != 0 && v >= 0) { if ((k & 1) != 0) v = bit < up.length ? up[bit][v] : -1; k >>>= 1; bit++; }
                    out[q] = v; m.worked(1);
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<IntTree, int[]> eulerTour() {
        return donor("tree.euler-tour.entry-exit", AlgorithmPurpose.TRAVERSE, AlgorithmShape.EULER_TOUR, "O(V+E)", "O(V)", (in, supplied) -> {
            int n = in.offsets().length - 1, timer = 0; int[] tin = new int[n], tout = new int[n], off = in.offsets(), edges = in.edges();
            Arrays.fill(tin, -1); int[] stackV = new int[n * 2 + 1], stackI = new int[n * 2 + 1], parent = new int[n]; Arrays.fill(parent, -2);
            int sp = 0; stackV[sp] = in.root(); stackI[sp++] = off[in.root()]; parent[in.root()] = -1;
            IProgressMonitor m = start(supplied, "euler-tour", Math.max(1, n + edges.length));
            try {
                while (sp > 0) {
                    m.checkCanceled(); int idx = sp - 1, v = stackV[idx];
                    if (tin[v] < 0) tin[v] = timer++;
                    int ei = stackI[idx];
                    if (ei < off[v + 1]) {
                        int x = edges[ei]; stackI[idx] = ei + 1; m.worked(1);
                        if (x == parent[v] || parent[x] != -2) continue;
                        parent[x] = v; stackV[sp] = x; stackI[sp++] = off[x];
                    } else {
                        tout[v] = timer++; sp--; m.worked(1);
                    }
                }
                int[] out = new int[n * 2]; for (int i = 0; i < n; i++) { out[i << 1] = tin[i]; out[i << 1 | 1] = tout[i]; } return out;
            } finally { m.done(); }
        });
    }

    public static List<AlgorithmDescriptor> descriptors() {
        return List.of(
                insertionSort().descriptor(), mergeSort().descriptor(), quickSort().descriptor(), heapSort().descriptor(),
                countingSort().descriptor(), radixSort().descriptor(), dutchFlag().descriptor(), majorityVote().descriptor(),
                fenwickRangeSum().descriptor(), segmentTreeRangeMin().descriptor(), sparseTableRangeMin().descriptor(),
                trieMembership().descriptor(), horspool().descriptor(), rabinKarp().descriptor(), zAlgorithm().descriptor(), manacher().descriptor(),
                ahoCorasickCounts().descriptor(), treePreorder().descriptor(), lowestCommonAncestor().descriptor(),
                binaryLifting().descriptor(), eulerTour().descriptor());
    }

    private static <I, O> CanonicalAlgorithmDonors.Donor<I, O> donor(
            String id, AlgorithmPurpose purpose, AlgorithmShape shape, String time, String space,
            ProgressAlgorithm<I, O> algorithm) {
        return new CanonicalAlgorithmDonors.Donor<>(
                new AlgorithmDescriptor(id, purpose, shape,
                        EnumSet.of(PatternView.STRATEGY, PatternView.TEMPLATE_METHOD, PatternView.DAG_NODE),
                        time, space, true, false, true, false),
                algorithm);
    }

    private static IProgressMonitor start(IProgressMonitor supplied, String name, long total) {
        IProgressMonitor m = ProgressMonitors.nonNull(supplied); m.beginTask(name, total); m.checkCanceled(); return m;
    }

    private static void siftDown(long[] a, int size, int i, IProgressMonitor m) {
        long v = a[i]; int half = size >>> 1;
        while (i < half) {
            m.checkCanceled(); int child = (i << 1) + 1, right = child + 1;
            if (right < size && a[right] > a[child]) child = right;
            if (a[child] <= v) break;
            a[i] = a[child]; i = child; m.worked(1);
        }
        a[i] = v;
    }

    private static long prefix(long[] bit, int end) {
        long sum = 0; for (int x = end; x > 0; x -= x & -x) sum += bit[x]; return sum;
    }

    private static boolean transformedEqual(String s, int a, int b) {
        boolean sepA = (a & 1) == 0, sepB = (b & 1) == 0;
        return sepA == sepB && (sepA || s.charAt(a >>> 1) == s.charAt(b >>> 1));
    }

    private static void validateLower(String s) {
        Objects.requireNonNull(s, "string");
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < 'a' || s.charAt(i) > 'z') {
            throw new IllegalArgumentException("lowercase a-z only: " + s);
        }
    }

    private static void validateParent(int[] parent) {
        if (parent.length == 0) throw new IllegalArgumentException("empty parent");
        int roots = 0;
        for (int i = 0; i < parent.length; i++) {
            if (parent[i] == -1) roots++;
            else if (parent[i] < 0 || parent[i] >= parent.length || parent[i] == i) throw new IllegalArgumentException("parent");
        }
        if (roots != 1) throw new IllegalArgumentException("exactly one root required");
    }

    private static int[] depths(int[] parent) {
        int[] depth = new int[parent.length]; Arrays.fill(depth, -1);
        for (int i = 0; i < parent.length; i++) {
            int v = i, d = 0;
            while (v >= 0 && depth[v] < 0) { v = parent[v]; d++; if (d > parent.length) throw new IllegalArgumentException("cycle"); }
            int base = v < 0 ? -1 : depth[v];
            v = i;
            while (v >= 0 && depth[v] < 0) { depth[v] = base + d; v = parent[v]; d--; }
        }
        return depth;
    }
}
