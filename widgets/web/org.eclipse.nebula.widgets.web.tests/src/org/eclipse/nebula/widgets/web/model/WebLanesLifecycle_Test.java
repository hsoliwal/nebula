// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.*;

import org.junit.Test;

/** Lane ownership regressions; no GC timing or toolkit/native assumptions. */
public class WebLanesLifecycle_Test {
    private static InfiniteScrollModel model(String... labels) {
        InfiniteScrollModel m = InfiniteScrollModel.sample();
        m.clear();
        for (String s : labels) m.add(s, s == null ? null : "detail:" + s);
        m.layout(240, 96);
        return m;
    }
    private static Object lane(WebLanes m, String name) throws Exception {
        Field f = WebLanes.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(m);
    }
    private static List<Integer> events(WebLanes m) {
        List<Integer> e = new ArrayList<>();
        m.setDelta((item, mask) -> { e.add(item); e.add(mask); });
        return e;
    }
    @Test public void removedKeysFollowSurvivingIdentity() {
        String a = new String("same"), b = new String("same"), c = new String("third");
        InfiniteScrollModel m = model(a, b, c);
        m.localize(s -> "L:" + s);
        m.remove(0);
        List<String> seen = new ArrayList<>();
        m.localize(s -> { seen.add(s); return s; });
        assertEquals(2, seen.size());
        assertSame(b, seen.get(0));
        assertSame(c, seen.get(1));
    }
    @Test public void removedTailReferencesAreReleasedImmediately() throws Exception {
        InfiniteScrollModel m = model("a", "b", "c");
        m.localize(s -> "L:" + s);
        Object texts = lane(m, "text"), details = lane(m, "detail"), keys = lane(m, "keys");
        m.remove(1);
        assertSame(texts, lane(m, "text"));
        assertSame(details, lane(m, "detail"));
        assertSame(keys, lane(m, "keys"));
        assertNull(((String[]) texts)[2]);
        assertNull(((String[]) details)[2]);
        assertNull(((String[]) keys)[2]);
    }
    @Test public void firstMiddleLastRemovalKeepsKeysAligned() {
        for (int at = 0; at < 3; at++) {
            InfiniteScrollModel m = model("a", "b", "c");
            m.localize(s -> "L:" + s);
            List<String> expected = new ArrayList<>(List.of("a", "b", "c"));
            expected.remove(at);
            m.remove(at);
            List<String> seen = new ArrayList<>();
            m.localize(s -> { seen.add(s); return s; });
            assertEquals(expected, seen);
        }
    }
    @Test public void clearReleasesReferencesWithoutReplacingBackingLanes() throws Exception {
        InfiniteScrollModel m = model("a", "b", "c");
        m.localize(s -> "L:" + s);
        String[] texts = (String[]) lane(m, "text");
        String[] details = (String[]) lane(m, "detail");
        String[] keys = (String[]) lane(m, "keys");
        int[] flags = (int[]) lane(m, "flags");
        m.clear();
        assertSame(texts, lane(m, "text"));
        assertSame(details, lane(m, "detail"));
        assertSame(flags, lane(m, "flags"));
        for (int i = 0; i < 3; i++) { assertNull(texts[i]); assertNull(details[i]); assertNull(keys[i]); }
        assertNull(lane(m, "keys"));
    }
    @Test public void appendAfterLocalizationSeedsSourceKey() {
        InfiniteScrollModel m = model("a");
        m.localize(s -> "L:" + s);
        String added = new String("new");
        m.add(added);
        List<String> seen = new ArrayList<>();
        m.localize(s -> { seen.add(s); return s; });
        assertEquals(2, seen.size());
        assertSame(added, seen.get(1));
    }
    @Test public void assignedTextRebasesSourceKey() {
        InfiniteScrollModel m = model("a");
        m.localize(s -> "L:" + s);
        String replacement = new String("replacement");
        m.setText(0, replacement);
        List<String> seen = new ArrayList<>();
        m.localize(s -> { seen.add(s); return s; });
        assertSame(replacement, seen.get(0));
    }
    @Test public void activeKeysSurviveGrowthWithoutRetargeting() throws Exception {
        InfiniteScrollModel m = model("a");
        m.localize(s -> "L:" + s);
        int capacity = ((int[]) lane(m, "flags")).length;
        for (int i = 1; i <= capacity; i++) m.add("n" + i);
        List<String> seen = new ArrayList<>();
        m.localize(s -> { seen.add(s); return s; });
        assertEquals(capacity + 1, seen.size());
        assertEquals("a", seen.get(0));
        assertEquals("n" + capacity, seen.get(capacity));
    }
    @Test public void nullKeysBypassNullTranslator() {
        InfiniteScrollModel m = model((String) null);
        m.localize(null);
        assertNull(m.text(0));
        m.clear();
        m.localize(null);
    }
    @Test public void translatorFailureKeepsPartialMutationWithoutCallback() {
        InfiniteScrollModel m = model("a", null, "b", "c");
        m.localize(s -> "old:" + s);
        List<Integer> e = events(m);
        int generation = m.generation();
        RuntimeException problem = new RuntimeException("translator");
        List<String> seen = new ArrayList<>();
        try {
            m.localize(s -> { seen.add(s); if (s.equals("b")) throw problem; return "new:" + s; });
            fail("must throw");
        } catch (RuntimeException ex) { assertSame(problem, ex); }
        assertEquals(List.of("a", "b"), seen);
        assertEquals("new:a", m.text(0));
        assertNull(m.text(1));
        assertEquals("old:b", m.text(2));
        assertEquals("old:c", m.text(3));
        assertTrue(e.isEmpty());
        assertEquals(generation, m.generation());
    }
    @Test public void invalidRemoveKeepsExistingViewFilterResetOrder() {
        InfiniteScrollModel m = model("a", "b");
        m.setFilter("b");
        List<Integer> e = events(m);
        int generation = m.generation();
        try { m.remove(-1); fail("invalid"); }
        catch (IndexOutOfBoundsException ex) { assertEquals("item -1 of 2", ex.getMessage()); }
        assertEquals("", m.filter());
        assertEquals(2, m.logicalCount());
        assertEquals(2, m.count());
        assertTrue(e.isEmpty());
        assertEquals(generation, m.generation());
    }
    @Test public void successfulRemovePreservesInteractionCompatibility() throws Exception {
        InfiniteScrollModel m = model("a", "b", "c");
        for (String name : List.of("focus", "hover", "pressed")) {
            Field f = WebLanes.class.getDeclaredField(name);
            f.setAccessible(true); f.setInt(m, 2);
        }
        List<Integer> e = events(m);
        m.remove(0);
        assertEquals(1, ((Integer) lane(m, "focus")).intValue());
        assertEquals(2, ((Integer) lane(m, "hover")).intValue());
        assertEquals(2, ((Integer) lane(m, "pressed")).intValue());
        assertEquals(List.of(0, 64), e);
    }
    @Test public void callbacksObserveFinalizedRemovalAndClear() throws Exception {
        InfiniteScrollModel m = model("a", "b", "c");
        m.localize(s -> "L:" + s);
        List<Integer> e = new ArrayList<>();
        m.setDelta((item, mask) -> {
            e.add(mask);
            try {
                assertNull(((String[]) lane(m, "text"))[m.count()]);
                assertNull(((String[]) lane(m, "detail"))[m.count()]);
            } catch (Exception ex) { throw new AssertionError(ex); }
        });
        m.remove(1);
        m.clear();
        assertEquals(List.of(64, 64), e);
    }
}
