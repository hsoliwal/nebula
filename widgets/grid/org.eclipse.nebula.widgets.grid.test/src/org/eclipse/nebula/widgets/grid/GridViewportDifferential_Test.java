/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Recorded differential of the Grid viewport (docs/m3/GRID_MEDIATED_VIEWPORT.md): seeded scripts of
 * public API operations run against a stock Grid and a viewport Grid side by side. Every step records
 * answer | scroll bars | top row | selection | cell selection | focus | events, and every seed ends
 * with the printed pixels; any difference fails with the operation log, which is the reproduction.
 * <p>
 * Both Grids come from Grid builds loaded by their own class loader, so each reads the startup mode
 * when its classes initialize: the oracle with {@code swt.widget.mode.Grid=original}, the candidate
 * with {@code viewport}. {@code -Dgrid.differential.oracle=<classes directory>} uses another Grid
 * build as the oracle, for example upstream Nebula.
 * </p>
 */
public class GridViewportDifferential_Test {
	/** The package's logger (brief rule 22: java.util.logging only). */
	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("org.eclipse.nebula.widgets.grid");

	private static final int SEEDS = Integer.getInteger("grid.differential.seeds", 8).intValue();
	private static final int STEPS = Integer.getInteger("grid.differential.steps", 160).intValue();
	private static final int FIRST_SEED = Integer.getInteger("grid.differential.firstSeed", 0).intValue();
	/* mode of the candidate build (viewport by default, or original), compared against the oracle */
	private static final String CANDIDATE = System.getProperty("grid.differential.candidate", "viewport");
	private static final String PACKAGE = "org.eclipse.nebula.";

	private Display display;
	/*
	 * one shell per Grid, both at the same place: Grid reads the real cursor position (hover state
	 * after a scroll), so both Grids must see it at the same relative position
	 */
	private Shell stockShell, viewportShell;
	private Button focusHolder, stockFocusHolder;

	@Before
	public void setUp() {
		display = Display.getDefault();
		stockShell = shell();
		viewportShell = shell();
		focusHolder = new Button(viewportShell, SWT.PUSH);
		focusHolder.setBounds(5, 340, 60, 25);
		stockFocusHolder = new Button(stockShell, SWT.PUSH);
		stockFocusHolder.setBounds(5, 340, 60, 25);
	}

	private Shell shell() {
		final Shell shell = new Shell(display);
		shell.setBounds(10, 10, 330, 400);
		return shell;
	}

	@After
	public void tearDown() {
		stockShell.dispose();
		viewportShell.dispose();
	}

	/** Loads Grid builds again, child first, so their static state (the startup mode) is their own. */
	static final class IsolatingLoader extends URLClassLoader {
		IsolatingLoader(final URL[] urls, final ClassLoader parent) {
			super(urls, parent);
		}

		@Override
		protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
			if (!name.startsWith(PACKAGE)) {
				return super.loadClass(name, resolve);
			}
			synchronized (getClassLoadingLock(name)) {
				Class<?> loaded = findLoadedClass(name);
				if (loaded == null) {
					loaded = findClass(name);
				}
				if (resolve) {
					resolveClass(loaded);
				}
				return loaded;
			}
		}
	}

	private static URL location(final Class<?> type) {
		return type.getProtectionDomain().getCodeSource().getLocation();
	}

	/**
	 * A Grid build whose classes initialize with the given startup mode and the classic look (the
	 * differential compares the mechanics; the native look has its own test, GridNativeLook_Test).
	 */
	static ClassLoader gridBuild(final URL gridClasses, final String mode) throws Exception {
		final ClassLoader loader = new IsolatingLoader(new URL[] { gridClasses, location(GridDifferentialDriver.class) },
				GridViewportDifferential_Test.class.getClassLoader());
		final String key = "swt.widget.mode.Grid", lookKey = "org.eclipse.nebula.widgets.grid.look";
		final String old = System.getProperty(key), oldLook = System.getProperty(lookKey);
		System.setProperty(key, mode);
		System.setProperty(lookKey, "classic");
		try {
			Class.forName(PACKAGE + "widgets.grid.Grid", true, loader);
			try {
				Class.forName(PACKAGE + "widgets.grid.GridViewport", true, loader);
			} catch (final ClassNotFoundException e) {
				// a Grid build without the mode switch
			}
			Class.forName(PACKAGE + "widgets.grid.GridNativeLook", true, loader);
		} catch (final ClassNotFoundException e) {
			// a Grid build without the native look
		} finally {
			restore(key, old);
			restore(lookKey, oldLook);
		}
		return loader;
	}

	private static void restore(final String key, final String value) {
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
	}

	@SuppressWarnings("unchecked")
	static Function<int[], Object> driver(final ClassLoader build, final Shell shell, final int config) throws Exception {
		return (Function<int[], Object>) build.loadClass(GridDifferentialDriver.class.getName())
				.getConstructor(Composite.class, Integer.class, Integer.class)
				.newInstance(shell, Integer.valueOf(5), Integer.valueOf(config));
	}

	private static URL oracleClasses() throws MalformedURLException {
		final String oracle = System.getProperty("grid.differential.oracle");
		return oracle == null ? location(Grid.class) : new File(oracle).toURI().toURL();
	}

	/*
	 * SWT.VIRTUAL tree Grids (G4 step 1): the items get their texts from a SetData listener that
	 * counts the requests per item. Every step answers and paints as the oracle, and the SetData
	 * report (total, items asked, items asked more than once) is part of each compared state, so
	 * the candidate asks exactly the items stock asks, as often. The driver reads item texts by
	 * column (getText(0)), which requests data in both modes alike.
	 */
	@Test
	public void virtualTreeScriptsRequestDataLikeTheStockGrid() throws Exception {
		final ClassLoader stockBuild = gridBuild(oracleClasses(), "original");
		final ClassLoader candidateBuild = gridBuild(location(Grid.class), CANDIDATE);
		for (int seed = 20; seed < 24; seed++) {
			final int config = GridDifferentialDriver.VIRTUAL | seed % 4;
			final Function<int[], Object> stock = driver(stockBuild, stockShell, config);
			final Function<int[], Object> candidate = driver(candidateBuild, viewportShell, config);
			stockShell.open();
			viewportShell.open();
			final String pixels = run(seed, config, stock, candidate);
			final String report = String.valueOf(candidate.apply(new int[] { GridDifferentialDriver.STATE, 0, 0, 0 }));
			LOG.info("GridViewportDifferential_Test: virtual seed " + seed + " config " + config + ", " + STEPS
					+ " steps identical, " + pixels + ";" + report.substring(report.lastIndexOf(" | setdata")));
			for (final Shell shell : new Shell[] { stockShell, viewportShell }) {
				for (final org.eclipse.swt.widgets.Control child : shell.getChildren()) {
					if (!(child instanceof Button)) {
						child.dispose();
					}
				}
			}
		}
	}

	@Test
	public void seededScriptsAnswerAndPaintLikeTheStockGrid() throws Exception {
		final ClassLoader stockBuild = gridBuild(oracleClasses(), "original");
		final ClassLoader viewportBuild = gridBuild(location(Grid.class), CANDIDATE);
		for (int seed = FIRST_SEED; seed < FIRST_SEED + SEEDS; seed++) {
			final int config = seed % 4;
			final Function<int[], Object> stock = driver(stockBuild, stockShell, config);
			final Function<int[], Object> viewport = driver(viewportBuild, viewportShell, config);
			final int[] mode = { GridDifferentialDriver.MODE, 0, 0, 0 };
			assumeTrue("the candidate build does not run in mode " + CANDIDATE, CANDIDATE.equals(viewport.apply(mode)));
			stockShell.open();
			viewportShell.open();
			final String pixels = run(seed, config, stock, viewport);
			LOG.info("GridViewportDifferential_Test: seed " + seed + " config " + config + ", " + STEPS
					+ " steps identical, " + pixels + " (oracle " + stock.apply(mode) + ", candidate " + viewport.apply(mode) + ")");
			for (final Shell shell : new Shell[] { stockShell, viewportShell }) {
				for (final org.eclipse.swt.widgets.Control child : shell.getChildren()) {
					if (!(child instanceof Button)) {
						child.dispose();
					}
				}
			}
		}
	}

	/*
	 * Header and footer content changed after the columns exist, and the footer shown and hidden:
	 * the default mode measures the header and footer again and updates its scroll bars where stock
	 * keeps stale heights and bars (UPSTREAM_DEFECTS.tsv). Exactly the listed steps answer or paint
	 * otherwise than the oracle, and each of them does; every other step, among them the steps
	 * undoing a change, answers and paints as the oracle. The original mode differs in none.
	 */
	@Test
	public void headerAndFooterChangesDivergeExactlyWhereListed() throws Exception {
		final ClassLoader stockBuild = gridBuild(oracleClasses(), "original");
		final ClassLoader candidateBuild = gridBuild(location(Grid.class), CANDIDATE);
		final Function<int[], Object> stock = driver(stockBuild, stockShell, 0);
		final Function<int[], Object> candidate = driver(candidateBuild, viewportShell, 0);
		stockShell.open();
		viewportShell.open();
		final int[][] scene = {
				{ GridDifferentialDriver.HEADER_FONT, 1, 0, 0 }, // UPSTREAM_DEFECTS: header height after content
				{ GridDifferentialDriver.HEADER_FONT, 0, 0, 0 },
				{ GridDifferentialDriver.HEADER_IMAGE, 1, 1, 0 }, // header height after content
				{ GridDifferentialDriver.HEADER_IMAGE, 0, 1, 0 },
				{ GridDifferentialDriver.FOOTER_VISIBLE, 1, 0, 0 }, // scroll bars after footer visibility
				{ 0, 3, 0, 0 },
				{ GridDifferentialDriver.FOOTER_FONT, 1, 2, 0 }, // footer height after content
				{ GridDifferentialDriver.FOOTER_FONT, 0, 2, 0 },
				{ GridDifferentialDriver.FOOTER_TEXT, 1, 2, 0 },
				{ GridDifferentialDriver.FOOTER_VISIBLE, 0, 0, 0 }, // scroll bars after footer visibility
				{ 1, 3, 0, 0 } };
		final boolean fixes = !"original".equals(CANDIDATE);
		final java.util.Set<Integer> accepted = fixes ? java.util.Set.of(0, 2, 4, 6, 9) : java.util.Set.of();
		final int[] state = { GridDifferentialDriver.STATE, 0, 0, 0 };
		final int[] pixels = { GridDifferentialDriver.PIXELS, 0, 0, 0 };
		drain();
		final StringBuilder verdicts = new StringBuilder();
		for (int step = 0; step < scene.length; step++) {
			final Object stockAnswer = stock.apply(scene[step]);
			final Object candidateAnswer = candidate.apply(scene[step]);
			drain();
			final String expected = stockAnswer + " | " + stock.apply(state);
			final String actual = candidateAnswer + " | " + candidate.apply(state);
			final boolean samePixels = Arrays.equals(((ImageData) stock.apply(pixels)).data,
					((ImageData) candidate.apply(pixels)).data);
			final boolean same = expected.equals(actual) && samePixels;
			verdicts.append(step).append(same ? " identical" : " differs").append('\n');
			if (same == accepted.contains(step)) {
				fail("step " + step + " " + Arrays.toString(scene[step]) + (same ? " is identical but listed"
						: " differs but is not listed") + "\n  stock:     " + expected + "\n  candidate: " + actual
						+ "\n  pixels " + (samePixels ? "identical" : "differ") + "\n" + verdicts);
			}
		}
		LOG.info("GridViewportDifferential_Test: header/footer scene, " + scene.length + " steps, "
				+ accepted.size() + " listed divergences, each one observed (oracle " + stock.apply(new int[] {
						GridDifferentialDriver.MODE, 0, 0, 0 }) + ", candidate " + CANDIDATE + ")");
	}

	/*
	 * Header renderers that paint rows below their bounds (a custom empty column header and a
	 * custom column header, IRenderer.getOverflowBelow): stock paints the header first and the rows
	 * over it, so those rows show wherever a row paints nothing (right of the columns; under the
	 * columns once no rows are left). Every step answers and paints as the oracle, in both modes.
	 */
	@Test
	public void customHeaderOverflowPaintsAsStock() throws Exception {
		final ClassLoader stockBuild = gridBuild(oracleClasses(), "original");
		final ClassLoader candidateBuild = gridBuild(location(Grid.class), CANDIDATE);
		final Function<int[], Object> stock = driver(stockBuild, stockShell, 0);
		final Function<int[], Object> candidate = driver(candidateBuild, viewportShell, 0);
		stockShell.open();
		viewportShell.open();
		// a narrow last column leaves room for the empty column header right of the columns
		final int[][] scene = { { GridDifferentialDriver.OVERFLOW_RENDERERS, 1, 1, 0 }, { 13, 0, 3, 0 }, { 20, 3, 0, 0 },
				{ 20, 0, 0, 0 }, { 0, 3, 0, 0 }, { 2, 40, 0, 0 }, { GridDifferentialDriver.REMOVE_ALL, 1, 0, 0 } };
		final int[] state = { GridDifferentialDriver.STATE, 0, 0, 0 };
		final int[] pixels = { GridDifferentialDriver.PIXELS, 0, 0, 0 };
		drain();
		for (int step = 0; step < scene.length; step++) {
			final Object stockAnswer = stock.apply(scene[step]);
			final Object candidateAnswer = candidate.apply(scene[step]);
			drain();
			compare(new StringBuilder("overflow scene\n"), "step " + step, stockAnswer + " | " + stock.apply(state),
					candidateAnswer + " | " + candidate.apply(state));
			final ImageData expected = (ImageData) stock.apply(pixels);
			final ImageData actual = (ImageData) candidate.apply(pixels);
			if (!Arrays.equals(expected.data, actual.data)) {
				save(expected, "overflow-stock-" + step);
				save(actual, "overflow-candidate-" + step);
				fail("overflow scene step " + step + " " + Arrays.toString(scene[step]) + ": pixels differ "
						+ difference(expected, actual));
			}
		}
		LOG.info("GridViewportDifferential_Test: header overflow scene, " + scene.length
				+ " steps identical (oracle " + stock.apply(new int[] { GridDifferentialDriver.MODE, 0, 0, 0 })
				+ ", candidate " + CANDIDATE + ")");
	}

	/* runs one seed; answers the pixel verdict (a difference fails unless grid.differential.pixelsAdvisory) */
	private String run(final int seed, final int config, final Function<int[], Object> stock,
			final Function<int[], Object> viewport) {
		final Random random = new Random(0x6E1DL + seed);
		final StringBuilder log = new StringBuilder("seed " + seed + " config " + config + "\n");
		final int[] state = { GridDifferentialDriver.STATE, 0, 0, 0 };
		drain();
		compare(log, "initial", String.valueOf(stock.apply(state)), String.valueOf(viewport.apply(state)));
		for (int step = 0; step < STEPS; step++) {
			final int[] op = { random.nextInt(GridDifferentialDriver.KINDS), random.nextInt(1 << 20),
					random.nextInt(1 << 20), random.nextInt(1 << 20) };
			log.append(step).append(": ").append(Arrays.toString(op)).append('\n');
			final Object expected = stock.apply(op);
			final Object actual = viewport.apply(op);
			drain();
			final String stockState = expected + " | " + stock.apply(state);
			compare(log, "step " + step, stockState, actual + " | " + viewport.apply(state));
			// the last agreed answers, so a divergence report shows the state it started from
			log.append("   = ").append(stockState.length() > 240 ? stockState.substring(0, 240) : stockState).append('\n');
		}
		// each Grid is captured with its own shell active and its focus holder focused, so window
		// activation (borders, scroll bars) is the same on both sides
		final int[] pixels = { GridDifferentialDriver.PIXELS, 0, 0, 0 };
		activate(stockShell, stockFocusHolder);
		final ImageData expected = (ImageData) stock.apply(pixels);
		activate(viewportShell, focusHolder);
		final ImageData actual = (ImageData) viewport.apply(pixels);
		if (!Arrays.equals(expected.data, actual.data)) {
			save(expected, "stock-seed" + seed);
			save(actual, "viewport-seed" + seed);
			if (Boolean.getBoolean("grid.differential.pixelsAdvisory")) {
				return "pixels differ: " + difference(expected, actual);
			}
			fail("printed pixels differ after seed " + seed + " " + difference(expected, actual) + "\nlog:\n" + log);
		}
		return "pixels identical";
	}

	/* number of differing pixels and their bounding box */
	static String difference(final ImageData expected, final ImageData actual) {
		int count = 0, left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1;
		for (int y = 0; y < Math.min(expected.height, actual.height); y++) {
			for (int x = 0; x < Math.min(expected.width, actual.width); x++) {
				if (expected.getPixel(x, y) != actual.getPixel(x, y)) {
					count++;
					left = Math.min(left, x);
					top = Math.min(top, y);
					right = Math.max(right, x);
					bottom = Math.max(bottom, y);
				}
			}
		}
		return count + " pixels in [" + left + "," + top + " .. " + right + "," + bottom + "]";
	}

	private static void compare(final StringBuilder log, final String where, final String expected, final String actual) {
		if (!expected.equals(actual)) {
			fail(where + " differs\n  stock:    " + expected + "\n  viewport: " + actual + "\nlog:\n" + log);
		}
		assertEquals(expected, actual);
	}

	private void activate(final Shell shell, final Button holder) {
		shell.forceActive();
		holder.setFocus();
		drain();
	}

	private void drain() {
		while (display.readAndDispatch()) {
			// paint and process what the operation queued
		}
	}

	static void save(final ImageData data, final String name) {
		final String dir = System.getProperty("grid.differential.out");
		if (dir == null) {
			return;
		}
		final ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { data };
		loader.save(new File(dir, name + ".png").getPath(), SWT.IMAGE_PNG);
	}
}
