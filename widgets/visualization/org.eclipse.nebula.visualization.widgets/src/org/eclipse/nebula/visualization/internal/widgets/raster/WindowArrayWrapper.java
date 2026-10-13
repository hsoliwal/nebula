/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Synexia contributors - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.visualization.internal.widgets.raster;

import org.eclipse.nebula.visualization.widgets.datadefinition.DoubleArrayWrapper;
import org.eclipse.nebula.visualization.widgets.datadefinition.IPrimaryArrayWrapper;

/**
 * Read-only rectangular window of a row-major array wrapper ({@code channels} values per element), indexed like the
 * packed copy {@code IntensityGraphFigure} used to make for a crop: no copy of the window is made.
 */
public final class WindowArrayWrapper implements IPrimaryArrayWrapper {

	public final IPrimaryArrayWrapper source;
	public final int sourceWidth;
	public final int left;
	public final int top;
	public final int width;
	public final int height;
	public final int channels;

	public WindowArrayWrapper(IPrimaryArrayWrapper source, int sourceWidth, int left, int top, int width, int height,
			int channels) {
		this.source = source;
		this.sourceWidth = sourceWidth;
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.channels = channels;
	}

	@Override
	public double get(int i) {
		if (i < 0 || i >= getSize()) {
			throw new ArrayIndexOutOfBoundsException(i);
		}
		int p = i / channels;
		int c = i - p * channels;
		int row = p / width;
		int col = p - row * width;
		return source.get(((top + row) * sourceWidth + left + col) * channels + c);
	}

	@Override
	public int getSize() {
		return width * height * channels;
	}

	/** The packed array under a single-channel window of a {@link DoubleArrayWrapper}, or null. */
	public double[] packed() {
		return channels == 1 && source instanceof DoubleArrayWrapper ? ((DoubleArrayWrapper) source).data : null;
	}
}
