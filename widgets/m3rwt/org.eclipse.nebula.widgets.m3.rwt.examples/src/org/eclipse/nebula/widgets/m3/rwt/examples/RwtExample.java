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
package org.eclipse.nebula.widgets.m3.rwt.examples;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * One widget of the custom-widget matrix shown on RWT: what it creates, and one model change that the probe uses
 * to measure what a change sends to the browser while the widget is visible and while it is hidden.
 */
public abstract class RwtExample {

	/** Concept id of docs/m3/custom-widgets/WIDGET_MATRIX.tsv. */
	public final String concept;
	/** The class that the example shows. */
	public final String widget;

	protected RwtExample (String concept, String widget) {
		this.concept = concept;
		this.widget = widget;
	}

	/** Creates the widget in {@code parent} (a Shell with a FillLayout) and returns it. */
	public abstract Control create (Composite parent);

	/** Applies one model change to the widget created by {@link #create(Composite)}. */
	public abstract void change (Control control);

	/** URL path of the example's entry point. */
	public String path () {
		return "/" + concept.toLowerCase ().replace ('_', '-');
	}
}
