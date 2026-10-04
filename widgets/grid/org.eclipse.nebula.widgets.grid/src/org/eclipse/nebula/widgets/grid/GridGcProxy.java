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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;

/**
 * Scoped proxy around SWT's final {@link GC}.
 *
 * <p>Renderers keep receiving the original GC. This owner only manages
 * temporary clip/affine state and restores it deterministically, preventing
 * one viewport plane from leaking graphics state into a later z-plane.</p>
 */
final class GridGcProxy implements AutoCloseable {

	private final GC gc;
	private final Rectangle originalClipping;
	private Transform originalTransform;
	private boolean transformCaptured;
	private boolean closed;

	private GridGcProxy(GC gc) {
		if (gc == null) throw new IllegalArgumentException("gc");
		this.gc = gc;
		this.originalClipping = gc.getClipping();
	}

	static GridGcProxy wrap(GC gc) {
		return new GridGcProxy(gc);
	}

	GC gc() {
		return gc;
	}

	GridGcProxy clip(Rectangle clipping) {
		if (clipping == null) throw new IllegalArgumentException("clipping");
		gc.setClipping(originalClipping.intersection(clipping));
		return this;
	}

	GridGcProxy transform(GridAffineTransform affine) {
		if (affine == null) throw new IllegalArgumentException("affine");
		if (affine == GridAffineTransform.IDENTITY) return this;
		captureTransform();
		Transform next = new Transform(gc.getDevice());
		Transform delta = null;
		try {
			gc.getTransform(next);
			float[] e = affine.elements();
			delta = new Transform(gc.getDevice(), e);
			next.multiply(delta);
			gc.setTransform(next);
		} finally {
			if (delta != null) delta.dispose();
			next.dispose();
		}
		return this;
	}

	GridGcProxy translate(float x, float y) {
		return transform(GridAffineTransform.translation(x, y));
	}

	private void captureTransform() {
		if (transformCaptured) return;
		originalTransform = new Transform(gc.getDevice());
		gc.getTransform(originalTransform);
		transformCaptured = true;
	}

	@Override
	public void close() {
		if (closed) return;
		closed = true;
		if (transformCaptured) {
			try {
				gc.setTransform(originalTransform);
			} finally {
				originalTransform.dispose();
			}
		}
		gc.setClipping(originalClipping);
	}
}
