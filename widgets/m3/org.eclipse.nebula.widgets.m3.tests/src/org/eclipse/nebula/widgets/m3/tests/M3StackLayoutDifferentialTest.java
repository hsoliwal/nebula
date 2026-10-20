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
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.eclipse.nebula.widgets.m3.M3StackLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * Differential of M3StackLayout against SWT's StackLayout (docs/m3/custom-widgets/stack/M3_STACK_LAYOUT.md). Seeded
 * scripts of operations (add, dispose and reorder children, set the top control to a child, to null or to a foreign
 * control, change margins, change a child's text, resize, lay out, hide and show the parent) run on two identical
 * shells, one with each layout, on the same SWT jar. After every step the two must agree on the visibility of every
 * child, the bounds of every visible child, the preferred size (with and without hints), the Show and Hide events and
 * the composite's pixels (WM_PRINT on win32). Bounds of hidden children and their Move/Resize events are not compared:
 * M3StackLayout defers them by design. A second script compares page mode against a StackLayout whose pages all
 * exist. Seeds: <code>-Dnebula.m3.stack.seeds</code> (12), steps: <code>-Dnebula.m3.stack.steps</code> (150).
 */
public class M3StackLayoutDifferentialTest {

	static final int SEEDS = Integer.getInteger("nebula.m3.stack.seeds", 12).intValue();
	static final int STEPS = Integer.getInteger("nebula.m3.stack.steps", 150).intValue();
	static final int[] COLORS = { SWT.COLOR_DARK_BLUE, SWT.COLOR_DARK_GREEN, SWT.COLOR_DARK_RED, SWT.COLOR_YELLOW };

	/** One side of the differential: a shell, a composite with the layout under test, and its event log. */
	static final class Side {
		final Shell shell;
		final Composite composite;
		final StackLayout layout;
		final Label foreign;
		final List<Control> children = new ArrayList<>();
		final StringBuilder events = new StringBuilder();

		Side(Display display, StackLayout layout, int x) {
			this.layout = layout;
			shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
			shell.setBounds(x, 20, 320, 240);
			composite = new Composite(shell, SWT.NONE);
			composite.setBounds(4, 4, 300, 220);
			composite.setLayout(layout);
			foreign = new Label(shell, SWT.NONE);
			foreign.setBounds(0, 0, 1, 1);
			shell.open();
		}

		Control add(int id, int kind) {
			Control child;
			if (kind == 0) {
				Label label = new Label(composite, SWT.NONE);
				label.setText("label " + id);
				child = label;
			} else {
				Button button = new Button(composite, SWT.PUSH);
				button.setText("button " + id);
				child = button;
			}
			child.setBackground(composite.getDisplay().getSystemColor(COLORS[id % COLORS.length]));
			child.addListener(SWT.Show, e -> events.append("show ").append(id).append(' '));
			child.addListener(SWT.Hide, e -> events.append("hide ").append(id).append(' '));
			children.add(child);
			return child;
		}
	}

	final List<String> log = new ArrayList<>();
	int created;

	@Test
	public void childrenModeMatchesStackLayout() {
		Display display = Display.getDefault();
		for (int seed = 0; seed < SEEDS; seed++) {
			Side stock = new Side(display, new StackLayout(), 20);
			Side m3 = new Side(display, new M3StackLayout(), 360);
			try {
				log.clear();
				created = 0;
				Random random = new Random(0x57ACCL + seed);
				for (int step = 0; step < STEPS; step++) {
					int kind = random.nextInt(14), a = random.nextInt(1 << 16), b = random.nextInt(1 << 16);
					step(stock, kind, a, b);
					step(m3, kind, a, b);
					created++;
					compare(seed, step, stock, m3);
				}
			} finally {
				stock.shell.dispose();
				m3.shell.dispose();
			}
		}
	}

	void step(Side side, int kind, int a, int b) {
		side.events.setLength(0);
		List<Control> live = live(side);
		if (side.layout instanceof M3StackLayout) {
			/* the log line is written once per step, by the second side */
			log.add(describe(kind, a, b));
		}
		switch (kind) {
		case 0:
		case 1:
			side.add(created, a % 2);
			break;
		case 2:
			if (!live.isEmpty()) {
				live.get(a % live.size()).dispose();
			}
			break;
		case 3:
		case 4:
		case 5:
			side.layout.topControl = live.isEmpty() ? null : live.get(a % live.size());
			side.composite.layout(b % 2 == 0);
			break;
		case 6:
			side.layout.topControl = a % 2 == 0 ? null : side.foreign;
			side.composite.layout();
			break;
		case 7:
			side.layout.marginWidth = a % 9;
			side.layout.marginHeight = b % 9;
			side.composite.layout();
			break;
		case 8:
			side.composite.setSize(80 + a % 260, 60 + b % 200);
			side.composite.layout();
			break;
		case 9:
			if (!live.isEmpty() && live.get(a % live.size()) instanceof Label) {
				((Label) live.get(a % live.size())).setText(b % 2 == 0 ? "x" : "a much longer text " + b);
			}
			break;
		case 10:
			if (live.size() > 1) {
				live.get(a % live.size()).moveAbove(live.get(b % live.size()));
				side.composite.layout();
			}
			break;
		case 11:
			side.composite.setVisible(false);
			side.composite.setVisible(true);
			break;
		case 12:
			side.composite.layout(false);
			break;
		default:
			side.composite.layout(true);
			break;
		}
		flush(side.composite.getDisplay());
	}

	static String describe(int kind, int a, int b) {
		String[] names = { "add label", "add button", "dispose", "top", "top", "top", "top null/foreign", "margins", "resize",
				"setText", "moveAbove", "hide+show parent", "layout(false)", "layout(true)" };
		return names[kind] + " " + a + " " + b;
	}

	static List<Control> live(Side side) {
		List<Control> live = new ArrayList<>();
		for (Control child : side.children) {
			if (!child.isDisposed()) {
				live.add(child);
			}
		}
		return live;
	}

	void compare(int seed, int step, Side stock, Side m3) {
		String expected = state(stock), actual = state(m3);
		if (!expected.equals(actual)) {
			fail("seed " + seed + " step " + step + "\n  StackLayout:   " + expected + "\n  M3StackLayout: " + actual + "\nlog:\n"
					+ String.join("\n", log));
		}
	}

	static String state(Side side) {
		StringBuilder out = new StringBuilder();
		for (int id = 0; id < side.children.size(); id++) {
			Control child = side.children.get(id);
			if (child.isDisposed()) {
				continue;
			}
			out.append(id).append(child.getVisible() ? "+" : "-");
			if (child.getVisible()) {
				out.append(child.getBounds());
			}
			out.append(' ');
		}
		Point size = side.composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point hinted = side.composite.computeSize(120, SWT.DEFAULT, true);
		out.append("| size ").append(size).append(' ').append(hinted);
		out.append(" | events ").append(side.events);
		out.append("| pixels ").append(pixels(side.composite));
		return out.toString();
	}

	@Test
	public void pageModeMatchesStackLayoutWithEveryPage() {
		Display display = Display.getDefault();
		int pages = 40;
		for (int seed = 0; seed < SEEDS; seed++) {
			Side stock = new Side(display, new StackLayout(), 20);
			Side m3 = new Side(display, new M3StackLayout(), 360);
			try {
				for (int i = 0; i < pages; i++) {
					stock.add(i, i % 2);
				}
				M3StackLayout layout = (M3StackLayout) m3.layout;
				layout.setPages(m3.composite, pages, (parent, index) -> {
					Control page = m3.add(index, index % 2);
					return page;
				});
				layout.setResidencyBudget(1 + seed % 5);
				Random random = new Random(0xBA6EL + seed);
				for (int step = 0; step < STEPS; step++) {
					int kind = random.nextInt(4), a = random.nextInt(pages), b = random.nextInt(1 << 16);
					if (kind <= 1) {
						stock.layout.topControl = stock.children.get(a);
						stock.composite.layout();
						layout.showPage(a);
					} else if (kind == 2) {
						stock.composite.setSize(80 + b % 260, 60 + (b >> 4) % 200);
						m3.composite.setSize(80 + b % 260, 60 + (b >> 4) % 200);
						stock.composite.layout();
						m3.composite.layout();
					} else {
						stock.layout.marginWidth = m3.layout.marginWidth = b % 7;
						stock.layout.marginHeight = m3.layout.marginHeight = (b >> 3) % 7;
						stock.composite.layout();
						m3.composite.layout();
					}
					flush(display);
					assertEquals("seed " + seed + " step " + step, top(stock), top(m3));
					assertEquals(true, layout.getResidentCount() <= layout.getResidencyBudget() || !viewport());
				}
			} finally {
				stock.shell.dispose();
				m3.shell.dispose();
			}
		}
	}

	static boolean viewport() {
		return M3StackLayoutTest.VIEWPORT;
	}

	/* the top control's text, bounds and the composite's pixels */
	static String top(Side side) {
		Control top = side.layout.topControl;
		if (top == null) {
			return "no top control " + pixels(side.composite);
		}
		String text = top instanceof Label ? ((Label) top).getText() : ((Button) top).getText();
		return text + " " + top.getBounds() + " " + top.getVisible() + " " + pixels(side.composite);
	}

	static void flush(Display display) {
		while (display.readAndDispatch()) {
			// dispatch everything queued
		}
	}

	/* WM_PRINT of the composite and its children on win32 (independent of other windows); print elsewhere */
	static String pixels(Composite composite) {
		Point size = composite.getSize();
		Display display = composite.getDisplay();
		Image image = new Image(display, Math.max(1, size.x), Math.max(1, size.y));
		GC gc = new GC(image);
		try {
			if ("win32".equals(SWT.getPlatform())) {
				long hdc = gc.getClass().getField("handle").getLong(gc);
				long hwnd = composite.getClass().getField("handle").getLong(composite);
				Class<?> os = Class.forName("org.eclipse.swt.internal.win32.OS");
				Method send = os.getMethod("SendMessage", long.class, int.class, long.class, long.class);
				send.invoke(null, hwnd, 0x0317, hdc, 0x2L | 0x4L | 0x8L | 0x10L);
			} else {
				composite.print(gc);
			}
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		} finally {
			gc.dispose();
		}
		ImageData data = image.getImageData();
		image.dispose();
		return data.width + "x" + data.height + " " + Integer.toHexString(Arrays.hashCode(data.data));
	}
}
