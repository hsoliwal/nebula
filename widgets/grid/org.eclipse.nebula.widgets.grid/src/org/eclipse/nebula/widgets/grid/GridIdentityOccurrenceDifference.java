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
