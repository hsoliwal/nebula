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
 * Filter test on one model row. Called once per model row by
 * {@link RowPermutation#setFilter(RowPredicate)}; the answers are stored as one bit per row.
 *
 * @since 1.2
 */
@FunctionalInterface
public interface RowPredicate {

	/**
	 * @param modelRow a model row
	 * @return {@code true} if the row stays visible
	 */
	boolean test(int modelRow);
}
