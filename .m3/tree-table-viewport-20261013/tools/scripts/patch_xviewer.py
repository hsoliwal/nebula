import sys

p = sys.argv[1]
s = open(p, encoding='utf-8').read()


def rep(a, b):
    global s
    assert s.count(a) == 1, (a[:80], s.count(a))
    s = s.replace(a, b)


rep('''import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
''', '''import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
''')
rep('''import org.eclipse.jface.viewers.IBaseLabelProvider;
''', '''import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
''')
rep('''import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
''', '''import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreePathViewerSorter;
import org.eclipse.jface.viewers.TreeViewer;
''')
rep('''import org.eclipse.jface.viewers.ViewerCell;
''', '''import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
''')
rep('''import org.eclipse.swt.graphics.Color;
''', '''import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
''')
rep('''import org.eclipse.swt.widgets.Display;
''', '''import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
''')
rep('''   private final Map<String, Long> preComputeElapsedTime = new HashMap<>();
''', '''   private final Map<String, Long> preComputeElapsedTime = new HashMap<>();
   //FLLDCIM: HPCPOP — viewport state; objects exist only for rows the native tree or the API asked for
   private XViewerViewportContentProvider viewport;
   private final Set<TreeItem> viewportExpanded = new HashSet<>();
   private Font statusBoldFont;
   private boolean statusLayoutDirty;
''')
rep('''      tree.setHeaderVisible(true);
      tree.setLinesVisible(true);
      setUseHashlookup(true);
''', '''      tree.setHeaderVisible(true);
      tree.setLinesVisible(true);
      if ((tree.getStyle() & SWT.VIRTUAL) != 0) {
         tree.addListener(SWT.Expand, event -> trackExpanded(event.item));
      }
      tree.addListener(SWT.Dispose, event -> disposeStatusFont());
      setUseHashlookup(true);
''')
rep('''      //Dispose Menu
      if (menuManager != null) {
         menuManager.removeAll();
         menuManager.dispose();
      }
   }
''', '''      //Dispose Menu
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
''')
rep('''            filterText = new StyledText(searchComp, SWT.NONE);
''', '''            //FLLDCIM: one control — the status line is a helper surface and never takes focus
            filterText = new StyledText(searchComp, SWT.NO_FOCUS);
            filterText.addListener(SWT.Paint, event -> catchUpStatusLayout());
''')
rep('''         int visibleNum = getVisibleItemCount(getTree().getItems());
''', '''         int visibleNum = viewport == null ? getVisibleItemCount(getTree().getItems()) : viewportVisibleCount();
''')
rep('''      filterText.setText(str);
      filterText.getParent().getParent().layout();
      filterText.setToolTipText(str);
      filterText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
      filterText.setWordWrap(false);
      FontDescriptor boldDescriptor = FontDescriptor.createFrom(filterText.getFont()).setStyle(SWT.BOLD);
      filterText.setFont(boldDescriptor.createFont(Display.getCurrent()));
''', '''      filterText.setText(str);
      if (filterText.isVisible()) {
         statusLayoutDirty = false;
         filterText.getParent().getParent().layout();
      } else {
         //FLLDCIM: HPCPOP — no layout while not effectively visible; caught up once on first paint
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
''')
rep('''   public void setLoading(boolean loading) {
''', '''   private void catchUpStatusLayout() {
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
''')
rep('''      if (itemToReturn == null) {
         int sum;
         sum = 0;
         TreeItem[] allItems = getTree().getItems();
         for (TreeItem item : allItems) {
            sum = sum + getTree().getItemHeight();
            if (sum > point.y) {
               itemToReturn = item;
               break;
            }
         }
      }
      return itemToReturn;
''', '''      if (itemToReturn == null) {
         //FLLDCIM: HPCPOP — uniform row height: row = y / height, no root array (same result as the summing scan)
         Tree tree = getTree();
         int count = tree.getItemCount();
         int height = tree.getItemHeight();
         int row = point.y < height ? 0 : point.y / height;
         if (count > 0 && (height > 0 || point.y < 0) && row < count) {
            itemToReturn = tree.getItem(row);
         }
      }
      return itemToReturn;
''')
rep('''         TreeItem[] items = getTree().getItems();
         for (TreeItem item : items) {

            ViewerRow viewerRow = getViewerRowFromItem(item);
''', '''         TreeItem[] items = viewport == null ? getTree().getItems() : materializedRootItems();
         for (TreeItem item : items) {

            ViewerRow viewerRow = getViewerRowFromItem(item);
''')
rep('''   public Map<String, Long> getPreComputeElapsedTime() {
      return preComputeElapsedTime;
   }
''', '''   public Map<String, Long> getPreComputeElapsedTime() {
      return preComputeElapsedTime;
   }
''' + open(sys.argv[2], encoding='utf-8').read())
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched', p)
