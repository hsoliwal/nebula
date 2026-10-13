/*
 * Copyright (C) 2002-2024 Sebastiano Vigna (fastutil, it.unimi.dsi.fastutil.ints.IntArrays)
 * Copyright (c) 2026 Contributors to the Eclipse Foundation (adaptation for Nebula Grid)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.nebula.widgets.grid.model;

/**
 * Allocation-free in-place sort of an {@code int[]} of model rows.
 *
 * <p>Port of fastutil 8.5.14 {@code IntArrays.quickSort(int[], int, int, IntComparator)}
 * (Bentley-McIlroy, "Engineering a Sort Function", 1993), source sha256
 * {@code bdc7f734c75768149310f1a4ea02bd5e6d273964390f3a6b1ebc5a09970193c1}. Changes: the
 * comparator is the Grid {@link RowComparator} extended with the model-row tie-break, so the
 * order is total and the result is unique (equal to a stable sort from the identity); the
 * larger partition is iterated instead of recursed, bounding the stack depth by log2(n).</p>
 *
 * <p>GWT-safe: int arithmetic stays far below overflow (indices are array positions).</p>
 */
final class IntPermutationSort {

	private static final int NO_REC = 16;
	private static final int MEDIAN_OF_9 = 128;

	private IntPermutationSort() {
	}

	/**
	 * Total order on model rows: {@code comp} first, then the model row itself.
	 */
	static int compare(final RowComparator comp, final int a, final int b) {
		final int c = comp.compare(a, b);
		if (c != 0) {
			return c;
		}
		return a < b ? -1 : (a == b ? 0 : 1);
	}

	/**
	 * Sorts {@code x[from, to)} by {@link #compare(RowComparator, int, int)}.
	 */
	static void sort(final int[] x, int from, int to, final RowComparator comp) {
		while (to - from >= NO_REC) {
			final int len = to - from;
			int m = from + len / 2;
			int l = from;
			int n = to - 1;
			if (len > MEDIAN_OF_9) {
				final int s = len / 8;
				l = med3(x, l, l + s, l + 2 * s, comp);
				m = med3(x, m - s, m, m + s, comp);
				n = med3(x, n - 2 * s, n - s, n, comp);
			}
			m = med3(x, l, m, n, comp);
			final int v = x[m];
			int a = from;
			int b = a;
			int c = to - 1;
			int d = c;
			while (true) {
				int comparison;
				while (b <= c && (comparison = compare(comp, x[b], v)) <= 0) {
					if (comparison == 0) {
						swap(x, a++, b);
					}
					b++;
				}
				while (c >= b && (comparison = compare(comp, x[c], v)) >= 0) {
					if (comparison == 0) {
						swap(x, c, d--);
					}
					c--;
				}
				if (b > c) {
					break;
				}
				swap(x, b++, c--);
			}
			int s = Math.min(a - from, b - a);
			swap(x, from, b - s, s);
			s = Math.min(d - c, to - d - 1);
			swap(x, b, to - s, s);
			final int left = b - a;
			final int right = d - c;
			// recurse into the smaller side, iterate on the larger one
			if (left < right) {
				if (left > 1) {
					sort(x, from, from + left, comp);
				}
				if (right <= 1) {
					return;
				}
				from = to - right;
			} else {
				if (right > 1) {
					sort(x, to - right, to, comp);
				}
				if (left <= 1) {
					return;
				}
				to = from + left;
			}
		}
		selectionSort(x, from, to, comp);
	}

	private static int med3(final int[] x, final int a, final int b, final int c, final RowComparator comp) {
		final int ab = compare(comp, x[a], x[b]);
		final int ac = compare(comp, x[a], x[c]);
		final int bc = compare(comp, x[b], x[c]);
		return ab < 0 ? (bc < 0 ? b : ac < 0 ? c : a) : (bc > 0 ? b : ac > 0 ? c : a);
	}

	private static void selectionSort(final int[] a, final int from, final int to, final RowComparator comp) {
		for (int i = from; i < to - 1; i++) {
			int m = i;
			for (int j = i + 1; j < to; j++) {
				if (compare(comp, a[j], a[m]) < 0) {
					m = j;
				}
			}
			if (m != i) {
				final int u = a[i];
				a[i] = a[m];
				a[m] = u;
			}
		}
	}

	private static void swap(final int[] x, final int a, final int b) {
		final int t = x[a];
		x[a] = x[b];
		x[b] = t;
	}

	private static void swap(final int[] x, int a, int b, final int n) {
		for (int i = 0; i < n; i++, a++, b++) {
			swap(x, a, b);
		}
	}
}
