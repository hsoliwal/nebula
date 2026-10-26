// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
import java.util.Arrays;

import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding;
import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding;

/**
 * No-display Java 21 proof against actual product classes and real SWT/RWT
 * API signatures. Neither a UI test nor a framework-transpilation oracle.
 * Run with the compiled product and RWT 4.8.0 API JAR on the classpath.
 */
public final class OriginalVirtualPageProof {

    private OriginalVirtualPageProof() {
    }

    private static void require(boolean truth, String detail) {
        if (!truth) {
            throw new AssertionError(detail);
        }
    }

    private static void mustReject(Runnable operation, String detail) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected invalid argument: " + detail);
    }

    public static void main(String[] arguments) {
        String[] keys = {"root/a", "root/b"};
        int[] children = {3, 0};
        String[] cells = {"A", "active", "B", "done"};
        M3VirtualTreeBinding.Page treePage =
                new M3VirtualTreeBinding.Page("root", 64, 2, keys, children, cells);
        keys[0] = "mutated";
        children[0] = Integer.MAX_VALUE;
        cells[0] = "mutated";
        require("root/a".equals(treePage.keyAt(0)), "Tree key lane copy");
        require(treePage.childCountAt(0) == 3, "Tree child-count copy");
        require("A".equals(treePage.textAt(0, 0)), "Tree cell copy");
        require("done".equals(treePage.textAt(1, 1)), "Tree row-major cell");
        require(treePage.offset() == 64 && treePage.size() == 2,
                "Tree sibling geometry");
        mustReject(() -> new M3VirtualTreeBinding.Page("same", 0, 1,
                new String[] {"same"}, new int[] {0}, new String[] {"x"}),
                "self-parenting");
        mustReject(() -> new M3VirtualTreeBinding.Page(null, 0, 1,
                new String[] {"dup", "dup"}, new int[] {0, 0},
                new String[] {"a", "b"}), "duplicate sibling keys");
        mustReject(() -> new M3VirtualTreeBinding.Page(null, 0, 1,
                new String[] {"k"}, new int[] {-1}, new String[] {"x"}),
                "negative child count");
        String[] manyKeys = new String[M3VirtualTreeBinding.PAGE_ROWS];
        int[] manyChildren = new int[manyKeys.length];
        String[] manyCells = new String[manyKeys.length * 2];
        for (int i = 0; i < manyKeys.length; i++) {
            manyKeys[i] = "row-" + i;
            manyCells[i * 2] = "a".repeat(4096);
            manyCells[i * 2 + 1] = "b".repeat(4096);
        }
        mustReject(() -> new M3VirtualTreeBinding.Page(null, 0, 2,
                manyKeys, manyChildren, manyCells), "bounded aggregate tree text");
        Arrays.fill(manyCells, "ok");
        require(new M3VirtualTreeBinding.Page(null, 0, 2,
                manyKeys, manyChildren, manyCells).size() == 64, "bounded tree page");

        String[] tableKeys = {"key-a", "key-b"};
        String[] tableCells = {"Ada", "Engineer", "Lin", "Architect"};
        M3VirtualTableBinding.Page tablePage =
                new M3VirtualTableBinding.Page(0, 2, tableKeys, tableCells);
        tableKeys[0] = "mutated";
        tableCells[0] = "mutated";
        require("key-a".equals(tablePage.keyAt(0)), "Table key lane copy");
        require("Ada".equals(tablePage.textAt(0, 0)), "Table cell copy");
        require("Architect".equals(tablePage.textAt(1, 1)), "Table row-major cell");
        mustReject(() -> new M3VirtualTableBinding.Page(0, 1,
                new String[] {"duplicate", "duplicate"}, new String[] {"a", "b"}),
                "duplicate table keys");
        mustReject(() -> new M3VirtualTableBinding.Page(0, 1,
                new String[] {"key"}, new String[] {null}), "null table text");

        System.out.println("PASS original SWT/RWT virtual Table+Tree page contracts");
    }
}
