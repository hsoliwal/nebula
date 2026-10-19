/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.grid;

import java.util.Arrays;

/** Tests the complete Grid row index and real heap helpers, without widget-shaped doubles. */
public final class GridRankPixelProof {
    private static long checks,digest=19;
    private GridRankPixelProof() { }
    private static void eq(long a,long b){checks++;digest=31*digest+b;if(a!=b)throw new AssertionError(a+" != "+b);}
    private static void verify(GridRowExtents g,int[] e) {
        long sum=0;int rank=0;
        eq(e.length,g.size());eq(0,g.extentOfRank(0));
        for(int i=0;i<e.length;i++) {
            eq(sum,g.extentBefore(i));eq(rank,g.rankOf(i));eq(e[i],g.extentAt(i));
            if(e[i]>0) {
                eq(i,g.positionOfRank(rank));eq(sum,g.extentOfRank(rank));eq(rank,g.rankAtExtent(sum));
                sum+=e[i];rank++;
            }
        }
        eq(rank,g.visibleCount());eq(sum,g.totalExtent());eq(sum,g.extentOfRank(rank));
        eq(rank,g.rankAtExtent(sum));eq(-1,g.rankAtExtent(-1));
        for(int r:new int[]{-1,-2,Integer.MIN_VALUE,Integer.MAX_VALUE,rank+1}) {
            long expected=r==0?0:r==g.visibleCount()?g.totalExtent():g.extentBefore(g.positionOfRank(r));
            eq(expected,g.extentOfRank(r));
        }
    }
    public static void main(String[] args) {
        for(int mask=0;mask<512;mask++) {
            int[] e=new int[9];for(int i=0;i<9;i++)e[i]=(mask&(1<<i))==0?0:(i%3==0?Integer.MAX_VALUE:i+1);
            var g=new GridRowExtents();g.rebuild(e.length,i->e[i]);verify(g,e);
            for(int i=0;i<e.length;i++){e[i]=e[i]==0?i+1:0;g.set(i,e[i]);verify(g,e);}
            g.invalidate();eq(0,g.isValid(e.length)?1:0);g.rebuild(e.length,i->e[i]);verify(g,e);
        }
        var g=new GridRowExtents();int[] model=new int[0];
        for(int n=0;n<1025;n++) {
            int v=n%5==0?0:n%17==0?Integer.MAX_VALUE:n%137+1;
            g.append(v);model=Arrays.copyOf(model,n+1);model[n]=v;
            if(n<64 || (n&63)==0 || n==1024)verify(g,model);
        }
        RuntimeException marker=new RuntimeException("extent callback");
        try{g.rebuild(4,i->{if(i==2)throw marker;return 7;});throw new AssertionError("lost failure");}
        catch(RuntimeException ex){if(ex!=marker)throw ex;}
        eq(0,g.isValid(1025)?1:0);g.rebuild(3,i->i==1?0:17);verify(g,new int[]{17,0,17});
        System.out.println("NEBULA_RANK_PIXEL_PASS checks="+checks+" digest="+Long.toUnsignedString(digest));
    }
}
