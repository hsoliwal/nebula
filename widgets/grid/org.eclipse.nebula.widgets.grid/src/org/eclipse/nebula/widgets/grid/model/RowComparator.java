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
package org.eclipse.nebula.widgets.grid.model;

/**
 * Compares two model rows by their data. Rows are plain {@code int} model indices; no row
 * object is created.
 *
 * <p>{@link RowPermutation} breaks ties by the model row itself, so the comparator does not
 * need to be total: equal rows keep their model order (the result equals a stable sort).</p>
 *
 * @since 1.2
 */
@FunctionalInterface
public interface RowComparator {

	/**
	 * @param modelRowA a model row
	 * @param modelRowB another model row
	 * @return negative, zero or positive as {@code modelRowA} sorts before, equal to or after
	 *         {@code modelRowB}
	 */
	int compare(int modelRowA, int modelRowB);
}
