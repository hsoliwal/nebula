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
import org.eclipse.swt.graphics.Region;
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
final class GridGCProxy implements AutoCloseable {

	private final GC gc;
	private final Region originalClipping;
	private final boolean originalAdvanced;
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

	private GridGCProxy(GC gc) {
        if (gc == null) {
            throw new IllegalArgumentException("gc");
        }
		this.gc = gc;
		this.originalAdvanced = gc.getAdvanced();
		this.originalClipping = new Region(gc.getDevice());
		gc.getClipping(originalClipping);
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

	static GridGCProxy wrap(GC gc) {
		return new GridGCProxy(gc);
	}

	GC gc() {
		return gc;
	}

	GridGCProxy clip(Rectangle clipping) {
        if (clipping == null) {
            throw new IllegalArgumentException("clipping");
        }
		Region next = new Region(gc.getDevice());
		try {
			next.add(clipping);
			next.intersect(originalClipping);
			gc.setClipping(next);
		} finally {
			next.dispose();
		}
		return this;
	}

	GridGCProxy transform(GridTransform transform) {
        if (transform == null) {
            throw new IllegalArgumentException("transform");
        }
        if (transform.isIdentity()) {
            return this;
        }
		Transform next = new Transform(gc.getDevice());
		Transform delta = null;
		try {
			gc.getTransform(next);
			delta = new Transform(
					gc.getDevice(),
					transform.m11, transform.m12, transform.m21, transform.m22, transform.dx, transform.dy);
			next.multiply(delta);
			gc.setTransform(next);
		} finally {
            if (delta != null) {
                delta.dispose();
            }
			next.dispose();
		}
		return this;
	}

	GridGCProxy translate(float x, float y) {
		return transform(GridTransform.translate(x, y));
	}

	GridGCProxy lineAttributes(LineAttributes attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("attributes");
        }
		gc.setLineAttributes(copy(attributes));
		return this;
	}

	GridGCProxy alpha(int alpha) {
		gc.setAlpha(alpha);
		return this;
	}

	@Override
	public void close() {
        if (closed) {
            return;
        }
		closed = true;
		try {
			gc.setLineAttributes(originalLineAttributes);
			if (originalAdvanced) {
				gc.setTransform(originalTransform);
				gc.setAlpha(originalAlpha);
				gc.setAntialias(originalAntialias);
				gc.setTextAntialias(originalTextAntialias);
				gc.setInterpolation(originalInterpolation);
				gc.setFillRule(originalFillRule);
			} else {
				/*
				 * Turning advanced mode off resets transform/pattern/alpha/AA state.
				 * Do it before restoring the basic state and exact clip below.
				 */
				gc.setAdvanced(false);
			}
			gc.setXORMode(originalXorMode);
			gc.setForeground(originalForeground);
			gc.setBackground(originalBackground);
			if (originalAdvanced) {
				gc.setForegroundPattern(originalForegroundPattern);
				gc.setBackgroundPattern(originalBackgroundPattern);
			}
			gc.setFont(originalFont);
			gc.setClipping(originalClipping);
		} finally {
			originalTransform.dispose();
			originalClipping.dispose();
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
