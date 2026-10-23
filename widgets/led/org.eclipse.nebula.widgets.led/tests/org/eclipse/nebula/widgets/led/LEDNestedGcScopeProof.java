// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.led;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Independent headless old-vs-new model of BaseLED.onPaint's callback GC scope. */
public final class LEDNestedGcScopeProof {
    private static long checks;

    private static final class NativeGc {
        final int id;
        NativeGc(int id) { this.id = id; }
    }

    private static final class SourceModel {
        NativeGc gc;
        int paintDepth;
        final boolean candidate;
        final List<String> events = new ArrayList<>();

        SourceModel(boolean candidate) { this.candidate = candidate; }

        void paint(NativeGc eventGc, Runnable body) {
            // Before and after use the same setup and callback ordering.
            NativeGc previous = gc;
            int entryDepth = paintDepth++;
            gc = eventGc;
            try {
                events.add("background:" + gc.id);
                events.add("fill:" + gc.id);
                events.add("advanced:" + gc.id);
                events.add("antialias:" + gc.id);
                body.run();
            } finally {
                paintDepth--;
                // Original BaseLED leaves gc pointing at the last nested event.
                // Candidate restores it only inside a reentrant paint.
                if (candidate && entryDepth != 0) gc = previous;
            }
        }
    }

    private static void require(boolean passed, String explanation) {
        checks++;
        if (!passed) throw new AssertionError(explanation + " at comparison " + checks);
    }

    private static void steady(int seed) {
        NativeGc oldGc = new NativeGc(seed);
        NativeGc newGc = new NativeGc(seed);
        SourceModel old = new SourceModel(false);
        SourceModel now = new SourceModel(true);
        old.gc = new NativeGc(-3);
        now.gc = new NativeGc(-3);
        old.paint(oldGc, () -> old.events.add("draw:" + old.gc.id));
        now.paint(newGc, () -> now.events.add("draw:" + now.gc.id));
        require(old.events.equals(now.events), "single paint action order and colors unchanged");
        require(old.gc == oldGc && now.gc == newGc, "outermost GC field remains event GC");
        require(old.paintDepth == 0 && now.paintDepth == 0, "outer scopes balanced");
    }

    private static void nested(int seed, boolean failNested, boolean errorNested) {
        SourceModel old = new SourceModel(false);
        SourceModel now = new SourceModel(true);
        NativeGc oldOuter = new NativeGc(seed), nowOuter = new NativeGc(seed);
        NativeGc oldInner = new NativeGc(seed + 1), nowInner = new NativeGc(seed + 1);
        RuntimeException failure = new IllegalStateException("provider failure " + seed);
        AssertionError error = new AssertionError("provider fatal " + seed);

        Runnable oldInnerBody = () -> {
            old.events.add("inner:" + old.gc.id);
            if (failNested) { if (errorNested) throw error; else throw failure; }
        };
        Runnable newInnerBody = () -> {
            now.events.add("inner:" + now.gc.id);
            if (failNested) { if (errorNested) throw error; else throw failure; }
        };
        old.paint(oldOuter, () -> {
            try { old.paint(oldInner, oldInnerBody); }
            catch (RuntimeException | Error caught) {
                require(caught == (errorNested ? error : failure), "old primary error retained");
            }
            old.events.add("outer-resumed:" + old.gc.id);
        });
        now.paint(nowOuter, () -> {
            try { now.paint(nowInner, newInnerBody); }
            catch (RuntimeException | Error caught) {
                require(caught == (errorNested ? error : failure), "new primary error retained");
            }
            now.events.add("outer-resumed:" + now.gc.id);
        });
        require(old.events.subList(0, 9).equals(now.events.subList(0, 9)),
                "nested paint actions and event dispatch order unchanged");
        require(old.gc == oldInner && now.gc == nowOuter,
                "candidate restores outer GC, rejected original retains nested GC");
        require(old.events.get(9).equals("outer-resumed:" + oldInner.id),
                "original reentrancy defect reproduced");
        require(now.events.get(9).equals("outer-resumed:" + nowOuter.id),
                "outer paint resumes with its own GC");
        require(old.paintDepth == 0 && now.paintDepth == 0,
                "nested scopes balanced after normal and exceptional callbacks");
    }

    public static void main(String[] args) {
        boolean quick = args.length == 1 && "--quick".equals(args[0]);
        int count = quick ? 2_000 : 100_000;
        Random rng = new Random(0x4d334c454447434cL);
        for (int i = 0; i < count; i++) {
            int id = rng.nextInt(1_000_000);
            steady(id);
            nested(id, i % 3 != 0, i % 5 == 0);
        }
        System.out.println("LED_ORIGINAL_GC_SCOPE_PASS checks=" + checks + " quick=" + quick);
    }
}
