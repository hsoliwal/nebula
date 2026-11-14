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
 * API of the stat-kpi web widget (concept {@code stat-kpi}, family CONTAINER, pattern CONTAINER-1).
 * <p>
 * Viewport form: a node-table region (label + big value + delta + sparkline); pure composition
 * </p>
 * TODO CONTAINER-1: concept-specific operations beyond the family API.
 */
public interface StatKpiApi extends ContainerApi {
	/** Concept id in CONCEPTS.tsv. */
	String CONCEPT = "stat-kpi";

	/** Sample items used by the example and the tests. */
	String SAMPLE = "Active users~1284~12";
}
