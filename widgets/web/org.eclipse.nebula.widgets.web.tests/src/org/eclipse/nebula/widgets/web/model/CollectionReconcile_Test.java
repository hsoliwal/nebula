// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Native CollectionModel proof for keyed declarative snapshots from a React AST bridge. */
public class CollectionReconcile_Test {
    private static CollectionModel model() {
        CollectionModel model = new CollectionModel("message-list", 0,
                CollectionModel.MESSAGES, WebStates.ROUND, WebStates.V_DEFAULT);
        model.layout(320, 160);
        return model;
    }

    @Test
    public void keyedReorderPreservesPrimitiveStateAndPublishesOneDelta() {
        CollectionModel m = model();
        m.reconcile(new String[] {"a", "b", "c"}, new String[] {"Alpha", "Beta", "Gamma"});
        m.setValue(1, 17);
        m.setAux(1, -12);
        m.setGroup(1, 1);
        m.setState(1, WebStates.SELECTED, true);
        m.setFocus(1);
        List<Integer> deltas = new ArrayList<>();
        m.setDelta((item, mask) -> { deltas.add(item); deltas.add(mask); });
        int before = m.generation();
        m.reconcile(new String[] {"c", "a", "b", "d"},
                new String[] {"Gamma", "Alpha2", "Beta2", "Delta"});
        assertEquals(4, m.count());
        assertEquals("b", m.stableKey(2));
        assertEquals("Beta2", m.text(2));
        assertEquals(17, m.value(2));
        assertEquals(-12, m.aux(2));
        assertEquals(1, m.group(2));
        assertTrue(m.is(2, WebStates.SELECTED));
        assertEquals(2, m.focus());
        assertEquals(0, m.value(3));
        assertFalse(m.is(3, WebStates.SELECTED));
        assertEquals(before + 1, m.generation());
        assertEquals(List.of(-1, WebLanes.D_ITEMS), deltas);
        m.reconcile(new String[] {"c", "a", "b", "d"},
                new String[] {"Gamma", "Alpha2", "Beta2", "Delta"});
        assertEquals(before + 1, m.generation());
    }

    @Test
    public void validationIsAtomicEvenForHashCollisionAndCallerArrayMutation() {
        CollectionModel m = model();
        String[] keys = {"FB", "Ea"};
        m.reconcile(keys, new String[] {"One", "Two"});
        keys[0] = "mutated";
        assertEquals("FB", m.stableKey(0));
        int[] before = m.snapshot();
        int gen = m.generation();
        expectInvalid(() -> m.reconcile(new String[] {"a", "a"}, new String[] {"1", "2"}));
        expectInvalid(() -> m.reconcile(new String[] {null}, new String[] {"x"}));
        expectInvalid(() -> m.reconcile(new String[] {"a", "b"}, new String[] {"x"}));
        expectInvalid(() -> m.reconcile(null, new String[0]));
        assertEquals(gen, m.generation());
        assertEquals("FB", m.stableKey(0));
        assertEquals("Ea", m.stableKey(1));
        int[] after = m.snapshot();
        assertEquals(before.length, after.length);
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i], after[i]);
        }
    }

    @Test
    public void filterAndLocalizationUseTheCurrentSnapshot() {
        CollectionModel m = model();
        m.reconcile(new String[] {"x", "y", "z"},
                new String[] {"keep old", "other", "keep two"});
        m.setFilter("keep");
        assertEquals(2, m.logicalCount());
        m.reconcile(new String[] {"z", "y", "x"},
                new String[] {"keep third", "keep next", "gone"});
        assertEquals("keep", m.filter());
        assertEquals(2, m.logicalCount());
        assertEquals(0, m.itemAt(0));
        assertEquals(1, m.itemAt(1));
        m.localize(String::toUpperCase);
        assertEquals("KEEP NEXT", m.text(1));
        m.reconcile(new String[] {"y"}, new String[] {"new source"});
        m.localize(String::toUpperCase);
        assertEquals("NEW SOURCE", m.text(0));
    }

    @Test
    public void directImperativeMutationDiscardsStaleReactKeys() {
        CollectionModel m = model();
        m.reconcile(new String[] {"a", "b"}, new String[] {"A", "B"});
        m.add("C");
        assertEquals(null, m.stableKey(0));
        m.reconcile(new String[] {"b", "c"}, new String[] {"B2", "C2"});
        m.remove(0);
        assertEquals(null, m.stableKey(0));
        m.reconcile(new String[] {"x"}, new String[] {"X"});
        m.setLogicalCount(2);
        assertEquals(null, m.stableKey(0));
    }

    @Test
    public void longKeyedListKeepsResidentPaintBoundedAfterReorder() {
        MasonryModel m = new MasonryModel();
        int n = 100_000;
        String[] keys = new String[n], names = new String[n];
        for (int i = 0; i < n; i++) {
            keys[i] = Integer.toString(i);
            names[i] = "tile-" + i;
        }
        m.reconcile(keys, names);
        m.layout(896, 192);
        m.scrollTo(900_000L);
        m.setValue(0, 256);
        m.reconcile(keys, names);
        assertTrue(m.residentCount() > 0);
        assertTrue(m.residentCount() < 100);
        WebPaint p = new WebPaint();
        m.paint(p);
        assertTrue(p.count() <= m.residentCount() * 8 + 8);
    }

    private static void expectInvalid(Runnable call) {
        try {
            call.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("invalid keyed snapshot was accepted");
    }
}
