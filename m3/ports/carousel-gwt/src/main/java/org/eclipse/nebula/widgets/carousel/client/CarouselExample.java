// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.eclipse.nebula.widgets.carousel.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import java.util.ArrayList;
import java.util.List;

/** A self-contained caller of the explicit GWT browser source profile. */
public final class CarouselExample implements EntryPoint {
  private final List<ImageElement> images = new ArrayList<>();
  private final Label status = new Label("Loading the two local example images");
  private int attempts;
  @Override public void onModuleLoad() {
    RootPanel.get("carousel-status").add(status);
    for (String name : new String[] {"first.svg", "second.svg"}) {
      ImageElement image = Document.get().createImageElement();
      image.setSrc(GWT.getHostPageBaseURL() + "assets/" + name);
      images.add(image);
    }
    ready();
  }
  private void ready() {
    for (ImageElement image : images) {
      if (!image.getPropertyBoolean("complete") || image.getPropertyInt("naturalWidth") <= 0) {
        if (attempts++ >= 200) { status.setText("Local image assets could not be loaded"); return; }
        new Timer() { @Override public void run() { ready(); } }.schedule(25);
        return;
      }
    }
    try {
      Carousel carousel = new Carousel(320, 180);
      carousel.setImages(images);
      carousel.addSelectionHandler(event -> status.setText("Selected image " + (event.getSelectedItem() + 1)));
      RootPanel.get("carousel-example").add(carousel);
      Button previous = new Button("Previous");
      previous.addClickHandler(event -> carousel.previous());
      Button next = new Button("Next");
      next.addClickHandler(event -> carousel.next());
      RootPanel.get("carousel-controls").add(previous);
      RootPanel.get("carousel-controls").add(next);
      status.setText("Selected image 1. Arrow keys also navigate the focused canvas.");
    } catch (UnsupportedOperationException error) {
      status.setText("This browser requires Canvas2D: " + error.getMessage());
    }
  }
}
