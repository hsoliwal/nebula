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
 * Admits identity matching only for Object.equals semantics and checks the
 * exact-reference fast path. ClassValue retains class metadata, never input items.
 */
final class GridDifferenceIdentityPolicy {
    private static final ClassValue<Boolean> IDENTITY_EQUALITY = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("equals", Object.class).getDeclaringClass() == Object.class;
            } catch (NoSuchMethodException | SecurityException unavailable) {
                // Lack of reflective access never grants an optimization assumption.
                return false;
            }
        }
    };

    private GridDifferenceIdentityPolicy() {
    }

    static boolean identityOnly(Object[] values, Runnable checkpoint, boolean cancellable) {
        for (Object value : values) {
            GridDifferenceCheckpoint.check(checkpoint, cancellable);
            if (value != null && !IDENTITY_EQUALITY.get(value.getClass())) {
                return false;
            }
        }
        return true;
    }

    static boolean sameReferences(Object[] previous, Object[] current,
            Runnable checkpoint, boolean cancellable) {
        if (previous.length != current.length) {
            return false;
        }
        for (int i = 0; i < previous.length; i++) {
            GridDifferenceCheckpoint.check(checkpoint, cancellable);
            if (previous[i] != current[i]) {
                return false;
            }
        }
        return true;
    }
}
