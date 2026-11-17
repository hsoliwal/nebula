/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Test;

/** The drawn browser's bounded, off-UI-thread image store. */
public class ImageStoreTest {

	private final Display display = Display.getDefault();
	private final List<String> arrived = new ArrayList<>();
	private ImageStore store;

	@After
	public void tearDown() {
		if (store != null) store.dispose();
	}

	/** A base64 PNG data URI of a solid {@code width} x {@code height} image of colour {@code rgb}. */
	public static String dataUri(int width, int height, int rgb) {
		ImageData data = new ImageData(width, height, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) data.setPixel(x, y, rgb);
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { data };
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		loader.save(out, SWT.IMAGE_PNG);
		return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
	}

	private void await(BooleanSupplier done) {
		long end = System.currentTimeMillis() + 10_000;
		while (!done.getAsBoolean() && System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) Thread.onSpinWait();
		}
		assertTrue("timed out", done.getAsBoolean());
	}

	@Test
	public void loadsOffTheUiThreadAndKeepsTheImage() {
		store = new ImageStore(display, 4, (source, image) -> arrived.add(source));
		String red = dataUri(20, 10, 0xFF0000);
		assertNull("asking starts the load", store.get(red));
		await(() -> arrived.size() == 1);
		Image image = store.get(red);
		assertNotNull(image);
		assertEquals(20, image.getBounds().width);
		assertEquals(10, image.getBounds().height);
		assertEquals(1, arrived.size());
	}

	@Test
	public void theLeastRecentlyUsedImageIsDisposedWhenFull() {
		store = new ImageStore(display, 2, (source, image) -> arrived.add(source));
		String a = dataUri(2, 2, 0xFF), b = dataUri(3, 3, 0xFF00), c = dataUri(4, 4, 0xFF0000);
		store.get(a);
		await(() -> arrived.size() == 1);
		Image first = store.get(a);
		store.get(b);
		await(() -> arrived.size() == 2);
		store.get(c);
		await(() -> arrived.size() == 3);
		assertEquals(2, store.size());
		assertTrue(first.isDisposed());
	}

	@Test
	public void aSourceThatFailsIsNotAskedForAgain() {
		store = new ImageStore(display, 4, (source, image) -> arrived.add(source));
		String missing = "file:///no/such/drawn-browser-image.png", relative = "logo.png", junk = "data:image/png;base64,AAAA";
		store.get(missing);
		store.get(relative);
		store.get(junk);
		await(() -> store.failed(missing) && store.failed(relative) && store.failed(junk));
		assertNull(store.get(missing));
		assertEquals(0, arrived.size());
	}

	@Test
	public void disposeReleasesEveryImage() {
		store = new ImageStore(display, 4, (source, image) -> arrived.add(source));
		String red = dataUri(5, 5, 0xFF0000);
		store.get(red);
		await(() -> arrived.size() == 1);
		Image image = store.get(red);
		store.dispose();
		assertTrue(image.isDisposed());
		assertEquals(0, store.size());
		store = null;
	}
}
