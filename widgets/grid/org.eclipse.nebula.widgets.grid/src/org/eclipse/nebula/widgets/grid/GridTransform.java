/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import org.eclipse.swt.graphics.Rectangle;

/**
 * Allocation-free SWT-style transform value used by Grid paint planning.
 *
 * <p>The matrix layout matches SWT Transform: m11, m12, m21, m22, dx, dy.
 * It owns no native resource and can therefore be retained in immutable paint
 * plans without coupling logical viewport state to a GC.</p>
 */
final class GridTransform {

	static final GridTransform IDENTITY =
			new GridTransform(1, 0, 0, 1, 0, 0);

	final float m11;
	final float m12;
	final float m21;
	final float m22;
	final float dx;
	final float dy;

	GridTransform(float m11, float m12, float m21, float m22, float dx, float dy) {
		if (!Float.isFinite(m11) || !Float.isFinite(m12)
				|| !Float.isFinite(m21) || !Float.isFinite(m22)
				|| !Float.isFinite(dx) || !Float.isFinite(dy)) {
			throw new IllegalArgumentException("non-finite transform element");
		}
		this.m11 = m11;
		this.m12 = m12;
		this.m21 = m21;
		this.m22 = m22;
		this.dx = dx;
		this.dy = dy;
	}

	static GridTransform translate(float x, float y) {
        if (x == 0 && y == 0) {
            return IDENTITY;
        }
		return new GridTransform(1, 0, 0, 1, x, y);
	}

	static GridTransform scale(float x, float y) {
        if (x == 1 && y == 1) {
            return IDENTITY;
        }
		return new GridTransform(x, 0, 0, y, 0, 0);
	}

	static GridTransform rotate(float radians) {
		if (radians == 0f) {
			return IDENTITY;
		}
		// Preserve the exact integer geometry of canonical float quadrant angles.
		// Adjacent representable angles retain their ordinary trigonometric result.
		double quadrant = Math.rint(radians / (Math.PI / 2d));
		if (Math.abs(quadrant) <= 4d && radians == (float)(quadrant * (Math.PI / 2d))) {
			return switch ((int)quadrant & 3) {
				case 0 -> IDENTITY;
				case 1 -> new GridTransform(0, 1, -1, 0, 0, 0);
				case 2 -> new GridTransform(-1, 0, 0, -1, 0, 0);
				default -> new GridTransform(0, -1, 1, 0, 0, 0);
			};
		}
		float sin = (float)Math.sin(radians);
		float cos = (float)Math.cos(radians);
		return new GridTransform(cos, sin, -sin, cos, 0, 0);
	}

	static GridTransform shear(float x, float y) {
		if (x == 0f && y == 0f) {
			return IDENTITY;
		}
		return new GridTransform(1, y, x, 1, 0, 0);
	}

	GridTransform then(GridTransform next) {
        if (next == null) {
            throw new IllegalArgumentException("next");
        }
        if (next.isIdentity()) {
            return this;
        }
        if (isIdentity()) {
            return next;
        }
		return new GridTransform(
				next.m11 * m11 + next.m21 * m12,
				next.m12 * m11 + next.m22 * m12,
				next.m11 * m21 + next.m21 * m22,
				next.m12 * m21 + next.m22 * m22,
				next.m11 * dx + next.m21 * dy + next.dx,
				next.m12 * dx + next.m22 * dy + next.dy);
	}

	float determinant() {
		return m11 * m22 - m21 * m12;
	}

	GridTransform inverse() {
		if (isIdentity()) {
			return IDENTITY;
		}
		float determinant = determinant();
		if (!Float.isFinite(determinant) || determinant == 0f) {
			throw new IllegalStateException("non-invertible transform");
		}
		float inverseDeterminant = 1f / determinant;
		float i11 = m22 * inverseDeterminant;
		float i12 = -m12 * inverseDeterminant;
		float i21 = -m21 * inverseDeterminant;
		float i22 = m11 * inverseDeterminant;
		float idx = -(i11 * dx + i21 * dy);
		float idy = -(i12 * dx + i22 * dy);
		return new GridTransform(i11, i12, i21, i22, idx, idy);
	}

	boolean isIdentity() {
		return this == IDENTITY
				|| (same(m11, 1f) && same(m12, 0f) && same(m21, 0f)
				&& same(m22, 1f) && same(dx, 0f) && same(dy, 0f));
	}

	boolean isTranslationOnly() {
		return same(m11, 1f) && same(m12, 0f)
				&& same(m21, 0f) && same(m22, 1f);
	}

	private static boolean same(float left, float right) {
		return Float.floatToIntBits(left) == Float.floatToIntBits(right);
	}

	float[] elements() {
		return new float[] {m11, m12, m21, m22, dx, dy};
	}

	Rectangle mapBounds(Rectangle rectangle) {
        if (rectangle == null) {
            throw new IllegalArgumentException("rectangle");
        }
		float x1 = mapX(rectangle.x, rectangle.y);
		float y1 = mapY(rectangle.x, rectangle.y);
		float x2 = mapX(rectangle.x + rectangle.width, rectangle.y);
		float y2 = mapY(rectangle.x + rectangle.width, rectangle.y);
		float x3 = mapX(rectangle.x, rectangle.y + rectangle.height);
		float y3 = mapY(rectangle.x, rectangle.y + rectangle.height);
		float x4 = mapX(rectangle.x + rectangle.width, rectangle.y + rectangle.height);
		float y4 = mapY(rectangle.x + rectangle.width, rectangle.y + rectangle.height);
		int left = (int)Math.floor(Math.min(Math.min(x1, x2), Math.min(x3, x4)));
		int top = (int)Math.floor(Math.min(Math.min(y1, y2), Math.min(y3, y4)));
		int right = (int)Math.ceil(Math.max(Math.max(x1, x2), Math.max(x3, x4)));
		int bottom = (int)Math.ceil(Math.max(Math.max(y1, y2), Math.max(y3, y4)));
		return new Rectangle(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
	}

	private float mapX(float x, float y) {
		return m11 * x + m21 * y + dx;
	}

	private float mapY(float x, float y) {
		return m12 * x + m22 * y + dy;
	}
}
