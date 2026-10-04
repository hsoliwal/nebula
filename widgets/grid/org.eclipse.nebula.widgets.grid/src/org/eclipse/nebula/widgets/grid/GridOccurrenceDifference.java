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

import org.eclipse.nebula.widgets.grid.GridVisibleRangeDiff.Difference;

/**
 * Strategy contract for a stable previous/current occurrence sequence.
 *
 * <p>The composition owner validates arrays and selects the equality policy first.
 * Results preserve encounter order, multiplicity, object references, list mutability
 * and equals direction. Checkpoints may cancel but must not mutate inputs.</p>
 */
interface GridOccurrenceDifference {
    <T> Difference<T> between(T[] previous, T[] current, Runnable checkpoint, boolean cancellable);
}
