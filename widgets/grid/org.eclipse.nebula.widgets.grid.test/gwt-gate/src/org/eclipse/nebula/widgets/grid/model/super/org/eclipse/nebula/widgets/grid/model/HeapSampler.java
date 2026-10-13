/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.model;

/** GWT super-source of {@code HeapSampler}: JavaScript has no heap limit to sample. */
final class HeapSampler {

	private HeapSampler() {
	}

	static long maxMemory() {
		return 0L;
	}

	static long usedMemory() {
		return 0L;
	}
}
