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

import org.eclipse.nebula.widgets.grid.Grid.GridVisibleRange;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeDiff.Difference;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeSupport.RangeChangedEvent;

/** Package-private event-composition atom for visible-range change publication. */
final class GridVisibleRangeEventAtom {

	private GridVisibleRangeEventAtom() {
	}

	static boolean changed(
			Difference<GridItem> items,
			Difference<GridColumn> columns) {
		return !items.removed().isEmpty()
				|| !items.added().isEmpty()
				|| !columns.removed().isEmpty()
				|| !columns.added().isEmpty();
	}

	static RangeChangedEvent event(
			Grid grid,
			GridVisibleRange range,
			Difference<GridItem> items,
			Difference<GridColumn> columns) {
		RangeChangedEvent event = new RangeChangedEvent(grid, range);
		event.addedRows = items.added().toArray(new GridItem[items.added().size()]);
		event.removedRows = items.removed().toArray(new GridItem[items.removed().size()]);
		event.addedColumns = columns.added().toArray(new GridColumn[columns.added().size()]);
		event.removedColumns = columns.removed().toArray(new GridColumn[columns.removed().size()]);
		return event;
	}
}
