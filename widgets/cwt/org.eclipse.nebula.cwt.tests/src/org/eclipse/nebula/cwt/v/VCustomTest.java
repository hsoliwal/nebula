package org.eclipse.nebula.cwt.v;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.nebula.cwt.test.AbstractVTestCase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;

public class VCustomTest extends AbstractVTestCase {

	public void testCustomVirtualControl() {
		syncExec(() -> {
			VCanvas canvas = new VCanvas(getShell(), SWT.NONE);
			VPanel panel = canvas.getPanel();

			AtomicInteger paints = new AtomicInteger();
			AtomicInteger disposes = new AtomicInteger();
			VControlPainter painter = new VControlPainter() {
				@Override
				public void paintContent(VControl control, Event event) {
					paints.incrementAndGet();
					super.paintContent(control, event);
				}

				@Override
				public void dispose() {
					disposes.incrementAndGet();
					super.dispose();
				}
			};

			VCustom custom = new VCustom(panel, SWT.NONE, painter);
			assertEquals(VControl.Type.Custom, custom.getType());
			assertSame(panel, custom.getParent());
			assertEquals(1, panel.getChildren().length);
			assertSame(custom, panel.getChildren()[0]);

			custom.setPreferredSize(120, 80);
			assertEquals(120, custom.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
			assertEquals(80, custom.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			assertEquals(40, custom.computeSize(40, SWT.DEFAULT).x);
			assertEquals(50, custom.computeSize(SWT.DEFAULT, 50).y);

			custom.setPreferredSize(new org.eclipse.swt.graphics.Point(90, 70));
			org.eclipse.swt.graphics.Point preferred = custom.getPreferredSize();
			assertEquals(90, preferred.x);
			assertEquals(70, preferred.y);
			preferred.x = 1;
			assertEquals(90, custom.getPreferredSize().x);

			custom.setBounds(0, 0, 20, 20);
			Image image = new Image(getDisplay(), 20, 20);
			GC gc = new GC(image);
			try {
				Event event = new Event();
				event.display = getDisplay();
				event.gc = gc;
				event.x = 0;
				event.y = 0;
				event.width = 20;
				event.height = 20;
				custom.paintControl(event);
				assertEquals(1, paints.get());
			} finally {
				gc.dispose();
				image.dispose();
			}

			custom.clearPreferredSize();
			assertNull(custom.getPreferredSize());
			custom.dispose();
			assertEquals(1, disposes.get());
			assertEquals(0, panel.getChildren().length);
		});
	}

	public void testCustomRejectsInvalidConfiguration() {
		syncExec(() -> {
			VCanvas canvas = new VCanvas(getShell(), SWT.NONE);
			VCustom custom = new VCustom(canvas.getPanel(), SWT.NONE);

			try {
				custom.setPreferredSize(-1, 10);
				fail("negative preferred width must fail");
			} catch (IllegalArgumentException expected) {
				// expected
			}

			try {
				custom.setPainter(null);
				fail("null painter must fail");
			} catch (NullPointerException expected) {
				// expected
			}
		});
	}
}
