/*******************************************************************************
 * Copyright (c) 2004, 2007 Boeing.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Boeing - initial API and implementation
 *******************************************************************************/

package org.eclipse.nebula.widgets.xviewer;

import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreePathViewerSorter;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.nebula.widgets.xviewer.action.TableCustomizationAction;
import org.eclipse.nebula.widgets.xviewer.action.TableCustomizationDropDownAction;
import org.eclipse.nebula.widgets.xviewer.column.XViewerDaysTillTodayColumn;
import org.eclipse.nebula.widgets.xviewer.column.XViewerDiffsBetweenColumnsColumn;
import org.eclipse.nebula.widgets.xviewer.core.model.CustomizeData;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerColumn;
import org.eclipse.nebula.widgets.xviewer.customize.ColumnFilterDataUI;
import org.eclipse.nebula.widgets.xviewer.customize.CustomizeManager;
import org.eclipse.nebula.widgets.xviewer.customize.FilterDataUI;
import org.eclipse.nebula.widgets.xviewer.customize.SearchDataUI;
import org.eclipse.nebula.widgets.xviewer.edit.XViewerEditAdapter;
import org.eclipse.nebula.widgets.xviewer.util.Pair;
import org.eclipse.nebula.widgets.xviewer.util.internal.ElapsedTime;
import org.eclipse.nebula.widgets.xviewer.util.internal.ElapsedTime.Units;
import org.eclipse.nebula.widgets.xviewer.util.internal.HtmlUtil;
import org.eclipse.nebula.widgets.xviewer.util.internal.XViewerLib;
import org.eclipse.nebula.widgets.xviewer.util.internal.XViewerLog;
import org.eclipse.nebula.widgets.xviewer.util.internal.XViewerMenuDetectListener;
import org.eclipse.nebula.widgets.xviewer.util.internal.XViewerMouseListener;
import org.eclipse.nebula.widgets.xviewer.util.internal.dialog.HtmlDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;

/**
 * @author Donald G. Dunne
 */
public class XViewer extends TreeViewer {

   public static final String MENU_GROUP_PRE = "XVIEWER MENU GROUP PRE"; //$NON-NLS-1$
   public static final String MENU_GROUP_POST = "XVIEWER MENU GROUP POST"; //$NON-NLS-1$
   private StyledText filterText;
   private final MenuManager menuManager;
   private static boolean ctrlKeyDown = false;
   private static boolean altKeyDown = false;
   protected final IXViewerFactory xViewerFactory;
   private final FilterDataUI filterDataUI;
   private final SearchDataUI searchDataUI;
   private final ColumnFilterDataUI columnFilterDataUI;
   /* live XViewers sharing the Display-wide Ctrl/Alt key filters: the first installs them, the last removes them */
   private static int keyFilterUsers;
   private boolean usesKeyFilters;
   private XViewerGradient xViewerGradient = null;
   private XViewerEditAdapter editAdapter = null;
   private boolean columnMultiEditEnabled = false;
   private CustomizeManager customizeMgr;
   private TreeColumn rightClickSelectedColumn = null;
   private Integer rightClickSelectedColumnNum = null;
   private TreeItem rightClickSelectedItem = null;
   private Color searchColor;
   private boolean forcePend = false;
   private static final Map<Composite, Composite> parentToTopComposites = new HashMap<>();
   private boolean debugLoading = "true".equals(System.getProperty("DebugLoading"));
   private final Map<String, Long> preComputeElapsedTime = new HashMap<>();
   // viewport state; objects exist only for rows the native tree or the API asked for
   private XViewerViewportContentProvider viewport;
   private final Set<TreeItem> viewportExpanded = new HashSet<>();
   private Font statusBoldFont;
   private boolean statusLayoutDirty;
   /** A status update arrived while the status line was hidden; computed on its first paint or read. */
   private boolean statusTextDirty;
   /** System property giving the initial {@link #setBoundedPreCompute(boolean)} of new viewers; read once. */
   private static final String BOUNDED_PRECOMPUTE_PROPERTY = "nebula.xviewer.boundedPreCompute"; //$NON-NLS-1$
   private static final boolean BOUNDED_PRECOMPUTE_DEFAULT = readBoundedPreComputeDefault();
   /** Precomputed values are bounded to a window of rows when the viewer is a viewport; off by default. */
   private boolean boundedPreCompute = BOUNDED_PRECOMPUTE_DEFAULT;
   /** Rows computed in one call when the native tree asks for a row whose precomputed value is not resident. */
   static final int PRECOMPUTE_BLOCK = 64;
   /** First rows of a parent whose pass values are kept when a load, sort or filter pass ends. */
   static final int PRECOMPUTE_SEED = 256;
   /** Rows from the top row down whose pass values are kept when a root sort or filter pass ends. */
   static final int PRECOMPUTE_BAND = 192;
   /** Resident precomputed values per column while bounded; the columns' own maps are left to the caller. */
   private final Map<XViewerColumn, XViewerPreComputedWindow> preComputeWindows = new IdentityHashMap<>();
   /** Nesting depth of full-data passes; the outermost pass drops the transient maps. */
   private int preComputePassDepth;
   /** Transient full maps serving each precomputed column during the current pass. */
   private final Map<XViewerColumn, Map<Long, String>> passValues = new IdentityHashMap<>();

   public XViewer(Composite parent, int style, IXViewerFactory xViewerFactory) {
      this(parent, style, xViewerFactory, false, false);
   }

   public XViewer(Tree tree, IXViewerFactory xViewerFactory) {
      this(tree, xViewerFactory, false, false);
   }

   public XViewer(Composite parent, int style, IXViewerFactory xViewerFactory, boolean filterRealTime, boolean searchRealTime) {
      this(new Tree(createTopComposite(xViewerFactory, parent), style), xViewerFactory, filterRealTime, searchRealTime);
   }

   /**
    * Create top if search block should be at top; return parent regardless but cache composite to use
    */
   private static Composite createTopComposite(IXViewerFactory xViewerFactory, Composite parent) {
      if (xViewerFactory.isSearhTop()) {
         Composite topComposite = new Composite(parent, SWT.NONE);
         topComposite.setLayout(XViewerLib.getZeroMarginLayout(11, false));
         topComposite.setLayoutData(new GridData(SWT.FILL, SWT.None, true, false));
         parentToTopComposites.put(parent, topComposite);
      } else {
         parentToTopComposites.put(parent, parent);
      }
      return parent;
   }

   public XViewer(Tree tree, IXViewerFactory xViewerFactory, boolean filterRealTime, boolean searchRealTime) {
      super(tree);
      this.xViewerFactory = xViewerFactory;
      this.menuManager = new MenuManager();
      this.menuManager.setRemoveAllWhenShown(true);
      this.menuManager.createContextMenu(tree.getParent());
      if (xViewerFactory.isFilterUiAvailable()) {
         this.filterDataUI = new FilterDataUI(this, filterRealTime);
      } else {
         this.filterDataUI = null;
      }
      if (xViewerFactory.isSearchUiAvailable()) {
         this.searchDataUI = new SearchDataUI(this, searchRealTime);
      } else {
         this.searchDataUI = null;
      }
      this.columnFilterDataUI = new ColumnFilterDataUI(this);
      try {
         customizeMgr = new CustomizeManager(this, xViewerFactory);
      } catch (Exception ex) {
         XViewerLog.logAndPopup(Activator.class, Level.SEVERE, ex);
      }
      createSupportWidgets(parentToTopComposites.get(tree.getParent()));
      parentToTopComposites.remove(tree.getParent());

      tree.setHeaderVisible(true);
      tree.setLinesVisible(true);
      if ((tree.getStyle() & SWT.VIRTUAL) != 0) {
         tree.addListener(SWT.Expand, event -> trackExpanded(event.item));
      }
      tree.addListener(SWT.Dispose, event -> disposeStatusFont());
      setUseHashlookup(true);
      setupCtrlKeyListener();
      if (xViewerFactory.isCellGradientOn()) {
         xViewerGradient = new XViewerGradient(this);
      }
   }

   @Override
   protected ColumnViewerEditor createViewerEditor() {
      return super.createViewerEditor();
   }
   private static List<XViewerComputedColumn> computedColumns = new ArrayList<>();

   public Collection<XViewerComputedColumn> getComputedColumns() {
      if (computedColumns.size() == 0) {
         computedColumns.add(new XViewerDaysTillTodayColumn());
         computedColumns.add(new XViewerDiffsBetweenColumnsColumn());
      }
      return computedColumns;
   }

   public Collection<XViewerComputedColumn> getComputedColumns(XViewerColumn xCol) {
      List<XViewerComputedColumn> matchCols = new ArrayList<>();
      for (XViewerColumn computedCol : getComputedColumns()) {
         if (((XViewerComputedColumn) computedCol).isApplicableFor(xCol)) {
            matchCols.add((XViewerComputedColumn) computedCol);
         }
      }
      return matchCols;
   }

   public void dispose() {
      if (filterText != null && !filterText.isDisposed()) {
         filterText.dispose();
      }
      if (searchComp != null && !searchComp.isDisposed()) {
         searchComp.dispose();
      }

      // the shared Display filters leave with the last live XViewer, not with the first one //FLLDCIM: MARK — every-widget XViewer key filters
      if (usesKeyFilters) {
         usesKeyFilters = false;
         if (--keyFilterUsers == 0) {
            Display.getCurrent().removeFilter(SWT.KeyDown, displayKeysListener);
            Display.getCurrent().removeFilter(SWT.KeyUp, displayKeysListener);
            Display.getCurrent().removeFilter(SWT.FocusOut, displayFocusListener);
         }
      }

      //Dispose Menu
      if (menuManager != null) {
         menuManager.removeAll();
         menuManager.dispose();
      }
      disposeStatusFont();
   }

   private void disposeStatusFont() {
      if (statusBoldFont != null && !statusBoldFont.isDisposed()) {
         statusBoldFont.dispose();
      }
      statusBoldFont = null;
   }

   @Override
   public void setLabelProvider(IBaseLabelProvider labelProvider) {
      if (!(labelProvider instanceof IXViewerLabelProvider)) {
         throw new IllegalArgumentException(
            "Label Provider must extend XViewerLabelProvider or XViewerStyledTextLabelProvider"); //$NON-NLS-1$
      }
      super.setLabelProvider(labelProvider);
   }

   public void addCustomizeToViewToolbar(final ViewPart viewPart) {
      addCustomizeToViewToolbar(viewPart.getViewSite().getActionBars().getToolBarManager());
   }

   public void updateMenuActionsForTable() {
      // provided for subclass implementation
   }

   public void updateMenuActionsForHeader() {
      // provided for subclass implementation
   }

   public Action getCustomizeAction() {
      return new TableCustomizationDropDownAction(this);
   }

   public Action getCustomizeActionWithoutDropDown() {
      return new TableCustomizationAction(this);
   }

   public void addCustomizeToViewToolbar(IToolBarManager toolbarManager) {
      toolbarManager.add(new TableCustomizationDropDownAction(this));
   }

   protected void createSupportWidgets(Composite parent) {
      searchColor = Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
      searchComp = null;
      if (searchDataUI != null || filterDataUI != null || xViewerFactory.isLoadedStatusLabelAvailable()) {
         searchComp = new Composite(parent, SWT.NONE);
         searchComp.setLayout(XViewerLib.getZeroMarginLayout(11, false));
         searchComp.setLayoutData(new GridData(SWT.FILL, SWT.None, true, false));

         if (filterDataUI != null) {
            filterDataUI.createWidgets(searchComp);
            Label sep1 = new Label(searchComp, SWT.SEPARATOR);
            GridData gd = new GridData(SWT.RIGHT, SWT.NONE, false, false);
            gd.heightHint = 16;
            sep1.setLayoutData(gd);
         }

         if (searchDataUI != null) {
            searchDataUI.createWidgets(searchComp);
            Label sep2 = new Label(searchComp, SWT.SEPARATOR);
            GridData gd = new GridData(SWT.RIGHT, SWT.NONE, false, false);
            gd.heightHint = 16;
            sep2.setLayoutData(gd);
         }

         if (xViewerFactory.isLoadedStatusLabelAvailable()) {
            // the status line is a helper surface and never takes focus
            filterText = new StyledText(searchComp, SWT.NO_FOCUS);
            filterText.addListener(SWT.Paint, event -> catchUpStatusLayout());
            filterText.setText(" "); //$NON-NLS-1$
            filterText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
            filterText.addListener(SWT.MouseUp, getCustomizationMouseListener());
         }
      }

      this.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(org.eclipse.jface.viewers.DoubleClickEvent event) {
            handleDoubleClick();
         };
      });
      mouseListener = new XViewerMouseListener(this);
      getTree().addMouseListener(mouseListener);
      getTree().addListener(SWT.MenuDetect, new XViewerMenuDetectListener(this));
      getTree().setMenu(getMenuManager().getMenu());
      columnFilterDataUI.createWidgets();

      customizeMgr.loadCustomization();
   }

   public void handleDoubleClick(TreeColumn col, TreeItem item) {
      // provided for subclass implementation
   }

   public void handleDoubleClick() {
      // provided for subclass implementation
   }

   public int getCurrentColumnWidth(XViewerColumn xCol) {
      for (TreeColumn col : getTree().getColumns()) {
         if (col.getText().equals(xCol.getName())) {
            return col.getWidth();
         }
      }
      return 0;
   }

   /**
    * Called to set the input to the XViewer. This method MUST be used to ensure that XViewer loads properly. Especially
    * with the use of IXViewerPreComputedColumn.
    */
   public final void setInputXViewer(Object input) {
      // Allow pre-computed columns to prior to supplying Viewer with new input
      refreshColumnsWithPreCompute(input);
   }

   /**
    * This is called after all preComputed columns are done loading.
    */
   private void superInputChanged(Object input) {
      if (getTree() != null && !getTree().isDisposed()) {
         super.setInput(input);
      }
   }

   private List<Object> getInputObjects(Object input) {
      List<Object> objects = new ArrayList<>();
      if (input instanceof Collection) {
         Collection<?> collection = (Collection<?>) input;
         for (Object obj : collection) {
            objects.add(obj);
         }
      } else if (input instanceof Object[]) {
         Object[] arr = (Object[]) input;
         for (Object obj : arr) {
            objects.add(obj);
         }
      }
      return objects;
   }

   public void refreshColumnsWithPreCompute() {
      refreshColumnsWithPreCompute(getInput());
   }

   public void refreshColumnsWithPreCompute(final Object input) {
      final List<Object> inputObjects = getInputObjects(input);
      final XViewer xViewer = this;
      this.loading = true;

      if (!inputObjects.isEmpty()) {
         // a viewport stages the full precompute for the load only; afterwards a bounded window stays resident
         final Map<XViewerColumn, Map<Long, String>> staged = boundsPreCompute() ? new IdentityHashMap<>() : null;
         if (forcePend) {
            performPreCompute(inputObjects, staged);
            performLoad(inputObjects, xViewer, staged);
         } else {
            Job job = new Job("Refreshing Columns") {

               @Override
               protected IStatus run(IProgressMonitor monitor) {
                  ElapsedTime time = new ElapsedTime("performPreCompute");
                  performPreCompute(inputObjects, staged);
                  time.end(Units.SEC);
                  return Status.OK_STATUS;
               }

            };
            job.setSystem(false);
            job.addJobChangeListener(new JobChangeAdapter() {

               @Override
               public void done(IJobChangeEvent event) {
                  Display.getDefault().asyncExec(() -> {
                     ElapsedTime time = new ElapsedTime("performLoad");
                     performLoad(input, xViewer, staged);
                     time.end(Units.SEC);
                  });
               }
            });
            job.schedule();
         }
      }
   }

   /**
    * @param staged null to fill each column's own map (eager tree); otherwise receives one transient full map per
    * column, used by the load and then dropped (viewport)
    */
   private void performPreCompute(final List<Object> inputObjects, Map<XViewerColumn, Map<Long, String>> staged) {
      List<XViewerColumn> currentVisibleTableColumns = getCustomizeMgr().getCurrentVisibleTableColumns();
      for (XViewerColumn column : currentVisibleTableColumns) {
         if (column instanceof IXViewerPreComputedColumn) {
            IXViewerPreComputedColumn preComputedColumn = (IXViewerPreComputedColumn) column;
            Map<Long, String> values;
            if (staged != null) {
               values = new HashMap<>(inputObjects.size());
               staged.put(column, values);
            } else {
               if (column.getPreComputedValueMap() == null) {
                  column.setPreComputedValueMap(new HashMap<Long, String>(inputObjects.size()));
               } else {
                  column.getPreComputedValueMap().clear();
               }
               values = column.getPreComputedValueMap();
            }
            if (!inputObjects.isEmpty()) {
               try {
                  Long startTime = isDebugLoading() ? (new Date()).getTime() : 0L;
                  preComputedColumn.populateCachedValues(inputObjects, values);
                  if (isDebugLoading()) {
                     Long elapsedTime = preComputeElapsedTime.get("PRE - " + column.getName());
                     if (elapsedTime == null) {
                        elapsedTime = (new Date()).getTime() - startTime;
                     } else {
                        elapsedTime += (new Date()).getTime() - startTime;
                     }
                     preComputeElapsedTime.put("PRE - " + column.getName(), elapsedTime);
                  }
               } catch (Exception ex) {
                  XViewerLog.log(Activator.class, Level.SEVERE,
                     String.format("Error performing pre-compute for column %s", column), ex);
               }
            }
         }
      }
   }

   private void performLoad(final Object input, final XViewer xViewer, Map<XViewerColumn, Map<Long, String>> staged) {
      if (xViewer.getTree() != null && !xViewer.getTree().isDisposed()) {
         if (staged != null && !boundsPreCompute()) {
            // bounding went off while the background load ran: the columns keep the full maps, as by default
            for (Map.Entry<XViewerColumn, Map<Long, String>> entry : staged.entrySet()) {
               Map<Long, String> values = entry.getKey().getPreComputedValueMap();
               values.clear();
               values.putAll(entry.getValue());
            }
            staged = null;
         }
         if (staged == null) {
            xViewer.superInputChanged(input);
         } else {
            // the load sorts and filters with the staged values; the rows shown first keep them, the full maps go
            beginPreComputePass(null, staged);
            try {
               resetPreComputedValues();
               xViewer.superInputChanged(input);
               if (viewport != null && !getTree().isDisposed()) {
                  seedPreComputedWindows(viewport.displayChildren(getInput()), -1);
               }
            } finally {
               endPreComputePass();
            }
         }
         loading = false;
         updateStatusLabel();
      }
   }

   /**
    * Bounds the precomputed values this viewer keeps resident. By default (off) every {@link IXViewerPreComputedColumn}
    * keeps a value for each input row in its {@code preComputedValueMap}, computed in the background before the load.
    * When on, and the tree was created with {@code SWT.VIRTUAL} and has a plain {@link ITreeContentProvider} (the
    * viewer is a viewport, see {@link #isViewport()}):
    * <ul>
    * <li>the background call still receives every input object; its values serve the load, and those of the rows
    * shown first are kept;</li>
    * <li>the viewer keeps at most a bounded window of values per column, for the rows shown, their overscan and
    * recently used rows; a row asked for whose value is not resident is computed again, together with the following
    * rows that are not resident either, in one call on the UI thread;</li>
    * <li>a sort or filter pass computes the children it orders in one call on the UI thread, used for that pass only;
    * the values of the rows shown after the pass are kept, so the painted text is the value sorted and filtered
    * on;</li>
    * <li>the column's own {@code preComputedValueMap} is no longer filled; it is cleared at each load as before, and a
    * value put into it (or a map set with {@code setPreComputedValueMap}) is kept and read first.</li>
    * </ul>
    * A column whose values change between calls can then show a newer value for a row recomputed later. It has no
    * effect on an eager tree or with a caller-supplied lazy content provider, which keep the full map. The initial value
    * is on when the system property {@code nebula.xviewer.boundedPreCompute} is {@code true} (read once, when the
    * XViewer class is initialized); any other value leaves it off. A change applies to loads after it.
    *
    * @param bounded true to bound precomputed values to a window of rows
    * @since 1.1
    */
   public void setBoundedPreCompute(boolean bounded) {
      boundedPreCompute = bounded;
      if (!bounded) {
         preComputeWindows.clear();
      }
   }

   /**
    * @return true if precomputed values are bounded to a window of rows when the viewer is a viewport, see
    * {@link #setBoundedPreCompute(boolean)}
    * @since 1.1
    */
   public boolean isBoundedPreCompute() {
      return boundedPreCompute;
   }

   /**
    * Runs {@code action}, which reads the cells of many rows (for example a report, a copy or a sum over tree items),
    * as one pass. With bounded precompute in effect, {@code elements} is asked for the rows, and each precomputed
    * column computes the values of all of them in one {@code populateCachedValues} call when the action first reads
    * one, serves the rest of the action from them and drops them afterwards. Otherwise {@code elements} is not called
    * and only the action runs.
    * <p>
    * Must be called on the UI thread. A call inside another pass (nested) keeps the outer pass's map for each column
    * the outer pass already serves, so for those columns the given rows are ignored.
    * </p>
    *
    * @param elements supplies the rows whose cells the action reads; null rows are skipped
    * @param action the action
    * @throws NullPointerException with bounded precompute in effect, if {@code elements} or the collection it supplies
    * is null
    * @since 1.1
    */
   public void runPreComputePass(Supplier<? extends Collection<?>> elements, Runnable action) {
      if (!boundsPreCompute()) {
         action.run();
         return;
      }
      Collection<?> rows = elements.get();
      List<Object> present = new ArrayList<>(rows.size());
      for (Object element : rows) {
         if (element != null) {
            present.add(element);
         }
      }
      runPreComputePass(present.toArray(), action);
   }

   private void runPreComputePass(Object[] elements, Runnable action) {
      if (!boundsPreCompute()) {
         action.run();
         return;
      }
      beginPreComputePass(elements, null);
      try {
         action.run();
      } finally {
         endPreComputePass();
      }
   }

   private static boolean readBoundedPreComputeDefault() {
      return parseBoundedPreCompute(System.getProperty(BOUNDED_PRECOMPUTE_PROPERTY));
   }

   /**
    * @return true for {@code true}; false for null or {@code false}; false, logged, for any other value. The values are
    * matched ignoring case and surrounding blanks.
    */
   static boolean parseBoundedPreCompute(String value) {
      if (value == null || "false".equalsIgnoreCase(value.trim())) { //$NON-NLS-1$
         return false;
      }
      if ("true".equalsIgnoreCase(value.trim())) { //$NON-NLS-1$
         return true;
      }
      XViewerLog.log(Activator.class, Level.INFO,
         String.format("ignoring %s value '%s'; bounded precompute stays off", BOUNDED_PRECOMPUTE_PROPERTY, value)); //$NON-NLS-1$
      return false;
   }

   /** Bounding is in effect: it was asked for and the viewer is a viewport over a plain tree content provider. */
   boolean boundsPreCompute() {
      return boundedPreCompute && viewport != null;
   }

   /**
    * @return the map that serves the column's precomputed values when the column's XViewer bounds them (the pass map
    * during a full-data pass, else the column's window); null when it does not, so the column's own map serves them
    */
   static Map<Long, String> boundedPreComputedValues(XViewerColumn column) {
      Object owner = column.getXViewer();
      return owner instanceof XViewer ? ((XViewer) owner).preComputedValues(column) : null;
   }

   private Map<Long, String> preComputedValues(XViewerColumn column) {
      if (!boundsPreCompute()) {
         return null;
      }
      Map<Long, String> pass = passValues.get(column);
      return pass != null ? pass : preComputedWindow(column);
   }

   private XViewerPreComputedWindow preComputedWindow(XViewerColumn column) {
      XViewerPreComputedWindow window = preComputeWindows.get(column);
      if (window == null) {
         window = new XViewerPreComputedWindow();
         preComputeWindows.put(column, window);
      }
      return window;
   }

   /**
    * Starts a full-data pass (a load, a sort or filter over the children of a parent, or an action over many rows).
    * Until the outermost {@link #endPreComputePass()}, each visible precomputed column reads and writes a transient
    * full map: the staged load values when given, else values computed by one {@code populateCachedValues} call over
    * {@code elements} when the pass first reads one. The transient maps are dropped when the outermost pass ends.
    */
   private void beginPreComputePass(Object[] elements, Map<XViewerColumn, Map<Long, String>> staged) {
      preComputePassDepth++;
      for (XViewerColumn column : getCustomizeMgr().getCurrentTableColumns()) {
         if (column.isShow() && column instanceof IXViewerPreComputedColumn && !passValues.containsKey(column)) {
            Map<Long, String> full = staged == null ? null : staged.get(column);
            if (full != null) {
               passValues.put(column, full);
            } else if (elements != null) {
               passValues.put(column, new PassValues(column, (IXViewerPreComputedColumn) column, elements));
            }
         }
      }
   }

   private void endPreComputePass() {
      if (--preComputePassDepth == 0) {
         passValues.clear();
      }
   }

   /**
    * A new input starts empty, as the default precompute clears each column's map: the windows of the visible
    * precomputed columns and their own maps are cleared (a map set by the caller stays the column's map), and windows
    * of columns no longer shown are dropped.
    */
   private void resetPreComputedValues() {
      Map<XViewerColumn, XViewerPreComputedWindow> kept = new IdentityHashMap<>();
      for (XViewerColumn column : getCustomizeMgr().getCurrentTableColumns()) {
         if (column.isShow() && column instanceof IXViewerPreComputedColumn) {
            column.getPreComputedValueMap().clear();
            XViewerPreComputedWindow window = preComputedWindow(column);
            window.clear();
            kept.put(column, window);
         }
      }
      preComputeWindows.clear();
      preComputeWindows.putAll(kept);
   }

   /**
    * Keeps, in each column's window, the values the current pass computed for the rows of {@code parent} about to be
    * shown: the first rows and the rows around {@code top}. The painted text of those rows is then the value the pass
    * sorted and filtered on. The pass's rows are first dropped from the window, so none keeps a value from before the
    * pass; rows of other parents keep theirs, and a column the pass did not read keeps its window.
    * <p>
    * Reads no tree geometry: on GTK, {@code Tree.getItemHeight()} can paint rows, whose label requests would fill an
    * unread pass map with a full {@code populateCachedValues} call.
    * </p>
    *
    * @param top the index of the root row at the top of the tree, read before the pass began, or -1 for none
    */
   private void seedPreComputedWindows(Object[] rows, int top) {
      if (passValues.isEmpty() || rows.length == 0) {
         return;
      }
      int head = Math.min(rows.length, PRECOMPUTE_SEED);
      int from = top < 0 ? 0 : Math.max(head, top - PRECOMPUTE_BLOCK);
      int to = top < 0 ? 0 : Math.min(rows.length, top + PRECOMPUTE_BAND);
      for (Map.Entry<XViewerColumn, Map<Long, String>> entry : passValues.entrySet()) {
         Map<Long, String> values = entry.getValue();
         if (values instanceof PassValues && !((PassValues) values).isFilled()) {
            continue;
         }
         IXViewerPreComputedColumn preComputedColumn = (IXViewerPreComputedColumn) entry.getKey();
         XViewerPreComputedWindow window = preComputedWindow(entry.getKey());
         try {
            for (Object row : rows) {
               window.remove(preComputedColumn.getKey(row));
            }
            for (int row = 0; row < head; row++) {
               seed(preComputedColumn, values, window, rows[row]);
            }
            for (int row = from; row < to; row++) {
               seed(preComputedColumn, values, window, rows[row]);
            }
         } catch (RuntimeException ex) {
            // a row the column has no key for is computed when it is asked for, as before
         }
      }
   }

   private static void seed(IXViewerPreComputedColumn column, Map<Long, String> values, Map<Long, String> window,
      Object row) {
      Long key = column.getKey(row);
      if (values.containsKey(key)) {
         window.put(key, values.get(key));
      }
   }

   /** @return the index of the root row at the top of the tree, 0 if unknown */
   private int topRowIndex() {
      try {
         Tree tree = getTree();
         TreeItem top = tree.getTopItem();
         if (top == null || top.getParentItem() != null) {
            return 0;
         }
         return Math.max(0, tree.indexOf(top));
      } catch (RuntimeException ex) {
         return 0;
      }
   }


   /**
    * Called by the viewport bridge before the native tree's requested row is filled. With bounded precompute in
    * effect, when the row's value is not resident, computes it together with the following sibling rows that are not
    * resident either, up to {@link #PRECOMPUTE_BLOCK} rows, in one {@code populateCachedValues} call into the window.
    * Best effort: the label request still computes the row itself, as before, if this did not.
    */
   void viewportPrepareRow(Object[] siblings, int index) {
      if (!boundsPreCompute() || preComputePassDepth > 0) {
         return;
      }
      for (XViewerColumn column : getCustomizeMgr().getCurrentTableColumns()) {
         if (!column.isShow() || !(column instanceof IXViewerPreComputedColumn)) {
            continue;
         }
         IXViewerPreComputedColumn preComputedColumn = (IXViewerPreComputedColumn) column;
         XViewerPreComputedWindow window = preComputedWindow(column);
         try {
            if (resident(column, preComputedColumn, window, siblings[index])) {
               continue;
            }
            int end = Math.min(siblings.length, index + PRECOMPUTE_BLOCK);
            List<Object> block = new ArrayList<>(end - index);
            block.add(siblings[index]);
            for (int i = index + 1; i < end; i++) {
               if (!resident(column, preComputedColumn, window, siblings[i])) {
                  block.add(siblings[i]);
               }
            }
            preComputedColumn.populateCachedValues(block, window);
         } catch (RuntimeException ex) {
            // best effort: the label request computes the row as before and reports its failure in the cell
         }
      }
   }

   private static boolean resident(XViewerColumn column, IXViewerPreComputedColumn preComputedColumn,
      XViewerPreComputedWindow window, Object row) {
      Long key = preComputedColumn.getKey(row);
      return column.getPreComputedValue(key) != null || window.containsKey(key);
   }

   /**
    * The values of one precomputed column for the elements of one full-data pass. The map is computed by a single
    * {@code populateCachedValues} call over the elements when the pass first touches it, serves the rest of the pass
    * and is dropped with it. Every access goes through {@link #filled()}, so no inherited method sees it unfilled.
    */
   private static final class PassValues extends AbstractMap<Long, String> {
      private XViewerColumn column;
      private IXViewerPreComputedColumn source;
      private Object[] elements;
      private Map<Long, String> values;

      PassValues(XViewerColumn column, IXViewerPreComputedColumn source, Object[] elements) {
         this.column = column;
         this.source = source;
         this.elements = elements;
      }

      boolean isFilled() {
         return values != null;
      }

      private Map<Long, String> filled() {
         if (values == null) {
            values = new HashMap<>(Math.max(16, (int) (elements.length / 0.75f) + 1));
            List<Object> objects = new ArrayList<>(Arrays.asList(elements));
            IXViewerPreComputedColumn populator = source;
            XViewerColumn owner = column;
            source = null;
            column = null;
            elements = null;
            try {
               populator.populateCachedValues(objects, this);
            } catch (Exception ex) {
               XViewerLog.log(Activator.class, Level.SEVERE,
                  String.format("Error performing pre-compute for column %s", owner), ex);
            }
         }
         return values;
      }

      @Override
      public String get(Object key) {
         return filled().get(key);
      }

      @Override
      public boolean containsKey(Object key) {
         return filled().containsKey(key);
      }

      @Override
      public String put(Long key, String value) {
         return filled().put(key, value);
      }

      @Override
      public String remove(Object key) {
         return filled().remove(key);
      }

      @Override
      public void clear() {
         filled().clear();
      }

      @Override
      public int size() {
         return filled().size();
      }

      @Override
      public Set<Map.Entry<Long, String>> entrySet() {
         return filled().entrySet();
      }
   }

   /**
    * Will be called when Alt-Left-Click is done within table cell
    *
    * @return true if handled
    */
   public boolean handleAltLeftClick(TreeColumn treeColumn, TreeItem treeItem) {
      if (treeColumn.getData() instanceof XViewerColumn) {
         if ((XViewerColumn) treeColumn.getData() instanceof IAltLeftClickProvider) {
            return ((IAltLeftClickProvider) (XViewerColumn) treeColumn.getData()).handleAltLeftClick(treeColumn,
               treeItem);
         }
      }
      return false;
   }

   /**
    * Will be called when click is within the first 18 pixels of the cell rectangle where the icon would be. This method
    * will be called in addition to handleLeftClick since both are true.
    *
    * @return true if handled
    */
   public boolean handleLeftClickInIconArea(TreeColumn treeColumn, TreeItem treeItem) {
      return false;
   }

   /**
    * Will be called when a cell obtains a mouse left-click. This method will be called in addition to
    * handleLeftClickInIconArea if both are true
    */
   public boolean handleLeftClick(TreeColumn treeColumn, TreeItem treeItem) {
      return false;
   }

   public void handleColumnMultiEdit(TreeColumn treeColumn, Collection<TreeItem> treeItems) {
      if (treeColumn.getData() instanceof XViewerColumn) {
         if ((XViewerColumn) treeColumn.getData() instanceof IMultiColumnEditProvider) {
            ((IMultiColumnEditProvider) (XViewerColumn) treeColumn.getData()).handleColumnMultiEdit(treeColumn,
               treeItems);
         }
      }
   }

   public boolean isColumnMultiEditable(TreeColumn treeColumn, Collection<TreeItem> treeItems) {
      if (!isColumnMultiEditEnabled()) {
         return false;
      }
      return (((XViewerColumn) treeColumn.getData()).isMultiColumnEditable());
   }

   /**
    * If true, "Remove Selected from View" and "Remove Non-Selected from View" menu options will be shown.
    */
   public boolean isRemoveItemsMenuOptionEnabled() {
      return true;
   }

   public XViewerColumn getXTreeColumn(int columnIndex) {
      return (XViewerColumn) getTree().getColumn(columnIndex).getData();
   }

   // Only create one listener for all XViewers
   private void setupCtrlKeyListener() {
      if (!usesKeyFilters) {
         usesKeyFilters = true;
         if (keyFilterUsers++ == 0) {
            Display.getCurrent().addFilter(SWT.KeyDown, displayKeysListener);
            Display.getCurrent().addFilter(SWT.KeyUp, displayKeysListener);
            Display.getCurrent().addFilter(SWT.FocusOut, displayFocusListener);
         }
      }
   }
   static final Listener displayKeysListener = event -> {
      if (event.keyCode == SWT.CTRL) {
         if (event.type == SWT.KeyDown) {
            ctrlKeyDown = true;
         } else if (event.type == SWT.KeyUp) {
            ctrlKeyDown = false;
         }
      }
      if (event.keyCode == SWT.ALT) {
         if (event.type == SWT.KeyDown) {
            altKeyDown = true;
         } else if (event.type == SWT.KeyUp) {
            altKeyDown = false;
         }
      }
   };
   static final Listener displayFocusListener = event -> {
      // Clear when focus is lost
      ctrlKeyDown = false;
      altKeyDown = false;
   };
   private Composite searchComp;
   private XViewerMouseListener mouseListener;
   private boolean loading;

   public void resetDefaultSorter() {
      customizeMgr.resetDefaultSorter();
   }

   /**
    * Override this method if need to perform other tasks upon remove
    */
   @SuppressWarnings("unchecked")
   public void remove(Collection<Object> objects) {
      Object input = getInput();
      if (input instanceof Collection<?>) {
         Collection<Object> inputObj = (Collection<Object>) getInput();
         for (Object obj : objects) {
            super.remove(obj);
            inputObj.remove(obj);
         }
      }
   }

   /**
    * setInputXViewer(Object input) should be called for setting input to XViewer.
    *
    * @param objects
    */
   @Deprecated
   public void load(Collection<Object> objects) {
      setInputXViewer(objects);
   }

   @Override
   public void setSorter(ViewerSorter sorter) {
      super.setSorter(sorter);
      updateStatusLabel();
   }

   public MenuManager getMenuManager() {
      return this.menuManager;
   }

   public int getVisibleItemCount(TreeItem items[]) {
      int cnt = items.length;
      for (TreeItem item : items) {
         if (item.getExpanded()) {
            cnt += getVisibleItemCount(item.getItems());
         }
      }
      return cnt;
   }

   public int getVisibleItemCount() {
      TreeItem[] items = getTree().getItems();
      int cnt = items.length;
      for (TreeItem item : items) {
         if (item.getExpanded()) {
            cnt += getVisibleItemCount(item.getItems());
         }
      }
      return cnt;
   }

   public List<TreeItem> getVisibleItems() {
      List<TreeItem> toReturn = new ArrayList<>();
      getVisibleItems(toReturn, getTree().getItems());
      return toReturn;
   }

   private void getVisibleItems(List<TreeItem> toReturn, TreeItem items[]) {
      for (TreeItem item : items) {
         toReturn.add(item);
         if (item.getExpanded()) {
            getVisibleItems(toReturn, item.getItems());
         }
      }
   }

   @Override
   public void refresh() {
      if (getTree() == null || getTree().isDisposed()) {
         return;
      }
      super.refresh();
      // do not need to updateStatusLabel cause super.refresh will call refresh(element);
   }

   public boolean isFiltered() {
      return getFilters().length > 0;
   }

   @Override
   public void refresh(boolean updateLabels) {
      super.refresh(updateLabels);
      // do not need to updateStatusLabel cause super.refresh will call refresh(element, updateLabels);
   }

   @Override
   public void refresh(Object element, boolean updateLabels) {
      super.refresh(element, updateLabels);
      updateStatusLabel();
   }

   @Override
   public void refresh(Object element) {
      super.refresh(element);
      updateStatusLabel();
   }

   /**
    * Override this to add information to the status string. eg. extra filters etc.
    */
   public String getStatusString() {
      return ""; //$NON-NLS-1$
   }

   public void updateStatusLabel() {
      if (!xViewerFactory.isLoadedStatusLabelAvailable()) {
         return;
      }
      if (getTree().isDisposed() || filterText.isDisposed()) {
         return;
      }
      // nobody sees the status line: compute it once when it paints or is read //FLLDCIM: MARK — every-widget XViewer status line H
      if (!filterText.isVisible()) {
         statusTextDirty = true;
         return;
      }
      computeStatusLabel();
   }

   private void computeStatusLabel() {
      statusTextDirty = false;
      StringBuilder sb = new StringBuilder();
      boolean allItemsFiltered = false;
      boolean filtered = false;

      if (loading) {
         sb.append("Loading...");
      } else {
         // Status Line 1
         int loadedNum = 0;
         int visibleNum = viewport == null ? getVisibleItemCount(getTree().getItems()) : viewportVisibleCount();
         if (getRoot() != null && ((ITreeContentProvider) getContentProvider()) != null) {
            loadedNum = ((ITreeContentProvider) getContentProvider()).getChildren(getRoot()).length;
         }
         allItemsFiltered = loadedNum > 0 && visibleNum == 0;
         if (allItemsFiltered) {
            sb.append(XViewerText.get("status.all_filtered")); //$NON-NLS-1$
         }

         filtered = visibleNum < loadedNum;
         if (!allItemsFiltered && filtered) {
            sb.append(XViewerText.get("status.filtered")); //$NON-NLS-1$
         }
         sb.append(MessageFormat.format(XViewerText.get("status"), loadedNum, visibleNum, //$NON-NLS-1$
            ((IStructuredSelection) getSelection()).size()));
         customizeMgr.appendToStatusLabel(sb);
         if (filterDataUI != null) {
            filterDataUI.appendToStatusLabel(sb);
         }
         columnFilterDataUI.appendToStatusLabel(sb);
         sb.append(getStatusString());
         if (sb.length() > 0) {
            sb.append("\n"); //$NON-NLS-1$
         }

         // Status Line 2
         customizeMgr.getSortingStr(sb);
      }

      // Display status lines
      String str = sb.toString();
      filterText.setText(str);
      if (filterText.isVisible()) {
         statusLayoutDirty = false;
         filterText.getParent().getParent().layout();
      } else {
         // no layout while not effectively visible; caught up once on first paint
         statusLayoutDirty = true;
      }
      filterText.setToolTipText(str);
      filterText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
      filterText.setWordWrap(false);
      if (statusBoldFont == null || statusBoldFont.isDisposed()) {
         FontDescriptor boldDescriptor = FontDescriptor.createFrom(filterText.getFont()).setStyle(SWT.BOLD);
         statusBoldFont = boldDescriptor.createFont(Display.getCurrent());
      }
      filterText.setFont(statusBoldFont);

      if (loading) {
         filterText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      } else if (allItemsFiltered) {
         StyleRange filterStyleRange = new StyleRange();
         filterStyleRange.start = 0;
         filterStyleRange.length = 18;
         filterStyleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
         filterText.setStyleRange(filterStyleRange);
      } else if (filtered && !allItemsFiltered) {
         StyleRange filterStyleRange = new StyleRange();
         filterStyleRange.start = 0;
         filterStyleRange.length = 8;
         filterStyleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
         filterText.setStyleRange(filterStyleRange);
      } else {
         filterText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
      }
   }

   private void flushStatusLabel() {
      if (statusTextDirty && filterText != null && !filterText.isDisposed() && !getTree().isDisposed()) {
         computeStatusLabel();
      }
   }

   private void catchUpStatusLayout() {
      flushStatusLabel();
      if (statusLayoutDirty && filterText != null && !filterText.isDisposed()) {
         statusLayoutDirty = false;
         Display.getCurrent().asyncExec(() -> {
            if (!filterText.isDisposed()) {
               filterText.getParent().getParent().layout();
            }
         });
      }
   }

   public void setLoading(boolean loading) {
      this.loading = loading;
      updateStatusLabel();
   }

   private Listener getCustomizationMouseListener() {
      return event -> {
         if (event.button == 3 && event.count == 1) {
            CustomizeData custData = getCustomizeMgr().getCurrentCustomizeData();
            List<XViewerColumn> currentVisibleTableColumns = getCustomizeMgr().getCurrentVisibleTableColumns();
            custData.getColumnData().getColumns().clear();
            custData.getColumnData().getColumns().addAll(currentVisibleTableColumns);
            String custStr = custData.toString();
            custStr = custStr.replaceAll("XView", "\nXView");
            custStr = custStr.replaceFirst("guid", "\nguid");
            String html = HtmlUtil.simplePage(HtmlUtil.getPreData(custStr));
            String title = String.format("Customization [%s]-[%s]", custData.getName(), custData.getGuid());
            new HtmlDialog(title, title, html).open();
         }
      };
   }

   public String getViewerNamespace() {
      return getXViewerFactory().getNamespace();
   }

   public IXViewerFactory getXViewerFactory() {
      return xViewerFactory;
   }

   public StyledText getStatusLabel() {
      flushStatusLabel(); // a read sees the current status
      return filterText;
   }

   public FilterDataUI getFilterDataUI() {
      return filterDataUI;
   }

   public boolean isColumnMultiEditEnabled() {
      return columnMultiEditEnabled;
   }

   public void setColumnMultiEditEnabled(boolean columnMultiEditEnabled) {
      this.columnMultiEditEnabled = columnMultiEditEnabled;
   }

   public void setEnabled(boolean arg) {
      this.getControl().setEnabled(arg);
   }

   public TreeColumn getRightClickSelectedColumn() {
      return rightClickSelectedColumn;
   }

   public TreeItem getRightClickSelectedItem() {
      return rightClickSelectedItem;
   }

   public Integer getRightClickSelectedColumnNum() {
      return rightClickSelectedColumnNum;
   }

   public CustomizeManager getCustomizeMgr() {
      return customizeMgr;
   }

   public boolean isCtrlKeyDown() {
      return ctrlKeyDown;
   }

   public boolean isAltKeyDown() {
      return altKeyDown;
   }

   boolean searchMatch(String text) {
      if (searchDataUI == null) {
         return false;
      }
      return searchDataUI.match(text);
   }

   Color getSearchMatchColor() {
      return searchColor;
   }

   public boolean isSearch() {
      if (searchDataUI == null) {
         return false;
      }
      return searchDataUI.isSearch();
   }

   public String getColumnText(Object element, int col) {
      return ((IXViewerLabelProvider) getLabelProvider()).getColumnText(element, col);
   }

   /**
    * Mouse clicks can happen in table via XViewerMouseListener or in menu area via XViewerMenuDetectListener. Both are
    * processed here to use in UI
    */
   public void processRightClickMouseEvent(Point point) {
      rightClickSelectedColumn = null;
      rightClickSelectedColumnNum = null;
      rightClickSelectedItem = null;

      rightClickSelectedItem = getItemUnderMouseClick(point);
      rightClickSelectedColumn = getColumnUnderMouseClick(point);
      rightClickSelectedColumnNum = getColumnNumberUnderMouseClick(point);
   }

   public TreeColumn getColumnUnderMouseClick(Point point) throws ArrayIndexOutOfBoundsException {
      Integer columnNumber = getColumnNumberUnderMouseClick(point);
      if (columnNumber == null) {
         return null;
      }
      return getTree().getColumn(columnNumber);
   }

   public Integer getColumnNumberUnderMouseClick(Point point) {
      int[] columnOrder = getTree().getColumnOrder();
      int sum = 0;
      int columnCount = 0;
      for (int column : columnOrder) {
         TreeColumn col = getTree().getColumn(column);
         sum = sum + col.getWidth();
         if (sum > point.x) {
            break;
         }
         columnCount++;
      }

      if (columnCount > columnOrder.length - 1) {
         return null;
      }
      return columnOrder[columnCount];
   }

   public TreeItem getItemUnderMouseClick(Point point) throws ArrayIndexOutOfBoundsException {
      TreeItem itemToReturn = getTree().getItem(point);

      if (itemToReturn == null) {
         // uniform row height: row = y / height, no root array (same result as the summing scan)
         Tree tree = getTree();
         int count = tree.getItemCount();
         int height = tree.getItemHeight();
         int row = point.y < height ? 0 : point.y / height;
         if (count > 0 && (height > 0 || point.y < 0) && row < count) {
            itemToReturn = tree.getItem(row);
         }
      }
      return itemToReturn;
   }

   /**
    * Refresh only single column using normal label provider mechanism. This can be called after normal loading and
    * after columns compute their input in the background.
    */
   public void refreshColumn(XViewerColumn column) {
      refreshColumn(column.getId());
   }

   /**
    * Refresh only single column using normal label provider mechanism. This can be called after normal loading and
    * after columns compute their input in the background.
    */
   public void refreshColumn(String columnId) {
      Pair<XViewerColumn, Integer> column = getCustomizeMgr().getColumnNumFromXViewerColumn(columnId);
      IBaseLabelProvider baseLabelProvider = getLabelProvider();
      if (baseLabelProvider instanceof XViewerLabelProvider) {
         XViewerLabelProvider labelProvider = (XViewerLabelProvider) baseLabelProvider;

         TreeItem[] items = viewport == null ? getTree().getItems() : materializedRootItems();
         Runnable walk = () -> {
            for (TreeItem item : items) {

               ViewerRow viewerRow = getViewerRowFromItem(item);
               if (viewerRow != null) {
                  try {
                     ViewerCell cell = viewerRow.getCell(column.getSecond());
                     String value = null;
                     try {
                        value = labelProvider.getColumnText(item.getData(), column.getSecond());
                     } catch (Exception ex) {
                        value = String.format("Exception getting value from column [%s][%s]", column.getFirst().getId(),
                           ex.getLocalizedMessage());
                     }
                     if (value != null) {
                        cell.setText(value);
                     }
                  } catch (NullPointerException ex) {
                     // do nothing
                  }
               }
            }
         };
         if (boundsPreCompute()) {
            // the walked rows are computed in one call, and the rows painted keep their values
            Object[] rows = itemData(items);
            runPreComputePass(rows, () -> {
               walk.run();
               seedPreComputedWindows(rows, -1);
            });
         } else {
            walk.run();
         }
      }
   }

   /** @return the data of the items that have data, in item order */
   private static Object[] itemData(TreeItem[] items) {
      List<Object> rows = new ArrayList<>(items.length);
      for (TreeItem item : items) {
         Object data = item.getData();
         if (data != null) {
            rows.add(data);
         }
      }
      return rows.toArray();
   }

   /**
    * Override to provide extended filter capabilities
    */
   public XViewerTextFilter getXViewerTextFilter() {
      return new XViewerTextFilter(this);
   }

   public XViewerGradient getxViewerGradient() {
      return xViewerGradient;
   }

   public void setxViewerGradient(XViewerGradient xViewerGradient) {
      this.xViewerGradient = xViewerGradient;
   }

   public void setXViewerEditAdapter(XViewerEditAdapter editAdapter) {
      this.editAdapter = editAdapter;
      this.editAdapter.activate(this);
   }

   public ColumnFilterDataUI getColumnFilterDataUI() {
      return columnFilterDataUI;
   }

   public XViewerMouseListener getMouseListener() {
      return mouseListener;
   }

   public boolean isForcePend() {
      return forcePend;
   }

   public void setForcePend(boolean forcePend) {
      this.forcePend = forcePend;
   }

   public boolean isDebugLoading() {
      return debugLoading;
   }

   public void setDebugLoading(boolean debugLoading) {
      this.debugLoading = debugLoading;
   }

   public Map<String, Long> getPreComputeElapsedTime() {
      return preComputeElapsedTime;
   }

   /**
    * Sets the content provider. When the tree was created with {@code SWT.VIRTUAL} and the provider is a plain
    * {@link ITreeContentProvider}, it is served through an {@link XViewerViewportContentProvider} so that only rows the
    * native tree shows are materialized; {@link #getContentProvider()} then returns that bridge and
    * {@link XViewerViewportContentProvider#getDelegate()} returns the provider given here.
    */
   @Override
   public void setContentProvider(IContentProvider provider) {
      boolean lazy = provider instanceof ILazyTreeContentProvider || provider instanceof ILazyTreePathContentProvider;
      if (isViewport() && provider instanceof ITreeContentProvider && !lazy) {
         viewport = new XViewerViewportContentProvider(this, (ITreeContentProvider) provider);
         super.setContentProvider(viewport);
      } else {
         viewport = null;
         super.setContentProvider(provider);
      }
   }

   /**
    * @return true if the tree was created with {@code SWT.VIRTUAL}, so the viewer works as a viewport over the model
    * @since 1.1
    */
   public boolean isViewport() {
      Tree tree = getTree();
      return tree != null && !tree.isDisposed() && (tree.getStyle() & SWT.VIRTUAL) != 0;
   }

   /**
    * @return rows materialized by the viewport bridge since the last structural refresh, or -1 when the viewer is not a
    * viewport over a plain tree content provider
    * @since 1.1
    */
   public int getViewportMaterializedCount() {
      return viewport == null ? -1 : viewport.getMaterializedCount();
   }

   /**
    * Filtered and sorted children exactly as {@code AbstractTreeViewer#getSortedChildren} orders them.
    */
   Object[] viewportSortedChildren(Object parent, Object[] raw, boolean shown) {
      ViewerFilter[] filters = getFilters();
      ViewerComparator comparator = getComparator();
      // filters and comparators read every child's cells: with bounded precompute the values are computed for this
      // pass, and those of the rows about to be shown are kept so their text is what was sorted and filtered on
      boolean pass = boundsPreCompute() && (filters.length > 0 || comparator != null);
      Object input = getInput();
      // read before the pass: a row the native tree paints meanwhile must not fill an unread pass map
      int top = pass && shown && (parent == input || parent != null && parent.equals(input)) ? topRowIndex() : -1;
      if (pass) {
         beginPreComputePass(raw, null);
      }
      try {
         Object[] result = raw;
         for (ViewerFilter filter : filters) {
            result = filter.filter(this, parent, result);
         }
         if (comparator != null) {
            if (result == raw) {
               result = result.clone();
            }
            if (comparator instanceof TreePathViewerSorter) {
               ((TreePathViewerSorter) comparator).sort(this, viewportPath(parent), result);
            } else {
               comparator.sort(this, result);
            }
         }
         // only rows about to be shown keep their values; a probe of an unexpanded row's children keeps none
         if (pass && shown) {
            seedPreComputedWindows(result, top);
         }
         return result;
      } finally {
         if (pass) {
            endPreComputePass();
         }
      }
   }

   private TreePath viewportPath(Object element) {
      // Reverse only the temporary path directory, not the model or native TreeItems.
      List<Object> segments = new ArrayList<>();
      Object input = getInput();
      for (Object e = element; e != null && e != input && !e.equals(input); e = viewport.getParent(e)) {
         segments.add(e);
      }
      Object[] path = segments.toArray();
      for (int left = 0, right = path.length - 1; left < right; left++, right--) {
         Object saved = path[left];
         path[left] = path[right];
         path[right] = saved;
      }
      return new TreePath(path);
   }

   @Override
   protected void internalRefresh(Object element, boolean updateLabels) {
      if (viewport != null) {
         viewport.invalidate();
      }
      super.internalRefresh(element, updateLabels);
   }

   @Override
   public void add(Object parentElementOrTreePath, Object... childElements) {
      if (viewport != null) {
         viewport.invalidate();
      }
      super.add(parentElementOrTreePath, childElements);
   }

   @Override
   public void insert(Object parentElementOrTreePath, Object element, int position) {
      if (viewport != null) {
         viewport.invalidate();
      }
      super.insert(parentElementOrTreePath, element, position);
   }

   @Override
   public void remove(Object... elementsOrTreePaths) {
      if (viewport != null) {
         viewport.invalidate();
      }
      super.remove(elementsOrTreePaths);
   }

   @Override
   protected void setExpanded(Item node, boolean expand) {
      super.setExpanded(node, expand);
      if (expand) {
         trackExpanded(node);
      }
   }

   private void trackExpanded(Object item) {
      if (viewport != null && item instanceof TreeItem) {
         viewportExpanded.add((TreeItem) item);
      }
   }

   /**
    * A viewport holds items only for the rows the native tree or the API asked for. An element that was never shown has
    * no item, so the inherited lookup finds nothing and {@code setExpandedState}, {@code expandToLevel} and
    * {@code reveal} would do nothing. The row of the element, and the rows of its ancestors, are materialized first.
    */
   @Override
   protected Widget internalExpand(Object elementOrPath, boolean expand) {
      if (viewport != null && elementOrPath != null) {
         Object element =
            elementOrPath instanceof TreePath ? ((TreePath) elementOrPath).getLastSegment() : elementOrPath;
         if (element != null && !viewportHasItem(element)) {
            viewport.materialize(element);
         }
      }
      return super.internalExpand(elementOrPath, expand);
   }

   /**
    * @return true if the element has an item, that is, its row is materialized (the input has the tree itself)
    */
   boolean viewportHasItem(Object element) {
      return findItem(element) != null;
   }

   /**
    * Visible row count for a viewport tree: root count plus the child counts of expanded items whose ancestors are all
    * expanded. Only items the tree or the API expanded are visited, never the full root array.
    */
   private int viewportVisibleCount() {
      int count = getTree().getItemCount();
      viewportExpanded.removeIf(TreeItem::isDisposed);
      for (TreeItem item : viewportExpanded) {
         if (item.getExpanded() && ancestorsExpanded(item)) {
            count += item.getItemCount();
         }
      }
      return count;
   }

   private static boolean ancestorsExpanded(TreeItem item) {
      for (TreeItem p = item.getParentItem(); p != null; p = p.getParentItem()) {
         if (!p.getExpanded()) {
            return false;
         }
      }
      return true;
   }

   private TreeItem[] materializedRootItems() {
      Tree tree = getTree();
      int limit = Math.min(tree.getItemCount(), viewport.materializedRootLimit());
      List<TreeItem> items = new ArrayList<>();
      for (int i = 0; i < limit; i++) {
         if (viewport.isRootMaterialized(i)) {
            items.add(tree.getItem(i));
         }
      }
      return items.toArray(new TreeItem[items.size()]);
   }

}
