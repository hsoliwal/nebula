// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.chips;
import java.util.Random;

/** JNI C17 arithmetic witness, never called from the production widget. */
public final class ChipsCloseCenterJniProof {
    private static long comparisons;
    private static native int[] center(int x, int height);
    static { System.loadLibrary("m3_chips_close_center_test"); }

    private static void check(int x,int height) {
        int javaX=x+4+7;
        int javaY=(height-2*7)/2+7;
        int[] nativeValue=center(x,height);
        comparisons++;
        if(nativeValue==null || nativeValue.length!=2 || javaX!=nativeValue[0] || javaY!=nativeValue[1]) {
            throw new AssertionError("original Chips.drawClose JNI divergence at case="+comparisons
                    +" x="+x+" h="+height+" java="+javaX+","+javaY);
        }
    }
    public static void main(String[] args) {
        boolean quick=args.length>0 && "--quick".equals(args[0]);
        int[] boundary={Integer.MIN_VALUE,Integer.MIN_VALUE+1,-1000000,-21,-15,-14,-13,
                -1,0,1,7,14,15,28,100,65535,Integer.MAX_VALUE-1,Integer.MAX_VALUE};
        for(int x:boundary)for(int h:boundary)check(x,h);
        Random random=new Random(0x4d3343484950534cL);
        int n=quick?1000:150000;
        for(int i=0;i<n;i++) {
            int x=i%31==0?boundary[random.nextInt(boundary.length)]:random.nextInt();
            int h=i%29==0?boundary[random.nextInt(boundary.length)]:random.nextInt();
            check(x,h);
        }
        System.out.println("CHIPS_CLOSE_CENTER_JNI_PASS checks="+comparisons+" quick="+quick);
    }
}
