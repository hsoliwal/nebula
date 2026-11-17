// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Native, property-controlled row updates without per-row SWT controls. */
public class CollectionControlledProps_Test {
	private static CollectionModel model() {
		CollectionModel model = new CollectionModel("message-list", 0,
				CollectionModel.MESSAGES, WebStates.ROUND, WebStates.V_DEFAULT);
		model.layout(320, 160);
		return model;
	}

	@Test
	public void keyedCommitPreservesNativeEventStateAndUpdatesControlledBits() {
		CollectionModel m = model();
		m.reconcile(new String[] {"a", "b"}, new String[] {"Alpha", "Beta"});
		m.setState(1, WebStates.HOVER | WebStates.PRESSED, true);
		m.setValue(1, 42);
		m.setFocus(1);
		List<Integer> deltas = new ArrayList<>();
		m.setDelta((item, mask) -> { deltas.add(item); deltas.add(mask); });
		int before = m.generation();
		m.reconcileProps(new String[] {"b", "a", "c"},
				new String[] {"Beta", "Alpha", "Gamma"},
				new short[] {(short) (WebStates.DISABLED | WebStates.SELECTED),
						(short) WebStates.HIDDEN, (short) WebStates.READONLY},
				new short[] {(short) WebStates.SELECTED,
						(short) WebStates.HIDDEN, (short) WebStates.READONLY});
		assertEquals(3, m.count());
		assertEquals("b", m.stableKey(0));
		assertEquals(42, m.value(0));
		assertEquals(0, m.focus());
		assertTrue(m.is(0, WebStates.HOVER));
		assertTrue(m.is(0, WebStates.PRESSED));
		assertTrue(m.is(0, WebStates.SELECTED));
		assertFalse(m.is(0, WebStates.DISABLED));
		assertTrue(m.is(1, WebStates.HIDDEN));
		assertTrue(m.is(2, WebStates.READONLY));
		assertEquals(before + 1, m.generation());
		assertEquals(List.of(-1, WebLanes.D_ITEMS), deltas);
	}

	@Test
	public void invalidMasksAndKeysFailBeforeAnyMutation() {
		CollectionModel m = model();
		m.reconcile(new String[] {"x", "y"}, new String[] {"X", "Y"});
		int[] baseline = m.snapshot();
		int before = m.generation();
		expectInvalid(() -> m.reconcileProps(new String[] {"x"}, new String[] {"X"},
				new short[] {(short) WebStates.HOVER}, new short[] {0}));
		expectInvalid(() -> m.reconcileProps(new String[] {"x"}, new String[] {"X"},
				new short[] {0}, new short[] {(short) WebStates.DISABLED}));
		expectInvalid(() -> m.reconcileProps(new String[] {"x", "x"},
				new String[] {"X", "X"}, new short[2], new short[2]));
		expectInvalid(() -> m.reconcileProps(new String[] {"x"}, new String[] {"X"},
				new short[0], new short[1]));
		expectInvalid(() -> m.reconcileProps(new String[] {"x"}, new String[] {"X"},
				null, new short[1]));
		assertArrayEquals(baseline, m.snapshot());
		assertEquals(before, m.generation());
		assertEquals("x", m.stableKey(0));
	}

	@Test
	public void identicalCommitIsNoOpButControlledPropsCanReassertState() {
		CollectionModel m = model();
		String[] keys = {"k"}, labels = {"text"};
		short[] masks = {(short) (WebStates.CHECKED | WebStates.DISABLED)};
		short[] values = {(short) WebStates.CHECKED};
		m.reconcileProps(keys, labels, masks, values);
		keys[0] = "tampered";
		masks[0] = 0;
		values[0] = 0;
		int before = m.generation();
		m.reconcileProps(new String[] {"k"}, labels,
				new short[] {(short) (WebStates.CHECKED | WebStates.DISABLED)},
				new short[] {(short) WebStates.CHECKED});
		assertEquals(before, m.generation());
		assertEquals("k", m.stableKey(0));
		m.setState(0, WebStates.DISABLED, true);
		int changed = m.generation();
		m.reconcileProps(new String[] {"k"}, labels,
				new short[] {(short) (WebStates.CHECKED | WebStates.DISABLED)},
				new short[] {(short) WebStates.CHECKED});
		assertEquals(changed + 1, m.generation());
		assertFalse(m.is(0, WebStates.DISABLED));
		assertTrue(m.is(0, WebStates.CHECKED));
	}

	@Test
	public void hundredThousandRowsKeepTheMasonryPaintWindowBounded() {
		MasonryModel m = new MasonryModel();
		int n = 100_000;
		String[] keys = new String[n], labels = new String[n];
		short[] masks = new short[n], values = new short[n];
		for (int i = 0; i < n; i++) {
			keys[i] = Integer.toString(i);
			labels[i] = "tile" + i;
			masks[i] = (short) WebStates.HIDDEN;
			values[i] = i % 101 == 0 ? (short) WebStates.HIDDEN : 0;
		}
		m.reconcileProps(keys, labels, masks, values);
		m.layout(896, 192);
		m.scrollTo(900_000L);
		assertTrue(m.residentCount() > 0);
		assertTrue(m.residentCount() < 100);
		WebPaint paint = new WebPaint();
		m.paint(paint);
		assertTrue(paint.count() <= m.residentCount() * 8 + 8);
	}

	private static void expectInvalid(Runnable action) {
		try {
			action.run();
		} catch (IllegalArgumentException expected) {
			return;
		}
		throw new AssertionError("invalid controlled props were accepted");
	}
}
