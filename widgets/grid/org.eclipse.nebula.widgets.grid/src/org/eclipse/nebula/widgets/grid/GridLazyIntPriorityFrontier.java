/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.NoSuchElementException;

/**
 * Primitive max-priority frontier for Grid-local ordered work.
 *
 * <p>The frontier heapifies a defensive copy in linear time. {@link #peek()}
 * observes the current highest priority without removal; {@link #poll()}
 * removes only that top entry and repairs the frontier in logarithmic time.
 * It deliberately does not promise a fully sorted retained representation.</p>
 */
final class GridLazyIntPriorityFrontier {

	private final int[] heap;
	private int size;

	private GridLazyIntPriorityFrontier(int[] values) {
		heap = values.clone();
		size = heap.length;
		for (int parent = (size >>> 1) - 1; parent >= 0; parent--) {
			siftDown(parent);
		}
	}

	static GridLazyIntPriorityFrontier maxHeap(int[] values) {
		if (values == null) {
			throw new NullPointerException("values");
		}
		return new GridLazyIntPriorityFrontier(values);
	}

	int size() {
		return size;
	}

	boolean isEmpty() {
		return size == 0;
	}

	int peek() {
		if (size == 0) {
			throw new NoSuchElementException();
		}
		return heap[0];
	}

	int poll() {
		if (size == 0) {
			throw new NoSuchElementException();
		}
		int result = heap[0];
		int last = --size;
		if (last > 0) {
			heap[0] = heap[last];
			siftDown(0);
		}
		heap[last] = 0;
		return result;
	}

	private void siftDown(int parent) {
		int at = parent;
		while (true) {
			int left = (at << 1) + 1;
			if (left >= size) {
				return;
			}
			int right = left + 1;
			int child = right < size && heap[right] > heap[left] ? right : left;
			if (heap[at] >= heap[child]) {
				return;
			}
			int value = heap[at];
			heap[at] = heap[child];
			heap[child] = value;
			at = child;
		}
	}
}
