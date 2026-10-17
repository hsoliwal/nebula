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
