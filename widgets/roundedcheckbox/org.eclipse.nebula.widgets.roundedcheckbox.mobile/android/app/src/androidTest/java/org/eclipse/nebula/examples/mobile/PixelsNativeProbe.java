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
package org.eclipse.nebula.examples.mobile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;

/**
 * Runs on Android: the native pixel move of the SWT port (libswtpixels.so, AndroidPixels.moveNative) against
 * the port's Java lane and against a plain model, for random rectangles and distances on opaque bitmaps.
 *
 *   adb shell am instrument -w org.eclipse.nebula.examples.mobile.test/org.eclipse.nebula.examples.mobile.PixelsNativeProbe
 *
 * The result bundle names whether the library was loaded, the number of cases, the mismatches of the native
 * move and of the Java lane, and the time of both for one large move. The result code is RESULT_OK only when the
 * library is loaded, every native call answered true, and nothing mismatched.
 */
public final class PixelsNativeProbe extends Instrumentation {

	@Override
	public void onCreate (Bundle arguments) {
		super.onCreate (arguments);
		start ();
	}

	@Override
	public void onStart () {
		Bundle result = new Bundle ();
		int code = Activity.RESULT_CANCELED;
		try {
			Class<?> pixels = Class.forName ("org.eclipse.swt.internal.android.AndroidPixels", true, getTargetContext ().getClassLoader ());
			Class<?> [] signature = {Bitmap.class, int.class, int.class, int.class, int.class, int.class, int.class};
			Method isNative = pixels.getDeclaredMethod ("isNative");
			Method moveNative = pixels.getDeclaredMethod ("moveNative", signature);
			Method moveByLane = pixels.getDeclaredMethod ("moveByLane", signature);
			isNative.setAccessible (true);
			moveNative.setAccessible (true);
			moveByLane.setAccessible (true);
			boolean loaded = (Boolean) isNative.invoke (null);
			result.putString ("device", Build.MODEL + " API " + Build.VERSION.SDK_INT + " " + Arrays.toString (Build.SUPPORTED_ABIS));
			result.putString ("native_library_loaded", String.valueOf (loaded));

			Random random = new Random (7);
			int cases = 0, refused = 0, nativeMismatches = 0, laneMismatches = 0;
			for (int i = 0; loaded && i < 400; i++) {
				int w = 1 + random.nextInt (96), h = 1 + random.nextInt (96);
				int [] before = new int [w * h];
				for (int p = 0; p < before.length; p++) before [p] = 0xff000000 | random.nextInt (0x1000000);
				Bitmap a = Bitmap.createBitmap (w, h, Bitmap.Config.ARGB_8888);
				a.setPixels (before, 0, w, 0, 0, w, h);
				Bitmap b = a.copy (Bitmap.Config.ARGB_8888, true);
				int rw = 1 + random.nextInt (w), rh = 1 + random.nextInt (h);
				int x = random.nextInt (w - rw + 1), y = random.nextInt (h - rh + 1);
				int dx = random.nextInt (w - rw + 1) - x, dy = random.nextInt (h - rh + 1) - y;
				/* the model: the rectangle read from the picture before, written at its new place */
				int [] expected = before.clone ();
				for (int row = 0; row < rh; row++) System.arraycopy (before, (y + row) * w + x, expected, (y + dy + row) * w + x + dx, rw);
				if (!(Boolean) moveNative.invoke (null, a, x, y, rw, rh, dx, dy)) refused++;
				moveByLane.invoke (null, b, x, y, rw, rh, dx, dy);
				int [] afterNative = new int [w * h], afterLane = new int [w * h];
				a.getPixels (afterNative, 0, w, 0, 0, w, h);
				b.getPixels (afterLane, 0, w, 0, 0, w, h);
				cases++;
				if (!Arrays.equals (afterNative, expected)) nativeMismatches++;
				if (!Arrays.equals (afterLane, expected)) laneMismatches++;
				a.recycle ();
				b.recycle ();
			}
			result.putString ("cases", String.valueOf (cases));
			result.putString ("native_refused", String.valueOf (refused));
			result.putString ("native_mismatches", String.valueOf (nativeMismatches));
			result.putString ("java_lane_mismatches", String.valueOf (laneMismatches));

			/* a rectangle the native code must refuse: its target leaves the bitmap */
			Bitmap small = Bitmap.createBitmap (8, 8, Bitmap.Config.ARGB_8888);
			boolean outside = loaded && (Boolean) moveNative.invoke (null, small, 0, 0, 8, 8, 1, 0);
			result.putString ("native_refuses_a_target_outside", String.valueOf (loaded && !outside));

			/* one large move, both ways, for the record */
			if (loaded) {
				int size = 2048;
				Bitmap big = Bitmap.createBitmap (size, size, Bitmap.Config.ARGB_8888);
				big.eraseColor (0xff336699);
				long t0 = System.nanoTime ();
				for (int i = 0; i < 20; i++) moveNative.invoke (null, big, 0, 64, size, size - 64, 0, -64);
				long t1 = System.nanoTime ();
				for (int i = 0; i < 20; i++) moveByLane.invoke (null, big, 0, 64, size, size - 64, 0, -64);
				long t2 = System.nanoTime ();
				result.putString ("scroll_2048x1984_by_64_native_ms", String.valueOf ((t1 - t0) / 20_000_000.0));
				result.putString ("scroll_2048x1984_by_64_java_lane_ms", String.valueOf ((t2 - t1) / 20_000_000.0));
			}
			if (loaded && cases == 400 && refused == 0 && nativeMismatches == 0 && laneMismatches == 0 && !outside) code = Activity.RESULT_OK;
		} catch (Throwable failure) {
			Throwable cause = failure.getCause () == null ? failure : failure.getCause ();
			result.putString ("failure", cause.toString ());
		}
		finish (code, result);
	}
}
