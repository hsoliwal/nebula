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

import org.eclipse.swt.graphics.LineAttributes;

/**
 * Primitive graphics-state transition planner used by the Grid drawing proxy.
 *
 * <p>The DAG is deliberately allocation-light: stroke and alpha nodes are
 * retained as semantic state, and repeated requests collapse to the same node
 * instead of issuing another native SWT GC mutation. Ordered affine deltas are
 * composed in six primitive float lanes and become one native transition only
 * when the proxy crosses the raw-GC renderer boundary.</p>
 */
final class GridGCStateDAG {

	static final int STROKE = 1 << 0;
	static final int ALPHA = 1 << 1;
	static final int TRANSFORM = 1 << 2;

	private LineAttributes lineAttributes;
	private int alpha;
	private float transformM11 = 1f;
	private float transformM12;
	private float transformM21;
	private float transformM22 = 1f;
	private float transformDx;
	private float transformDy;
	private boolean transformPending;
	private int nativeTransitions;

	GridGCStateDAG(LineAttributes lineAttributes, int alpha) {
		if (lineAttributes == null) {
			throw new IllegalArgumentException("lineAttributes");
		}
		this.lineAttributes = copy(lineAttributes);
		this.alpha = alpha;
	}

	int planStroke(LineAttributes next) {
		if (next == null) {
			throw new IllegalArgumentException("next");
		}
		if (same(lineAttributes, next)) {
			return 0;
		}
		lineAttributes = copy(next);
		nativeTransitions++;
		return STROKE;
	}

	int planAlpha(int next) {
		if (alpha == next) {
			return 0;
		}
		alpha = next;
		nativeTransitions++;
		return ALPHA;
	}

	int planTransform(GridTransform delta) {
		if (delta == null) {
			throw new IllegalArgumentException("delta");
		}
		if (delta.isIdentity()) {
			return 0;
		}
		if (!transformPending) {
			transformM11 = delta.m11;
			transformM12 = delta.m12;
			transformM21 = delta.m21;
			transformM22 = delta.m22;
			transformDx = delta.dx;
			transformDy = delta.dy;
			transformPending = true;
		} else {
			float m11 = delta.m11 * transformM11 + delta.m21 * transformM12;
			float m12 = delta.m12 * transformM11 + delta.m22 * transformM12;
			float m21 = delta.m11 * transformM21 + delta.m21 * transformM22;
			float m22 = delta.m12 * transformM21 + delta.m22 * transformM22;
			float dx = delta.m11 * transformDx + delta.m21 * transformDy + delta.dx;
			float dy = delta.m12 * transformDx + delta.m22 * transformDy + delta.dy;
			transformM11 = m11;
			transformM12 = m12;
			transformM21 = m21;
			transformM22 = m22;
			transformDx = dx;
			transformDy = dy;
		}
		if (isPendingIdentity()) {
			resetTransform();
		}
		return TRANSFORM;
	}

	boolean hasPendingTransform() {
		return transformPending;
	}

	GridTransform consumeTransform() {
		if (!transformPending) {
			return GridTransform.IDENTITY;
		}
		GridTransform result = new GridTransform(
				transformM11, transformM12, transformM21, transformM22, transformDx, transformDy);
		resetTransform();
		nativeTransitions++;
		return result;
	}

	int nativeTransitions() {
		return nativeTransitions;
	}

	private boolean isPendingIdentity() {
		return transformPending
				&& same(transformM11, 1f)
				&& same(transformM12, 0f)
				&& same(transformM21, 0f)
				&& same(transformM22, 1f)
				&& same(transformDx, 0f)
				&& same(transformDy, 0f);
	}

	private void resetTransform() {
		transformM11 = transformM22 = 1f;
		transformM12 = transformM21 = transformDx = transformDy = 0f;
		transformPending = false;
	}

	private static boolean same(LineAttributes left, LineAttributes right) {
		if (Float.floatToIntBits(left.width) != Float.floatToIntBits(right.width)
				|| left.cap != right.cap
				|| left.join != right.join
				|| left.style != right.style
				|| Float.floatToIntBits(left.dashOffset) != Float.floatToIntBits(right.dashOffset)
				|| Float.floatToIntBits(left.miterLimit) != Float.floatToIntBits(right.miterLimit)) {
			return false;
		}
		if (left.dash == right.dash) {
			return true;
		}
		if (left.dash == null || right.dash == null || left.dash.length != right.dash.length) {
			return false;
		}
		for (int i = 0; i < left.dash.length; i++) {
			if (Float.floatToIntBits(left.dash[i]) != Float.floatToIntBits(right.dash[i])) {
				return false;
			}
		}
		return true;
	}

	private static boolean same(float left, float right) {
		return Float.floatToIntBits(left) == Float.floatToIntBits(right);
	}

	private static LineAttributes copy(LineAttributes attributes) {
		float[] dash = attributes.dash == null ? null : attributes.dash.clone();
		return new LineAttributes(
				attributes.width,
				attributes.cap,
				attributes.join,
				attributes.style,
				dash,
				attributes.dashOffset,
				attributes.miterLimit);
	}
}
