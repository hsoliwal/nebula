/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.m3.tt;

import org.eclipse.swt.widgets.Shell;

/**
 * Deterministic widget fixture for the pre/post differential API probe. One
 * implementation per widget family. Loaded in the same class loader as the
 * widget under test, so it is compiled against the public API only.
 */
public interface TtFixture {

	/** Builds the widget and its sample content inside {@code shell}. */
	void build(Shell shell);

	/** Returns a sample instance usable as receiver or argument of {@code type}, or null. */
	Object instanceOf(Class<?> type);

	/** Returns a loader-independent identity for a widget/item owned by the fixture, or null. */
	String idOf(Object object);

	/** The widget whose public state is snapshotted after every probed call. */
	Object primary();

	/**
	 * Returns a reason when the member must not be invoked (for example it
	 * opens a native modal dialog that a timer cannot close), else null.
	 */
	default String skipReason(java.lang.reflect.Member member) {
		return null;
	}
}
