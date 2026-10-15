// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.pagination.internal;

/** Optional bounded batch boundary. Existing widgets continue to use the Java PageWindow.
 * Each input triple is currentPage,totalPages,maxButtons. Each 32-int output record is
 * length,indexes...,padding(-2). Callers own both arrays; native code copies input first.
 * Load the qualified library explicitly with System.load before nativeWindows; no loader,
 * fallback dispatcher or global native state is installed by this class.
 */
public final class PageWindowBatch {
    public static final int MAX_BATCH = 4096;
    public static final int STRIDE = 32;
    private PageWindowBatch() { }
    public static int[] javaWindows(int[] triples) {
        if (triples == null || triples.length % 3 != 0 || triples.length > MAX_BATCH * 3)
            throw new IllegalArgumentException("invalid batch shape");
        int[] owned = triples.clone();
        for (int i=0; i<owned.length; i+=3) validate(owned[i],owned[i+1],owned[i+2]);
        int[] result = new int[owned.length / 3 * STRIDE];
        java.util.Arrays.fill(result,-2);
        for (int i=0; i<owned.length; i+=3) {
            int[] pages=PageWindow.getPageIndexes(owned[i],owned[i+1],owned[i+2]);
            int offset=i/3*STRIDE;
            result[offset]=pages.length;
            System.arraycopy(pages,0,result,offset+1,pages.length);
        }
        return result;
    }
    /** Explicit JNI entry point; not part of the GWT source set. Same bounded contract. */
    public static native int[] nativeWindows(int[] triples);
    private static void validate(int current,int total,int slots) {
        if(total<0 || current<0 || (total==0?current!=0:current>=total) || slots<5 || slots>31)
            throw new IllegalArgumentException("invalid page window");
    }
}
