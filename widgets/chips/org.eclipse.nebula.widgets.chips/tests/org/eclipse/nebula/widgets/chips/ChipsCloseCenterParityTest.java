// SPDX-License-Identifier: EPL-2.0
// Differential atom oracle: no substitute SWT widget and no extra viewport.
package org.eclipse.nebula.widgets.chips;

import java.util.Random;
import java.util.Arrays;

public final class ChipsCloseCenterParityTest {
    static final int RADIUS=7;
    static long comparisons, oldPoints, newPoints, reentries, coldNpe, exceptions;
    static final class Point {int x,y;Point(int x,int y){this.x=x;this.y=y;}}
    static final class Painter {
        Point closeCenter;
        int[] strokes;
        boolean failAfterCenter;
        boolean failBeforeCenter;
        void paintOld(int x,int height) {
            if(failBeforeCenter) throw new IllegalStateException("before");
            closeCenter=new Point(x+4+RADIUS,(height-2*RADIUS)/2+RADIUS);
            oldPoints++;
            if(failAfterCenter) throw new IllegalStateException("after");
            strokes=lines(x,height,closeCenter);
        }
        void paintNew(int x,int height) {
            if(failBeforeCenter) throw new IllegalStateException("before");
            int cx=x+4+RADIUS,cy=(height-2*RADIUS)/2+RADIUS;
            if(closeCenter==null){closeCenter=new Point(cx,cy);newPoints++;}
            else {closeCenter.x=cx;closeCenter.y=cy;}
            if(failAfterCenter) throw new IllegalStateException("after");
            strokes=lines(x,height,closeCenter);
        }
        boolean mouseUp(int x,int y) {
            // Verbatim original int intermediate products; overflow is intentional.
            float dist=(float)Math.sqrt((x-closeCenter.x)*(x-closeCenter.x)
                 +(y-closeCenter.y)*(y-closeCenter.y));
            return dist < RADIUS;
        }
    }
    static int[] lines(int x,int height,Point center) {
        int top=(height-2*RADIUS)/2;
        return new int[]{x+4,top,2*RADIUS,2*RADIUS,
            center.x-3,center.y-3,center.x+3,center.y+3,
            center.x+3,center.y-3,center.x-3,center.y+3};
    }
    static void eq(boolean b,String s){comparisons++;if(!b)throw new AssertionError(s+" at "+comparisons);}
    static String run(Painter p,boolean original,int x,int height){
        try { if(original)p.paintOld(x,height);else p.paintNew(x,height);return "OK"; }
        catch(RuntimeException e){exceptions++;return e.getClass().getName()+":"+e.getMessage();}
    }
    static void compare(Painter old,Painter next,int x,int height,boolean before,boolean after,Random r) {
        old.failBeforeCenter=next.failBeforeCenter=before;
        old.failAfterCenter=next.failAfterCenter=after;
        String a=run(old,true,x,height),b=run(next,false,x,height);
        eq(a.equals(b),"exception/class/callback order");
        eq(old.closeCenter==null?next.closeCenter==null:
            next.closeCenter!=null && old.closeCenter.x==next.closeCenter.x
              &&old.closeCenter.y==next.closeCenter.y,"close center geometry/lifetime");
        eq(Arrays.equals(old.strokes,next.strokes),"native GC line/oval coordinates");
        if(!before && !after){
            for(int k=0;k<4;k++){
                int ex=k==0?old.closeCenter.x:k==1?old.closeCenter.x+RADIUS:r.nextInt();
                int ey=k==0?old.closeCenter.y:k==1?old.closeCenter.y:r.nextInt();
                eq(old.mouseUp(ex,ey)==next.mouseUp(ex,ey),"unchanged inclusive/exclusive circle hit");
            }
        }
    }
    public static void main(String[] args) {
        Random r=new Random(0x4348495053565031L);
        Painter old=new Painter(),next=new Painter();
        eq(old.closeCenter==null && next.closeCenter==null,"cold closeCenter is null");
        try {old.mouseUp(0,0);throw new AssertionError("old prepaint NPE expected");}
        catch(NullPointerException expected){coldNpe++;}
        try {next.mouseUp(0,0);throw new AssertionError("new prepaint NPE expected");}
        catch(NullPointerException expected){coldNpe++;}
        final int[] extreme={0,1,-1,7,14,Integer.MIN_VALUE,Integer.MAX_VALUE,0x55555555};
        for(int x:extreme)for(int h:extreme){compare(old,next,x,h,false,false,r);}
        for(int i=0;i<120000;i++) {
            int x=i%33==0?extreme[r.nextInt(extreme.length)]:r.nextInt();
            int h=i%27==0?extreme[r.nextInt(extreme.length)]:r.nextInt();
            boolean before=i%101==0,after=i%83==0&&!before;
            compare(old,next,x,h,before,after,r);
            if(i%2048==0){
                int xx=r.nextInt(),hh=r.nextInt();
                compare(old,next,xx,hh,false,false,r);reentries++;
            }

        }
        eq(oldPoints>100000 && newPoints==1,"per-paint Point allocation eliminated after first draw");
        eq(coldNpe==2,"cold mouse event exception contract");
        System.out.println("CHIPS_CLOSE_CENTER_JAVA_PASS assertions="+comparisons+" oldPointAlloc="+oldPoints
               +" newPointAlloc="+newPoints+" extraPaints="+reentries+" exceptions="+exceptions
               +" prePaintNpe="+coldNpe);
    }
}
