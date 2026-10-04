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

import java.util.List;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeDiff.Difference;

/** Skeletal strategy base sharing the ordered completion checkpoint and result atom. */
abstract class AbstractGridOccurrenceDifference implements GridOccurrenceDifference {
    protected final <T> Difference<T> complete(List<T> removed, List<T> added,
            Runnable checkpoint, boolean cancellable) {
        GridDifferenceCheckpoint.check(checkpoint, cancellable);
        return new Difference<>(removed, added);
    }
}
