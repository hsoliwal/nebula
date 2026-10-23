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
 * Invocation-local identity multiset for the visible-range difference atom.
 * Use in three ordered phases: add all previous occurrences, match all current
 * occurrences, then skip the first matched occurrences while traversing previous.
 * Null is a supported identity. This is not an equals-based collection.
 */
interface GridIdentityOccurrenceCounter<T> {
    void addPrevious(T value);

    boolean matchCurrent(T value);

    boolean skipMatchedPrevious(T value);
}
