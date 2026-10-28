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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.m3.M3PageProvider;
import org.eclipse.nebula.widgets.m3.M3StackLayout;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3StackLayout API: drop-in children, pages, residency budget, errors. Runs in both startup modes
 * (<code>-Dswt.widget.mode.M3StackLayout=original</code> for the stock algorithm).
 */
public class M3StackLayoutTest {

	static final boolean VIEWPORT = WidgetMode.of("M3StackLayout", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT;

	Display display;
	Shell shell;
	Composite composite;
	M3StackLayout layout;
	final List<String> calls = new ArrayList<>();

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 300, 200);
		composite = new Composite(shell, SWT.NONE);
		composite.setBounds(0, 0, 300, 200);
		layout = new M3StackLayout();
		composite.setLayout(layout);
		shell.open();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	Label child(String text) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(text);
		return label;
	}

	/* a provider that creates labels and records every create and release */
	M3PageProvider provider() {
		return new M3PageProvider() {
			public Control createPage(Composite parent, int index) {
				calls.add("create " + index);
				Label label = new Label(parent, SWT.NONE);
				label.setText("page " + index);
				return label;
			}

			public void releasePage(int index, Control page) {
				calls.add("release " + index);
				page.dispose();
			}
		};
	}

	@Test
	public void topControlGetsTheAreaAndOthersAreParked() {
		Label a = child("a"), b = child("b"), c = child("c");
		layout.marginWidth = 3;
		layout.marginHeight = 5;
		layout.topControl = b;
		composite.layout();
		assertTrue(b.getVisible());
		assertFalse(a.getVisible());
		assertFalse(c.getVisible());
		Rectangle area = composite.getClientArea();
		assertEquals(new Rectangle(area.x + 3, area.y + 5, area.width - 6, area.height - 10), b.getBounds());
		layout.topControl = c;
		composite.layout();
		assertFalse(b.getVisible());
		assertTrue(c.getVisible());
		assertEquals(b.getBounds(), c.getBounds());
	}

	@Test
	public void hiddenChildBoundsFollowTheMode() {
		Label a = child("a"), b = child("b");
		layout.topControl = a;
		composite.layout();
		Rectangle expected = a.getBounds();
		if (VIEWPORT) {
			assertNotSame(expected, b.getBounds());
			assertEquals(new Rectangle(0, 0, 0, 0), b.getBounds());
		} else {
			assertEquals(expected, b.getBounds());
		}
	}

	/* documented difference: a parked child that the client shows keeps its deferred bounds until the next layout */
	@Test
	public void parkedChildShownByTheClientUntilTheNextLayout() {
		Label a = child("a"), b = child("b");
		layout.topControl = a;
		composite.layout();
		b.setVisible(true);
		assertEquals(VIEWPORT ? new Rectangle(0, 0, 0, 0) : a.getBounds(), b.getBounds());
		composite.layout();
		assertFalse(b.getVisible());
	}

	@Test
	public void nullAndForeignTopControlHideEveryChild() {
		Label a = child("a"), b = child("b");
		layout.topControl = null;
		composite.layout();
		assertFalse(a.getVisible());
		assertFalse(b.getVisible());
		layout.topControl = new Label(shell, SWT.NONE);
		a.setVisible(true);
		composite.layout();
		assertFalse(a.getVisible());
		assertFalse(b.getVisible());
	}

	@Test
	public void preferredSizeIsTheStackLayoutSize() {
		child("short");
		child("a much longer label text");
		StackLayout stock = new StackLayout();
		Composite other = new Composite(shell, SWT.NONE);
		other.setLayout(stock);
		new Label(other, SWT.NONE).setText("short");
		new Label(other, SWT.NONE).setText("a much longer label text");
		assertEquals(other.computeSize(SWT.DEFAULT, SWT.DEFAULT), composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		assertEquals(new Point(50, 60), composite.computeSize(50, 60, true));
	}

	@Test
	public void pagesAreCreatedOnFirstShow() {
		layout.setPages(composite, 1_000_000, provider());
		assertEquals(1_000_000, layout.getPageCount());
		assertEquals(0, layout.getResidentCount());
		assertEquals(-1, layout.getTopPage());
		layout.showPage(999_999);
		Control page = layout.getPage(999_999);
		assertSame(page, layout.topControl);
		assertTrue(page.getVisible());
		assertEquals(999_999, layout.getTopPage());
		layout.showPage(999_999);
		assertEquals(List.of("create 999999"), calls);
		assertNull(layout.getPage(0));
		assertEquals(1, composite.getChildren().length);
	}

	@Test
	public void budgetReleasesTheLeastRecentlyShownPage() {
		layout.setPages(composite, 100, provider());
		layout.setResidencyBudget(3);
		assertEquals(3, layout.getResidencyBudget());
		for (int index : new int[] { 1, 2, 3, 1, 4, 5 }) {
			layout.showPage(index);
		}
		if (VIEWPORT) {
			assertEquals(List.of("create 1", "create 2", "create 3", "create 4", "release 2", "create 5", "release 3"), calls);
			assertArrayEquals(new int[] { 1, 4, 5 }, layout.getResidentPages());
		} else {
			assertEquals(List.of("create 1", "create 2", "create 3", "create 4", "create 5"), calls);
			assertEquals(5, layout.getResidentCount());
		}
		calls.clear();
		layout.setResidencyBudget(1);
		assertEquals(5, layout.getTopPage());
		if (VIEWPORT) {
			assertEquals(List.of("release 1", "release 4"), calls);
			assertArrayEquals(new int[] { 5 }, layout.getResidentPages());
		}
	}

	@Test
	public void releasePageNeverReleasesTheTopPage() {
		layout.setPages(composite, 10, provider());
		layout.showPage(2);
		layout.showPage(3);
		assertFalse(layout.releasePage(3));
		assertFalse(layout.releasePage(7));
		assertTrue(layout.releasePage(2));
		assertEquals(List.of("create 2", "create 3", "release 2"), calls);
		assertFalse(layout.isResident(2));
		assertTrue(layout.isResident(3));
	}

	@Test
	public void shrinkingThePageCountReleasesPagesAtOrBeyondIt() {
		layout.setPages(composite, 10, provider());
		layout.showPage(1);
		layout.showPage(8);
		layout.setPageCount(5);
		assertEquals(5, layout.getPageCount());
		assertEquals(List.of("create 1", "create 8", "release 8"), calls);
		assertNull(layout.topControl);
		assertEquals(-1, layout.getTopPage());
		assertFalse(layout.getPage(1).getVisible());
		layout.setPageCount(9);
		assertEquals(9, layout.getPageCount());
	}

	@Test
	public void aPageKeptByTheProviderIsHiddenWhenReleased() {
		List<Control> kept = new ArrayList<>();
		layout.setPages(composite, 4, new M3PageProvider() {
			public Control createPage(Composite parent, int index) {
				return new Label(parent, SWT.NONE);
			}

			public void releasePage(int index, Control page) {
				kept.add(page);
			}
		});
		layout.showPage(3);
		layout.setPageCount(2);
		assertEquals(1, kept.size());
		assertFalse(kept.get(0).getVisible());
		assertFalse(kept.get(0).isDisposed());
	}

	@Test
	public void defaultReleaseDisposesThePage() {
		layout.setPages(composite, 4, (parent, index) -> new Label(parent, SWT.NONE));
		layout.setResidencyBudget(1);
		layout.showPage(0);
		Control first = layout.getPage(0);
		layout.showPage(1);
		assertEquals(VIEWPORT, first.isDisposed());
	}

	@Test
	public void aPageDisposedByTheClientIsCreatedAgain() {
		layout.setPages(composite, 4, provider());
		layout.showPage(0);
		layout.showPage(1);
		layout.getPage(0).dispose();
		assertEquals(1, layout.getResidentCount());
		layout.showPage(0);
		assertEquals(List.of("create 0", "create 1", "create 0"), calls);
		layout.topControl.dispose();
		assertEquals(-1, layout.getTopPage());
	}

	@Test
	public void topPageIsUnknownWhenTheClientSetsAnotherTopControl() {
		layout.setPages(composite, 4, provider());
		layout.showPage(2);
		layout.topControl = child("client");
		assertEquals(-1, layout.getTopPage());
		layout.topControl = null;
		assertEquals(-1, layout.getTopPage());
	}

	@Test
	public void setPagesReleasesThePreviousPages() {
		layout.setPages(composite, 4, provider());
		layout.showPage(2);
		M3PageProvider next = provider();
		layout.setPages(composite, 2, next);
		assertSame(next, layout.getPageProvider());
		assertEquals(List.of("create 2", "release 2"), calls);
		assertEquals(0, layout.getResidentCount());
		assertEquals(-1, layout.getTopPage());
	}

	@Test
	public void errors() {
		expect(SWT.ERROR_NULL_ARGUMENT, () -> layout.setPages(null, 1, provider()));
		expect(SWT.ERROR_NULL_ARGUMENT, () -> layout.setPages(composite, 1, null));
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.setPages(composite, -1, provider()));
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.showPage(0));
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.setPageCount(1));
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.getPage(0));
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.setResidencyBudget(0));
		assertNull(layout.getPageProvider());
		assertEquals(0, layout.getPageCount());
		layout.setPages(composite, 3, provider());
		expect(SWT.ERROR_INVALID_RANGE, () -> layout.showPage(3));
		expect(SWT.ERROR_INVALID_RANGE, () -> layout.showPage(-1));
		expect(SWT.ERROR_INVALID_RANGE, () -> layout.releasePage(3));
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.setPageCount(-1));
		Composite disposed = new Composite(shell, SWT.NONE);
		disposed.dispose();
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.setPages(disposed, 1, provider()));
	}

	@Test
	public void providerMustReturnALiveChildOfTheParent() {
		layout.setPages(composite, 3, (parent, index) -> null);
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.showPage(0));
		layout.setPages(composite, 3, (parent, index) -> new Label(shell, SWT.NONE));
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.showPage(0));
		layout.setPages(composite, 3, (parent, index) -> {
			Label label = new Label(parent, SWT.NONE);
			label.dispose();
			return label;
		});
		expect(SWT.ERROR_INVALID_ARGUMENT, () -> layout.showPage(0));
	}

	@Test
	public void shrinkingAfterTheParentIsDisposedDoesNotLayOut() {
		layout.setPages(composite, 4, provider());
		layout.showPage(3);
		composite.dispose();
		layout.setPageCount(1);
		assertEquals(1, layout.getPageCount());
	}

	@Test
	public void toStringNamesTheLayout() {
		assertEquals("M3StackLayout {}", layout.toString());
	}

	static void expect(int code, Runnable action) {
		try {
			action.run();
			fail("expected SWT error " + code);
		} catch (IllegalArgumentException e) {
			assertEquals(message(code), e.getMessage());
		}
	}

	static String message(int code) {
		try {
			SWT.error(code);
			return null;
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		}
	}
}
