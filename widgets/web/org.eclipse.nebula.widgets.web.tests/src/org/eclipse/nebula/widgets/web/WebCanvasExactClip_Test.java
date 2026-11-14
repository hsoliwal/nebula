// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.eclipse.nebula.widgets.web.model.WebModel;
import org.eclipse.nebula.widgets.web.model.WebPaint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Actual native Image/GC paint dispatch. No Shell.open and no SCREEN claim. */
public class WebCanvasExactClip_Test {
    private static final int W = 128, H = 96, INK = 0x123456, LATER = 0xcd3625;
    private static final RGB SENTINEL = new RGB(0, 255, 255);
    private Display display;
    private boolean ownsDisplay;
    private Shell shell;
    private Model model;
    private Probe canvas;

    private static final class MarkerFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class Model extends WebModel {
        int emissions, labels = 1, x = 4, y = 5, w = 112, h = 36;
        int style = 14;
        boolean later;
        String label = "Exact clip";

        Model() {
            super("canvas-exact-clip-test", 0, 0, 0, 0);
        }

        @Override
        public int aspects() {
            return 0;
        }

        @Override
        protected void doLayout() {
            // This fixture owns only painting; it supplies explicit record boxes.
        }

        @Override
        protected void emit(WebPaint target) {
            emissions++;
            for (int i = 0; i < labels; i++) {
                target.text(x, y + i * 8, w, h, INK, style, label);
            }
            if (later) {
                target.rect(0, 0, W, H, LATER);
            }
        }
    }

    private static final class Probe extends WebCanvas<Model> {
        Runnable fontAction;

        Probe(Composite parent, Model value) {
            super(parent, SWT.NONE, value);
        }

        @Override
        public Font getFont() {
            if (fontAction != null) {
                fontAction.run();
            }
            return super.getFont();
        }
    }

    private final class Frame implements AutoCloseable {
        final Image image = new Image(display, W, H);
        final GC gc = new GC(image);

        Frame() {
            gc.setBackground(display.getSystemColor(SWT.COLOR_CYAN));
            gc.fillRectangle(0, 0, W, H);
        }

        @Override
        public void close() {
            if (!gc.isDisposed()) {
                gc.dispose();
            }
            image.dispose();
        }
    }

    @Before
    public void createNativeImageReceiver() {
        display = Display.getCurrent();
        ownsDisplay = display == null;
        if (ownsDisplay) {
            DeviceData tracking = new DeviceData();
            tracking.tracking = true;
            display = new Display(tracking);
        }
        shell = new Shell(display);
        model = new Model();
        canvas = new Probe(shell, model);
        canvas.setSize(W, H);
    }

    @After
    public void closeOwnedReceiver() {
        if (canvas != null) {
            canvas.fontAction = null;
        }
        if (shell != null && !shell.isDisposed()) {
            shell.dispose();
        }
        if (ownsDisplay && display != null && !display.isDisposed()) {
            display.dispose();
        }
    }

    private void paint(GC gc, int x, int y, int w, int h) {
        Event event = new Event();
        event.gc = gc;
        event.x = x;
        event.y = y;
        event.width = w;
        event.height = h;
        canvas.notifyListeners(SWT.Paint, event);
    }

    private void paint(GC gc) {
        paint(gc, 0, 0, W, H);
    }

    private Region hole() {
        Region region = new Region(display);
        region.add(2, 3, 116, 75);
        region.subtract(24, 11, 27, 34);
        region.subtract(76, 3, 10, 58);
        return region;
    }

    private static void sameRegion(Region expected, Region actual) {
        Region difference = new Region(expected.getDevice());
        try {
            difference.add(expected);
            difference.subtract(actual);
            assertTrue("caller clip lost pixels", difference.isEmpty());
            difference.add(actual);
            difference.subtract(expected);
            assertTrue("caller clip gained pixels", difference.isEmpty());
        } finally {
            difference.dispose();
        }
    }

    private Region deviceClip(GC gc) {
        Transform saved = new Transform(display);
        Region result = new Region(display);
        boolean success = false;
        try {
            gc.getTransform(saved);
            try {
                gc.setTransform(null);
                gc.getClipping(result);
            } finally {
                gc.setTransform(saved);
            }
            success = true;
            return result;
        } finally {
            saved.dispose();
            if (!success) {
                result.dispose();
            }
        }
    }

    private void exactClip(GC gc, Region expectedDevice) {
        Region actual = deviceClip(gc);
        try {
            sameRegion(expectedDevice, actual);
        } finally {
            actual.dispose();
        }
    }

    private static RGB pixel(ImageData image, int x, int y) {
        return image.palette.getRGB(image.getPixel(x, y));
    }

    private static void outsideUnchanged(Image image, Region allowedDevice) {
        ImageData data = image.getImageData();
        for (int y = 0; y < data.height; y++) {
            for (int x = 0; x < data.width; x++) {
                if (!allowedDevice.contains(x, y)) {
                    assertEquals("outside incoming device clip: " + x + "," + y,
                            SENTINEL, pixel(data, x, y));
                }
            }
        }
    }

    private void identityCase(boolean later, int eventX, int eventY, int eventW, int eventH) {
        model.later = later;
        Region entry = hole();
        try (Frame f = new Frame()) {
            try {
                f.gc.setClipping(entry);
                assertTrue(f.gc.isClipped());
                paint(f.gc, eventX, eventY, eventW, eventH);
                exactClip(f.gc, entry);
                assertTrue(f.gc.isClipped());
                assertFalse(f.gc.isDisposed());
                outsideUnchanged(f.image, entry);
                if (later) {
                    assertEquals(new RGB(0xcd, 0x36, 0x25), pixel(f.image.getImageData(), 8, 8));
                }
            } finally {
                entry.dispose();
            }
        }
    }

    @Test
    public void disjointClipConfinesTextAndFollowingPrimitive() {
        identityCase(true, 0, 0, W, H);
    }

    @Test
    public void textBoundsIntersectTheExactIncomingRegion() {
        identityCase(false, 0, 0, W, H);
    }

    @Test
    public void emptyIncomingClipRemainsEmpty() {
        Region empty = new Region(display);
        try (Frame f = new Frame()) {
            try {
                f.gc.setClipping(empty);
                paint(f.gc);
                exactClip(f.gc, empty);
                assertTrue(f.gc.isClipped());
                outsideUnchanged(f.image, empty);
            } finally {
                empty.dispose();
            }
        }
    }

    @Test
    public void originallyUnclippedStateRemainsUnclipped() {
        try (Frame f = new Frame()) {
            f.gc.setClipping((Region) null);
            assertFalse(f.gc.isClipped());
            paint(f.gc);
            assertFalse(f.gc.isClipped());
            assertFalse(f.gc.isDisposed());
        }
    }

    @Test
    public void narrowEventRectangleDoesNotReplaceCallerRegion() {
        identityCase(false, 7, 9, 12, 10);
    }

    @Test
    public void zeroTextBoundsRestoreTheEntryRegion() {
        model.w = 0;
        model.h = 0;
        identityCase(false, 0, 0, W, H);
    }

    private void expectedLabel(GC gc, Region entry, Font font, int style) {
        gc.setAntialias(SWT.ON);
        org.eclipse.swt.graphics.Color savedBackground = gc.getBackground();
        org.eclipse.swt.graphics.Color savedForeground = gc.getForeground();
        org.eclipse.swt.graphics.Color background = new org.eclipse.swt.graphics.Color(display,
                model.theme().slot(0) >> 16 & 255,
                model.theme().slot(0) >> 8 & 255,
                model.theme().slot(0) & 255);
        org.eclipse.swt.graphics.Color ink = null;
        Region clipped = null;
        try {
            ink = new org.eclipse.swt.graphics.Color(display, 0x12, 0x34, 0x56);
            clipped = new Region(display);
            gc.setBackground(background);
            gc.setClipping(entry);
            gc.fillRectangle(0, 0, W, H);
            gc.setFont(font);
            gc.setForeground(ink);
            Point ext = gc.textExtent(model.label);
            int x = (style & WebPaint.CENTER) != 0 ? model.x + (model.w - ext.x) / 2
                    : (style & WebPaint.RIGHT) != 0 ? model.x + model.w - ext.x : model.x;
            int y = model.y + (model.h - ext.y) / 2;
            clipped.add(entry);
            clipped.intersect(model.x, model.y, model.w, model.h);
            gc.setClipping(clipped);
            gc.drawText(model.label, x, y, true);
            if ((style & WebPaint.UNDERLINE) != 0) {
                gc.drawLine(x, y + ext.y - 1, x + ext.x, y + ext.y - 1);
            }
        } finally {
            gc.setClipping(entry);
            gc.setBackground(savedBackground);
            gc.setForeground(savedForeground);
            if (clipped != null) {
                clipped.dispose();
            }
            if (ink != null) {
                ink.dispose();
            }
            background.dispose();
        }
    }

    @Test
    public void alignmentAndUnderlineMatchClippedNativeTwin() {
        for (int flags : new int[] { 0, WebPaint.CENTER, WebPaint.RIGHT,
                WebPaint.CENTER | WebPaint.UNDERLINE }) {
            model.style = 14 | flags;
            Region entry = hole();
            try (Frame actual = new Frame(); Frame expected = new Frame()) {
                try {
                    actual.gc.setClipping(entry);
                    paint(actual.gc);
                    expectedLabel(expected.gc, entry, actual.gc.getFont(), model.style);
                    ImageData a = actual.image.getImageData(), e = expected.image.getImageData();
                    for (int y = 0; y < H; y++) {
                        for (int x = 0; x < W; x++) {
                            assertEquals("native twin: " + flags + "," + x + "," + y,
                                    pixel(e, x, y), pixel(a, x, y));
                        }
                    }
                    exactClip(actual.gc, entry);
                } finally {
                    entry.dispose();
                }
            }
        }
    }

    private void transformed(float[] matrix) {
        Region entry = hole();
        Transform transform = new Transform(display, matrix);
        Transform after = new Transform(display);
        try (Frame f = new Frame()) {
            Region before = null;
            try {
                f.gc.setClipping(entry);
                f.gc.setTransform(transform);
                before = deviceClip(f.gc);
                paint(f.gc);
                f.gc.getTransform(after);
                float[] elements = new float[6];
                after.getElements(elements);
                assertArrayEquals(matrix, elements, 0.0001f);
                exactClip(f.gc, before);
                assertTrue(f.gc.isClipped());
                outsideUnchanged(f.image, before);
            } finally {
                if (before != null) {
                    before.dispose();
                }
                after.dispose();
                transform.dispose();
                entry.dispose();
            }
        }
    }

    @Test
    public void integerTranslationPreservesExactDeviceClip() {
        transformed(new float[] { 1, 0, 0, 1, 5, 7 });
    }

    @Test
    public void affineMatricesPreserveExactDeviceClip() {
        for (float[] matrix : new float[][] {
                { 1.25f, 0, 0, 0.75f, 3.5f, 2.25f },
                { 1, 0.25f, 0.125f, 1, 4, 3 },
                { 0.8f, 0.6f, -0.6f, 0.8f, 24, 2 },
                { -1, 0, 0, 1, 120, 0 } }) {
            transformed(matrix);
        }
    }

    @Test
    public void everyLabelCapturesClipAfterItsFontOverride() {
        model.labels = 2;
        Region first = new Region(display), second = new Region(display);
        first.add(4, 5, 28, 22);
        second.add(54, 12, 29, 23);
        int[] calls = { 0 };
        try (Frame f = new Frame()) {
            try {
                canvas.fontAction = () -> f.gc.setClipping(calls[0]++ == 0 ? first : second);
                paint(f.gc);
                assertEquals(2, calls[0]);
                exactClip(f.gc, second);
                assertTrue(f.gc.isClipped());
            } finally {
                canvas.fontAction = null;
                second.dispose();
                first.dispose();
            }
        }
    }

    @Test
    public void fontOverrideFailureKeepsItsEarlyClipEffect() {
        MarkerFailure failure = new MarkerFailure();
        Region changed = new Region(display);
        changed.add(8, 9, 17, 19);
        try (Frame f = new Frame()) {
            try {
                canvas.fontAction = () -> {
                    f.gc.setClipping(changed);
                    throw failure;
                };
                try {
                    paint(f.gc);
                    fail("expected original font hook failure");
                } catch (MarkerFailure actual) {
                    assertSame(failure, actual);
                }
                exactClip(f.gc, changed);
                assertFalse(f.gc.isDisposed());
                assertEquals(1, model.emissions);
            } finally {
                canvas.fontAction = null;
                changed.dispose();
            }
        }
    }

    @Test
    public void nullPaintGCFailsBeforeModelEmission() {
        try {
            paint(null);
            fail("expected original null GC failure");
        } catch (NullPointerException expected) {
            assertEquals(0, model.emissions);
        }
    }

    @Test
    public void disposedPaintGCFailsBeforeModelEmission() {
        try (Frame f = new Frame()) {
            f.gc.dispose();
            try {
                paint(f.gc);
                fail("expected original disposed GC failure");
            } catch (SWTException expected) {
                assertEquals(SWT.ERROR_GRAPHIC_DISPOSED, expected.code);
                assertEquals(0, model.emissions);
            }
        }
    }

    private Set<Object> trackedGraphics() {
        DeviceData data = display.getDeviceData();
        assertTrue("actual Device tracking is required, never skipped", data.tracking);
        Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object value : data.objects) {
            if (value instanceof Resource resource && !resource.isDisposed()) {
                result.add(value);
            }
        }
        return result;
    }

    private void assertGraphics(Set<Object> expected) {
        Set<Object> actual = trackedGraphics();
        if (expected.equals(actual)) {
            return;
        }
        StringBuilder message = new StringBuilder("live graphics expected=")
                .append(expected.size()).append(" actual=").append(actual.size());
        DeviceData data = display.getDeviceData();
        int reported = 0;
        for (int i = 0; i < data.objects.length && reported < 3; i++) {
            Object value = data.objects[i];
            if (value != null && actual.contains(value) && !expected.contains(value)) {
                message.append("\nADDED ").append(value.getClass().getName());
                if (data.errors != null && i < data.errors.length && data.errors[i] != null) {
                    StackTraceElement[] trace = data.errors[i].getStackTrace();
                    for (int j = 0; j < Math.min(10, trace.length); j++) {
                        message.append("\n  ").append(trace[j]);
                    }
                }
                reported++;
            }
        }
        fail(message.toString());
    }

    private static boolean recordedGCCopy(Object value, Throwable allocation) {
        if (allocation == null) {
            return false;
        }
        boolean regionCopy = false, clippingOperation = false, transformOperation = false;
        for (StackTraceElement frame : allocation.getStackTrace()) {
            if (frame.getClassName().equals("org.eclipse.swt.graphics.Region")
                    && frame.getMethodName().equals("copy")) {
                regionCopy = true;
            }
            if (frame.getClassName().equals("org.eclipse.swt.graphics.GC$SetClippingRegionOperation")
                    && frame.getMethodName().equals("<init>")) {
                clippingOperation = true;
            }
            if (frame.getClassName().equals("org.eclipse.swt.graphics.GC$SetTransformOperation")
                    && frame.getMethodName().equals("<init>")) {
                transformOperation = true;
            }
        }
        return value instanceof Region && regionCopy && clippingOperation
                || value instanceof Transform && transformOperation;
    }

    private Set<Object> graphicsByOwner(boolean gcCopies) {
        DeviceData data = display.getDeviceData();
        assertTrue("actual Device tracking is required, never skipped", data.tracking);
        Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < data.objects.length; i++) {
            Object value = data.objects[i];
            if (value instanceof Resource resource && !resource.isDisposed()) {
                Throwable allocation = data.errors != null && i < data.errors.length
                        ? data.errors[i] : null;
                // Unknown resources and absent stacks remain application-owned.
                if (recordedGCCopy(value, allocation) == gcCopies) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private Set<Object> applicationGraphics() {
        return graphicsByOwner(false);
    }

    private void assertApplicationGraphics(Set<Object> expected) {
        assertEquals("Canvas/application-owned live graphics", expected, applicationGraphics());
    }

    private void assertDirectResourcesRemainChecked(Set<Object> baseline) {
        Region directRegion = new Region(display);
        Transform directTransform = null;
        try {
            directTransform = new Transform(display);
            Set<Object> actual = applicationGraphics();
            assertTrue("direct Region must not be classified as GC-owned", actual.contains(directRegion));
            assertTrue("direct Transform must not be classified as GC-owned", actual.contains(directTransform));
            assertFalse("new direct resources must be detected", baseline.equals(actual));
        } finally {
            if (directTransform != null) {
                directTransform.dispose();
            }
            directRegion.dispose();
        }
        assertApplicationGraphics(baseline);
    }

    private void callerRetiresRecordedCopies(GC gc, Set<Object> baseline) {
        assertFalse("Canvas must leave the borrowed Image GC live", gc.isDisposed());
        assertApplicationGraphics(baseline);
        Set<Object> copies = graphicsByOwner(true);
        assertFalse("fixture must observe actual recorded GC copies", copies.isEmpty());
        Set<Object> expected = Collections.newSetFromMap(new IdentityHashMap<>());
        expected.addAll(baseline);
        assertTrue("caller GC was tracked in the baseline", expected.remove(gc));
        // The caller retires its GC. Never dispose SWT's copies directly.
        gc.dispose();
        assertTrue(gc.isDisposed());
        for (Object value : copies) {
            assertTrue("recorded GC copy survives caller disposal: " + value.getClass().getName(),
                    ((Resource) value).isDisposed());
        }
        assertApplicationGraphics(expected);
        System.out.println("RESOURCE\tm3.image-gc-recorded-ownership/v1\tretiredCopies\t" + copies.size());
    }

    private static Object field(Object target, String name) throws Exception {
        Field value = target.getClass().getDeclaredField(name);
        value.setAccessible(true);
        return value.get(target);
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method result = type.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }

    @Test
    public void paintClipLazilyReusesThreeOwnedSnapshotsAndDisposesThem() throws Exception {
        Class<?> type = Class.forName(WebCanvas.class.getName() + "$PaintClip",
                true, WebCanvas.class.getClassLoader());
        Constructor<?> constructor = type.getDeclaredConstructor(GC.class);
        constructor.setAccessible(true);
        Method apply = method(type, "apply", int.class, int.class, int.class, int.class);
        Method restore = method(type, "restore"), dispose = method(type, "dispose");
        Region entry = hole();
        try (Frame f = new Frame()) {
            Object clipping = null;
            try {
                f.gc.setClipping(entry);
                Set<Object> before = applicationGraphics();
                assertDirectResourcesRemainChecked(before);
                clipping = constructor.newInstance(f.gc);
                assertNull(field(clipping, "originalClip"));
                assertNull(field(clipping, "workingClip"));
                assertNull(field(clipping, "originalTransform"));
                assertApplicationGraphics(before);
                apply.invoke(clipping, 4, 5, 80, 36);
                Object saved = field(clipping, "originalClip");
                Object working = field(clipping, "workingClip");
                Object transform = field(clipping, "originalTransform");
                assertTrue(saved instanceof Region);
                assertTrue(working instanceof Region);
                assertTrue(transform instanceof Transform);
                restore.invoke(clipping);
                exactClip(f.gc, entry);
                Set<Object> during = applicationGraphics();
                during.removeAll(before);
                assertEquals(3, during.size());
                assertTrue(during.contains(saved));
                assertTrue(during.contains(working));
                assertTrue(during.contains(transform));
                for (int i = 0; i < 120; i++) {
                    apply.invoke(clipping, 4 + i % 2, 5, 80, 36);
                    restore.invoke(clipping);
                    assertSame(saved, field(clipping, "originalClip"));
                    assertSame(working, field(clipping, "workingClip"));
                    assertSame(transform, field(clipping, "originalTransform"));
                }
                dispose.invoke(clipping);
                dispose.invoke(clipping);
                assertTrue(((Region) saved).isDisposed());
                assertTrue(((Region) working).isDisposed());
                assertTrue(((Transform) transform).isDisposed());
                assertApplicationGraphics(before);
                exactClip(f.gc, entry); // Last GC mutation before recording all live copies.
                assertFalse(f.gc.isDisposed());
                callerRetiresRecordedCopies(f.gc, before);
            } finally {
                if (clipping != null) {
                    dispose.invoke(clipping);
                }
                entry.dispose();
            }
        }
    }

    @Test
    public void noTextFramesRetainNoNewNativeGraphicsResources() {
        model.labels = 0;
        model.later = true;
        try (Frame f = new Frame()) {
            paint(f.gc);
            Set<Object> before = trackedGraphics();
            for (int i = 0; i < 120; i++) {
                paint(f.gc);
                assertGraphics(before);
            }
        }
    }

    @Test
    public void warmCache120FramesRespectCanvasAndRecordedGCOwnership() {
        model.labels = 3;
        Region entry = hole();
        try (Frame f = new Frame()) {
            try {
                f.gc.setClipping(entry);
                paint(f.gc); // Admit only the widget's existing lazy color/font cache.
                Set<Object> before = applicationGraphics();
                assertDirectResourcesRemainChecked(before);
                for (int i = 0; i < 120; i++) {
                    f.gc.setClipping(entry);
                    paint(f.gc);
                    assertApplicationGraphics(before);
                    assertFalse(f.gc.isDisposed());
                }
                // This ownership control does not turn into another pixel assertion.
                callerRetiresRecordedCopies(f.gc, before);
            } finally {
                entry.dispose();
            }
        }
    }

    @Test
    public void repeatedPaintsRestoreExactClipAndKeepBorrowedGCLive() {
        Region entry = hole();
        try (Frame f = new Frame()) {
            try {
                for (int i = 0; i < 4; i++) {
                    f.gc.setClipping(entry);
                    paint(f.gc);
                    exactClip(f.gc, entry);
                    assertFalse(f.gc.isDisposed());
                }
            } finally {
                entry.dispose();
            }
        }
    }

    @Test
    public void finitePaintObservationHasNoSpeedThreshold() {
        final int warmups = 8, iterations = 16;
        Region entry = hole();
        try (Frame f = new Frame()) {
            try {
                for (int i = 0; i < warmups; i++) {
                    f.gc.setClipping(entry);
                    paint(f.gc);
                    exactClip(f.gc, entry);
                }
                long[] samples = new long[5];
                for (int sample = 0; sample < samples.length; sample++) {
                    long start = System.nanoTime();
                    for (int i = 0; i < iterations; i++) {
                        f.gc.setClipping(entry);
                        paint(f.gc);
                    }
                    samples[sample] = System.nanoTime() - start;
                    exactClip(f.gc, entry);
                    assertTrue(samples[sample] >= 0);
                }
                System.out.println("PERF\tm3.canvas-exact-clip/v1\twarmups\t" + warmups
                        + "\tsamples\t5\titerationsPerSample\t" + iterations
                        + "\tsampleNs\t" + Arrays.toString(samples)
                        + "\ttiming\tImageGC-paint-plus-fixture-overhead"
                        + "\tspeedThreshold\tNONE\tSCREEN\tfalse");
            } finally {
                entry.dispose();
            }
        }
    }
}
