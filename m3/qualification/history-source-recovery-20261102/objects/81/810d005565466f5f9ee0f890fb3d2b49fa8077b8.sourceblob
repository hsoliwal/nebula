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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * JVM side of the gate: writes the same workload digests, one line each, to the file named by the
 * first argument (no console output, brief rule 22; {@code run_gate.py} reads the file).
 */
public final class GateJava {
	public static void main(final String[] args) throws IOException {
		Files.write(Paths.get(args[0]), GateWorkload.run(), StandardCharsets.UTF_8);
	}
}
