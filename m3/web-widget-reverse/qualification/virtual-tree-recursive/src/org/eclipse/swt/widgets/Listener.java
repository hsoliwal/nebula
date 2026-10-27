// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.swt.widgets;

/** TEST ONLY; intentionally implements the original listener signature. */
@FunctionalInterface
public interface Listener {
    void handleEvent(Event event);
}
