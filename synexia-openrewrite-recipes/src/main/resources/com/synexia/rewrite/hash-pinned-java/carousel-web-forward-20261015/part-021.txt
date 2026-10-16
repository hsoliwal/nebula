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

public final class OriginalGeometryReference {
  private OriginalGeometryReference() {}
  /** Requires positive image dimensions and nonnegative client extents. */
  public static int[] fit(int imageWidth, int imageHeight, int clientWidth, int clientHeight) {
    if (imageWidth <= clientWidth && imageHeight <= clientHeight) {
      return new int[] {imageWidth, imageHeight};
    }
    final float ratio = imageWidth * 1f / imageHeight * 1f;

    int newWidth = imageWidth;
    int newHeight = imageHeight;
    while (newWidth > Math.max(5, clientWidth) - 5 || newHeight > Math.max(5, clientHeight) - 5) {
      newWidth = (int) (newWidth * .9f);
      newHeight = (int) (newWidth / ratio);
    }
    return new int[] {newWidth, newHeight};
  }
}
