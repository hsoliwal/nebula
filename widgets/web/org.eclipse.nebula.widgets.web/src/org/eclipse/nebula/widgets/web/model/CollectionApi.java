/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
// Generated fromtemplates/family/COLLECTION.api.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

/** API of the virtual collections (pattern COLLECTION-1). */
public interface CollectionApi extends WebApi {
	/**
	 * Applies a declarative keyed row snapshot. Existing implementers remain binary-compatible;
	 * implementations without stable-key reconciliation fail explicitly.
	 */
	default void reconcile(String[] keys, String[] labels) {
		throw new UnsupportedOperationException("keyed collection reconciliation is not supported");
	}

	/**
	 * Reconciles declarative row properties together with keys and labels. Each unsigned
	 * 16-bit mask selects the controlled state bits; the corresponding value supplies
	 * those bits. Unmasked state stays native and is preserved across keyed moves.
	 * Implementations without an atomic property bridge fail explicitly.
	 */
	default void reconcileProps(String[] keys, String[] labels, short[] masks, short[] values) {
		throw new UnsupportedOperationException("controlled collection properties are not supported");
	}

	/** Returns a stable row key, or null when this implementation has no keyed snapshot. */
	default String stableKey(int item) {
		return null;
	}

	/**
	 * Enables an opt-in, zero-basis weighted row layout for gallery tiles. Each
	 * unsigned short is a positive grow weight, associated with its current item
	 * identity; each visible row distributes the available width proportionally.
	 * Passing null restores the original equal-width gallery layout. The array
	 * must match {@link #count()} and is never retained by reference. This is a
	 * subset of Flexbox grow, not a complete CSS layout engine.
	 */
	default void setGalleryFlexWeights(short[] weights) {
		throw new UnsupportedOperationException("gallery flex weights are not supported");
	}

	/** Returns the unsigned grow weight, or one in the original equal-width mode. */
	default int galleryFlexWeight(int item) {
		throw new UnsupportedOperationException("gallery flex weights are not supported");
	}

	/** Replaces the rows: {@code ;}-separated labels; boards and messages take {@code column~label} / {@code side~text}. */
	void load(String sample);

	/** Rows before the end at which an infinite list asks for more (one D_LOAD delta per item count). */
	void setThreshold(int items);

	/** True after the load threshold fired for the current item count. */
	boolean needsMore();

	/** Current overscroll pull in pixels (pull to refresh). */
	int pull();

	/** Ends a refresh: clears LOADING and the pull. */
	void endRefresh();

	/** Moves an item to another index (reorder). */
	void move(int from, int to);

	/** Moves a board card to another column. */
	void moveToColumn(int item, int column);

	/** Sets a row's swipe offset (clamped to the action width, 0 = closed). */
	void swipe(int item, int offset);
}
