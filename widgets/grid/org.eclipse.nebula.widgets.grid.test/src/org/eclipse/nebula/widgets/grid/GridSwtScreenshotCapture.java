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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ScrollBar;

/** SWT-native screenshot evidence helper for Grid visual regressions. */
final class GridSwtScreenshotCapture {

	enum Method {
		CONTROL_PRINT,
		CONTROL_COPY_AREA,
		DISPLAY_COPY_AREA
	}

	static final class Result {
		private final Path path;
		private final Method method;
		private final String sha256;

		Result(final Path path, final Method method, final String sha256) {
			this.path = path;
			this.method = method;
			this.sha256 = sha256;
		}

		Path path() {
			return path;
		}

		Method method() {
			return method;
		}

		String sha256() {
			return sha256;
		}
	}

	private GridSwtScreenshotCapture() {
	}

	/**
	 * Awaits the required native observation and then a quiet interval for that
	 * counter. GTK can deliver a frame after the current event queue becomes empty;
	 * unrelated workbench events must not extend the Grid's settling interval.
	 * This only pumps events: it does not request paints or synthesize callbacks.
	 */
	static void awaitStableCounter(final Display display, final IntSupplier counter,
			final int minimum, final String message) {
		final long start = System.nanoTime();
		long unchangedSince = start;
		int observed = counter.getAsInt();
		while (observed < minimum || System.nanoTime() - unchangedSince < 200_000_000L) {
			if (System.nanoTime() - start >= 5_000_000_000L) {
				throw new AssertionError(message + ": observed " + observed + ", required " + minimum);
			}
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5L);
				} catch (final InterruptedException failure) {
					Thread.currentThread().interrupt();
					throw new AssertionError("interrupted while awaiting native observation", failure);
				}
			}
			final int current = counter.getAsInt();
			if (current != observed) {
				observed = current;
				unchangedSince = System.nanoTime();
			}
		}
	}

	static Result captureControl(final Control control, final Path path) throws IOException {
		if (control == null || path == null) {
			throw new IllegalArgumentException("control and path are required");
		}
		if (control.isDisposed()) {
			throw new IllegalArgumentException("disposed control");
		}

		final Point size = control.getSize();
		if (size.x <= 0 || size.y <= 0) {
			throw new IllegalArgumentException("control has no drawable area");
		}

		final Image image = new Image(control.getDisplay(), size.x, size.y);
		Method method = Method.CONTROL_PRINT;
		try {
			GC imageGc = new GC(image);
			try {
				if (!control.print(imageGc)) {
					method = Method.CONTROL_COPY_AREA;
					imageGc.dispose();
					imageGc = null;
					final GC controlGc = new GC(control);
					try {
						controlGc.copyArea(image, 0, 0);
					} finally {
						controlGc.dispose();
					}
				}
			} finally {
				if (imageGc != null && !imageGc.isDisposed()) {
					imageGc.dispose();
				}
			}
			savePng(image, path);
		} finally {
			image.dispose();
		}
		return new Result(path, method, sha256(path));
	}

	static Result captureNativeShell(final Control control, final Path path) throws IOException {
		if (control == null || path == null) {
			throw new IllegalArgumentException("control and path are required");
		}
		if (control.isDisposed()) {
			throw new IllegalArgumentException("disposed control");
		}

		final Shell shell = control.getShell();
		final Rectangle bounds = shell.getBounds();
		if (bounds.width <= 0 || bounds.height <= 0) {
			throw new IllegalArgumentException("shell has no drawable area");
		}

		final Display display = control.getDisplay();
		final Image image = new Image(display, bounds.width, bounds.height);
		try {
			final GC displayGc = new GC(display);
			try {
				displayGc.copyArea(image, bounds.x, bounds.y);
			} finally {
				displayGc.dispose();
			}
			savePng(image, path);
		} finally {
			image.dispose();
		}
		return new Result(path, Method.DISPLAY_COPY_AREA, sha256(path));
	}

	/** Visible outer bounds in display coordinates; reject partially clipped targets. */
	static Rectangle screenBounds (Control control) {
		Objects.requireNonNull (control, "control");
		if (control.isDisposed () || !control.isVisible ()) {
			throw new IllegalArgumentException ("screen capture requires a visible control");
		}
		Display display = control.getDisplay ();
		if (Display.getCurrent () != display) {
			throw new IllegalStateException ("screen capture requires the SWT UI thread");
		}
		Rectangle screen = outerDisplayBounds (control);
		if (screen.width <= 0 || screen.height <= 0 || !screen.equals (screen.intersection (display.getBounds ()))) {
			throw new IllegalArgumentException ("screen capture must be fully on the display: " + screen + " outside "
					+ display.getBounds () + ", shell " + control.getShell ().getBounds () + ", minimized "
					+ control.getShell ().getMinimized ());
		}
		for (Composite ancestor = control instanceof Shell ? null : control.getParent ();
				ancestor != null; ancestor = ancestor.getParent ()) {
			Rectangle client = ancestor.getClientArea ();
			Point origin = clientToDisplay (ancestor, client.x, client.y);
			Rectangle visible = new Rectangle (origin.x, origin.y, client.width, client.height);
			if (!screen.equals (screen.intersection (visible))) {
				throw new IllegalArgumentException ("screen capture is clipped by "
						+ ancestor.getClass ().getSimpleName () + ": " + screen + " outside " + visible);
			}
		}
		return screen;
	}

	private static Rectangle outerDisplayBounds (Control control) {
		Rectangle bounds = control.getBounds ();
		Point origin = control instanceof Shell ? new Point (bounds.x, bounds.y)
				: clientToDisplay (control.getParent (), bounds.x, bounds.y);
		return new Rectangle (origin.x, origin.y, bounds.width, bounds.height);
	}

	private static Point clientToDisplay (Composite parent, int x, int y) {
		Point origin = parent.toDisplay (x, y);
		// GTK3 TabFolder normalizes client x/y although its event window includes tab trim.
		if (parent instanceof TabFolder && "gtk".equals (SWT.getPlatform ())
				&& !"1".equals (System.getenv ("SWT_GTK4"))) {
			Rectangle client = parent.getClientArea ();
			Rectangle trim = parent.computeTrim (0, 0, client.width, client.height);
			origin.x -= trim.x;
			origin.y -= trim.y;
		}
		return origin;
	}

	/** Capture actual visible pixels; there is deliberately no offscreen rendering fallback. */
	static Result captureScreenControl (Control control, Path path) throws IOException {
		Objects.requireNonNull (path, "path");
		Rectangle bounds = screenBounds (control);
		Display display = control.getDisplay ();
		Image image = new Image (display, bounds.width, bounds.height);
		try {
			GC displayGc = new GC (display);
			try { displayGc.copyArea (image, bounds.x, bounds.y); }
			finally { displayGc.dispose (); }
			savePng (image, path);
		} finally { image.dispose (); }
		return new Result (path, Method.DISPLAY_COPY_AREA, sha256 (path));
	}

	/** Captures strict display pixels and retains the PNG for native-oracle diagnostics. */
	static ImageData captureScreenData(final Control control, final String label) throws IOException {
		final Path output = Path.of(System.getProperty("nebula.grid.native.oracle.evidence.dir",
				"target/m3-native-oracle-screenshots"));
		final Path path = output.resolve(label + "-" + System.nanoTime() + ".png");
		final Result result = captureScreenControl(control, path);
		if (result.method() != Method.DISPLAY_COPY_AREA) {
			throw new AssertionError("native oracle requires display-copy pixels");
		}
		final ImageData data = new ImageLoader().load(path.toString())[0];
		final Point size = control.getSize();
		if (data.width != size.x || data.height != size.y) {
			throw new AssertionError("screen-capture dimensions changed");
		}
		final java.util.Set<Integer> colors = new java.util.HashSet<>();
		for (int y = 0; y < data.height && colors.size() <= 4; y++) {
			for (int x = 0; x < data.width && colors.size() <= 4; x++) {
				colors.add(Integer.valueOf(data.getPixel(x, y)));
			}
		}
		if (colors.size() <= 4) throw new AssertionError("blank or incomplete native Grid capture");
		System.out.println("GRID_SCREEN_CAPTURE method=DISPLAY_COPY_AREA path=" + path
				+ " sha256=" + result.sha256());
		return data;
	}

	/** Immutable parent-relative GTK native scrollbar observation. */
	static final class ScrollPaintBar {
		final Rectangle track;
		final Rectangle thumb;
		/** Only proven native track/slider geometry may justify an exclusion. */
		final boolean measuredExtent;

		ScrollPaintBar(final Rectangle track, final Rectangle thumb) {
			this(track, thumb, true);
		}

		/** GTK3 may allocate a visible overlay bar as 1x1 while its slider spans many pixels. */
		static ScrollPaintBar unmeasuredGtk3Overlay(final Rectangle track, final Rectangle thumb) {
			return new ScrollPaintBar(track, thumb, false);
		}

		private ScrollPaintBar(final Rectangle track, final Rectangle thumb, final boolean measuredExtent) {
			this.track = copy(Objects.requireNonNull(track, "track"));
			this.thumb = copy(Objects.requireNonNull(thumb, "thumb"));
			this.measuredExtent = measuredExtent;
			if (this.track.width <= 0 || this.track.height <= 0 || this.thumb.width <= 0
					|| this.thumb.height <= 0) {
				throw new AssertionError("nonpositive native GTK scrollbar allocation");
			}
			if (measuredExtent ? !contains(this.track, this.thumb)
					: (this.track.width != 1 || this.track.height != 1
							|| contains(this.track, this.thumb))) {
				throw new AssertionError("unmeasured native GTK scrollbar is not a collapsed overlay allocation");
			}
		}
	}

	/**
	 * One actual GC(Display) screenshot and the two native scrollbar samples that bracketed it.
	 * Never infer an exclusion from colors, native platform guesses or a past screenshot.
	 */
	static final class ScrollPaintFrame {
		final ImageData pixels;
		final Rectangle client;
		final boolean gtk3;
		final boolean nativeOverlayMode;
		final ScrollPaintBar verticalBefore;
		final ScrollPaintBar verticalAfter;
		final ScrollPaintBar horizontalBefore;
		final ScrollPaintBar horizontalAfter;

		ScrollPaintFrame(final ImageData pixels, final Rectangle client, final boolean gtk3,
				final boolean nativeOverlayMode, final ScrollPaintBar verticalBefore, final ScrollPaintBar verticalAfter,
				final ScrollPaintBar horizontalBefore, final ScrollPaintBar horizontalAfter) {
			this.pixels = Objects.requireNonNull(pixels, "pixels");
			this.client = copy(Objects.requireNonNull(client, "client"));
			this.gtk3 = gtk3;
			this.nativeOverlayMode = nativeOverlayMode;
			this.verticalBefore = verticalBefore;
			this.verticalAfter = verticalAfter;
			this.horizontalBefore = horizontalBefore;
			this.horizontalAfter = horizontalAfter;
			if (this.client.x < 0 || this.client.y < 0 || this.client.width <= 0
					|| this.client.height <= 0 || !contains(new Rectangle(0, 0, pixels.width, pixels.height), this.client)
					|| (verticalBefore == null) != (verticalAfter == null)
					|| (horizontalBefore == null) != (horizontalAfter == null)
					|| !gtk3 && (nativeOverlayMode || verticalBefore != null || horizontalBefore != null)) {
				throw new AssertionError("native ScrollPaint frame geometry or visibility drift");
			}
			// Validate BOTH samples; all clipping is supported by observed, parent-relative bars.
			checkBars(verticalBefore, horizontalBefore);
			checkBars(verticalAfter, horizontalAfter);
			if ((verticalOverlayWidth(verticalBefore) > 0) != (verticalOverlayWidth(verticalAfter) > 0)
					|| (horizontalOverlayHeight(horizontalBefore) > 0) != (horizontalOverlayHeight(horizontalAfter) > 0)
					|| (verticalBefore != null && verticalBefore.measuredExtent != verticalAfter.measuredExtent)
					|| (horizontalBefore != null && horizontalBefore.measuredExtent != horizontalAfter.measuredExtent)) {
				throw new AssertionError("native GTK3 overlay/inset mode drift during screenshot");
			}
		}

		private void checkBars(final ScrollPaintBar vertical, final ScrollPaintBar horizontal) {
			final long right = (long) client.x + client.width;
			final long bottom = (long) client.y + client.height;
			final int verticalWidth = verticalOverlayWidth(vertical);
			final int horizontalHeight = horizontalOverlayHeight(horizontal);
			if (verticalWidth >= client.width || horizontalHeight >= client.height) {
				throw new AssertionError("native scrollbar consumes entire client");
			}
			if (vertical != null && verticalWidth > 0) {
				final Rectangle r = vertical.track;
				if (r.y > client.y || (long) r.y + r.height < bottom - horizontalHeight
						|| (long) r.y + r.height > bottom) {
					throw new AssertionError("GTK3 vertical overlay track leaves an unowned content pixel");
				}
			}
			if (horizontal != null && horizontalHeight > 0) {
				final Rectangle r = horizontal.track;
				if (r.x > client.x || (long) r.x + r.width < right - verticalWidth
						|| (long) r.x + r.width > right) {
					throw new AssertionError("GTK3 horizontal overlay track leaves an unowned content pixel");
				}
			}
		}

		/** An inset native bar owns no pixels in the Grid's client rectangle. */
		private int verticalOverlayWidth(final ScrollPaintBar bar) {
			// A collapsed GTK3 allocation is native evidence, not proof of painted width.
			// Treat the entire client as content when the native extent is unmeasurable.
			if (bar == null || !bar.measuredExtent) return 0;
			final long right = (long) client.x + client.width;
			final Rectangle r = bar.track;
			if (r.width > 32) throw new AssertionError("unbounded GTK3 native vertical bar width");
			if (r.x == right - r.width) {
				if (!nativeOverlayMode) throw new AssertionError("SWT reports inset scrollbars over Grid content");
				return r.width;
			}
			if (r.x >= right && (long) r.x <= right + 32) {
				if (nativeOverlayMode) throw new AssertionError("SWT reports overlay scrollbars outside Grid content");
				return 0;
			}
			throw new AssertionError("native vertical bar is neither edge overlay nor inset");
		}

		private int horizontalOverlayHeight(final ScrollPaintBar bar) {
			if (bar == null || !bar.measuredExtent) return 0;
			final long bottom = (long) client.y + client.height;
			final Rectangle r = bar.track;
			if (r.height > 32) throw new AssertionError("unbounded GTK3 native horizontal bar height");
			if (r.y == bottom - r.height) {
				if (!nativeOverlayMode) throw new AssertionError("SWT reports inset scrollbars over Grid content");
				return r.height;
			}
			if (r.y >= bottom && (long) r.y <= bottom + 32) {
				if (nativeOverlayMode) throw new AssertionError("SWT reports overlay scrollbars outside Grid content");
				return 0;
			}
			throw new AssertionError("native horizontal bar is neither bottom overlay nor inset");
		}

		int rightStrip() {
			return Math.max(verticalOverlayWidth(verticalBefore), verticalOverlayWidth(verticalAfter));
		}

		int bottomStrip() {
			return Math.max(horizontalOverlayHeight(horizontalBefore), horizontalOverlayHeight(horizontalAfter));
		}

	}

	private static Rectangle copy(final Rectangle rect) {
		return new Rectangle(rect.x, rect.y, rect.width, rect.height);
	}

	private static boolean contains(final Rectangle outer, final Rectangle inner) {
		return inner.x >= outer.x && inner.y >= outer.y
				&& (long) inner.x + inner.width <= (long) outer.x + outer.width
				&& (long) inner.y + inner.height <= (long) outer.y + outer.height;
	}

	private static ScrollPaintBar snapshotBar(final ScrollBar bar, final boolean gtk3,
			final boolean nativeOverlayMode) {
		if (!gtk3 || bar == null || !bar.getVisible()) return null;
		final Rectangle track = bar.getThumbTrackBounds();
		final Rectangle thumb = bar.getThumbBounds();
		// Actual GTK3 may allocate a mapped overlay scrollbar as 1x1 while the
		// slider range is larger. Never infer or mask a guessed visual strip.
		if (nativeOverlayMode && track.width == 1 && track.height == 1
				&& thumb.width > 0 && thumb.height > 0 && !contains(track, thumb)) {
			return ScrollPaintBar.unmeasuredGtk3Overlay(track, thumb);
		}
		return new ScrollPaintBar(track, thumb);
	}

	/** Capture a pre/post native geometry envelope around an unchanged GC(Display) image copy. */
	static ScrollPaintFrame captureScrollPaintFrame(final Grid grid, final Supplier<ImageData> screenCopy) {
		Objects.requireNonNull(grid, "grid");
		Objects.requireNonNull(screenCopy, "screenCopy");
		final Rectangle client = copy(grid.getClientArea());
		final Point size = grid.getSize();
		final int border = grid.getBorderWidth();
		final boolean gtk3 = "gtk".equals(SWT.getPlatform()) && !"1".equals(System.getenv("SWT_GTK4"));
		final int modeBefore = grid.getScrollbarsMode();
		if (modeBefore != SWT.NONE && modeBefore != SWT.SCROLLBAR_OVERLAY) {
			throw new AssertionError("unsupported native scrollbar mode");
		}
		final boolean nativeOverlayMode = gtk3 && modeBefore == SWT.SCROLLBAR_OVERLAY;
		final ScrollPaintBar v0 = snapshotBar(grid.getVerticalBar(), gtk3, nativeOverlayMode);
		final ScrollPaintBar h0 = snapshotBar(grid.getHorizontalBar(), gtk3, nativeOverlayMode);
		final ImageData pixels = screenCopy.get(); // The original actual-screen GC(Display) copy.
		final ScrollPaintBar v1 = snapshotBar(grid.getVerticalBar(), gtk3, nativeOverlayMode);
		final ScrollPaintBar h1 = snapshotBar(grid.getHorizontalBar(), gtk3, nativeOverlayMode);
		final int modeAfter = grid.getScrollbarsMode();
		if (modeAfter != modeBefore) {
			throw new AssertionError("native scrollbar mode changed during display capture");
		}
		if (!client.equals(grid.getClientArea()) || !size.equals(grid.getSize())
				|| border != grid.getBorderWidth() || pixels.width != size.x || pixels.height != size.y) {
			throw new AssertionError("native Grid/scrollbar geometry moved while copying display pixels");
		}
		return new ScrollPaintFrame(pixels, client, gtk3,
				nativeOverlayMode, v0, v1, h0, h1);
	}

	/** Common half-open Grid-content rectangle of two actual measured screen frames. */
	static Rectangle scrollPaintContentClip(final ScrollPaintFrame first, final ScrollPaintFrame second) {
		Objects.requireNonNull(first, "first");
		Objects.requireNonNull(second, "second");
		if (!first.client.equals(second.client) || first.pixels.width != second.pixels.width
				|| first.pixels.height != second.pixels.height || first.gtk3 != second.gtk3
				|| first.nativeOverlayMode != second.nativeOverlayMode
				|| (first.verticalBefore == null) != (second.verticalBefore == null)
				|| (first.horizontalBefore == null) != (second.horizontalBefore == null)
				|| (first.rightStrip() > 0) != (second.rightStrip() > 0)
				|| (first.bottomStrip() > 0) != (second.bottomStrip() > 0)) {
			throw new AssertionError("native ScrollPaint frames have incomparable platform/geometry/visibility");
		}
		final int right = Math.max(first.rightStrip(), second.rightStrip());
		final int bottom = Math.max(first.bottomStrip(), second.bottomStrip());
		if ((first.verticalBefore != null && second.verticalBefore != null
					&& first.verticalBefore.measuredExtent != second.verticalBefore.measuredExtent)
				|| (first.horizontalBefore != null && second.horizontalBefore != null
					&& first.horizontalBefore.measuredExtent != second.horizontalBefore.measuredExtent)) {
			throw new AssertionError("unmeasured/native-measured ScrollPaint ownership drift");
		}
		if (first.client.width <= right || first.client.height <= bottom) {
			throw new AssertionError("no stable Grid content pixels remain");
		}
		return new Rectangle(first.client.x, first.client.y,
				first.client.width - right, first.client.height - bottom);
	}

	/** Compare every non-chrome content pixel; any changed body pixel remains fatal. */
	static boolean sameScrollPaintedContent(final ScrollPaintFrame screen, final ImageData expected,
			final ScrollPaintFrame otherScreen) {
		Objects.requireNonNull(expected, "expected");
		final Rectangle content = scrollPaintContentClip(screen, otherScreen);
		if (screen.pixels.width != expected.width || screen.pixels.height != expected.height) {
			throw new AssertionError("native screen and model paint dimensions differ");
		}
		for (int y = content.y; y < content.y + content.height; y++) {
			for (int x = content.x; x < content.x + content.width; x++) {
				if (screen.pixels.getPixel(x, y) != expected.getPixel(x, y)) return false;
			}
		}
		return true;
	}

	/**
	 * A captured native frame and its measured GTK scrollbar overlay width.
	 * A scrollbar may animate between two frames. Keep its geometry with the
	 * pixels so comparisons use one common exclusion width, never two different
	 * numbers of hashed pixels.
	 */
	static final class GridClientFrame {
		final ImageData pixels;
		final Rectangle client;
		final int border;
		final int overlayWidth;

		GridClientFrame(ImageData pixels, Rectangle client, int border, int overlayWidth) {
			this.pixels = Objects.requireNonNull(pixels, "pixels");
			this.client = Objects.requireNonNull(client, "client");
			this.border = border;
			this.overlayWidth = overlayWidth;
		}
	}

	/**
	 * Capture real display pixels, not Control.print. Measure the native overlay
	 * on both sides of capture to include animated width changes conservatively.
	 */
	static GridClientFrame captureGridClientFrame(final Grid grid, final String label)
			throws IOException {
		final Rectangle clientBefore = grid.getClientArea();
		final int border = grid.getBorderWidth();
		final int overlayBefore = gtkOverlayWidth(grid);
		final ImageData pixels = captureScreenData(grid, label);
		final int overlayAfter = gtkOverlayWidth(grid);
		if (!clientBefore.equals(grid.getClientArea()) || border != grid.getBorderWidth()) {
			throw new AssertionError("Grid geometry changed during native screen capture");
		}
		return new GridClientFrame(pixels, clientBefore, border,
				Math.max(overlayBefore, overlayAfter));
	}

	/**
	 * Hash two native captures with the SAME measured exclusion width. Mask the
	 * union of their independently measured overlay strips only, never the body.
	 * Reusing two separately computed digests would hash different pixel counts
	 * when GTK3 animates the right-aligned scrollbar from 4 to 6 pixels.
	 */
	static String[] comparableGridClientHashes(final Grid grid,
			final GridClientFrame earlier, final GridClientFrame later,
			final Rectangle region) {
		Objects.requireNonNull(grid, "grid");
		Objects.requireNonNull(earlier, "earlier");
		Objects.requireNonNull(later, "later");
		if (earlier.border != later.border || !earlier.client.equals(later.client)
				|| !earlier.client.equals(grid.getClientArea())
				|| earlier.border != grid.getBorderWidth()
				|| earlier.pixels.width != later.pixels.width
				|| earlier.pixels.height != later.pixels.height) {
			throw new AssertionError("native Grid content geometry changed between compared frames");
		}
		final int overlay = Math.max(earlier.overlayWidth, later.overlayWidth);
		return new String[] {
				gridClientContentHash(grid, earlier.pixels, region, overlay),
				gridClientContentHash(grid, later.pixels, region, overlay)
		};
	}

	/**
	 * Existing single-frame content digest remains available to other tests.
	 * Paired visual comparisons must instead use comparableGridClientHashes.
	 */
	static String gridClientContentHash(final Grid grid, final ImageData image,
			final Rectangle clientRegion) {
		return gridClientContentHash(grid, image, clientRegion, gtkOverlayWidth(grid));
	}

	private static String gridClientContentHash(final Grid grid, final ImageData image,
			final Rectangle clientRegion, final int overlayWidth) {
		Objects.requireNonNull(grid, "grid");
		Objects.requireNonNull(image, "image");
		Objects.requireNonNull(clientRegion, "clientRegion");
		final Rectangle client = grid.getClientArea();
		if (clientRegion.x < 0 || clientRegion.y < 0 || clientRegion.width <= 0
				|| clientRegion.height <= 0
				|| (long)clientRegion.x + clientRegion.width > client.width
				|| (long)clientRegion.y + clientRegion.height > client.height) {
			throw new AssertionError("Grid content region outside native client: " + clientRegion);
		}
		if (overlayWidth < 0 || overlayWidth > 32) {
			throw new AssertionError("invalid GTK overlay strip width: " + overlayWidth);
		}
		final int border = grid.getBorderWidth();
		final Rectangle captureRegion = new Rectangle(border + clientRegion.x,
				border + clientRegion.y, clientRegion.width, clientRegion.height);
		return exactContentHash(image, captureRegion,
				border + client.width - overlayWidth);
	}

	/** Only the native GTK3 vertical scrollbar's actual bounded track may be omitted. */
	private static int gtkOverlayWidth(final Grid grid) {
		final org.eclipse.swt.widgets.ScrollBar vertical = grid.getVerticalBar();
		if (!"gtk".equals(SWT.getPlatform())
				|| "1".equals(System.getenv("SWT_GTK4"))
				|| vertical == null || !vertical.getVisible()) {
			return 0;
		}
		final Rectangle track = vertical.getThumbTrackBounds();
		final Rectangle thumb = vertical.getThumbBounds();
		if (track.width <= 0 || track.width > 32 || track.height <= 0
				|| thumb.width <= 0 || thumb.height <= 0
				|| thumb.width > track.width || thumb.height > track.height
				|| thumb.y < track.y || (long)thumb.y + thumb.height > (long)track.y + track.height) {
			throw new AssertionError("invalid measured native GTK3 scrollbar geometry: "
					+ track + " thumb=" + thumb);
		}
		return track.width;
	}

	/**
	 * Exact RGB-word digest of the non-overlay part of a capture rectangle.
	 * The excluded strip must be explicit and cannot hide changes to body pixels.
	 * Package-local for deterministic native-image regression fixtures.
	 */
	static String exactContentHash(final ImageData image, final Rectangle region,
			final int rightLimit) {
		Objects.requireNonNull(image, "image");
		Objects.requireNonNull(region, "region");
		if (region.x < 0 || region.y < 0 || region.width <= 0 || region.height <= 0
				|| (long)region.x + region.width > image.width
				|| (long)region.y + region.height > image.height
				|| rightLimit > image.width || rightLimit <= region.x) {
			throw new AssertionError("invalid native content/scrollbar region: " + region
					+ ", rightLimit=" + rightLimit);
		}
		final int endX = Math.min(region.x + region.width, rightLimit);
		final MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (final NoSuchAlgorithmException impossible) {
			throw new AssertionError(impossible);
		}
		for (int y = region.y; y < region.y + region.height; y++) {
			for (int x = region.x; x < endX; x++) {
				final int pixel = image.getPixel(x, y);
				digest.update((byte)pixel);
				digest.update((byte)(pixel >>> 8));
				digest.update((byte)(pixel >>> 16));
			}
		}
		final byte[] hash = digest.digest();
		final StringBuilder hex = new StringBuilder(hash.length * 2);
		for (final byte b : hash) {
			hex.append(Character.forDigit((b >>> 4) & 0x0f, 16));
			hex.append(Character.forDigit(b & 0x0f, 16));
		}
		return hex.toString();
	}

	private static void savePng(final Image image, final Path path) throws IOException {
		final Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		final ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { image.getImageData() };
		loader.save(path.toString(), SWT.IMAGE_PNG);
	}

	private static String sha256(final Path path) throws IOException {
		try {
			final byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
			final StringBuilder hex = new StringBuilder(digest.length * 2);
			for (final byte value : digest) {
				hex.append(Character.forDigit((value >>> 4) & 0x0f, 16));
				hex.append(Character.forDigit(value & 0x0f, 16));
			}
			return hex.toString();
		} catch (final NoSuchAlgorithmException impossible) {
			throw new AssertionError(impossible);
		}
	}
}
