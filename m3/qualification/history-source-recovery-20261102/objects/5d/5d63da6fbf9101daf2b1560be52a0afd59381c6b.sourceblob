/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.xviewer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * @since 1.2
 */
public final class XViewerViewportContentProvider implements ILazyTreeContentProvider, ITreeContentProvider {
   //FLLDCIM: HPCPOP — only rows the native virtual tree requests are materialized
   private static final Object[] EMPTY = new Object[0];
   /** Child references kept across cached parents before least-recently-used parents are dropped. */
   static final int CACHE_BUDGET = 1 << 20;

   private final XViewer viewer;
   private final ITreeContentProvider delegate;
   private final Map<Object, Object[]> sortedChildren = new LinkedHashMap<>(16, 0.75f, true);
   private int cachedReferences;
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
      if (index < 0 || index >= children.length) {
         return;
      }
      Object child = children[index];
      materializedCount++;
      if (isRoot(parent)) {
         markRoot(index);
      }
      viewer.replace(parent, index, child);
      viewer.setHasChildren(child, hasVisibleChildren(child));
   }

   @Override
   public void updateChildCount(Object element, int currentChildCount) {
      int count = children(element).length;
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

   /** Drops cached child orderings and the materialized-row bits. */
   void invalidate() {
      sortedChildren.clear();
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

   private void markRoot(int index) {
      int word = index >>> 5;
      if (word >= materializedRoots.length) {
         int[] grown = new int[Math.max(word + 1, materializedRoots.length * 2)];
         System.arraycopy(materializedRoots, 0, grown, 0, materializedRoots.length);
         materializedRoots = grown;
      }
      materializedRoots[word] |= 1 << (index & 31);
   }

   private boolean isRoot(Object parent) {
      Object input = viewer.getInput();
      return parent == input || parent != null && parent.equals(input);
   }

   private boolean hasVisibleChildren(Object element) {
      if (viewer.getFilters().length == 0) {
         return delegate.hasChildren(element);
      }
      return children(element).length > 0;
   }

   private void evictColdest() {
      Iterator<Object[]> eldest = sortedChildren.values().iterator();
      while (cachedReferences > CACHE_BUDGET && sortedChildren.size() > 1) {
         cachedReferences -= eldest.next().length;
         eldest.remove();
      }
   }

   private Object[] children(Object parent) {
      if (parent == null) {
         return EMPTY;
      }
      Object[] cached = sortedChildren.get(parent);
      if (cached == null) {
         Object[] raw = isRoot(parent) ? delegate.getElements(parent) : delegate.getChildren(parent);
         cached = viewer.viewportSortedChildren(parent, raw == null ? EMPTY : raw);
         sortedChildren.put(parent, cached);
         cachedReferences += cached.length;
         evictColdest();
      }
      return cached;
   }
}
