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
package org.eclipse.nebula.widgets.compositetable.day;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.nebula.widgets.compositetable.day.internal.DayEditorCalendarableItemControl;
import org.eclipse.nebula.widgets.compositetable.timeeditor.CalendarableItem;
import org.eclipse.nebula.widgets.compositetable.timeeditor.EventContentProvider;
import org.eclipse.nebula.widgets.compositetable.timeeditor.EventCountProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget H leaf of DayEditor: resizes while the editor is hidden move no event box (before: every box was
 * positioned on each resize); the reveal lays them out once, to the bounds of an editor laid out visibly at that size.
 */
public class DayEditorHiddenLayoutTest extends TestCase {

	private Display display;
	private Shell shell;
	private Composite holder;
	private DayEditor editor;
	private int moves;

	@Override
	protected void setUp() throws Exception {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(700, 500);
		holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		editor = editor(holder);
		shell.open();
		spin();
	}

	@Override
	protected void tearDown() throws Exception {
		shell.dispose();
	}

	public void testHiddenResizesMoveNoEventBox() {
		List<DayEditorCalendarableItemControl> boxes = boxes(editor);
		assertFalse("the editor shows event boxes", boxes.isEmpty());
		for (DayEditorCalendarableItemControl box : boxes) {
			box.addListener(SWT.Move, e -> moves++);
			box.addListener(SWT.Resize, e -> moves++);
		}
		holder.setVisible(false);
		spin();
		moves = 0;
		shell.setSize(560, 420);
		spin();
		shell.setSize(820, 560);
		spin();
		assertEquals("hidden resizes move no event box", 0, moves);

		holder.setVisible(true);
		spin();
		assertEquals("the reveal lays the boxes out like a visible editor", reference(820, 560), bounds(editor));
	}

	private List<String> reference(int width, int height) {
		Shell other = new Shell(display);
		other.setLayout(new FillLayout());
		other.setSize(width, height);
		DayEditor visible = editor(other);
		other.open();
		spin();
		List<String> result = bounds(visible);
		other.dispose();
		return result;
	}

	private static DayEditor editor(Composite parent) {
		DayEditor editor = new DayEditor(parent, SWT.NONE);
		editor.setTimeBreakdown(3, 2);
		editor.setEventCountProvider(new EventCountProvider() {
			@Override
			public int getNumberOfEventsInDay(Date day) {
				return 2;
			}
		});
		editor.setEventContentProvider(new EventContentProvider() {
			@Override
			public void refresh(Date day, CalendarableItem[] controls) {
				for (int i = 0; i < controls.length; i++) {
					GregorianCalendar start = new GregorianCalendar();
					start.setTime(day);
					start.set(Calendar.HOUR_OF_DAY, 9 + 2 * i);
					GregorianCalendar end = (GregorianCalendar) start.clone();
					end.add(Calendar.HOUR_OF_DAY, 1);
					controls[i].setStartTime(start.getTime());
					controls[i].setEndTime(end.getTime());
					controls[i].setText("event " + i);
				}
			}
		});
		GregorianCalendar today = new GregorianCalendar(2026, Calendar.NOVEMBER, 13);
		editor.setStartDate(today.getTime());
		return editor;
	}

	private static List<String> bounds(DayEditor editor) {
		List<String> result = new ArrayList<>();
		for (DayEditorCalendarableItemControl box : boxes(editor)) {
			Rectangle b = box.getBounds();
			result.add(box.isVisible() + "@" + b.x + "," + b.y + "," + b.width + "x" + b.height);
		}
		return result;
	}

	private static List<DayEditorCalendarableItemControl> boxes(Control control) {
		List<DayEditorCalendarableItemControl> result = new ArrayList<>();
		if (control instanceof DayEditorCalendarableItemControl) {
			result.add((DayEditorCalendarableItemControl) control);
		}
		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				result.addAll(boxes(child));
			}
		}
		return result;
	}

	private void spin() {
		long end = System.currentTimeMillis() + 400;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
