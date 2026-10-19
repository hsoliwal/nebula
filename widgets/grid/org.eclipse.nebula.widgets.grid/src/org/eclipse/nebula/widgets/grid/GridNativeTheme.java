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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

/**
 * The platform's visual styles, for the parts of a Grid that look like the native list and tree:
 * on Windows the themed parts the Explorer list view, tree view, header and check box draw (the
 * same {@code uxtheme} calls SWT's own {@code Table} and {@code Tree} use). The Grid bundle is
 * platform independent, so SWT's Windows internals are reached reflectively; on other platforms,
 * or when they are missing, {@link #isAvailable()} is {@code false} and the renderers fall back to
 * system colours.
 * <p>
 * Theme handles are opened per Grid and closed when it is disposed.
 * </p>
 */
final class GridNativeTheme {

	/** Theme classes. */
	static final String LIST = "Explorer::ListView", TREE = "Explorer::TreeView", HEADER = "Header", BUTTON = "Button";

	/** List and tree item part and states (vssym32.h: LVP_LISTITEM, LISS_*, TVP_TREEITEM, TREIS_*). */
	static final int ITEM = 1, ITEM_NORMAL = 1, ITEM_HOT = 2, ITEM_SELECTED = 3, ITEM_DISABLED = 4,
			ITEM_SELECTED_NOT_FOCUS = 5, ITEM_HOT_SELECTED = 6;

	/** Tree glyph part and states (TVP_GLYPH, GLPS_CLOSED, GLPS_OPENED; TVP_HOTGLYPH). */
	static final int GLYPH = 2, HOT_GLYPH = 4, GLYPH_CLOSED = 1, GLYPH_OPENED = 2;

	/** Header item part and states (HP_HEADERITEM, HIS_NORMAL, HIS_HOT, HIS_PRESSED). */
	static final int HEADER_ITEM = 1, HEADER_NORMAL = 1, HEADER_HOT = 2, HEADER_PRESSED = 3;

	/** Check box part and states (BP_CHECKBOX, CBS_*). */
	static final int CHECK_BOX = 3, UNCHECKED = 1, UNCHECKED_DISABLED = 4, CHECKED = 5, CHECKED_DISABLED = 8, MIXED = 9,
			MIXED_DISABLED = 12;

	/** The list's column line part (LVP_COLUMNDETAIL). */
	static final int COLUMN_DETAIL = 10;

	/** DrawText flags: one line, vertically centred, end ellipsis, no prefix. */
	static final int TEXT_FLAGS = 0x20 | 0x4 | 0x8000 | 0x800;

	/* WM_QUERYUISTATE and its UISF_HIDEFOCUS bit */
	private static final int QUERY_UI_STATE = 0x129, HIDE_FOCUS = 0x1;

	/* theme part size kind TS_TRUE */
	private static final int TRUE_SIZE = 1;

	private static final Api API = Api.load();

	private final Control control;
	private final long[] handles = new long[4];
	private final String[] classes = { LIST, TREE, HEADER, BUTTON };
	private final Map<String, Image> images = new HashMap<>();

	/**
	 * @param control the control the themes are opened for
	 */
	GridNativeTheme(final Control control) {
		this.control = control;
		control.addListener(SWT.Dispose, e -> close());
	}

	/** @return whether themed drawing is available on this platform */
	boolean isAvailable() {
		return API != null;
	}

	/**
	 * Draws a theme part.
	 *
	 * @param gc         the GC (its clipping applies)
	 * @param themeClass one of {@link #LIST}, {@link #TREE}, {@link #HEADER}, {@link #BUTTON}
	 * @param part       the part
	 * @param state      the state
	 * @param bounds     where the part is drawn
	 * @param clip       the part of {@code bounds} to draw, or {@code null}
	 * @return whether the part was drawn
	 */
	boolean draw(final GC gc, final String themeClass, final int part, final int state, final Rectangle bounds,
			final Rectangle clip) {
		final long theme = theme(themeClass);
		return theme != 0 && API.draw(theme, gc.handle, part, state, bounds, clip);
	}

	/**
	 * @return whether the window hides keyboard focus cues (the native tree then draws no focus
	 *         rectangle); {@code false} when unknown
	 */
	boolean hidesFocusCues() {
		return API != null && (API.uiState(control) & HIDE_FOCUS) != 0;
	}

	/**
	 * Draws text the way a theme part draws it (font from the GC, colour from the theme).
	 *
	 * @param gc         the GC
	 * @param themeClass the theme class
	 * @param part       the part
	 * @param state      the state
	 * @param text       the text
	 * @param bounds     the text rectangle
	 * @return whether the text was drawn
	 */
	boolean drawText(final GC gc, final String themeClass, final int part, final int state, final String text,
			final Rectangle bounds) {
		final long theme = theme(themeClass);
		return theme != 0 && API.drawText(theme, gc, part, state, text, bounds);
	}

	/**
	 * Draws a theme part the way SWT's native list shows its check boxes: SWT paints the part on a
	 * bitmap filled with the list background (the part's anti-aliased edges are blended once), and
	 * the list view then draws that bitmap through its 32-bit image list, which blends the edges a
	 * second time with the alpha the first paint left in the bitmap. The result is computed from two
	 * renderings of the part, on white and on black, and cached per part, state, size and
	 * background.
	 *
	 * @param gc         the GC
	 * @param themeClass the theme class
	 * @param part       the part
	 * @param state      the state
	 * @param bounds     where the part is drawn
	 * @param background the background the part is painted on
	 * @return whether the part was drawn
	 */
	boolean drawOpaque(final GC gc, final String themeClass, final int part, final int state, final Rectangle bounds,
			final Color background) {
		final long theme = theme(themeClass);
		if (theme == 0) {
			return false;
		}
		final String key = themeClass + '/' + part + '/' + state + '/' + bounds.width + 'x' + bounds.height + '/'
				+ background.getRGB();
		Image image = images.get(key);
		if (image == null) {
			final ImageData onWhite = render(theme, part, state, bounds.width, bounds.height, SWT.COLOR_WHITE);
			final ImageData onBlack = render(theme, part, state, bounds.width, bounds.height, SWT.COLOR_BLACK);
			final RGB back = background.getRGB();
			final ImageData result = new ImageData(bounds.width, bounds.height, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
			for (int y = 0; y < bounds.height; y++) {
				for (int x = 0; x < bounds.width; x++) {
					final RGB w = onWhite.palette.getRGB(onWhite.getPixel(x, y));
					final RGB b = onBlack.palette.getRGB(onBlack.getPixel(x, y));
					// alpha of the part at this pixel, as the first blend left it in the bitmap
					final int alpha = 255 - Math.max(w.red - b.red, Math.max(w.green - b.green, w.blue - b.blue));
					final RGB first = background.getRGB().equals(new RGB(255, 255, 255)) ? w
							: new RGB(blend(b.red, back.red, alpha), blend(b.green, back.green, alpha), blend(b.blue, back.blue, alpha));
					result.setPixel(x, y, result.palette.getPixel(new RGB(straight(first.red, back.red, alpha),
							straight(first.green, back.green, alpha), straight(first.blue, back.blue, alpha))));
				}
			}
			image = new Image(control.getDisplay(), result);
			images.put(key, image);
		}
		gc.drawImage(image, bounds.x, bounds.y);
		return true;
	}

	/* the part painted on a bitmap filled with a system colour */
	private ImageData render(final long theme, final int part, final int state, final int width, final int height,
			final int fill) {
		final Image image = new Image(control.getDisplay(), width, height);
		try {
			final GC imageGc = new GC(image);
			try {
				imageGc.setBackground(control.getDisplay().getSystemColor(fill));
				imageGc.fillRectangle(0, 0, width, height);
				API.draw(theme, imageGc.handle, part, state, new Rectangle(0, 0, width, height), null);
			} finally {
				imageGc.dispose();
			}
			return image.getImageData();
		} finally {
			image.dispose();
		}
	}

	/* the first blend: premultiplied part colour over the background */
	private static int blend(final int premultiplied, final int background, final int alpha) {
		return Math.min(255, premultiplied + (background * (255 - alpha) + 127) / 255);
	}

	/* the second blend: the bitmap colour with straight alpha over the background */
	private static int straight(final int color, final int background, final int alpha) {
		return (color * alpha + background * (255 - alpha) + 127) / 255;
	}

	/**
	 * @param gc         the GC
	 * @param themeClass the theme class
	 * @param part       the part
	 * @param state      the state
	 * @return the part's true size, or {@code null} when unavailable
	 */
	Point size(final GC gc, final String themeClass, final int part, final int state) {
		final long theme = theme(themeClass);
		return theme == 0 ? null : API.size(theme, gc.handle, part, state);
	}

	private long theme(final String themeClass) {
		if (API == null) {
			return 0;
		}
		for (int i = 0; i < classes.length; i++) {
			if (classes[i].equals(themeClass)) {
				if (handles[i] == 0) {
					handles[i] = API.open(control, themeClass);
				}
				return handles[i];
			}
		}
		return 0;
	}

	private void close() {
		for (final Image image : images.values()) {
			image.dispose();
		}
		images.clear();
		for (int i = 0; i < handles.length; i++) {
			if (handles[i] != 0) {
				API.close(handles[i]);
				handles[i] = 0;
			}
		}
	}

	/** The reflective bridge to SWT's Windows {@code OS} class; {@code null} elsewhere. */
	private static final class Api {
		private final Method open, close, draw, partSize, text, send;
		private final Constructor<?> rect, size;
		private final Field left, top, right, bottom, cx, cy, handle;

		private Api(final Class<?> os, final Class<?> rectClass, final Class<?> sizeClass) throws ReflectiveOperationException {
			open = os.getMethod("OpenThemeData", long.class, char[].class, int.class);
			close = os.getMethod("CloseThemeData", long.class);
			draw = os.getMethod("DrawThemeBackground", long.class, long.class, int.class, int.class, rectClass, rectClass);
			partSize = os.getMethod("GetThemePartSize", long.class, long.class, int.class, int.class, rectClass, int.class,
					sizeClass);
			text = os.getMethod("DrawThemeText", long.class, long.class, int.class, int.class, char[].class, int.class,
					int.class, int.class, rectClass);
			send = os.getMethod("SendMessage", long.class, int.class, long.class, long.class);
			rect = rectClass.getConstructor();
			size = sizeClass.getConstructor();
			left = rectClass.getField("left");
			top = rectClass.getField("top");
			right = rectClass.getField("right");
			bottom = rectClass.getField("bottom");
			cx = sizeClass.getField("cx");
			cy = sizeClass.getField("cy");
			handle = Control.class.getField("handle");
		}

		static Api load() {
			if (!"win32".equals(SWT.getPlatform())) {
				return null;
			}
			try {
				final ClassLoader loader = SWT.class.getClassLoader();
				return new Api(Class.forName("org.eclipse.swt.internal.win32.OS", true, loader),
						Class.forName("org.eclipse.swt.internal.win32.RECT", true, loader),
						Class.forName("org.eclipse.swt.internal.win32.SIZE", true, loader));
			} catch (final ReflectiveOperationException | LinkageError e) {
				return null;
			}
		}

		long open(final Control control, final String themeClass) {
			try {
				final char[] name = (themeClass + '\0').toCharArray();
				// the visual styles of the control's monitor DPI, as SWT's own controls open them
				final int dpi = control.getMonitor().getZoom() * 96 / 100;
				return ((Long) open.invoke(null, Long.valueOf(handle.getLong(control)), name, Integer.valueOf(dpi))).longValue();
			} catch (final ReflectiveOperationException e) {
				return 0;
			}
		}

		int uiState(final Control control) {
			try {
				return (int) ((Long) send.invoke(null, Long.valueOf(handle.getLong(control)), Integer.valueOf(QUERY_UI_STATE),
						Long.valueOf(0), Long.valueOf(0))).longValue();
			} catch (final ReflectiveOperationException e) {
				return 0;
			}
		}

		void close(final long theme) {
			try {
				close.invoke(null, Long.valueOf(theme));
			} catch (final ReflectiveOperationException e) {
				// the handle is gone with the process
			}
		}

		boolean draw(final long theme, final long hdc, final int part, final int state, final Rectangle bounds,
				final Rectangle clip) {
			try {
				draw.invoke(null, Long.valueOf(theme), Long.valueOf(hdc), Integer.valueOf(part), Integer.valueOf(state),
						rect(bounds), clip == null ? null : rect(clip));
				return true;
			} catch (final ReflectiveOperationException e) {
				return false;
			}
		}

		boolean drawText(final long theme, final GC gc, final int part, final int state, final String string,
				final Rectangle bounds) {
			try {
				final char[] chars = string.toCharArray();
				text.invoke(null, Long.valueOf(theme), Long.valueOf(gc.handle), Integer.valueOf(part), Integer.valueOf(state),
						chars, Integer.valueOf(chars.length), Integer.valueOf(TEXT_FLAGS), Integer.valueOf(0), rect(bounds));
				return true;
			} catch (final ReflectiveOperationException e) {
				return false;
			}
		}

		Point size(final long theme, final long hdc, final int part, final int state) {
			try {
				final Object result = size.newInstance();
				partSize.invoke(null, Long.valueOf(theme), Long.valueOf(hdc), Integer.valueOf(part), Integer.valueOf(state),
						null, Integer.valueOf(TRUE_SIZE), result);
				return new Point(cx.getInt(result), cy.getInt(result));
			} catch (final ReflectiveOperationException e) {
				return null;
			}
		}

		private Object rect(final Rectangle r) throws ReflectiveOperationException {
			final Object result = rect.newInstance();
			left.setInt(result, r.x);
			top.setInt(result, r.y);
			right.setInt(result, r.x + r.width);
			bottom.setInt(result, r.y + r.height);
			return result;
		}
	}
}
