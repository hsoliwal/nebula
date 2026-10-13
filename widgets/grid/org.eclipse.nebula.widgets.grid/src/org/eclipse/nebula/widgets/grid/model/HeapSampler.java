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

/**
 * Heap measurement for {@link HeapGrowth}: the only class of the model package that touches
 * {@code java.lang.Runtime}. A GWT build replaces it (super-source) with a sampler that reports no
 * information, so growth falls back to plain doubling.
 */
final class HeapSampler {

	private HeapSampler() {
	}

	/** @return {@code Runtime.maxMemory()} */
	static long maxMemory() {
		return Runtime.getRuntime().maxMemory();
	}

	/** @return used heap ({@code totalMemory - freeMemory}) */
	static long usedMemory() {
		final Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}
}
