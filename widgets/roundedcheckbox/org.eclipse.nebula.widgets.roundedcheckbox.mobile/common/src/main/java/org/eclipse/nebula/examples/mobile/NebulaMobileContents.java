// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckbox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Shell;

/** One shared demo body for the Android and iOS SWT hosts. */
public final class NebulaMobileContents {
    private NebulaMobileContents() {}

    public static final class State {
        public volatile int attempts;
        public volatile boolean selected;
        public volatile int scaleSelection = 25;
        public volatile int progressSelection = 25;
        public volatile int scaleEvents;
        public volatile int listSelection = -1;
        public volatile int listEvents;
        public volatile int listDataLoads;
        public volatile String nativeText = "";
        public volatile int textModifications;
        public volatile int textVerifications;
        public volatile int comboSelection = -1;
        public volatile int comboEvents;
        public volatile String comboText = "";
        public volatile int tableSelection = -1;
        public volatile int tableSelections;
        public volatile int tableDataLoads;
        public volatile int tableHeaderSelections;
        public volatile boolean tableExpanded;
        public volatile boolean disposed;
        Text editor;
        Table table;
    }

    public static State create(Shell shell) {
        State state = new State();
        shell.addListener(SWT.Dispose, event -> {
            state.editor = null;
            state.table = null;
            state.disposed = true;
        });
        Canvas labels = new Canvas(shell, SWT.DOUBLE_BUFFERED);
        RoundedCheckbox selectable = new RoundedCheckbox(shell, SWT.NONE);
        RoundedCheckbox vetoed = new RoundedCheckbox(shell, SWT.NONE);
        RoundedCheckbox disabled = new RoundedCheckbox(shell, SWT.NONE);
        vetoed.setSelection(true);
        vetoed.setGrayed(true);
        disabled.setSelection(true);
        disabled.setEnabled(false);
        // Consume real SWT mobile controls (not Android/UIKit demonstration clones).
        ProgressBar progress = new ProgressBar(shell, SWT.HORIZONTAL);
        Scale scale = new Scale(shell, SWT.HORIZONTAL);
        progress.setSelection(25);
        scale.setSelection(25);
        // Original SWT List, not a platform-specific demo replacement:
        // one billion logical VIRTUAL items, only visible source rows loaded.
        List rows = new List(shell, SWT.SINGLE | SWT.VIRTUAL | SWT.V_SCROLL);
        rows.addListener(SWT.SetData, event -> {
            state.listDataLoads++;
            rows.setItem(event.index, "Row " + event.index);
        });
        rows.setItemCount(1_000_000_000);
        rows.setTopIndex(999_999_990);
        rows.addListener(SWT.Selection, event -> {
            state.listSelection = rows.getSelectionIndex();
            state.listEvents++;
            if (!labels.isDisposed()) labels.redraw();
        });
        // The actual SWT Text owner is backed by EditText / UITextField.
        // Native keyboard events must go through its Verify/Modify lifecycle.
        Text editor = new Text(shell, SWT.SINGLE | SWT.BORDER);
        state.editor = editor;
        editor.setMessage("Native keyboard input");
        editor.addVerifyListener(event -> {
            state.textVerifications++;
            if (event.text.contains("!")) event.doit = false;
        });
        editor.addModifyListener(event -> {
            state.nativeText = editor.getText();
            state.textModifications++;
            if (!labels.isDisposed()) labels.redraw();
        });
        // The production SWT Combo is an original Text/List/Button/Shell
        // composition; its native Android/UIKit popup sends Combo.Selection.
        Combo combo = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setItems("First", "Second", "Third");
        combo.select(0);
        state.comboSelection = combo.getSelectionIndex();
        state.comboText = combo.getText();
        combo.addListener(SWT.Selection, event -> {
            state.comboSelection = combo.getSelectionIndex();
            state.comboText = combo.getText();
            state.comboEvents++;
            if (!labels.isDisposed()) labels.redraw();
        });
        // Actual original SWT Table/TableItem/TableColumn, not a list clone:
        // the billion-row model allocates only explicit and visible row peers.
        Table table = new Table(shell, SWT.VIRTUAL | SWT.SINGLE | SWT.FULL_SELECTION);
        state.table = table;
        TableColumn itemName = new TableColumn(table, SWT.LEFT);
        itemName.setText("Name");
        itemName.setWidth(160);
        itemName.addListener(SWT.Selection, event -> state.tableHeaderSelections++);
        TableColumn itemValue = new TableColumn(table, SWT.RIGHT);
        itemValue.setText("Value");
        itemValue.setWidth(120);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.addListener(SWT.SetData, event -> {
            state.tableDataLoads++;
            TableItem item = (TableItem) event.item;
            item.setText(0, "Table row " + event.index);
            item.setText(1, "Value " + event.index);
        });
        table.setItemCount(1_000_000_000);
        table.setTopIndex(999_999_994);
        table.addListener(SWT.Selection, event -> {
            state.tableSelection = table.getSelectionIndex();
            state.tableSelections++;
            if (!labels.isDisposed()) labels.redraw();
        });
        // Reposition the *same* original Table to an on-screen viewport,
        // retaining its 1-billion-row model and stable SWT item identities.
        Button tableToggle = new Button(shell, SWT.PUSH);
        tableToggle.setText("Expand Table");
        scale.addListener(SWT.Selection, event -> {
            int value = scale.getSelection();
            state.scaleSelection = value;
            state.scaleEvents++;
            progress.setSelection(value);
            state.progressSelection = progress.getSelection();
            if (!labels.isDisposed()) labels.redraw();
        });

        selectable.addListener(SWT.Selection, event -> {
            state.attempts++;
            shell.getDisplay().asyncExec(() -> {
                if (!labels.isDisposed()) {
                    state.selected = selectable.getSelection();
                    labels.redraw();
                }
            });
        });
        vetoed.addListener(SWT.Selection, event -> {
            event.doit = false;
            state.attempts++;
            labels.redraw();
        });
        labels.addListener(SWT.Paint, event -> {
            Rectangle area = labels.getClientArea();
            event.gc.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            event.gc.fillRectangle(area);
            event.gc.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_BLACK));
            event.gc.drawText("Nebula on SWT mobile", 16, 16, true);
            event.gc.drawText("Toggle", 80, 76, true);
            event.gc.drawText("Selection veto", 80, 148, true);
            event.gc.drawText("Disabled", 80, 220, true);
            event.gc.drawText("Selection: " + selectable.getSelection(), 16, 292, true);
            event.gc.drawText("Attempts: " + state.attempts, 16, 324, true);
            event.gc.drawText("Scale drives ProgressBar: " + state.progressSelection, 16, 356, true);
            event.gc.drawText("Billion-row List selected: " + state.listSelection, 16, 474, true);
            event.gc.drawText("Text: " + state.nativeText, 16, 724, true);
            event.gc.drawText("Combo: " + state.comboText + " / " + state.comboSelection, 16, 756, true);
            event.gc.drawText("Table selection: " + state.tableSelection, 16, 926, true);
        });
        Runnable layout = () -> {
            Rectangle area = shell.getClientArea();
            labels.setBounds(0, 0, area.width, area.height);
            selectable.setBounds(20, 64, 44, 44);
            vetoed.setBounds(20, 136, 44, 44);
            disabled.setBounds(20, 208, 44, 44);
            progress.setBounds(20, 388, Math.max(30, area.width - 40), 24);
            scale.setBounds(20, 428, Math.max(30, area.width - 40), 34);
            int listHeight = Math.max(80, Math.min(100, area.height - 640));
            rows.setBounds(20, 500, Math.max(30, area.width - 40), listHeight);
            editor.setBounds(20, 500 + listHeight + 16, Math.max(30, area.width - 40), 40);
            combo.setBounds(20, 500 + listHeight + 72, Math.max(30, area.width - 40), 34);
            int tableY = 500 + listHeight + 122;
            if (state.tableExpanded) {
                table.setBounds(12, 68, Math.max(30, area.width - 24),
                        Math.max(96, area.height - 82));
                tableToggle.setBounds(Math.max(6, area.width - 125), 12, 115, 36);
            } else {
                table.setBounds(20, tableY, Math.max(30, area.width - 40),
                        Math.max(96, Math.min(148, area.height - tableY - 8)));
                tableToggle.setBounds(Math.max(6, area.width - 125), 260, 115, 36);
            }
        };
        tableToggle.addListener(SWT.Selection, event -> {
            state.tableExpanded = !state.tableExpanded;
            tableToggle.setText(state.tableExpanded ? "Restore" : "Expand Table");
            layout.run();
        });
        shell.addListener(SWT.Resize, event -> layout.run());
        layout.run();
        return state;
    }
}
