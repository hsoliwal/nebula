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
// Generated fromtemplates/nebula/Widget.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web;

import org.eclipse.nebula.widgets.web.model.BackdropApi;
import org.eclipse.nebula.widgets.web.model.BackdropModel;
import org.eclipse.swt.widgets.Composite;

/**
 * Web backdrop for Nebula: one drawn Canvas on the shared viewport core, family OVERLAY
 * (pattern OVERLAY-1). Paints through the GC only, repaints only model damage, defers while the
 * part is hidden. Behaviour is the {@link BackdropApi} of {@link #api()}.
 */
public class Backdrop extends WebCanvas<BackdropModel> {
	public Backdrop(Composite parent, int style) {
		super(parent, style, new BackdropModel());
	}

	/** The widget API (the lane model). */
	public BackdropApi api() {
		return getModel();
	}
}
