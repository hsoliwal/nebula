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
 * API of the card web widget (concept {@code card}, family CONTAINER, pattern CONTAINER-1).
 * <p>
 * Viewport form: a node-table region painted on the screen Canvas (border-radius + shadow + padding from the style table); no widget, like the 346 card components already in the corpus
 * </p>
 * TODO CONTAINER-1: concept-specific operations beyond the family API.
 */
public interface CardApi extends ContainerApi {
	/** Concept id in CONCEPTS.tsv. */
	String CONCEPT = "card";

	/** Sample items used by the example and the tests. */
	String SAMPLE = "Quarterly revenue~Up 12% on last quarter";
}
