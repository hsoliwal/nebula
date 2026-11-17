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
 ******************************************************************************/
package org.eclipse.nebula.widgets.web.model;

/** A responsive, variable-height tile collection backed by primitive viewport lanes. */
public interface MasonryApi extends CollectionApi {
	String CONCEPT = "masonry";
	String SAMPLE = "Tall card;Short card;Feature tile;Another card;Photo;Text tile";

	/**
	 * Requested tile height in pixels. Values at or below zero use the current
	 * item extent; tall values are capped at four times that extent.
	 */
	default int tileHeight(int item) {
		return value(item);
	}

	/** Changes one tile's height and updates its position through model damage. */
	default void setTileHeight(int item, int pixels) {
		setValue(item, pixels);
	}
}
