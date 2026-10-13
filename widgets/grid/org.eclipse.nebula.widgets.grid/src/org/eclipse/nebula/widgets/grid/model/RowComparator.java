/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
