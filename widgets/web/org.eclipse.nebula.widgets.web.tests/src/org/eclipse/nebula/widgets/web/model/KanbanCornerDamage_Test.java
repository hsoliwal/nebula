// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Stationary column corners need repair in the same publication as a body scroll copy. */
public class KanbanCornerDamage_Test {
    private static CollectionModel board(int shape) {
        CollectionModel model = new CollectionModel("corner-damage", 0, CollectionModel.KANBAN, shape, 0);
        model.setLogicalCount(60);
        for (int i = 0; i < model.count(); i++) model.setGroup(i, i % 3);
        model.layout(360, 192);
        model.takeDamage(new int[4]);
        model.takeBlit();
        return model;
    }

    private static int[] observe(CollectionModel model, long offset, int expectedBlit) {
        int[] callbacks = new int[1], damage = new int[4];
        model.setDelta((item, mask) -> {
            assertEquals(WebView.D_SCROLL, mask);
            callbacks[0]++;
            assertEquals("eligible copy reaches the consumer", expectedBlit, model.takeBlit());
            assertTrue("repair is published before the synchronous consumer", model.takeDamage(damage));
        });
        model.scrollTo(offset);
        assertEquals("one existing publication", 1, callbacks[0]);
        assertEquals("the consumer drained the complete repair", false, model.takeDamage(new int[4]));
        return damage;
    }

    @Test
    public void upwardCopyRepairsTheOriginalAndShiftedCornerBandTogether() {
        CollectionModel model = board(WebStates.ROUND);
        int[] damage = observe(model, 7, -7);
        int originalCornerTop = 192 - model.radius() - 1;
        int copiedCornerTop = originalCornerTop - 7;
        assertTrue("repair includes the copied rounded corner", damage[1] <= copiedCornerTop);
        assertTrue("repair includes the original rounded corner", damage[1] + damage[3] >= 192);
        assertTrue("the header is not part of a body repair", damage[1] >= model.viewOrigin());
    }

    @Test
    public void downwardCopyRepairsBottomCornersBeforeTheConsumerRuns() {
        CollectionModel model = board(WebStates.ROUND);
        model.scrollTo(14);
        model.takeBlit();
        model.takeDamage(new int[4]);
        int[] damage = observe(model, 7, 7);
        // One damage rectangle conservatively joins top exposure and fixed bottom-corner repair.
        assertArrayEquals(new int[] {0, model.viewOrigin(), 360, 192 - model.viewOrigin()}, damage);
    }

    @Test
    public void squareColumnBackgroundKeepsTheExistingExposedStripDamage() {
        CollectionModel model = board(WebStates.RECT);
        assertEquals(0, model.radius());
        assertArrayEquals(new int[] {0, 185, 360, 7}, observe(model, 7, -7));
    }
}
