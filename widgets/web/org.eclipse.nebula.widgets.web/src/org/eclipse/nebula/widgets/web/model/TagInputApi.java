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
// Generated fromtemplates/member/Api.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;


/**
 * API of the tag-input web widget (concept {@code tag-input}, family INPUT, pattern INPUT-1).
 * <p>
 * Viewport form: a Text plus a row of chip nodes backed by a packed tag-id array
 * </p>
 * TODO INPUT-1: concept-specific operations beyond the family API.
 */
public interface TagInputApi extends InputApi {
	/** Concept id in CONCEPTS.tsv. */
	String CONCEPT = "tag-input";

	/** Sample items used by the example and the tests. */
	String SAMPLE = "java;swt;swing";
}
