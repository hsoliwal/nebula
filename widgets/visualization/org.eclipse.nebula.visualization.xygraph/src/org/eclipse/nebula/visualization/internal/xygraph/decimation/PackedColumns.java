/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.visualization.internal.xygraph.decimation;

/**
 * Immutable snapshot of packed sample columns: {@code x[0 .. size)} and {@code y[0 .. size)}. The writer only
 * appends past {@code size} or replaces the arrays, so values below {@code size} never change for a given snapshot.
 */
public final class PackedColumns {

	public static final PackedColumns EMPTY = new PackedColumns(new double[0], new double[0], 0, true, 0);

	public final double[] x;
	public final double[] y;
	public final int size;
	/** x is non-decreasing and NaN free for every index below size */
	public final boolean sorted;
	/** incremented on every replacement of the arrays (not on appends) */
	public final int generation;

	public PackedColumns(double[] x, double[] y, int size, boolean sorted, int generation) {
		this.x = x;
		this.y = y;
		this.size = size;
		this.sorted = sorted;
		this.generation = generation;
	}
}
