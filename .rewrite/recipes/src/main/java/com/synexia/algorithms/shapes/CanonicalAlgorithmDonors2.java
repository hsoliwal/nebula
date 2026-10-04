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

/** Executable representatives for the remaining canonical computational shapes. */
public final class CanonicalAlgorithmDonors2 {

    private CanonicalAlgorithmDonors2() {}

    public record Donor<I,O>(AlgorithmDescriptor descriptor, ProgressAlgorithm<I,O> algorithm)
            implements ProgressAlgorithm<I,O> {
        public Donor {
            Objects.requireNonNull(descriptor);
            Objects.requireNonNull(algorithm);
        }
        @Override public O execute(I input, IProgressMonitor monitor) {
            return algorithm.execute(input, ProgressMonitors.nonNull(monitor));
        }
    }

    public record LongKey(long[] values,long key) {
        public LongKey { values=Objects.requireNonNull(values).clone(); }
        @Override public long[] values(){return values.clone();}
    }
    public record LongPair(long[] left,long[] right) {
        public LongPair { left=Objects.requireNonNull(left).clone(); right=Objects.requireNonNull(right).clone(); }
        @Override public long[] left(){return left.clone();}
        @Override public long[] right(){return right.clone();}
    }
    public record LongPivot(long[] values,long pivot) {
        public LongPivot { values=Objects.requireNonNull(values).clone(); }
        @Override public long[] values(){return values.clone();}
    }
    public record EdgeList(int vertices,int[] from,int[] to) {
        public EdgeList {
            if(vertices<0)throw new IllegalArgumentException();
            from=Objects.requireNonNull(from).clone();to=Objects.requireNonNull(to).clone();
            if(from.length!=to.length)throw new IllegalArgumentException();
            for(int x:from)if(x<0||x>=vertices)throw new IllegalArgumentException();
            for(int x:to)if(x<0||x>=vertices)throw new IllegalArgumentException();
        }
        @Override public int[] from(){return from.clone();}
        @Override public int[] to(){return to.clone();}
    }
    public record CapacityMatrix(long[][] capacity,int source,int sink) {
        public CapacityMatrix {
            capacity=copy(capacity);
            int n=capacity.length;
            for(long[] row:capacity)if(row.length!=n)throw new IllegalArgumentException("square");
            if(source<0||source>=n||sink<0||sink>=n)throw new IllegalArgumentException("vertex");
            for(long[] row:capacity)for(long x:row)if(x<0)throw new IllegalArgumentException("capacity");
        }
        @Override public long[][] capacity(){return copy(capacity);}
    }
    public record HeuristicGraph(int[] offsets,int[] edges,long[] score,int source,int target) {
        public HeuristicGraph {
            offsets=Objects.requireNonNull(offsets).clone();edges=Objects.requireNonNull(edges).clone();score=Objects.requireNonNull(score).clone();
            if(offsets.length<2||offsets[0]!=0||offsets[offsets.length-1]!=edges.length||score.length!=offsets.length-1)throw new IllegalArgumentException();
            int n=score.length;if(source<0||source>=n||target<0||target>=n)throw new IllegalArgumentException();
            for(int e:edges)if(e<0||e>=n)throw new IllegalArgumentException();
        }
        @Override public int[] offsets(){return offsets.clone();}
        @Override public int[] edges(){return edges.clone();}
        @Override public long[] score(){return score.clone();}
    }
    public record IntervalSet(long[] starts,long[] ends) {
        public IntervalSet {
            starts=Objects.requireNonNull(starts).clone();ends=Objects.requireNonNull(ends).clone();
            if(starts.length!=ends.length)throw new IllegalArgumentException();
            for(int i=0;i<starts.length;i++)if(ends[i]<starts[i])throw new IllegalArgumentException();
        }
        @Override public long[] starts(){return starts.clone();}
        @Override public long[] ends(){return ends.clone();}
    }
    public record IntervalPair(IntervalSet left,IntervalSet right) {
        public IntervalPair { Objects.requireNonNull(left);Objects.requireNonNull(right); }
    }
    public record IntervalPoint(IntervalSet intervals,long point) {
        public IntervalPoint { Objects.requireNonNull(intervals); }
    }
    public record CostMatrix(long[][] costs) {
        public CostMatrix {
            costs=copy(costs);
            int n=costs.length;
            for(long[] row:costs)if(row.length!=n)throw new IllegalArgumentException("square");
        }
        @Override public long[][] costs(){return copy(costs);}
    }

    public static Donor<LongKey,Integer> gallopingSearch(){
        return donor("search.galloping.long",AlgorithmPurpose.FIND_ONE,AlgorithmShape.GALLOPING_SEARCH,"O(log i)","O(1)",(in,s)->{
            long[] a=in.values();IProgressMonitor m=start(s,"galloping-search",Math.max(1,a.length));
            try{
                if(a.length==0)return -1;
                int lo=0;long step=1L;
                while((long)lo+step<a.length){int probe=(int)((long)lo+step);if(a[probe]>=in.key())break;m.checkCanceled();lo=probe;step<<=1;m.worked(1);}
                int hi=(int)Math.min((long)a.length-1,(long)lo+step);
                while(lo<=hi){m.checkCanceled();int mid=(lo+hi)>>>1;long v=a[mid];m.worked(1);if(v<in.key())lo=mid+1;else if(v>in.key())hi=mid-1;else return mid;}
                return -(lo+1);
            }finally{m.done();}
        });
    }

    public static Donor<LongKey,Integer> jumpSearch(){
        return donor("search.jump.long",AlgorithmPurpose.FIND_ONE,AlgorithmShape.JUMP_SEARCH,"O(sqrt n)","O(1)",(in,s)->{
            long[] a=in.values();IProgressMonitor m=start(s,"jump-search",Math.max(1,a.length));
            try{
                if(a.length==0)return -1;
                int block=Math.max(1,(int)Math.sqrt(a.length));
                int start=0,end=(int)Math.min((long)a.length,(long)block);
                while(end<a.length&&a[end-1]<in.key()){
                    m.checkCanceled();m.worked(1);start=end;end=(int)Math.min((long)a.length,(long)start+block);
                }
                int found=-1;
                for(int i=start;i<end&&a[i]<=in.key();i++){
                    m.checkCanceled();m.worked(1);
                    if(a[i]==in.key()){found=i;break;}
                }
                return exactResult(a,in.key());
            }finally{m.done();}
        });
    }

    public static Donor<LongKey,Integer> fibonacciSearch(){
        return donor("search.fibonacci.long",AlgorithmPurpose.FIND_ONE,AlgorithmShape.FIBONACCI_SEARCH,"O(log n)","O(1)",(in,s)->{
            long[] a=in.values();IProgressMonitor m=start(s,"fibonacci-search",Math.max(1,a.length));
            try{
                int n=a.length;if(n==0)return -1;
                long f2=0,f1=1,f=1;
                while(f<n){m.checkCanceled();f2=f1;f1=f;f=Math.addExact(f2,f1);m.worked(1);}
                int offset=-1,found=-1;
                while(f>1){
                    m.checkCanceled();m.worked(1);
                    long candidate=(long)offset+f2;
                    int i=(int)Math.min(candidate,(long)n-1);
                    long current=a[i];
                    if(current<in.key()){f=f1;f1=f2;f2=f-f1;offset=i;}
                    else if(current>in.key()){f=f2;f1-=f2;f2=f-f1;}
                    else{found=i;break;}
                }
                int last=offset+1;
                if(found<0&&f1==1&&last<n&&a[last]==in.key())found=last;
                return exactResult(a,in.key());
            }finally{m.done();}
        });
    }

    public static Donor<LongKey,Integer> ternarySearch(){
        return donor("search.ternary.long",AlgorithmPurpose.FIND_ONE,AlgorithmShape.TERNARY_SEARCH,"O(log n)","O(1)",(in,s)->{
            long[] a=in.values();IProgressMonitor m=start(s,"ternary-search",Math.max(1,a.length));
            try{
                int low=0,high=a.length-1;
                while(low<=high){
                    m.checkCanceled();m.worked(1);
                    int third=(high-low)/3;
                    int left=low+third;
                    int right=high-third;
                    long lv=a[left],rv=a[right];
                    if(lv==in.key()||rv==in.key())break;
                    if(in.key()<lv)high=left-1;
                    else if(in.key()>rv)low=right+1;
                    else{low=left+1;high=right-1;}
                }
                return exactResult(a,in.key());
            }finally{m.done();}
        });
    }

    public static Donor<LongPivot,long[]> partitionWalk(){
        return donor("partition.walk.long",AlgorithmPurpose.PARTITIONING,AlgorithmShape.PARTITION_WALK,"O(n)","O(n) clone",(in,s)->{
            long[] a=in.values();int w=0;IProgressMonitor m=start(s,"partition-walk",Math.max(1,a.length));
            try{
                for(int i=0;i<a.length;i++){m.checkCanceled();if(a[i]<in.pivot()){long t=a[w];a[w]=a[i];a[i]=t;w++;}m.worked(1);}
                return a;
            }finally{m.done();}
        });
    }

    public static Donor<LongPair,long[]> mergeWalkIntersection(){
        return donor("merge.walk.intersection.long",AlgorithmPurpose.MERGE,AlgorithmShape.MERGE_WALK,"O(n+m)","O(min(n,m))",(in,s)->{
            long[] a=in.left(),b=in.right(),out=new long[Math.min(a.length,b.length)];int i=0,j=0,k=0;
            IProgressMonitor m=start(s,"merge-walk",Math.max(1,a.length+b.length));
            try{
                while(i<a.length&&j<b.length){m.checkCanceled();if(a[i]<b[j])i++;else if(a[i]>b[j])j++;else{out[k++]=a[i];i++;j++;}m.worked(1);}
                return Arrays.copyOf(out,k);
            }finally{m.done();}
        });
    }

    public static Donor<HeuristicGraph,int[]> bestFirst(){
        return donor("graph.best-first.heuristic-int",AlgorithmPurpose.REACHABILITY,AlgorithmShape.BEST_FIRST,"O((V+E) log V)","O(V)",(in,s)->{
            int[] off=in.offsets(),edges=in.edges();long[] score=in.score();byte[] seen=new byte[score.length];int[] parent=new int[score.length];Arrays.fill(parent,-1);
            PriorityQueue<Integer> q=new PriorityQueue<>((a,b)->Long.compare(score[a],score[b]));q.add(in.source());
            IProgressMonitor m=start(s,"best-first",Math.max(1,score.length+edges.length));
            try{
                while(!q.isEmpty()){m.checkCanceled();int v=q.remove();if(seen[v]!=0)continue;seen[v]=1;m.worked(1);if(v==in.target())break;
                    for(int i=off[v];i<off[v+1];i++){int x=edges[i];if(seen[x]==0){if(parent[x]<0)parent[x]=v;q.add(x);}m.worked(1);}}
                if(seen[in.target()]==0)return new int[0];
                int len=1;for(int v=in.target();v!=in.source();v=parent[v])len++;
                int[] path=new int[len];int p=len-1;for(int v=in.target();;v=parent[v]){path[p--]=v;if(v==in.source())break;}return path;
            }finally{m.done();}
        });
    }

    public static Donor<EdgeList,int[]> unionFind(){
        return donor("graph.union-find.int",AlgorithmPurpose.TRAVERSE,AlgorithmShape.UNION_FIND,"O((V+E) alpha(V))","O(V)",(in,s)->{
            int[] p=new int[in.vertices()],rank=new int[in.vertices()];for(int i=0;i<p.length;i++)p[i]=i;int[] f=in.from(),t=in.to();
            IProgressMonitor m=start(s,"union-find",Math.max(1,f.length+p.length));
            try{
                for(int i=0;i<f.length;i++){m.checkCanceled();union(p,rank,f[i],t[i]);m.worked(1);}
                for(int i=0;i<p.length;i++){p[i]=find(p,i);m.worked(1);}return p;
            }finally{m.done();}
        });
    }

    public static Donor<CapacityMatrix,Long> maxFlow(){
        return donor("graph.max-flow.edmonds-karp.long",AlgorithmPurpose.FAN_IN,AlgorithmShape.MAX_FLOW,"O(VE^2)","O(V^2)",(in,s)->{
            long[][] r=in.capacity();int n=r.length;long flow=0;int[] parent=new int[n],q=new int[n];
            IProgressMonitor m=start(s,"max-flow",IProgressMonitor.UNKNOWN);
            try{
                while(true){m.checkCanceled();Arrays.fill(parent,-1);parent[in.source()]=in.source();int h=0,t=0;q[t++]=in.source();
                    while(h<t&&parent[in.sink()]<0){int v=q[h++];for(int x=0;x<n;x++){if(parent[x]<0&&r[v][x]>0){parent[x]=v;q[t++]=x;if(x==in.sink())break;}m.worked(1);}}
                    if(parent[in.sink()]<0)return flow;
                    long add=Long.MAX_VALUE;for(int v=in.sink();v!=in.source();v=parent[v])add=Math.min(add,r[parent[v]][v]);
                    for(int v=in.sink();v!=in.source();v=parent[v]){int u=parent[v];r[u][v]-=add;r[v][u]+=add;}flow+=add;
                }
            }finally{m.done();}
        });
    }

    public static Donor<long[],long[]> differenceScan(){
        return donor("scan.difference.long",AlgorithmPurpose.DIFFERENCE,AlgorithmShape.DIFFERENCE_SCAN,"O(n)","O(n)",(a,s)->{
            Objects.requireNonNull(a);long[] out=new long[a.length];IProgressMonitor m=start(s,"difference-scan",Math.max(1,a.length));
            try{for(int i=0;i<a.length;i++){m.checkCanceled();out[i]=a[i]-(i==0?0:a[i-1]);m.worked(1);}return out;}finally{m.done();}
        });
    }

    public static Donor<LongPair,Boolean> subsequence(){
        return donor("sequence.subsequence.long",AlgorithmPurpose.SEQUENCE_MATCH,AlgorithmShape.SUBSEQUENCE,"O(n+m)","O(1)",(in,s)->{
            long[] needle=in.left(),hay=in.right();int i=0;IProgressMonitor m=start(s,"subsequence",Math.max(1,hay.length));
            try{for(long x:hay){m.checkCanceled();if(i<needle.length&&needle[i]==x)i++;m.worked(1);}return i==needle.length;}finally{m.done();}
        });
    }

    public static Donor<Integer,Long> memoizedFibonacci(){
        return donor("dp.memo.fibonacci.long",AlgorithmPurpose.ACCUMULATION,AlgorithmShape.MEMOIZATION,"O(n)","O(n)",(n,s)->{
            if(n<0||n>92)throw new IllegalArgumentException("n");long[] memo=new long[n+2];Arrays.fill(memo,-1);memo[0]=0;memo[1]=1;
            IProgressMonitor m=start(s,"memoization",Math.max(1,n));
            try{return fib(n,memo,m);}finally{m.done();}
        });
    }

    public static Donor<Integer,Long> nQueensConstraintSearch(){
        return donor("constraint.nqueens.count",AlgorithmPurpose.SEQUENCE_MATCH,AlgorithmShape.CONSTRAINT_SEARCH,"O(n!)","O(n)",(n,s)->{
            if(n<0||n>32)throw new IllegalArgumentException("n");IProgressMonitor m=start(s,"constraint-search",IProgressMonitor.UNKNOWN);
            try{return queens(0,n,0L,0L,0L,m);}finally{m.done();}
        });
    }

    public static Donor<IntervalPair,long[][]> intervalJoin(){
        return donor("interval.join.long",AlgorithmPurpose.MERGE,AlgorithmShape.INTERVAL_JOIN,"O(n*m)","O(matches)",(in,s)->{
            long[] as=in.left().starts(),ae=in.left().ends(),bs=in.right().starts(),be=in.right().ends();List<long[]> out=new ArrayList<>();
            IProgressMonitor m=start(s,"interval-join",Math.max(1,(long)as.length*bs.length));
            try{for(int i=0;i<as.length;i++)for(int j=0;j<bs.length;j++){m.checkCanceled();long lo=Math.max(as[i],bs[j]),hi=Math.min(ae[i],be[j]);if(lo<=hi)out.add(new long[]{lo,hi});m.worked(1);}return out.toArray(long[][]::new);}finally{m.done();}
        });
    }

    public static Donor<IntervalPoint,Integer> intervalIndex(){
        return donor("interval.index.point-count.long",AlgorithmPurpose.MEMBERSHIP,AlgorithmShape.INTERVAL_INDEX,"O(n log n) build + O(log n) query","O(n)",(in,s)->{
            long[] starts=in.intervals().starts(),ends=in.intervals().ends();Arrays.sort(starts);Arrays.sort(ends);IProgressMonitor m=start(s,"interval-index",Math.max(1,starts.length));
            try{m.checkCanceled();int opened=upperBound(starts,in.point());int closed=lowerBound(ends,in.point());m.worked(starts.length);return opened-closed;}finally{m.done();}
        });
    }

    public static Donor<CostMatrix,int[]> assignment(){
        return donor("matching.assignment.hungarian-long",AlgorithmPurpose.FAN_IN,AlgorithmShape.ASSIGNMENT,"O(n^3)","O(n)",(in,s)->{
            long[][] a=in.costs();int n=a.length;long[] u=new long[n+1],v=new long[n+1];int[] p=new int[n+1],way=new int[n+1];
            IProgressMonitor m=start(s,"assignment",(long)Math.max(1,n)*Math.max(1,n));
            try{
                for(int i=1;i<=n;i++){p[0]=i;long[] min=new long[n+1];Arrays.fill(min,Long.MAX_VALUE);byte[] used=new byte[n+1];int j0=0;
                    do{m.checkCanceled();used[j0]=1;int i0=p[j0],j1=0;long delta=Long.MAX_VALUE;
                        for(int j=1;j<=n;j++)if(used[j]==0){long cur=a[i0-1][j-1]-u[i0]-v[j];if(cur<min[j]){min[j]=cur;way[j]=j0;}if(min[j]<delta){delta=min[j];j1=j;}m.worked(1);}
                        for(int j=0;j<=n;j++)if(used[j]!=0){u[p[j]]+=delta;v[j]-=delta;}else min[j]-=delta;j0=j1;
                    }while(p[j0]!=0);
                    do{int j1=way[j0];p[j0]=p[j1];j0=j1;}while(j0!=0);
                }
                int[] assignment=new int[n];Arrays.fill(assignment,-1);for(int j=1;j<=n;j++)if(p[j]>0)assignment[p[j]-1]=j-1;return assignment;
            }finally{m.done();}
        });
    }

    public static Donor<CostMatrix,int[]> rankedMatching(){
        return donor("matching.ranked.greedy-long",AlgorithmPurpose.FAN_IN,AlgorithmShape.RANKED_MATCHING,"O(n^2 log n)","O(n^2)",(in,s)->{
            long[][] c=in.costs();int n=c.length;record E(int l,int r,long c){}List<E> edges=new ArrayList<>(n*n);
            for(int i=0;i<n;i++)for(int j=0;j<n;j++)edges.add(new E(i,j,c[i][j]));edges.sort((x,y)->Long.compare(x.c(),y.c()));
            int[] out=new int[n];Arrays.fill(out,-1);byte[] used=new byte[n];IProgressMonitor m=start(s,"ranked-matching",Math.max(1,edges.size()));
            try{for(E e:edges){m.checkCanceled();if(out[e.l()]<0&&used[e.r()]==0){out[e.l()]=e.r();used[e.r()]=1;}m.worked(1);}return out;}finally{m.done();}
        });
    }

    public static List<AlgorithmDescriptor> descriptors(){
        return List.of(gallopingSearch().descriptor(),jumpSearch().descriptor(),
                fibonacciSearch().descriptor(),partitionWalk().descriptor(),mergeWalkIntersection().descriptor(),
                bestFirst().descriptor(),unionFind().descriptor(),maxFlow().descriptor(),differenceScan().descriptor(),
                subsequence().descriptor(),memoizedFibonacci().descriptor(),nQueensConstraintSearch().descriptor(),
                intervalJoin().descriptor(),intervalIndex().descriptor(),assignment().descriptor(),rankedMatching().descriptor());
    }

    private static <I,O> Donor<I,O> donor(String id,AlgorithmPurpose purpose,AlgorithmShape shape,String time,String space,ProgressAlgorithm<I,O> algorithm){
        return new Donor<>(new AlgorithmDescriptor(id,purpose,shape,EnumSet.of(PatternView.STRATEGY,PatternView.TEMPLATE_METHOD,PatternView.DAG_NODE),time,space,true,false,true,false),algorithm);
    }
    private static IProgressMonitor start(IProgressMonitor supplied,String name,long total){IProgressMonitor m=ProgressMonitors.nonNull(supplied);m.beginTask(name,total);m.checkCanceled();return m;}
    private static int find(int[] p,int x){while(p[x]!=x){p[x]=p[p[x]];x=p[x];}return x;}
    private static void union(int[] p,int[] rank,int a,int b){a=find(p,a);b=find(p,b);if(a==b)return;if(rank[a]<rank[b])p[a]=b;else if(rank[a]>rank[b])p[b]=a;else{p[b]=a;rank[a]++;}}
    private static long fib(int n,long[] memo,IProgressMonitor m){m.checkCanceled();if(memo[n]>=0)return memo[n];m.worked(1);return memo[n]=Math.addExact(fib(n-1,memo,m),fib(n-2,memo,m));}
    private static long queens(int row,int n,long cols,long d1,long d2,IProgressMonitor m){m.checkCanceled();m.worked(1);if(row==n)return 1;long mask=n==64?-1L:((1L<<n)-1),available=mask&~(cols|d1|d2),count=0;while(available!=0){long bit=available&-available;available-=bit;count+=queens(row+1,n,cols|bit,((d1|bit)<<1)&mask,(d2|bit)>>>1,m);}return count;}
    private static int exactResult(long[] a,long key){int insertion=lowerBound(a,key);return insertion<a.length&&a[insertion]==key?insertion:-(insertion+1);}
    private static int lowerBound(long[] a,long x){int l=0,r=a.length;while(l<r){int m=(l+r)>>>1;if(a[m]<x)l=m+1;else r=m;}return l;}
    private static int upperBound(long[] a,long x){int l=0,r=a.length;while(l<r){int m=(l+r)>>>1;if(a[m]<=x)l=m+1;else r=m;}return l;}
    private static long[][] copy(long[][] a){Objects.requireNonNull(a);long[][] out=new long[a.length][];for(int i=0;i<a.length;i++)out[i]=Objects.requireNonNull(a[i]).clone();return out;}
}
