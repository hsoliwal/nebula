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
