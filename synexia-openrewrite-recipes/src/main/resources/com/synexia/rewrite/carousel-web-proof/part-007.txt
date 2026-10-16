// SPDX-License-Identifier: EPL-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.widgets.carousel.client;

final class Float32 {
  private Float32() {}
  static native float round(double value) /*-{
    return $wnd.Math.fround(value);
  }-*/;
}
