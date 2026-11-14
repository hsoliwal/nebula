// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.*;

import org.junit.Test;

/** Logical resizing preserves identity, lifetime, viewport geometry and publication order. */
public class WebLogicalCount_Test {
    private static InfiniteScrollModel model() {
        InfiniteScrollModel m = InfiniteScrollModel.sample();
        m.clear();
        for (int i = 0; i < 12; i++) {
            int id = m.add(new String("row" + i), new String("detail" + i));
            m.setValue(id, i + 10); m.setAux(id, i + 20); m.setGroup(id, i % 3);
            m.setState(id, WebStates.SELECTED, true);
        }
        m.layout(240, 96);
        return m;
    }
    private static String[] keys(WebLanes m) throws Exception {
        Field f = WebLanes.class.getDeclaredField("keys"); f.setAccessible(true);
        return (String[]) f.get(m);
    }
    private static List<Integer> events(WebLanes m) {
        List<Integer> e = new ArrayList<>(); m.setDelta((item, mask) -> { e.add(item); e.add(mask); }); return e;
    }
    @Test public void shrinkPreservesSurvivorsAndPublishesAfterGeometry() throws Exception {
        InfiniteScrollModel m = model();
        String[] survivors = Arrays.copyOf(m.text, 3);
        m.localize(s -> "L:" + s);
        m.setFilter("row1"); assertEquals(10, m.itemAt(1));
        m.scrollTo(Long.MAX_VALUE);
        m.focus = m.hover = m.pressed = 11;
        int gen = m.generation(); List<Integer> e = new ArrayList<>();
        m.setDelta((item, mask) -> {
            e.add(item); e.add(mask);
            assertEquals(3, m.count()); assertEquals(3, m.logicalCount()); assertEquals(1, m.itemAt(1));
            assertEquals("", m.filter()); assertEquals(2, m.focus); assertEquals(-1, m.hover); assertEquals(-1, m.pressed);
            assertEquals(Math.max(0, m.logicalExtent() - m.viewLength()), m.scrollOffset());
            assertTrue(m.window(0) <= m.window(1)); assertTrue(m.window(1) <= 3);
            assertTrue(m.itemRect(0, 2) > 0); assertTrue(m.itemRect(0, 3) > 0);
        });
        m.setLogicalCount(3);
        assertEquals(List.of(-1, 64), e); assertEquals(gen + 1, m.generation());
        assertEquals(10, m.value(0)); assertEquals(22, m.aux(2)); assertEquals(2, m.group(2));
        List<String> seen = new ArrayList<>(); m.setDelta(null);
        m.localize(s -> { seen.add(s); return s; });
        for (int i = 0; i < 3; i++) assertSame(survivors[i], seen.get(i));
    }
    @Test public void sameCountInvalidatesFilterButPreservesPayloadAndValidState() {
        InfiniteScrollModel m = model();
        m.setLogicalCount(3);
        m.focus = 1; m.hover = 2; m.pressed = 0;
        String text = m.text(1), detail = m.detail(1);
        m.setFilter("row2"); List<Integer> e = events(m); int gen = m.generation();
        m.setLogicalCount(3);
        assertEquals(List.of(-1, 64), e); assertEquals(gen + 1, m.generation());
        assertEquals("", m.filter()); assertEquals(3, m.logicalCount());
        assertSame(text, m.text(1)); assertSame(detail, m.detail(1));
        assertEquals(11, m.value(1)); assertTrue(m.is(1, WebStates.SELECTED));
        assertEquals(1, m.focus); assertEquals(2, m.hover); assertEquals(0, m.pressed);
    }
    @Test public void retiredReferencesAreReleasedBeforeRegrowth() throws Exception {
        InfiniteScrollModel m = model(); m.localize(s -> "L:" + s);
        String[] text = m.text, detail = m.detail, key = keys(m); int[] flags = m.flags;
        m.setLogicalCount(3);
        assertSame(text, m.text); assertSame(detail, m.detail); assertSame(key, keys(m)); assertSame(flags, m.flags);
        for (int i = 3; i < 12; i++) { assertNull(text[i]); assertNull(detail[i]); assertNull(key[i]); }
        m.setLogicalCount(0);
        for (int i = 0; i < 12; i++) { assertNull(text[i]); assertNull(detail[i]); assertNull(key[i]); }
        assertEquals(-1, m.focus); assertEquals(-1, m.hover); assertEquals(-1, m.pressed); assertEquals(0L, m.scrollOffset());
    }
    @Test public void shrinkZeroRegrowDoesNotResurrectAnyItemLane() throws Exception {
        InfiniteScrollModel m = model(); m.localize(s -> "L:" + s);
        m.setLogicalCount(3); m.setLogicalCount(3); m.setLogicalCount(0); m.setLogicalCount(12);
        for (int i = 0; i < 12; i++) {
            assertNull(m.text(i)); assertNull(m.detail(i)); assertNull(keys(m)[i]);
            assertEquals(0, m.flags[i]); assertEquals(0, m.value(i)); assertEquals(0, m.aux(i)); assertEquals(0, m.group(i));
        }
        List<String> seen = new ArrayList<>(); m.localize(s -> { seen.add(s); return s; }); assertTrue(seen.isEmpty());
    }
    @Test public void negativeCountPreservesFailureOrderAndCompleteState() throws Exception {
        InfiniteScrollModel m = model(); m.localize(s -> "L:" + s); m.setFilter("row1"); m.scrollTo(10);
        int[] before = m.snapshot(); String[] text = m.text.clone(), key = keys(m).clone(); int gen = m.generation();
        List<Integer> e = events(m); m.takeDamage(new int[4]);
        try { m.setLogicalCount(-1); fail("negative count"); }
        catch (IllegalArgumentException ex) { assertEquals("fromIndex(12) > toIndex(-1)", ex.getMessage()); }
        assertArrayEquals(before, m.snapshot()); assertArrayEquals(text, m.text); assertArrayEquals(key, keys(m));
        assertEquals("row1", m.filter()); assertEquals(gen, m.generation()); assertTrue(e.isEmpty());
        assertFalse(m.takeDamage(new int[4]));
    }
    @Test public void removedPressCannotActivateOrThrowOnPointerUp() {
        InfiniteScrollModel m = model(); m.pressed = 11; m.hover = 11; m.focus = 11;
        m.setLogicalCount(3); List<Integer> e = events(m);
        m.pointer(WebStates.P_UP, 10, 10, 0);
        assertEquals(-1, m.pressed); assertTrue(e.isEmpty());
    }
    @Test public void callbackFailurePropagatesAfterFinalizedState() {
        InfiniteScrollModel m = model(); RuntimeException problem = new RuntimeException("delta");
        int gen = m.generation(); List<Integer> e = new ArrayList<>();
        m.setDelta((item, mask) -> { e.add(mask); assertEquals(3, m.count()); assertTrue(m.window(1) <= 3); throw problem; });
        try { m.setLogicalCount(3); fail("delta failure"); } catch (RuntimeException ex) { assertSame(problem, ex); }
        assertEquals(3, m.count()); assertEquals(gen + 1, m.generation()); assertEquals(List.of(64), e);
    }
    @Test public void galleryClampUsesVirtualRowExtent() {
        CollectionModel m = new CollectionModel("gallery", 0, CollectionModel.GALLERY, 0, 0);
        for (int i = 0; i < 12; i++) m.add("row" + i);
        m.layout(240, 112); m.scrollTo(Long.MAX_VALUE); List<Integer> e = events(m);
        m.setLogicalCount(3);
        assertEquals(224L, m.logicalExtent()); assertEquals(112L, m.scrollOffset());
        assertEquals(List.of(-1, 64), e); assertTrue(m.window(0) <= m.window(1)); assertTrue(m.window(1) <= 3);
    }
    @Test public void millionLogicalRowsRetainBoundedResidentGeometry() {
        InfiniteScrollModel m = model(); m.setLogicalCount(1_000_000);
        m.scrollTo(Long.MAX_VALUE); assertTrue(m.residentCount() <= 8);
        m.setLogicalCount(3); assertTrue(m.residentCount() <= 3); assertEquals(24L, m.scrollOffset());
        m.setLogicalCount(0); m.setLogicalCount(1_000_000);
        assertEquals(1_000_000, m.count()); assertTrue(m.residentCount() <= 8);
        assertNull(m.text(999_999)); assertEquals(0, m.flags[999_999]);
    }
}
