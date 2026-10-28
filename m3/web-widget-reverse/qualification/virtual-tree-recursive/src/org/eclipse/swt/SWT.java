// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.swt;

/** TEST-ONLY constants: API shape, not Eclipse SWT or an SWT replacement. */
public final class SWT {
    public static final int VIRTUAL = 1;
    public static final int MULTI = 2;
    public static final int SetData = 3;
    public static final int Selection = 4;
    public static final int DefaultSelection = 5;
    public static final int Dispose = 6;
    public static final int ERROR_NULL_ARGUMENT = 7;
    public static final int ERROR_INVALID_ARGUMENT = 8;
    public static final int ERROR_INVALID_RANGE = 9;
    private SWT() { }
    public static void error(int code) {
        throw new IllegalArgumentException("SWT.error(" + code + ")");
    }
}
