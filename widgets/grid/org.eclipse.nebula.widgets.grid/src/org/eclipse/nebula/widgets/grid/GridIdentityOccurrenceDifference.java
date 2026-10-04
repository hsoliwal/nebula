/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeDiff.Difference;

/** Packed identity occurrence matching; no calls to element equals/hashCode. */
final class GridIdentityOccurrenceDifference extends AbstractGridOccurrenceDifference {
    @Override
    public <T> Difference<T> between(T[] previous, T[] current,
            Runnable checkpoint, boolean cancellable) {
        GridIdentityOccurrenceCounter<T> counts = new GridIdentityOccurrenceTable<>();
        for (T value : previous) {
            GridDifferenceCheckpoint.check(checkpoint, cancellable);
            counts.addPrevious(value);
        }
        List<T> added = new ArrayList<>();
        for (T value : current) {
            GridDifferenceCheckpoint.check(checkpoint, cancellable);
            if (!counts.matchCurrent(value)) {
                added.add(value);
            }
        }
        List<T> removed = new ArrayList<>();
        for (T value : previous) {
            GridDifferenceCheckpoint.check(checkpoint, cancellable);
            if (!counts.skipMatchedPrevious(value)) {
                removed.add(value);
            }
        }
        return complete(removed, added, checkpoint, cancellable);
    }
}
