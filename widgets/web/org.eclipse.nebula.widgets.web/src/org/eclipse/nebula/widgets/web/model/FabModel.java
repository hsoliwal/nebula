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
 * Lane model of the fab web widget: family OVERLAY (pattern OVERLAY-1) in mode
 * {@code CORNER_END}, role {@code button}, shape {@code CIRCLE}. Generated from the family template;
 * every behaviour lives in {@link OverlayModel}.
 */
public class FabModel extends OverlayModel implements FabApi {
	public FabModel() {
		super(CONCEPT, WebStates.role("button"), OverlayModel.CORNER_END, WebStates.CIRCLE, 1);
	}

	/** A model filled with {@link #SAMPLE}. */
	public static FabModel sample() {
		FabModel m = new FabModel();
		m.load(SAMPLE);
		return m;
	}
}
