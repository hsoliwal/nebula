/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import org.junit.Test;

/** Immutable framebuffer publication and coalesced damage-contract tests. */
public class RfbFrameDamageTest {
    @Test public void publishesDefensiveCopyAndMergesChangedRegions() {
        int[] firstPixels = new int[8 * 6];
        firstPixels[0] = 0x112233;
        RfbClient.Frame first = new RfbClient.Frame(8, 6, firstPixels, "first",
                1, 1, 1, 2, 2);
        firstPixels[0] = 0xff0000;
        assertEquals(0x112233, first.rgbCopy()[0]);

        int[] secondPixels = new int[8 * 6];
        secondPixels[0] = 0xaabbcc;
        RfbClient.Frame second = new RfbClient.Frame(8, 6, secondPixels,
                "second", 2, 5, 3, 2, 1);
        RfbClient.Frame latest = second.withAdditionalDamage(first);
        assertEquals(2, latest.sequence());
        assertEquals("second", latest.title());
        assertEquals(1, latest.dirtyX());
        assertEquals(1, latest.dirtyY());
        assertEquals(6, latest.dirtyWidth());
        assertEquals(3, latest.dirtyHeight());
        assertEquals(0xaabbcc, latest.rgbCopy()[0]);
        assertEquals(1, first.dirtyX());
        assertEquals(2, first.dirtyWidth());
        int[] escaped = latest.rgbCopy();
        escaped[0] = 0;
        assertEquals(0xaabbcc, latest.rgbCopy()[0]);
    }

    @Test public void multipleCoalescedUpdatesKeepAllDamage() {
        int[] pixels = new int[10 * 10];
        RfbClient.Frame a = new RfbClient.Frame(10, 10, pixels, "", 1, 1, 1, 1, 1);
        RfbClient.Frame b = new RfbClient.Frame(10, 10, pixels, "", 2, 7, 2, 1, 2);
        RfbClient.Frame c = new RfbClient.Frame(10, 10, pixels, "", 3, 3, 8, 1, 1);
        RfbClient.Frame latest = b.withAdditionalDamage(a);
        latest = c.withAdditionalDamage(latest);
        assertEquals(1, latest.dirtyX());
        assertEquals(1, latest.dirtyY());
        assertEquals(7, latest.dirtyWidth());
        assertEquals(8, latest.dirtyHeight());
        assertEquals(3, latest.sequence());
    }

    @Test public void resizeForcesNewEntireFramebufferToBeDirty() {
        RfbClient.Frame before = new RfbClient.Frame(10, 10,
                new int[100], "", 1, 2, 2, 3, 3);
        RfbClient.Frame after = new RfbClient.Frame(4, 3,
                new int[12], "", 2, 1, 1, 1, 1);
        RfbClient.Frame merged = after.withAdditionalDamage(before);
        assertEquals(0, merged.dirtyX());
        assertEquals(0, merged.dirtyY());
        assertEquals(4, merged.dirtyWidth());
        assertEquals(3, merged.dirtyHeight());
    }
}
