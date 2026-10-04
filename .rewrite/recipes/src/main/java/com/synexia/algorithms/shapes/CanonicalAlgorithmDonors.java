// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Extended canonical donor library.
 *
 * <p>The methods in this class intentionally encode computational shapes rather than story-level
 * problems. Every donor is primitive-first and every execution receives a progress monitor.
 */
public final class CanonicalAlgorithmDonors {

    private CanonicalAlgorithmDonors() {}

    public record Donor<I, O>(AlgorithmDescriptor descriptor, ProgressAlgorithm<I, O> algorithm)
            implements ProgressAlgorithm<I, O> {
        public Donor {
            Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(algorithm, "algorithm");
        }
        @Override public O execute(I input, IProgressMonitor monitor) {
            return algorithm.execute(input, ProgressMonitors.nonNull(monitor));
        }
    }

    public record LongKey(long[] values, long key) {
        public LongKey { values = Objects.requireNonNull(values, "values").clone(); }
        @Override public long[] values() { return values.clone(); }
    }
    public record LongK(long[] values, int k) {
        public LongK {
            values = Objects.requireNonNull(values, "values").clone();
            if (k < 0 || k >= values.length) throw new IllegalArgumentException("k");
        }
        @Override public long[] values() { return values.clone(); }
    }
    public record LongTarget(long[] values, long target) {
        public LongTarget { values = Objects.requireNonNull(values, "values").clone(); }
        @Override public long[] values() { return values.clone(); }
    }
    public record NextGraph(int[] next, int start) {
        public NextGraph {
            next = Objects.requireNonNull(next, "next").clone();
            if (start < 0 || start >= next.length) throw new IllegalArgumentException("start");
            for (int n : next) if (n < -1 || n >= next.length) throw new IllegalArgumentException("next");
        }
        @Override public int[] next() { return next.clone(); }
    }
    public record IntGraph(int[] offsets, int[] edges, int source) {
        public IntGraph {
            offsets = Objects.requireNonNull(offsets, "offsets").clone();
            edges = Objects.requireNonNull(edges, "edges").clone();
            validateCsr(offsets, edges);
            if (source < 0 || source >= offsets.length - 1) throw new IllegalArgumentException("source");
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
    }
    public record IntGraphPair(int[] offsets, int[] edges, int source, int target) {
        public IntGraphPair {
            offsets = Objects.requireNonNull(offsets, "offsets").clone();
            edges = Objects.requireNonNull(edges, "edges").clone();
            validateCsr(offsets, edges);
            int n = offsets.length - 1;
            if (source < 0 || source >= n || target < 0 || target >= n) throw new IllegalArgumentException("vertex");
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
    }
    public record Intervals(long[] starts, long[] ends) {
        public Intervals {
            starts = Objects.requireNonNull(starts, "starts").clone();
            ends = Objects.requireNonNull(ends, "ends").clone();
            if (starts.length != ends.length) throw new IllegalArgumentException("length");
            for (int i = 0; i < starts.length; i++) if (ends[i] < starts[i]) throw new IllegalArgumentException("interval");
        }
        @Override public long[] starts() { return starts.clone(); }
        @Override public long[] ends() { return ends.clone(); }
    }
    public record Knapsack(long[] weights, long[] values, long capacity) {
        public Knapsack {
            weights = Objects.requireNonNull(weights, "weights").clone();
            values = Objects.requireNonNull(values, "values").clone();
            if (weights.length != values.length || capacity < 0) throw new IllegalArgumentException();
            for (long w : weights) if (w < 0) throw new IllegalArgumentException("weight");
        }
        @Override public long[] weights() { return weights.clone(); }
        @Override public long[] values() { return values.clone(); }
    }
    public record Bipartite(int leftSize, int rightSize, int[] offsets, int[] edges) {
        public Bipartite {
            if (leftSize < 0 || rightSize < 0) throw new IllegalArgumentException();
            offsets = Objects.requireNonNull(offsets, "offsets").clone();
            edges = Objects.requireNonNull(edges, "edges").clone();
            if (offsets.length != leftSize + 1 || offsets[0] != 0 || offsets[leftSize] != edges.length) {
                throw new IllegalArgumentException("offsets");
            }
            for (int i = 1; i < offsets.length; i++) if (offsets[i] < offsets[i - 1]) throw new IllegalArgumentException("offsets");
            for (int e : edges) if (e < 0 || e >= rightSize) throw new IllegalArgumentException("edge");
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
    }
    public record Preferences(int[][] proposerPreferences, int[][] receiverRank) {
        public Preferences {
            proposerPreferences = copy2d(proposerPreferences);
            receiverRank = copy2d(receiverRank);
            if (proposerPreferences.length != receiverRank.length) throw new IllegalArgumentException("size");
        }
        @Override public int[][] proposerPreferences() { return copy2d(proposerPreferences); }
        @Override public int[][] receiverRank() { return copy2d(receiverRank); }
    }

    public static Donor<LongKey, Integer> exponentialSearch() {
        return donor("search.exponential.long", AlgorithmPurpose.FIND_ONE, AlgorithmShape.EXPONENTIAL_SEARCH,
                "O(log i)", "O(1)", (input, supplied) -> {
            IProgressMonitor m = start(supplied, "exponential-search", Math.max(1, input.values().length));
            try {
                long[] a = input.values();
                if (a.length == 0) return -1;
                m.checkCanceled();
                if (a[0] == input.key()) return 0;
                long bound = 1L;
                while (bound < a.length && a[(int) bound] < input.key()) {
                    m.checkCanceled();
                    m.worked(1);
                    bound <<= 1;
                }
                int lo = (int) (bound >>> 1);
                int hi = (int) Math.min(bound, (long) a.length - 1L);
                while (lo <= hi) {
                    m.checkCanceled();
                    int mid = (lo + hi) >>> 1;
                    long v = a[mid];
                    m.worked(1);
                    if (v < input.key()) lo = mid + 1;
                    else if (v > input.key()) hi = mid - 1;
                    else return mid;
                }
                return -(lo + 1);
            } finally { m.done(); }
        });
    }

    public static Donor<LongK, Long> quickSelect() {
        return donor("select.quick.long", AlgorithmPurpose.TOP_K, AlgorithmShape.QUICKSELECT,
                "O(n) expected", "O(n) clone", (input, supplied) -> {
            long[] a = input.values();
            IProgressMonitor m = start(supplied, "quickselect", Math.max(1, a.length));
            try {
                int lo = 0, hi = a.length - 1, k = input.k();
                while (lo <= hi) {
                    m.checkCanceled();
                    long pivot = a[(lo + hi) >>> 1];
                    int i = lo, j = hi;
                    while (i <= j) {
                        while (a[i] < pivot) i++;
                        while (a[j] > pivot) j--;
                        if (i <= j) {
                            long t = a[i]; a[i] = a[j]; a[j] = t;
                            i++; j--;
                        }
                        m.worked(1);
                    }
                    if (k <= j) hi = j;
                    else if (k >= i) lo = i;
                    else return a[k];
                }
                throw new IllegalStateException();
            } finally { m.done(); }
        });
    }

    public static Donor<LongTarget, int[]> twoPointerPair() {
        return donor("pointer.two-sum.sorted.long", AlgorithmPurpose.FIND_ONE, AlgorithmShape.TWO_POINTER,
                "O(n)", "O(1)", (input, supplied) -> {
            long[] a = input.values();
            IProgressMonitor m = start(supplied, "two-pointer", Math.max(1, a.length));
            try {
                int l = 0, r = a.length - 1;
                while (l < r) {
                    m.checkCanceled();
                    long sum = a[l] + a[r];
                    m.worked(1);
                    if (sum == input.target()) return new int[]{l, r};
                    if (sum < input.target()) l++; else r--;
                }
                return new int[]{-1, -1};
            } finally { m.done(); }
        });
    }

    public static Donor<NextGraph, Boolean> fastSlowCycle() {
        return donor("pointer.fast-slow-cycle.int", AlgorithmPurpose.REACHABILITY, AlgorithmShape.FAST_SLOW_POINTER,
                "O(n)", "O(1)", (input, supplied) -> {
            int[] next = input.next();
            IProgressMonitor m = start(supplied, "fast-slow-cycle", Math.max(1, next.length));
            try {
                int slow = input.start(), fast = input.start();
                while (fast >= 0 && next[fast] >= 0) {
                    m.checkCanceled();
                    slow = next[slow];
                    fast = next[next[fast]];
                    m.worked(1);
                    if (slow == fast) return true;
                }
                return false;
            } finally { m.done(); }
        });
    }

    public static Donor<long[], long[]> prefixScan() {
        return donor("scan.prefix-sum.long", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.PREFIX_SCAN,
                "O(n)", "O(n)", (values, supplied) -> {
            long[] a = Objects.requireNonNull(values, "values");
            long[] out = new long[a.length + 1];
            IProgressMonitor m = start(supplied, "prefix-scan", Math.max(1, a.length));
            try {
                for (int i = 0; i < a.length; i++) {
                    m.checkCanceled();
                    out[i + 1] = out[i] + a[i];
                    m.worked(1);
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static Donor<long[], Long> kadane() {
        return donor("sequence.kadane.long", AlgorithmPurpose.TOP_K, AlgorithmShape.KADANE,
                "O(n)", "O(1)", (values, supplied) -> {
            long[] a = Objects.requireNonNull(values, "values");
            if (a.length == 0) throw new IllegalArgumentException("empty");
            IProgressMonitor m = start(supplied, "kadane", a.length);
            try {
                long best = a[0], current = a[0];
                m.worked(1);
                for (int i = 1; i < a.length; i++) {
                    m.checkCanceled();
                    current = Math.max(a[i], current + a[i]);
                    best = Math.max(best, current);
                    m.worked(1);
                }
                return best;
            } finally { m.done(); }
        });
    }

    public static Donor<long[], Integer> lisLength() {
        return donor("sequence.lis.long", AlgorithmPurpose.SEQUENCE_MATCH, AlgorithmShape.LONGEST_INCREASING_SUBSEQUENCE,
                "O(n log n)", "O(n)", (values, supplied) -> {
            long[] a = Objects.requireNonNull(values, "values");
            long[] tails = new long[a.length];
            int size = 0;
            IProgressMonitor m = start(supplied, "lis", Math.max(1, a.length));
            try {
                for (long v : a) {
                    m.checkCanceled();
                    int p = Arrays.binarySearch(tails, 0, size, v);
                    if (p < 0) p = -p - 1;
                    tails[p] = v;
                    if (p == size) size++;
                    m.worked(1);
                }
                return size;
            } finally { m.done(); }
        });
    }

    public static Donor<PrimitiveInputs.LongWindow, long[]> slidingMaximum() {
        return donor("window.monotonic-max.long", AlgorithmPurpose.RUNNING_RANGE, AlgorithmShape.MONOTONIC_QUEUE,
                "O(n)", "O(width)", (input, supplied) -> {
            long[] a = input.values();
            int w = input.width();
            long[] out = new long[a.length - w + 1];
            int[] dq = new int[a.length];
            int head = 0, tail = 0;
            IProgressMonitor m = start(supplied, "monotonic-window", Math.max(1, a.length));
            try {
                for (int i = 0; i < a.length; i++) {
                    m.checkCanceled();
                    while (head < tail && dq[head] <= i - w) head++;
                    while (head < tail && a[dq[tail - 1]] <= a[i]) tail--;
                    dq[tail++] = i;
                    if (i >= w - 1) out[i - w + 1] = a[dq[head]];
                    m.worked(1);
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static Donor<long[], int[]> nextGreaterIndices() {
        return donor("stack.next-greater.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.MONOTONIC_STACK,
                "O(n)", "O(n)", (values, supplied) -> {
            long[] a = Objects.requireNonNull(values, "values");
            int[] out = new int[a.length];
            Arrays.fill(out, -1);
            int[] stack = new int[a.length];
            int top = 0;
            IProgressMonitor m = start(supplied, "monotonic-stack", Math.max(1, a.length));
            try {
                for (int i = 0; i < a.length; i++) {
                    m.checkCanceled();
                    while (top > 0 && a[stack[top - 1]] < a[i]) out[stack[--top]] = i;
                    stack[top++] = i;
                    m.worked(1);
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static Donor<IntGraph, int[]> depthFirstSearch() {
        return donor("graph.dfs.csr-int", AlgorithmPurpose.TRAVERSE, AlgorithmShape.DFS,
                "O(V+E)", "O(V)", (input, supplied) -> {
            int[] offsets = input.offsets(), edges = input.edges();
            int n = offsets.length - 1;
            int[] stack = new int[n], order = new int[n];
            byte[] seen = new byte[n];
            int sp = 0, count = 0;
            stack[sp++] = input.source();
            seen[input.source()] = 1;
            IProgressMonitor m = start(supplied, "dfs", (long)n + edges.length);
            try {
                while (sp > 0) {
                    m.checkCanceled();
                    int v = stack[--sp];
                    order[count++] = v;
                    m.worked(1);
                    for (int i = offsets[v + 1] - 1; i >= offsets[v]; i--) {
                        int x = edges[i];
                        if (seen[x] == 0) {
                            seen[x] = 1;
                            stack[sp++] = x;
                        }
                        m.worked(1);
                    }
                }
                return Arrays.copyOf(order, count);
            } finally { m.done(); }
        });
    }

    public static Donor<IntGraph, int[]> connectedComponents() {
        return donor("graph.components.csr-int", AlgorithmPurpose.TRAVERSE, AlgorithmShape.CONNECTED_COMPONENTS,
                "O(V+E)", "O(V)", (input, supplied) -> {
            int[] offsets = input.offsets(), edges = input.edges();
            int n = offsets.length - 1;
            int[] label = new int[n], queue = new int[n];
            Arrays.fill(label, -1);
            int component = 0;
            IProgressMonitor m = start(supplied, "components", (long)n + edges.length);
            try {
                for (int root = 0; root < n; root++) {
                    if (label[root] >= 0) continue;
                    int h = 0, t = 0;
                    queue[t++] = root; label[root] = component;
                    while (h < t) {
                        m.checkCanceled();
                        int v = queue[h++];
                        m.worked(1);
                        for (int i = offsets[v]; i < offsets[v + 1]; i++) {
                            int x = edges[i];
                            if (label[x] < 0) { label[x] = component; queue[t++] = x; }
                            m.worked(1);
                        }
                    }
                    component++;
                }
                return label;
            } finally { m.done(); }
        });
    }

    public static Donor<IntGraphPair, Boolean> bidirectionalReachability() {
        return donor("graph.bidirectional-reachability.csr-int", AlgorithmPurpose.REACHABILITY, AlgorithmShape.BIDIRECTIONAL_SEARCH,
                "O(V+E)", "O(V)", (input, supplied) -> {
            if (input.source() == input.target()) return true;
            int[] offsets = input.offsets(), edges = input.edges();
            int n = offsets.length - 1;
            int[][] rev = reverseCsr(offsets, edges);
            int[] q1 = new int[n], q2 = new int[n];
            byte[] s1 = new byte[n], s2 = new byte[n];
            int h1=0,t1=0,h2=0,t2=0;
            q1[t1++]=input.source(); s1[input.source()]=1;
            q2[t2++]=input.target(); s2[input.target()]=1;
            IProgressMonitor m = start(supplied, "bidirectional-search", (long)n + edges.length);
            try {
                while (h1<t1 && h2<t2) {
                    m.checkCanceled();
                    int v=q1[h1++];
                    for(int i=offsets[v];i<offsets[v+1];i++){ int x=edges[i]; if(s2[x]!=0)return true; if(s1[x]==0){s1[x]=1;q1[t1++]=x;} m.worked(1);}
                    int u=q2[h2++];
                    for(int i=rev[0][u];i<rev[0][u+1];i++){ int x=rev[1][i]; if(s1[x]!=0)return true; if(s2[x]==0){s2[x]=1;q2[t2++]=x;} m.worked(1);}
                }
                return false;
            } finally { m.done(); }
        });
    }

    public static Donor<Intervals, long[][]> mergeIntervals() {
        return donor("interval.merge.long", AlgorithmPurpose.MERGE, AlgorithmShape.INTERVAL_MERGE,
                "O(n log n)", "O(n)", (input, supplied) -> {
            int n=input.starts().length;
            long[][] a=new long[n][2];
            long[] s=input.starts(), e=input.ends();
            for(int i=0;i<n;i++){a[i][0]=s[i];a[i][1]=e[i];}
            Arrays.sort(a, Comparator.comparingLong(x->x[0]));
            List<long[]> out=new ArrayList<>();
            IProgressMonitor m=start(supplied,"interval-merge",Math.max(1,n));
            try {
                for(long[] x:a){
                    m.checkCanceled();
                    if(out.isEmpty() || out.get(out.size()-1)[1] < x[0]) out.add(x.clone());
                    else out.get(out.size()-1)[1]=Math.max(out.get(out.size()-1)[1],x[1]);
                    m.worked(1);
                }
                return out.toArray(long[][]::new);
            } finally {m.done();}
        });
    }

    public static Donor<Intervals, Integer> maxOverlap() {
        return donor("interval.sweep.max-overlap.long", AlgorithmPurpose.RUNNING_RANGE, AlgorithmShape.SWEEP_LINE,
                "O(n log n)", "O(n)", (input,supplied)->{
            int n=input.starts().length;
            long[][] ev=new long[n*2][2];
            long[] s=input.starts(), e=input.ends();
            for(int i=0;i<n;i++){ev[i*2]=new long[]{s[i],1};ev[i*2+1]=new long[]{e[i],-1};}
            Arrays.sort(ev,(a,b)->{int c=Long.compare(a[0],b[0]);return c!=0?c:Long.compare(a[1],b[1]);});
            int cur=0,best=0;
            IProgressMonitor m=start(supplied,"sweep-line",Math.max(1,ev.length));
            try{
                for(long[] x:ev){m.checkCanceled();cur+=(int)x[1];best=Math.max(best,cur);m.worked(1);}
                return best;
            }finally{m.done();}
        });
    }

    public static Donor<Intervals, Long> coveredLength() {
        return donor("interval.coverage.long", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.COVERAGE,
                "O(n log n)", "O(n)", (input,supplied)->{
            long[][] merged=mergeIntervals().execute(input,ProgressMonitors.child(ProgressMonitors.nonNull(supplied),1));
            long total=0;
            IProgressMonitor m=start(supplied,"coverage",Math.max(1,merged.length));
            try{for(long[] x:merged){m.checkCanceled();total+=x[1]-x[0];m.worked(1);}return total;}finally{m.done();}
        });
    }

    public static Donor<Intervals, Integer> greedyIntervalSchedule() {
        return donor("greedy.interval-schedule.long", AlgorithmPurpose.SCHEDULING, AlgorithmShape.GREEDY,
                "O(n log n)", "O(n)", (input,supplied)->{
            int n=input.starts().length;
            long[][] a=new long[n][2]; long[] s=input.starts(),e=input.ends();
            for(int i=0;i<n;i++)a[i]=new long[]{s[i],e[i]};
            Arrays.sort(a,Comparator.comparingLong(x->x[1]));
            long last=Long.MIN_VALUE; int count=0;
            IProgressMonitor m=start(supplied,"greedy-interval-schedule",Math.max(1,n));
            try{
                for(long[] x:a){m.checkCanceled();if(x[0]>=last){count++;last=x[1];}m.worked(1);}
                return count;
            }finally{m.done();}
        });
    }

    public static Donor<LongTarget, Boolean> subsetSumBacktracking() {
        return donor("backtracking.subset-sum.long", AlgorithmPurpose.SEQUENCE_MATCH, AlgorithmShape.BACKTRACKING,
                "O(2^n)", "O(n)", (input,supplied)->{
            long[] a=input.values();
            IProgressMonitor m=start(supplied,"subset-sum-backtracking",IProgressMonitor.UNKNOWN);
            try{return subsetDfs(a,0,input.target(),m);}finally{m.done();}
        });
    }

    public static Donor<Knapsack, Long> branchAndBoundKnapsack() {
        return donor("optimization.knapsack-branch-bound.long", AlgorithmPurpose.TOP_K, AlgorithmShape.BRANCH_AND_BOUND,
                "O(2^n) worst-case", "O(n)", (input,supplied)->{
            long[] w=input.weights(),v=input.values();
            Integer[] order=new Integer[w.length];
            for(int i=0;i<order.length;i++)order[i]=i;
            Arrays.sort(order,(a,b)->Double.compare((double)v[b]/Math.max(1L,w[b]),(double)v[a]/Math.max(1L,w[a])));
            IProgressMonitor m=start(supplied,"branch-and-bound-knapsack",IProgressMonitor.UNKNOWN);
            try{return knapsackDfs(order,w,v,0,0,0,input.capacity(),0,m);}finally{m.done();}
        });
    }

    public static Donor<Bipartite, int[]> bipartiteMatching() {
        return donor("matching.bipartite.kuhn-int", AlgorithmPurpose.FAN_IN, AlgorithmShape.BIPARTITE_MATCHING,
                "O(VE)", "O(V)", (input,supplied)->{
            int[] offsets=input.offsets(),edges=input.edges();
            int[] rightMatch=new int[input.rightSize()];
            Arrays.fill(rightMatch,-1);
            IProgressMonitor m=start(supplied,"bipartite-matching",Math.max(1,input.leftSize()));
            try{
                for(int left=0;left<input.leftSize();left++){
                    m.checkCanceled();
                    byte[] seen=new byte[input.rightSize()];
                    augment(left,offsets,edges,rightMatch,seen,m);
                    m.worked(1);
                }
                return rightMatch;
            }finally{m.done();}
        });
    }

    public static Donor<Preferences, int[]> stableMatching() {
        return donor("matching.stable.gale-shapley-int", AlgorithmPurpose.FAN_IN, AlgorithmShape.STABLE_MATCHING,
                "O(n^2)", "O(n)", (input,supplied)->{
            int[][] pref=input.proposerPreferences(),rank=input.receiverRank();
            int n=pref.length;
            int[] next=new int[n], receiver=new int[n], proposer=new int[n], queue=new int[Math.max(1,n)];
            Arrays.fill(receiver,-1);Arrays.fill(proposer,-1);
            int head=0,tail=0,count=n;
            for(int i=0;i<n;i++){queue[tail]=i;tail=(tail+1)%queue.length;}
            IProgressMonitor m=start(supplied,"stable-matching",(long)n*n);
            try{
                while(count>0){
                    m.checkCanceled();
                    int p=queue[head];head=(head+1)%queue.length;count--;
                    if(next[p]>=pref[p].length)continue;
                    int r=pref[p][next[p]++];
                    int current=receiver[r];
                    if(current<0 || rank[r][p]<rank[r][current]){
                        receiver[r]=p;proposer[p]=r;
                        if(current>=0){
                            proposer[current]=-1;
                            queue[tail]=current;tail=(tail+1)%queue.length;count++;
                        }
                    } else {
                        queue[tail]=p;tail=(tail+1)%queue.length;count++;
                    }
                    m.worked(1);
                }
                return proposer;
            }finally{m.done();}
        });
    }

    public static List<AlgorithmDescriptor> descriptors() {
        return List.of(
                exponentialSearch().descriptor(), quickSelect().descriptor(), twoPointerPair().descriptor(),
                fastSlowCycle().descriptor(), prefixScan().descriptor(), kadane().descriptor(),
                lisLength().descriptor(), slidingMaximum().descriptor(), nextGreaterIndices().descriptor(),
                depthFirstSearch().descriptor(), connectedComponents().descriptor(),
                bidirectionalReachability().descriptor(), mergeIntervals().descriptor(),
                maxOverlap().descriptor(), coveredLength().descriptor(), greedyIntervalSchedule().descriptor(),
                subsetSumBacktracking().descriptor(), branchAndBoundKnapsack().descriptor(),
                bipartiteMatching().descriptor(), stableMatching().descriptor());
    }

    private static <I,O> Donor<I,O> donor(String id, AlgorithmPurpose purpose, AlgorithmShape shape,
            String time, String space, ProgressAlgorithm<I,O> algorithm) {
        return new Donor<>(new AlgorithmDescriptor(id,purpose,shape,
                EnumSet.of(PatternView.STRATEGY,PatternView.TEMPLATE_METHOD,PatternView.DAG_NODE),
                time,space,true,false,true,false),algorithm);
    }

    private static IProgressMonitor start(IProgressMonitor supplied,String name,long total){
        IProgressMonitor m=ProgressMonitors.nonNull(supplied);
        m.beginTask(name,total);
        m.checkCanceled();
        return m;
    }

    private static boolean subsetDfs(long[] a,int i,long remaining,IProgressMonitor m){
        m.checkCanceled(); m.worked(1);
        if(remaining==0)return true;
        if(i==a.length)return false;
        return subsetDfs(a,i+1,remaining,m)||subsetDfs(a,i+1,remaining-a[i],m);
    }

    private static long knapsackDfs(Integer[] order,long[] w,long[] v,int pos,long used,long value,
            long capacity,long best,IProgressMonitor m){
        m.checkCanceled();m.worked(1);
        if(used>capacity)return best;
        best=Math.max(best,value);
        if(pos==order.length)return best;
        double bound=value; long remain=capacity-used;
        for(int p=pos;p<order.length && remain>0;p++){
            int i=order[p];
            long take=Math.min(remain,w[i]);
            bound+=w[i]==0?v[i]:((double)v[i]*take/w[i]);
            remain-=take;
        }
        if(bound<=best)return best;
        int idx=order[pos];
        best=knapsackDfs(order,w,v,pos+1,used+w[idx],value+v[idx],capacity,best,m);
        return knapsackDfs(order,w,v,pos+1,used,value,capacity,best,m);
    }

    private static boolean augment(int left,int[] offsets,int[] edges,int[] match,byte[] seen,IProgressMonitor m){
        for(int i=offsets[left];i<offsets[left+1];i++){
            m.checkCanceled();
            int r=edges[i];
            if(seen[r]!=0)continue;
            seen[r]=1;m.worked(1);
            if(match[r]<0 || augment(match[r],offsets,edges,match,seen,m)){match[r]=left;return true;}
        }
        return false;
    }

    private static void validateCsr(int[] offsets,int[] edges){
        if(offsets.length<2 || offsets[0]!=0 || offsets[offsets.length-1]!=edges.length)throw new IllegalArgumentException("CSR");
        for(int i=1;i<offsets.length;i++)if(offsets[i]<offsets[i-1])throw new IllegalArgumentException("offsets");
        int n=offsets.length-1;for(int e:edges)if(e<0||e>=n)throw new IllegalArgumentException("edge");
    }

    private static int[][] reverseCsr(int[] offsets,int[] edges){
        int n=offsets.length-1;int[] ro=new int[n+1];
        for(int e:edges)ro[e+1]++;
        for(int i=1;i<=n;i++)ro[i]+=ro[i-1];
        int[] cursor=ro.clone(),re=new int[edges.length];
        for(int v=0;v<n;v++)for(int i=offsets[v];i<offsets[v+1];i++)re[cursor[edges[i]]++]=v;
        return new int[][]{ro,re};
    }

    private static int[][] copy2d(int[][] value){
        Objects.requireNonNull(value,"value");
        int[][] out=new int[value.length][];
        for(int i=0;i<value.length;i++)out[i]=Objects.requireNonNull(value[i],"row").clone();
        return out;
    }
}
