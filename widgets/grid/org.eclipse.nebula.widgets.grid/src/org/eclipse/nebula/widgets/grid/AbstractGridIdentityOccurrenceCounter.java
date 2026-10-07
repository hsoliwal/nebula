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

/**
 * Canonical extension base for the private occurrence-counter contract.
 * Deliberately owns no widget state, native handles, callbacks or payload copies.
 * Concrete providers must preserve the interface's ordered-phase semantics.
 */
abstract class AbstractGridIdentityOccurrenceCounter<T>
        implements GridIdentityOccurrenceCounter<T> {
    protected AbstractGridIdentityOccurrenceCounter() {
    }
}
