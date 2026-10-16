/*******************************************************************************
 * Copyright (c) 2020 Laurent CARON.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Laurent CARON (laurent.caron at gmail dot com) - initial API and
 * implementation
 * Hitesh Soliwal and contributors - GWT Canvas, timer and resource ownership
 *******************************************************************************/
package org.eclipse.nebula.widgets.carousel.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.Timer;

/** The existing image owner expressed directly with GWT browser primitives. */
final class ImageContainer {
  private static final int MAX_SIDE = 4096;
  private static final int MAX_PIXELS = 1_048_576;
  private final Carousel carousel;
  private final Canvas canvas;
  private final Context2d context;
  private ImageElement image;
  private Canvas scrollImage;
  private Timer timer;
  private int width, height, slider = -1;
  private int generation;
  private boolean disposed, pending;
  private int dirtyLeft, dirtyTop, dirtyRight, dirtyBottom;
  private int visibleLeft, visibleTop, visibleRight, visibleBottom;

  ImageContainer(Carousel carousel, int width, int height) {
    validateSize(width, height);
    this.carousel = carousel;
    canvas = Canvas.createIfSupported();
    if (canvas == null) throw new UnsupportedOperationException("Canvas2D is required");
    context = canvas.getContext2d();
    if (context == null) throw new UnsupportedOperationException("Canvas2D context is required");
    resize(width, height);
  }
  Canvas canvas() { return canvas; }
  boolean isAnimating() { return timer != null; }
  int bufferPixels() {
    return scrollImage == null ? 0 : scrollImage.getCoordinateSpaceWidth() * scrollImage.getCoordinateSpaceHeight();
  }
  static void requireReady(ImageElement image) {
    if (image == null || !image.getPropertyBoolean("complete")
        || image.getPropertyInt("naturalWidth") <= 0 || image.getPropertyInt("naturalHeight") <= 0) {
      throw new IllegalArgumentException("ready positive-size image required");
    }
  }
  private static void validateSize(int width, int height) {
    if (width < 0 || height < 0 || width > MAX_SIDE || height > MAX_SIDE
        || (long) width * height > MAX_PIXELS) throw new IllegalArgumentException("viewport budget");
  }
  void resize(int width, int height) {
    validateSize(width, height);
    cancelAnimation();
    this.width = width;
    this.height = height;
    canvas.setCoordinateSpaceWidth(width);
    canvas.setCoordinateSpaceHeight(height);
    canvas.setWidth(width + "px");
    canvas.setHeight(height + "px");
    redraw();
  }
  void setImage(ImageElement image) {
    requireReady(image);
    cancelAnimation();
    this.image = image;
    redraw();
  }
  void moveNext() {
    int next = carousel.selection + 1;
    if (next == carousel.getImages().size()) next = 0;
    move(next, true);
  }
  void movePrevious() {
    int next = carousel.selection - 1;
    if (next == -1) next = carousel.getImages().size() - 1;
    move(next, false);
  }
  void moveTo(int selected) { move(selected, selected > carousel.selection); }
  private void move(int selected, boolean left) {
    ImageElement target = carousel.imageAt(selected);
    slide(target, left);
    // Original order: prepare slide, update selection, synchronously notify listeners.
    carousel.selectedByUser(selected);
  }
  private void slide(ImageElement target, boolean left) {
    requireReady(target);
    if (image != null) requireReady(image);
    cancelAnimation();
    if (image == null || !presentationVisible()) {
      image = target;
      redraw();
      return;
    }
    Canvas buffer = Canvas.createIfSupported();
    if (buffer == null) throw new UnsupportedOperationException("Canvas2D buffer is required");
    buffer.setCoordinateSpaceWidth(width * 2);
    buffer.setCoordinateSpaceHeight(height);
    Context2d bufferContext = buffer.getContext2d();
    bufferContext.setFillStyle("#ffffff");
    bufferContext.fillRect(0, 0, width * 2, height);
    drawFitted(bufferContext, left ? image : target, 0);
    drawFitted(bufferContext, left ? target : image, width);
    scrollImage = buffer;
    slider = left ? 0 : width;
    image = target;
    final int animationWidth = width;
    final int epoch = generation;
    timer = new Timer() {
      @Override public void run() {
        if (disposed || epoch != generation) return;
        if (!presentationVisible()) { cancelAnimation(); return; }
        redraw();
        int step = Math.max(1, animationWidth / 5);
        slider += left ? step : -step;
        if ((left && slider >= animationWidth) || (!left && slider <= 0)) {
          cancelAnimation();
          redraw();
        } else schedule(50);
      }
    };
    timer.schedule(200);
    redraw();
  }
  private void drawFitted(Context2d target, ImageElement image, int offset) {
    int iw = image.getPropertyInt("naturalWidth"), ih = image.getPropertyInt("naturalHeight");
    int[] fitted = CarouselGeometry.fit(iw, ih, width, height);
    if (fitted[0] > 0 && fitted[1] > 0) target.drawImage(image, 0, 0, iw, ih,
        offset + (width - fitted[0]) / 2, (height - fitted[1]) / 2, fitted[0], fitted[1]);
  }
  void redraw() { redraw(0, 0, width, height); }
  void redraw(int x, int y, int requestedWidth, int requestedHeight) {
    if (disposed || requestedWidth <= 0 || requestedHeight <= 0) return;
    int left = (int) Math.max(0L, Math.min((long) width, x));
    int top = (int) Math.max(0L, Math.min((long) height, y));
    int right = (int) Math.max(0L, Math.min((long) width, (long) x + requestedWidth));
    int bottom = (int) Math.max(0L, Math.min((long) height, (long) y + requestedHeight));
    if (left >= right || top >= bottom) return;
    if (!pending) {
      dirtyLeft = left; dirtyTop = top; dirtyRight = right; dirtyBottom = bottom;
      pending = true;
      Scheduler.get().scheduleDeferred(() -> {
        pending = false;
        if (!disposed) paint();
      });
    } else {
      dirtyLeft = Math.min(dirtyLeft, left); dirtyTop = Math.min(dirtyTop, top);
      dirtyRight = Math.max(dirtyRight, right); dirtyBottom = Math.max(dirtyBottom, bottom);
    }
  }
  private void paint() {
    if (image == null || !presentationVisible()) return;
    int left = Math.max(dirtyLeft, visibleLeft), top = Math.max(dirtyTop, visibleTop);
    int right = Math.min(dirtyRight, visibleRight), bottom = Math.min(dirtyBottom, visibleBottom);
    if (left >= right || top >= bottom) return;
    requireReady(image);
    context.save();
    try {
      context.beginPath(); context.rect(left, top, right - left, bottom - top); context.clip();
      context.setFillStyle("#ffffff"); context.fillRect(left, top, right - left, bottom - top);
      if (slider == -1) drawFitted(context, image, 0);
      else if (scrollImage != null) context.drawImage(scrollImage.getCanvasElement(),
          slider, 0, width, height, 0, 0, width, height);
    } finally { context.restore(); }
  }
  private boolean presentationVisible() {
    return !disposed && width > 0 && height > 0 && visibleClip(canvas.getElement(), this);
  }
  void suspend() { cancelAnimation(); }
  private void cancelAnimation() {
    generation++;
    if (timer != null) timer.cancel();
    timer = null;
    slider = -1;
    if (scrollImage != null) {
      scrollImage.setCoordinateSpaceWidth(0);
      scrollImage.setCoordinateSpaceHeight(0);
      scrollImage = null;
    }
  }
  void dispose() {
    if (disposed) return;
    disposed = true;
    cancelAnimation();
    image = null;
    canvas.setCoordinateSpaceWidth(0);
    canvas.setCoordinateSpaceHeight(0);
  }

  /** Browser binding for effective visibility and ancestor/window clip, in unscaled CSS pixels. */
  private static native boolean visibleClip(Element node, ImageContainer owner) /*-{
    if (!$doc.documentElement.contains(node)) return false;
    var r = node.getBoundingClientRect();
    if (r.width <= 0 || r.height <= 0) return false;
    var l = Math.max(0, r.left), t = Math.max(0, r.top);
    var rr = Math.min($doc.documentElement.clientWidth, r.right);
    var bb = Math.min($doc.documentElement.clientHeight, r.bottom);
    for (var e = node; e; e = e.parentElement) {
      var s = $wnd.getComputedStyle(e);
      if (s.display === "none" || s.visibility === "hidden" || s.visibility === "collapse"
          || Number(s.opacity) === 0 || (s.transform && s.transform !== "none")) return false;
      if (e !== node) {
        var p = e.getBoundingClientRect();
        if (s.overflowX !== "visible") {
          l = Math.max(l, p.left + e.clientLeft); rr = Math.min(rr, p.left + e.clientLeft + e.clientWidth);
        }
        if (s.overflowY !== "visible") {
          t = Math.max(t, p.top + e.clientTop); bb = Math.min(bb, p.top + e.clientTop + e.clientHeight);
        }
      }
    }
    if (l >= rr || t >= bb) return false;
    owner.@org.eclipse.nebula.widgets.carousel.client.ImageContainer::visibleLeft = Math.max(0, Math.floor(l - r.left));
    owner.@org.eclipse.nebula.widgets.carousel.client.ImageContainer::visibleTop = Math.max(0, Math.floor(t - r.top));
    owner.@org.eclipse.nebula.widgets.carousel.client.ImageContainer::visibleRight = Math.ceil(rr - r.left);
    owner.@org.eclipse.nebula.widgets.carousel.client.ImageContainer::visibleBottom = Math.ceil(bb - r.top);
    return true;
  }-*/;
}
