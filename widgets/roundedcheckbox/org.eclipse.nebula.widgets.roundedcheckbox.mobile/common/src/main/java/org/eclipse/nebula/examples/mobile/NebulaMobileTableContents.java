// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * The original SWT Table/TableColumn/TableItem mobile consumer. It reuses the
 * existing Android/UIKit Display, Shell, native Canvas and GC peer hierarchy,
 * never a competing Android-only Table implementation.
 *
 * An owned Shell provides a separate, reachable page so existing RoundedCheckbox,
 * Scale, List, Text and Combo native acceptance coordinates stay unchanged.
 */
public final class NebulaMobileTableContents {
    private NebulaMobileTableContents() {}

    /** Connect the original Table page and the button that opens it. */
    public static void attach(Shell root, NebulaMobileContents.State state) {
        Shell tablePage = new Shell(root, SWT.NO_TRIM | SWT.ON_TOP);
        Table table = new Table(tablePage,
                SWT.SINGLE | SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        TableColumn indexColumn = new TableColumn(table, SWT.LEFT);
        indexColumn.setText("Logical index");
        indexColumn.setWidth(145);
        TableColumn valueColumn = new TableColumn(table, SWT.LEFT);
        valueColumn.setText("Native cell");
        valueColumn.setWidth(185);
        indexColumn.addSelectionListener(SelectionListener.widgetSelectedAdapter(event ->
                state.tableHeaderEvents++));
        table.addListener(SWT.SetData, event -> {
            if (!(event.item instanceof TableItem item)) return;
            state.tableDataLoads++;
            item.setText(0, "Row " + event.index);
            item.setText(1, "Value " + event.index);
        });
        table.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            state.tableEvents++;
            state.tableSelection = table.getSelectionIndex();
        }));
        table.setItemCount(1_000_000_000);
        table.setTopIndex(999_999_990);
        table.addListener(SWT.Paint, event -> state.tableTopIndex = table.getTopIndex());
        state.tableTopIndex = table.getTopIndex();
        state.tableRowHeight = table.getItemHeight();
        state.tableHeaderHeight = table.getHeaderHeight();

        Button back = new Button(tablePage, SWT.PUSH);
        back.setText("Back");
        back.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            tablePage.setVisible(false);
            state.tableShown = false;
        }));

        Runnable layout = () -> {
            Rectangle bounds = root.getClientArea();
            tablePage.setBounds(0, 0, bounds.width, bounds.height);
            back.setBounds(12, 12, 92, 36);
            table.setBounds(12, 64, Math.max(1, bounds.width - 24),
                    Math.max(0, bounds.height - 86));
        };
        root.addListener(SWT.Resize, event -> layout.run());

        Button open = new Button(root, SWT.PUSH);
        open.setText("Table");
        open.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            layout.run();
            state.tableShown = true;
            tablePage.open();
        }));
        root.addListener(SWT.Resize, event -> {
            if (!open.isDisposed()) {
                Rectangle bounds = root.getClientArea();
                open.setBounds(Math.max(6, bounds.width - 124), 260, 110, 34);
            }
        });
        layout.run();
        Rectangle bounds = root.getClientArea();
        open.setBounds(Math.max(6, bounds.width - 124), 260, 110, 34);
    }
}
