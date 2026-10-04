/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
