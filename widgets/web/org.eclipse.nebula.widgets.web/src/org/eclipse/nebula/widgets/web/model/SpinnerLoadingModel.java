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
 * Lane model of the spinner-loading web widget: family ANIMVALUE (pattern ANIMVALUE-1) in mode
 * {@code SPINNER}, role {@code progressbar}, shape {@code CIRCLE}. Generated from the family template;
 * every behaviour lives in {@link AnimvalueModel}.
 */
public class SpinnerLoadingModel extends AnimvalueModel implements SpinnerLoadingApi {
	public SpinnerLoadingModel() {
		super(CONCEPT, WebStates.role("progressbar"), AnimvalueModel.SPINNER, WebStates.CIRCLE, 0);
	}

	/** A model filled with {@link #SAMPLE}. */
	public static SpinnerLoadingModel sample() {
		SpinnerLoadingModel m = new SpinnerLoadingModel();
		m.load(SAMPLE);
		return m;
	}
}
