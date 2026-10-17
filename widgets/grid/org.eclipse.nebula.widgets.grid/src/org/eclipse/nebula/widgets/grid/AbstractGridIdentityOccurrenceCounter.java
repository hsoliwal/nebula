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
