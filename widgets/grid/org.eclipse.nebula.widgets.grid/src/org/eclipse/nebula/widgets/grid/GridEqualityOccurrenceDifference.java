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
import java.util.Arrays;
import java.util.Iterator;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeDiff.Difference;

/** Original remove-first-equal matching, preserving asymmetric/throwing equals. */
final class GridEqualityOccurrenceDifference extends AbstractGridOccurrenceDifference {
    @Override
    public <T> Difference<T> between(T[] previous, T[] current,
            Runnable checkpoint, boolean cancellable) {
        ArrayList<T> removed = new ArrayList<>(Arrays.asList(previous));
        ArrayList<T> added = new ArrayList<>(Arrays.asList(current));
        Iterator<T> iterator = added.iterator();
        while (iterator.hasNext()) {
            GridDifferenceCheckpoint.check(checkpoint, cancellable);
            if (removed.remove(iterator.next())) {
                iterator.remove();
            }
        }
        return complete(removed, added, checkpoint, cancellable);
    }
}
