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
// Generated fromtemplates/family/TOKEN.api.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

/** API of the small token renderers (pattern TOKEN-1). */
public interface TokenApi extends WebApi {
	/** Replaces the items with a sample spec: {@code ;}-separated item texts (counts for badges, glyph names for icons). */
	void load(String sample);

	/** Overflow limit of count badges: values above it paint as {@code max+}. */
	void setMax(int max);

	/** Chips show a remove glyph and Delete/Backspace removes the focused chip. */
	void setRemovable(boolean removable);

	boolean isRemovable();

	/** The text painted for an item (initials, a bounded count, or the text). */
	String display(int item);
}
