// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Native Flexbox-grow subset: primitive weights under the existing gallery viewport. */
public class CollectionFlexLayout_Test {
	private static CollectionModel gallery(int width) {
		CollectionModel model = new CollectionModel("gallery", 0,
				CollectionModel.GALLERY, WebStates.ROUND, WebStates.V_DEFAULT);
		model.layout(width, 240);
		return model;
	}

	@Test
	public void weightedRowsDistributeAllPixelsWithRtlAndPartialLastRow() {
		CollectionModel model = gallery(401);
		model.reconcile(new String[] {"a", "b", "c", "d", "e"},
				new String[] {"A", "B", "C", "D", "E"});
		model.setGalleryFlexWeights(new short[] {1, 2, 1, 3, 1});
		assertRect(model, 0, 0, 100);
		assertRect(model, 1, 100, 200);
		assertRect(model, 2, 300, 101);
		assertRect(model, 3, 0, 300);
		assertRect(model, 4, 300, 101);
		assertEquals(1, model.hitTest(150, 10));
		model.setRtl(true);
		assertRect(model, 0, 301, 100);
		assertRect(model, 1, 101, 200);
		assertRect(model, 2, 0, 101);
		assertRect(model, 3, 101, 300);
		assertRect(model, 4, 0, 101);
		model.setGalleryFlexWeights(null);
		assertRect(model, 0, 268, 133);
		assertEquals(1, model.galleryFlexWeight(0));
	}

	@Test
	public void stableKeysAndImperativeMovesPreserveWeightsAndNativeBits() {
		CollectionModel model = gallery(401);
		model.reconcile(new String[] {"a", "b", "c"}, new String[] {"A", "B", "C"});
		model.setGalleryFlexWeights(new short[] {1, 3, 2});
		model.setValue(1, 42);
		model.setState(1, WebStates.HOVER, true);
		model.reconcile(new String[] {"c", "b", "a", "d"},
				new String[] {"C", "B", "A", "D"});
		assertWeights(model, 2, 3, 1, 1);
		assertEquals(42, model.value(1));
		assertTrue(model.is(1, WebStates.HOVER));
		model.move(0, 3);
		assertWeights(model, 3, 1, 1, 2);
		assertEquals(null, model.stableKey(0));
		model.remove(1);
		assertWeights(model, 3, 1, 2);
		model.add("new");
		assertWeights(model, 3, 1, 2, 1);
		model.setLogicalCount(11);
		assertEquals(11, model.count());
		for (int i = 4; i < 11; i++) {
			assertEquals(1, model.galleryFlexWeight(i));
		}
		model.clear();
		model.add("reset");
		assertEquals(1, model.galleryFlexWeight(0));
	}

	@Test
	public void invalidSnapshotsRefuseAtomicallyAndIdenticalInputsAreNoOps() {
		CollectionModel model = gallery(401);
		model.reconcile(new String[] {"a", "b", "c"}, new String[] {"A", "B", "C"});
		short[] caller = {1, 2, (short) 65535};
		List<Integer> deltas = new ArrayList<>();
		model.setDelta((item, mask) -> { deltas.add(item); deltas.add(mask); });
		int generation = model.generation();
		model.setGalleryFlexWeights(caller);
		assertEquals(generation + 1, model.generation());
		assertEquals(List.of(-1, WebLanes.D_LAYOUT), deltas);
		caller[0] = 99;
		assertWeights(model, 1, 2, 65535);
		int[] immutable = model.snapshot();
		generation = model.generation();
		model.setGalleryFlexWeights(new short[] {1, 2, (short) 65535});
		assertEquals(generation, model.generation());
		expectInvalid(() -> model.setGalleryFlexWeights(new short[] {1, 0, 3}));
		expectInvalid(() -> model.setGalleryFlexWeights(new short[] {1, 2}));
		assertEquals(generation, model.generation());
		assertArrayEquals(immutable, model.snapshot());
		assertWeights(model, 1, 2, 65535);
		CollectionModel palette = new CollectionModel("palette", 0,
				CollectionModel.PALETTE, WebStates.ROUND, WebStates.V_DEFAULT);
		expectUnsupported(() -> palette.setGalleryFlexWeights(new short[0]));
		expectUnsupported(() -> palette.galleryFlexWeight(0));
	}

	@Test
	public void filterUsesWeightsOfStableItemsAndVisibleWindowOnly() {
		CollectionModel model = gallery(401);
		model.reconcile(new String[] {"a", "b", "c", "d"},
				new String[] {"omit", "keep B", "omit C", "keep D"});
		model.setGalleryFlexWeights(new short[] {1, 3, 1, 1});
		model.setFilter("keep");
		assertEquals(2, model.logicalCount());
		assertRect(model, 1, 0, 300);
		assertRect(model, 3, 300, 101);
		assertEquals(0, model.itemRect(0, 2));
		model.reconcile(new String[] {"d", "c", "b", "a"},
				new String[] {"keep D", "omit C", "keep B", "omit"});
		assertWeights(model, 1, 1, 3, 1);
		assertEquals(2, model.logicalCount());
		assertRect(model, 0, 0, 100);
		assertRect(model, 2, 100, 301);
	}

	@Test
	public void hugeUnsignedWeightsUseOverflowSafePixelProjection() {
		CollectionModel model = gallery(Integer.MAX_VALUE);
		model.setLogicalCount(70_000);
		short[] weights = new short[70_000];
		Arrays.fill(weights, (short) 65535);
		model.setGalleryFlexWeights(weights);
		assertEquals(65535, model.galleryFlexWeight(69_999));
		int expected = (int) ((long) Integer.MAX_VALUE * 69_999 / 70_000);
		assertRect(model, 69_999, expected, Integer.MAX_VALUE - expected);
	}

	private static void assertRect(CollectionModel model, int item, int x, int width) {
		assertEquals("x at item " + item, x, model.itemRect(item, 0));
		assertEquals("width at item " + item, width, model.itemRect(item, 2));
	}

	private static void assertWeights(CollectionModel model, int... weights) {
		assertEquals(weights.length, model.count());
		for (int i = 0; i < weights.length; i++) {
			assertEquals("weight at " + i, weights[i], model.galleryFlexWeight(i));
		}
	}

	private static void expectInvalid(Runnable op) {
		try {
			op.run();
		} catch (IllegalArgumentException expected) {
			return;
		}
		throw new AssertionError("invalid flex snapshot accepted");
	}

	private static void expectUnsupported(Runnable op) {
		try {
			op.run();
		} catch (UnsupportedOperationException expected) {
			return;
		}
		throw new AssertionError("non-gallery flex was accepted");
	}
}
