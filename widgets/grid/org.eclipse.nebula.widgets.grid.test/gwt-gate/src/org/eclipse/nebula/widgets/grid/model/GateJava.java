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

/** JVM side of the gate: prints the same workload digests. */
public final class GateJava {
	public static void main(final String[] args) {
		for (final String line : GateWorkload.run()) {
			System.out.println(line);
		}
	}
}
