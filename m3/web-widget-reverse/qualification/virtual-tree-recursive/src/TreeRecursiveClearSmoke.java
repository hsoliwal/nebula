// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
import java.util.Objects;
import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Runs the UNMODIFIED original product M3VirtualTreeBinding.java with
 * test-only SWT event shapes; no claim of native GTK/RAP conformance.
 */
public final class TreeRecursiveClearSmoke {
    private static final class Model implements M3VirtualTreeBinding.Nodes {
        int fetches;
        String rootKey = "root-0";
        String childPrefix = "Child ";
        String grandchildPrefix = "Grandchild ";

        @Override public int rootCount() { return 2; }
        @Override public int columns() { return 2; }
        @Override public int[] path(String key) { return null; }

        @Override
        public M3VirtualTreeBinding.Page fetch(String parentKey, int offset, int length) {
            fetches++;
            String[] ids = new String[length];
            int[] counts = new int[length];
            String[] cells = new String[length * 2];
            for (int i = 0; i < length; i++) {
                int index = offset + i;
                if (parentKey == null) {
                    ids[i] = index == 0 ? rootKey : "root-" + index;
                    counts[i] = index == 0 ? 10 : 0;
                    cells[2 * i] = "Root " + index;
                } else if (parentKey.equals(rootKey)) {
                    ids[i] = "child-" + index;
                    counts[i] = index == 0 ? 3 : 0;
                    cells[2 * i] = childPrefix + index;
                } else if (parentKey.equals("child-0")) {
                    ids[i] = "grandchild-" + index;
                    cells[2 * i] = grandchildPrefix + index;
                } else {
                    throw new AssertionError("Unexpected parent " + parentKey);
                }
                cells[2 * i + 1] = "status";
            }
            return new M3VirtualTreeBinding.Page(parentKey, offset, 2, ids, counts, cells);
        }
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    public static void main(String[] args) {
        Model model = new Model();
        Tree tree = new Tree(SWT.VIRTUAL | SWT.MULTI, 2);
        try (M3VirtualTreeBinding binding = M3VirtualTreeBinding.bind(tree, model)) {
            TreeItem root = tree.getItem(0);
            equal("Root 0", root.getText(0));
            root.setExpanded(true);
            TreeItem child = root.getItem(0);
            equal("Child 0", child.getText(0));
            child.setExpanded(true);
            equal("Grandchild 2", child.getItem(2).getText(0));
            equal(3, binding.getResidentPageCount());

            int fetches = model.fetches;
            model.childPrefix = "UPDATED child ";
            model.grandchildPrefix = "UPDATED grandchild ";
            binding.invalidateRoots(0, 0);
            equal("Root 0", root.getText(0));
            root.setExpanded(true);
            equal("UPDATED child 0", root.getItem(0).getText(0));
            equal("UPDATED grandchild 2", root.getItem(0).getItem(2).getText(0));
            if (model.fetches < fetches + 3) {
                throw new AssertionError("Recursive invalidation did not fetch all three pages");
            }
            if (binding.getResidentPageCount() > M3VirtualTreeBinding.HOT_PAGES) {
                throw new AssertionError("Page-cache budget exceeded");
            }

            // Request child data BEFORE re-materializing its cleared parent.
            // Old Widget.getData metadata is intentionally still present.
            model.rootKey = "root-replacement";
            model.childPrefix = "FINAL child ";
            binding.invalidateRoots(0, 0);
            equal("FINAL child 0", root.getItem(0).getText(0));
            equal("root-replacement", root.getData(M3VirtualTreeBinding.ROW_KEY));
            System.out.println("PASS original M3VirtualTreeBinding under test-only "
                    + "SWT.SetData recursive invalidation, fetches=" + model.fetches);
        }
    }
}
