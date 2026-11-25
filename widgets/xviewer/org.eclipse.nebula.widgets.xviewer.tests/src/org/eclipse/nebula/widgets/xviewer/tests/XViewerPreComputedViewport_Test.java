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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.nebula.widgets.xviewer.IXViewerPreComputedColumn;
import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.XViewerFactory;
import org.eclipse.nebula.widgets.xviewer.XViewerLabelProvider;
import org.eclipse.nebula.widgets.xviewer.core.model.SortDataType;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerAlign;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Precomputed columns of a viewport XViewer ({@code SWT.VIRTUAL} tree) over 100,000 input rows: the values computed
 * by {@link IXViewerPreComputedColumn#populateCachedValues} stay resident only for a bounded window of rows after
 * load, scrolling, sorting and filtering, while sort and filter results, painted text and the widget tree are those of
 * the eager path. Each phase prints an {@code XVIEWER_PRECOMPUTE} line, so the same test measures master and head.
 */
public class XViewerPreComputedViewport_Test {
   private static final int ROWS = 100_000;
   /** Precomputed values a viewport keeps per column, whatever the input size (the window). */
   private static final int WINDOW = 1024;
   /** Visible rows of a 600 px tree plus native overscan, with headroom (as XViewerViewport_Test). */
   private static final int MATERIALIZED_BOUND = 256;
   /** Rows computed together with a requested row that is not resident. */
   private static final int BLOCK = 64;
   /** Values that may stay reachable: the window, plus the text of rows the native tree materialized. */
   private static final int RETAINED_BOUND = WINDOW + 2 * MATERIALIZED_BOUND;
   private static final String PRE_ID = "tt.pre"; //$NON-NLS-1$
   private static final String FILTER = "v0123"; //$NON-NLS-1$

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

   /**
    * Fails on master: there the column map holds a value for each of the 100,000 input rows from load on, and every
    * value stays reachable.
    */
   @Test
   public void precomputedValuesStayBoundedAfterLoadScrollSortAndFilter() {
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats);
      Integer[] roots = Flat.roots(ROWS);
      List<String> violations = new ArrayList<>();

      long start = System.nanoTime();
      viewer.setInputXViewer(roots);
      shell.open();
      pump();
      long loadNanos = System.nanoTime() - start;
      Tree tree = viewer.getTree();
      assertEquals(ROWS, tree.getItemCount());
      assertEquals(Pre.value(0), tree.getItem(0).getText(1));
      check("load", viewer, stats, loadNanos, violations);

      tree.setTopItem(tree.getItem(ROWS - 1));
      tree.update();
      pump();
      assertEquals(Pre.value(ROWS - 1), tree.getItem(ROWS - 1).getText(1));
      check("scroll-end", viewer, stats, 0, violations);

      start = System.nanoTime();
      clickHeader(viewer, PRE_ID);
      pump();
      long sortNanos = System.nanoTime() - start;
      check("sort", viewer, stats, sortNanos, violations);

      start = System.nanoTime();
      viewer.getCustomizeMgr().setColumnFilterText(PRE_ID, FILTER);
      pump();
      long filterNanos = System.nanoTime() - start;
      assertEquals(100, tree.getItemCount());
      check("filter", viewer, stats, filterNanos, violations);

      viewer.getCustomizeMgr().setColumnFilterText(PRE_ID, null);
      tree.setTopItem(tree.getItem(ROWS / 2));
      tree.update();
      pump();
      assertEquals(ROWS, tree.getItemCount());
      check("unfilter-scroll", viewer, stats, 0, violations);

      assertTrue(String.join("\n", violations), violations.isEmpty());
   }

   /**
    * Sorting and filtering by the precomputed column orders the 100,000 rows exactly by the column's values, and the
    * painted cells show the values the rows were ordered by. Passes on master and head; the printed order digest is
    * the same on both.
    */
   @Test
   public void sortAndFilterByPrecomputedColumnMatchColumnOrder() throws Exception {
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats);
      Integer[] roots = Flat.roots(ROWS);
      viewer.setInputXViewer(roots);
      shell.open();
      pump();
      Tree tree = viewer.getTree();

      clickHeader(viewer, PRE_ID);
      pump();
      XViewerColumn pre = column(viewer, PRE_ID);
      Integer[] expected = roots.clone();
      Comparator<Integer> byValue = Comparator.comparing(Pre::value);
      Arrays.sort(expected, pre.isSortForward() ? byValue : byValue.reversed());
      Object[] shown = displayOrder(viewer);
      System.out.println("XVIEWER_PRECOMPUTE order=sorted rows=" + shown.length + " sha256=" + digest(shown)); //$NON-NLS-1$ //$NON-NLS-2$
      assertArrayEquals(expected, shown);
      for (int row = 0; row < 16; row++) {
         assertEquals(Pre.value(expected[row].intValue()), tree.getItem(row).getText(1));
         assertSame(shown[row], tree.getItem(row).getData());
      }
      tree.setTopItem(tree.getItem(ROWS - 1));
      tree.update();
      pump();
      for (int row = ROWS - 16; row < ROWS; row++) {
         assertEquals(Pre.value(expected[row].intValue()), tree.getItem(row).getText(1));
      }

      viewer.getCustomizeMgr().setColumnFilterText(PRE_ID, FILTER);
      pump();
      List<Integer> filtered = new ArrayList<>();
      for (Integer id : expected) {
         if (Pre.value(id.intValue()).contains(FILTER)) {
            filtered.add(id);
         }
      }
      Object[] shownFiltered = displayOrder(viewer);
      System.out.println("XVIEWER_PRECOMPUTE order=filtered rows=" + shownFiltered.length + " sha256=" //$NON-NLS-1$ //$NON-NLS-2$
         + digest(shownFiltered));
      assertArrayEquals(filtered.toArray(), shownFiltered);
      assertEquals(filtered.size(), tree.getItemCount());
      for (int row = 0; row < filtered.size(); row++) {
         assertEquals(Pre.value(filtered.get(row).intValue()), tree.getItem(row).getText(1));
      }
   }

   /**
    * The eager tree keeps its original precompute (one value per input row) and the viewport shows the same rows in
    * the same order with the same text after sorting and filtering by the precomputed column.
    */
   @Test
   public void eagerAndViewportAgreeOnPrecomputedSortAndFilter() {
      int rows = 2_000;
      Stats eagerStats = new Stats();
      Stats lazyStats = new Stats();
      XViewer eager = viewer(SWT.NONE, eagerStats);
      XViewer lazy = viewer(SWT.VIRTUAL, lazyStats);
      for (XViewer v : new XViewer[] {eager, lazy}) {
         v.setInputXViewer(Flat.roots(rows));
      }
      shell.open();
      pump();
      assertEquals("the eager precompute is unchanged", rows, column(eager, PRE_ID).getPreComputedValueMap().size()); //$NON-NLS-1$
      assertEquals(texts(eager.getTree()), texts(lazy.getTree()));
      for (XViewer v : new XViewer[] {eager, lazy}) {
         clickHeader(v, PRE_ID);
      }
      pump();
      assertEquals(texts(eager.getTree()), texts(lazy.getTree()));
      for (XViewer v : new XViewer[] {eager, lazy}) {
         v.getCustomizeMgr().setColumnFilterText(PRE_ID, "v01"); //$NON-NLS-1$
      }
      pump();
      assertTrue(eager.getTree().getItemCount() > 0);
      assertEquals(texts(eager.getTree()), texts(lazy.getTree()));
      assertEquals(eager.getStatusLabel().getText(), lazy.getStatusLabel().getText());
   }

   /**
    * One scroll frame over 100,000 rows paints, labels and computes only the rows it shows (plus overscan and one
    * block of following rows), and the viewer adds no child control or shell. Passes on master and head.
    */
   @Test
   public void scrollFramePaintsVisibleCellsOnlyWithoutChildControls() {
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats);
      Tree tree = viewer.getTree();
      Composite parent = tree.getParent();
      Control[] parentChildren = parent.getChildren();
      int shells = display.getShells().length;
      int[] painted = new int[1];
      tree.addListener(SWT.PaintItem, event -> {
         if (event.index == 1) {
            painted[0]++;
         }
      });
      viewer.setInputXViewer(Flat.roots(ROWS));
      shell.open();
      pump();

      int[] frames = {ROWS / 2, ROWS / 4, ROWS - 1};
      for (int top : frames) {
         painted[0] = 0;
         int labels = stats.labels;
         int computed = stats.computed;
         int materialized = viewer.getViewportMaterializedCount();
         tree.setTopItem(tree.getItem(top));
         tree.update();
         pump();
         int frameLabels = stats.labels - labels;
         int frameComputed = stats.computed - computed;
         int frameRows = viewer.getViewportMaterializedCount() - materialized;
         System.out.println("XVIEWER_PRECOMPUTE frame top=" + top + " paintedCells=" + painted[0] + " labelCells=" //$NON-NLS-1$ //$NON-NLS-2$
            + frameLabels + " computed=" + frameComputed + " materializedRows=" + frameRows); //$NON-NLS-1$ //$NON-NLS-2$
         assertTrue("painted precomputed cells per frame: " + painted[0], painted[0] <= MATERIALIZED_BOUND); //$NON-NLS-1$
         assertTrue("labelled precomputed cells per frame: " + frameLabels, frameLabels <= MATERIALIZED_BOUND); //$NON-NLS-1$
         assertTrue("values computed per frame: " + frameComputed, frameComputed <= MATERIALIZED_BOUND + BLOCK); //$NON-NLS-1$
         assertTrue("rows materialized per frame: " + frameRows, frameRows <= MATERIALIZED_BOUND); //$NON-NLS-1$
         assertEquals(Pre.value(top), tree.getItem(top).getText(1));
      }
      clickHeader(viewer, PRE_ID);
      viewer.getCustomizeMgr().setColumnFilterText(PRE_ID, FILTER);
      pump();
      assertEquals("no child control in the tree", 0, tree.getChildren().length); //$NON-NLS-1$
      assertArrayEquals("no control added beside the tree", parentChildren, parent.getChildren()); //$NON-NLS-1$
      assertEquals("no shell opened", shells, display.getShells().length); //$NON-NLS-1$
   }

   /**
    * The column map of a viewport is a bounded {@link Map}: it keeps at most the window, keeps what is used, maps each
    * resident key to its own value, and supports null keys and values, removal and write-through entries like
    * {@link java.util.HashMap}. Fails on master, where the map grows with every key.
    */
   @Test
   public void viewportColumnMapIsABoundedMap() {
      XViewer viewer = viewer(SWT.VIRTUAL, new Stats());
      viewer.setInputXViewer(Flat.roots(10));
      shell.open();
      pump();
      Map<Long, String> map = column(viewer, PRE_ID).getPreComputedValueMap();
      map.clear();
      assertTrue(map.isEmpty());
      Long kept = Long.valueOf(-1);
      map.put(kept, "kept"); //$NON-NLS-1$
      for (long key = 0; key < 4 * WINDOW; key++) {
         map.put(Long.valueOf(key), "x" + key); //$NON-NLS-1$
         assertEquals("a value in use stays resident", "kept", map.get(kept)); //$NON-NLS-1$ //$NON-NLS-2$
      }
      assertTrue("resident values: " + map.size(), map.size() <= WINDOW); //$NON-NLS-1$
      assertEquals("x" + (4 * WINDOW - 1), map.get(Long.valueOf(4L * WINDOW - 1))); //$NON-NLS-1$
      int entries = 0;
      for (Map.Entry<Long, String> entry : map.entrySet()) {
         assertEquals(entry.getValue(), map.get(entry.getKey()));
         assertTrue(entry.getKey().longValue() == -1 || entry.getValue().equals("x" + entry.getKey())); //$NON-NLS-1$
         entries++;
      }
      assertEquals(map.size(), entries);

      map.put(null, "null key"); //$NON-NLS-1$
      assertEquals("null key", map.get(null)); //$NON-NLS-1$
      map.put(Long.valueOf(7), null);
      assertTrue(map.containsKey(Long.valueOf(7)));
      assertNull(map.get(Long.valueOf(7)));
      assertNull(map.remove(Long.valueOf(7)));
      assertFalse(map.containsKey(Long.valueOf(7)));
      int size = map.size();
      Iterator<Map.Entry<Long, String>> iterator = map.entrySet().iterator();
      iterator.next();
      iterator.remove();
      assertEquals(size - 1, map.size());
      Map.Entry<Long, String> first = map.entrySet().iterator().next();
      first.setValue("written"); //$NON-NLS-1$
      assertEquals("written", map.get(first.getKey())); //$NON-NLS-1$
      map.clear();
      assertEquals(0, map.size());
      assertFalse(map.containsKey(null));
   }

   private void check(String phase, XViewer viewer, Stats stats, long nanos, List<String> violations) {
      int resident = column(viewer, PRE_ID).getPreComputedValueMap().size();
      int alive = stats.alive();
      Runtime runtime = Runtime.getRuntime();
      long usedHeap = runtime.totalMemory() - runtime.freeMemory();
      System.out.println("XVIEWER_PRECOMPUTE phase=" + phase + " residentMapEntries=" + resident //$NON-NLS-1$ //$NON-NLS-2$
         + " reachableValues=" + alive + " populateCalls=" + stats.calls + " valuesComputed=" + stats.computed //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         + " materializedRows=" + viewer.getViewportMaterializedCount() + " usedHeapAfterGcKiB=" //$NON-NLS-1$ //$NON-NLS-2$
         + usedHeap / 1024 + " millis=" + nanos / 1_000_000); //$NON-NLS-1$
      if (resident > WINDOW) {
         violations.add(phase + ": " + resident + " precomputed values resident, window is " + WINDOW); //$NON-NLS-1$ //$NON-NLS-2$
      }
      if (alive > RETAINED_BOUND) {
         violations.add(phase + ": " + alive + " precomputed values still reachable, bound is " + RETAINED_BOUND); //$NON-NLS-1$ //$NON-NLS-2$
      }
   }

   private static XViewerColumn column(XViewer viewer, String id) {
      for (TreeColumn treeColumn : viewer.getTree().getColumns()) {
         XViewerColumn xCol = (XViewerColumn) treeColumn.getData();
         if (id.equals(xCol.getId())) {
            return xCol;
         }
      }
      throw new AssertionError("no column " + id); //$NON-NLS-1$
   }

   /** The header click of the original column handler: sort by the column. */
   private static void clickHeader(XViewer viewer, String id) {
      for (TreeColumn treeColumn : viewer.getTree().getColumns()) {
         if (id.equals(((XViewerColumn) treeColumn.getData()).getId())) {
            treeColumn.notifyListeners(SWT.Selection, new Event());
            return;
         }
      }
      throw new AssertionError("no column " + id); //$NON-NLS-1$
   }

   /** Root rows in the order the viewport bridge gives the native tree (same private member on master and head). */
   private static Object[] displayOrder(XViewer viewer) throws Exception {
      Object bridge = viewer.getContentProvider();
      Method children = bridge.getClass().getDeclaredMethod("children", Object.class); //$NON-NLS-1$
      children.setAccessible(true);
      return (Object[]) children.invoke(bridge, viewer.getInput());
   }

   private static String digest(Object[] rows) throws Exception {
      MessageDigest md = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
      for (Object row : rows) {
         md.update(String.valueOf(row).getBytes(StandardCharsets.UTF_8));
         md.update((byte) ',');
      }
      StringBuilder hex = new StringBuilder();
      for (byte b : md.digest()) {
         hex.append(String.format("%02x", Integer.valueOf(b & 0xff))); //$NON-NLS-1$
      }
      return hex.toString();
   }

   private static List<String> texts(Tree tree) {
      List<String> out = new ArrayList<>();
      for (int i = 0; i < tree.getItemCount(); i++) {
         out.add(tree.getItem(i).getText(0) + '|' + tree.getItem(i).getText(1));
      }
      return out;
   }

   private void pump() {
      for (int i = 0; i < 10_000 && display.readAndDispatch(); i++) {
         // drain pending paint and async work
      }
   }

   private XViewer viewer(int style, Stats stats) {
      Composite parent = new Composite(shell, SWT.NONE);
      parent.setLayout(new GridLayout(1, false));
      parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      XViewerColumn name =
         new XViewerColumn("tt.name", "Name", 120, XViewerAlign.Left, true, SortDataType.String, false, null); //$NON-NLS-1$ //$NON-NLS-2$
      XViewerFactory factory = new XViewerFactory("tt.precompute.viewport.test") { //$NON-NLS-1$
         @Override
         public boolean isAdmin() {
            return false;
         }
      };
      factory.registerColumns(name, new Pre(stats));
      XViewer viewer = new XViewer(parent, style | SWT.MULTI | SWT.FULL_SELECTION, factory);
      viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      viewer.setContentProvider(new Flat());
      viewer.setLabelProvider(new Labels(viewer, stats));
      viewer.setForcePend(true);
      return viewer;
   }

   /** What the precomputed column computed, and which of those values are still reachable. */
   static final class Stats {
      int calls;
      int computed;
      int labels;
      final List<WeakReference<String>> produced = new ArrayList<>();

      int alive() {
         int alive = produced.size();
         for (int attempt = 0; attempt < 10; attempt++) {
            System.gc();
            alive = 0;
            for (WeakReference<String> ref : produced) {
               if (ref.get() != null) {
                  alive++;
               }
            }
            if (alive <= RETAINED_BOUND) {
               break;
            }
            try {
               Thread.sleep(20);
            } catch (InterruptedException ex) {
               Thread.currentThread().interrupt();
               break;
            }
         }
         produced.removeIf(ref -> ref.get() == null);
         return alive;
      }
   }

   /**
    * A precomputed column over Integer rows: the value of row {@code id} is a fixed permutation of the ids, so sorting
    * by it reorders the rows. Each call computes new String objects, which the test tracks weakly.
    */
   static final class Pre extends XViewerColumn implements IXViewerPreComputedColumn {
      private final Stats stats;

      Pre(Stats stats) {
         super(PRE_ID, "Pre", 120, XViewerAlign.Left, true, SortDataType.String, false, null); //$NON-NLS-1$
         this.stats = stats;
      }

      static String value(int id) {
         String digits = Integer.toString((int) (id * 7919L % ROWS));
         StringBuilder value = new StringBuilder(7).append('v');
         for (int pad = digits.length(); pad < 6; pad++) {
            value.append('0');
         }
         return value.append(digits).toString();
      }

      @Override
      public Pre copy() {
         Pre copy = new Pre(stats);
         super.copy(this, copy);
         return copy;
      }

      @Override
      public Long getKey(Object obj) {
         return Long.valueOf(((Integer) obj).longValue());
      }

      @Override
      public void populateCachedValues(Collection<?> objects, Map<Long, String> preComputedValueMap) {
         stats.calls++;
         for (Object obj : objects) {
            String value = value(((Integer) obj).intValue());
            stats.computed++;
            stats.produced.add(new WeakReference<>(value));
            preComputedValueMap.put(getKey(obj), value);
         }
      }

      @Override
      public String getText(Object obj, Long key, String cachedValue) {
         return cachedValue;
      }
   }

   /** A flat model of Integer rows. */
   static final class Flat implements ITreeContentProvider {
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
         return parent instanceof Integer ? new Object[0] : getElements(parent);
      }

      @Override
      public Object getParent(Object element) {
         return null;
      }

      @Override
      public boolean hasChildren(Object element) {
         return false;
      }
   }

   /** Name cells show "n" + id; counts the precomputed cells the viewer asks text for. */
   static final class Labels extends XViewerLabelProvider {
      private final Stats stats;

      Labels(XViewer viewer, Stats stats) {
         super(viewer);
         this.stats = stats;
      }

      @Override
      public String getColumnText(Object element, int columnIndex) {
         if (getTreeColumnOffIndex(columnIndex) instanceof IXViewerPreComputedColumn) {
            stats.labels++;
         }
         return super.getColumnText(element, columnIndex);
      }

      @Override
      public Image getColumnImage(Object element, XViewerColumn xCol, int columnIndex) {
         return null;
      }

      @Override
      public String getColumnText(Object element, XViewerColumn xCol, int columnIndex) {
         return "n" + element; //$NON-NLS-1$
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
