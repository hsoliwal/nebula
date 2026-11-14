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
// Generated fromtemplates/family/COMMANDS.api.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

/** API of the grouped command buttons (pattern COMMANDS-1). */
public interface CommandsApi extends WebApi {
	/** Replaces the commands: {@code ;}-separated {@code group~label[~L]} ({@code L} = large), or plain labels. */
	void load(String sample);

	/** Ribbon group captions, by group id. */
	void setCaptions(String... names);

	/** Kind of a command: PUSH, TOGGLE, SPLIT_KIND or DROPDOWN of {@link CommandsModel}. */
	void setKind(int item, int kind);

	boolean isMenuOpen();

	void setMenuOpen(boolean open);
}
