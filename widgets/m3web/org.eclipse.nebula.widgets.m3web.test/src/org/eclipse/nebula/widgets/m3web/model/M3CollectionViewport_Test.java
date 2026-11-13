// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.m3web.model;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

/** Row-to-item projection contracts, including index-only counts with no payload allocation. */
public class M3CollectionViewport_Test {
    private static M3CollectionModel gallery(int count, int width, int height) {
        M3CollectionModel m = new M3CollectionModel("gallery", 0, M3CollectionModel.GALLERY, 0, 0);
        m.setLogicalCount(count); m.layout(width, height); return m;
    }
    private static void bounded(M3WebView m) {
        int n = m.logicalCount();
        assertTrue("visible starts nonnegative", m.window(0) >= 0);
        assertTrue("visible is half-open", m.window(0) <= m.window(1));
        assertTrue("visible ends within count", m.window(1) <= n);
        assertTrue("resident starts nonnegative", m.window(2) >= 0);
        assertTrue("resident contains visible start", m.window(2) <= m.window(0));
        assertTrue("resident contains visible end", m.window(3) >= m.window(1));
        assertTrue("resident ends within count", m.window(3) <= n);
    }
    private static void emptyWindow(M3WebView m) {
        bounded(m);
        assertEquals(m.window(0), m.window(1));
        assertEquals(m.window(0), m.window(2));
        assertEquals(m.window(0), m.window(3));
    }
    /** Exercises only count/index geometry; does not pretend that billions of payloads were allocated. */
    private static final class IndexGallery extends M3CollectionModel {
        int logical;
        IndexGallery() { super("gallery-index", 0, GALLERY, 0, 0); }
        @Override public int logicalCount() { return logical; }
        void projectAt(int n, int w, int h, long at) {
            logical = n; width = w; height = h; scroll = at; project();
        }
    }
    @Test public void zeroHeightPartialLastRowHasEmptyBoundedWindow() {
        M3CollectionModel m = gallery(3, 240, 0); m.scrollTo(Long.MAX_VALUE);
        emptyWindow(m); assertEquals(3, m.window(0));
        assertEquals(0, m.residentCount());
        for (int i = 0; i < m.count(); i++) { assertEquals(0, m.itemRect(i, 2)); assertEquals(0, m.itemRect(i, 3)); }
    }
    @Test public void zeroHeightFilteredPartialRowUsesLogicalPositions() {
        M3CollectionModel m = gallery(5, 240, 0);
        for (int i = 0; i < 5; i++) m.setText(i, i % 2 == 0 ? "keep" : "hide");
        m.setFilter("keep"); assertEquals(3, m.logicalCount()); assertEquals(2, m.itemAt(1));
        m.scrollTo(Long.MAX_VALUE); emptyWindow(m); assertEquals(3, m.window(0));
        m.setFilter("no-match"); emptyWindow(m); assertEquals(0, m.logicalCount());
    }
    @Test public void zeroHeightRtlDoesNotInventResidentItems() {
        M3CollectionModel m = gallery(3, 240, 0); m.setRtl(true); m.scrollTo(Long.MAX_VALUE);
        emptyWindow(m);
        for (int i = 0; i < m.count(); i++) { assertEquals(0, m.itemRect(i, 2)); assertEquals(0, m.itemRect(i, 3)); }
    }
    @Test public void rowCeilingPromotesBeforeIntegerAddition() {
        IndexGallery m = new IndexGallery();
        m.projectAt(Integer.MAX_VALUE, 240, 112, 0);
        assertEquals(1_073_741_824L * 112, m.logicalExtent());
        m.projectAt(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
        int per = Integer.MAX_VALUE / 112;
        assertEquals(((Integer.MAX_VALUE + (long) per - 1) / per) * 112, m.logicalExtent());
    }
    @Test public void lastVisibleBoundaryPromotesBeforeMultiplication() {
        IndexGallery m = new IndexGallery();
        long at = 1_073_741_823L * 112;
        m.projectAt(Integer.MAX_VALUE, 240, 112, at);
        bounded(m);
        assertEquals(Integer.MAX_VALUE - 1, m.window(0));
        assertEquals(Integer.MAX_VALUE, m.window(1));
        assertEquals(Integer.MAX_VALUE - 5, m.window(2));
        assertEquals(Integer.MAX_VALUE, m.window(3));
    }
    @Test public void pastEndEmptyProjectionSaturatesAllFourBoundaries() {
        IndexGallery m = new IndexGallery();
        m.projectAt(Integer.MAX_VALUE, 240, 0, Long.MAX_VALUE);
        emptyWindow(m); assertEquals(Integer.MAX_VALUE, m.window(0));
    }
    @Test public void publicGalleryRangesMatchIndependentRowArithmetic() {
        for (int n : new int[] {0, 1, 2, 3, 7, 17, 33}) {
            for (int w : new int[] {0, 1, 111, 112, 224, 240, 1000}) {
                for (int h : new int[] {0, 1, 111, 112, 113, 225}) {
                    M3CollectionModel m = gallery(n, w, h);
                    long extent = ((n + (long) Math.max(1, w / 112) - 1) / Math.max(1, w / 112)) * 112;
                    assertEquals(extent, m.logicalExtent());
                    for (long request : new long[] {0, 1, 111, 112, Long.MAX_VALUE}) {
                        m.scrollTo(request); bounded(m);
                        int per = Math.max(1, w / 112);
                        long firstRow = Math.min(n, m.scrollOffset() / 112);
                        long visibleRows = h <= 0 ? 0 : Math.min(n, (m.scrollOffset() % 112 + h + 111) / 112);
                        long endRow = visibleRows == 0 ? firstRow : Math.min(n, firstRow + visibleRows);
                        long first = Math.min(n, firstRow * per), end = Math.min(n, endRow * per);
                        assertEquals(first, m.window(0)); assertEquals(end, m.window(1));
                        if (h == 0 || first == end) emptyWindow(m);
                    }
                }
            }
        }
    }
    @Test public void galleryResizeStillPublishesOnlyOneItemsDelta() {
        M3CollectionModel m = gallery(12, 240, 0); m.scrollTo(Long.MAX_VALUE);
        List<Integer> changes = new ArrayList<>();
        m.setDelta((item, mask) -> { changes.add(item); changes.add(mask); bounded(m); });
        m.setLogicalCount(3);
        assertEquals(List.of(-1, 64), changes); emptyWindow(m);
    }
    @Test public void nonGalleryProjectionAndExtentRemainUnscaled() {
        for (int mode : new int[] {M3CollectionModel.INFINITE, M3CollectionModel.MESSAGES, M3CollectionModel.PALETTE}) {
            M3CollectionModel m = new M3CollectionModel("rows", 0, mode, 0, 0);
            m.setLogicalCount(17); m.layout(240, 96); m.scrollTo(Long.MAX_VALUE);
            assertEquals(17L * m.itemExtent(), m.logicalExtent()); bounded(m);
            assertEquals(17, m.window(1));
        }
    }
    @Test public void hugeIndexOnlyProjectionRetainsFixedFourIntWindow() {
        IndexGallery m = new IndexGallery();
        int[] window = m.window;
        for (int n : new int[] {1, 17, 1_000_000, Integer.MAX_VALUE}) {
            long rows = (n + 1L) / 2;
            m.projectAt(n, 240, 112, Math.max(0, rows - 1) * 112);
            bounded(m); assertSame(window, m.window); assertEquals(4, window.length);
        }
        m.itemExtent = 1;
        m.projectAt(Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 0);
        bounded(m);
        assertArrayEquals(new int[] {0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE}, m.window);
        assertEquals(1L, m.logicalExtent());
        assertEquals(0, m.count()); assertEquals(8, m.flags.length);
    }
}
