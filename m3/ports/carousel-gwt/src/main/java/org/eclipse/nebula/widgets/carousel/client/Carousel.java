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
 * Hitesh Soliwal and contributors - explicit GWT browser source profile
 *******************************************************************************/
package org.eclipse.nebula.widgets.carousel.client;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import java.util.ArrayList;
import java.util.List;

/** Browser source profile of the existing Carousel owner; not SWT binary compatibility. */
public final class Carousel extends Composite implements HasSelectionHandlers<Integer> {
  int selection;
  final ImageContainer imageContainer;
  private List<ImageElement> images = new ArrayList<>();
  private boolean disposed;
  private HandlerRegistration resizeHandler;
  private HandlerRegistration scrollHandler;

  public Carousel(int width, int height) {
    imageContainer = new ImageContainer(this, width, height);
    initWidget(imageContainer.canvas());
    setStyleName("nebula-carousel");
    getElement().setAttribute("role", "img");
    getElement().setAttribute("aria-label", "Image carousel");
    imageContainer.canvas().setTabIndex(0);
    imageContainer.canvas().addKeyDownHandler(event -> {
      if (event.getNativeKeyCode() == KeyCodes.KEY_LEFT) previous();
      else if (event.getNativeKeyCode() == KeyCodes.KEY_RIGHT) next();
    });
  }

  /** Caller retains ownership of the list and ready image elements. */
  public void setImages(List<ImageElement> values) {
    checkOpen();
    if (values == null || values.isEmpty()) throw new IllegalArgumentException("nonempty images required");
    for (ImageElement image : values) ImageContainer.requireReady(image);
    images = values;
    selection = 0;
    imageContainer.setImage(values.get(0));
  }
  public List<ImageElement> getImages() { checkOpen(); return images; }
  public int getSelection() { checkOpen(); return selection; }

  /** Like the SWT owner, programmatic selection does not fire a user Selection event. */
  public void setSelection(int selected) {
    checkOpen();
    ImageElement image = imageAt(selected);
    selection = selected;
    imageContainer.setImage(image);
  }
  public void next() { checkOpen(); imageContainer.moveNext(); }
  public void previous() { checkOpen(); imageContainer.movePrevious(); }
  public void moveTo(int selected) { checkOpen(); imageContainer.moveTo(selected); }
  @Override public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler) {
    checkOpen();
    if (handler == null) throw new IllegalArgumentException("selection handler required");
    return addHandler(handler, SelectionEvent.getType());
  }
  void selectedByUser(int selected) {
    selection = selected;
    SelectionEvent.fire(this, selected);
  }
  ImageElement imageAt(int selected) {
    if (selected < 0 || selected >= images.size()) throw new IllegalArgumentException("selection out of range");
    ImageElement image = images.get(selected);
    ImageContainer.requireReady(image);
    return image;
  }

  public void resize(int width, int height) { checkOpen(); imageContainer.resize(width, height); }
  /** Request after an external ancestor visibility/layout change. */
  public void refresh() { checkOpen(); imageContainer.redraw(); }
  public void redraw(int x, int y, int width, int height) {
    checkOpen(); imageContainer.redraw(x, y, width, height);
  }
  @Override public void setVisible(boolean visible) {
    checkOpen();
    super.setVisible(visible);
    if (visible) imageContainer.redraw(); else imageContainer.suspend();
  }
  public boolean isDisposed() { return disposed; }
  public void dispose() {
    if (disposed) return;
    disposed = true;
    removeWindowHandlers();
    imageContainer.dispose();
    removeFromParent();
  }
  @Override protected void onLoad() {
    super.onLoad();
    if (!disposed) {
      resizeHandler = Window.addResizeHandler(event -> imageContainer.redraw());
      scrollHandler = Window.addWindowScrollHandler(event -> imageContainer.redraw());
      imageContainer.redraw();
    }
  }
  @Override protected void onUnload() {
    removeWindowHandlers();
    imageContainer.suspend();
    super.onUnload();
  }
  private void removeWindowHandlers() {
    if (resizeHandler != null) resizeHandler.removeHandler();
    if (scrollHandler != null) scrollHandler.removeHandler();
    resizeHandler = null;
    scrollHandler = null;
  }
  private void checkOpen() {
    if (disposed) throw new IllegalStateException("carousel disposed");
  }
}
