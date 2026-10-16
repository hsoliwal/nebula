// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.widgets.carousel.client;

import com.google.gwt.core.client.EntryPoint;

public final class GeometryEntryPoint implements EntryPoint {
  @Override public void onModuleLoad() {
    publish(GeometryWorkload.lines(), GeometryCorpus.cases().length);
  }

  private static native void publish(String lines, int count) /*-{
    if ($wnd.__carouselGeometryProof) throw new Error("duplicate geometry publication");
    $wnd.__carouselGeometryProof = {lines: lines, count: count};
  }-*/;
}
