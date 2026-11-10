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
package org.eclipse.nebula.widgets.timeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.nebula.widgets.timeline.jface.DefaultTimelineStyleProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget leaf of the timeline: the style provider is disposed when the composite is disposed (its contract: clean
 * up colours and fonts), also when only an ancestor is disposed, and once for an explicit dispose().
 */
public class TimelineDisposeTest {

	static final class CountingStyle extends DefaultTimelineStyleProvider {
		int disposed;

		CountingStyle(ResourceManager resourceManager) {
			super(resourceManager);
		}

		@Override
		public void dispose() {
			disposed++;
			super.dispose();
		}
	}

	private Shell shell;

	@Before
	public void setUp() {
		shell = new Shell(Display.getDefault());
		shell.setLayout(new FillLayout());
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void theStyleProviderIsDisposedWithAnAncestor() {
		Composite view = new Composite(shell, SWT.NONE);
		view.setLayout(new FillLayout());
		TimelineComposite timeline = new TimelineComposite(view, SWT.NONE);
		CountingStyle style = install(timeline);

		view.dispose();
		assertTrue(timeline.isDisposed());
		assertEquals("the style provider of a timeline disposed with its ancestor", 1, style.disposed);
	}

	@Test
	public void anExplicitDisposeDisposesTheStyleProviderOnce() {
		TimelineComposite timeline = new TimelineComposite(shell, SWT.NONE);
		CountingStyle style = install(timeline);

		timeline.dispose();
		assertEquals(1, style.disposed);
	}

	private static CountingStyle install(TimelineComposite timeline) {
		CountingStyle style = new CountingStyle(timeline.getRootFigure().getResourceManager());
		timeline.getRootFigure().setStyleProvider(style);
		return style;
	}
}
