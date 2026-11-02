/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
import java.lang.reflect.Method;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Runs one Nebula example/snippet main for a fixed time, then captures its shell on screen (capture mode
 * display_copyArea: shell forced active and on top, {@code new GC(display).copyArea}) and closes every shell so the
 * example's own event loop ends. Prints {@code label, example, CAPTURED|EXIT_<status>, bright_pixels, shell_size}; an exception after the capture (window teardown) shows as EXIT_EXCEPTION.
 * Usage: {@code ExampleRunner <label> <millis> <mainClass>}.
 */
public class ExampleRunner {

	public static void main(String[] args) throws Exception {
		String label = args[0];
		int millis = Integer.parseInt(args[1]);
		String main = args[2];
		Display display = Display.getDefault();
		int[] bright = { -1 };
		String[] size = { "-" };
		display.timerExec(millis, () -> {
			Shell[] shells = display.getShells();
			if (shells.length > 0) {
				Shell s = shells[0];
				s.forceActive();
				s.moveAbove(null);
				while (display.readAndDispatch()) {
					// settle
				}
				Rectangle r = s.getBounds();
				size[0] = r.width + "x" + r.height;
				Image img = new Image(display, r.width, r.height);
				GC gc = new GC(display);
				gc.copyArea(img, r.x, r.y);
				gc.dispose();
				byte[] d = img.getImageData().data;
				img.dispose();
				int lit = 0;
				for (int i = 0; i + 2 < d.length; i += 3) {
					lit += (d[i] & 0xff) + (d[i + 1] & 0xff) + (d[i + 2] & 0xff) > 384 ? 1 : 0;
				}
				bright[0] = lit;
			}
			// printed here: several snippets call System.exit(0) once their window closes
			System.out.println(label + "\t" + main + "\tCAPTURED\t" + bright[0] + "\t" + size[0]);
			System.out.flush();
			for (Shell s : display.getShells()) {
				s.dispose();
			}
		});
		String status = "OK";
		try {
			Method m = Class.forName(main).getMethod("main", String[].class);
			m.invoke(null, (Object) new String[0]);
		} catch (java.lang.reflect.InvocationTargetException e) {
			Throwable c = e.getCause();
			status = "EXCEPTION:" + c.getClass().getSimpleName() + ":" + String.valueOf(c.getMessage()).replace('\t', ' ');
		}
		System.out.println(label + "\t" + main + "\tEXIT_" + status + "\t" + bright[0] + "\t" + size[0]);
		if (!display.isDisposed()) {
			display.dispose();
		}
		System.exit(0);
	}
}
