/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.compositetable;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Assume;
import org.junit.Test;

/**
 * Recorded differential of the CompositeTable viewport (docs/m3/custom-widgets/compositetable/COMPOSITETABLE_VIEWPORT.md):
 * seeded scripts of public API operations (row count, top row, current row and column, page and row navigation,
 * typing into the current editor, refresh of one or all rows, hide and show, resize, max rows visible, lines) run
 * against a reference CompositeTable (stock Nebula on the official SWT jar, which records) and a candidate (this
 * build, which compares). Every step records the top and current rows, the row controls, the first and last bound
 * rows, the editor texts, the events (RowConstruction, RowContent, Scroll, RowFocus) and the pixels. Any difference
 * fails with the operation log.
 * <p>
 * Opt-in: <code>-Dnebula.compositetable.recording=&lt;dir&gt;</code>. A CompositeTable without widget modes (stock)
 * records; this one records only with <code>-Dnebula.compositetable.record=true</code> and compares otherwise.
 * </p>
 */
public class CompositeTableViewportDifferentialTest {

	static final int SEEDS = Integer.getInteger("nebula.compositetable.seeds", 8).intValue();
	static final int STEPS = Integer.getInteger("nebula.compositetable.steps", 100).intValue();

	/** A row of three text editors. */
	public static class Row extends Composite {
		public final Text[] texts = new Text[3];

		public Row(Composite parent, int style) {
			super(parent, style);
			setLayout(new FillLayout(SWT.HORIZONTAL));
			for (int i = 0; i < texts.length; i++) {
				texts[i] = new Text(this, SWT.BORDER);
			}
		}
	}

	Display display;
	Shell shell;
	CompositeTable table;
	final StringBuilder events = new StringBuilder();
	final List<String> log = new ArrayList<>();
	int rowsInModel;

	@Test
	public void recordedAgainstReference() throws IOException {
		String dir = System.getProperty("nebula.compositetable.recording");
		Assume.assumeTrue("-Dnebula.compositetable.recording=<dir> is not set", dir != null);
		boolean records = "stock".equals(mode()) || Boolean.getBoolean("nebula.compositetable.record");
		display = Display.getDefault();
		for (int seed = 0; seed < SEEDS; seed++) {
			List<String> lines = run(seed);
			Path file = Paths.get(dir, "CompositeTable-seed" + seed + ".txt");
			if (records) {
				Files.createDirectories(file.getParent());
				Files.write(file, lines, StandardCharsets.UTF_8);
			} else {
				compare(seed, Files.readAllLines(file, StandardCharsets.UTF_8), lines);
			}
		}
	}

	/* the CompositeTable mode the startup properties ask for, or "stock" for a CompositeTable without modes */
	static String mode() {
		try {
			Class.forName("org.eclipse.nebula.widgets.compositetable.internal.WidgetMode");
			String value = System.getProperty("swt.widget.mode.CompositeTable", System.getProperty("swt.widget.mode", "viewport"));
			return value.toLowerCase(Locale.ROOT);
		} catch (ClassNotFoundException e) {
			return "stock";
		}
	}

	void compare(int seed, List<String> expected, List<String> actual) {
		for (int i = 0; i < Math.max(expected.size(), actual.size()); i++) {
			String e = i < expected.size() ? expected.get(i) : "<none>";
			String a = i < actual.size() ? actual.get(i) : "<none>";
			if (!e.equals(a)) {
				fail("seed " + seed + " line " + i + " differs from the recording (" + mode() + ")\n  recorded: " + e
						+ "\n  this run: " + a + "\nlog:\n" + String.join("\n", log));
			}
		}
	}

	List<String> run(int seed) {
		log.clear();
		log.add("seed " + seed);
		List<String> lines = new ArrayList<>();
		lines.add("# CompositeTable differential seed " + seed);
		create();
		try {
			Random random = new Random(0xC0A7B1EL + seed);
			for (int step = 0; step < STEPS; step++) {
				events.setLength(0);
				Consumer<CompositeTable> op = op(random);
				String thrown = "";
				try {
					op.accept(table);
				} catch (RuntimeException e) {
					thrown = " threw " + e.getClass().getSimpleName();
				}
				flush();
				String state;
				try {
					state = state();
				} catch (RuntimeException e) {
					state = "state threw " + e.getClass().getSimpleName();
				}
				lines.add(state + thrown + " | events " + events);
			}
		} finally {
			shell.dispose();
		}
		return lines;
	}

	void create() {
		rowsInModel = 50;
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 420, 260);
		shell.setLayout(new FillLayout());
		table = new CompositeTable(shell, SWT.NONE);
		new Row(table, SWT.NONE);
		table.setRunTime(true);
		table.addRowConstructionListener(new RowConstructionListener() {
			@Override
			public void rowConstructed(Control newRow) {
				events.append("Con ");
			}

			@Override
			public void headerConstructed(Control newHeader) {
				events.append("Hdr ");
			}
		});
		table.addRowContentProvider((sender, currentObjectOffset, rowControl) -> {
			events.append("Row:").append(currentObjectOffset).append(' ');
			Row row = (Row) rowControl;
			for (int i = 0; i < row.texts.length; i++) {
				row.texts[i].setText("r" + currentObjectOffset + "c" + i);
			}
		});
		table.addScrollListener(new ScrollListener() {
			@Override
			public void tableScrolled(ScrollEvent scrollEvent) {
				events.append("Scr:").append(scrollEvent.userScrollDirection).append(' ');
			}
		});
		table.addRowFocusListener(new RowFocusAdapter() {
			@Override
			public void arrive(CompositeTable sender, int currentObjectOffset, Control newRow) {
				events.append("Arr:").append(currentObjectOffset).append(' ');
			}

			@Override
			public void depart(CompositeTable sender, int currentObjectOffset, Control row) {
				events.append("Dep:").append(currentObjectOffset).append(' ');
			}
		});
		table.setNumRowsInCollection(rowsInModel);
		shell.open();
		flush();
	}

	Consumer<CompositeTable> op(Random random) {
		int kind = random.nextInt(14);
		int a = random.nextInt(1 << 16), b = random.nextInt(1000);
		boolean flag = random.nextBoolean();
		switch (kind) {
		case 0:
			rowsInModel = a % 120;
			log.add("setNumRowsInCollection " + rowsInModel);
			return t -> t.setNumRowsInCollection(rowsInModel);
		case 1:
		case 2:
			log.add("setTopRow " + a % 130);
			return t -> t.setTopRow(a % 130);
		case 3:
			log.add("setSelection " + b % 3 + "," + a % 12);
			return t -> t.setSelection(b % 3, a % 12);
		case 4:
			log.add(flag ? "doPageDown" : "doPageUp");
			return flag ? CompositeTable::doPageDown : CompositeTable::doPageUp;
		case 5:
			log.add(flag ? "doRowDown" : "doRowUp");
			return flag ? CompositeTable::doRowDown : CompositeTable::doRowUp;
		case 6:
			log.add("type " + b);
			return t -> {
				Control current = t.getCurrentRowControl();
				if (current instanceof Row) {
					((Row) current).texts[b % 3].setText("typed " + b);
				}
			};
		case 7:
			log.add(flag ? "refreshAllRows" : "refreshRow " + a % 12);
			return t -> {
				if (flag) {
					t.refreshAllRows();
				} else {
					t.refreshRow(a % 12);
				}
			};
		case 8:
			log.add("hide, change " + (flag ? "rows" : "top") + ", show");
			return t -> {
				t.setVisible(false);
				if (flag) {
					rowsInModel = 10 + a % 90;
					t.setNumRowsInCollection(rowsInModel);
				} else {
					t.setTopRow(a % 60);
				}
				flush();
				t.setVisible(true);
			};
		case 9:
			log.add("resize " + (240 + a % 300) + "x" + (100 + b % 260));
			return t -> shell.setSize(240 + a % 300, 100 + b % 260);
		case 10:
			log.add("setMaxRowsVisible " + (2 + b % 12));
			return t -> t.setMaxRowsVisible(2 + b % 12);
		case 11:
			log.add("setLinesVisible " + flag);
			return t -> t.setLinesVisible(flag);
		case 12:
			log.add(flag ? "doFocusInitialRow" : "doFocusLastRow");
			return flag ? CompositeTable::doFocusInitialRow : CompositeTable::doFocusLastRow;
		default:
			log.add("setCurrentRow " + a % 12);
			return t -> t.setCurrentRow(a % 12);
		}
	}

	String state() {
		StringBuilder out = new StringBuilder();
		out.append("rows ").append(table.getNumRowsInCollection()).append(" top ").append(table.getTopRow()).append(" cur ")
				.append(table.getCurrentRow()).append('/').append(table.getCurrentColumn()).append(" visible ")
				.append(table.getNumRowsVisible()).append(" max ").append(table.getMaxRowsVisible()).append(" lines ")
				.append(table.getLinesVisible()).append(" sel ").append(table.getSelection());
		Control[] controls = table.getRowControls();
		out.append(" controls ").append(controls.length);
		for (int i : new int[] { 0, controls.length - 1 }) {
			if (i >= 0 && i < controls.length && controls[i] instanceof Row) {
				Row row = (Row) controls[i];
				out.append(" row").append(i).append('@').append(table.getControlRow(row)).append('=').append(row.texts[0].getText())
						.append(',').append(row.texts[2].getText()).append(row.getBounds());
			}
		}
		Control current = table.getCurrentRowControl();
		out.append(" current ").append(current instanceof Row ? ((Row) current).texts[0].getText() : "-");
		out.append(" | pixels ").append(hash(render(table)));
		return out.toString();
	}

	void flush() {
		while (display.readAndDispatch()) {
			// dispatch everything queued
		}
		table.update();
		while (display.readAndDispatch()) {
			// and what the paint queued
		}
	}

	/* the control's pixels: WM_PRINT on win32 (what the window draws, independent of other windows), print elsewhere */
	ImageData render(Control control) {
		Point size = control.getSize();
		Image image = new Image(display, Math.max(1, size.x), Math.max(1, size.y));
		GC gc = new GC(image);
		try {
			if ("win32".equals(SWT.getPlatform())) {
				try {
					long hdc = gc.getClass().getField("handle").getLong(gc);
					long hwnd = control.getClass().getField("handle").getLong(control);
					Class<?> os = Class.forName("org.eclipse.swt.internal.win32.OS");
					Method send = os.getMethod("SendMessage", long.class, int.class, long.class, long.class);
					send.invoke(null, hwnd, 0x0317, hdc, 0x2L | 0x4L | 0x8L | 0x10L);
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			} else {
				control.print(gc);
			}
		} finally {
			gc.dispose();
		}
		ImageData data = image.getImageData();
		image.dispose();
		return data;
	}

	static String hash(ImageData data) {
		return data.width + "x" + data.height + " " + Integer.toHexString(Arrays.hashCode(data.data));
	}
}
