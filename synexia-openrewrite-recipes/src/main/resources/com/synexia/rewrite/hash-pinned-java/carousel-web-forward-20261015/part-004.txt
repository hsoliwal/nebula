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
 *******************************************************************************/
// Pure geometry adapted from the qualified ImageContainer; original attribution retained.
package org.eclipse.nebula.widgets.carousel.client;

public final class CarouselGeometry {
  private CarouselGeometry() {}
  /** Requires positive image dimensions and nonnegative client extents. */
  public static int[] fit(int imageWidth, int imageHeight, int clientWidth, int clientHeight) {
    if (imageWidth <= clientWidth && imageHeight <= clientHeight) {
      return new int[] {imageWidth, imageHeight};
    }
    final float ratio = Float32.round(Float32.round(imageWidth) / Float32.round(imageHeight));

    int newWidth = imageWidth;
    int newHeight = imageHeight;
    while (newWidth > Math.max(5, clientWidth) - 5 || newHeight > Math.max(5, clientHeight) - 5) {
      newWidth = (int) Float32.round(Float32.round(newWidth) * Float32.round(.9f));
      newHeight = (int) Float32.round(Float32.round(newWidth) / ratio);
    }
    return new int[] {newWidth, newHeight};
  }
}
