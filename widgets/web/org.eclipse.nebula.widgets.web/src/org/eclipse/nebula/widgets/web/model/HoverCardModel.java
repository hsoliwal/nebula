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
 * Lane model of the hover-card web widget: family OVERLAY (pattern OVERLAY-1) in mode
 * {@code TARGET}, role {@code tooltip}, shape {@code ROUND}. Generated from the family template;
 * every behaviour lives in {@link OverlayModel}.
 */
public class HoverCardModel extends OverlayModel implements HoverCardApi {
	public HoverCardModel() {
		super(CONCEPT, WebStates.role("tooltip"), OverlayModel.TARGET, WebStates.ROUND, 0);
	}

	/** A model filled with {@link #SAMPLE}. */
	public static HoverCardModel sample() {
		HoverCardModel m = new HoverCardModel();
		m.load(SAMPLE);
		return m;
	}
}
