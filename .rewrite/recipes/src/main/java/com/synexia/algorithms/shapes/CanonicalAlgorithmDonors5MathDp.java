// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/** Monitor-first DP, number-theory, bit and geometry donors deduplicated from direct Java corpora. */
public final class CanonicalAlgorithmDonors5MathDp {
    private CanonicalAlgorithmDonors5MathDp() {}

    public record StringPair(String left, String right) {
        public StringPair { Objects.requireNonNull(left,"left"); Objects.requireNonNull(right,"right"); }
    }

    public record CoinInput(int[] coins, int amount) {
        public CoinInput {
            coins=Objects.requireNonNull(coins,"coins").clone();
            if(amount<0)throw new IllegalArgumentException("amount");
            for(int c:coins)if(c<=0)throw new IllegalArgumentException("coin");
        }
        @Override public int[] coins(){return coins.clone();}
    }

    public record Knapsack(long[] weight,long[] value,int capacity) {
        public Knapsack {
            weight=Objects.requireNonNull(weight,"weight").clone(); value=Objects.requireNonNull(value,"value").clone();
            if(weight.length!=value.length||capacity<0)throw new IllegalArgumentException();
            for(long w:weight)if(w<0||w>Integer.MAX_VALUE)throw new IllegalArgumentException("weight");
        }
        @Override public long[] weight(){return weight.clone();}
        @Override public long[] value(){return value.clone();}
    }

    public record MatrixChain(int[] dimensions) {
        public MatrixChain {
            dimensions=Objects.requireNonNull(dimensions,"dimensions").clone();
            if(dimensions.length<2)throw new IllegalArgumentException("at least one matrix");
            for(int d:dimensions)if(d<=0)throw new IllegalArgumentException("dimension");
        }
        @Override public int[] dimensions(){return dimensions.clone();}
    }

    public record CostMatrix(long[][] cost) {
        public CostMatrix {
            Objects.requireNonNull(cost,"cost"); int n=cost.length; long[][] copy=new long[n][];
            for(int i=0;i<n;i++){copy[i]=Objects.requireNonNull(cost[i],"row").clone();if(copy[i].length!=n)throw new IllegalArgumentException("square");}
            cost=copy;
        }
        @Override public long[][] cost(){long[][] c=new long[cost.length][];for(int i=0;i<c.length;i++)c[i]=cost[i].clone();return c;}
    }

    public record DigitSum(long bound,int sum) {
        public DigitSum { if(bound<0||sum<0)throw new IllegalArgumentException(); }
    }

    public record LongPair(long a,long b) {}

    public record ModPow(long base,long exponent,long modulus) {
        public ModPow { if(exponent<0||modulus<=0)throw new IllegalArgumentException(); }
    }

    public record Crt(long[] residue,long[] modulus) {
        public Crt {
            residue=Objects.requireNonNull(residue,"residue").clone(); modulus=Objects.requireNonNull(modulus,"modulus").clone();
            if(residue.length==0||residue.length!=modulus.length)throw new IllegalArgumentException("length");
            for(long m:modulus)if(m<=0)throw new IllegalArgumentException("modulus");
        }
        @Override public long[] residue(){return residue.clone();}
        @Override public long[] modulus(){return modulus.clone();}
    }

    public record SubsetTarget(int[] values,int target) {
        public SubsetTarget {
            values=Objects.requireNonNull(values,"values").clone();
            if(target<0)throw new IllegalArgumentException("target");
            for(int v:values)if(v<0)throw new IllegalArgumentException("value");
        }
        @Override public int[] values(){return values.clone();}
    }

    public record ThreePoints(long ax,long ay,long bx,long by,long cx,long cy) {}

    public record Points(long[] x,long[] y) {
        public Points {
            x=Objects.requireNonNull(x,"x").clone(); y=Objects.requireNonNull(y,"y").clone();
            if(x.length!=y.length)throw new IllegalArgumentException("length");
        }
        @Override public long[] x(){return x.clone();}
        @Override public long[] y(){return y.clone();}
    }

    public static CanonicalAlgorithmDonors.Donor<StringPair,Integer> longestCommonSubsequence(){
        return donor("dp.lcs.string",AlgorithmPurpose.SEQUENCE_MATCH,AlgorithmShape.LONGEST_COMMON_SUBSEQUENCE,"O(nm)","O(min(n,m))",(in,s)->{
            String a=in.left(),b=in.right(); if(a.length()<b.length()){String t=a;a=b;b=t;} int[] dp=new int[b.length()+1];
            IProgressMonitor m=start(s,"lcs",Math.max(1L,(long)a.length()*Math.max(1,b.length())));
            try{for(int i=1;i<=a.length();i++){int diag=0;for(int j=1;j<=b.length();j++){m.checkCanceled();int old=dp[j];dp[j]=a.charAt(i-1)==b.charAt(j-1)?diag+1:Math.max(dp[j],dp[j-1]);diag=old;m.worked(1);}}return dp[b.length()];}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<CoinInput,Integer> coinChange(){
        return donor("dp.coin-change.min",AlgorithmPurpose.ACCUMULATION,AlgorithmShape.COIN_CHANGE,"O(amount*coins)","O(amount)",(in,s)->{
            int[] dp=new int[in.amount()+1];Arrays.fill(dp,in.amount()+1);dp[0]=0;int[] coins=in.coins();
            IProgressMonitor m=start(s,"coin-change",Math.max(1L,(long)Math.max(1,in.amount())*Math.max(1,coins.length)));
            try{for(int a=1;a<=in.amount();a++)for(int c:coins){m.checkCanceled();if(c<=a&&dp[a-c]<=in.amount())dp[a]=Math.min(dp[a],dp[a-c]+1);m.worked(1);}return dp[in.amount()]>in.amount()?-1:dp[in.amount()];}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<Knapsack,Long> zeroOneKnapsack(){
        return donor("dp.knapsack-01.long",AlgorithmPurpose.TOP_K,AlgorithmShape.KNAPSACK_01,"O(n*capacity)","O(capacity)",(in,s)->{
            long[] dp=new long[in.capacity()+1],w=in.weight(),val=in.value();
            IProgressMonitor m=start(s,"knapsack-01",Math.max(1L,(long)w.length*Math.max(1,in.capacity())));
            try{for(int i=0;i<w.length;i++){int wi=(int)w[i];for(int c=in.capacity();c>=wi;c--){m.checkCanceled();dp[c]=Math.max(dp[c],safeAdd(dp[c-wi],val[i]));m.worked(1);}}return dp[in.capacity()];}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<MatrixChain,Long> intervalDp(){
        return donor("dp.interval.matrix-chain",AlgorithmPurpose.ACCUMULATION,AlgorithmShape.INTERVAL_DP,"O(n^3)","O(n^2)",(in,s)->{
            int[] d=in.dimensions();int n=d.length-1;long[][] dp=new long[n][n];
            IProgressMonitor m=start(s,"interval-dp",Math.max(1L,(long)n*n*n));
            try{for(int len=2;len<=n;len++)for(int i=0;i+len<=n;i++){int j=i+len-1;dp[i][j]=Long.MAX_VALUE;for(int k=i;k<j;k++){m.checkCanceled();long mul=Math.multiplyExact(Math.multiplyExact((long)d[i],d[k+1]),d[j+1]);long cand=safeAdd(safeAdd(dp[i][k],dp[k+1][j]),mul);if(cand<dp[i][j])dp[i][j]=cand;m.worked(1);}}return n==0?0:dp[0][n-1];}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<CostMatrix,Long> bitmaskDp(){
        return donor("dp.bitmask.tsp-cycle",AlgorithmPurpose.SCHEDULING,AlgorithmShape.BITMASK_DP,"O(n^2*2^n)","O(n*2^n)",(in,s)->{
            long[][] c=in.cost();int n=c.length;if(n==0)return 0L;if(n>20)throw new IllegalArgumentException("n > 20");int states=1<<n;long[][] dp=new long[states][n];for(long[] row:dp)Arrays.fill(row,Long.MAX_VALUE);dp[1][0]=0;
            IProgressMonitor m=start(s,"bitmask-dp",IProgressMonitor.UNKNOWN);
            try{for(int mask=1;mask<states;mask++){if((mask&1)==0)continue;for(int u=0;u<n;u++)if((mask&(1<<u))!=0&&dp[mask][u]!=Long.MAX_VALUE){for(int v=0;v<n;v++)if((mask&(1<<v))==0){m.checkCanceled();int nm=mask|(1<<v);long cand=safeAdd(dp[mask][u],c[u][v]);if(cand<dp[nm][v])dp[nm][v]=cand;m.worked(1);}}}long best=n==1?0:Long.MAX_VALUE;int full=states-1;for(int u=1;u<n;u++)if(dp[full][u]!=Long.MAX_VALUE)best=Math.min(best,safeAdd(dp[full][u],c[u][0]));return best;}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<DigitSum,Long> digitDp(){
        return donor("dp.digit.count-by-sum",AlgorithmPurpose.ACCUMULATION,AlgorithmShape.DIGIT_DP,"O(digits*sum*10)","O(sum)",(in,s)->{
            char[] digits=Long.toString(in.bound()).toCharArray();int target=in.sum();long[][] loose=new long[2][target+1],tight=new long[2][target+1];tight[0][0]=1;int cur=0,next=1;
            IProgressMonitor m=start(s,"digit-dp",Math.max(1L,(long)digits.length*Math.max(1,target+1)*10));
            try{for(char ch:digits){Arrays.fill(loose[next],0);Arrays.fill(tight[next],0);int lim=ch-'0';for(int sum=0;sum<=target;sum++){for(int d=0;d<=9&&sum+d<=target;d++){m.checkCanceled();if(loose[cur][sum]!=0)loose[next][sum+d]=Math.addExact(loose[next][sum+d],loose[cur][sum]);m.worked(1);}if(tight[cur][sum]!=0)for(int d=0;d<=lim&&sum+d<=target;d++){if(d==lim)tight[next][sum+d]=Math.addExact(tight[next][sum+d],tight[cur][sum]);else loose[next][sum+d]=Math.addExact(loose[next][sum+d],tight[cur][sum]);}}int t=cur;cur=next;next=t;}return Math.addExact(loose[cur][target],tight[cur][target]);}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<LongPair,Long> gcdEuclid(){
        return donor("number.gcd.euclid.long",AlgorithmPurpose.ACCUMULATION,AlgorithmShape.GCD_EUCLID,"O(log min(a,b))","O(1)",(in,s)->{
            if(in.a()==Long.MIN_VALUE||in.b()==Long.MIN_VALUE)throw new IllegalArgumentException("Long.MIN_VALUE unsupported");long a=Math.abs(in.a()),b=Math.abs(in.b());IProgressMonitor m=start(s,"gcd",IProgressMonitor.UNKNOWN);
            try{while(b!=0){m.checkCanceled();long t=a%b;a=b;b=t;m.worked(1);}return a;}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<Integer,int[]> sieve(){
        return donor("number.sieve.eratosthenes",AlgorithmPurpose.FIND_ONE,AlgorithmShape.SIEVE,"O(n log log n)","O(n)",(n,s)->{
            if(n<0)throw new IllegalArgumentException("n");boolean[] composite=new boolean[n+1];IProgressMonitor m=start(s,"sieve",Math.max(1,n));
            try{for(int p=2;(long)p*p<=n;p++)if(!composite[p])for(int x=p*p;x<=n;x+=p){m.checkCanceled();composite[x]=true;m.worked(1);}int count=0;for(int i=2;i<=n;i++)if(!composite[i])count++;int[] primes=new int[count];for(int i=2,k=0;i<=n;i++)if(!composite[i])primes[k++]=i;return primes;}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<ModPow,Long> modularExponentiation(){
        return donor("number.mod-pow.long",AlgorithmPurpose.ACCUMULATION,AlgorithmShape.MODULAR_EXPONENTIATION,"O(log exponent)","O(1)",(in,s)->{
            long mod=in.modulus(),base=floorMod(in.base(),mod),e=in.exponent(),res=1%mod;IProgressMonitor m=start(s,"modular-exponentiation",IProgressMonitor.UNKNOWN);
            try{while(e>0){m.checkCanceled();if((e&1)!=0)res=mulMod(res,base,mod);base=mulMod(base,base,mod);e>>>=1;m.worked(1);}return res;}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<LongPair,long[]> extendedGcd(){
        return donor("number.extended-gcd.long",AlgorithmPurpose.ACCUMULATION,AlgorithmShape.EXTENDED_GCD,"O(log min(a,b))","O(1)",(in,s)->{
            long oldR=in.a(),r=in.b(),oldS=1,ss=0,oldT=0,tt=1;IProgressMonitor m=start(s,"extended-gcd",IProgressMonitor.UNKNOWN);
            try{while(r!=0){m.checkCanceled();long q=oldR/r;long nr=oldR-q*r;oldR=r;r=nr;long ns=oldS-q*ss;oldS=ss;ss=ns;long nt=oldT-q*tt;oldT=tt;tt=nt;m.worked(1);}if(oldR<0){oldR=-oldR;oldS=-oldS;oldT=-oldT;}return new long[]{oldR,oldS,oldT};}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<Crt,Long> chineseRemainder(){
        return donor("number.crt.pairwise-coprime",AlgorithmPurpose.ACCUMULATION,AlgorithmShape.CHINESE_REMAINDER,"O(k log M)","O(1)",(in,s)->{
            long x=0,mod=1;long[] r=in.residue(),modes=in.modulus();IProgressMonitor mon=start(s,"chinese-remainder",Math.max(1,r.length));
            try{for(int i=0;i<r.length;i++){mon.checkCanceled();long[] eg=egcd(mod,modes[i]);if(eg[0]!=1)throw new IllegalArgumentException("moduli must be pairwise coprime");long delta=floorMod(r[i]-floorMod(x,modes[i]),modes[i]);long k=mulMod(delta,floorMod(eg[1],modes[i]),modes[i]);x=Math.addExact(x,Math.multiplyExact(mod,k));mod=Math.multiplyExact(mod,modes[i]);x=floorMod(x,mod);mon.worked(1);}return x;}finally{mon.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<Long,Boolean> millerRabin(){
        return donor("number.miller-rabin.long",AlgorithmPurpose.MEMBERSHIP,AlgorithmShape.MILLER_RABIN,"O(log^3 n) fixed bases","O(1)",(n,s)->{
            Objects.requireNonNull(n,"n");IProgressMonitor m=start(s,"miller-rabin",IProgressMonitor.UNKNOWN);
            try{return isPrime64(n,m);}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[],Long> xorBasis(){
        return donor("bit.xor-linear-basis.long",AlgorithmPurpose.TOP_K,AlgorithmShape.XOR_BASIS,"O(64n)","O(64)",(values,s)->{
            Objects.requireNonNull(values,"values");long[] basis=new long[64];IProgressMonitor m=start(s,"xor-basis",Math.max(1L,64L*values.length));
            try{for(long original:values){long x=original;for(int bit=63;bit>=0&&x!=0;bit--){m.checkCanceled();if(((x>>>bit)&1)==0){m.worked(1);continue;}if(basis[bit]==0){basis[bit]=x;break;}x^=basis[bit];m.worked(1);}}long best=0;for(int bit=63;bit>=0;bit--)best=Math.max(best,best^basis[bit]);return best;}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<SubsetTarget,Boolean> bitsetDp(){
        return donor("bit.bitset-subset-sum",AlgorithmPurpose.MEMBERSHIP,AlgorithmShape.BITSET_DP,"O(n*target/64)","O(target/64)",(in,s)->{
            int words=(in.target()>>>6)+1;long[] bits=new long[words];bits[0]=1;int[] vals=in.values();IProgressMonitor m=start(s,"bitset-dp",Math.max(1L,(long)words*Math.max(1,vals.length)));
            try{for(int v:vals){int ws=v>>>6,bs=v&63;for(int i=words-1;i>=0;i--){m.checkCanceled();long shifted=0;int src=i-ws;if(src>=0){shifted=bits[src]<<bs;if(bs!=0&&src-1>=0)shifted|=bits[src-1]>>>(64-bs);}bits[i]|=shifted;m.worked(1);}}return (bits[in.target()>>>6]&(1L<<(in.target()&63)))!=0;}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<ThreePoints,Long> orientation(){
        return donor("geometry.orientation.long",AlgorithmPurpose.FIND_ONE,AlgorithmShape.ORIENTATION,"O(1)","O(1)",(p,s)->{
            IProgressMonitor m=start(s,"orientation",1);try{long abx=Math.subtractExact(p.bx(),p.ax()),aby=Math.subtractExact(p.by(),p.ay()),acx=Math.subtractExact(p.cx(),p.ax()),acy=Math.subtractExact(p.cy(),p.ay());long cross=Math.subtractExact(Math.multiplyExact(abx,acy),Math.multiplyExact(aby,acx));m.worked(1);return cross;}finally{m.done();}
        });
    }

    public static CanonicalAlgorithmDonors.Donor<Points,long[][]> convexHull(){
        return donor("geometry.convex-hull.monotonic-chain",AlgorithmPurpose.MAINTAIN_ORDER,AlgorithmShape.CONVEX_HULL,"O(n log n)","O(n)",(in,s)->{
            long[] x=in.x(),y=in.y();int n=x.length;if(n<=1)return n==0?new long[0][2]:new long[][]{{x[0],y[0]}};
            Integer[] order=new Integer[n];for(int i=0;i<n;i++)order[i]=i;Arrays.sort(order,(a,b)->{int c=Long.compare(x[a],x[b]);return c!=0?c:Long.compare(y[a],y[b]);});
            int[] hull=new int[n*2];int k=0;IProgressMonitor m=start(s,"convex-hull",Math.max(1,2L*n));
            try{for(int idx:order){m.checkCanceled();while(k>=2&&cross(x[hull[k-2]],y[hull[k-2]],x[hull[k-1]],y[hull[k-1]],x[idx],y[idx])<=0)k--;hull[k++]=idx;m.worked(1);}int lower=k;for(int oi=n-2;oi>=0;oi--){int idx=order[oi];m.checkCanceled();while(k>lower&&cross(x[hull[k-2]],y[hull[k-2]],x[hull[k-1]],y[hull[k-1]],x[idx],y[idx])<=0)k--;hull[k++]=idx;m.worked(1);}if(k>1)k--;long[][] out=new long[k][2];for(int i=0;i<k;i++){out[i][0]=x[hull[i]];out[i][1]=y[hull[i]];}return out;}finally{m.done();}
        });
    }

    public static List<AlgorithmDescriptor> descriptors(){
        return List.of(longestCommonSubsequence().descriptor(),coinChange().descriptor(),zeroOneKnapsack().descriptor(),intervalDp().descriptor(),bitmaskDp().descriptor(),digitDp().descriptor(),
                gcdEuclid().descriptor(),sieve().descriptor(),modularExponentiation().descriptor(),extendedGcd().descriptor(),chineseRemainder().descriptor(),millerRabin().descriptor(),
                xorBasis().descriptor(),bitsetDp().descriptor(),orientation().descriptor(),convexHull().descriptor());
    }

    private static <I,O> CanonicalAlgorithmDonors.Donor<I,O> donor(String id,AlgorithmPurpose purpose,AlgorithmShape shape,String time,String space,ProgressAlgorithm<I,O> algorithm){
        return new CanonicalAlgorithmDonors.Donor<>(new AlgorithmDescriptor(id,purpose,shape,EnumSet.of(PatternView.STRATEGY,PatternView.TEMPLATE_METHOD,PatternView.DAG_NODE),time,space,true,false,true,false),algorithm);
    }
    private static IProgressMonitor start(IProgressMonitor supplied,String name,long total){IProgressMonitor m=ProgressMonitors.nonNull(supplied);m.beginTask(name,total);m.checkCanceled();return m;}
    private static long safeAdd(long a,long b){if(b>0&&a>Long.MAX_VALUE-b)return Long.MAX_VALUE;if(b<0&&a<Long.MIN_VALUE-b)return Long.MIN_VALUE;return a+b;}
    private static long floorMod(long x,long m){long r=x%m;return r<0?r+m:r;}
    private static long mulMod(long a,long b,long mod){a=floorMod(a,mod);b=floorMod(b,mod);long res=0;while(b>0){if((b&1)!=0)res=addMod(res,a,mod);a=addMod(a,a,mod);b>>>=1;}return res;}
    private static long addMod(long a,long b,long mod){return a>=mod-b?a-(mod-b):a+b;}
    private static long[] egcd(long a,long b){long or=a,r=b,os=1,s=0,ot=0,t=1;while(r!=0){long q=or/r;long nr=or-q*r;or=r;r=nr;long ns=os-q*s;os=s;s=ns;long nt=ot-q*t;ot=t;t=nt;}if(or<0)return new long[]{-or,-os,-ot};return new long[]{or,os,ot};}
    private static boolean isPrime64(long n,IProgressMonitor m){if(n<2)return false;for(long p:new long[]{2,3,5,7,11,13,17,19,23,29,31,37}){if(n%p==0)return n==p;}long d=n-1;int s=0;while((d&1)==0){d>>>=1;s++;}long[] bases={2,325,9375,28178,450775,9780504,1795265022};for(long a:bases){m.checkCanceled();if(a%n==0)continue;long x=powMod(a,d,n);if(x==1||x==n-1){m.worked(1);continue;}boolean witness=true;for(int r=1;r<s;r++){x=mulMod(x,x,n);if(x==n-1){witness=false;break;}}if(witness)return false;m.worked(1);}return true;}
    private static long powMod(long a,long e,long mod){long r=1%mod;a=floorMod(a,mod);while(e>0){if((e&1)!=0)r=mulMod(r,a,mod);a=mulMod(a,a,mod);e>>>=1;}return r;}
    private static long cross(long ax,long ay,long bx,long by,long cx,long cy){return Math.subtractExact(Math.multiplyExact(Math.subtractExact(bx,ax),Math.subtractExact(cy,ay)),Math.multiplyExact(Math.subtractExact(by,ay),Math.subtractExact(cx,ax)));}
}
