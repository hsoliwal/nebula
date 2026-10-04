// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Writes the additive eight-phase repository execution projection. */
public final class RepositorySupersetExecutionWriter {
  private RepositorySupersetExecutionWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(RepositorySupersetExecutionIndex index, Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(index, "index");
    Path output =
        Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
    Files.createDirectories(output);
    Files.writeString(
        output.resolve("M3_EXECUTION_FINDINGS.tsv"),
        findings(index),
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("M3_EXECUTION_SUMMARY.tsv"),
        summary(index),
        StandardCharsets.UTF_8);
  }

  static String findings(RepositorySupersetExecutionIndex index) {
    StringBuilder out =
        new StringBuilder("phase\tseverity\tcapability\tpath\tline\tevidence\tstrategy\n");
    for (RepositorySupersetExecutionIndex.Finding finding : index.findings()) {
      row(
          out,
          finding.phase().name(),
          finding.severity().name(),
          finding.capability(),
          finding.path(),
          Integer.toString(finding.line()),
          finding.evidence(),
          finding.strategy());
    }
    return out.toString();
  }

  static String summary(RepositorySupersetExecutionIndex index) {
    StringBuilder out = new StringBuilder("phase\tfindings\tinfo\treview\terror\n");
    for (RepositorySupersetExecutionIndex.Summary summary : index.summaries()) {
      row(
          out,
          summary.phase().name(),
          Integer.toString(summary.findings()),
          Integer.toString(summary.info()),
          Integer.toString(summary.review()),
          Integer.toString(summary.error()));
    }
    return out.toString();
  }

  private static void row(StringBuilder out, String... cells) {
    for (int index = 0; index < cells.length; index++) {
      if (index > 0) out.append('\t');
      out.append(tsv(cells[index]));
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
