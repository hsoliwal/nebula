/****************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.nebula.cwt.v;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;

/**
 * Generic retained virtual control whose appearance is supplied by an
 * {@link IControlPainter}.
 *
 * <p>This class intentionally carries no application model. Clients keep
 * semantic state externally and project only visual state, events and data
 * through the existing {@link VControl} contract.</p>
 */
public class VCustom extends VControl {

	private Point preferredSize;

	/**
	 * Creates a custom virtual control using the default {@link VControlPainter}.
	 *
	 * @param panel parent virtual panel
	 * @param style SWT style bits
	 */
	public VCustom(VPanel panel, int style) {
		this(panel, style, new VControlPainter());
	}

	/**
	 * Creates a custom virtual control using the supplied painter.
	 *
	 * @param panel parent virtual panel
	 * @param style SWT style bits
	 * @param painter non-null painter
	 */
	public VCustom(VPanel panel, int style, IControlPainter painter) {
		super(panel, style);
		setPainter(painter);
	}

	@Override
	public Type getType() {
		return Type.Custom;
	}

	/**
	 * Returns a defensive copy of the preferred size, or {@code null} when
	 * normal {@link VControl} intrinsic sizing is used.
	 *
	 * @return preferred size or null
	 */
	public Point getPreferredSize() {
		return preferredSize == null
				? null
				: new Point(preferredSize.x, preferredSize.y);
	}

	/**
	 * Sets the complete preferred control size used by virtual layouts.
	 *
	 * @param width preferred width, non-negative
	 * @param height preferred height, non-negative
	 */
	public void setPreferredSize(int width, int height) {
		if (width < 0 || height < 0) {
			throw new IllegalArgumentException("preferred size must be non-negative");
		}
		preferredSize = new Point(width, height);
	}

	/**
	 * Sets the complete preferred control size used by virtual layouts.
	 *
	 * @param size non-null, non-negative preferred size
	 */
	public void setPreferredSize(Point size) {
		Objects.requireNonNull(size, "size");
		setPreferredSize(size.x, size.y);
	}

	/**
	 * Restores normal {@link VControl} intrinsic sizing.
	 */
	public void clearPreferredSize() {
		preferredSize = null;
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		Point size = preferredSize == null
				? super.computeSize(wHint, hHint, changed)
				: new Point(preferredSize.x, preferredSize.y);

		if (wHint != SWT.DEFAULT) {
			size.x = Math.max(0, wHint);
		}
		if (hHint != SWT.DEFAULT) {
			size.y = Math.max(0, hHint);
		}
		return size;
	}

	@Override
	public void setPainter(IControlPainter painter) {
		super.setPainter(Objects.requireNonNull(painter, "painter"));
	}
}
