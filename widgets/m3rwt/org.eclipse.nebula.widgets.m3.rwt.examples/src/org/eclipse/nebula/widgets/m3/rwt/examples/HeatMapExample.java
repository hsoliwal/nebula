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

import org.eclipse.nebula.widgets.m3.M3HeatMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** HEATMAP on RWT: M3HeatMap over 10,000 rows by 100 columns; only the visible cells are painted. */
public final class HeatMapExample extends RwtExample {

	static final int ROWS = 10_000;
	static final int COLUMNS = 100;

	public HeatMapExample () {
		super ("HEATMAP", "org.eclipse.nebula.widgets.m3.M3HeatMap");
	}

	@Override
	public Control create (Composite parent) {
		M3HeatMap map = new M3HeatMap (parent, SWT.H_SCROLL | SWT.V_SCROLL);
		map.setSize (ROWS, COLUMNS);
		float[] values = new float[ROWS * COLUMNS];
		for (int i = 0; i < values.length; i++) values[i] = ((i * 2654435761L) >>> 16 & 0xFFFF) / 65535f;
		map.setValues (values);
		return map;
	}

	@Override
	public void change (Control control) {
		((M3HeatMap) control).setValue (0, 0, 0.5f);
	}
}
