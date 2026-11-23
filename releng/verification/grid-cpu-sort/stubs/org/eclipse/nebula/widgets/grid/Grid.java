/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.grid;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.nebula.widgets.grid.model.RowPermutation;
/** Strict thread-checking stub: used only by the headless scheduling contract. */
public final class Grid {
    private final Display display;
    private RowPermutation permutation;
    private final List<Listener> disposeListeners = new ArrayList<>();
    private boolean disposed;
    private int refreshCount;
    public Grid(Display display, int rows) {
        this.display = display;
        permutation = new RowPermutation(rows);
    }
    private void checkUi() {
        if (Thread.currentThread() != display.getThread()) {
            throw new AssertionError("Grid accessed from worker");
        }
    }
    public Display getDisplay() { checkUi(); return display; }
    public RowPermutation getRowPermutation() { checkUi(); return permutation; }
    public void refreshRowPermutation() { checkUi(); refreshCount++; }
    public int refreshCount() { checkUi(); return refreshCount; }
    public boolean isDisposed() { checkUi(); return disposed; }
    public void addListener(int type, Listener listener) {
        checkUi(); if (type == SWT.Dispose) disposeListeners.add(listener);
    }
    public void removeListener(int type, Listener listener) {
        checkUi(); if (type == SWT.Dispose) disposeListeners.remove(listener);
    }
    public void dispose() {
        checkUi();
        if (disposed) return;
        Event event = new Event(); event.widget = this;
        for (Listener listener : List.copyOf(disposeListeners)) listener.handleEvent(event);
        disposed = true;
    }
}
