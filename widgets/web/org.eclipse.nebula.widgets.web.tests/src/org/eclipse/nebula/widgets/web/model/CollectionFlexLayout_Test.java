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

	@Test
	public void weightedWarmScrollRetiresThePreviousResidentWindow() {
		CollectionModel model = weightedGallery(false);
		assertTrue(model.itemRect(0, 2) > 0);
		model.scrollTo(1120);
		assertTrue(model.window(2) > 0);
		assertEquals("retired width", 0, model.itemRect(0, 2));
		assertEquals("retired height", 0, model.itemRect(0, 3));
		int distant = model.itemAt(model.window(0));
		assertTrue(model.itemRect(distant, 2) > 0);
		model.scrollTo(0);
		assertRect(model, 0, 0, 50);
		assertEquals("distant width after return", 0, model.itemRect(distant, 2));
	}

	@Test
	public void weightedRtlWarmScrollMatchesCompleteLayoutAndPointerGeometry() {
		CollectionModel model = weightedGallery(false), reference = weightedGallery(true);
		model.setRtl(true);
		reference.setRtl(true);
		for (long offset : new long[] {1, 112, 1120, Long.MAX_VALUE, 337, 0}) {
			model.scrollTo(offset);
			reference.scrollTo(offset);
			assertEquivalent(reference, model, "RTL scroll " + offset);
		}
	}

	@Test
	public void weightedResidencySurvivesFilteringWeightChangesAndRegrowth() {
		CollectionModel model = weightedGallery(false), reference = weightedGallery(true);
		for (boolean rtl : new boolean[] {false, true, false}) {
			model.setRtl(rtl);
			reference.setRtl(rtl);
			for (String filter : new String[] {"keep", "", "missing", ""}) {
				model.setFilter(filter);
				reference.setFilter(filter);
				for (long offset : new long[] {1120, 1121, Long.MAX_VALUE, 0}) {
					model.scrollTo(offset);
					reference.scrollTo(offset);
					assertEquivalent(reference, model, "filtered scroll " + filter + "," + rtl + "," + offset);
				}
			}
			model.setGalleryFlexWeights(null);
			reference.setGalleryFlexWeights(null);
			model.scrollTo(1120);
			reference.scrollTo(1120);
			assertEquivalent(reference, model, "ordinary rows " + rtl);
			short[] weights = weights(model.count());
			model.setGalleryFlexWeights(weights);
			reference.setGalleryFlexWeights(weights);
			model.scrollTo(0);
			reference.scrollTo(0);
			assertEquivalent(reference, model, "restored weights " + rtl);
		}
		for (int count : new int[] {3, 0, 95}) {
			model.setLogicalCount(count);
			reference.setLogicalCount(count);
			model.scrollTo(Long.MAX_VALUE);
			reference.scrollTo(Long.MAX_VALUE);
			assertEquivalent(reference, model, "logical count " + count);
		}
	}

	@Test
	public void invalidRemovalRetainsInheritedFilterResetWithoutChangingWeights() {
		CollectionModel model = weightedGallery(false);
		for (int item : new int[] {-1, model.count(), Integer.MAX_VALUE}) {
			model.setFilter("keep");
			int[] before = model.snapshot();
			int generation = model.generation();
			List<Integer> deltas = new ArrayList<>();
			model.setDelta((changed, mask) -> {
				deltas.add(Integer.valueOf(changed));
				deltas.add(Integer.valueOf(mask));
			});
			try {
				model.remove(item);
				throw new AssertionError("invalid removal accepted");
			} catch (IndexOutOfBoundsException expected) {
				assertEquals("item " + item + " of 95", expected.getMessage());
			}
			assertEquals("", model.filter());
			assertEquals(95, model.logicalCount());
			assertArrayEquals(before, model.snapshot());
			assertEquals(generation, model.generation());
			assertTrue(deltas.isEmpty());
			for (int i = 0; i < model.count(); i++) {
				assertEquals("retained weight " + i, weights(3)[i % 3], model.galleryFlexWeight(i));
				assertEquals("key-" + i, model.stableKey(i));
			}
		}
	}

	/** Unrecognized subclasses retain the inherited complete layout and hit-test path. */
	private static final class FullLayoutGallery extends CollectionModel {
		FullLayoutGallery() {
			super("gallery", 0, GALLERY, WebStates.ROUND, WebStates.V_DEFAULT);
		}
	}

	private static CollectionModel weightedGallery(boolean reference) {
		CollectionModel model = reference ? new FullLayoutGallery() : gallery(401);
		String[] keys = new String[95], labels = new String[95];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = "key-" + i;
			labels[i] = (i % 3 == 0 ? "keep " : "other ") + i;
		}
		model.reconcile(keys, labels);
		model.layout(401, 240);
		model.setGalleryFlexWeights(weights(keys.length));
		return model;
	}

	private static short[] weights(int count) {
		short[] result = new short[count];
		for (int i = 0; i < count; i++) {
			result[i] = (short) (i % 3 == 0 ? 1 : i % 3 == 1 ? 2 : 5);
		}
		return result;
	}

	private static void assertEquivalent(CollectionModel reference, CollectionModel model, String where) {
		assertArrayEquals(where + " primitive lanes", reference.snapshot(), model.snapshot());
		WebPaint expected = new WebPaint(), actual = new WebPaint();
		reference.paint(expected);
		model.paint(actual);
		assertEquals(where + " paint count", expected.count(), actual.count());
		assertEquals(where + " paint hash", expected.hash(), actual.hash());
		for (int y : new int[] {-1, 0, 1, 55, 111, 112, 239, 240}) {
			for (int x : new int[] {-1, 0, 1, 49, 50, 100, 150, 200, 350, 400, 401}) {
				assertEquals(where + " pointer " + x + "," + y,
						reference.hitTest(x, y), model.hitTest(x, y));
			}
		}
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
