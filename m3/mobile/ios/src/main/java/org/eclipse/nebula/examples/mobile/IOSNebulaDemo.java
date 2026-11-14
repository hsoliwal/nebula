/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.examples.mobile;

import org.eclipse.swt.internal.mobile.IOSPlatform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Entry point for the native iOS embedding application after SWTIOSAttach has
 * registered the UIKit JNI bridge in its supplied JavaVM. The unchanged Nebula
 * widgets are instantiated by the same shared demo used on Android.
 */
public final class IOSNebulaDemo {
	private IOSNebulaDemo() { }
	public static Thread start() {
		return IOSPlatform.start(() -> {
			Display display = new Display();
			try {
				Shell shell = new Shell(display);
				NebulaMobileContents.create(shell);
				shell.open();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch()) display.sleep();
				}
			} finally { if (!display.isDisposed()) display.dispose(); }
		});
	}
}
