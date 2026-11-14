/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.internal.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridViewport.RowSink;
import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridViewport.Window;
import org.junit.Test;

/**
 * The native slots of {@link MobileGridViewport} stay bounded: overscan, a stable view, single-row updates, a long
 * index jump, shrinking and closing. Same checks as the former isolated main() harness of the mobile preview.
 */
public class MobileGridViewport_Test {

	private static final class Sink implements RowSink {
		final Map<Integer, Long> slots = new HashMap<>();
		int binds;

		@Override
		public void bind(int slot, long row, String[] cells, boolean selected) {
			assertEquals("wrong cell", "row-" + row, cells[0]);
			slots.put(Integer.valueOf(slot), Long.valueOf(row));
			binds++;
		}

		@Override
		public void release(int slot, long oldRow) {
			assertTrue("stale slot", slots.remove(Integer.valueOf(slot), Long.valueOf(oldRow)));
		}
	}

	@Test
	public void nativeSlotsStayBounded() {
		Sink sink = new Sink();
		long huge = 1_000_000_000_000L;
		MobileGridViewport viewport = new MobileGridViewport(huge, 3, 20, 2,
				(row, col) -> col == 0 ? "row-" + row : "cell-" + col, sink);
		Window first = viewport.show(0, 80);
		assertTrue("initial 4 visible", first.visibleStart() == 0 && first.visibleEnd() == 4);
		assertTrue("bounded overscan", sink.slots.size() <= 6 && sink.binds == 6);
		viewport.show(0, 80);
		assertEquals("stable view does not fetch model twice", 6, sink.binds);
		viewport.select(2);
		viewport.show(0, 80);
		assertEquals("selection updates one retained row", 7, sink.binds);
		viewport.invalidate(3);
		viewport.show(0, 80);
		assertEquals("single row invalidation", 8, sink.binds);
		long jump = huge - 4;
		Window far = viewport.show(jump * 20L, 80);
		assertTrue("long index jump", far.visibleStart() == jump && far.visibleEnd() == huge);
		assertTrue("bounded far viewport", sink.slots.size() <= 8);
		viewport.setItemCount(10);
		viewport.show(0, 80);
		assertTrue("shrink releases out of range", sink.slots.values().stream().allMatch(r -> r.longValue() < 10));
		viewport.setItemCount(0);
		viewport.show(0, 80);
		assertTrue("zero row cleanup", sink.slots.isEmpty());
		viewport.close();
		assertTrue("close cleanup", sink.slots.isEmpty());
	}
}
