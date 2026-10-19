// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.roundedcheckbox;

import java.util.Random;

/** Test-only model of the original SWT.Paint GC field and nested-only repair. */
public final class RoundedCheckboxPaintScopeProof {
    private static long checks;

    private static final class Context {
        private final int identity;
        Context(int identity) { this.identity = identity; }
    }

    private static final class Original {
        Context gc;
        void paint(Context eventGc, Runnable draw) {
            gc = eventGc;
            draw.run();
        }
    }

    private static final class Repaired {
        Context gc;
        int paintDepth;
        void paint(Context eventGc, Runnable draw) {
            Context previous = gc;
            boolean nested = paintDepth != 0;
            paintDepth++;
            gc = eventGc;
            try {
                draw.run();
            } finally {
                paintDepth--;
                if (nested) gc = previous;
            }
        }
    }

    private static void eq(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason + " at check " + checks);
    }

    private static void nested(int seed, boolean failure) {
        var outer = new Context(seed);
        var inner = new Context(seed ^ 0x55aa55aa);
        var before = new Original();
        before.paint(outer, () -> before.paint(inner, () -> {}));
        eq(before.gc == inner, "actual historical stale nested event GC");

        var after = new Repaired();
        after.paint(outer, () -> {
            eq(after.gc == outer && after.paintDepth == 1, "outer enter");
            try {
                after.paint(inner, () -> {
                    eq(after.gc == inner && after.paintDepth == 2, "inner entry");
                    if (failure) throw new IllegalStateException("renderer failure");
                });
                eq(!failure, "successful nested exit only");
            } catch (IllegalStateException expected) {
                eq(failure && "renderer failure".equals(expected.getMessage()),
                        "original exception propagated without replacement");
            }
            eq(after.gc == outer && after.paintDepth == 1,
                    "original outer GC recovered before remaining paint calls");
        });
        eq(after.gc == outer && after.paintDepth == 0,
                "outermost callback retains its GC as original implementation");
    }

    private static void depthThree(int seed) {
        var one = new Context(seed);
        var two = new Context(seed + 1);
        var three = new Context(seed + 2);
        var after = new Repaired();
        after.paint(one, () -> {
            after.paint(two, () -> {
                after.paint(three, () -> {
                    eq(after.gc == three && after.paintDepth == 3,
                            "third nested paint sees own native GC");
                });
                eq(after.gc == two && after.paintDepth == 2,
                        "second callback GC restored");
            });
            eq(after.gc == one && after.paintDepth == 1,
                    "first callback GC restored");
        });
        eq(after.gc == one && after.paintDepth == 0, "outer completion");
    }

    private static void topLevelFailure(int seed) {
        var incoming = new Context(seed);
        var after = new Repaired();
        try {
            after.paint(incoming, () -> { throw new AssertionError("paint error"); });
            throw new AssertionError("missing source exception");
        } catch (AssertionError expected) {
            eq("paint error".equals(expected.getMessage()), "error object survives");
        }
        eq(after.gc == incoming && after.paintDepth == 0,
                "top-level failure keeps original post-paint GC retention");
        var next = new Context(seed + 3);
        after.paint(next, () -> eq(after.gc == next && after.paintDepth == 1,
                "subsequent callback still enters cleanly"));
        eq(after.gc == next && after.paintDepth == 0, "after failure retry works");
    }

    public static void main(String[] args) {
        Random random = new Random(0x4d3347524f554e44L);
        for (int n = 0; n < 20000; n++) {
            int id = random.nextInt();
            nested(id, false);
            nested(id, true);
            depthThree(id);
            topLevelFailure(id);
        }
        System.out.println("ROUNDED_CHECKBOX_PAINT_SCOPE_PASS checks=" + checks);
    }
}
