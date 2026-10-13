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
package org.eclipse.nebula.widgets.xviewer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
      viewer.setInputXViewer(Model.roots(ROOTS));
      shell.open();
      pump();
      Tree tree = viewer.getTree();
      assertEquals(ROOTS, tree.getItemCount());
      int first = viewer.getViewportMaterializedCount();
      assertTrue("materialized after first paint: " + first, first > 0 && first <= MATERIALIZED_BOUND);

      tree.setTopItem(tree.getItem(ROOTS - 1));
      tree.update();
      pump();
      int scrolled = viewer.getViewportMaterializedCount();
      assertTrue("materialized after scroll to end: " + scrolled, scrolled - first <= MATERIALIZED_BOUND);
      assertEquals("n" + (ROOTS - 1), tree.getItem(ROOTS - 1).getText(0));

      Integer root = Integer.valueOf(ROOTS - 1);
      viewer.setExpandedState(root, true);
      pump();
      assertEquals(KIDS, tree.getItem(ROOTS - 1).getItemCount());
      assertTrue(viewer.getStatusLabel().getText().contains(String.valueOf(ROOTS + KIDS)));
      assertTrue(viewer.getViewportMaterializedCount() - scrolled <= MATERIALIZED_BOUND);
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
      pump();
      String before = state(viewer);
      String pixels = pixels(tree);

      tree.setTopItem(tree.getItem(19_999));
      tree.update();
      pump();
      tree.setTopItem(tree.getItem(0));
      tree.update();
      pump();
      assertEquals(before, state(viewer));
      assertEquals(pixels, pixels(tree));
   }

   private static String state(XViewer viewer) {
      Tree tree = viewer.getTree();
      StringBuilder s = new StringBuilder();
      s.append(((org.eclipse.jface.viewers.IStructuredSelection) viewer.getSelection()).toList());
      s.append('|').append(java.util.Arrays.asList(viewer.getExpandedElements()));
      s.append('|').append(tree.getTopItem().getText(0));
      s.append('|').append(viewer.getStatusLabel().getText());
      for (int i = 0; i < 8; i++) {
         s.append('|').append(tree.getItem(i).getText(0)).append(tree.getItem(i).getExpanded());
      }
      return s.toString();
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
