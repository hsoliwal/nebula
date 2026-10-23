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
package org.eclipse.nebula.widgets.m3;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Creates and releases the pages of an {@link M3StackLayout} in page mode. A page is created the first time it is
 * shown (and again after it was released) and released when the layout's residency budget is exceeded.
 *
 * @see M3StackLayout#setPages(Composite, int, M3PageProvider)
 */
@FunctionalInterface
public interface M3PageProvider {

	/**
	 * Creates the control of a page. The control must be a direct child of <code>parent</code>.
	 *
	 * @param parent the composite the layout is set on
	 * @param index the page index, <code>0 &lt;= index &lt; pageCount</code>
	 * @return the page control, never <code>null</code>
	 */
	Control createPage(Composite parent, int index);

	/**
	 * Releases a page that is no longer resident. The default disposes the control; override it to save the page's
	 * state first, or to keep the control elsewhere. After this call the layout no longer refers to the control.
	 *
	 * @param index the page index
	 * @param page the page control (not disposed)
	 */
	default void releasePage(int index, Control page) {
		page.dispose();
	}
}
