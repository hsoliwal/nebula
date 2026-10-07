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

import java.util.Collections;
import java.util.Objects;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeDiff.Difference;

/** Ordered composition of admission, identity guards and occurrence strategies. */
final class GridVisibleRangeDiffComposition {
    private static final GridOccurrenceDifference IDENTITY = new GridIdentityOccurrenceDifference();
    private static final GridOccurrenceDifference EQUALITY = new GridEqualityOccurrenceDifference();

    private GridVisibleRangeDiffComposition() {
    }

    static <T> Difference<T> between(T[] previous, T[] current,
            Runnable checkpoint, boolean cancellable) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        GridDifferenceCheckpoint.check(checkpoint, cancellable);
        if (!GridDifferenceIdentityPolicy.identityOnly(previous, checkpoint, cancellable)
                || !GridDifferenceIdentityPolicy.identityOnly(current, checkpoint, cancellable)) {
            return EQUALITY.between(previous, current, checkpoint, cancellable);
        }
        if (GridDifferenceIdentityPolicy.sameReferences(previous, current, checkpoint, cancellable)) {
            return new Difference<>(Collections.emptyList(), Collections.emptyList());
        }
        return IDENTITY.between(previous, current, checkpoint, cancellable);
    }
}
