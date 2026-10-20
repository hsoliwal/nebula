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
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.nebula.widgets.m3.M3HeatMap;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3HeatMap: API, errors, range and palette mapping, geometry, selection events, delta repaints, pixels against a
 * reference painted cell by cell. Runs in both startup modes.
 */
public class M3HeatMapTest {

	static final boolean VIEWPORT = WidgetMode.of("M3HeatMap", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	Display display;
	Shell shell;
	M3HeatMap map;
	final List<Rectangle> paints = new ArrayList<>();
	final List<Point> selections = new ArrayList<>();

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 300, 200);
		map = new M3HeatMap(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		map.setBounds(0, 0, 300, 200);
		map.addListener(SWT.Paint, e -> paints.add(new Rectangle(e.x, e.y, e.width, e.height)));
		map.addListener(SWT.Selection, e -> selections.add(new Point(e.x, e.y)));
		shell.open();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	void flush() {
		while (display.readAndDispatch()) {
			// dispatch everything queued
		}
	}

	@Test
	public void sizeKeepsValuesInsideTheNewGrid() {
		map.setGridSize(3, 4);
		assertEquals(3, map.getRowCount());
		assertEquals(4, map.getColumnCount());
		map.setValue(1, 2, 5f);
		map.setValue(2, 3, 7f);
		map.setGridSize(2, 3);
		assertEquals(5f, map.getValue(1, 2), 0f);
		assertEquals(0f, map.getValue(0, 0), 0f);
		map.setGridSize(4, 5);
		assertEquals(5f, map.getValue(1, 2), 0f);
		assertEquals(0f, map.getValue(3, 4), 0f);
		map.setGridSize(0, 0);
		assertEquals(0, map.getRowCount());
	}

	@Test
	public void autoRangeFollowsTheValuesAndSetRangeTurnsItOff() {
		map.setGridSize(2, 2);
		assertTrue(map.isAutoRange());
		assertEquals(0f, map.getMinimum(), 0f);
		assertEquals(1f, map.getMaximum(), 0f);
		map.setValues(new float[] { 2f, 4f, 6f, 8f });
		assertEquals(2f, map.getMinimum(), 0f);
		assertEquals(8f, map.getMaximum(), 0f);
		map.setValue(0, 0, -1f);
		assertEquals(-1f, map.getMinimum(), 0f);
		map.setValues(new float[] { 3f, 3f, 3f, 3f });
		assertEquals(3f, map.getMinimum(), 0f);
		assertEquals(4f, map.getMaximum(), 0f);
		map.setRange(10f, 20f);
		assertFalse(map.isAutoRange());
		map.setValue(1, 1, 100f);
		assertEquals(20f, map.getMaximum(), 0f);
		assertEquals(255, map.indexOf(100f));
		assertEquals(0, map.indexOf(-5f));
		assertEquals(128, map.indexOf(15f));
		assertEquals(0, map.indexOf(Float.NaN));
		map.setAutoRange(true);
		assertEquals(3f, map.getMinimum(), 0f);
		assertEquals(100f, map.getMaximum(), 0f);
	}

	@Test
	public void paletteStopsInterpolateIntoTheLookupTable() {
		map.setPalette(new RGB[] { new RGB(0, 0, 0), new RGB(255, 255, 255) });
		int[] lut = map.getLookupTable();
		assertEquals(256, lut.length);
		assertEquals(0x000000, lut[0]);
		assertEquals(0xffffff, lut[255]);
		assertEquals(0x808080, lut[128]);
		map.setPalette(new RGB[] { new RGB(255, 0, 0), new RGB(0, 255, 0), new RGB(0, 0, 255) });
		lut = map.getLookupTable();
		assertEquals(0xff0000, lut[0]);
		assertEquals(0x0000ff, lut[255]);
		// the middle stop sits at t = 0.5, between indices 127 and 128 (t = i / 255): both are within one step of pure green
		assertTrue(((lut[127] >> 8) & 0xff) >= 0xfe && ((lut[128] >> 8) & 0xff) >= 0xfe);
		map.setRange(0f, 1f);
		assertEquals(lut[255], map.colorOf(2f));
		assertEquals(lut[0], map.colorOf(0f));
	}

	@Test
	public void geometryAndHitTest() {
		map.setGridSize(10, 20);
		map.setCellSize(10, 5);
		assertEquals(new Point(10, 5), map.getCellSize());
		assertEquals(new Rectangle(30, 10, 10, 5), map.getCellBounds(2, 3));
		assertEquals(new Point(3, 2), map.getCell(new Point(35, 12)));
		assertNull(map.getCell(new Point(250, 10)));
		assertNull(map.getCell(new Point(-1, 10)));
		assertNull(map.getCell(new Point(10, 60)));
		Point size = map.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		assertTrue(size.x >= 200 && size.y >= 50);
		assertEquals(map.computeSize(70, 40), map.computeSize(70, 40, true));
	}

	@Test
	public void scrollingMovesTheOrigin() {
		map.setGridSize(100, 100);
		map.setCellSize(10, 10);
		map.setOrigin(50, 70);
		assertEquals(new Point(50, 70), map.getOrigin());
		assertEquals(new Rectangle(-50, -70, 10, 10), map.getCellBounds(0, 0));
		map.setOrigin(-5, 5000);
		Point origin = map.getOrigin();
		assertEquals(0, origin.x);
		assertEquals(1000 - map.getClientArea().height, origin.y);
		map.showCell(0, 0);
		assertEquals(new Point(0, 0), map.getOrigin());
		map.showCell(99, 99);
		Rectangle client = map.getClientArea();
		assertEquals(new Point(1000 - client.width, 1000 - client.height), map.getOrigin());
		map.showCell(50, 50);
		assertTrue(map.getCellBounds(50, 50).intersects(client));
		map.getVerticalBar().setSelection(300);
		map.getVerticalBar().notifyListeners(SWT.Selection, new Event());
		assertEquals(300, map.getOrigin().y);
		map.getHorizontalBar().setSelection(200);
		map.getHorizontalBar().notifyListeners(SWT.Selection, new Event());
		assertEquals(200, map.getOrigin().x);
	}

	@Test
	public void selectionEventsAndKeys() {
		map.setGridSize(5, 5);
		map.setCellSize(20, 20);
		assertEquals(new Point(-1, -1), map.getSelection());
		map.setSelection(2, 3);
		map.setSelection(2, 3);
		assertEquals(new Point(3, 2), map.getSelection());
		assertEquals(List.of(new Point(3, 2)), selections);
		click(45, 25);
		assertEquals(new Point(2, 1), map.getSelection());
		key(SWT.ARROW_RIGHT);
		key(SWT.ARROW_DOWN);
		key(SWT.ARROW_LEFT);
		key(SWT.ARROW_UP);
		key(SWT.HOME);
		assertEquals(new Point(2, 1), map.getSelection());
		assertEquals("setSelection, click and four arrow moves", 6, selections.size());
		map.setSelection(0, 0);
		key(SWT.ARROW_UP);
		key(SWT.ARROW_LEFT);
		assertEquals(new Point(0, 0), map.getSelection());
		map.setSelection(-1, -1);
		assertEquals(new Point(-1, -1), map.getSelection());
		key(SWT.ARROW_DOWN);
		assertEquals(new Point(-1, -1), map.getSelection());
		click(400, 400);
		assertEquals(new Point(-1, -1), map.getSelection());
		Event right = new Event();
		right.button = 3;
		right.x = 5;
		right.y = 5;
		map.notifyListeners(SWT.MouseDown, right);
		assertEquals(new Point(-1, -1), map.getSelection());
		map.setSelection(4, 4);
		map.setGridSize(3, 3);
		assertEquals(new Point(-1, -1), map.getSelection());
	}

	void click(int x, int y) {
		Event event = new Event();
		event.button = 1;
		event.x = x;
		event.y = y;
		map.notifyListeners(SWT.MouseDown, event);
	}

	void key(int keyCode) {
		Event event = new Event();
		event.keyCode = keyCode;
		map.notifyListeners(SWT.KeyDown, event);
	}

	@Test
	public void deltaRepaintsOnlyTheRowsOnScreen() {
		map.setGridSize(1000, 10);
		map.setCellSize(10, 10);
		map.setRange(0f, 1f);
		flush();
		paints.clear();
		map.update(500, 600);
		flush();
		assertTrue(paints.isEmpty());
		map.update(2, 3);
		flush();
		assertEquals(1, paints.size());
		assertEquals(20, paints.get(0).y);
		assertEquals(20, paints.get(0).height);
		paints.clear();
		map.setValue(700, 5, 0.5f);
		flush();
		assertTrue(paints.isEmpty());
		map.setValue(1, 1, 0.5f);
		flush();
		assertEquals(1, paints.size());
		assertEquals(new Rectangle(10, 10, 10, 10), paints.get(0));
		paints.clear();
		map.update(-5, 2000);
		map.update(3, 2);
		flush();
		assertEquals(1, paints.size());
		map.setGridSize(0, 0);
		map.update(0, 0);
		map.setGridSize(2, 2);
		map.setAutoRange(true);
		map.update(0, 1);
	}

	@Test
	public void pixelsMatchACellByCellReference() {
		map.setGridSize(40, 50);
		map.setCellSize(9, 7);
		Random random = new Random(7);
		float[] values = new float[40 * 50];
		for (int i = 0; i < values.length; i++) {
			values[i] = random.nextInt(5) * 0.25f;
		}
		map.setValues(values);
		map.setRange(0f, 1f);
		map.setOrigin(13, 9);
		assertEquals("a grid larger than the client scrolls", new Point(13, 9), map.getOrigin());
		flush();
		map.update();
		Rectangle client = map.getClientArea();
		Image actual = new Image(display, client.width, client.height);
		GC gc = new GC(map);
		gc.copyArea(actual, 0, 0);
		gc.dispose();
		Image expected = new Image(display, client.width, client.height);
		gc = new GC(expected);
		gc.setBackground(map.getBackground());
		gc.fillRectangle(0, 0, client.width, client.height);
		int[] lut = map.getLookupTable();
		for (int row = 0; row < 40; row++) {
			for (int column = 0; column < 50; column++) {
				int rgb = lut[map.indexOf(values[row * 50 + column])];
				org.eclipse.swt.graphics.Color color = new org.eclipse.swt.graphics.Color(display, (rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
				gc.setBackground(color);
				gc.fillRectangle(column * 9 - 13, row * 7 - 9, 9, 7);
				color.dispose();
			}
		}
		gc.dispose();
		ImageData a = actual.getImageData(), e = expected.getImageData();
		actual.dispose();
		expected.dispose();
		M3TestSupport.assertSamePixels("heatmap-cells", e, a);
	}

	@Test
	public void hiddenMapFoldsDeltasAndPaintsOnceOnShow() {
		if (!VIEWPORT) {
			return;
		}
		map.setGridSize(12, 16);
		map.setCellSize(9, 7);
		map.setRange(0f, 1f);
		float[] values = new float[12 * 16];
		Random random = new Random(11);
		M3TestSupport.assertHiddenGate(map, paints, i -> {
			int cell = random.nextInt(values.length);
			values[cell] = random.nextInt(5) * 0.25f;
			map.setValue(cell / 16, cell % 16, values[cell]);
		});
		M3HeatMap fresh = new M3HeatMap(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		fresh.setBounds(0, 0, 300, 200);
		fresh.setGridSize(12, 16);
		fresh.setCellSize(9, 7);
		fresh.setValues(values);
		fresh.setRange(0f, 1f);
		fresh.moveAbove(map);
		M3TestSupport.flush(display);
		org.eclipse.swt.graphics.ImageData expected = M3TestSupport.pixels(fresh);
		fresh.dispose();
		M3TestSupport.flush(display);
		assertArrayEquals(expected.data, M3TestSupport.pixels(map).data);
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		map.setGridSize(400, 400);
		map.setCellSize(4, 4);
		map.setOrigin(100, 100);
		flush();
		int height = map.getSize().y, old = map.getSize().x;
		org.eclipse.nebula.widgets.m3.internal.M3Counters.reset();
		for (int width : M3TestSupport.sweepWidths(300)) {
			paints.clear();
			map.setSize(width, height);
			map.update();
			if (VIEWPORT) {
				for (Rectangle paint : paints) {
					assertTrue(old + " -> " + width + ": " + paint, paint.x >= Math.min(old, width) - 20);
				}
			}
			old = width;
		}
		assertEquals(org.eclipse.nebula.widgets.m3.internal.M3Counters.line(), 0, org.eclipse.nebula.widgets.m3.internal.M3Counters.fullRepaints);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> map.setGridSize(-1, 2));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> map.setGridSize(2, -1));
		map.setGridSize(2, 2);
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> map.setValues(null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> map.setValues(new float[3]));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> map.setValue(2, 0, 1f));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> map.getValue(0, 2));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> map.getValue(-1, 0));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> map.getCellBounds(0, -1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> map.setSelection(2, 2));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> map.setSelection(-1, 0));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> map.showCell(5, 5));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> map.setRange(2f, 1f));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> map.setRange(Float.NaN, 1f));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> map.setPalette(null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> map.setPalette(new RGB[] { new RGB(0, 0, 0) }));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> map.setPalette(new RGB[] { new RGB(0, 0, 0), null }));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> map.setCellSize(0, 1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> map.setCellSize(1, 0));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> map.getCell(null));
	}
}
