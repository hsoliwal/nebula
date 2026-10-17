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
package org.eclipse.nebula.widgets.grid.internal;

/**
 * Whether the Grid of this class loader fixes the upstream defects listed in
 * {@code docs/m3/grid-viewport/UPSTREAM_DEFECTS.tsv} (every mode but {@code original}, which stays
 * behaviourally identical to stock Nebula Grid). The Grid sets it once, when its mode is read; the
 * internal renderers read it.
 */
public final class UpstreamFixes {
	private static volatile boolean enabled;

	private UpstreamFixes() {
	}

	/**
	 * @return whether the upstream defects are fixed
	 */
	public static boolean enabled() {
		return enabled;
	}

	/**
	 * @param value whether the upstream defects are fixed (set by the Grid when its mode is read)
	 */
	public static void set(final boolean value) {
		enabled = value;
	}
}
