// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.swt;
/** TEST-ONLY API symbols. NOT the Eclipse SWT implementation. */
public final class SWT {
    public static final int VIRTUAL=1, MULTI=2, SetData=3, Selection=4, DefaultSelection=5, Dispose=6;
    public static final int ERROR_NULL_ARGUMENT=7,ERROR_INVALID_ARGUMENT=8,ERROR_INVALID_RANGE=9;
    private SWT() {}
    public static void error(int code) { throw new IllegalArgumentException("SWT.error("+code+")"); }
}
