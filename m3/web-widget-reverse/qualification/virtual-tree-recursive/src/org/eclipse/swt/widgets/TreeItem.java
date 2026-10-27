// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.swt.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TEST ONLY: deliberately retains application-defined getData keys when
 * clear(true) recursively invalidates visual content. No graphics or OS.
 */
public final class TreeItem {
    final Tree owner;
    final TreeItem parent;
    final int relativeIndex;
    final List<TreeItem> children = new ArrayList<>();
    final String[] text;
    final Map<String, Object> data = new HashMap<>();
    boolean materialized;
    boolean expanded;

    TreeItem(Tree tree, TreeItem parent, int index, int columns) {
        this.owner = tree;
        this.parent = parent;
        this.relativeIndex = index;
        this.text = new String[columns];
        Arrays.fill(text, "");
    }
    public TreeItem getParentItem() { return parent; }
    public int getItemCount() { return children.size(); }
    public TreeItem getItem(int index) { return children.get(index); }
    public void setItemCount(int count) { owner.resize(children, count, this); }
    public String getText(int column) { owner.realize(this); return text[column]; }
    public void setText(int column, String value) { text[column] = value; }
    public Object getData(String key) { return data.get(key); }
    public void setData(String key, Object value) { data.put(key, value); }
    public boolean getExpanded() { return expanded; }
    public void setExpanded(boolean value) { expanded = value; }

    void clear(boolean recursive) {
        materialized = false;
        Arrays.fill(text, "");
        // This simulator intentionally preserves application-defined data.
        if (recursive) {
            for (TreeItem child : children) child.clear(true);
        }
    }
}
