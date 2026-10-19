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
package org.eclipse.nebula.widgets.tablecombo;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.junit.Assume;
import org.junit.Test;

/**
 * Recorded differential of the TableCombo viewport (docs/m3/custom-widgets/tablecombo/TABLECOMBO_VIEWPORT.md): seeded
 * scripts of public API operations (add rows with text per column and images, define columns, select by index, set
 * the text, change the visible item count, open and close the drop-down, arrow keys, editable, remove and removeAll,
 * display column, header and lines, line colours, resize; setItemCount for SWT.VIRTUAL seeds) run against a reference
 * TableCombo (stock Nebula on the official SWT jar, which records) and a candidate TableCombo (this build, which
 * compares). Every step records the text, selection, counts, hit answers, the drop-down's visibility and bounds, the
 * events and the pixels of the combo and of the open drop-down. Any difference fails with the operation log.
 * <p>
 * Opt-in: <code>-Dnebula.tablecombo.recording=&lt;dir&gt;</code>. A TableCombo without widget modes (stock) records;
 * this TableCombo records only with <code>-Dnebula.tablecombo.record=true</code> and compares otherwise.
 * </p>
 */
public class TableComboViewportDifferentialTest {

	static final int SEEDS = Integer.getInteger("nebula.tablecombo.seeds", 8).intValue();
	static final int STEPS = Integer.getInteger("nebula.tablecombo.steps", 100).intValue();

	Display display;
	Shell shell;
	TableCombo combo;
	Image[] images;
	final StringBuilder events = new StringBuilder();
	final List<String> log = new ArrayList<>();
	int created;

	@Test
	public void recordedAgainstReference() throws IOException {
		String dir = System.getProperty("nebula.tablecombo.recording");
		Assume.assumeTrue("-Dnebula.tablecombo.recording=<dir> is not set", dir != null);
		boolean records = "stock".equals(mode()) || Boolean.getBoolean("nebula.tablecombo.record");
		display = Display.getDefault();
		images = new Image[] { image(16, SWT.COLOR_DARK_BLUE), image(16, SWT.COLOR_DARK_GREEN) };
		try {
			for (int seed = 0; seed < SEEDS; seed++) {
				List<String> lines = run(seed);
				Path file = Paths.get(dir, "TableCombo-seed" + seed + ".txt");
				if (records) {
					Files.createDirectories(file.getParent());
					Files.write(file, lines, StandardCharsets.UTF_8);
				} else {
					compare(seed, Files.readAllLines(file, StandardCharsets.UTF_8), lines);
				}
			}
		} finally {
			for (Image image : images) {
				image.dispose();
			}
		}
	}

	/* the TableCombo mode the startup properties ask for, or "stock" for a TableCombo without modes */
	static String mode() {
		try {
			Class.forName("org.eclipse.nebula.widgets.tablecombo.internal.WidgetMode");
			String value = System.getProperty("swt.widget.mode.TableCombo", System.getProperty("swt.widget.mode", "viewport"));
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
		boolean virtual = seed >= 6;
		log.clear();
		log.add("seed " + seed + (virtual ? " virtual" : ""));
		List<String> lines = new ArrayList<>();
		lines.add("# TableCombo differential seed " + seed + (virtual ? " virtual" : ""));
		create(virtual);
		try {
			Random random = new Random(0x7AB1EL + seed);
			for (int step = 0; step < STEPS; step++) {
				events.setLength(0);
				Consumer<TableCombo> op = op(random, virtual);
				String thrown = "";
				try {
					op.accept(combo);
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

	void create(boolean virtual) {
		created = 0;
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 360, 120);
		shell.setLayout(new GridLayout(1, false));
		combo = new TableCombo(shell, SWT.BORDER | (virtual ? SWT.VIRTUAL : SWT.NONE));
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		combo.defineColumns(new String[] { "Id", "Name", "Group" }, new int[] { 50, 120, 80 });
		combo.addListener(SWT.Selection, e -> events.append("Sel:").append(combo.getSelectionIndex()).append(' '));
		combo.addListener(SWT.DefaultSelection, e -> events.append("Def ").append(' '));
		combo.addListener(SWT.Modify, e -> events.append("Mod:").append(combo.getText()).append(' '));
		if (virtual) {
			combo.getTable().addListener(SWT.SetData, e -> {
				events.append("Set:").append(e.index).append(' ');
				TableItem item = (TableItem) e.item;
				item.setText(new String[] { Integer.toString(e.index), "row " + e.index, "group " + e.index / 10 });
				if (e.index % 3 == 0) {
					item.setImage(images[e.index % 2]);
				}
			});
		}
		shell.open();
		flush();
	}

	Consumer<TableCombo> op(Random random, boolean virtual) {
		int kind = random.nextInt(16);
		int a = random.nextInt(1 << 16), b = random.nextInt(1000);
		int count = 1 + random.nextInt(random.nextInt(4) == 0 ? 40 : 6);
		boolean flag = random.nextBoolean();
		if (virtual && kind <= 1) {
			log.add("setItemCount " + (a % 60));
			return c -> c.getTable().setItemCount(a % 60);
		}
		switch (kind) {
		case 0:
		case 1:
			log.add("add " + count + " rows");
			return c -> {
				for (int i = 0; i < count; i++) {
					TableItem item = new TableItem(c.getTable(), SWT.NONE);
					item.setText(new String[] { Integer.toString(created), "row " + created, "group " + created / 10 });
					if (created % 3 == 0) {
						item.setImage(images[created % 2]);
					}
					created++;
				}
			};
		case 2:
		case 3:
			log.add("select " + a);
			return c -> c.select(c.getItemCount() == 0 ? -1 : a % c.getItemCount());
		case 4:
			log.add("setText " + (flag ? "row " + b : "typed " + b));
			return c -> c.setText(flag ? "row " + b : "typed " + b);
		case 5:
			log.add("setVisibleItemCount " + (1 + b % 12));
			return c -> c.setVisibleItemCount(1 + b % 12);
		case 6:
		case 7:
			log.add("setTableVisible " + flag);
			return c -> c.setTableVisible(flag);
		case 8:
			log.add("key " + (flag ? "down" : "up") + " x" + (1 + b % 4));
			return c -> {
				for (int i = 0; i < 1 + b % 4; i++) {
					Event event = new Event();
					event.keyCode = flag ? SWT.ARROW_DOWN : SWT.ARROW_UP;
					c.getTextControl().notifyListeners(SWT.KeyDown, event);
				}
			};
		case 9:
			log.add("setEditable " + flag);
			return c -> c.setEditable(flag);
		case 10:
			log.add("remove " + a);
			return c -> {
				if (c.getItemCount() > 0) {
					c.getTable().remove(a % c.getItemCount());
				}
			};
		case 11:
			log.add("setDisplayColumnIndex " + b % 3);
			return c -> c.setDisplayColumnIndex(b % 3);
		case 12:
			log.add("header " + flag + " lines " + (b % 2 == 0));
			return c -> {
				c.setShowTableHeader(flag);
				c.setShowTableLines(b % 2 == 0);
			};
		case 13:
			log.add("line colours " + flag);
			return c -> {
				c.setOddLinesBackgroundColor(flag ? display.getSystemColor(SWT.COLOR_INFO_BACKGROUND) : null);
				c.setEvenLinesForegroundColor(flag ? display.getSystemColor(SWT.COLOR_DARK_RED) : null);
			};
		case 14:
			log.add("resize " + (200 + a % 300) + "x" + (80 + b % 100));
			return c -> shell.setSize(200 + a % 300, 80 + b % 100);
		default:
			log.add(flag ? "removeAll" : "noop");
			return c -> {
				if (flag) {
					c.getTable().removeAll();
				}
			};
		}
	}

	String state() {
		StringBuilder out = new StringBuilder();
		int count = combo.getItemCount();
		out.append("text ").append(combo.getText()).append(" sel ").append(combo.getSelectionIndex()).append(' ')
				.append(combo.getSelection()).append(" count ").append(count).append(" visible ")
				.append(combo.getVisibleItemCount()).append(" editable ").append(combo.getEditable());
		for (int i : new int[] { 0, count / 2, count - 1 }) {
			if (i >= 0 && i < count) {
				out.append(" item").append(i).append('=').append(combo.getItem(i));
			}
		}
		out.append(" indexOf ").append(combo.indexOf("row 3")).append(' ').append(combo.indexOf("row 7", 2));
		out.append(" size ").append(combo.computeSize(SWT.DEFAULT, SWT.DEFAULT)).append(" height ").append(combo.getItemHeight());
		Shell popup = combo.getTable().getShell();
		boolean dropped = popup != shell && popup.isVisible();
		out.append(" popup ").append(dropped);
		if (dropped) {
			out.append(popup.getBounds()).append(" top ").append(combo.getTable().getTopIndex()).append(" tsel ")
					.append(combo.getTable().getSelectionIndex());
		}
		out.append(" | pixels ").append(hash(render(combo)));
		if (dropped) {
			out.append(' ').append(hash(render(popup)));
		}
		return out.toString();
	}

	Image image(int size, int color) {
		Image image = new Image(display, size, size);
		GC gc = new GC(image);
		gc.setBackground(display.getSystemColor(color));
		gc.fillRectangle(0, 0, size, size);
		gc.dispose();
		return image;
	}

	void flush() {
		while (display.readAndDispatch()) {
			// dispatch everything queued
		}
		combo.update();
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
