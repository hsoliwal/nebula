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
package org.eclipse.nebula.widgets.grid;

/**
 * Internal structural callbacks for a DataVisualizer whose payload is keyed by
 * logical Grid row rather than by GridItem facade identity.
 */
interface GridRowAddressedDataVisualizer {
	void insertRow(Grid grid, int index);
	void removeRow(Grid grid, int index);
	/** {@code count} rows were inserted at {@code index}: one shift of the rows after them. */
	void insertRows(Grid grid, int index, int count);
	/** The rows [{@code index}, {@code index + count}) were removed: one shift of the rows after them. */
	void removeRows(Grid grid, int index, int count);
	void truncateRows(Grid grid, int count);
	void clearRows(Grid grid);
}
