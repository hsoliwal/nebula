/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

/**
 * The drawn browser's images: decoded off the UI thread, made into SWT images on it, kept in a bounded cache that
 * disposes the least recently used one when full. A source is asked for only when its block is painted, so a page
 * loads the images that are shown. Sources: {@code data:} URIs (base64) and URLs {@link URLConnection} opens
 * ({@code file:}, {@code http:}, {@code https:}). A source that fails is not asked for again.
 */
public final class ImageStore {

	private static final int TIMEOUT_MS = 10_000;

	private final Display display;
	private final int capacity;
	private final BiConsumer<String, Image> loaded;
	private final Map<String, Image> images = new LinkedHashMap<>(16, .75f, true);
	private final Set<String> pending = new HashSet<>(), failed = new HashSet<>();
	private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "DrawnBrowser images");
		thread.setDaemon(true);
		return thread;
	});
	private boolean disposed;

	/**
	 * @param capacity the number of images kept
	 * @param loaded   told on the UI thread when an image has loaded
	 */
	public ImageStore(Display display, int capacity, BiConsumer<String, Image> loaded) {
		this.display = display;
		this.capacity = capacity;
		this.loaded = loaded;
	}

	/** The image of {@code source}, or null while it loads (asking starts the load) or when it failed. */
	public Image get(String source) {
		Image image = images.get(source);
		if (image != null) return image;
		if (disposed || failed.contains(source) || !pending.add(source)) return null;
		loader.execute(() -> {
			ImageData data = decode(source);
			if (!display.isDisposed()) display.asyncExec(() -> arrived(source, data));
		});
		return null;
	}

	/** Whether {@code source} could not be loaded. */
	public boolean failed(String source) {
		return failed.contains(source);
	}

	/** Number of images kept. */
	public int size() {
		return images.size();
	}

	public void dispose() {
		disposed = true;
		loader.shutdownNow();
		images.values().forEach(Image::dispose);
		images.clear();
	}

	private void arrived(String source, ImageData data) {
		pending.remove(source);
		if (disposed) return;
		if (data == null) {
			failed.add(source);
			return;
		}
		Image image = new Image(display, data);
		images.put(source, image);
		for (Iterator<Image> eldest = images.values().iterator(); images.size() > capacity;) {
			eldest.next().dispose();
			eldest.remove();
		}
		loaded.accept(source, image);
	}

	/* the first frame of the source's image, or null; runs on the loader thread (ImageLoader needs no Display) */
	static ImageData decode(String source) {
		try (InputStream in = open(source)) {
			ImageData[] frames = new ImageLoader().load(in);
			return frames.length > 0 ? frames[0] : null;
		} catch (IOException | SWTException | IllegalArgumentException e) {
			return null;
		}
	}

	private static InputStream open(String source) throws IOException {
		if (source.startsWith("data:")) {
			int comma = source.indexOf(',');
			if (comma < 0 || !source.substring(0, comma).endsWith(";base64")) throw new IOException("not a base64 data URI");
			return new ByteArrayInputStream(Base64.getMimeDecoder().decode(source.substring(comma + 1)));
		}
		URLConnection connection = URI.create(source).toURL().openConnection();
		connection.setConnectTimeout(TIMEOUT_MS);
		connection.setReadTimeout(TIMEOUT_MS);
		return connection.getInputStream();
	}
}
