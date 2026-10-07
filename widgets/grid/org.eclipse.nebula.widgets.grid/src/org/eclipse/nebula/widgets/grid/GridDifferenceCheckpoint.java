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

import java.util.concurrent.CancellationException;

/** Shared cancellation atom; the historical non-cancellable entry remains inert. */
final class GridDifferenceCheckpoint {
    private GridDifferenceCheckpoint() {
    }

    static void check(Runnable checkpoint, boolean cancellable) {
        if (!cancellable) {
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Visible-range difference interrupted");
        }
        if (checkpoint != null) {
            checkpoint.run();
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Visible-range difference interrupted");
        }
    }
}
