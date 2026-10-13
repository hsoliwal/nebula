/*******************************************************************************
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0, available at
 * https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.*;
import java.util.Random;
import org.eclipse.swt.graphics.Rectangle;
import org.junit.Test;

/** Independent area/plane oracle; never derives expectations from the paint owner. */
public class GridPaintDAG_Test {

    @Test public void emptyClipAndClientHaveNoPlanes() {
        Rectangle area = new Rectangle(0, 0, 100, 120);
        for (int width : new int[] {-7, 0, 1, 10}) {
            for (int height : new int[] {-9, 0, 1, 10}) {
                if (width > 0 && height > 0) continue;
                Rectangle empty = new Rectangle(10, 20, width, height);
                for (int flags = 0; flags < 16; flags++) {
                    assertEquals("empty clip " + empty, 0, actual(empty, area, 30, 30, flags));
                    assertEquals("empty client " + empty, 0, actual(area, empty, 30, 30, flags));
                    assertEquals("aliased empty rectangle", 0, actual(empty, empty, 30, 30, flags));
                }
            }
        }
    }

    @Test public void emptyExtremaDoNotOverflowIntoAPlane() {
        for (int x : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            for (int y : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
                Rectangle empty = new Rectangle(x, y, 0, 0);
                assertEquals(0, actual(empty, empty, Integer.MAX_VALUE, Integer.MAX_VALUE, 15));
                assertEquals(0, actual(new Rectangle(-10, -10, 20, 20), empty, 4, 4, 15));
            }
        }
    }

    @Test public void nullValidationRetainsOrderAndMessages() {
        try {
            actual(null, null, 0, 0, 0);
            fail("null clip must fail first");
        } catch (IllegalArgumentException expected) {
            assertEquals("clip", expected.getMessage());
        }
        try {
            actual(new Rectangle(0, 0, 0, 0), null, 0, 0, 0);
            fail("empty clip does not suppress null client validation");
        } catch (IllegalArgumentException expected) {
            assertEquals("clientArea", expected.getMessage());
        }
    }

    @Test public void touchingEdgesAndChromeOnlyDamageHaveNoBody() {
        Rectangle area = new Rectangle(0, 0, 100, 120);
        assertEquals(0, actual(new Rectangle(100, 0, 10, 10), area, 20, 20, 15));
        assertEquals(1 | 8 | 32, actual(new Rectangle(0, 0, 100, 20), area, 20, 20, 15));
        assertEquals(1 | 16 | 32, actual(new Rectangle(0, 100, 100, 20), area, 20, 20, 15));
        assertEquals(1 | 2 | 4 | 32, actual(new Rectangle(0, 20, 100, 80), area, 20, 20, 15));
    }

    @Test public void boundedPositiveAreaCorpusMatchesIndependentLatticeOracle() {
        int visited = 0;
        for (Rectangle area : new Rectangle[] {new Rectangle(0, 0, 6, 8), new Rectangle(-3, -5, 8, 9)}) {
            for (int x : new int[] {-6, 0, 3, 8}) for (int y : new int[] {-9, -2, 0, 4, 12}) {
                for (int width : new int[] {1, 3, 9}) for (int height : new int[] {1, 3, 9}) {
                    Rectangle clip = new Rectangle(x, y, width, height);
                    for (int header : new int[] {-3, 0, 2, 9}) for (int footer : new int[] {-2, 0, 3}) {
                        for (int flags = 0; flags < 16; flags++) {
                            assertEquals("case " + visited, lattice(clip, area, header, footer, flags),
                                    actual(clip, area, header, footer, flags));
                            visited++;
                        }
                    }
                }
            }
        }
        assertEquals(69120, visited);
    }

    @Test public void seededPositiveAreaMasksPreserveIndependentRegionAlgebra() {
        Random random = new Random(0x4E4542554C41L);
        for (int index = 0; index < 8192; index++) {
            Rectangle area = new Rectangle(random.nextInt(2000) - 1000, random.nextInt(2000) - 1000,
                    random.nextInt(1000) + 1, random.nextInt(1000) + 1);
            Rectangle clip = new Rectangle(area.x + random.nextInt(2000) - 1000,
                    area.y + random.nextInt(2000) - 1000, random.nextInt(1000) + 1, random.nextInt(1000) + 1);
            int header = random.nextInt(1200) - 100;
            int footer = random.nextInt(1200) - 100;
            int flags = random.nextInt(16);
            assertEquals("seeded case " + index, regions(clip, area, header, footer, flags),
                    actual(clip, area, header, footer, flags));
        }
    }

    static int actual(Rectangle clip, Rectangle area, int header, int footer, int flags) {
        return GridPaintDAG.plan(clip, area, header, footer,
                (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0);
    }

    private static int lattice(Rectangle clip, Rectangle area, int header, int footer, int flags) {
        int mask = 0;
        int topInset = (flags & 1) != 0 ? Math.max(header, 0) : 0;
        int bottomInset = (flags & 2) != 0 ? Math.max(footer, 0) : 0;
        // Every integer cell represents an interior half-pixel; touching edges have no cell.
        for (int y = area.y; y < area.y + area.height; y++) for (int x = area.x; x < area.x + area.width; x++) {
            if (x < clip.x || x >= clip.x + clip.width || y < clip.y || y >= clip.y + clip.height) continue;
            mask |= 1;
            if (y >= area.y + topInset && y < area.y + area.height - bottomInset) mask |= 2;
            if ((flags & 1) != 0 && y < area.y + topInset) mask |= 8;
            if ((flags & 2) != 0 && y >= area.y + area.height - bottomInset) mask |= 16;
        }
        if ((mask & 2) != 0 && (flags & 4) != 0) mask |= 4;
        if (mask != 0 && (flags & 8) != 0) mask |= 32;
        return mask;
    }

    private static int regions(Rectangle clip, Rectangle area, int header, int footer, int flags) {
        if (!overlap(clip, area.x, area.y, area.width, area.height)) return 0;
        int result = 1 | ((flags & 8) != 0 ? 32 : 0);
        int top = (flags & 1) != 0 ? Math.max(header, 0) : 0;
        int bottom = (flags & 2) != 0 ? Math.max(footer, 0) : 0;
        if (overlap(clip, area.x, area.y + top, area.width, area.height - top - bottom)) {
            result |= 2 | ((flags & 4) != 0 ? 4 : 0);
        }
        if ((flags & 1) != 0 && overlap(clip, area.x, area.y, area.width, Math.min(top, area.height))) result |= 8;
        int bottomHeight = Math.min(bottom, area.height);
        if ((flags & 2) != 0 && overlap(clip, area.x, area.y + area.height - bottomHeight,
                area.width, bottomHeight)) result |= 16;
        return result;
    }

    private static boolean overlap(Rectangle clip, long x, long y, long width, long height) {
        return width > 0 && height > 0 && clip.width > 0 && clip.height > 0
                && Math.max(x, clip.x) < Math.min(x + width, (long) clip.x + clip.width)
                && Math.max(y, clip.y) < Math.min(y + height, (long) clip.y + clip.height);
    }
}
