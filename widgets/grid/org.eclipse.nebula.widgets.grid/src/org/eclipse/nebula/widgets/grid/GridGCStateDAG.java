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
 * instead of issuing another native SWT GC mutation. Transform composition
 * remains ordered because applying the same affine delta twice is observable.</p>
 */
final class GridGCStateDAG {

	static final int STROKE = 1 << 0;
	static final int ALPHA = 1 << 1;
	static final int TRANSFORM = 1 << 2;

	private LineAttributes lineAttributes;
	private int alpha;
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
		nativeTransitions++;
		return TRANSFORM;
	}

	int nativeTransitions() {
		return nativeTransitions;
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
