/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;

/**
 * Scoped proxy around SWT's final {@link GC}.
 *
 * <p>Renderers keep receiving the original GC, but every viewport plane is wrapped
 * in one of these scopes. The proxy snapshots all public mutable graphics state
 * that can leak between Grid renderers and restores it deterministically on close.
 * Native graphics resources remain owned by SWT; only the temporary Transform
 * snapshot is allocated and disposed by this scope.</p>
 */
final class GridGcProxy implements AutoCloseable {

	private final GC gc;
	private final Rectangle originalClipping;
	private final Transform originalTransform;
	private final LineAttributes originalLineAttributes;
	private final int originalAlpha;
	private final int originalAntialias;
	private final int originalTextAntialias;
	private final int originalInterpolation;
	private final int originalFillRule;
	private final boolean originalXorMode;
	private final Color originalForeground;
	private final Color originalBackground;
	private final Pattern originalForegroundPattern;
	private final Pattern originalBackgroundPattern;
	private final Font originalFont;
	private boolean closed;

	private GridGcProxy(GC gc) {
		if (gc == null) throw new IllegalArgumentException("gc");
		this.gc = gc;
		this.originalClipping = gc.getClipping();
		this.originalTransform = new Transform(gc.getDevice());
		gc.getTransform(originalTransform);
		this.originalLineAttributes = copy(gc.getLineAttributes());
		this.originalAlpha = gc.getAlpha();
		this.originalAntialias = gc.getAntialias();
		this.originalTextAntialias = gc.getTextAntialias();
		this.originalInterpolation = gc.getInterpolation();
		this.originalFillRule = gc.getFillRule();
		this.originalXorMode = gc.getXORMode();
		this.originalForeground = gc.getForeground();
		this.originalBackground = gc.getBackground();
		this.originalForegroundPattern = gc.getForegroundPattern();
		this.originalBackgroundPattern = gc.getBackgroundPattern();
		this.originalFont = gc.getFont();
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

	GridGcProxy lineAttributes(LineAttributes attributes) {
		if (attributes == null) throw new IllegalArgumentException("attributes");
		gc.setLineAttributes(copy(attributes));
		return this;
	}

	GridGcProxy alpha(int alpha) {
		gc.setAlpha(alpha);
		return this;
	}

	@Override
	public void close() {
		if (closed) return;
		closed = true;
		try {
			gc.setTransform(originalTransform);
			gc.setLineAttributes(originalLineAttributes);
			gc.setAlpha(originalAlpha);
			gc.setAntialias(originalAntialias);
			gc.setTextAntialias(originalTextAntialias);
			gc.setInterpolation(originalInterpolation);
			gc.setFillRule(originalFillRule);
			gc.setXORMode(originalXorMode);
			gc.setForeground(originalForeground);
			gc.setBackground(originalBackground);
			gc.setForegroundPattern(originalForegroundPattern);
			gc.setBackgroundPattern(originalBackgroundPattern);
			gc.setFont(originalFont);
			gc.setClipping(originalClipping);
		} finally {
			originalTransform.dispose();
		}
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
