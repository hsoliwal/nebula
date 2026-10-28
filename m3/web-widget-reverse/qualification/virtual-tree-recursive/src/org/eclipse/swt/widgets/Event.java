// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.swt.widgets;

/** TEST ONLY; index is relative to the Tree or parent TreeItem. */
public final class Event {
    public int index;
    public Object item;
    public Event(int index, Object item) {
        this.index = index;
        this.item = item;
    }
}
