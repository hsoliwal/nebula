// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.eclipse.nebula.widgets.carousel.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;
import java.util.ArrayList;
import java.util.List;

/** Runs against an actual browser Canvas and Timer; no DOM or drawing doubles. */
public final class CarouselBrowserContract implements EntryPoint {
  private int assertions;
  private int readinessPolls;
  private boolean finished;
  private Carousel carousel;
  private final List<ImageElement> borrowed = new ArrayList<>();
  private final List<Integer> events = new ArrayList<>();

  @Override public void onModuleLoad() {
    GWT.setUncaughtExceptionHandler(this::fail);
    publish("RUNNING", 0, "Waiting for local image assets");
    for (String name : new String[] {"first.svg", "second.svg"}) {
      ImageElement image = Document.get().createImageElement();
      image.setSrc(GWT.getHostPageBaseURL() + "assets/" + name);
      borrowed.add(image);
    }
    pollReady();
  }

  private void pollReady() {
    for (ImageElement image : borrowed) {
      if (!image.getPropertyBoolean("complete") || image.getPropertyInt("naturalWidth") == 0) {
        check(readinessPolls++ < 250, "Local image asset did not load");
        later(this::pollReady, 20);
        return;
      }
    }
    carousel = new Carousel(200, 100);
    RootPanel.get("fixture").add(carousel);
    installCounter(carousel.imageContainer.canvas().getCanvasElement());
    carousel.addSelectionHandler(event -> {
      check(event.getSelectedItem() == carousel.getSelection(), "Listener sees new selection");
      events.add(event.getSelectedItem());
    });
    carousel.setImages(borrowed);
    check(carousel.getImages() == borrowed, "Borrowed list identity");
    check(carousel.getImages().get(0) == borrowed.get(0), "Borrowed image identity");
    carousel.setSelection(1);
    carousel.setSelection(0);
    check(events.isEmpty(), "Programmatic selection has no user event");
    try { carousel.setSelection(-1); throw new AssertionError("Invalid selection accepted"); }
    catch (IllegalArgumentException expected) { check(carousel.getSelection() == 0, "Selection refusal atomic"); }
    try { carousel.resize(-1, 100); throw new AssertionError("Invalid viewport accepted"); }
    catch (IllegalArgumentException expected) { check(carousel.imageContainer.canvas().getCoordinateSpaceWidth() == 200, "Viewport refusal atomic"); }
    later(this::checkInitialPixels, 40);
  }

  private void checkInitialPixels() {
    CanvasElement canvas = carousel.imageContainer.canvas().getCanvasElement();
    check(pixel(canvas, 100, 50) == 0xcc3322ff, "Actual centered red image pixel");
    check(pixel(canvas, 0, 0) == 0xffffffff, "Actual unscaled image white margin");
    check(draws(canvas) > 0, "Actual Canvas operations observed");
    clear(canvas);
    carousel.redraw(0, 0, 10, 10);
    later(() -> {
      check(pixel(canvas, 0, 0) == 0xffffffff, "Damage rectangle repainted");
      check((pixel(canvas, 100, 50) & 255) == 0, "Outside damage stays untouched");
      carousel.refresh();
      carousel.next();
      check(carousel.getSelection() == 1 && events.get(events.size() - 1) == 1,
          "Next updates single owner before notifying");
      check(carousel.imageContainer.bufferPixels() == 40_000, "Two finite viewport buffer");
      carousel.previous();
      check(carousel.getSelection() == 0, "Overlapping opposite slide owns final selection");
      waitSettled(this::checkOverlap, 0);
    }, 40);
  }

  private void waitSettled(Runnable next, int polls) {
    check(polls < 200, "Animation terminates");
    if (carousel.imageContainer.isAnimating()) later(() -> waitSettled(next, polls + 1), 20);
    else later(next, 40);
  }

  private void checkOverlap() {
    CanvasElement canvas = carousel.imageContainer.canvas().getCanvasElement();
    check(pixel(canvas, 100, 50) == 0xcc3322ff, "Canceled slide cannot overwrite final image");
    check(carousel.imageContainer.bufferPixels() == 0, "Completed buffer released");
    carousel.previous();
    check(carousel.getSelection() == 1, "Previous wraps at first image");
    carousel.next();
    check(carousel.getSelection() == 0, "Next wraps at last image");
    carousel.setVisible(false);
    int before = draws(canvas);
    carousel.next();
    check(carousel.getSelection() == 1, "Hidden navigation preserves CPU selection");
    check(carousel.imageContainer.bufferPixels() == 0, "Hidden navigation allocates no animation buffer");
    later(() -> {
      check(draws(canvas) == before, "Hidden navigation issues zero Canvas operations");
      carousel.setVisible(true);
      carousel.refresh();
      later(this::checkRevealAndDispose, 40);
    }, 600);
  }

  private void checkRevealAndDispose() {
    CanvasElement canvas = carousel.imageContainer.canvas().getCanvasElement();
    check(pixel(canvas, 100, 50) == 0x2244ccff, "Reveal paints selected image");
    carousel.getElement().getStyle().setProperty("position", "absolute");
    carousel.getElement().getStyle().setProperty("left", "-5000px");
    int before = draws(canvas);
    carousel.refresh();
    later(() -> {
      check(draws(canvas) == before, "Outside-window owner issues zero Canvas operations");
      carousel.getElement().getStyle().clearProperty("position");
      carousel.getElement().getStyle().clearProperty("left");
      carousel.resize(0, 0);
      int empty = draws(canvas);
      carousel.next();
      check(draws(canvas) == empty && carousel.imageContainer.bufferPixels() == 0,
          "Empty owner does not paint or allocate buffer");
      carousel.resize(200, 100);
      carousel.previous();
      carousel.dispose();
      int disposed = draws(canvas);
      check(carousel.isDisposed(), "Dispose retires owner");
      check(carousel.imageContainer.bufferPixels() == 0 && !carousel.imageContainer.isAnimating(),
          "Dispose cancels timer and releases owned buffer");
      later(() -> {
        check(draws(canvas) == disposed, "No timer or deferred repaint after dispose");
        for (ImageElement image : borrowed) check(image.getPropertyBoolean("complete")
            && image.getPropertyInt("naturalWidth") == 80, "Caller image survives dispose");
        finished = true;
        publish("PASS", assertions, "Real Canvas, image, event and timer contracts passed");
      }, 700);
    }, 40);
  }

  private void later(Runnable action, int delay) {
    new Timer() { @Override public void run() {
      if (finished) return;
      try { action.run(); } catch (Throwable error) { fail(error); }
    } }.schedule(delay);
  }
  private void check(boolean value, String message) {
    assertions++;
    if (!value) throw new AssertionError(message);
  }
  private void fail(Throwable error) {
    if (finished) return;
    finished = true;
    if (carousel != null) carousel.dispose();
    publish("FAIL", assertions, String.valueOf(error));
  }
  private static native void installCounter(CanvasElement canvas) /*-{
    var c = canvas.getContext("2d"); c.__carouselDraws = 0;
    ["drawImage", "clearRect", "fillRect"].forEach(function(name) {
      var original = c[name]; c[name] = function() {
        c.__carouselDraws++; return original.apply(c, arguments);
      };
    });
  }-*/;
  private static native int draws(CanvasElement canvas) /*-{
    return canvas.getContext("2d").__carouselDraws | 0;
  }-*/;
  private static native void clear(CanvasElement canvas) /*-{
    canvas.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
  }-*/;
  private static native int pixel(CanvasElement canvas, int x, int y) /*-{
    var d = canvas.getContext("2d").getImageData(x, y, 1, 1).data;
    return (d[0] << 24) | (d[1] << 16) | (d[2] << 8) | d[3];
  }-*/;
  private static native void publish(String status, int assertions, String message) /*-{
    $wnd.carouselContract = {status: status, assertions: assertions, message: message};
    $doc.title = "Carousel contract " + status;
    var out = $doc.getElementById("results");
    if (out) out.textContent = JSON.stringify($wnd.carouselContract);
  }-*/;
}
