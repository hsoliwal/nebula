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
package org.eclipse.nebula.widgets.geomap.internal.geomapbrowser;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.eclipse.nebula.widgets.geomap.GeoMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget H leaf of the GeoMapBrowser info page: while another page is on top of the browser's page stack, map
 * events do not rewrite the info table; the page shows current values once it is on top again.
 */
public class InfoPageHiddenTest {

	private static final int INFO_PAGE = 2;
	private Display display;
	private Shell shell;
	private GeoMapBrowser browser;

	@Before
	public void setUp() {
		display = Display.getCurrent() != null ? Display.getCurrent() : new Display();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		browser = new GeoMapBrowser(shell, SWT.NONE);
		shell.setSize(800, 500);
		shell.open();
		spin(200);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void aHiddenInfoPageTakesNoMapUpdatesAndShowsCurrentValuesWhenShown() throws Exception {
		final PageContainer pages = browser.getPageContainer();
		pages.showPage(INFO_PAGE);
		spin(100);
		final Table table = infoTable();
		final String zoomBefore = table.getItem(0).getText(1);
		pages.showPage(0);
		spin(100);

		final GeoMap map = browser.getGeoMap();
		map.setZoom(map.getZoom() + 2);
		spin(100);
		assertEquals("a hidden info page keeps its zoom text", zoomBefore, table.getItem(0).getText(1));

		pages.showPage(INFO_PAGE);
		spin(100);
		assertEquals("the shown info page has the current zoom", Integer.toString(map.getZoom()),
				table.getItem(0).getText(1));
	}

	@Test
	public void aShownInfoPageFollowsTheMap() throws Exception {
		browser.getPageContainer().showPage(INFO_PAGE);
		spin(100);
		final Table table = infoTable();
		final GeoMap map = browser.getGeoMap();
		map.setZoom(map.getZoom() + 1);
		assertEquals("a shown info page follows the zoom at once", Integer.toString(map.getZoom()),
				table.getItem(0).getText(1));
	}

	private Table infoTable() throws ReflectiveOperationException {
		final Field field = InfoPage.class.getDeclaredField("table");
		field.setAccessible(true);
		return (Table) field.get(browser.getInfoPage());
	}

	private void spin(int milliseconds) {
		final long end = System.currentTimeMillis() + milliseconds;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (final InterruptedException e) {
					return;
				}
			}
		}
	}
}
