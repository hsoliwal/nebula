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
package org.eclipse.nebula.widgets.m3.rwt.examples;

/** Deterministic sample data for the examples (no randomness, so runs compare). */
public final class RwtSampleData {

	private static final String[] STEMS = {"Berlin", "Bern", "Bergen", "Boston", "Bombay", "Cairo", "Delhi", "Dublin", "Lima", "Lisbon", "London", "Madrid", "Oslo", "Paris", "Pune", "Quito", "Riga", "Rome", "Seoul", "Tokyo"};

	private RwtSampleData () {
	}

	/** {@code count} distinct words: a stem and a number. */
	public static String[] words (int count) {
		String[] words = new String[count];
		for (int i = 0; i < count; i++) words[i] = STEMS[i % STEMS.length] + " " + (i / STEMS.length + 1);
		return words;
	}

	/** Text of cell (row, column). */
	public static String cell (int row, int column) {
		return column == 0 ? STEMS[row % STEMS.length] + " " + row : Integer.toString ((row * 31 + column * 17) % 1000);
	}
}
