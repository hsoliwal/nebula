// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.widgets.carousel.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** The original loop is a separate JVM oracle, never an expected-output generator in GWT. */
public final class GeometryJvmProof {
  private GeometryJvmProof() {}

  public static void main(String[] args) throws Exception {
    if (args.length > 1) throw new IllegalArgumentException("usage: GeometryJvmProof [output.tsv]");
    checkTrace(10, 10, 40, 40, 10, 10, 15, 15);
    checkTrace(100, 50, 55, 30, 45, 22, 5, 4);
    checkTrace(42, 84, 50, 50, 20, 40, 15, 5);
    for (int[] c : GeometryCorpus.cases()) {
      int[] expected = OriginalGeometryReference.fit(c[0], c[1], c[2], c[3]);
      int[] actual = CarouselGeometry.fit(c[0], c[1], c[2], c[3]);
      if (!Arrays.equals(expected, actual)) {
        throw new AssertionError("original-loop mismatch " + Arrays.toString(c)
            + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
      }
    }
    String lines = GeometryWorkload.lines();
    if (args.length == 0) System.out.print(lines);
    else Files.writeString(Path.of(args[0]), lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    System.out.println("GEOMETRY_SHA256=" + HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(lines.getBytes(StandardCharsets.UTF_8))));
    System.out.println("GEOMETRY_CASES=" + GeometryCorpus.cases().length);
    System.out.println("ORIGINAL_LOOP_PARITY=PASS");
  }

  private static void checkTrace(int iw, int ih, int cw, int ch,
      int width, int height, int x, int y) {
    int[] fit = CarouselGeometry.fit(iw, ih, cw, ch);
    if (!Arrays.equals(fit, new int[] {width, height})
        || (cw - fit[0]) / 2 != x || (ch - fit[1]) / 2 != y) {
      throw new AssertionError("qualified pixel trace changed");
    }
  }
}
