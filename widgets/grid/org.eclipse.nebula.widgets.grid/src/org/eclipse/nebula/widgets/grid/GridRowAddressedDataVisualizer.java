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
package org.eclipse.nebula.widgets.grid;

/**
 * Internal structural callbacks for a DataVisualizer whose payload is keyed by
 * logical Grid row rather than by GridItem facade identity.
 */
interface GridRowAddressedDataVisualizer {
	void insertRow(Grid grid, int index);
	void removeRow(Grid grid, int index);
	void truncateRows(Grid grid, int count);
	void clearRows(Grid grid);
}
