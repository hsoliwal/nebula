// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;
import java.util.*;

import org.junit.Test;

/** Row-to-item projection contracts, including index-only counts with no payload allocation. */
public class CollectionViewport_Test {
    private static CollectionModel gallery(int count, int width, int height) {
        CollectionModel m = new CollectionModel("gallery", 0, CollectionModel.GALLERY, 0, 0);
        m.setLogicalCount(count); m.layout(width, height); return m;
    }
    private static void bounded(WebView m) {
        int n = m.logicalCount();
        assertTrue("visible starts nonnegative", m.window(0) >= 0);
        assertTrue("visible is half-open", m.window(0) <= m.window(1));
        assertTrue("visible ends within count", m.window(1) <= n);
        assertTrue("resident starts nonnegative", m.window(2) >= 0);
        assertTrue("resident contains visible start", m.window(2) <= m.window(0));
        assertTrue("resident contains visible end", m.window(3) >= m.window(1));
        assertTrue("resident ends within count", m.window(3) <= n);
    }
    private static void emptyWindow(WebView m) {
        bounded(m);
        assertEquals(m.window(0), m.window(1));
        assertEquals(m.window(0), m.window(2));
        assertEquals(m.window(0), m.window(3));
    }
    /** Exercises only count/index geometry; does not pretend that billions of payloads were allocated. */
    private static final class IndexGallery extends CollectionModel {
        int logical;
        IndexGallery() { super("gallery-index", 0, GALLERY, 0, 0); }
        @Override public int logicalCount() { return logical; }
        void projectAt(int n, int w, int h, long at) {
            logical = n; width = w; height = h; scroll = at; project();
        }
    }
    @Test public void zeroHeightPartialLastRowHasEmptyBoundedWindow() {
        CollectionModel m = gallery(3, 240, 0); m.scrollTo(Long.MAX_VALUE);
        emptyWindow(m); assertEquals(3, m.window(0));
        assertEquals(0, m.residentCount());
        for (int i = 0; i < m.count(); i++) { assertEquals(0, m.itemRect(i, 2)); assertEquals(0, m.itemRect(i, 3)); }
    }
    @Test public void zeroHeightFilteredPartialRowUsesLogicalPositions() {
        CollectionModel m = gallery(5, 240, 0);
        for (int i = 0; i < 5; i++) m.setText(i, i % 2 == 0 ? "keep" : "hide");
        m.setFilter("keep"); assertEquals(3, m.logicalCount()); assertEquals(2, m.itemAt(1));
        m.scrollTo(Long.MAX_VALUE); emptyWindow(m); assertEquals(3, m.window(0));
        m.setFilter("no-match"); emptyWindow(m); assertEquals(0, m.logicalCount());
    }
    @Test public void zeroHeightRtlDoesNotInventResidentItems() {
        CollectionModel m = gallery(3, 240, 0); m.setRtl(true); m.scrollTo(Long.MAX_VALUE);
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
                    CollectionModel m = gallery(n, w, h);
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
        CollectionModel m = gallery(12, 240, 0); m.scrollTo(Long.MAX_VALUE);
        List<Integer> changes = new ArrayList<>();
        m.setDelta((item, mask) -> { changes.add(item); changes.add(mask); bounded(m); });
        m.setLogicalCount(3);
        assertEquals(List.of(-1, 64), changes); emptyWindow(m);
    }
    @Test public void nonGalleryProjectionAndExtentRemainUnscaled() {
        for (int mode : new int[] {CollectionModel.INFINITE, CollectionModel.MESSAGES, CollectionModel.PALETTE}) {
            CollectionModel m = new CollectionModel("rows", 0, mode, 0, 0);
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

    @Test public void thumbAtBottomPreservesExactLargeTrackPosition() {
        CollectionModel m = new CollectionModel("thumb-overflow", 0,
                CollectionModel.REORDER, WebStates.RECT, WebStates.V_DEFAULT);
        m.setLogicalCount(3); m.setItemExtent(Integer.MAX_VALUE);
        m.layout(120, 1); m.scrollTo(Long.MAX_VALUE);
        int[] out = new int[2]; m.thumb(Integer.MAX_VALUE, out);
        assertEquals(6_442_450_941L, m.logicalExtent());
        assertEquals(6_442_450_940L, m.scrollOffset());
        assertArrayEquals(new int[] {2_147_483_639, 8}, out);
    }
    @Test public void thumbAtBottomProjectsHugeLogicalExtentWithoutPayloads() {
        IndexGallery m = new IndexGallery();
        int[] window = m.window;
        m.itemExtent = Integer.MAX_VALUE;
        long extent = (long) Integer.MAX_VALUE * Integer.MAX_VALUE;
        m.projectAt(Integer.MAX_VALUE, 1, 1, extent - 1);
        int[] out = new int[2]; m.thumb(1000, out);
        assertEquals(4_611_686_014_132_420_609L, m.logicalExtent());
        assertArrayEquals(new int[] {992, 8}, out);
        assertSame(window, m.window); assertEquals(4, m.window.length);
        assertEquals(0, m.count()); assertEquals(8, m.flags.length);
    }
    @Test public void thumbProjectionMatchesBigIntegerMatrixWithoutPayloads() {
        IndexGallery m = new IndexGallery();
        int[] window = m.window, out = new int[2];
        int checked = 0;
        for (int n : new int[] {0, 1, 2, 3, 7, 31, 1_000_000, Integer.MAX_VALUE}) {
            for (int item : new int[] {1, 7, 40, 4096, Integer.MAX_VALUE}) {
                m.itemExtent = item;
                long total = (long) n * item;
                for (int height : new int[] {0, 1, 31, 96, Integer.MAX_VALUE}) {
                    long max = Math.max(0, total - height);
                    for (long at : new long[] {0, Math.min(1, max), max / 3, max / 2, max == 0 ? 0 : max - 1, max}) {
                        m.projectAt(n, 1, height, at);
                        assertEquals(total, m.logicalExtent());
                        java.math.BigInteger ext = java.math.BigInteger.valueOf(total).max(java.math.BigInteger.ONE);
                        java.math.BigInteger view = java.math.BigInteger.valueOf(height);
                        java.math.BigInteger range = ext.subtract(view).max(java.math.BigInteger.ONE);
                        java.math.BigInteger position = java.math.BigInteger.valueOf(at).min(range);
                        for (int track : new int[] {Integer.MIN_VALUE, -31, -1, 0, 1, 7, 8, 9, 31, 200, 1000, Integer.MAX_VALUE}) {
                            java.math.BigInteger pixels = java.math.BigInteger.valueOf(track);
                            java.math.BigInteger len = pixels.min(java.math.BigInteger.valueOf(8))
                                    .max(pixels.min(pixels.multiply(view).divide(ext)));
                            int expected = pixels.subtract(len).multiply(position).divide(range).intValueExact();
                            m.thumb(track, out);
                            assertEquals("thumb offset", expected, out[0]);
                            assertEquals("thumb length", len.intValueExact(), out[1]);
                            checked++;
                        }
                    }
                }
            }
        }
        assertEquals(14_400, checked);
        assertSame(window, m.window); assertEquals(4, m.window.length);
        assertEquals(0, m.count()); assertEquals(8, m.flags.length);
    }
}
