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
// Generated fromtemplates/family/ANIMVALUE.api.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

/** API of the lane-driven animated values (pattern ANIMVALUE-1). */
public interface AnimvalueApi extends WebApi {
	/** Replaces the items: skeleton row kinds ({@code title}, {@code line}, {@code avatar}) or one label. */
	void load(String sample);

	void setMax(int max);

	int max();

	/** Determinate progress of item 0 (0 = indeterminate). */
	void setProgress(int value);

	/** Ease-in-out cubic of the phase (0..1000). */
	int eased();

	/** Runs the transition forward, or back when it already ran. */
	void play();
}
