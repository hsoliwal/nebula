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
// Generated fromtemplates/member/Model.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;


/**
 * Lane model of the drawer web widget: family OVERLAY (pattern OVERLAY-1) in mode
 * {@code EDGE_START}, role {@code dialog}, shape {@code RECT}. Generated from the family template;
 * every behaviour lives in {@link OverlayModel}.
 */
public class DrawerModel extends OverlayModel implements DrawerApi {
	public DrawerModel() {
		super(CONCEPT, WebStates.role("dialog"), OverlayModel.EDGE_START, WebStates.RECT, 0);
	}

	/** A model filled with {@link #SAMPLE}. */
	public static DrawerModel sample() {
		DrawerModel m = new DrawerModel();
		m.load(SAMPLE);
		return m;
	}
}
