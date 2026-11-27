/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.xviewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Viewport bridge used when an {@link XViewer}'s tree was created with
 * {@code SWT.VIRTUAL}: the caller's {@link ITreeContentProvider} is served
 * through JFace's lazy protocol, so the native virtual tree asks only for the
 * rows it shows (plus native overscan) and nothing else is materialized.
 * <p>
 * Filtering and sorting follow {@code AbstractTreeViewer#getSortedChildren}:
 * every viewer filter in order, then the viewer comparator. Sorted children are
 * cached only for parents the tree asked about; any structural refresh clears
 * the cache. Materialized root rows are tracked as a packed bit set.
 * </p>
 * <p>
 * The cached child orderings are cold, recomputable payload: they are kept in least-recently-used order and
 * the oldest are destroyed once more than {@link #CACHE_BUDGET} child references are held, then recomputed on
 * the next request. Model elements are the caller's objects and are never copied or wrapped.
 * </p>
 *
 * @since 1.1
 */
public final class XViewerViewportContentProvider implements ILazyTreeContentProvider, ITreeContentProvider {
   // only rows the native virtual tree requests are materialized
   private static final Object[] EMPTY = new Object[0];
   /** Child references kept across cached parents before least-recently-used parents are dropped. */
   static final int CACHE_BUDGET = 1 << 20;

   private final XViewer viewer;
   private final ITreeContentProvider delegate;
   private final Map<Object, Object[]> sortedChildren = new LinkedHashMap<>(16, 0.75f, true);
   private int cachedReferences;
   /** Parents whose cached children came from a bounded-precompute probe that kept no values for display. */
   private final Set<Object> probedOnly = new HashSet<>();
   /** Parents whose children are being ordered now; a native callback may ask for one of their rows meanwhile. */
   private final List<Object> ordering = new ArrayList<>();
   /** Rows asked for while their parent was being ordered, filled once the order is known. */
   private final List<Object> deferredParents = new ArrayList<>();
   private final List<Integer> deferredIndexes = new ArrayList<>();
   private int[] materializedRoots = new int[0];
   private int materializedCount;

   XViewerViewportContentProvider(XViewer viewer, ITreeContentProvider delegate) {
      this.viewer = viewer;
      this.delegate = delegate;
   }

   /**
    * @return the caller's content provider served by this bridge
    */
   public ITreeContentProvider getDelegate() {
      return delegate;
   }

   /**
    * @return the number of rows materialized through {@link #updateElement(Object, int)} since the last structural
    * refresh
    */
   public int getMaterializedCount() {
      return materializedCount;
   }

   /**
    * @return distinct root rows materialized since the last structural refresh (bit-set cardinality); equal to the
    * root share of {@link #getMaterializedCount()} when every root row was requested once per residency
    */
   public int getMaterializedRootCount() {
      int count = 0;
      for (int word : materializedRoots) {
         count += Integer.bitCount(word);
      }
      return count;
   }

   @Override
   public void updateElement(Object parent, int index) {
      Object[] children = children(parent);
      if (children == null) {
         // asked for by a native callback while this parent is being ordered: filled once the order is known
         deferredParents.add(parent);
         deferredIndexes.add(Integer.valueOf(index));
         return;
      }
      if (index < 0 || index >= children.length) {
         return;
      }
      Object child = children[index];
      // SWT.VIRTUAL may request an existing root again after scrolling.
      // Count distinct resident root indices, not repeated SetData callbacks;
      // still replace the row each time to preserve the native/JFace contract.
      if (!isRoot(parent) || markRoot(index)) {
         materializedCount++;
      }
      viewer.viewportPrepareRow(children, index);
      viewer.replace(parent, index, child);
      viewer.setHasChildren(child, hasVisibleChildren(child));
   }

   @Override
   public void updateChildCount(Object element, int currentChildCount) {
      Object[] children = children(element);
      if (children == null) {
         return; // re-entered while this element's children are being ordered; that call sets the count
      }
      int count = children.length;
      if (count != currentChildCount) {
         viewer.setChildCount(element, count);
      }
   }

   @Override
   public Object getParent(Object element) {
      return delegate.getParent(element);
   }

   @Override
   public Object[] getElements(Object inputElement) {
      return delegate.getElements(inputElement);
   }

   @Override
   public Object[] getChildren(Object parentElement) {
      return delegate.getChildren(parentElement);
   }

   @Override
   public boolean hasChildren(Object element) {
      return delegate.hasChildren(element);
   }

   @Override
   public void dispose() {
      invalidate();
      delegate.dispose();
   }

   @Override
   public void inputChanged(Viewer changed, Object oldInput, Object newInput) {
      invalidate();
      delegate.inputChanged(changed, oldInput, newInput);
   }

   /**
    * Materializes the row of {@code element}, after the rows of its ancestors that have no item yet, so an item API
    * finds the item of an element the native tree has not asked for.
    *
    * @return false if the element is not a row of the tree (filtered out, not in the model, or the tree has no such
    * row yet)
    */
   boolean materialize(Object element) {
      Object known = delegate.getParent(element);
      Object parent = known == null ? viewer.getInput() : known;
      if (parent == null || !isRoot(parent) && !viewer.viewportHasItem(parent) && !materialize(parent)) {
         return false;
      }
      Object[] siblings = children(parent);
      if (siblings == null) {
         return false;
      }
      int index = Arrays.asList(siblings).indexOf(element);
      if (index < 0 || isRoot(parent) && index >= viewer.getTree().getItemCount()) {
         return false;
      }
      if (!isRoot(parent)) {
         viewer.setChildCount(parent, siblings.length);
      }
      updateElement(parent, index);
      return true;
   }

   /**
    * @return the filtered and sorted children of {@code parent} in display order, as the native tree is given them
    */
   Object[] displayChildren(Object parent) {
      Object[] children = children(parent);
      return children == null ? EMPTY : children;
   }

   /** Drops cached child orderings and the materialized-row bits. */
   void invalidate() {
      sortedChildren.clear();
      probedOnly.clear();
      deferredParents.clear();
      deferredIndexes.clear();
      cachedReferences = 0;
      materializedRoots = new int[0];
      materializedCount = 0;
   }

   /**
    * @return true if the root row at {@code index} has been materialized since the last structural refresh
    */
   boolean isRootMaterialized(int index) {
      int word = index >>> 5;
      return index >= 0 && word < materializedRoots.length && (materializedRoots[word] & 1 << (index & 31)) != 0;
   }

   /**
    * @return one past the highest root index that may be materialized
    */
   int materializedRootLimit() {
      return materializedRoots.length << 5;
   }

   /** Returns true only for the first materialization of this root index. */
   private boolean markRoot(int index) {
      int word = index >>> 5;
      if (word >= materializedRoots.length) {
         int[] grown = new int[Math.max(word + 1, materializedRoots.length * 2)];
         System.arraycopy(materializedRoots, 0, grown, 0, materializedRoots.length);
         materializedRoots = grown;
      }
      int bit = 1 << (index & 31);
      boolean fresh = (materializedRoots[word] & bit) == 0;
      materializedRoots[word] |= bit;
      return fresh;
   }

   private boolean isRoot(Object parent) {
      Object input = viewer.getInput();
      return parent == input || parent != null && parent.equals(input);
   }

   private boolean hasVisibleChildren(Object element) {
      if (viewer.getFilters().length == 0) {
         return delegate.hasChildren(element);
      }
      Object[] children = children(element, false);
      return children == null ? delegate.hasChildren(element) : children.length > 0;
   }

   /** Fills the rows of {@code parent} a native callback asked for while it was being ordered. */
   private void fillDeferred(Object parent) {
      for (int i = 0; i < deferredParents.size(); i++) {
         Object waiting = deferredParents.get(i);
         if (waiting == parent || waiting != null && waiting.equals(parent)) {
            deferredParents.remove(i);
            int index = deferredIndexes.remove(i).intValue();
            i--;
            updateElement(parent, index);
         }
      }
   }

   private void evictColdest() {
      Iterator<Map.Entry<Object, Object[]>> eldest = sortedChildren.entrySet().iterator();
      while (cachedReferences > CACHE_BUDGET && sortedChildren.size() > 1) {
         Map.Entry<Object, Object[]> entry = eldest.next();
         cachedReferences -= entry.getValue().length;
         probedOnly.remove(entry.getKey());
         eldest.remove();
      }
   }

   private Object[] children(Object parent) {
      return children(parent, true);
   }

   /**
    * @param shown false when only asked whether the children exist (an unexpanded row), so their precomputed values
    * are not kept for display
    * @return the ordered children, or null when re-entered (from a native callback) while they are being ordered
    */
   private Object[] children(Object parent, boolean shown) {
      if (parent == null) {
         return EMPTY;
      }
      Object[] cached = sortedChildren.get(parent);
      if (cached != null && shown && !probedOnly.isEmpty() && probedOnly.remove(parent)) {
         // the cached order came from a probe whose precomputed values were dropped: order again for display, so the
         // rows shown keep the values they are sorted and filtered on
         cachedReferences -= cached.length;
         sortedChildren.remove(parent);
         cached = null;
      }
      if (cached == null) {
         if (ordering.contains(parent)) {
            return null;
         }
         ordering.add(parent);
         try {
            Object[] raw = isRoot(parent) ? delegate.getElements(parent) : delegate.getChildren(parent);
            cached = viewer.viewportSortedChildren(parent, raw == null ? EMPTY : raw, shown);
         } finally {
            // if ordering fails, rows asked for meanwhile stay deferred and are filled by its next successful ordering
            ordering.remove(ordering.size() - 1);
         }
         sortedChildren.put(parent, cached);
         if (!shown && viewer.boundsPreCompute()) {
            probedOnly.add(parent);
         }
         cachedReferences += cached.length;
         evictColdest();
         fillDeferred(parent);
      }
      return cached;
   }
}
