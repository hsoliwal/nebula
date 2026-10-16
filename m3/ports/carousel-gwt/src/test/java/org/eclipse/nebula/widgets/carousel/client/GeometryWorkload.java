// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.widgets.carousel.client;

/** Both execution lanes call the actual production geometry owner. */
public final class GeometryWorkload {
  private GeometryWorkload() {}

  public static String lines() {
    StringBuilder output = new StringBuilder();
    for (int[] c : GeometryCorpus.cases()) {
      int[] fit = CarouselGeometry.fit(c[0], c[1], c[2], c[3]);
      if (fit.length != 2) throw new AssertionError("geometry result shape");
      output.append(c[0]).append('\t').append(c[1]).append('\t')
          .append(c[2]).append('\t').append(c[3]).append('\t')
          .append(fit[0]).append('\t').append(fit[1]).append('\t')
          .append((c[2] - fit[0]) / 2).append('\t')
          .append((c[3] - fit[1]) / 2).append('\n');
    }
    return output.toString();
  }
}
