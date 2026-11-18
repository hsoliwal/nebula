// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Full-lane test oracle with an independently specified fixed-header board viewport.
 * It stays an unrecognized subclass and does not call the resident traversal helpers.
 */
abstract class CollectionPlaneChecks extends CollectionModel {
    CollectionPlaneChecks(String concept, int role, int mode, int shape, int variant) {
        super(concept, role, mode, shape, variant);
    }

    @Override
    public int viewOrigin() {
        if (mode != KANBAN) return super.viewOrigin();
        return Math.min(height, WebStates.metric(size, 0));
    }

    @Override
    public int viewLength() {
        if (mode != KANBAN) return super.viewLength();
        return Math.max(0, height - WebStates.metric(size, 0));
    }

    @Override
    public int hitTest(int x, int y) {
        if (mode != KANBAN) return super.hitTest(x, y);
        if (x < 0 || x >= width || y < Math.min(height, WebStates.metric(size, 0)) || y >= height) return -1;
        for (int i = count - 1; i >= 0; i--) {
            int b = i * 4;
            if ((flags[i] & WebStates.HIDDEN) == 0 && x >= rects[b] && y >= rects[b + 1]
                    && x < rects[b] + rects[b + 2] && y < rects[b + 1] + rects[b + 3]) return i;
        }
        return -1;
    }

    /** Every original field remains compared; explicit plane metadata replaces no assertion. */
    static void assertPaint(CollectionModel model, WebPaint expected, WebPaint actual, String where) {
        assertEquals(where + " record count", expected.count(), actual.count());
        for (int r = 0; r < expected.count(); r++) {
            for (int field = 0; field < WebPaint.STRIDE; field++) {
                assertEquals(where + " record=" + r + " field=" + field, expected.get(r, field), actual.get(r, field));
            }
            int op = expected.get(r, WebPaint.F_OP), ref = expected.get(r, WebPaint.F_REF);
            if (op == WebPaint.TEXT) {
                assertEquals(where + " resolved text=" + r, expected.text(ref), actual.text(actual.get(r, WebPaint.F_REF)));
            } else if (op == WebPaint.POLY) {
                for (int point = 0; point < 2 * expected.get(r, WebPaint.F_ARG); point++) {
                    assertEquals(where + " polyline=" + r + ":" + point,
                            expected.point(ref + point), actual.point(actual.get(r, WebPaint.F_REF) + point));
                }
            }
        }
        if (model.mode != KANBAN) {
            assertEquals(where + " original unclipped hash", expected.hash(), actual.hash());
            return;
        }
        int columns = 1;
        for (int i = 0; i < model.count(); i++) columns = Math.max(columns, model.group(i) + 1);
        int header = Math.min(model.height, WebStates.metric(model.size, 0));
        int body = Math.max(0, model.height - WebStates.metric(model.size, 0));
        // Existing board chrome emits a column background and a header label for each column.
        // All subsequent records, including the final inherited focus ring, belong to its body.
        for (int r = 0; r < actual.count(); r++) {
            assertFalse(where + " full-scan reference has no implicit clip", expected.hasClip(r));
            if (r < 2 * columns) {
                assertFalse(where + " header record=" + r, actual.hasClip(r));
            } else {
                assertTrue(where + " body record=" + r, actual.hasClip(r));
                assertEquals(0, actual.clipRect(r, 0));
                assertEquals(header, actual.clipRect(r, 1));
                assertEquals(model.width, actual.clipRect(r, 2));
                assertEquals(body, actual.clipRect(r, 3));
            }
        }
    }
}
