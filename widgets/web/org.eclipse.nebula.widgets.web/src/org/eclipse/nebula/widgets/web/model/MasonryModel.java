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

/**
 * A masonry layout in the existing COLLECTION viewport family. A single
 * {@code CollectionModel} owns the long item offsets, packed column indices,
 * row heights and stable integer item identities.
 */
public class MasonryModel extends CollectionModel implements MasonryApi {
	public MasonryModel() {
		super(CONCEPT, WebStates.role("grid"), MASONRY, WebStates.ROUND, WebStates.V_DEFAULT);
	}

	public static MasonryModel sample() {
		MasonryModel model = new MasonryModel();
		model.load(SAMPLE);
		return model;
	}
}
