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
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Batch threshold: inline below it, worker + UI-thread apply above it, stale results dropped. */
public class GridModelJobs_Test {

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void smallSortRunsInline() {
		final Grid grid = grid(1000);
		final int[] keys = keys(1000);
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeys(grid, keys, true, r -> {
			throw new AssertionError("no worker below the threshold");
		});
		assertTrue(done.isDone());
		assertTrue(done.join());
		assertArrayEquals(RowPermutation.sortedOrder(keys, 1000, true), view(grid.getRowPermutation()));
	}

	@Test
	public void largeSortRunsOnTheWorkerAndAppliesOnTheUiThread() {
		final int rows = GridModelJobs.BATCH_THRESHOLD + 7;
		final Grid grid = grid(rows);
		final int[] keys = keys(rows);
		final List<Runnable> worker = new ArrayList<>();
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeys(grid, keys, false, worker::add);
		assertEquals(1, worker.size());
		assertFalse("nothing applied before the worker ran", grid.getRowPermutation().isSorted());
		keys[0] = Integer.MAX_VALUE; // the job sorts its snapshot, not the caller's array
		final int[] expected = RowPermutation.sortedOrder(java.util.Arrays.copyOf(keys(rows), rows), rows, false);
		new Thread(worker.get(0)).start();
		while (!done.isDone()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		assertTrue(done.join());
		assertArrayEquals(expected, view(grid.getRowPermutation()));
		assertEquals(rows, grid.getItemCount());
	}

	@Test
	public void resultForAChangedPermutationIsDropped() {
		final int rows = GridModelJobs.BATCH_THRESHOLD;
		final Grid grid = grid(rows);
		final List<Runnable> worker = new ArrayList<>();
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeys(grid, keys(rows), false, worker::add);
		grid.getRowPermutation().setFilter(m -> (m & 1) == 0);
		grid.refreshRowPermutation();
		worker.get(0).run();
		while (!done.isDone()) {
			display.readAndDispatch();
		}
		assertFalse(done.join());
		assertFalse(grid.getRowPermutation().isSorted());
		assertEquals(rows / 2, grid.getItemCount());
	}

	private Grid grid(final int rows) {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL);
		new GridColumn(grid, SWT.NONE).setWidth(80);
		grid.setItemCount(rows);
		grid.setRowPermutation(new RowPermutation(rows));
		return grid;
	}

	private static int[] keys(final int rows) {
		final Random random = new Random(rows);
		final int[] keys = new int[rows];
		for (int i = 0; i < rows; i++) {
			keys[i] = random.nextInt(1000);
		}
		return keys;
	}

	private static int[] view(final RowPermutation p) {
		final int[] out = new int[p.getViewCount()];
		p.copyModelRows(0, out, 0, out.length);
		return out;
	}
}
