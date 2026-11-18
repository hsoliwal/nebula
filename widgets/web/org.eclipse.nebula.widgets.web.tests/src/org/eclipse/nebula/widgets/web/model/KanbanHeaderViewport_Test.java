// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The board header and scrolling cards occupy different viewport planes. */
public class KanbanHeaderViewport_Test {

    private static CollectionModel board(boolean leaf) {
        CollectionModel model = leaf ? new KanbanBoardModel()
                : new CollectionModel("board", 0, CollectionModel.KANBAN, WebStates.ROUND, 0);
        model.setLogicalCount(60);
        for (int i = 0; i < model.count(); i++) {
            model.setGroup(i, i % 3);
            model.setText(i, "Card " + i);
        }
        model.layout(360, 192);
        return model;
    }

    @Test
    public void scrollLengthExcludesTheExistingHeaderGeometry() {
        for (boolean leaf : new boolean[] {false, true}) {
            CollectionModel model = board(leaf);
            int header = model.itemRect(0, 1);
            assertTrue("the initial card begins below a header", header > 0);
            assertEquals("scroll copy starts at the card plane", header, model.viewOrigin());
            assertEquals("only the body contributes visible scroll length", 192 - header, model.viewLength());
        }
    }

    @Test
    public void partiallyVisibleCardsCannotReceiveHeaderPointerHits() {
        for (boolean leaf : new boolean[] {false, true}) {
            for (boolean rtl : new boolean[] {false, true}) {
                CollectionModel model = board(leaf);
                int header = model.itemRect(0, 1);
                model.setRtl(rtl);
                model.scrollTo(17);
                int x = model.itemRect(0, 0) + 8;
                assertTrue("retain the partial card's real geometry", model.itemRect(0, 1) < header);
                assertEquals("header belongs to the header plane", -1, model.hitTest(x, header - 1));
                assertEquals("the visible portion still targets its original item", 0, model.hitTest(x, header));
            }
        }
    }

    @Test
    public void bottomScrollMakesTheLastCardsFullyReachable() {
        CollectionModel model = board(true);
        int header = model.itemRect(0, 1);
        model.scrollTo(Long.MAX_VALUE);
        assertEquals(model.logicalExtent() - (192 - header), model.scrollOffset());
        for (int i = 57; i < 60; i++) {
            assertTrue("last card remains resident", model.itemRect(i, 2) > 0);
            assertTrue("last card bottom fits within the body", model.itemRect(i, 1) + model.itemRect(i, 3) <= 192);
        }
    }

    @Test
    public void cleanShortScrollCopyNeverIncludesTheHeader() {
        CollectionModel model = board(true);
        int header = model.itemRect(0, 1);
        int[] damage = new int[4];
        assertTrue(model.takeDamage(damage));
        assertEquals(0, model.takeBlit());
        model.scrollTo(7);
        int delta = model.takeBlit();
        assertEquals(-7, delta);
        // These are the existing WebCanvas.changed source/destination equations.
        int source = model.viewOrigin() + Math.max(0, -delta);
        int destination = source + delta;
        int span = model.viewLength() - Math.abs(delta);
        assertTrue("copy source stays below the header", source >= header);
        assertTrue("copy destination stays below the header", destination >= header);
        assertTrue("the copied strip remains in the body", source + span <= 192);
        assertTrue(model.takeDamage(damage));
        assertTrue("exposed-band redraw stays below the header", damage[1] >= header);
    }

    @Test
    public void otherModesRetainTheirOriginalWholeCanvasViewport() {
        for (int mode = CollectionModel.SWIPE; mode <= CollectionModel.MASONRY; mode++) {
            CollectionModel model = new CollectionModel("other", 0, mode, 0, 0);
            model.setLogicalCount(60);
            model.layout(360, 192);
            assertEquals(0, model.viewOrigin());
            assertEquals(192, model.viewLength());
        }
    }

    @Test
    public void externalSubclassRetainsItsOriginalViewportAndHitHooks() {
        CollectionModel model = new CollectionModel("external", 0, CollectionModel.KANBAN, 0, 0) { };
        model.setLogicalCount(20);
        model.layout(360, 192);
        int header = model.itemRect(0, 1);
        model.scrollTo(17);
        assertEquals(0, model.viewOrigin());
        assertEquals(192, model.viewLength());
        assertEquals(0, model.hitTest(model.itemRect(0, 0) + 8, header - 1));
    }
}
