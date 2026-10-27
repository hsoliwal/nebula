// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.swt.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.swt.SWT;

/**
 * TEST-ONLY minimal SWT.VIRTUAL event simulator. It does not paint, scroll
 * or implement the real Tree; never publish under a product source root.
 */
public final class Tree {
    private final int style;
    private final int columnCount;
    private final List<TreeItem> roots = new ArrayList<>();
    private final Map<Integer, List<Listener>> listeners = new HashMap<>();
    private TreeItem[] selection = new TreeItem[0];
    private boolean disposed;

    public Tree(int style, int columns) {
        this.style = style;
        this.columnCount = columns;
    }
    public int getStyle() { return style; }
    public int getColumnCount() { return columnCount; }
    public int getItemCount() { return roots.size(); }
    public TreeItem getItem(int index) { return roots.get(index); }
    public void setItemCount(int count) { resize(roots, count, null); }

    void resize(List<TreeItem> items, int count, TreeItem parent) {
        if (count < 0) throw new IllegalArgumentException("Negative item count");
        while (items.size() > count) items.remove(items.size() - 1);
        while (items.size() < count) {
            items.add(new TreeItem(this, parent, items.size(), columnCount));
        }
    }

    public void addListener(int type, Listener listener) {
        listeners.computeIfAbsent(type, ignored -> new ArrayList<>()).add(listener);
    }
    public void removeListener(int type, Listener listener) {
        List<Listener> current = listeners.get(type);
        if (current != null) current.remove(listener);
    }

    void realize(TreeItem item) {
        if (item.materialized) return;
        item.materialized = true;
        for (Listener listener : List.copyOf(listeners.getOrDefault(SWT.SetData, List.of()))) {
            listener.handleEvent(new Event(item.relativeIndex, item));
        }
    }

    public void clear(int index, boolean recursive) { roots.get(index).clear(recursive); }
    public void clearAll(boolean recursive) {
        for (TreeItem root : roots) root.clear(recursive);
    }
    public void setSelection(TreeItem[] selected) { selection = selected.clone(); }
    public TreeItem[] getSelection() { return selection.clone(); }
    public boolean isDisposed() { return disposed; }
    public void dispose() {
        disposed = true;
        for (Listener listener : List.copyOf(listeners.getOrDefault(SWT.Dispose, List.of()))) {
            listener.handleEvent(new Event(-1, null));
        }
    }
}
