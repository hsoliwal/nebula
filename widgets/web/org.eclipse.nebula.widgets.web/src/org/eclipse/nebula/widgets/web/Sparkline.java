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
package org.eclipse.nebula.widgets.web;

import org.eclipse.nebula.widgets.web.model.SparklineApi;
import org.eclipse.nebula.widgets.web.model.SparklineModel;
import org.eclipse.swt.widgets.Composite;

/** A React-style sparkline drawn into one SWT Canvas, with no per-point controls. */
public class Sparkline extends WebCanvas<SparklineModel> {
	public Sparkline(Composite parent, int style) {
		super(parent, style, new SparklineModel());
	}

	public SparklineApi api() {
		return getModel();
	}
}
