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
 * API of the code-block web widget (concept {@code code-block}, family TOKEN, pattern TOKEN-1).
 * <p>
 * Viewport form: the family pattern (see CONCEPTS.tsv)
 * </p>
 * TODO TOKEN-1: concept-specific operations beyond the family API.
 */
public interface CodeBlockApi extends TokenApi {
	/** Concept id in CONCEPTS.tsv. */
	String CONCEPT = "code-block";

	/** Sample items used by the example and the tests. */
	String SAMPLE = "int answer = 42;";
}
