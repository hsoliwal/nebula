/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.swt.custom;
import java.util.concurrent.Future;
public final class BusyIndicator {
    private BusyIndicator() {}
    public static void showWhile(Future<?> task) {
        try { task.get(); } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
