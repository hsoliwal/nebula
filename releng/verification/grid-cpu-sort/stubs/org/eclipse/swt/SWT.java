/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.swt;
public final class SWT {
    public static final int Dispose = 12;
    public static final int ERROR_INVALID_ARGUMENT = 5;
    private SWT() {}
    public static void error(int code) { throw new SWTException(code); }
}
