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
package org.eclipse.nebula.widgets.gallery.tests;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assume;
import org.junit.Test;

/**
 * Recorded differential of the Gallery viewport (docs/m3/GALLERY_VIEWPORT.md): seeded scripts of public API
 * operations (add and dispose items and groups, expand and collapse groups, select, show, scroll with the bar, click,
 * press arrow keys, rename, set images, change the item size, resize) run against a reference Gallery (stock Nebula
 * on the official SWT jar, which records) and a candidate Gallery (this Gallery, which compares). Every step records
 * the counts, group and item bounds, the scroll bar, the selection, hit tests, the events and the Paint regions;
 * every seed the rendered pixels (WM_PRINT on win32). Any difference fails with the operation log.
 * <p>
 * Opt-in: <code>-Dnebula.gallery.recording=&lt;dir&gt;</code>. A Gallery without widget modes (stock) records; this
 * Gallery records only with <code>-Dnebula.gallery.record=true</code> and compares otherwise.
 * </p>
 */
public class GalleryViewportDifferentialTest {

	static final int SEEDS = Integer.getInteger("nebula.gallery.seeds", 8).intValue();
	static final int STEPS = Integer.getInteger("nebula.gallery.steps", 100).intValue();

	Display display;
	Shell shell;
	Gallery gallery;
	DefaultGalleryGroupRenderer groupRenderer;
	Image[] images;
	final StringBuilder events = new StringBuilder(), paints = new StringBuilder();
	int created;

	@Test
	public void recordedAgainstReference() throws IOException {
		String dir = System.getProperty("nebula.gallery.recording");
		Assume.assumeTrue("-Dnebula.gallery.recording=<dir> is not set", dir != null);
		boolean records = "stock".equals(GalleryScaleOracleTest.mode()) || Boolean.getBoolean("nebula.gallery.record");
		display = Display.getDefault();
		images = new Image[] { image(32, SWT.COLOR_DARK_BLUE), image(48, SWT.COLOR_DARK_GREEN) };
		try {
			for (int seed = 0; seed < SEEDS; seed++) {
				List<String> lines = run(seed);
				Path file = Paths.get(dir, "Gallery-seed" + seed + ".txt");
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

	void compare(int seed, List<String> expected, List<String> actual) {
		for (int i = 0; i < Math.max(expected.size(), actual.size()); i++) {
			String e = i < expected.size() ? expected.get(i) : "<none>";
			String a = i < actual.size() ? actual.get(i) : "<none>";
			if (!e.equals(a)) {
				fail("seed " + seed + " line " + i + " differs from the recording (" + GalleryScaleOracleTest.mode()
						+ ")\n  recorded: " + e + "\n  this run: " + a + "\nlog:\n" + String.join("\n", log));
			}
		}
	}

	final List<String> log = new ArrayList<>();

	List<String> run(int seed) {
		boolean virtual = seed >= 6;
		log.clear();
		log.add("seed " + seed + (virtual ? " virtual" : ""));
		List<String> lines = new ArrayList<>();
		lines.add("# Gallery differential seed " + seed + (virtual ? " virtual" : ""));
		create(virtual);
		try {
			Random random = new Random(0x6A11E7L + seed);
			for (int step = 0; step < STEPS; step++) {
				events.setLength(0);
				paints.setLength(0);
				Consumer<Gallery> op = op(random, virtual);
				String thrown = "";
				try {
					op.accept(gallery);
				} catch (RuntimeException e) {
					/* stock Gallery throws on some steps (for example an arrow key while no item fits a row) */
					thrown = " threw " + e.getClass().getSimpleName();
				}
				flush();
				String state;
				try {
					state = state();
				} catch (RuntimeException e) {
					state = "state threw " + e.getClass().getSimpleName();
				}
				lines.add(state + thrown + " | events " + events + " | paint " + paints);
			}
			settle(100);
			ImageData pixels = render();
			lines.add("pixels " + hash(pixels));
			evidence(seed, pixels);
		} finally {
			shell.dispose();
		}
		return lines;
	}

	void create(boolean virtual) {
		created = 0;
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 420, 320);
		shell.setLayout(new FillLayout());
		gallery = new Gallery(shell, SWT.V_SCROLL | SWT.MULTI | (virtual ? SWT.VIRTUAL : SWT.NONE));
		groupRenderer = new DefaultGalleryGroupRenderer();
		groupRenderer.setMinMargin(2);
		groupRenderer.setItemHeight(56);
		groupRenderer.setItemWidth(72);
		groupRenderer.setAutoMargin(true);
		gallery.setGroupRenderer(groupRenderer);
		gallery.setItemRenderer(new DefaultGalleryItemRenderer());
		gallery.addListener(SWT.Selection, e -> events.append("Sel:").append(text(e.item)).append(' '));
		gallery.addListener(SWT.DefaultSelection, e -> events.append("Def:").append(text(e.item)).append(' '));
		gallery.addListener(SWT.Paint, e -> paints.append(e.x).append(',').append(e.y).append(' ').append(e.width)
				.append('x').append(e.height).append(';'));
		gallery.addTreeListener(new TreeListener() {
			public void treeExpanded(TreeEvent e) {
				events.append("Exp:").append(text(e.item)).append(' ');
			}

			public void treeCollapsed(TreeEvent e) {
				events.append("Col:").append(text(e.item)).append(' ');
			}
		});
		if (virtual) {
			gallery.addListener(SWT.SetData, e -> {
				GalleryItem item = (GalleryItem) e.item;
				events.append("Set ");
				if (item.getParentItem() == null) {
					item.setText("group " + e.index);
					item.setItemCount(5 + (e.index * 7) % 30);
				} else {
					item.setText("item " + e.index);
					item.setImage(images[e.index % 2]);
				}
			});
		}
		shell.open();
		flush();
	}

	Consumer<Gallery> op(Random random, boolean virtual) {
		int kind = random.nextInt(18);
		int a = random.nextInt(1 << 16), b = random.nextInt(1000);
		int count = 1 + random.nextInt(random.nextInt(4) == 0 ? 60 : 8);
		boolean flag = random.nextBoolean();
		int groups = gallery.getItemCount();
		if (virtual && kind <= 2) {
			log.add("setItemCount " + (1 + a % 40));
			return g -> g.setItemCount(1 + a % 40);
		}
		switch (kind) {
		case 0:
		case 1:
			log.add("add " + count + " items to group " + a);
			return g -> {
				if (g.getItemCount() == 0) {
					group(g);
				}
				GalleryItem group = g.getItem(a % g.getItemCount());
				for (int i = 0; i < count; i++) {
					item(group, flag ? -1 : Math.min(b % (group.getItemCount() + 1), group.getItemCount()));
				}
			};
		case 2:
			log.add("add a group with " + count + " items");
			return g -> {
				GalleryItem group = group(g);
				for (int i = 0; i < count; i++) {
					item(group, -1);
				}
			};
		case 3:
			log.add("dispose an item " + a);
			return g -> {
				GalleryItem item = anyItem(g, a);
				if (item != null) {
					item.dispose();
				}
			};
		case 4:
			log.add("dispose a group " + a);
			return g -> {
				if (g.getItemCount() > 0) {
					g.getItem(a % g.getItemCount()).dispose();
				}
			};
		case 5:
		case 6:
			log.add("setExpanded group " + a + " " + flag);
			return g -> {
				if (g.getItemCount() > 0) {
					g.getItem(a % g.getItemCount()).setExpanded(flag);
				}
			};
		case 7:
			log.add("setSelection " + a + (flag ? " and showItem" : ""));
			return g -> {
				GalleryItem item = anyItem(g, a);
				if (item != null) {
					g.setSelection(new GalleryItem[] { item });
					if (flag) {
						g.showItem(item);
					}
				}
			};
		case 8:
		case 9:
			log.add("scroll bar to " + a);
			return g -> {
				ScrollBar bar = g.getVerticalBar();
				if (bar != null && bar.getVisible()) {
					bar.setSelection(a % (bar.getMaximum() + 1));
					bar.notifyListeners(SWT.Selection, new Event());
				}
			};
		case 10:
			log.add("click at " + (5 + a % 400) + "," + (5 + b % 300) + (flag ? " twice" : ""));
			return g -> click(g, 5 + a % 400, 5 + b % 300, flag);
		case 11:
			log.add("key " + (flag ? "down" : "right") + " x" + (1 + b % 5));
			return g -> {
				for (int i = 0; i < 1 + b % 5; i++) {
					Event event = new Event();
					event.keyCode = flag ? SWT.ARROW_DOWN : SWT.ARROW_RIGHT;
					g.notifyListeners(SWT.KeyDown, event);
				}
			};
		case 12:
			log.add("resize " + (150 + a % 400) + "x" + (100 + b % 300));
			return g -> shell.setSize(150 + a % 400, 100 + b % 300);
		case 13:
			log.add("setText " + a);
			return g -> {
				GalleryItem item = anyItem(g, a);
				if (item != null) {
					item.setText(flag ? "renamed " + a : "r");
				}
			};
		case 14:
			log.add("setImage " + a + " " + b % 3);
			return g -> {
				GalleryItem item = anyItem(g, a);
				if (item != null) {
					item.setImage(b % 3 == 2 ? null : images[b % 3]);
				}
			};
		case 15:
			log.add("item size " + (40 + b % 80) + "x" + (30 + a % 70));
			return g -> {
				groupRenderer.setItemSize(40 + b % 80, 30 + a % 70);
				g.redraw();
			};
		case 16:
			log.add("deselectAll");
			return Gallery::deselectAll;
		default:
			log.add(groups > 0 && flag ? "removeAll" : "noop");
			return g -> {
				if (flag) {
					g.removeAll();
				}
			};
		}
	}

	GalleryItem group(Gallery g) {
		GalleryItem group = new GalleryItem(g, SWT.NONE);
		group.setText("group " + created++);
		group.setExpanded(true);
		return group;
	}

	GalleryItem item(GalleryItem group, int index) {
		GalleryItem item = index < 0 ? new GalleryItem(group, SWT.NONE) : new GalleryItem(group, SWT.NONE, index);
		item.setText("item " + created);
		item.setImage(images[created++ % 2]);
		return item;
	}

	/* an item chosen by a number over all groups (null when there is none) */
	GalleryItem anyItem(Gallery g, int a) {
		int groups = g.getItemCount();
		if (groups == 0) {
			return null;
		}
		GalleryItem group = g.getItem(a % groups);
		int items = group.getItemCount();
		return items == 0 ? null : group.getItem((a / groups) % items);
	}

	void click(Gallery g, int x, int y, boolean twice) {
		Event down = new Event();
		down.x = x;
		down.y = y;
		down.button = 1;
		down.count = 1;
		g.notifyListeners(SWT.MouseDown, down);
		Event up = new Event();
		up.x = x;
		up.y = y;
		up.button = 1;
		up.count = 1;
		g.notifyListeners(SWT.MouseUp, up);
		if (twice) {
			Event dbl = new Event();
			dbl.x = x;
			dbl.y = y;
			dbl.button = 1;
			dbl.count = 2;
			g.notifyListeners(SWT.MouseDoubleClick, dbl);
		}
	}

	String state() {
		StringBuilder out = new StringBuilder();
		int groups = gallery.getItemCount();
		out.append("groups ").append(groups);
		ScrollBar bar = gallery.getVerticalBar();
		if (bar != null) {
			out.append(" bar ").append(bar.getMinimum()).append('/').append(bar.getMaximum()).append(" t").append(bar.getThumb())
					.append(" s").append(bar.getSelection()).append(" i").append(bar.getIncrement()).append(" p")
					.append(bar.getPageIncrement()).append(bar.getVisible() ? " v" : " h").append(bar.getEnabled() ? "e" : "d");
		}
		for (int i : new int[] { 0, groups / 2, groups - 1 }) {
			if (i < 0 || i >= groups) {
				continue;
			}
			GalleryItem group = gallery.getItem(i);
			out.append(" g").append(i).append(' ').append(group.getItemCount()).append(group.isExpanded() ? "+" : "-")
					.append(group.getBounds());
			int items = group.getItemCount();
			for (int j : new int[] { 0, items - 1 }) {
				if (j >= 0 && j < items) {
					out.append(" i").append(j).append(group.getItem(j).getBounds());
				}
			}
		}
		GalleryItem[] selection = gallery.getSelection();
		out.append(" sel ").append(selection.length).append(selection.length > 0 ? " " + text(selection[0]) : "");
		for (int[] p : new int[][] { { 10, 10 }, { 100, 60 }, { 200, 150 }, { 300, 250 } }) {
			GalleryItem hit = gallery.getItem(new Point(p[0], p[1]));
			out.append(" hit ").append(hit == null ? "-" : text(hit));
		}
		return out.toString();
	}

	static String text(Object item) {
		return item instanceof GalleryItem ? ((GalleryItem) item).getText() : "-";
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
		gallery.update();
		while (display.readAndDispatch()) {
			// and what the paint queued
		}
	}

	void settle(int millis) {
		long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	/* the gallery's pixels: WM_PRINT on win32 (what the window draws, independent of other windows), print elsewhere */
	ImageData render() {
		Point size = gallery.getSize();
		Image image = new Image(display, Math.max(1, size.x), Math.max(1, size.y));
		GC gc = new GC(image);
		try {
			if ("win32".equals(SWT.getPlatform())) {
				try {
					long hdc = gc.getClass().getField("handle").getLong(gc);
					long hwnd = gallery.getClass().getField("handle").getLong(gallery);
					Class<?> os = Class.forName("org.eclipse.swt.internal.win32.OS");
					Method send = os.getMethod("SendMessage", long.class, int.class, long.class, long.class);
					send.invoke(null, hwnd, 0x0317, hdc, 0x2L | 0x4L | 0x8L | 0x10L);
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			} else {
				gallery.print(gc);
			}
		} finally {
			gc.dispose();
		}
		ImageData data = image.getImageData();
		image.dispose();
		return data;
	}

	/* -Dnebula.gallery.evidence=<dir>: the final pixels of every seed as Gallery-seed<n>-<mode>.png */
	static void evidence(int seed, ImageData data) {
		String dir = System.getProperty("nebula.gallery.evidence");
		if (dir == null) {
			return;
		}
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { data };
		new java.io.File(dir).mkdirs();
		loader.save(Paths.get(dir, "Gallery-seed" + seed + "-" + GalleryScaleOracleTest.mode() + ".png").toString(),
				SWT.IMAGE_PNG);
	}

	static String hash(ImageData data) {
		return data.width + "x" + data.height + " " + Integer.toHexString(Arrays.hashCode(data.data));
	}

	static Field field(Class<?> type, String name) throws NoSuchFieldException {
		return type.getField(name);
	}
}
