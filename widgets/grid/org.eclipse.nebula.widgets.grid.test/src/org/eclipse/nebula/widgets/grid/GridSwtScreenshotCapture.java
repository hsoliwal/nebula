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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

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
			throw new IllegalArgumentException ("screen capture must be fully on the display");
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

	/**
	 * Compare the original Grid's actual on-screen client pixels, never an
	 * offscreen Control.print image. GTK3's right-aligned overlay scrollbar
	 * paints independently from the Grid body and can animate its thumb even
	 * between a blit and a full redraw. Exclude only the native scrollbar's
	 * measured track width, with a hard cap; all remaining pixels are exact.
	 */
	static String gridClientContentHash(final Grid grid, final ImageData image,
			final Rectangle clientRegion) {
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
		final int border = grid.getBorderWidth();
		final Rectangle captureRegion = new Rectangle(border + clientRegion.x,
				border + clientRegion.y, clientRegion.width, clientRegion.height);
		int rightLimit = border + client.width;
		final org.eclipse.swt.widgets.ScrollBar vertical = grid.getVerticalBar();
		if ("gtk".equals(SWT.getPlatform())
				&& !"1".equals(System.getenv("SWT_GTK4"))
				&& vertical != null && vertical.getVisible()) {
			final Rectangle track = vertical.getThumbTrackBounds();
			final Rectangle thumb = vertical.getThumbBounds();
			if (track.width <= 0 || track.width > 32 || track.height <= 0
					|| thumb.width <= 0 || thumb.height <= 0
					|| thumb.width > track.width || thumb.height > track.height
					|| thumb.y < track.y || (long)thumb.y + thumb.height > (long)track.y + track.height) {
				throw new AssertionError("invalid measured native GTK3 scrollbar geometry: "
						+ track + " thumb=" + thumb);
			}
			rightLimit -= track.width;
		}
		return exactContentHash(image, captureRegion, rightLimit);
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
