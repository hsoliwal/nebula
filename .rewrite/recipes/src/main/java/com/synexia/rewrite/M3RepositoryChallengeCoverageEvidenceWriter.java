// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Deterministic writer for tri-platform challenge coverage evidence. */
public final class M3RepositoryChallengeCoverageEvidenceWriter {
  public static final String COVERAGE_FILE = "CHALLENGE_SUPERSET_COVERAGE.tsv";
  public static final String SUMMARY_FILE = "CHALLENGE_SUPERSET_COVERAGE_SUMMARY.tsv";
  public static final String ROOT_FILE = "CHALLENGE_SUPERSET_COVERAGE.sha256";

  private M3RepositoryChallengeCoverageEvidenceWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(
      M3RepositoryChallengeCoverageEvidence evidence, Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(evidence, "evidence");
    Path output =
        Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
    Files.createDirectories(output);
    Files.writeString(output.resolve(COVERAGE_FILE), evidence.tsv(), StandardCharsets.UTF_8);
    Files.writeString(output.resolve(SUMMARY_FILE), summary(evidence), StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve(ROOT_FILE), "ROOT  " + evidence.root() + "\n", StandardCharsets.UTF_8);
  }

  static String summary(M3RepositoryChallengeCoverageEvidence evidence) {
    StringBuilder out = new StringBuilder("metric\tvalue\n");
    metric(out, "logicalProblems", evidence.logicalProblems());
    metric(out, "sourceImplementations", evidence.sourceImplementations());
    metric(out, "sourceSupersetted", evidence.sourceSupersetted());
    metric(out, "pending", evidence.pending());
    metric(out, "complete", evidence.complete());
    metric(out, "root", evidence.root());
    for (M3RepositoryChallengeCoverageEvidence.Platform row : evidence.platforms()) {
      String prefix = "platform." + row.platform() + ".";
      metric(out, prefix + "logicalProblems", row.logicalProblems());
      metric(out, prefix + "sourceImplementations", row.sourceImplementations());
      metric(out, prefix + "sourceSupersetted", row.sourceSupersetted());
      metric(out, prefix + "pending", row.pending());
    }
    metric(out, "mutationAuthority", evidence.mutationAuthority());
    metric(out, "sourceCopyAuthority", evidence.sourceCopyAuthority());
    metric(out, "replacementAuthority", evidence.replacementAuthority());
    metric(out, "promotionAuthority", evidence.promotionAuthority());
    return out.toString();
  }

  private static void metric(StringBuilder out, String name, Object value) {
    out.append(name).append('\t').append(Objects.toString(value, "")).append('\n');
  }
}
