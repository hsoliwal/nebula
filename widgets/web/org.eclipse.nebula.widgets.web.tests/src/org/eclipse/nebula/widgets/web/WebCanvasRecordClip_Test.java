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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.nebula.widgets.web.model.CollectionModel;
import org.eclipse.nebula.widgets.web.model.WebModel;
import org.eclipse.nebula.widgets.web.model.WebPaint;
import org.eclipse.nebula.widgets.web.model.WebStates;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Real native Image/GC and Canvas.scroll contracts; no visible-screen claim. */
public class WebCanvasRecordClip_Test {

	private static final int W = 128, H = 96, INK = 0x173b51, LATER = 0xca4427;
	private static final RGB SENTINEL = new RGB(0, 255, 255);
	private Display display;
	private boolean ownsDisplay;
	private Shell shell;
	private ClipModel model;
	private Probe canvas;

	private static final class MarkerFailure extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	private static final class ClipModel extends WebModel {
		boolean text;
		int phase;

		ClipModel() {
			super("record-clip-native-test", 0, 0, 0, 0);
		}

		@Override
		public int aspects() {
			return 0;
		}

		@Override
		protected void doLayout() {
			// Explicit fixture geometry, independent of a model family's layout.
		}

		@Override
		protected void emit(WebPaint target) {
			if (phase == 2) {
				target.rect(0, 0, W, H, INK);
				return;
			}
			target.clip(20, 24, phase == 1 ? 0 : 72, 40);
			if (text) {
				target.text(0, 0, W, H, INK, 18, "MMMMMMMMMMMMMMMMMMMM");
			}
			target.rect(0, 0, W, H, INK);
			target.clearClip();
			target.rect(4, 4, 8, 8, LATER);
		}
	}

	private final class Probe extends WebCanvas<ClipModel> {
		RuntimeException fontFailure;

		Probe(ClipModel value) {
			super(shell, SWT.NONE, value);
		}

		@Override
		public Font getFont() {
			if (fontFailure != null) {
				throw fontFailure;
			}
			return super.getFont();
		}
	}

	private final class BlitProbe extends WebCanvas<CollectionModel> {
		int calls, destinationY, sourceY, copiedHeight;

		BlitProbe(CollectionModel value) {
			super(shell, SWT.NONE, value);
		}

		@Override
		public void scroll(int destX, int destY, int x, int y, int width, int height, boolean all) {
			calls++;
			destinationY = destY;
			sourceY = y;
			copiedHeight = height;
			super.scroll(destX, destY, x, y, width, height, all);
		}
	}

	private final class Frame implements AutoCloseable {
		final Image image;
		final GC gc;

		Frame(int width, int height) {
			image = new Image(display, width, height);
			gc = new GC(image);
			gc.setBackground(display.getSystemColor(SWT.COLOR_CYAN));
			gc.fillRectangle(0, 0, width, height);
		}

		@Override
		public void close() {
			if (!gc.isDisposed()) gc.dispose();
			image.dispose();
		}
	}

	@Before
	public void createNativeReceiver() {
		display = Display.getCurrent();
		ownsDisplay = display == null;
		if (ownsDisplay) display = new Display();
		shell = new Shell(display);
		model = new ClipModel();
		canvas = new Probe(model);
		canvas.setSize(W, H);
		model.layout(W, H);
	}

	@After
	public void disposeNativeReceiver() {
		if (canvas != null) canvas.fontFailure = null;
		if (shell != null && !shell.isDisposed()) shell.dispose();
		if (ownsDisplay && display != null && !display.isDisposed()) display.dispose();
	}

	private static void paint(WebCanvas<?> target, GC gc, int width, int height) throws Exception {
		paint(target, gc, 0, 0, width, height);
	}

	private static void paint(WebCanvas<?> target, GC gc, int x, int y, int width, int height) throws Exception {
		Event event = new Event();
		event.gc = gc;
		event.x = x;
		event.y = y;
		event.width = width;
		event.height = height;
		// Directly invoke the existing native listener so failure assertions do not
		// depend on a shared Display's application exception-handler policy.
		Method listener = WebCanvas.class.getDeclaredMethod("onPaint", Event.class);
		listener.setAccessible(true);
		listener.invoke(target, event);
	}

	private Region callerClip() {
		Region region = new Region(display);
		region.add(0, 0, W, H);
		region.subtract(40, 30, 16, 20);
		region.subtract(100, 0, 8, H);
		return region;
	}

	private Region deviceClip(GC gc) {
		Transform transform = new Transform(display);
		Region region = new Region(display);
		boolean success = false;
		try {
			gc.getTransform(transform);
			try {
				gc.setTransform(null);
				gc.getClipping(region);
			} finally {
				gc.setTransform(transform);
			}
			success = true;
			return region;
		} finally {
			transform.dispose();
			if (!success) region.dispose();
		}
	}

	private void assertCallerState(GC gc, Region expectedClip, float[] expectedTransform) {
		Region actual = deviceClip(gc);
		Region difference = new Region(display);
		Transform transform = new Transform(display);
		try {
			difference.add(expectedClip);
			difference.subtract(actual);
			assertTrue("caller clip lost pixels", difference.isEmpty());
			difference.add(actual);
			difference.subtract(expectedClip);
			assertTrue("caller clip gained pixels or filled a hole", difference.isEmpty());
			gc.getTransform(transform);
			float[] actualTransform = new float[6];
			transform.getElements(actualTransform);
			assertArrayEquals("caller transform changed", expectedTransform, actualTransform, 0f);
			assertFalse(gc.isDisposed());
		} finally {
			transform.dispose();
			difference.dispose();
			actual.dispose();
		}
	}

	private static RGB rgb(int value) {
		return new RGB(value >> 16 & 255, value >> 8 & 255, value & 255);
	}

	private static RGB pixel(ImageData data, int x, int y) {
		return data.palette.getRGB(data.getPixel(x, y));
	}

	private void nativeMaskCase(int dx, int dy) throws Exception {
		Region caller = callerClip();
		Transform transform = new Transform(display);
		try (Frame frame = new Frame(W, H)) {
			transform.translate(dx, dy);
			frame.gc.setTransform(transform);
			frame.gc.setClipping(caller);
			Region expectedClip = deviceClip(frame.gc);
			try {
				float[] expectedTransform = new float[6];
				transform.getElements(expectedTransform);
				paint(canvas, frame.gc, W, H);
				assertCallerState(frame.gc, expectedClip, expectedTransform);
				ImageData data = frame.image.getImageData();
				RGB background = rgb(model.theme().slot(0));
				for (int y = 0; y < H; y++) {
					for (int x = 0; x < W; x++) {
						int localX = x - dx, localY = y - dy;
						RGB expected = SENTINEL;
						if (expectedClip.contains(x, y)) {
							expected = background;
							if (model.phase != 1 && localX >= 20 && localX < 92 && localY >= 24 && localY < 64) expected = rgb(INK);
							if (localX >= 4 && localX < 12 && localY >= 4 && localY < 12) expected = rgb(LATER);
						}
						assertEquals("native clip pixel at " + x + "," + y, expected, pixel(data, x, y));
					}
				}
			} finally {
				expectedClip.dispose();
			}
		} finally {
			transform.dispose();
			caller.dispose();
		}
	}

	@Test
	public void recordClipsIntersectCallerHolesAndPreserveTranslations() throws Exception {
		nativeMaskCase(0, 0);
		nativeMaskCase(7, 5);
	}

	@Test
	public void nestedTextClipRestoresTheRecordClipAndLaterUnclippedRecord() throws Exception {
		model.text = true;
		nativeMaskCase(0, 0);
		nativeMaskCase(7, 5);
	}

	@Test
	public void emptyClipAndFrameResetCannotLeakToLaterDrawing() throws Exception {
		model.phase = 1;
		nativeMaskCase(0, 0);
		model.phase = 2;
		try (Frame frame = new Frame(W, H)) {
			paint(canvas, frame.gc, W, H);
			ImageData data = frame.image.getImageData();
			for (int y = 0; y < H; y++) {
				for (int x = 0; x < W; x++) assertEquals(rgb(INK), pixel(data, x, y));
			}
		}
	}

	@Test
	public void textFailureRestoresExactCallerClipAndTransform() throws Exception {
		model.text = true;
		MarkerFailure failure = new MarkerFailure();
		canvas.fontFailure = failure;
		Region caller = callerClip();
		Transform transform = new Transform(display);
		try (Frame frame = new Frame(W, H)) {
			transform.translate(7, 5);
			frame.gc.setTransform(transform);
			frame.gc.setClipping(caller);
			Region expectedClip = deviceClip(frame.gc);
			try {
				float[] expectedTransform = new float[6];
				transform.getElements(expectedTransform);
				try {
					paint(canvas, frame.gc, W, H);
					fail("text replay must expose the injected failure");
				} catch (InvocationTargetException expected) {
					assertSame(failure, expected.getCause());
				}
				assertCallerState(frame.gc, expectedClip, expectedTransform);
			} finally {
				expectedClip.dispose();
			}
		} finally {
			canvas.fontFailure = null;
			transform.dispose();
			caller.dispose();
		}
	}

	private CollectionModel board() {
		return board(WebStates.ROUND);
	}

	private CollectionModel board(int shape) {
		CollectionModel board = new CollectionModel("kanban-native-header-test", 0, CollectionModel.KANBAN, shape, 0);
		for (int i = 0; i < 48; i++) {
			int item = board.add("Card " + i);
			board.setGroup(item, i % 3);
		}
		return board;
	}

	@Test
	public void partialScrollAndFocusedCardLeaveHeaderPixelsUnchanged() throws Exception {
		CollectionModel board = board();
		WebCanvas<CollectionModel> boardCanvas = new WebCanvas<>(shell, SWT.NONE, board);
		boardCanvas.setSize(256, 192);
		board.layout(256, 192);
		int header = board.viewOrigin();
		assertTrue("the native board requires a separate header strip", header > 0);
		try (Frame before = new Frame(256, 192); Frame after = new Frame(256, 192)) {
			paint(boardCanvas, before.gc, 256, 192);
			board.setFocus(0);
			board.setState(-1, WebStates.FOCUS, true);
			board.scrollTo(13);
			paint(boardCanvas, after.gc, 256, 192);
			ImageData original = before.image.getImageData(), moved = after.image.getImageData();
			int bodyChanges = 0;
			for (int y = 0; y < 192; y++) {
				for (int x = 0; x < 256; x++) {
					if (y < header) assertEquals("fixed header pixel " + x + "," + y, pixel(original, x, y), pixel(moved, x, y));
					else if (!pixel(original, x, y).equals(pixel(moved, x, y))) bodyChanges++;
				}
			}
			assertTrue("the body must actually change", bodyChanges > 100);
			assertEquals(-1, board.hitTest(20, header - 1));
		}
	}

	@Test
	public void cleanShortScrollUsesNativeBlitOnlyInsideTheBody() {
		CollectionModel board = board();
		BlitProbe boardCanvas = new BlitProbe(board);
		boardCanvas.setSize(256, 192);
		board.layout(256, 192);
		board.takeDamage(new int[4]);
		board.takeBlit();
		boardCanvas.calls = 0;
		board.scrollTo(13);
		assertEquals("one existing Canvas.scroll call", 1, boardCanvas.calls);
		assertTrue(boardCanvas.sourceY >= board.viewOrigin());
		assertTrue(boardCanvas.destinationY >= board.viewOrigin());
		assertEquals(board.viewLength() - 13, boardCanvas.copiedHeight);
	}

	@Test
	public void retainedNativePixelsMatchFreshPaintAfterScrollInBothDirections() throws Exception {
		for (int shape : new int[] {WebStates.ROUND, WebStates.PILL}) {
			for (boolean rtl : new boolean[] {false, true}) {
				CollectionModel board = board(shape);
				board.setRtl(rtl);
				WebCanvas<CollectionModel> boardCanvas = new WebCanvas<>(shell, SWT.NONE, board);
				boardCanvas.setSize(256, 192);
				// Consume the same public damage/blit protocol explicitly for the image
				// receiver. The separate Canvas.scroll test exercises the live callback.
				board.setDelta(null);
				board.layout(256, 192);
				int[] damage = new int[4];
				board.takeDamage(damage);
				board.takeBlit();
				Image retained = new Image(display, 256, 192);
				try {
					GC initial = new GC(retained);
					try {
						paint(boardCanvas, initial, 256, 192);
					} finally {
						initial.dispose();
					}
					for (int offset : new int[] {7, 20, 9, 37, 21, 0}) {
						board.scrollTo(offset);
						int delta = board.takeBlit();
						if (delta != 0) {
							int source = board.viewOrigin() + Math.max(0, -delta);
							int destination = source + delta;
							int span = board.viewLength() - Math.abs(delta);
							GC copy = new GC(retained);
							try {
								copy.setClipping(0, destination, 256, span);
								copy.copyArea(0, source, 256, span, 0, destination, false);
							} finally {
								copy.dispose();
							}
						}
						assertTrue("a moved viewport must publish repair damage", board.takeDamage(damage));
						GC repair = new GC(retained);
						try {
							repair.setClipping(damage[0], damage[1], damage[2], damage[3]);
							paint(boardCanvas, repair, damage[0], damage[1], damage[2], damage[3]);
						} finally {
							repair.dispose();
						}
						try (Frame fresh = new Frame(256, 192)) {
							paint(boardCanvas, fresh.gc, 256, 192);
							ImageData expected = fresh.image.getImageData(), actual = retained.getImageData();
							for (int y = 0; y < 192; y++) {
								for (int x = 0; x < 256; x++) {
									RGB expectedPixel = pixel(expected, x, y), actualPixel = pixel(actual, x, y);
									if (!expectedPixel.equals(actualPixel)) {
										assertEquals("retained native pixel at " + x + "," + y + ", shape=" + shape
												+ ", rtl=" + rtl + ", offset=" + offset, expectedPixel, actualPixel);
									}
								}
							}
						}
					}
				} finally {
					retained.dispose();
					boardCanvas.dispose();
				}
			}
		}
	}
}
