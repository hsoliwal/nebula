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
import java.util.PriorityQueue;

/** Monitor-first graph donors mined from direct Java problem corpora and deduplicated by mechanism. */
public final class CanonicalAlgorithmDonors4Graph {
    private CanonicalAlgorithmDonors4Graph() {}

    public record WeightedEdges(int vertices, int[] from, int[] to, long[] weight, int source) {
        public WeightedEdges {
            if (vertices < 0) throw new IllegalArgumentException("vertices");
            from = cloneOf(from); to = cloneOf(to); weight = cloneOf(weight);
            if (from.length != to.length || from.length != weight.length) throw new IllegalArgumentException("edge lengths");
            if (vertices == 0 ? source != -1 : source < 0 || source >= vertices) throw new IllegalArgumentException("source");
            for (int i = 0; i < from.length; i++) {
                if (from[i] < 0 || from[i] >= vertices || to[i] < 0 || to[i] >= vertices) throw new IllegalArgumentException("edge");
            }
        }
        @Override public int[] from() { return from.clone(); }
        @Override public int[] to() { return to.clone(); }
        @Override public long[] weight() { return weight.clone(); }
    }

    public record HeuristicEdges(int vertices, int[] from, int[] to, long[] weight, long[] heuristic, int source, int target) {
        public HeuristicEdges {
            if (vertices <= 0) throw new IllegalArgumentException("vertices");
            from = cloneOf(from); to = cloneOf(to); weight = cloneOf(weight); heuristic = cloneOf(heuristic);
            if (from.length != to.length || from.length != weight.length || heuristic.length != vertices) throw new IllegalArgumentException("length");
            if (source < 0 || source >= vertices || target < 0 || target >= vertices) throw new IllegalArgumentException("vertex");
            for (int i = 0; i < from.length; i++) {
                if (from[i] < 0 || from[i] >= vertices || to[i] < 0 || to[i] >= vertices || weight[i] < 0) throw new IllegalArgumentException("edge");
            }
        }
        @Override public int[] from() { return from.clone(); }
        @Override public int[] to() { return to.clone(); }
        @Override public long[] weight() { return weight.clone(); }
        @Override public long[] heuristic() { return heuristic.clone(); }
    }

    public record ZeroOneCsr(int[] offsets, int[] edges, byte[] weight, int source) {
        public ZeroOneCsr {
            offsets = cloneOf(offsets); edges = cloneOf(edges); weight = Objects.requireNonNull(weight, "weight").clone();
            validateCsr(offsets, edges);
            if (edges.length != weight.length) throw new IllegalArgumentException("weight length");
            if (source < 0 || source >= offsets.length - 1) throw new IllegalArgumentException("source");
            for (int i = 0; i < weight.length; i++) if (weight[i] != 0 && weight[i] != 1) throw new IllegalArgumentException("0/1 weights");
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
        @Override public byte[] weight() { return weight.clone(); }
    }

    public record UndirectedWeightedEdges(int vertices, int[] a, int[] b, long[] weight) {
        public UndirectedWeightedEdges {
            if (vertices < 0) throw new IllegalArgumentException("vertices");
            a = cloneOf(a); b = cloneOf(b); weight = cloneOf(weight);
            if (a.length != b.length || a.length != weight.length) throw new IllegalArgumentException("length");
            for (int i = 0; i < a.length; i++) {
                if (a[i] < 0 || a[i] >= vertices || b[i] < 0 || b[i] >= vertices) throw new IllegalArgumentException("edge");
            }
        }
        @Override public int[] a() { return a.clone(); }
        @Override public int[] b() { return b.clone(); }
        @Override public long[] weight() { return weight.clone(); }
    }

    public record Csr(int[] offsets, int[] edges) {
        public Csr {
            offsets = cloneOf(offsets); edges = cloneOf(edges); validateCsr(offsets, edges);
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
    }

    public record DirectedEdges(int vertices, int[] from, int[] to) {
        public DirectedEdges {
            if (vertices < 0) throw new IllegalArgumentException("vertices");
            from = cloneOf(from); to = cloneOf(to);
            if (from.length != to.length) throw new IllegalArgumentException("length");
            for (int i = 0; i < from.length; i++) {
                if (from[i] < 0 || from[i] >= vertices || to[i] < 0 || to[i] >= vertices) throw new IllegalArgumentException("edge");
            }
        }
        @Override public int[] from() { return from.clone(); }
        @Override public int[] to() { return to.clone(); }
    }

    public record FlowNetwork(int vertices, int[] from, int[] to, long[] capacity, long[] cost, int source, int sink) {
        public FlowNetwork {
            if (vertices <= 0) throw new IllegalArgumentException("vertices");
            from = cloneOf(from); to = cloneOf(to); capacity = cloneOf(capacity); cost = cloneOf(cost);
            if (from.length != to.length || from.length != capacity.length || from.length != cost.length) throw new IllegalArgumentException("length");
            if (source < 0 || source >= vertices || sink < 0 || sink >= vertices || source == sink) throw new IllegalArgumentException("terminal");
            for (int i = 0; i < from.length; i++) {
                if (from[i] < 0 || from[i] >= vertices || to[i] < 0 || to[i] >= vertices || capacity[i] < 0) throw new IllegalArgumentException("edge");
            }
        }
        @Override public int[] from() { return from.clone(); }
        @Override public int[] to() { return to.clone(); }
        @Override public long[] capacity() { return capacity.clone(); }
        @Override public long[] cost() { return cost.clone(); }
    }

    public static CanonicalAlgorithmDonors.Donor<HeuristicEdges, Long> aStar() {
        return donor("graph.astar.edge-list", AlgorithmPurpose.SHORTEST_PATH, AlgorithmShape.A_STAR, "O((V+E) log V)", "O(V+E)", (in, supplied) -> {
            int n = in.vertices(); int[] from = in.from(), to = in.to(); long[] w = in.weight(), h = in.heuristic();
            int[] head = heads(n, from); int[] next = nextBySource(n, from); long[] g = new long[n]; Arrays.fill(g, Long.MAX_VALUE); g[in.source()] = 0;
            record Node(int v, long f, long g) {}
            PriorityQueue<Node> pq = new PriorityQueue<>((x,y) -> Long.compare(x.f(), y.f()));
            pq.add(new Node(in.source(), h[in.source()], 0));
            IProgressMonitor m = start(supplied, "a-star", IProgressMonitor.UNKNOWN);
            try {
                while (!pq.isEmpty()) {
                    m.checkCanceled(); Node cur = pq.remove();
                    if (cur.g() != g[cur.v()]) continue;
                    if (cur.v() == in.target()) return cur.g();
                    for (int e = head[cur.v()]; e >= 0; e = next[e]) {
                        long ng = safeAdd(cur.g(), w[e]); int v = to[e];
                        if (ng < g[v]) { g[v] = ng; pq.add(new Node(v, safeAdd(ng, h[v]), ng)); }
                        m.worked(1);
                    }
                }
                return Long.MAX_VALUE;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<WeightedEdges, long[]> bellmanFord() {
        return donor("graph.bellman-ford.edge-list", AlgorithmPurpose.SHORTEST_PATH, AlgorithmShape.BELLMAN_FORD, "O(VE)", "O(V)", (in, supplied) -> {
            int n = in.vertices(); int[] f = in.from(), t = in.to(); long[] w = in.weight(), d = new long[n]; Arrays.fill(d, Long.MAX_VALUE);
            if (n == 0) return d; d[in.source()] = 0;
            IProgressMonitor m = start(supplied, "bellman-ford", Math.max(1L, (long)Math.max(0,n-1) * f.length));
            try {
                for (int pass = 1; pass < n; pass++) {
                    boolean changed = false;
                    for (int e = 0; e < f.length; e++) {
                        m.checkCanceled();
                        if (d[f[e]] != Long.MAX_VALUE) {
                            long nd = safeAddSigned(d[f[e]], w[e]);
                            if (nd < d[t[e]]) { d[t[e]] = nd; changed = true; }
                        }
                        m.worked(1);
                    }
                    if (!changed) break;
                }
                for (int e = 0; e < f.length; e++) if (d[f[e]] != Long.MAX_VALUE && safeAddSigned(d[f[e]], w[e]) < d[t[e]]) {
                    throw new IllegalArgumentException("reachable negative cycle");
                }
                return d;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<WeightedEdges, long[][]> floydWarshall() {
        return donor("graph.floyd-warshall.edge-list", AlgorithmPurpose.SHORTEST_PATH, AlgorithmShape.FLOYD_WARSHALL, "O(V^3)", "O(V^2)", (in, supplied) -> {
            int n = in.vertices(); long[][] d = new long[n][n]; for (long[] row : d) Arrays.fill(row, Long.MAX_VALUE);
            for (int i = 0; i < n; i++) d[i][i] = 0;
            int[] f = in.from(), t = in.to(); long[] w = in.weight(); for (int e = 0; e < f.length; e++) d[f[e]][t[e]] = Math.min(d[f[e]][t[e]], w[e]);
            IProgressMonitor m = start(supplied, "floyd-warshall", Math.max(1L, (long)n*n*n));
            try {
                for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) {
                    m.checkCanceled(); if (d[i][k] == Long.MAX_VALUE) { m.worked(n); continue; }
                    for (int j = 0; j < n; j++) {
                        if (d[k][j] != Long.MAX_VALUE) d[i][j] = Math.min(d[i][j], safeAddSigned(d[i][k], d[k][j]));
                        m.worked(1);
                    }
                }
                return d;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<ZeroOneCsr, long[]> zeroOneBfs() {
        return donor("graph.zero-one-bfs.csr", AlgorithmPurpose.SHORTEST_PATH, AlgorithmShape.ZERO_ONE_BFS, "O(V+E)", "O(V)", (in, supplied) -> {
            int[] off = in.offsets(), edges = in.edges(); byte[] w = in.weight(); int n = off.length - 1; long[] d = new long[n]; Arrays.fill(d, Long.MAX_VALUE);
            ArrayDeque<Integer> q = new ArrayDeque<>(); d[in.source()] = 0; q.add(in.source());
            IProgressMonitor m = start(supplied, "zero-one-bfs", Math.max(1, n + edges.length));
            try {
                while (!q.isEmpty()) {
                    m.checkCanceled(); int u = q.removeFirst();
                    for (int e = off[u]; e < off[u+1]; e++) {
                        int v = edges[e]; long nd = d[u] + w[e];
                        if (nd < d[v]) { d[v] = nd; if (w[e] == 0) q.addFirst(v); else q.addLast(v); }
                        m.worked(1);
                    }
                }
                return d;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<UndirectedWeightedEdges, Long> primMst() {
        return donor("graph.prim-mst.edge-list", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.PRIM_MST, "O(V^2+E)", "O(V^2)", (in, supplied) -> {
            int n = in.vertices(); if (n == 0) return 0L;
            long[][] g = new long[n][n]; for (long[] row : g) Arrays.fill(row, Long.MAX_VALUE);
            int[] a = in.a(), b = in.b(); long[] w = in.weight(); for (int e = 0; e < a.length; e++) { g[a[e]][b[e]] = Math.min(g[a[e]][b[e]], w[e]); g[b[e]][a[e]] = Math.min(g[b[e]][a[e]], w[e]); }
            long[] key = new long[n]; Arrays.fill(key, Long.MAX_VALUE); key[0] = 0; byte[] used = new byte[n]; long total = 0;
            IProgressMonitor m = start(supplied, "prim-mst", Math.max(1L, (long)n*n));
            try {
                for (int step = 0; step < n; step++) {
                    m.checkCanceled(); int u = -1; for (int v = 0; v < n; v++) if (used[v] == 0 && (u < 0 || key[v] < key[u])) u = v;
                    if (u < 0 || key[u] == Long.MAX_VALUE) throw new IllegalArgumentException("graph disconnected");
                    used[u] = 1; total = safeAddSigned(total, key[u]);
                    for (int v = 0; v < n; v++) { if (used[v] == 0 && g[u][v] < key[v]) key[v] = g[u][v]; m.worked(1); }
                }
                return total;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<UndirectedWeightedEdges, Long> kruskalMst() {
        return donor("graph.kruskal-mst.edge-list", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.KRUSKAL_MST, "O(E log E)", "O(V+E)", (in, supplied) -> {
            int n = in.vertices(); int[] a = in.a(), b = in.b(); long[] w = in.weight(); sortEdges(a,b,w);
            int[] p = new int[n], rank = new int[n]; for (int i = 0; i < n; i++) p[i] = i; int chosen = 0; long total = 0;
            IProgressMonitor m = start(supplied, "kruskal-mst", Math.max(1, a.length));
            try {
                for (int e = 0; e < a.length && chosen < n - 1; e++) {
                    m.checkCanceled(); int x = find(p,a[e]), y = find(p,b[e]);
                    if (x != y) { unionRoots(p,rank,x,y); total = safeAddSigned(total,w[e]); chosen++; }
                    m.worked(1);
                }
                if (n > 0 && chosen != n - 1) throw new IllegalArgumentException("graph disconnected");
                return total;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<Csr, int[]> stronglyConnectedComponents() {
        return donor("graph.scc.tarjan.csr", AlgorithmPurpose.TRAVERSE, AlgorithmShape.STRONGLY_CONNECTED_COMPONENTS, "O(V+E)", "O(V)", (in, supplied) -> {
            int n = in.offsets().length - 1; int[] off = in.offsets(), edges = in.edges(), index = new int[n], low = new int[n], stack = new int[n], comp = new int[n];
            Arrays.fill(index,-1); Arrays.fill(comp,-1); byte[] on = new byte[n]; int[] state = {0,0,n-1};
            IProgressMonitor m = start(supplied, "tarjan-scc", Math.max(1, n + edges.length));
            try {
                for (int v = 0; v < n; v++) if (index[v] < 0) tarjan(v,off,edges,index,low,stack,on,comp,state,m);
                return comp;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<Csr, int[][]> bridges() {
        return donor("graph.bridges.tarjan.csr-undirected", AlgorithmPurpose.REACHABILITY, AlgorithmShape.BRIDGES_ARTICULATION, "O(V+E)", "O(V+E)", (in, supplied) -> {
            int n = in.offsets().length - 1; int[] off = in.offsets(), edges = in.edges(), disc = new int[n], low = new int[n]; Arrays.fill(disc,-1);
            List<int[]> out = new ArrayList<>(); int[] time = {0};
            IProgressMonitor m = start(supplied, "bridges", Math.max(1, n + edges.length));
            try {
                for (int v = 0; v < n; v++) if (disc[v] < 0) bridgeDfs(v,-1,off,edges,disc,low,time,out,m);
                return out.toArray(int[][]::new);
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<DirectedEdges, int[]> eulerianPath() {
        return donor("graph.eulerian-path.directed", AlgorithmPurpose.TRAVERSE, AlgorithmShape.EULERIAN_PATH, "O(V+E)", "O(V+E)", (in, supplied) -> {
            int n = in.vertices(); int[] from = in.from(), to = in.to(), inDeg = new int[n], outDeg = new int[n], head = new int[n], next = new int[from.length]; Arrays.fill(head,-1);
            for (int e = 0; e < from.length; e++) { next[e] = head[from[e]]; head[from[e]] = e; outDeg[from[e]]++; inDeg[to[e]]++; }
            int start = -1, plus = 0, minus = 0;
            for (int v = 0; v < n; v++) {
                int d = outDeg[v]-inDeg[v]; if (d == 1) { start=v; plus++; } else if (d == -1) minus++; else if (d != 0) throw new IllegalArgumentException("not Eulerian");
                if (start < 0 && outDeg[v] > 0) start = v;
            }
            if (!((plus==1&&minus==1)||(plus==0&&minus==0))) throw new IllegalArgumentException("not Eulerian");
            if (from.length == 0) return n == 0 ? new int[0] : new int[]{0};
            int[] stack = new int[from.length+1], path = new int[from.length+1]; int sp=0, pp=0; stack[sp++]=start;
            IProgressMonitor m = start(supplied, "eulerian-path", Math.max(1, from.length));
            try {
                while (sp>0) {
                    m.checkCanceled(); int v=stack[sp-1], e=head[v];
                    if (e>=0) { head[v]=next[e]; stack[sp++]=to[e]; m.worked(1); }
                    else path[pp++]=stack[--sp];
                }
                if (pp != from.length+1) throw new IllegalArgumentException("edges are disconnected");
                for (int i=0,j=pp-1;i<j;i++,j--) { int t=path[i]; path[i]=path[j]; path[j]=t; }
                return path;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<FlowNetwork, long[]> minCostMaxFlow() {
        return donor("graph.min-cost-max-flow.spfa", AlgorithmPurpose.FAN_IN, AlgorithmShape.MIN_COST_MAX_FLOW, "O(flow * V * E)", "O(V+E)", (in, supplied) -> {
            int n=in.vertices(), m0=in.from().length, m=m0<<1; int[] head=new int[n],to=new int[m],next=new int[m]; long[] cap=new long[m],cost=new long[m]; Arrays.fill(head,-1);
            int[] f=in.from(), t=in.to(); long[] c=in.capacity(), co=in.cost();
            for(int i=0,e=0;i<m0;i++){to[e]=t[i];cap[e]=c[i];cost[e]=co[i];next[e]=head[f[i]];head[f[i]]=e++;to[e]=f[i];cap[e]=0;cost[e]=-co[i];next[e]=head[t[i]];head[t[i]]=e++;}
            long flow=0,total=0; long[] dist=new long[n]; int[] prev=new int[n]; byte[] queued=new byte[n];
            IProgressMonitor monitor=start(supplied,"min-cost-max-flow",IProgressMonitor.UNKNOWN);
            try {
                while(true){
                    monitor.checkCanceled(); Arrays.fill(dist,Long.MAX_VALUE); Arrays.fill(prev,-1); Arrays.fill(queued,(byte)0);
                    ArrayDeque<Integer> q=new ArrayDeque<>(); dist[in.source()]=0;q.add(in.source());queued[in.source()]=1;
                    while(!q.isEmpty()){int u=q.removeFirst();queued[u]=0;for(int e=head[u];e>=0;e=next[e]){if(cap[e]>0){long nd=safeAddSigned(dist[u],cost[e]);if(dist[u]!=Long.MAX_VALUE&&nd<dist[to[e]]){dist[to[e]]=nd;prev[to[e]]=e;if(queued[to[e]]==0){queued[to[e]]=1;q.addLast(to[e]);}}}monitor.worked(1);}}
                    if(prev[in.sink()]<0) return new long[]{flow,total};
                    long add=Long.MAX_VALUE;for(int v=in.sink();v!=in.source();){int e=prev[v];add=Math.min(add,cap[e]);v=to[e^1];}
                    for(int v=in.sink();v!=in.source();){int e=prev[v];cap[e]-=add;cap[e^1]+=add;total=safeAddSigned(total,Math.multiplyExact(add,cost[e]));v=to[e^1];}
                    flow=Math.addExact(flow,add);
                }
            } finally { monitor.done(); }
        });
    }

    public static List<AlgorithmDescriptor> descriptors() {
        return List.of(aStar().descriptor(), bellmanFord().descriptor(), floydWarshall().descriptor(), zeroOneBfs().descriptor(),
                primMst().descriptor(), kruskalMst().descriptor(), stronglyConnectedComponents().descriptor(), bridges().descriptor(),
                eulerianPath().descriptor(), minCostMaxFlow().descriptor());
    }

    private static <I,O> CanonicalAlgorithmDonors.Donor<I,O> donor(String id, AlgorithmPurpose purpose, AlgorithmShape shape, String time, String space, ProgressAlgorithm<I,O> algorithm) {
        return new CanonicalAlgorithmDonors.Donor<>(new AlgorithmDescriptor(id,purpose,shape,
                EnumSet.of(PatternView.STRATEGY,PatternView.TEMPLATE_METHOD,PatternView.DAG_NODE),time,space,true,false,true,false),algorithm);
    }
    private static IProgressMonitor start(IProgressMonitor supplied,String name,long total){IProgressMonitor m=ProgressMonitors.nonNull(supplied);m.beginTask(name,total);m.checkCanceled();return m;}
    private static int[] cloneOf(int[] a){return Objects.requireNonNull(a,"array").clone();}
    private static long[] cloneOf(long[] a){return Objects.requireNonNull(a,"array").clone();}
    private static void validateCsr(int[] off,int[] edges){if(off.length<1||off[0]!=0||off[off.length-1]!=edges.length)throw new IllegalArgumentException("csr");for(int i=1;i<off.length;i++)if(off[i]<off[i-1])throw new IllegalArgumentException("csr");int n=off.length-1;for(int e:edges)if(e<0||e>=n)throw new IllegalArgumentException("edge");}
    private static long safeAdd(long a,long b){if(a==Long.MAX_VALUE)return Long.MAX_VALUE;if(b>0&&a>Long.MAX_VALUE-b)return Long.MAX_VALUE;if(b<0&&a<Long.MIN_VALUE-b)return Long.MIN_VALUE;return a+b;}
    private static long safeAddSigned(long a,long b){return safeAdd(a,b);}
    private static int[] heads(int n,int[] from){int[] head=new int[n];Arrays.fill(head,-1);for(int e=0;e<from.length;e++)head[from[e]]=e;return head;}
    private static int[] nextBySource(int n,int[] from){int[] head=new int[n],next=new int[from.length];Arrays.fill(head,-1);for(int e=0;e<from.length;e++){next[e]=head[from[e]];head[from[e]]=e;}return next;}
    private static int find(int[] p,int x){while(p[x]!=x){p[x]=p[p[x]];x=p[x];}return x;}
    private static void unionRoots(int[] p,int[] rank,int a,int b){if(rank[a]<rank[b])p[a]=b;else if(rank[a]>rank[b])p[b]=a;else{p[b]=a;rank[a]++;}}
    private static void sortEdges(int[] a,int[] b,long[] w){if(w.length<2)return;quickEdges(a,b,w,0,w.length-1);}
    private static void quickEdges(int[] a,int[] b,long[] w,int lo,int hi){while(lo<hi){long pivot=w[(lo+hi)>>>1];int i=lo,j=hi;while(i<=j){while(w[i]<pivot)i++;while(w[j]>pivot)j--;if(i<=j){long tw=w[i];w[i]=w[j];w[j]=tw;int ta=a[i];a[i]=a[j];a[j]=ta;int tb=b[i];b[i]=b[j];b[j]=tb;i++;j--;}}if(j-lo<hi-i){if(lo<j)quickEdges(a,b,w,lo,j);lo=i;}else{if(i<hi)quickEdges(a,b,w,i,hi);hi=j;}}}
    private static void tarjan(int v,int[] off,int[] edges,int[] index,int[] low,int[] stack,byte[] on,int[] comp,int[] state,IProgressMonitor m){
        // Explicit DFS frames preserve recursive Tarjan's adjacency order and component IDs.
        // Grow per DFS tree, so disconnected singleton roots do not allocate O(V) each.
        int capacity = Math.min(16, index.length);
        int[] vertices = new int[capacity], cursor = new int[capacity];
        int depth = 0;
        ProgressMonitors.checkCanceled(m);
        index[v] = low[v] = state[0]++;
        stack[state[1]++] = v;
        on[v] = 1;
        m.worked(1);
        vertices[0] = v;
        cursor[0] = off[v];
        while (depth >= 0) {
            ProgressMonitors.checkCanceled(m);
            int current = vertices[depth];
            if (cursor[depth] < off[current + 1]) {
                int next = edges[cursor[depth]++];
                if (index[next] < 0) {
                    if (depth + 1 == vertices.length) {
                        int grown = (int) Math.min(index.length, 2L * vertices.length);
                        vertices = Arrays.copyOf(vertices, grown);
                        cursor = Arrays.copyOf(cursor, grown);
                    }
                    index[next] = low[next] = state[0]++;
                    stack[state[1]++] = next;
                    on[next] = 1;
                    m.worked(1);
                    vertices[++depth] = next;
                    cursor[depth] = off[next];
                } else {
                    if (on[next] != 0) low[current] = Math.min(low[current], index[next]);
                    m.worked(1);
                }
            } else {
                if (low[current] == index[current]) {
                    int member;
                    do {
                        ProgressMonitors.checkCanceled(m);
                        member = stack[--state[1]];
                        on[member] = 0;
                        comp[member] = state[2];
                    } while (member != current);
                    state[2]--;
                }
                depth--;
                if (depth >= 0) {
                    int parent = vertices[depth];
                    low[parent] = Math.min(low[parent], low[current]);
                    m.worked(1);
                }
            }
        }
    }
    private static void bridgeDfs(int v,int parent,int[] off,int[] edges,int[] disc,int[] low,int[] time,List<int[]> out,IProgressMonitor m){
        // Resume each parent after its child, preserving the recursive postorder output.
        // A frame skips only the first parent occurrence; parallel arcs remain back edges.
        int capacity = Math.min(16, disc.length);
        int[] vertices = new int[capacity], parents = new int[capacity], cursor = new int[capacity];
        byte[] skippedParent = new byte[capacity];
        int depth = 0;
        ProgressMonitors.checkCanceled(m);
        disc[v] = low[v] = time[0]++;
        vertices[0] = v;
        parents[0] = parent;
        cursor[0] = off[v];
        while (depth >= 0) {
            ProgressMonitors.checkCanceled(m);
            int current = vertices[depth];
            if (cursor[depth] < off[current + 1]) {
                int next = edges[cursor[depth]++];
                if (next == parents[depth] && skippedParent[depth] == 0) {
                    skippedParent[depth] = 1;
                    continue;
                }
                if (disc[next] < 0) {
                    if (depth + 1 == vertices.length) {
                        int grown = (int) Math.min(disc.length, 2L * vertices.length);
                        vertices = Arrays.copyOf(vertices, grown);
                        parents = Arrays.copyOf(parents, grown);
                        cursor = Arrays.copyOf(cursor, grown);
                        skippedParent = Arrays.copyOf(skippedParent, grown);
                    }
                    disc[next] = low[next] = time[0]++;
                    vertices[++depth] = next;
                    parents[depth] = current;
                    cursor[depth] = off[next];
                    skippedParent[depth] = 0;
                } else {
                    low[current] = Math.min(low[current], disc[next]);
                    m.worked(1);
                }
            } else {
                depth--;
                if (depth >= 0) {
                    int previous = vertices[depth];
                    low[previous] = Math.min(low[previous], low[current]);
                    if (low[current] > disc[previous]) out.add(new int[]{previous, current});
                    m.worked(1);
                }
            }
        }
    }
}
