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

/**
 * Existing Grid visible-range difference API and result value.
 *
 * <p>Behavior is composed by GridVisibleRangeDiffComposition. Its identity
 * policy, occurrence strategies and completion checkpoint are separate,
 * package-private replaceable atoms; widget API and result identity remain here.</p>
 */
final class GridVisibleRangeDiff {
    private GridVisibleRangeDiff() {
    }

    static final class Difference<T> {
        private final List<T> removed;
        private final List<T> added;

        Difference(List<T> removed, List<T> added) {
            this.removed = removed;
            this.added = added;
        }

        List<T> removed() {
            return removed;
        }

        List<T> added() {
            return added;
        }
    }

    /** Historical paint path: preserves its non-cancellable behavior. */
    static <T> Difference<T> between(T[] previous, T[] current) {
        return GridVisibleRangeDiffComposition.between(previous, current, null, false);
    }

    /** A caller may adapt its existing progress monitor as monitor::checkCanceled. */
    static <T> Difference<T> between(T[] previous, T[] current, Runnable checkpoint) {
        return GridVisibleRangeDiffComposition.between(previous, current, checkpoint, true);
    }
}
