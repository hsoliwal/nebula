// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Out-of-tree probe for docs/m3/GRID_MEDIATED_VIEWPORT.md: which TreeItem calls of a hidden
// SWT.VIRTUAL Tree request SWT.SetData for the receiver (the contract GridItem.setItemCount mirrors).
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class TreeVirtualContractProbe {
	static final StringBuilder LOG = new StringBuilder();

	public static void main(String[] args) {
		Display display = Display.getDefault();
		for (int style : new int[] { SWT.VIRTUAL, SWT.VIRTUAL | SWT.CHECK }) {
			for (boolean shown : new boolean[] { false, true }) {
				Shell shell = new Shell(display);
				shell.setSize(300, 300);
				Tree tree = new Tree(shell, style);
				tree.setBounds(0, 0, 250, 250);
				tree.addListener(SWT.SetData, e -> LOG.append("SetData(" + ((TreeItem) e.item).getData("n") + " index " + e.index + ") "));
				TreeItem[] roots = new TreeItem[12];
				for (int i = 0; i < roots.length; i++) {
					roots[i] = new TreeItem(tree, SWT.NONE);
					roots[i].setData("n", "R" + i);
				}
				if (shown) {
					shell.open();
					while (display.readAndDispatch()) { }
					LOG.setLength(0);
				}
				System.out.println("style " + Integer.toHexString(style) + (shown ? " shown" : " hidden"));
				call("getItemCount", () -> roots[5].getItemCount());
				call("getItem(0) childless", () -> roots[6].getItem(0));
				call("getItems", () -> roots[7].getItems());
				call("getExpanded", () -> roots[8].getExpanded());
				call("setItemCount(3)", () -> roots[9].setItemCount(3));
				call("getItem(1) of the new children", () -> roots[9].getItem(1).setData("n", "R9.1"));
				call("setText", () -> roots[10].setText("x"));
				call("getText after setText", () -> roots[10].getText());
				call("getParentItem", () -> roots[11].getParentItem());
				call("indexOf", () -> roots[11].indexOf(roots[9].getItem(0)));
				call("getChecked", () -> roots[11].getChecked());
				call("clear(0) of R9", () -> roots[9].clear(0, false));
				call("getItem(0) of R9", () -> roots[9].getItem(0).setData("n", "R9.0"));
				call("getText of R9.0", () -> roots[9].getItem(0).getText());
				// Dispose order: R0 > A (A1, A2 > A2a), B (B1); C; removed by setItemCount and by dispose
				for (int round = 0; round < 2; round++) {
					TreeItem r = roots[round];
					r.setItemCount(3);
					TreeItem a = named(r.getItem(0), "A");
					a.setItemCount(2);
					named(a.getItem(0), "A1");
					TreeItem a2 = named(a.getItem(1), "A2");
					a2.setItemCount(1);
					named(a2.getItem(0), "A2a");
					TreeItem b = named(r.getItem(1), "B");
					b.setItemCount(1);
					named(b.getItem(0), "B1");
					named(r.getItem(2), "C");
					if (round == 0) {
						call("Dispose order, setItemCount(0)", () -> r.setItemCount(0));
					} else {
						call("Dispose order, A.dispose()", () -> a.dispose());
					}
				}
				shell.dispose();
			}
		}
	}

	static TreeItem named(TreeItem item, String name) {
		item.setData("n", name);
		item.addListener(SWT.Dispose, e -> LOG.append("Dispose(" + name + ") "));
		return item;
	}

	static void call(String name, Runnable r) {
		LOG.setLength(0);
		String outcome = "";
		try {
			r.run();
		} catch (RuntimeException e) {
			outcome = " threw " + e;
		}
		System.out.println("  " + name + ": " + LOG + outcome);
	}
}
