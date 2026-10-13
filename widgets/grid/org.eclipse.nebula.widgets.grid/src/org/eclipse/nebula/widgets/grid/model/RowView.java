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
 * Displayed rows over a model: view position {@code 0..getViewCount()-1} to model row and back.
 * Implemented by {@link RowPermutation} (sort, filter) and {@link RowGrouping} (group headers
 * over a permutation).
 *
 * @since 1.2
 */
public interface RowView {

	/**
	 * @return number of model rows
	 */
	int getModelCount();

	/**
	 * @return number of displayed rows
	 */
	int getViewCount();

	/**
	 * @param viewIndex a displayed row
	 * @return the model row shown there, or {@code -1} for a row that shows no model row (a group
	 *         header)
	 */
	int modelRow(int viewIndex);

	/**
	 * @param modelRow a model row
	 * @return its displayed row, or {@code -1} if it is not displayed
	 */
	int viewIndex(int modelRow);

	/**
	 * @return a counter that changes whenever the mapping changes (31-bit, wraps to 0)
	 */
	int getVersion();
}
