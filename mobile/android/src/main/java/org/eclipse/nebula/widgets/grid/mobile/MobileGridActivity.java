/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.grid.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridViewBinding;
import org.eclipse.swt.internal.mobile.MobileWidgetRuntime;
import org.eclipse.swt.internal.mobile.MobileWidgetRuntime.Bounds;
import org.eclipse.swt.internal.mobile.android.AndroidMobileHost;

/** Android smoke application binding Nebula's viewport to SWT mobile native Views. */
public final class MobileGridActivity extends Activity {
	private MobileWidgetRuntime display;
	private MobileWidgetRuntime.Node shell;
	private MobileGridViewBinding grid;
	private FrameLayout root;
	private long scrollPixels;
	private float previousY;

	@Override protected void onCreate(Bundle state) {
		super.onCreate(state);
		root = new FrameLayout(this);
		setContentView(root);
		display = new MobileWidgetRuntime(new AndroidMobileHost(this, root));
		shell = display.createShell();
		grid = new MobileGridViewBinding(shell, 1_000_000_000_000L, 3, 72, 3,
				(row, column) -> switch (column) {
					case 0 -> "#" + row;
					case 1 -> "Nebula native row " + row;
					default -> "Cell " + (row % 12);
				}, row -> setTitle("Selected row " + row));
		root.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
			if (right - left > 0 && bottom - top > 0 && grid != null) {
				shell.setBounds(new Bounds(0, 0, right - left, bottom - top));
				grid.show(scrollPixels, right - left, bottom - top);
			}
		});
	}

	/** Independent long-pixel scroll offset; the native View count stays bounded. */
	@Override public boolean dispatchTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_DOWN) previousY = event.getY();
		else if (action == MotionEvent.ACTION_MOVE && grid != null && root.getHeight() > 0) {
			float delta = previousY - event.getY();
			previousY = event.getY();
			long change = (long) delta;
			if (change > 0 && scrollPixels > Long.MAX_VALUE - change) scrollPixels = Long.MAX_VALUE;
			else scrollPixels = Math.max(0, scrollPixels + change);
			grid.show(scrollPixels, root.getWidth(), root.getHeight());
		}
		return super.dispatchTouchEvent(event);
	}

	@Override protected void onResume() {
		super.onResume();
		if (display != null) display.setForeground(true);
	}
	@Override protected void onPause() {
		if (display != null) display.setForeground(false);
		super.onPause();
	}
	@Override protected void onDestroy() {
		if (grid != null) { grid.close(); grid = null; }
		if (display != null) { display.close(); display = null; }
		super.onDestroy();
	}
}
