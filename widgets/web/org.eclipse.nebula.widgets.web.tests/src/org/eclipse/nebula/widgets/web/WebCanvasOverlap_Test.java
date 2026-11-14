// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.nebula.widgets.web.model.WebModel;
import org.eclipse.nebula.widgets.web.model.WebPaint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Real SWT receiver, intercepted copy/redraw arguments. No Shell is opened and no
 * intercepted scroll delegates to native copy. This proves neither pixels nor
 * safety across native scroll's synchronous paint flush.
 */
public class WebCanvasOverlap_Test {
    private Display display;
    private boolean ownsDisplay;
    private Shell shell;
    private Model model;
    private Probe canvas;

    private static final class MarkerFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class Model extends WebModel {
        final List<String> trace = new ArrayList<>();
        final List<int[]> damageAliases = new ArrayList<>();
        final MarkerFailure failure = new MarkerFailure();
        final int[] rectangle = { 2, 3, 4, 5 };
        String fault = "";
        boolean horizontal, dirty;
        int origin = 20, length = 60, distance;

        Model() {
            super("canvas-overlap-test", 0, 0, 0, 0);
        }

        void failAt(String stage) {
            if (fault.equals(stage)) {
                throw failure;
            }
        }

        void pulse() {
            fire(-1, 0);
        }

        @Override
        public int aspects() {
            return 0;
        }

        @Override
        protected void doLayout() {
            // Geometry is supplied explicitly; this is not a View projection test.
        }

        @Override
        protected void emit(WebPaint target) {
            // No native pixel oracle is requested.
        }

        @Override
        public int takeBlit() {
            trace.add("blit");
            failAt("blit");
            int value = distance;
            distance = 0;
            return value;
        }

        @Override
        public boolean scrollsHorizontally() {
            trace.add("axis");
            failAt("axis");
            return horizontal;
        }

        @Override
        public int viewOrigin() {
            trace.add("origin");
            failAt("origin");
            return origin;
        }

        @Override
        public int viewLength() {
            trace.add("length");
            failAt("length");
            return length;
        }

        @Override
        public boolean takeDamage(int[] output) {
            trace.add("damage");
            failAt("damage");
            damageAliases.add(output);
            if (!dirty) {
                return false;
            }
            System.arraycopy(rectangle, 0, output, 0, rectangle.length);
            dirty = false;
            return true;
        }

        @Override
        public boolean isAnimating() {
            trace.add("anim");
            return false;
        }
    }

    private static final class Probe extends WebCanvas<Model> {
        final List<int[]> copies = new ArrayList<>();
        final List<int[]> redraws = new ArrayList<>();
        boolean recording;

        Probe(Composite parent, Model supplied) {
            super(parent, SWT.NONE, supplied);
        }

        @Override
        public Point getSize() {
            if (recording) {
                model.trace.add("size");
                model.failAt("size");
            }
            return super.getSize();
        }

        @Override
        public void scroll(int destX, int destY, int x, int y, int width, int height, boolean all) {
            if (recording) {
                model.trace.add("scroll");
                copies.add(new int[] { destX, destY, x, y, width, height, all ? 1 : 0 });
                model.failAt("scroll");
            }
            // Deliberately never call super.scroll: arguments only, no native copy.
        }

        @Override
        public void redraw(int x, int y, int width, int height, boolean all) {
            if (recording) {
                model.trace.add("redraw");
                redraws.add(new int[] { x, y, width, height, all ? 1 : 0 });
                model.failAt("redraw");
            }
            // Deliberately retain the request in the fixture only.
        }

        void part(boolean visible) {
            setPartVisible(visible);
        }
    }

    @Before
    public void createReceiver() {
        display = Display.getCurrent();
        ownsDisplay = display == null;
        if (ownsDisplay) {
            display = new Display();
        }
        shell = new Shell(display);
        model = new Model();
        canvas = new Probe(shell, model);
        canvas.setSize(100, 80);
        canvas.recording = true;
        given(false, 20, 60, 0, false);
    }

    @After
    public void closeOwnedReceiver() {
        if (canvas != null) {
            canvas.recording = false;
        }
        if (shell != null && !shell.isDisposed()) {
            shell.dispose();
        }
        if (ownsDisplay && display != null && !display.isDisposed()) {
            display.dispose();
        }
    }

    private void given(boolean horizontal, int origin, int length, int distance, boolean dirty) {
        model.trace.clear();
        model.damageAliases.clear();
        canvas.copies.clear();
        canvas.redraws.clear();
        model.fault = "";
        model.horizontal = horizontal;
        model.origin = origin;
        model.length = length;
        model.distance = distance;
        model.dirty = dirty;
    }

    private void trace(String... stages) {
        assertEquals(Arrays.asList(stages), model.trace);
    }

    private void overlap(boolean horizontal, int origin, int length, int distance) {
        given(horizontal, origin, length, distance, true);
        model.pulse();
        assertEquals(1, canvas.copies.size());
        int[] actual = canvas.copies.get(0);
        long magnitude = Math.abs((long) distance);
        assertTrue("valid short-shift domain", magnitude > 0 && magnitude < length);
        long source = (long) origin + Math.max(0L, -(long) distance);
        long span = (long) length - magnitude;
        long destination = source + distance;
        long end = (long) origin + length;
        assertTrue("representable viewport", origin >= 0 && end <= Integer.MAX_VALUE);
        int[] expected = horizontal
                ? new int[] { Math.toIntExact(destination), 0, Math.toIntExact(source), 0,
                        Math.toIntExact(span), 80, 0 }
                : new int[] { 0, Math.toIntExact(destination), 0, Math.toIntExact(source),
                        100, Math.toIntExact(span), 0 };
        assertArrayEquals(expected, actual);
        int axis = horizontal ? 0 : 1;
        int extent = horizontal ? 4 : 5;
        assertTrue("fixed leading strip is untouched", actual[axis] >= origin);
        assertTrue("fixed trailing strip is untouched", (long) actual[axis] + actual[extent] <= end);
        assertTrue("source starts within viewport", actual[axis + 2] >= origin);
        assertTrue("source ends within viewport", (long) actual[axis + 2] + actual[extent] <= end);
        trace("blit", "size", "axis", "origin", "length", "scroll", "damage", "redraw", "anim");
        assertArrayEquals(new int[] { 2, 3, 4, 5, 0 }, canvas.redraws.get(0));
        assertEquals(0, model.distance);
        assertFalse(model.dirty);
    }

    @Test
    public void verticalUpPreservesFixedStrips() {
        overlap(false, 20, 60, -10);
    }

    @Test
    public void verticalDownPreservesFixedStrips() {
        overlap(false, 20, 60, 10);
    }

    @Test
    public void horizontalLeftPreservesFixedStrips() {
        overlap(true, 20, 60, -10);
    }

    @Test
    public void horizontalRightPreservesFixedStrips() {
        overlap(true, 20, 60, 10);
    }

    @Test
    public void unitAndAlmostFullShiftsStayInsideViewport() {
        for (boolean horizontal : new boolean[] { false, true }) {
            for (int distance : new int[] { -1, 1, -59, 59 }) {
                overlap(horizontal, 20, 60, distance);
            }
        }
    }

    @Test
    public void fullClientViewportCopiesOnlyOverlap() {
        for (int distance : new int[] { -10, 10 }) {
            overlap(false, 0, 80, distance);
            overlap(true, 0, 100, distance);
        }
    }

    @Test
    public void representableNearMaxOriginDoesNotWrap() {
        // Injected coordinate arguments only; no huge native widget or bitmap.
        for (boolean horizontal : new boolean[] { false, true }) {
            overlap(horizontal, Integer.MAX_VALUE - 60, 60, -1);
            overlap(horizontal, Integer.MAX_VALUE - 60, 60, 1);
        }
    }

    @Test
    public void zeroBlitSkipsGeometryAndDrainsDamage() {
        given(false, 20, 60, 0, true);
        model.pulse();
        trace("blit", "damage", "redraw", "anim");
        assertTrue(canvas.copies.isEmpty());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 0 }, canvas.redraws.get(0));
        assertFalse(model.dirty);
    }

    @Test
    public void successfulBlitPreservesGetterAndDamageOrder() {
        given(false, 20, 60, -10, true);
        model.pulse();
        trace("blit", "size", "axis", "origin", "length", "scroll", "damage", "redraw", "anim");
        assertEquals(1, canvas.copies.size());
        assertEquals(1, canvas.redraws.size());
        assertEquals(0, model.distance);
        assertFalse(model.dirty);
    }

    @Test
    public void sameDamageArrayIsReusedAcrossDeltas() {
        given(false, 20, 60, 0, true);
        model.pulse();
        model.dirty = true;
        model.pulse();
        assertEquals(2, model.damageAliases.size());
        assertSame(model.damageAliases.get(0), model.damageAliases.get(1));
        assertArrayEquals(model.rectangle, model.damageAliases.get(0));
        assertEquals(2, canvas.redraws.size());
    }

    @Test
    public void hiddenDeltaIsDeferredUntilVisible() {
        canvas.part(false);
        given(false, 20, 60, -10, true);
        model.pulse();
        assertTrue(model.trace.isEmpty());
        assertEquals(-10, model.distance);
        assertTrue(model.dirty);
        canvas.part(true);
        trace("blit", "size", "axis", "origin", "length", "scroll", "damage", "redraw", "anim");
        assertEquals(1, canvas.copies.size());
        assertEquals(1, canvas.redraws.size());
        canvas.part(true);
        assertEquals(1, canvas.copies.size());
        assertEquals(1, canvas.redraws.size());
    }

    @Test
    public void disposedDeltaDoesNotConsumeModel() {
        canvas.dispose();
        given(false, 20, 60, -10, true);
        model.pulse();
        assertTrue(model.trace.isEmpty());
        assertEquals(-10, model.distance);
        assertTrue(model.dirty);
        assertTrue(canvas.copies.isEmpty());
        assertTrue(canvas.redraws.isEmpty());
    }

    @Test
    public void noDamageSkipsRedrawAndStillChecksAnimation() {
        given(true, 20, 60, -10, false);
        model.pulse();
        trace("blit", "size", "axis", "origin", "length", "scroll", "damage", "anim");
        assertEquals(1, canvas.copies.size());
        assertTrue(canvas.redraws.isEmpty());
    }

    @Test
    public void getterFailuresKeepConsumedBlitAndStopAtFailure() {
        String[] stages = { "size", "axis", "origin", "length" };
        for (int i = 0; i < stages.length; i++) {
            given(false, 20, 60, -10, true);
            model.fault = stages[i];
            try {
                model.pulse();
                fail("expected getter failure");
            } catch (MarkerFailure expected) {
                assertSame(model.failure, expected);
            }
            String[] prefix = Arrays.copyOf(new String[] { "blit", "size", "axis", "origin", "length" }, i + 2);
            trace(prefix);
            assertEquals(0, model.distance);
            assertTrue(model.dirty);
            assertTrue(canvas.copies.isEmpty());
            assertTrue(canvas.redraws.isEmpty());
        }
    }

    @Test
    public void scrollFailureLeavesDamagePendingAndStopsAnimation() {
        given(false, 20, 60, -10, true);
        model.fault = "scroll";
        try {
            model.pulse();
            fail("expected scroll failure");
        } catch (MarkerFailure expected) {
            assertSame(model.failure, expected);
        }
        trace("blit", "size", "axis", "origin", "length", "scroll");
        assertEquals(1, canvas.copies.size());
        assertEquals(0, model.distance);
        assertTrue(model.dirty);
        assertTrue(model.damageAliases.isEmpty());
        assertTrue(canvas.redraws.isEmpty());
    }

    @Test
    public void redrawFailureKeepsDamageConsumedAndStopsAnimation() {
        given(false, 20, 60, 0, true);
        model.fault = "redraw";
        try {
            model.pulse();
            fail("expected redraw failure");
        } catch (MarkerFailure expected) {
            assertSame(model.failure, expected);
        }
        trace("blit", "damage", "redraw");
        assertFalse(model.dirty);
        assertEquals(1, model.damageAliases.size());
        assertEquals(1, canvas.redraws.size());
    }

    private void samplePulse(int distance) {
        given(false, 20, 60, distance, true);
        model.pulse();
    }

    @Test
    public void finiteDispatchObservationHasNoSpeedThreshold() {
        final int warmups = 128, iterations = 512;
        for (int i = 0; i < warmups; i++) {
            samplePulse((i & 1) == 0 ? -1 : 1);
        }
        long[] samples = new long[5];
        for (int sample = 0; sample < samples.length; sample++) {
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                samplePulse((i & 1) == 0 ? -1 : 1);
            }
            samples[sample] = System.nanoTime() - start;
            assertTrue("nonnegative elapsed observation", samples[sample] >= 0);
        }
        System.out.println("PERF\tm3.canvas-overlap-dispatch/v1\twarmups\t" + warmups
                + "\tsamples\t5\titerationsPerSample\t" + iterations
                + "\tsampleNs\t" + Arrays.toString(samples)
                + "\ttiming\tmodel-delta-dispatch-plus-fixture-overhead"
                + "\tspeedThreshold\tNONE\tnativeCopyClaim\tfalse");
        assertEquals(1, canvas.copies.size());
        assertEquals(59, canvas.copies.get(0)[5]);
        assertEquals(0, model.distance);
        assertFalse(model.dirty);
    }
}
