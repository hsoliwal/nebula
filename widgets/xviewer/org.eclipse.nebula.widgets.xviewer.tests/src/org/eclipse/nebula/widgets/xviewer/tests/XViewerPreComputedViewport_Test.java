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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.widgets.xviewer.IXViewerPreComputedColumn;
import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.XViewerFactory;
import org.eclipse.nebula.widgets.xviewer.XViewerLabelProvider;
import org.eclipse.nebula.widgets.xviewer.XViewerTreeReport;
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
import org.eclipse.swt.widgets.TreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Precomputed columns of a viewport XViewer ({@code SWT.VIRTUAL} tree). By default the viewer keeps master's full
 * precompute: one value per input row in the column's map, computed in a background job. With bounded precompute
 * (opt-in, {@code XViewer.setBoundedPreCompute(true)} or {@code -Dnebula.xviewer.boundedPreCompute=true}) the values
 * computed by {@link IXViewerPreComputedColumn#populateCachedValues} stay resident only for a bounded window of rows,
 * while sort and filter results, painted text and the widget tree stay those of the default path. Each viewer opts in
 * or out through the setter, resolved reflectively, so this class also runs against master, where it does not exist
 * and the viewer keeps the full map; each phase prints an {@code XVIEWER_PRECOMPUTE} line, so the same test measures
 * master and head.
 */
public class XViewerPreComputedViewport_Test {
   private static final int ROWS = 100_000;
   /** Precomputed values a bounded viewport keeps per column, whatever the input size (the window). */
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
    * value stays reachable. Opts in through the setter.
    */
   @Test
   public void precomputedValuesStayBoundedAfterLoadScrollSortAndFilter() {
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, true);
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
    * painted cells show the values the rows were ordered by. Bounded through the system property; passes on master
    * (full map) and head (bounded); the printed order digest is the same on both.
    */
   @Test
   public void sortAndFilterByPrecomputedColumnMatchColumnOrder() throws Exception {
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, true);
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
    * The eager tree keeps its original precompute (one value per input row) even when bounding is asked for, and the
    * bounded viewport shows the same rows in the same order with the same text after sorting and filtering by the
    * precomputed column.
    */
   @Test
   public void eagerAndViewportAgreeOnPrecomputedSortAndFilter() {
      int rows = 2_000;
      XViewer eager = viewer(SWT.NONE, new Stats(), true);
      XViewer lazy = viewer(SWT.VIRTUAL, new Stats(), true);
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
    * One scroll frame over 100,000 rows of a bounded viewport paints, labels and computes only the rows it shows (plus
    * overscan and one block of following rows), and the viewer adds no child control or shell. Passes on master and
    * head.
    */
   @Test
   public void scrollFramePaintsVisibleCellsOnlyWithoutChildControls() {
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, true);
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
    * The default path is master's, even on a {@code SWT.VIRTUAL} tree: the background job (no forced pend) calls
    * {@code populateCachedValues} once, off the UI thread, with every input object, into the column's own map, which
    * keeps a value per row and its identity; a map set with {@code setPreComputedValueMap} is the one read. Passes on
    * master and head alike.
    */
   @Test
   public void defaultBackgroundLoadKeepsMastersFullSnapshot() {
      int rows = 2_000;
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, false);
      viewer.setForcePend(false);
      Thread ui = display.getThread();
      viewer.setInputXViewer(Flat.roots(rows));
      shell.open();
      Tree tree = viewer.getTree();
      long deadline = System.currentTimeMillis() + 60_000;
      while (tree.getItemCount() != rows && System.currentTimeMillis() < deadline) {
         if (!display.readAndDispatch()) {
            sleep(10);
         }
      }
      pump();
      assertEquals(rows, tree.getItemCount());
      assertEquals("one background call", 1, stats.calls); //$NON-NLS-1$
      assertEquals("with every input object", rows, stats.computed); //$NON-NLS-1$
      assertNotSame("off the UI thread", ui, stats.lastThread); //$NON-NLS-1$
      XViewerColumn pre = column(viewer, PRE_ID);
      Map<Long, String> map = pre.getPreComputedValueMap();
      assertEquals("a value per input row", rows, map.size()); //$NON-NLS-1$
      for (int id = 0; id < rows; id++) {
         assertEquals(Pre.value(id), map.get(Long.valueOf(id)));
      }
      assertEquals(Pre.value(0), tree.getItem(0).getText(1));
      assertEquals(Pre.value(rows - 1), tree.getItem(rows - 1).getText(1));
      assertEquals("rows were read from the map, not recomputed", 1, stats.calls); //$NON-NLS-1$
      viewer.refresh();
      pump();
      assertSame("refresh keeps the column's map", map, pre.getPreComputedValueMap()); //$NON-NLS-1$

      Map<Long, String> mine = new HashMap<>();
      mine.put(Long.valueOf(0), "mine0"); //$NON-NLS-1$
      pre.setPreComputedValueMap(mine);
      viewer.refresh();
      pump();
      assertSame(mine, pre.getPreComputedValueMap());
      assertEquals("mine0", tree.getItem(0).getText(1)); //$NON-NLS-1$
   }

   /**
    * The reviewer's harness, bounded: 5,000 rows sorted by a column whose values come from a counter (a value
    * changes each time it is computed). The painted rows must be the values the rows were sorted on, as on master,
    * after a sort from the top and after a reverse sort while scrolled to the middle. Fails on the first bounded head
    * (painted v004729.. while sorted on v000000..).
    */
   @Test
   public void boundedSortPaintsTheSortKeys() {
      int rows = 5_000;
      Counter counter = new Counter(rows);
      XViewer viewer = viewer(SWT.VIRTUAL, counter, true, new CounterColumn(counter));
      viewer.setInputXViewer(Flat.roots(rows));
      shell.open();
      pump();
      Tree tree = viewer.getTree();

      clickHeader(viewer, PRE_ID);
      pump();
      assertPaintedSortKeys(tree, 0, counter);

      tree.setTopItem(tree.getItem(rows / 2));
      tree.update();
      pump();
      clickHeader(viewer, PRE_ID);
      pump();
      int top = topIndex(tree);
      System.out.println("XVIEWER_PRECOMPUTE counter top=" + top + " fullBatches=" + counter.fullBatches); //$NON-NLS-1$ //$NON-NLS-2$
      assertPaintedSortKeys(tree, top, counter);
   }

   /**
    * With bounding on, a map the caller sets with {@code setPreComputedValueMap} stays the column's map and its values
    * are painted, as on master.
    */
   @Test
   public void boundedKeepsCallerInstalledMap() {
      int rows = 2_000;
      XViewer viewer = viewer(SWT.VIRTUAL, new Stats(), true);
      viewer.setInputXViewer(Flat.roots(rows));
      shell.open();
      pump();
      XViewerColumn pre = column(viewer, PRE_ID);
      Map<Long, String> mine = new HashMap<>();
      mine.put(Long.valueOf(0), "mine0"); //$NON-NLS-1$
      pre.setPreComputedValueMap(mine);
      viewer.refresh();
      pump();
      assertSame("the caller's map is kept", mine, pre.getPreComputedValueMap()); //$NON-NLS-1$
      assertEquals("mine0", viewer.getTree().getItem(0).getText(1)); //$NON-NLS-1$
      assertEquals(Pre.value(1), viewer.getTree().getItem(1).getText(1));
   }

   /**
    * With bounding on, actions that read the cells of many rows (refresh of one column, the tree report) compute the
    * rows in one batched call instead of one call per row, with correct values. 1,500 materialized rows exceed the
    * window, so a row-by-row walk would evict each value before reading it again.
    */
   @Test
   public void boundedItemWalksComputeOneBatch() throws Exception {
      int rows = 1_500;
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, true);
      viewer.setInputXViewer(Flat.roots(rows));
      shell.open();
      pump();
      Tree tree = viewer.getTree();
      for (int row = 0; row < rows; row++) {
         assertEquals(Pre.value(row), tree.getItem(row).getText(1));
      }
      int calls = stats.calls;
      viewer.refreshColumn(PRE_ID);
      System.out.println("XVIEWER_PRECOMPUTE walk=refreshColumn calls=" + (stats.calls - calls)); //$NON-NLS-1$
      assertTrue("refreshColumn calls: " + (stats.calls - calls), stats.calls - calls <= 1); //$NON-NLS-1$
      for (int row = 0; row < rows; row += 97) {
         assertEquals(Pre.value(row), tree.getItem(row).getText(1));
      }
      calls = stats.calls;
      String html = new XViewerTreeReport(viewer).getHtml();
      System.out.println("XVIEWER_PRECOMPUTE walk=report calls=" + (stats.calls - calls)); //$NON-NLS-1$
      assertTrue("report calls: " + (stats.calls - calls), stats.calls - calls <= 1); //$NON-NLS-1$
      for (int row = 0; row < rows; row += 97) {
         assertTrue(Pre.value(row), html.contains(Pre.value(row)));
      }
   }

   /**
    * With a caller-supplied lazy content provider bounding does not apply: the column keeps the full background map
    * and shown rows are read from it, as on master.
    */
   @Test
   public void boundedRequestWithLazyProviderKeepsFullMap() {
      int rows = 2_000;
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, true);
      Integer[] roots = Flat.roots(rows);
      viewer.setContentProvider(new LazyRoots(viewer, roots));
      viewer.setInputXViewer(roots);
      shell.open();
      pump();
      Tree tree = viewer.getTree();
      assertEquals(rows, tree.getItemCount());
      assertEquals(Pre.value(0), tree.getItem(0).getText(1));
      assertEquals(Pre.value(rows - 1), tree.getItem(rows - 1).getText(1));
      assertEquals("one background call, no per-row recompute", 1, stats.calls); //$NON-NLS-1$
      assertEquals(rows, column(viewer, PRE_ID).getPreComputedValueMap().size());
   }

   /**
    * The bounded window is a {@link Map}: it keeps at most the window, keeps what is used, maps each resident key to
    * its own value, and supports null keys and values, removal and write-through entries like
    * {@link java.util.HashMap}. Fails on master, which has no such class.
    */
   @Test
   public void boundedWindowIsABoundedMap() throws Exception {
      Class<?> type = Class.forName("org.eclipse.nebula.widgets.xviewer.XViewerPreComputedWindow", true, //$NON-NLS-1$
         XViewer.class.getClassLoader());
      java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<Long, String> map = (Map<Long, String>) constructor.newInstance();
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

      // random 64-bit keys against a reference: a resident key always maps to its latest value
      java.util.Random random = new java.util.Random(20261127L);
      Map<Long, String> reference = new HashMap<>();
      long[] used = new long[4096];
      for (int op = 0; op < 20_000; op++) {
         long key = op % 4 == 3 ? used[random.nextInt(Math.min(op, used.length))] : random.nextLong();
         used[op % used.length] = key;
         if (op % 5 == 4) {
            map.remove(Long.valueOf(key));
            reference.remove(Long.valueOf(key));
            assertFalse("removed key " + key, map.containsKey(Long.valueOf(key))); //$NON-NLS-1$
            continue;
         }
         String value = "r" + op; //$NON-NLS-1$
         map.put(Long.valueOf(key), value);
         reference.put(Long.valueOf(key), value);
         assertEquals("the key just put is resident", value, map.get(Long.valueOf(key))); //$NON-NLS-1$
         long probe = used[random.nextInt(Math.min(op + 1, used.length))];
         if (map.containsKey(Long.valueOf(probe))) {
            assertEquals(reference.get(Long.valueOf(probe)), map.get(Long.valueOf(probe)));
         }
      }
      assertTrue("resident values: " + map.size(), map.size() <= WINDOW); //$NON-NLS-1$
      int resident = 0;
      for (Map.Entry<Long, String> entry : map.entrySet()) {
         assertEquals(reference.get(entry.getKey()), entry.getValue());
         resident++;
      }
      assertEquals(map.size(), resident);
   }

   /**
    * Bounded, with a filter on a hierarchical model: painting a root row asks whether its 300 children pass the filter,
    * a filter pass over them. Those probes must not push the visible root rows' values out of the window, so each
    * visible root row shows the value it was filtered on. Fails on 1d2c823 (probe passes seeded the window).
    */
   @Test
   public void boundedFilteredTreePaintsTheFilterKeys() {
      int roots = 500;
      Counter counter = new Counter(roots);
      XViewer viewer = viewer(SWT.VIRTUAL, counter, true, new CounterColumn(counter), new Tree300(roots));
      viewer.getCustomizeMgr().setColumnFilterText(PRE_ID, "v"); //$NON-NLS-1$
      viewer.setInputXViewer(Flat.roots(roots));
      shell.open();
      pump();
      Tree tree = viewer.getTree();
      assertEquals(roots, tree.getItemCount());
      for (int row = 0; row < 20; row++) {
         TreeItem item = tree.getItem(row);
         String key = counter.lastFull.get(Long.valueOf(((Integer) item.getData()).longValue()));
         assertEquals("root row " + row + " shows the value it was filtered on", key, item.getText(1)); //$NON-NLS-1$ //$NON-NLS-2$
      }
   }

   /**
    * Bounded on the default background-job path (no forced pend): the staged values computed off the UI thread are
    * handed to the load on the UI thread; the column's own map stays empty and only a window stays resident. Fails on
    * master, which keeps the full map.
    */
   @Test
   public void boundedBackgroundLoadHandsStagedValuesToTheUiThread() {
      int rows = 2_000;
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, true);
      viewer.setForcePend(false);
      viewer.setInputXViewer(Flat.roots(rows));
      shell.open();
      awaitRows(viewer.getTree(), rows);
      assertEquals("one background call", 1, stats.calls); //$NON-NLS-1$
      assertNotSame("off the UI thread", display.getThread(), stats.lastThread); //$NON-NLS-1$
      assertEquals(Pre.value(0), viewer.getTree().getItem(0).getText(1));
      assertEquals(0, column(viewer, PRE_ID).getPreComputedValueMap().size());
      int resident = resident(viewer);
      assertTrue("resident values: " + resident, resident > 0 && resident <= WINDOW); //$NON-NLS-1$
   }

   /**
    * Bounding turned off while a bounded background load runs: the load hands the staged values to the column's own
    * map, as by default, instead of leaving it empty. Passes on master (no bounding at all).
    */
   @Test
   public void boundingTurnedOffDuringBackgroundLoadKeepsFullMap() {
      int rows = 2_000;
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, true);
      viewer.setForcePend(false);
      viewer.setInputXViewer(Flat.roots(rows));
      setBounded(viewer, false);
      shell.open();
      awaitRows(viewer.getTree(), rows);
      assertEquals(rows, column(viewer, PRE_ID).getPreComputedValueMap().size());
      assertEquals(Pre.value(rows - 1), viewer.getTree().getItem(rows - 1).getText(1));
      assertEquals("rows read from the full map", 1, stats.calls); //$NON-NLS-1$
   }

   /**
    * With bounding off (the default) the item walks behave as on master: refreshColumn and the tree report read the
    * full map and make no populate call.
    */
   @Test
   public void defaultItemWalksMakeNoPopulateCall() throws Exception {
      int rows = 600;
      Stats stats = new Stats();
      XViewer viewer = viewer(SWT.VIRTUAL, stats, false);
      viewer.setInputXViewer(Flat.roots(rows));
      shell.open();
      pump();
      Tree tree = viewer.getTree();
      for (int row = 0; row < rows; row++) {
         assertEquals(Pre.value(row), tree.getItem(row).getText(1));
      }
      int calls = stats.calls;
      viewer.refreshColumn(PRE_ID);
      String html = new XViewerTreeReport(viewer).getHtml();
      assertEquals("no populate call from the walks", calls, stats.calls); //$NON-NLS-1$
      assertTrue(html.contains(Pre.value(rows - 1)));
   }

   /**
    * Bounded, hierarchical, with a filter: painting a root row probes whether its children pass the filter, and that
    * probe keeps no values. Expanding the row must order its children again for display, so each shown child row
    * shows the value the filter read for it. Fails on 576c229 (the probe's cached order was reused).
    */
   @Test
   public void boundedExpandedChildrenPaintTheirFilterKeys() {
      int roots = 200;
      Counter counter = new Counter(roots);
      XViewer viewer = viewer(SWT.VIRTUAL, counter, true, new CounterColumn(counter), new Tree300(roots));
      Map<Object, String> keys = new HashMap<>();
      viewer.addFilter(new org.eclipse.jface.viewers.ViewerFilter() {
         @Override
         public boolean select(Viewer v, Object parent, Object element) {
            keys.put(element, ((XViewer) v).getColumnText(element, 1));
            return true;
         }
      });
      viewer.setInputXViewer(Flat.roots(roots));
      shell.open();
      pump();
      viewer.setExpandedState(Integer.valueOf(0), true);
      pump();
      TreeItem root = viewer.getTree().getItem(0);
      assertTrue(root.getExpanded());
      for (int row = 0; row < 12; row++) {
         TreeItem child = root.getItem(row);
         assertEquals("child row " + row + " shows the value it was filtered on", keys.get(child.getData()), //$NON-NLS-1$ //$NON-NLS-2$
            child.getText(1));
      }
   }

   /** An invalid {@code nebula.xviewer.boundedPreCompute} value leaves bounding off and is logged. */
   @Test
   public void invalidBoundedPreComputePropertyIsOffAndLogged() throws Exception {
      Method parse = XViewer.class.getDeclaredMethod("parseBoundedPreCompute", String.class); //$NON-NLS-1$
      parse.setAccessible(true);
      java.util.logging.Logger logger = java.util.logging.Logger.getLogger("XViewer Log"); //$NON-NLS-1$
      List<String> logged = new ArrayList<>();
      java.util.logging.Handler handler = new java.util.logging.Handler() {
         @Override
         public void publish(java.util.logging.LogRecord record) {
            logged.add(record.getMessage());
         }

         @Override
         public void flush() {
            // nothing buffered
         }

         @Override
         public void close() {
            // nothing held
         }
      };
      logger.addHandler(handler);
      try {
         assertEquals(Boolean.TRUE, parse.invoke(null, "true")); //$NON-NLS-1$
         assertEquals(Boolean.TRUE, parse.invoke(null, " TRUE ")); //$NON-NLS-1$
         assertEquals(Boolean.FALSE, parse.invoke(null, (Object) null));
         assertEquals(Boolean.FALSE, parse.invoke(null, "false")); //$NON-NLS-1$
         assertTrue("valid values are not logged: " + logged, logged.isEmpty()); //$NON-NLS-1$
         assertEquals(Boolean.FALSE, parse.invoke(null, "viewport")); //$NON-NLS-1$
         assertEquals(1, logged.size());
         assertTrue(logged.get(0), logged.get(0).contains("nebula.xviewer.boundedPreCompute") //$NON-NLS-1$
            && logged.get(0).contains("viewport")); //$NON-NLS-1$
      } finally {
         logger.removeHandler(handler);
      }
   }

   private void awaitRows(Tree tree, int rows) {
      long deadline = System.currentTimeMillis() + 60_000;
      while (tree.getItemCount() != rows && System.currentTimeMillis() < deadline) {
         if (!display.readAndDispatch()) {
            sleep(10);
         }
      }
      pump();
      assertEquals(rows, tree.getItemCount());
   }

   private static void assertPaintedSortKeys(Tree tree, int top, Counter counter) {
      String previous = null;
      for (int row = top; row < top + 12 && row < tree.getItemCount(); row++) {
         TreeItem item = tree.getItem(row);
         String painted = item.getText(1);
         String sortKey = counter.lastFull.get(Long.valueOf(((Integer) item.getData()).longValue()));
         assertEquals("row " + row + " shows the value it was sorted on", sortKey, painted); //$NON-NLS-1$ //$NON-NLS-2$
         if (previous != null) {
            int order = previous.compareTo(painted);
            assertTrue("rows " + (row - 1) + ".." + row + " in sort order: " + previous + ", " + painted, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
               order != 0 && Integer.signum(order) == counter.direction(tree));
         }
         previous = painted;
      }
   }

   private static int topIndex(Tree tree) {
      TreeItem top = tree.getTopItem();
      return top == null ? 0 : tree.indexOf(top);
   }

   private void check(String phase, XViewer viewer, Stats stats, long nanos, List<String> violations) {
      int resident = resident(viewer);
      int alive = stats.alive();
      Runtime runtime = Runtime.getRuntime();
      long usedHeap = runtime.totalMemory() - runtime.freeMemory();
      System.out.println("XVIEWER_PRECOMPUTE phase=" + phase + " residentValues=" + resident //$NON-NLS-1$ //$NON-NLS-2$
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

   /**
    * Precomputed values the viewer holds for the column: its own map plus, where the viewer keeps bounded windows (a
    * private field on head, absent on master), their values.
    */
   private static int resident(XViewer viewer) {
      int resident = column(viewer, PRE_ID).getPreComputedValueMap().size();
      try {
         Field field = XViewer.class.getDeclaredField("preComputeWindows"); //$NON-NLS-1$
         field.setAccessible(true);
         for (Object window : ((Map<?, ?>) field.get(viewer)).values()) {
            resident += ((Map<?, ?>) window).size();
         }
      } catch (NoSuchFieldException ex) {
         // master: the column's map is all there is
      } catch (IllegalAccessException ex) {
         throw new AssertionError(ex);
      }
      return resident;
   }

   /** Opts in through the setter; on master, which has none, the viewer keeps its full map. */
   private static void setBounded(XViewer viewer, boolean bounded) {
      try {
         Method setter = XViewer.class.getMethod("setBoundedPreCompute", boolean.class); //$NON-NLS-1$
         setter.invoke(viewer, Boolean.valueOf(bounded));
      } catch (NoSuchMethodException ex) {
         // master
      } catch (ReflectiveOperationException ex) {
         throw new AssertionError(ex);
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

   /** The header click of the original column handler: sort by the column, again to reverse. */
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

   private static void sleep(long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
      }
   }

   private XViewer viewer(int style, Stats stats, boolean bounded) {
      return viewer(style, stats, bounded, new Pre(stats));
   }

   /**
    * @param bounded sets bounded precompute on or off through the setter, so a run with
    * {@code nebula.xviewer.boundedPreCompute=true} keeps the default where a test needs it
    */
   private XViewer viewer(int style, Stats stats, boolean bounded, XViewerColumn pre) {
      return viewer(style, stats, bounded, pre, new Flat());
   }

   private XViewer viewer(int style, Stats stats, boolean bounded, XViewerColumn pre, ITreeContentProvider model) {
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
      factory.registerColumns(name, pre);
      XViewer viewer = new XViewer(parent, style | SWT.MULTI | SWT.FULL_SELECTION, factory);
      setBounded(viewer, bounded);
      viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      viewer.setContentProvider(model);
      viewer.setLabelProvider(new Labels(viewer, stats));
      viewer.setForcePend(true);
      return viewer;
   }

   /** What the precomputed column computed, and which of those values are still reachable. */
   static class Stats {
      int calls;
      int computed;
      int labels;
      Thread lastThread;
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
            sleep(20);
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
         return pad(id * 7919L % ROWS);
      }

      static String pad(long number) {
         String digits = Long.toString(number);
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
         stats.lastThread = Thread.currentThread();
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

   /** Values from a counter, as in the review harness: each computation of a row gives a new, larger value. */
   static final class Counter extends Stats {
      private final int rows;
      private int next;
      int fullBatches;
      /** The values of the most recent call that covered every row: what a load or a sort pass ordered on. */
      final Map<Long, String> lastFull = new HashMap<>();

      Counter(int rows) {
         this.rows = rows;
      }

      String nextValue() {
         return Pre.pad(next++);
      }

      /** +1 if the column now sorts descending (the previous value compares greater), else -1. */
      int direction(Tree tree) {
         for (TreeColumn treeColumn : tree.getColumns()) {
            XViewerColumn xCol = (XViewerColumn) treeColumn.getData();
            if (PRE_ID.equals(xCol.getId())) {
               return xCol.isSortForward() ? -1 : 1;
            }
         }
         return -1;
      }
   }

   static final class CounterColumn extends XViewerColumn implements IXViewerPreComputedColumn {
      private final Counter counter;

      CounterColumn(Counter counter) {
         super(PRE_ID, "Counter", 120, XViewerAlign.Left, true, SortDataType.String, false, null); //$NON-NLS-1$
         this.counter = counter;
      }

      @Override
      public CounterColumn copy() {
         CounterColumn copy = new CounterColumn(counter);
         super.copy(this, copy);
         return copy;
      }

      @Override
      public Long getKey(Object obj) {
         return Long.valueOf(((Integer) obj).longValue());
      }

      @Override
      public void populateCachedValues(Collection<?> objects, Map<Long, String> preComputedValueMap) {
         counter.calls++;
         boolean full = objects.size() == counter.rows;
         if (full) {
            counter.fullBatches++;
            counter.lastFull.clear();
         }
         for (Object obj : objects) {
            String value = counter.nextValue();
            counter.computed++;
            preComputedValueMap.put(getKey(obj), value);
            if (full) {
               counter.lastFull.put(getKey(obj), value);
            }
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

   /**
    * A caller-supplied lazy provider over flat roots, in input order; it also answers the plain tree protocol, which
    * XViewer's status line reads.
    */
   static final class LazyRoots implements ILazyTreeContentProvider, ITreeContentProvider {
      private final XViewer viewer;
      private final Integer[] roots;

      LazyRoots(XViewer viewer, Integer[] roots) {
         this.viewer = viewer;
         this.roots = roots;
      }

      @Override
      public void updateElement(Object parent, int index) {
         if (index >= 0 && index < roots.length && !(parent instanceof Integer)) {
            viewer.replace(parent, index, roots[index]);
            viewer.setChildCount(roots[index], 0);
         }
      }

      @Override
      public void updateChildCount(Object element, int currentChildCount) {
         int count = element instanceof Integer ? 0 : roots.length;
         if (count != currentChildCount) {
            viewer.setChildCount(element, count);
         }
      }

      @Override
      public Object getParent(Object element) {
         return element instanceof Integer ? viewer.getInput() : null;
      }

      @Override
      public Object[] getElements(Object input) {
         return roots;
      }

      @Override
      public Object[] getChildren(Object parent) {
         return parent instanceof Integer ? new Object[0] : roots;
      }

      @Override
      public boolean hasChildren(Object element) {
         return false;
      }

      @Override
      public void inputChanged(Viewer changed, Object oldInput, Object newInput) {
         // stateless
      }

      @Override
      public void dispose() {
         // stateless
      }
   }

   /** Integer roots 0..R-1, each with 300 children R + r*300 + j. */
   static final class Tree300 implements ITreeContentProvider {
      private static final int KIDS = 300;
      private final int roots;

      Tree300(int roots) {
         this.roots = roots;
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
         Object[] kids = new Object[KIDS];
         for (int j = 0; j < KIDS; j++) {
            kids[j] = Integer.valueOf(roots + id * KIDS + j);
         }
         return kids;
      }

      @Override
      public Object getParent(Object element) {
         int id = ((Integer) element).intValue();
         return id < roots ? null : Integer.valueOf((id - roots) / KIDS);
      }

      @Override
      public boolean hasChildren(Object element) {
         return ((Integer) element).intValue() < roots;
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
