// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.StreamSupport;

/** Prevents generated evidence from becoming input to the next repository inventory pass. */
final class M3RepositorySupersetExecutionOutputPolicy {
  private static final Set<String> SAFE_SEGMENTS = Set.of("target", "build", "out", "dist");

  private M3RepositorySupersetExecutionOutputPolicy() {
    throw new AssertionError("No instances");
  }

  static void requireSafe(Path root, Path output) {
    if (!output.startsWith(root)) {
      return;
    }
    boolean safe =
        StreamSupport.stream(root.relativize(output).spliterator(), false)
            .map(Path::toString)
            .anyMatch(SAFE_SEGMENTS::contains);
    if (!safe) {
      throw new IllegalArgumentException("repository output must use an excluded build directory");
    }
  }
}
