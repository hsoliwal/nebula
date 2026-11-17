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
 *******************************************************************************/
package org.eclipse.nebula.widgets.web.model;

/** A point-series viewport with stable integer item identities and no point widgets. */
public interface SparklineApi extends WebApi {
	String CONCEPT = "sparkline";
	String SAMPLE = "3;5;2;8;6;9;4";

	/** Replaces the series with semicolon-separated signed decimal values. */
	void load(String sample);

	/** Replaces the series from a primitive array without retaining the caller's array. */
	void load(int[] values);

	/** The minimum value in the series, or zero when it is empty. */
	int minValue();

	/** The maximum value in the series, or zero when it is empty. */
	int maxValue();
}
