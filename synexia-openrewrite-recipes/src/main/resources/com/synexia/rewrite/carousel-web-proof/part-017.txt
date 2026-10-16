// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.widgets.carousel.client;

import java.util.Map;
import java.util.TreeMap;

/** Positive image bounds and nonnegative SWT/Canvas client extents; no image allocation. */
public final class GeometryCorpus {
  private GeometryCorpus() {}

  public static int[][] cases() {
    Map<String, int[]> cases = new TreeMap<>();
    add(cases, 10, 10, 40, 40);
    add(cases, 100, 50, 55, 30);
    add(cases, 42, 84, 50, 50);
    int[][] smallImages = {{10,10}, {100,50}, {42,84}, {1,1}, {1,37}, {37,1}};
    for (int[] image : smallImages) {
      for (int width = 0; width <= 10; width++) {
        for (int height = 0; height <= 10; height++) {
          add(cases, image[0], image[1], width, height);
        }
      }
    }
    int[] edges = {9,10,11,49,50,51,99,100,101,255,256,257,
        1023,1024,1025,65535,65536,65537,16777215,16777216,16777217,16777218,
        2147483646,2147483647};
    int[][] clients = {{3,40}, {40,3}, {55,30}, {50,50}, {100,100}, {1024,1024}};
    for (int edge : edges) {
      int[][] images = {{edge,edge}, {edge,edge-1}, {edge-1,edge}, {edge,1}, {1,edge}};
      for (int[] image : images) {
        for (int[] client : clients) add(cases, image[0], image[1], client[0], client[1]);
      }
    }
    int[][] ratios = {{3,7}, {7,3}, {42,84}, {84,42}, {100,50}, {50,100}, {999,1000}, {1000,999}};
    int[][] ratioClients = {{6,6}, {17,11}, {55,30}, {50,50}, {999,999}};
    for (int[] image : ratios) {
      for (int[] client : ratioClients) add(cases, image[0], image[1], client[0], client[1]);
    }
    return cases.values().toArray(new int[0][]);
  }

  private static void add(Map<String, int[]> cases, int iw, int ih, int cw, int ch) {
    if (iw <= 0 || ih <= 0 || cw < 0 || ch < 0) throw new AssertionError("invalid corpus bounds");
    cases.put(iw + ":" + ih + ":" + cw + ":" + ch, new int[] {iw, ih, cw, ch});
  }
}
