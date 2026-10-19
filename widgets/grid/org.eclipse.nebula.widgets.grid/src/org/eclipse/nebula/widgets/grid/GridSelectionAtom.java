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

import java.util.List;

/** Package-private row-selection pattern atom shared by Grid's index overloads. */
final class GridSelectionAtom {

	private GridSelectionAtom() {
	}

	static boolean selectOne(List<GridItem> selectedItems, GridItem item) {
		if (selectedItems.contains(item)) {
			return false;
		}
		selectedItems.add(item);
		return true;
	}

	static void selectRange(
			List<GridItem> items, List<GridItem> selectedItems, int start, int end) {
		for (int index = start; index <= end; index++) {
			if (index < 0) {
				continue;
			}
			if (index >= items.size()) {
				break;
			}
			selectOne(selectedItems, items.get(index));
		}
	}

	static void selectIndices(
			List<GridItem> items, List<GridItem> selectedItems, int[] indices) {
		for (int index : indices) {
			if (index >= 0 && index < items.size()) {
				selectOne(selectedItems, items.get(index));
			}
		}
	}

	static void deselectRange(
			List<GridItem> items, List<GridItem> selectedItems, int start, int end) {
		for (int index = start; index <= end; index++) {
			if (index < 0) {
				continue;
			}
			if (index >= items.size()) {
				break;
			}
			selectedItems.remove(items.get(index));
		}
	}

	static void deselectIndices(
			List<GridItem> items, List<GridItem> selectedItems, int[] indices) {
		for (int index : indices) {
			if (index >= 0 && index < items.size()) {
				selectedItems.remove(items.get(index));
			}
		}
	}
}
