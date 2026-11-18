/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.swt;
public final class SWTException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public SWTException(int code) { super("SWT " + code); }
}
