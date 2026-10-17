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
