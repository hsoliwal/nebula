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
 *******************************************************************************/
package org.eclipse.nebula.widgets.web;

import static org.junit.Assert.*;

import org.eclipse.nebula.widgets.web.model.WebModel;
import org.eclipse.nebula.widgets.web.model.WebPaint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Native Font/Image/GC contracts; no Shell.open and no visible-screen claim. */
public class WebCanvasFontCache_Test {

	private static final int WIDTH = 256, HEIGHT = 72, STYLE = 18;
	private Display display;
	private boolean ownsDisplay;
	private Shell shell;
	private Font firstBase, secondBase;
	private WebCanvasCache cache;

	private static final class LabelModel extends WebModel {
		LabelModel() {
			super("font-cache-test", 0, 0, 0, 0);
		}

		@Override
		public int aspects() {
			return 0;
		}

		@Override
		protected void doLayout() {
			// The text record supplies this fixture's geometry.
		}

		@Override
		protected void emit(WebPaint target) {
			target.text(4, 4, WIDTH - 8, HEIGHT - 8, 0x17374a, STYLE, "MWil 0123456789 AaBb");
		}
	}

	@Before
	public void createNativeFonts() {
		display = Display.getCurrent();
		ownsDisplay = display == null;
		if (ownsDisplay) {
			display = new Display();
		}
		shell = new Shell(display);
		firstBase = new Font(display, display.getSystemFont().getFontData());
		String firstName = family(firstBase);
		for (FontData data : display.getFontList(null, true)) {
			if (firstName.equalsIgnoreCase(data.getName())) {
				continue;
			}
			Font candidate = new Font(display, data.getName(), 12, SWT.NORMAL);
			if (!firstName.equalsIgnoreCase(family(candidate))) {
				secondBase = candidate;
				break;
			}
			candidate.dispose();
		}
		assertNotNull("Two distinct native font families are required; this test must not be skipped", secondBase);
		cache = new WebCanvasCache(display);
	}

	@After
	public void disposeOwnedGraphics() {
		if (cache != null) {
			cache.dispose();
		}
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
		if (firstBase != null && !firstBase.isDisposed()) {
			firstBase.dispose();
		}
		if (secondBase != null && !secondBase.isDisposed()) {
			secondBase.dispose();
		}
		if (ownsDisplay && display != null && !display.isDisposed()) {
			display.dispose();
		}
	}

	private static String family(Font font) {
		return font.getFontData()[0].getName();
	}

	@Test
	public void unchangedBaseAndFontStyleReuseTheNativeResource() {
		Font font = cache.font(firstBase, STYLE);
		assertSame(font, cache.font(firstBase, STYLE));
		assertSame(font, cache.font(firstBase, STYLE | WebPaint.CENTER | WebPaint.UNDERLINE));
		Font bold = cache.font(firstBase, STYLE | WebPaint.BOLD);
		assertNotSame(font, bold);
		assertSame(bold, cache.font(firstBase, STYLE | WebPaint.BOLD));
		assertEquals(family(firstBase), family(font));
		assertEquals(family(firstBase), family(bold));
	}

	@Test
	public void repeatedDefaultControlFontQueriesKeepOneDerivedFont() {
		WebCanvas<LabelModel> canvas = new WebCanvas<>(shell, SWT.NONE, new LabelModel());
		Font derived = cache.font(canvas.getFont(), STYLE);
		for (int sample = 0; sample < 64; sample++) {
			assertSame(derived, cache.font(canvas.getFont(), STYLE));
		}
		canvas.dispose();
		assertFalse(derived.isDisposed());
	}

	@Test
	public void changedBaseUsesNewFamilyAndPreservesPriorFontsUntilDisposal() {
		Font plain = cache.font(firstBase, STYLE);
		Font bold = cache.font(firstBase, STYLE | WebPaint.BOLD);
		Color color = cache.color(0x17374a);
		Font replacement = cache.font(secondBase, STYLE);
		assertEquals(family(secondBase), family(replacement));
		assertFalse(plain.isDisposed());
		assertFalse(bold.isDisposed());
		assertFalse(firstBase.isDisposed());
		assertFalse(secondBase.isDisposed());
		assertFalse(color.isDisposed());
		assertSame(color, cache.color(0x17374a));
		assertSame(replacement, cache.font(secondBase, STYLE));
		assertSame(plain, cache.font(firstBase, STYLE));
		assertSame(bold, cache.font(firstBase, STYLE | WebPaint.BOLD));
		cache.dispose();
		assertTrue(plain.isDisposed());
		assertTrue(bold.isDisposed());
		assertTrue(replacement.isDisposed());
		assertFalse(firstBase.isDisposed());
		assertFalse(secondBase.isDisposed());
	}

	@Test
	public void aDisposedFormerBaseDoesNotPreventReplacement() {
		Font previous = cache.font(firstBase, STYLE);
		firstBase.dispose();
		Font current = cache.font(secondBase, STYLE);
		assertEquals(family(secondBase), family(current));
		assertFalse(previous.isDisposed());
		assertFalse(secondBase.isDisposed());
	}

	@Test
	public void repeatedBaseChangesReuseEveryExistingFamilyStyle() {
		Font[][] seen = new Font[2][];
		for (int turn = 0; turn < 64; turn++) {
			int family = turn & 1;
			Font base = family == 0 ? firstBase : secondBase;
			Font[] current = { cache.font(base, STYLE), cache.font(base, STYLE | WebPaint.BOLD),
					cache.font(base, STYLE + 2) };
			if (seen[family] == null) {
				seen[family] = current;
			} else {
				for (int style = 0; style < current.length; style++) {
					assertSame("A known family/style must not allocate another native font",
							seen[family][style], current[style]);
				}
			}
			for (Font font : current) {
				assertFalse(font.isDisposed());
				assertEquals(family(base), family(font));
			}
		}
		cache.dispose();
		for (Font[] fonts : seen) {
			for (Font font : fonts) {
				assertTrue(font.isDisposed());
			}
		}
		assertFalse(firstBase.isDisposed());
		assertFalse(secondBase.isDisposed());
	}

	@Test
	public void monospacedStyleKeepsItsExistingFamilyIndependentOfBase() {
		Font mono = cache.font(firstBase, STYLE | WebPaint.MONO);
		assertSame(mono, cache.font(secondBase, STYLE | WebPaint.MONO));
		assertSame(mono, cache.font(firstBase, STYLE | WebPaint.MONO));
	}

	@Test
	public void changingBaseBetweenDrawsKeepsTheCurrentNativeGCUsable() {
		Image image = new Image(display, WIDTH, HEIGHT);
		GC gc = new GC(image);
		try {
			Font previous = cache.font(firstBase, STYLE);
			gc.setFont(previous);
			gc.drawText("First family", 4, 4, true);
			Font current = cache.font(secondBase, STYLE);
			gc.setFont(current);
			gc.drawText("Second family", 4, 36, true);
			assertEquals(family(secondBase), family(gc.getFont()));
			assertFalse("The prior font may still be selected in a native GC", previous.isDisposed());
			assertFalse(current.isDisposed());
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	@Test
	public void setFontAfterPaintingMatchesAnIndependentFreshCanvas() {
		WebCanvas<LabelModel> warm = new WebCanvas<>(shell, SWT.NONE, new LabelModel());
		WebCanvas<LabelModel> fresh = new WebCanvas<>(shell, SWT.NONE, new LabelModel());
		warm.setSize(WIDTH, HEIGHT);
		fresh.setSize(WIDTH, HEIGHT);
		warm.setFont(firstBase);
		paint(warm, firstBase);
		warm.setFont(secondBase);
		fresh.setFont(secondBase);
		ImageData actual = paint(warm, secondBase);
		ImageData expected = paint(fresh, secondBase);
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				assertEquals("pixel " + x + "," + y,
						expected.palette.getRGB(expected.getPixel(x, y)),
						actual.palette.getRGB(actual.getPixel(x, y)));
				assertEquals(expected.getAlpha(x, y), actual.getAlpha(x, y));
			}
		}
		warm.dispose();
		fresh.dispose();
		assertFalse(firstBase.isDisposed());
		assertFalse(secondBase.isDisposed());
	}

	private ImageData paint(WebCanvas<?> canvas, Font expectedBase) {
		Image image = new Image(display, WIDTH, HEIGHT);
		GC gc = new GC(image);
		try {
			Event event = new Event();
			event.gc = gc;
			event.width = WIDTH;
			event.height = HEIGHT;
			canvas.notifyListeners(SWT.Paint, event);
			assertEquals(family(expectedBase), family(gc.getFont()));
			return image.getImageData();
		} finally {
			gc.dispose();
			image.dispose();
		}
	}
}
