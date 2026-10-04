// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Deterministic receipt writer for broad/structural inventory convergence. */
public final class RepositoryInventoryConvergenceWriter {
  private RepositoryInventoryConvergenceWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(
      RepositoryInventoryConvergence.Report report, Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(report, "report");
    Path output =
        Objects.requireNonNull(outputDirectory, "outputDirectory")
            .toAbsolutePath()
            .normalize();
    Files.createDirectories(output);
    Files.writeString(
        output.resolve("REPOSITORY_INVENTORY_CONVERGENCE.tsv"),
        differences(report),
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("REPOSITORY_INVENTORY_CONVERGENCE_SUMMARY.tsv"),
        summary(report),
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("REPOSITORY_INVENTORY_CONVERGENCE.sha256"),
        "ROOT  " + report.rootSha256() + "\n",
        StandardCharsets.UTF_8);
  }

  static String differences(RepositoryInventoryConvergence.Report report) {
    StringBuilder out =
        new StringBuilder("kind\tpath\tbroad_value\tstructural_value\n");
    for (RepositoryInventoryConvergence.Difference difference : report.differences()) {
      row(
          out,
          difference.kind().name(),
          difference.path(),
          difference.broadValue(),
          difference.structuralValue());
    }
    return out.toString();
  }

  static String summary(RepositoryInventoryConvergence.Report report) {
    StringBuilder out = new StringBuilder("metric\tvalue\n");
    metric(out, "broadRoot", report.broadRoot());
    metric(out, "structuralRoot", report.structuralRoot());
    metric(out, "broadFiles", report.broadFiles());
    metric(out, "structuralFiles", report.structuralFiles());
    metric(out, "commonFiles", report.commonFiles());
    metric(out, "broadOnlyFiles", report.broadOnlyFiles());
    metric(out, "structuralOnlyFiles", report.structuralOnlyFiles());
    metric(out, "exactShaMatches", report.exactShaMatches());
    metric(out, "moduleOwnerMatches", report.moduleOwnerMatches());
    metric(out, "differences", report.differences().size());
    metric(out, "sharedPathsAgree", report.sharedPathsAgree());
    metric(out, "rootSha256", report.rootSha256());
    return out.toString();
  }

  private static void metric(StringBuilder out, String name, Object value) {
    row(out, name, Objects.toString(value, ""));
  }

  private static void row(StringBuilder out, String... values) {
    for (int index = 0; index < values.length; index++) {
      if (index > 0) out.append('\t');
      out.append(tsv(values[index]));
    }
    out.append('\n');
  }

  private static String tsv(String value) {
    return Objects.toString(value, "")
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }
}
