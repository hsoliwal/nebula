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
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * The viewport mode, whose point is density, retains no more heap per explicit {@link GridItem}
 * than the original mode. The mode is read once per JVM, so the test loads the Grid's classes
 * twice more, each copy in its own class loader initialised in one mode, and builds the same Grid
 * with each: 100,000 explicit items, one column, the built-in data visualizer. The retained heap
 * is read with {@link MemoryMXBean} after repeated full collections, the smallest of three
 * alternating rounds per mode. Both copies use the classic look, so neither touches the native
 * look's per-Display cache. 100,000 rows keep every lane array of both modes below the G1
 * humongous threshold of a 1 MB region, so the comparison measures the structures rather than
 * the region rounding of a few large arrays.
 * <p>
 * The structural causes are checked directly in the JVM's own mode: the viewport mode keeps no
 * per-item lock object, no second list of a flat Grid's root items and 21 bytes of row-tree lanes
 * per slot.
 * </p>
 */
public class GridItemDensity_Test {
	private static final int ITEMS = 100_000;
	/* GC accounting noise between the two copies, as a fraction of the original mode's bytes */
	private static final double TOLERANCE = 0.01;
	private static final String PACKAGE = "org.eclipse.nebula.widgets.grid.";
	private static final String MODE = "swt.widget.mode.Grid";
	private static final String LOOK = "org.eclipse.nebula.widgets.grid.look";

	@Test
	public void viewportModeRetainsNoMoreHeapPerItemThanTheOriginalMode() throws Exception {
		final Display display = Display.getDefault();
		final ModeCopy original = new ModeCopy("original");
		final ModeCopy viewport = new ModeCopy("viewport");
		double originalBytes = Double.MAX_VALUE;
		double viewportBytes = Double.MAX_VALUE;
		for (int round = 0; round < 3; round++) {
			originalBytes = Math.min(originalBytes, original.bytesPerItem(display, ITEMS));
			viewportBytes = Math.min(viewportBytes, viewport.bytesPerItem(display, ITEMS));
		}
		final String measured = String.format("%,d explicit items: viewport %.1f, original %.1f bytes per item",
				Integer.valueOf(ITEMS), Double.valueOf(viewportBytes), Double.valueOf(originalBytes));
		System.out.println("GridItemDensity_Test: " + measured);
		assertTrue("the original copy measured a sensible size: " + measured, originalBytes > 64);
		assertTrue("the viewport mode retains more per item: " + measured,
				viewportBytes <= originalBytes * (1 + TOLERANCE));
	}

	@Test
	public void viewportModeKeepsNoPerItemLockNorSecondRootListAndUniformRowLanes() throws Exception {
		final Display display = Display.getDefault();
		final Shell shell = new Shell(display);
		try {
			final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL);
			new GridColumn(grid, SWT.NONE).setWidth(80);
			for (int i = 0; i < 1000; i++) {
				new GridItem(grid, SWT.NONE);
			}
			final GridItem item = grid.getItem(500);
			final List<?> rootItems = (List<?>) field(Grid.class, "rootItems").get(grid);
			if (GridViewport.ENABLED) {
				assertNull("the viewport mode makes no lock object per item", field(GridItem.class, "ROW_LOCK").get(item));
				assertTrue("a flat Grid's roots are its items: no second list", rootItems.isEmpty());
				assertEquals("rows of one height keep only the slot lanes", 21, grid.rowTree().laneBytesPerSlot());
			} else {
				assertNotNull("the original mode keeps stock's lock per item", field(GridItem.class, "ROW_LOCK").get(item));
				assertEquals("the original mode keeps stock's root list", 1000, rootItems.size());
			}
			assertEquals(1000, grid.getRootItemCount());
			assertEquals(500, item.getRowIndex());
		} finally {
			shell.dispose();
		}
	}

	private static Field field(final Class<?> type, final String name) throws NoSuchFieldException {
		final Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	/* the Grid's classes once more, initialised in one mode */
	private static final class ModeCopy {
		private final Constructor<?> newGrid;
		private final Constructor<?> newColumn;
		private final Constructor<?> newItem;
		private final Method setWidth;
		private final Method removeAll;
		private final Method getItemCount;

		ModeCopy(final String mode) throws Exception {
			final ClassLoader loader = new GridClasses(Grid.class.getClassLoader());
			final String oldMode = System.getProperty(MODE);
			final String oldLook = System.getProperty(LOOK);
			System.setProperty(MODE, mode);
			System.setProperty(LOOK, "classic");
			try {
				// the classes that read the properties, initialised now
				Class.forName(PACKAGE + "GridViewport", true, loader);
				Class.forName(PACKAGE + "GridNativeLook", true, loader);
			} finally {
				restore(MODE, oldMode);
				restore(LOOK, oldLook);
			}
			final Class<?> grid = Class.forName(PACKAGE + "Grid", true, loader);
			final Class<?> column = Class.forName(PACKAGE + "GridColumn", true, loader);
			final Class<?> item = Class.forName(PACKAGE + "GridItem", true, loader);
			final Field enabled = Class.forName(PACKAGE + "GridViewport", true, loader).getDeclaredField("ENABLED");
			enabled.setAccessible(true);
			assertEquals("the copy's mode", Boolean.valueOf("viewport".equals(mode)), enabled.get(null));
			newGrid = grid.getConstructor(Composite.class, int.class);
			newColumn = column.getConstructor(grid, int.class);
			newItem = item.getConstructor(grid, int.class);
			setWidth = column.getMethod("setWidth", int.class);
			removeAll = grid.getMethod("removeAll");
			getItemCount = grid.getMethod("getItemCount");
		}

		private static void restore(final String key, final String value) {
			if (value == null) {
				System.clearProperty(key);
			} else {
				System.setProperty(key, value);
			}
		}

		double bytesPerItem(final Display display, final int items) throws Exception {
			final Shell shell = new Shell(display);
			try {
				final Object grid = newGrid.newInstance(shell, Integer.valueOf(SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL));
				setWidth.invoke(newColumn.newInstance(grid, Integer.valueOf(SWT.NONE)), Integer.valueOf(80));
				// every per-item path once before the baseline (class loading, first-item sizing)
				for (int i = 0; i < 200; i++) {
					newItem.newInstance(grid, Integer.valueOf(SWT.NONE));
				}
				removeAll.invoke(grid);
				final long before = settledHeap();
				for (int i = 0; i < items; i++) {
					newItem.newInstance(grid, Integer.valueOf(SWT.NONE));
				}
				final long after = settledHeap();
				assertEquals(Integer.valueOf(items), getItemCount.invoke(grid));
				return (after - before) / (double) items;
			} finally {
				shell.dispose();
			}
		}

		/* the used heap after full collections: the smallest of several reads */
		private static long settledHeap() throws InterruptedException {
			final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
			long least = Long.MAX_VALUE;
			for (int i = 0; i < 6; i++) {
				System.gc();
				Thread.sleep(30);
				least = Math.min(least, memory.getHeapMemoryUsage().getUsed());
			}
			return least;
		}
	}

	/* loads the Grid bundle's own classes again (child first), everything else from the bundle */
	private static final class GridClasses extends ClassLoader {
		private final ClassLoader bundle;

		GridClasses(final ClassLoader bundle) {
			super(bundle);
			this.bundle = bundle;
		}

		@Override
		protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
			if (!name.startsWith(PACKAGE)) {
				return super.loadClass(name, resolve);
			}
			synchronized (getClassLoadingLock(name)) {
				Class<?> type = findLoadedClass(name);
				if (type == null) {
					try (InputStream in = bundle.getResourceAsStream(name.replace('.', '/') + ".class")) {
						if (in == null) {
							throw new ClassNotFoundException(name);
						}
						final byte[] bytes = in.readAllBytes();
						type = defineClass(name, bytes, 0, bytes.length);
					} catch (final IOException e) {
						throw new ClassNotFoundException(name, e);
					}
				}
				if (resolve) {
					resolveClass(type);
				}
				return type;
			}
		}
	}
}
