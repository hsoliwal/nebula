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
package org.eclipse.nebula.widgets.grid.model;

import com.google.gwt.core.client.EntryPoint;

/** GWT entry: prints the workload digests (one console line each). */
public final class GateEntry implements EntryPoint {
	@Override
	public void onModuleLoad() {
		for (final String line : GateWorkload.run()) {
			log(line);
		}
	}

	private static native void log(String line) /*-{
		$wnd.console.log(line);
	}-*/;
}
