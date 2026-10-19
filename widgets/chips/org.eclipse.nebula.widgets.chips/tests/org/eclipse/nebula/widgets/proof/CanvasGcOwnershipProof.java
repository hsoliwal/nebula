// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.proof;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Differential Java 21 model of original and candidate SWT GC ownership atoms. */
public final class CanvasGcOwnershipProof {
    private static long checks;
    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason + " @" + checks);
    }
    private static final class Failure extends RuntimeException {
        Failure(String message) { super(message); }
        private static final long serialVersionUID = 1L;
    }
    private static final class FakeGc {
        int background = 10, foreground = 20, created = 1, closed;
        int failBackground, failForeground;
        final List<String> calls = new ArrayList<>();
        void setBackground(int value) {
            calls.add("B" + value);
            if (failBackground-- > 0) throw new Failure("background cleanup");
            background = value;
        }
        void setForeground(int value) {
            calls.add("F" + value);
            if (failForeground-- > 0) throw new Failure("foreground cleanup");
            foreground = value;
        }
        void dispose() { calls.add("D"); closed++; }
        int textExtent(boolean fail) {
            calls.add("T");
            if (fail) throw new Failure("native text measurement");
            return 13;
        }
    }
    @FunctionalInterface private interface Painter { void paint(FakeGc gc); }
    private static void restoreOnFailure(FakeGc gc, int back, int fore,
                                         RuntimeException failure) {
        try { gc.setBackground(back); }
        catch (RuntimeException | Error cleanup) {
            if (cleanup != failure) failure.addSuppressed(cleanup);
        }
        try { gc.setForeground(fore); }
        catch (RuntimeException | Error cleanup) {
            if (cleanup != failure) failure.addSuppressed(cleanup);
        }
    }
    private static void oldChips(FakeGc gc, Painter painter) {
        int back = gc.background, fore = gc.foreground;
        painter.paint(gc);
        gc.setBackground(back);
        gc.setForeground(fore);
    }
    private static void newChips(FakeGc gc, Painter painter) {
        int back = gc.background, fore = gc.foreground;
        try { painter.paint(gc); }
        catch (RuntimeException failure) {
            restoreOnFailure(gc, back, fore, failure);
            throw failure;
        }
        gc.setBackground(back);
        gc.setForeground(fore);
    }
    private static void oldPGroup(FakeGc gc, Painter strategy, Painter toggle,
                                  Painter toolitems) {
        int back = gc.background, fore = gc.foreground;
        strategy.paint(gc);
        gc.setBackground(back);
        gc.setForeground(fore);
        toggle.paint(gc);
        toolitems.paint(gc);
    }
    private static void newPGroup(FakeGc gc, Painter strategy, Painter toggle,
                                  Painter toolitems) {
        int back = gc.background, fore = gc.foreground;
        try {
            strategy.paint(gc);
            gc.setBackground(back);
            gc.setForeground(fore);
            toggle.paint(gc);
            toolitems.paint(gc);
        } catch (RuntimeException failure) {
            restoreOnFailure(gc, back, fore, failure);
            throw failure;
        }
    }
    private static int oldSize(FakeGc gc, boolean failure) {
        int extent = gc.textExtent(failure);
        int width = 4 + extent;
        gc.dispose();
        return width;
    }
    private static int newSize(FakeGc gc, boolean failure) {
        try { return 4 + gc.textExtent(failure); }
        finally { gc.dispose(); }
    }
    private static RuntimeException getFailure(Runnable callback) {
        try { callback.run(); } catch (RuntimeException failure) { return failure; }
        throw new AssertionError("expected injected failure");
    }
    public static void main(String[] arguments) {
        var random = new Random(0x4d3347434c45414eL);
        for (int pass = 0; pass < 80000; pass++) {
            int phase = pass % 5;
            var original = new FakeGc();
            var candidate = new FakeGc();
            int delta = random.nextInt(100);
            Painter simple = gc -> { gc.setBackground(30 + delta); gc.setForeground(40 + delta); };
            if (phase == 0) {
                oldChips(original, simple);
                newChips(candidate, simple);
            } else if (phase == 1) {
                oldPGroup(original, simple, simple, simple);
                newPGroup(candidate, simple, simple, simple);
            } else if (phase == 2) {
                check(oldSize(original, false) == newSize(candidate, false), "size result");
            } else if (phase == 3) {
                RuntimeException old = getFailure(() -> oldChips(original, gc -> {
                    gc.setBackground(88);
                    throw new Failure("paint-phase");
                }));
                RuntimeException next = getFailure(() -> newChips(candidate, gc -> {
                    gc.setBackground(88);
                    throw new Failure("paint-phase");
                }));
                check(old.getMessage().equals(next.getMessage()), "Chips primary exception");
                check(candidate.background == 10 && candidate.foreground == 20,
                      "Chips exception restores borrowed GC");
            } else {
                RuntimeException old = getFailure(() -> oldPGroup(original, simple, gc -> {
                    gc.setForeground(77);
                    throw new Failure("toggle-phase");
                }, simple));
                RuntimeException next = getFailure(() -> newPGroup(candidate, simple, gc -> {
                    gc.setForeground(77);
                    throw new Failure("toggle-phase");
                }, simple));
                check(old.getMessage().equals(next.getMessage()), "PGroup primary exception");
                check(candidate.background == 10 && candidate.foreground == 20,
                      "PGroup exception restores borrowed GC");
            }
            if (phase < 3) {
                check(original.calls.equals(candidate.calls), "success call order parity");
                check(original.background == candidate.background &&
                      original.foreground == candidate.foreground,
                      "normal paint GC state parity");
                check(original.closed == candidate.closed, "normal owned GC disposal parity");
            }
        }
        FakeGc oldMeasurement = new FakeGc(), newMeasurement = new FakeGc();
        check("native text measurement".equals(getFailure(() -> oldSize(oldMeasurement, true)).getMessage()),
              "original text measurement failure");
        check("native text measurement".equals(getFailure(() -> newSize(newMeasurement, true)).getMessage()),
              "candidate text measurement failure identity");
        check(oldMeasurement.closed == 0 && newMeasurement.closed == 1,
              "original leak repaired on candidate failure");
        FakeGc fail = new FakeGc(); fail.failBackground = 1; fail.failForeground = 1;
        RuntimeException primary = new Failure("original callback failure");
        RuntimeException actual = getFailure(() -> newChips(fail, gc -> { throw primary; }));
        check(actual == primary, "primary callback exception preserved");
        check(actual.getSuppressed().length == 2, "both failed cleanups suppressed");
        FakeGc nested = new FakeGc();
        newPGroup(nested, gc -> newChips(gc, inner -> inner.setBackground(88)),
                  gc -> gc.setForeground(77), gc -> gc.setBackground(66));
        check(nested.background == 66 && nested.foreground == 77,
              "normal nested painter preserves last rendered GC state");
        System.out.println("M3_ORIGINAL_CANVAS_GC_PROOF_PASS assertions=" + checks);
    }
}
