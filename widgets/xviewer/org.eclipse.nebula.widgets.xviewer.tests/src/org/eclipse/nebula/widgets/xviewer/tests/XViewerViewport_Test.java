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
package org.eclipse.nebula.widgets.xviewer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.XViewerFactory;
import org.eclipse.nebula.widgets.xviewer.XViewerLabelProvider;
import org.eclipse.nebula.widgets.xviewer.XViewerViewportContentProvider;
import org.eclipse.nebula.widgets.xviewer.core.model.SortDataType;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerAlign;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * XViewer viewport: a 1,000,000-node model (100,000 roots x 9 children) on a {@code SWT.VIRTUAL} tree materializes
 * only the rows the native tree shows, and the viewport bridge orders, filters and expands exactly like the eager
 * JFace path.
 */
public class XViewerViewport_Test {
   private static final int ROOTS = 100_000;
   private static final int KIDS = 9;
   /** Visible rows of a 600 px tree plus native overscan, with headroom. */
   private static final int MATERIALIZED_BOUND = 256;
   /** Rows above and below the shown rows a native row request may still be for (rounding of the scroll bar). */
   private static final int WINDOW_SLACK = 2;

   private Display display;
   private Shell shell;

   @Before
   public void setUp() {
      display = Display.getDefault();
      shell = new Shell(display);
      shell.setLayout(new GridLayout(1, false));
      shell.setBounds(10, 10, 800, 600);
   }

   @After
   public void tearDown() {
      shell.dispose();
   }

   @Test
   public void millionNodeVirtualViewerMaterializesOnlyVisibleRows() {
      XViewer viewer = viewer(SWT.VIRTUAL, ROOTS, KIDS);
      assertTrue(viewer.isViewport());
      assertTrue(viewer.getContentProvider() instanceof XViewerViewportContentProvider);
      assertTrue(((XViewerViewportContentProvider) viewer.getContentProvider()).getDelegate() instanceof Model);
      // the root rows the native tree asks for, seen after the viewer's own SetData listener has filled them; while
      // scrolling, each must be a row the tree shows at that moment, and the scroll positions they are shown at count
      Set<Integer> requestedRoots = new HashSet<>();
      List<String> outsideWindow = new ArrayList<>();
      Set<Integer> shownTops = new HashSet<>();
      boolean[] scrolling = {false};
      viewer.getTree().addListener(SWT.SetData, event -> {
         if (((TreeItem) event.item).getParentItem() != null) {
            return;
         }
         requestedRoots.add(Integer.valueOf(event.index));
         if (scrolling[0]) {
            int[] window = shownWindow((Tree) event.widget);
            shownTops.add(Integer.valueOf(window[0]));
            if (event.index < window[0] - WINDOW_SLACK || event.index > window[0] + window[1] + WINDOW_SLACK) {
               outsideWindow.add(event.index + " (top " + window[0] + ", page " + window[1] + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
         }
      });
      Integer[] roots = Model.roots(ROOTS);
      viewer.setInputXViewer(roots);
      shell.open();
      pump();
      Tree tree = viewer.getTree();
      XViewerViewportContentProvider bridge = (XViewerViewportContentProvider) viewer.getContentProvider();
      assertEquals(ROOTS, tree.getItemCount());
      int first = viewer.getViewportMaterializedCount();
      assertTrue("materialized after first paint: " + first, first > 0 && first <= MATERIALIZED_BOUND);
      assertEquals("one request per root row", first, bridge.getMaterializedRootCount());

      // SWT GTK3 animates setTopItem (see settleScroll): a frame painted while the animation runs shows, and so
      // materializes, a page of rows between the two ends. Such rows are legitimate only if they are on screen when
      // the native tree asks for them; no other row may be materialized while scrolling.
      scrolling[0] = true;
      tree.setTopItem(tree.getItem(ROOTS - 1));
      tree.update();
      pump();
      scrolling[0] = false;
      int scrolled = viewer.getViewportMaterializedCount();
      assertTrue("materialized after scroll to end: " + scrolled, scrolled - first <= MATERIALIZED_BOUND);
      // API reads, not native requests: not checked against the shown rows
      assertEquals("n" + (ROOTS - 1), tree.getItem(ROOTS - 1).getText(0));
      assertSame("item data is the caller's element", roots[ROOTS - 1], tree.getItem(ROOTS - 1).getData());
      assertEquals("one request per root row", scrolled, bridge.getMaterializedRootCount());
      // the frames of the scroll to the end that are still to come
      scrolling[0] = true;
      settleScroll(tree);
      scrolling[0] = false;
      assertTrue("rows asked for outside the shown rows while scrolling to the end: " + outsideWindow, //$NON-NLS-1$
         outsideWindow.isEmpty());
      int resident = viewer.getViewportMaterializedCount();
      Set<Integer> residentRoots = new HashSet<>(requestedRoots);
      assertEquals("each resident root row was asked for by the native tree", resident, residentRoots.size()); //$NON-NLS-1$

      // the round trip: no resident row is asked for again, every row asked for is on screen then, and every row
      // first shown on the way is counted once
      requestedRoots.clear();
      shownTops.clear();
      scrolling[0] = true;
      tree.setTopItem(tree.getItem(0));
      tree.update();
      settleScroll(tree);
      tree.setTopItem(tree.getItem(ROOTS - 1));
      tree.update();
      settleScroll(tree);
      scrolling[0] = false;
      Set<Integer> shownOnTheWay = new HashSet<>(requestedRoots);
      shownOnTheWay.removeAll(residentRoots);
      Set<Integer> askedAgain = new HashSet<>(requestedRoots);
      askedAgain.retainAll(residentRoots);
      int page = shownWindow(tree)[1];
      System.out.println("XVIEWER_VIEWPORT million round trip: " + resident + " resident rows, " //$NON-NLS-1$ //$NON-NLS-2$
         + shownOnTheWay.size() + " rows first shown on the way at " + shownTops.size() + " scroll positions, " //$NON-NLS-1$ //$NON-NLS-2$
         + askedAgain.size() + " resident rows asked for again, " + outsideWindow.size() //$NON-NLS-1$
         + " rows asked for off screen, page " + page); //$NON-NLS-1$
      assertTrue("rows asked for outside the shown rows during the round trip: " + outsideWindow, //$NON-NLS-1$
         outsideWindow.isEmpty());
      assertTrue("resident rows are not requested again: " + askedAgain, askedAgain.isEmpty()); //$NON-NLS-1$
      assertTrue("rows first shown on the way: " + shownOnTheWay.size() + " at " + shownTops.size() //$NON-NLS-1$ //$NON-NLS-2$
         + " scroll positions of a " + page + "-row page", //$NON-NLS-1$ //$NON-NLS-2$
         shownOnTheWay.size() <= shownTops.size() * (page + 2 * WINDOW_SLACK + 1));
      assertEquals("each row first shown on the way is counted once", resident + shownOnTheWay.size(), //$NON-NLS-1$
         viewer.getViewportMaterializedCount());
      assertEquals("one request per root row", viewer.getViewportMaterializedCount(), //$NON-NLS-1$
         bridge.getMaterializedRootCount());
      int roundTrip = viewer.getViewportMaterializedCount();
      assertSame(roots[0], tree.getItem(0).getData());

      Integer root = Integer.valueOf(ROOTS - 1);
      viewer.setExpandedState(root, true);
      pump();
      assertEquals(KIDS, tree.getItem(ROOTS - 1).getItemCount());
      // the status line is recomputed on load, sort, refresh and mouse-up, not by an API expansion
      viewer.updateStatusLabel();
      // the status text is a MessageFormat pattern, so the count carries the locale's grouping (100,009)
      String shown = MessageFormat.format("{0}", Integer.valueOf(ROOTS + KIDS));
      String status = viewer.getStatusLabel().getText();
      assertTrue(status, status.contains(shown));
      assertTrue(viewer.getViewportMaterializedCount() - roundTrip <= MATERIALIZED_BOUND);
   }

   @Test
   public void viewportOrdersFiltersAndExpandsLikeEagerViewer() {
      XViewer eager = viewer(SWT.NONE, 200, 3);
      XViewer lazy = viewer(SWT.VIRTUAL, 200, 3);
      assertTrue(!eager.isViewport() && eager.getViewportMaterializedCount() == -1);
      for (XViewer v : new XViewer[] {eager, lazy}) {
         v.setInputXViewer(Model.roots(200));
         v.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object a, Object b) {
               return Integer.compare(((Integer) b).intValue(), ((Integer) a).intValue());
            }
         });
         v.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parent, Object element) {
               return ((Integer) element).intValue() % 3 != 1;
            }
         });
         v.setExpandedState(Integer.valueOf(198), true);
      }
      shell.open();
      pump();
      assertEquals(texts(eager.getTree()), texts(lazy.getTree()));
      assertEquals(eager.getStatusLabel().getText(), lazy.getStatusLabel().getText());
      assertEquals(eager.getExpandedElements().length, lazy.getExpandedElements().length);
   }

   @Test
   public void stateSurvivesScrollingAwayAndBack() {
      XViewer viewer = viewer(SWT.VIRTUAL, 20_000, 3);
      viewer.setInputXViewer(Model.roots(20_000));
      shell.open();
      pump();
      Tree tree = viewer.getTree();
      viewer.setExpandedState(Integer.valueOf(2), true);
      viewer.setSelection(new org.eclipse.jface.viewers.StructuredSelection(Integer.valueOf(4)));
      tree.setTopItem(tree.getItem(0));
      settleScroll(tree);
      // Native SWT can scroll the selected row into view. Restore the actual
      // visible anchor when testing a round trip, not a different fixed row.
      TreeItem topBefore = visibleTopItem(tree);
      String before = state(viewer);
      String pixels = pixels(tree);

      tree.setTopItem(tree.getItem(19_999));
      int requested = tree.getVerticalBar().getSelection();
      tree.update();
      settleScroll(tree);
      int away = tree.getVerticalBar().getSelection();
      tree.setTopItem(topBefore);
      tree.update();
      settleScroll(tree);
      System.out.println("XVIEWER_VIEWPORT round trip: scroll bar " + requested + " when away was asked, " + away //$NON-NLS-1$ //$NON-NLS-2$
         + " settled away, " + tree.getVerticalBar().getSelection() + " settled back"); //$NON-NLS-1$ //$NON-NLS-2$
      assertEquals(before, state(viewer));
      assertEquals(pixels, pixels(tree));
   }

   /**
    * Pumps until the scroll has settled. SWT GTK3 scrolls {@code Tree.setTopItem()} through
    * {@code gtk_tree_view_scroll_to_cell}, which (with GTK animations enabled) animates the vertical adjustment over
    * 200 ms of frame-clock ticks ({@code gtk_adjustment_animate_to_value}) instead of setting it. {@link #pump()}
    * returns at the first moment nothing is pending, which can fall between two animation frames, so a row read
    * after it can be one the animation passes through: a CI run read row 17126 at the top after the round trip back
    * to row 0. The scroll has settled when the vertical scroll bar's selection and the row shown at the top have held
    * for five checks 20 ms apart, longer than a frame. Fails after 10 s.
    */
   private void settleScroll(Tree tree) {
      int lastSelection = Integer.MIN_VALUE;
      TreeItem lastTop = null;
      int stable = 0;
      long deadline = System.currentTimeMillis() + 10_000;
      while (System.currentTimeMillis() < deadline) {
         pump();
         int selection = tree.getVerticalBar().getSelection();
         TreeItem top = visibleTopItem(tree);
         stable = selection == lastSelection && top == lastTop ? stable + 1 : 0;
         lastSelection = selection;
         lastTop = top;
         if (stable >= 5) {
            return;
         }
         try {
            Thread.sleep(20);
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            break;
         }
      }
      throw new AssertionError("the scroll did not settle: scroll bar " + lastSelection + ", top row " //$NON-NLS-1$ //$NON-NLS-2$
         + (lastTop == null ? null : lastTop.getText(0)));
   }


   /**
    * The original XViewer must capture encounter order without retaining a LinkedList
    * node for every input occurrence. One-shot callers and null/duplicate identity
    * have the same source semantics as the previous implementation.
    */
   @Test
   public void densePrecomputeStagingPreservesInputOccurrenceIdentity() throws Exception {
      XViewer viewer = viewer(SWT.NONE, 4, 0);
      java.lang.reflect.Method copy = XViewer.class.getDeclaredMethod("getInputObjects", Object.class); //$NON-NLS-1$
      copy.setAccessible(true);
      Object first = new Object();
      Object second = new Object();
      Object[] entries = {first, null, first, second};
      @SuppressWarnings("unchecked")
      List<Object> captured = (List<Object>) copy.invoke(viewer, (Object) entries);
      assertTrue("the precompute snapshot uses an indexed dynamic array", captured instanceof ArrayList<?>); //$NON-NLS-1$
      assertEquals(entries.length, captured.size());
      for (int i = 0; i < entries.length; i++) {
         assertSame(entries[i], captured.get(i));
      }
      java.util.Collection<Object> oneShot = new java.util.AbstractCollection<>() {
         private boolean consumed;
         @Override public int size() {
            throw new AssertionError("the source size is not a prepass"); //$NON-NLS-1$
         }
         @Override public java.util.Iterator<Object> iterator() {
            if (consumed) {
               throw new AssertionError("the input was visited twice"); //$NON-NLS-1$
            }
            consumed = true;
            return java.util.Arrays.asList(entries).iterator();
         }
      };
      @SuppressWarnings("unchecked")
      List<Object> fromCollection = (List<Object>) copy.invoke(viewer, oneShot);
      for (int i = 0; i < entries.length; i++) {
         assertSame(entries[i], fromCollection.get(i));
      }
      assertTrue(((List<?>) copy.invoke(viewer, new Object())).isEmpty());
   }

   @Test
   public void arrayIndexedViewportPathKeepsRootFirstAndCallerIdentity() throws Exception {
      XViewer viewer = viewer(SWT.VIRTUAL, 7, 2);
      Integer[] roots = Model.roots(7);
      viewer.setInputXViewer(roots);
      shell.open();
      pump();
      assertTrue(viewer.isViewport());
      java.lang.reflect.Method path = XViewer.class.getDeclaredMethod("viewportPath", Object.class); //$NON-NLS-1$
      path.setAccessible(true);
      Integer child = Integer.valueOf(7 + 4 * 2 + 1);
      org.eclipse.jface.viewers.TreePath result =
         (org.eclipse.jface.viewers.TreePath) path.invoke(viewer, child);
      assertEquals(2, result.getSegmentCount());
      assertEquals(Integer.valueOf(4), result.getSegment(0));
      assertSame(child, result.getSegment(1));
      org.eclipse.jface.viewers.TreePath rootPath =
         (org.eclipse.jface.viewers.TreePath) path.invoke(viewer, roots[3]);
      assertEquals(1, rootPath.getSegmentCount());
      assertSame(roots[3], rootPath.getSegment(0));
   }

   /** Native SWT can emit duplicate SetData callbacks after a viewport round trip.
    * Their JFace replace callback is still required, but they are not new residents. */
   @Test
   public void repeatedNativeRootRequestsCountOnlyDistinctRows() {
      XViewer viewer = viewer(SWT.VIRTUAL, 64, 2);
      Integer[] roots = Model.roots(64);
      viewer.setInputXViewer(roots);
      shell.open();
      pump();
      Tree tree = viewer.getTree();
      assertSame(roots[0], tree.getItem(0).getData());
      XViewerViewportContentProvider bridge =
            (XViewerViewportContentProvider) viewer.getContentProvider();
      int count = viewer.getViewportMaterializedCount();
      int rootCount = bridge.getMaterializedRootCount();
      assertTrue("initial visible roots must materialize", count > 0);
      assertEquals("initial requests describe distinct roots", count, rootCount);
      for (int repeated = 0; repeated < 128; repeated++) {
         bridge.updateElement(viewer.getInput(), 0);
      }
      assertEquals("repeat SetData callbacks must not inflate residency",
            count, viewer.getViewportMaterializedCount());
      assertEquals("packed bitset cardinality remains unchanged",
            rootCount, bridge.getMaterializedRootCount());
      assertSame("native row retains the caller element", roots[0], tree.getItem(0).getData());
   }

   private static String state(XViewer viewer) {
      Tree tree = viewer.getTree();
      StringBuilder s = new StringBuilder();
      s.append(((org.eclipse.jface.viewers.IStructuredSelection) viewer.getSelection()).toList());
      s.append('|').append(java.util.Arrays.asList(viewer.getExpandedElements()));
      s.append('|').append(visibleTopItem(tree).getText(0));
      s.append('|').append(viewer.getStatusLabel().getText());
      for (int i = 0; i < 8; i++) {
         s.append('|').append(tree.getItem(i).getText(0)).append(tree.getItem(i).getExpanded());
      }
      return s.toString();
   }

   /**
    * The row actually shown at the top of the tree. SWT GTK's {@code Tree.getTopItem()} answers the selected row
    * instead while the vertical adjustment still equals the value its last {@code setTopItem()} cached
    * ({@code Tree._getCachedTopItem}): here, after {@code setTopItem(item 0)} on the unscrolled tree with row 4
    * selected, it returned row 4 while row 0 was at the top. On GTK3 the rows live in the tree view's bin window,
    * whose y = 0 is the first pixel row below the header, so the row there is hit-tested instead (GTK4 counts the
    * header in the same coordinates).
    */
   private static TreeItem visibleTopItem(Tree tree) {
      if (!"gtk".equals(SWT.getPlatform())) {
         return tree.getTopItem();
      }
      int x = tree.getColumn(0).getWidth() * 3 / 4;
      TreeItem item = tree.getItem(new org.eclipse.swt.graphics.Point(x, 1));
      if (item == null && tree.getHeaderVisible()) {
         item = tree.getItem(new org.eclipse.swt.graphics.Point(x, tree.getHeaderHeight() + 1));
      }
      assertTrue("no row at the top of the tree", item != null);
      return item;
   }

   /**
    * The root rows the tree shows now, read from its vertical scroll bar so that it holds whether the bar counts rows
    * (win32) or pixels (GTK): {top row, rows in a page rounded up}. For a tree of root rows of one height.
    */
   private static int[] shownWindow(Tree tree) {
      org.eclipse.swt.widgets.ScrollBar bar = tree.getVerticalBar();
      long rows = tree.getItemCount();
      long max = Math.max(1, bar.getMaximum());
      return new int[] {(int) (bar.getSelection() * rows / max), (int) ((bar.getThumb() * rows + max - 1) / max)};
   }

   /** SHA-256 of the control's own rendering (Control.print), independent of window occlusion. */
   static String pixels(org.eclipse.swt.widgets.Control control) {
      org.eclipse.swt.graphics.Point size = control.getSize();
      Image image = new Image(control.getDisplay(), size.x, size.y);
      org.eclipse.swt.graphics.GC gc = new org.eclipse.swt.graphics.GC(image);
      try {
         control.print(gc);
         org.eclipse.swt.graphics.ImageData data = image.getImageData();
         java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
         md.update(data.data);
         StringBuilder hex = new StringBuilder();
         for (byte b : md.digest()) {
            hex.append(String.format("%02x", b & 0xff));
         }
         return hex.toString();
      } catch (java.security.NoSuchAlgorithmException ex) {
         throw new IllegalStateException(ex);
      } finally {
         gc.dispose();
         image.dispose();
      }
   }

   @Test
   public void plainTreeKeepsCallerContentProvider() {
      XViewer viewer = viewer(SWT.NONE, 5, 1);
      assertTrue(viewer.getContentProvider() instanceof Model);
      viewer.setInputXViewer(Model.roots(5));
      assertSame(viewer.getTree().getItem(0).getData(), viewer.getTree().getItem(0).getData());
   }

   private static List<String> texts(Tree tree) {
      List<String> out = new ArrayList<>();
      for (int i = 0; i < tree.getItemCount(); i++) {
         TreeItem item = tree.getItem(i);
         out.add(item.getText(0));
         if (item.getExpanded()) {
            for (int j = 0; j < item.getItemCount(); j++) {
               out.add("  " + item.getItem(j).getText(0));
            }
         }
      }
      return out;
   }

   private void pump() {
      for (int i = 0; i < 10_000 && display.readAndDispatch(); i++) {
         // drain pending paint and async work
      }
   }

   private XViewer viewer(int style, int roots, int kids) {
      Composite parent = new Composite(shell, SWT.NONE);
      parent.setLayout(new GridLayout(1, false));
      parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      XViewerColumn name =
         new XViewerColumn("tt.name", "Name", 200, XViewerAlign.Left, true, SortDataType.String, false, null);
      XViewerFactory factory = new XViewerFactory("tt.viewport.test") {
         @Override
         public boolean isAdmin() {
            return false;
         }
      };
      factory.registerColumns(name);
      XViewer viewer = new XViewer(parent, style | SWT.MULTI | SWT.FULL_SELECTION, factory);
      viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      viewer.setContentProvider(new Model(roots, kids));
      viewer.setLabelProvider(new Labels(viewer));
      viewer.setForcePend(true);
      return viewer;
   }

   /** Integer ids: roots 0..R-1, child j of root r is R + r*K + j; no per-node objects beyond the ids. */
   static final class Model implements ITreeContentProvider {
      private final int roots;
      private final int kids;

      Model(int roots, int kids) {
         this.roots = roots;
         this.kids = kids;
      }

      static Integer[] roots(int count) {
         Integer[] r = new Integer[count];
         for (int i = 0; i < count; i++) {
            r[i] = Integer.valueOf(i);
         }
         return r;
      }

      @Override
      public Object[] getElements(Object input) {
         return input instanceof List ? ((List<?>) input).toArray() : (Object[]) input;
      }

      @Override
      public Object[] getChildren(Object parent) {
         if (!(parent instanceof Integer)) {
            return getElements(parent);
         }
         int id = ((Integer) parent).intValue();
         if (id >= roots) {
            return new Object[0];
         }
         Object[] c = new Object[kids];
         for (int j = 0; j < kids; j++) {
            c[j] = Integer.valueOf(roots + id * kids + j);
         }
         return c;
      }

      @Override
      public Object getParent(Object element) {
         int id = ((Integer) element).intValue();
         return id < roots ? null : Integer.valueOf((id - roots) / kids);
      }

      @Override
      public boolean hasChildren(Object element) {
         return element instanceof Integer && ((Integer) element).intValue() < roots && kids > 0;
      }
   }

   static final class Labels extends XViewerLabelProvider {
      Labels(XViewer viewer) {
         super(viewer);
      }

      @Override
      public Image getColumnImage(Object element, XViewerColumn xCol, int columnIndex) {
         return null;
      }

      @Override
      public String getColumnText(Object element, XViewerColumn xCol, int columnIndex) {
         return "n" + element;
      }

      @Override
      public void addListener(ILabelProviderListener listener) {
         // stateless
      }

      @Override
      public void dispose() {
         // stateless
      }

      @Override
      public boolean isLabelProperty(Object element, String property) {
         return false;
      }

      @Override
      public void removeListener(ILabelProviderListener listener) {
         // stateless
      }
   }
}
