// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Deterministic writer for the per-API repository implementation queue. */
public final class RepositoryImplementationQueueWriter {
  private RepositoryImplementationQueueWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(RepositoryImplementationQueue queue, Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(queue, "queue");
    Path output =
        Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
    Files.createDirectories(output);
    Files.writeString(
        output.resolve("IMPLEMENTATION_QUEUE.tsv"),
        items(queue),
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("IMPLEMENTATION_QUEUE_SUMMARY.tsv"),
        summary(queue),
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("IMPLEMENTATION_QUEUE.sha256"),
        "ROOT  " + queue.rootSha256() + "\n",
        StandardCharsets.UTF_8);
  }

  static String items(RepositoryImplementationQueue queue) {
    StringBuilder out =
        new StringBuilder(
            "ordinal\tstable_id\tdecision\tmodule\tpath\tline\towner\tname\tsignature"
                + "\tstructure_owner\tstructure_path\trepresentation_owner\trepresentation_path"
                + "\tcapabilities\timplementation_strategy\tconfidence\tgap_action"
                + "\tdonor_action\tdonor_identities\tdonor_shapes\tdonor_kernels"
                + "\tdonor_repositories\tdonor_mechanics\trequired_gates\treason\n");
    for (RepositoryImplementationQueue.Item item : queue.items()) {
      row(
          out,
          Integer.toString(item.ordinal()),
          item.stableId(),
          item.decision().name(),
          item.modulePath(),
          item.path(),
          Integer.toString(item.line()),
          item.owner(),
          item.name(),
          item.signature(),
          item.structureOwner(),
          item.structurePath(),
          item.representationOwner(),
          item.representationPath(),
          item.capabilities(),
          item.implementationStrategy(),
          item.confidence().name(),
          item.gapAction().name(),
          item.donorAction(),
          String.join(",", item.donorIdentities()),
          String.join(",", item.donorShapes()),
          String.join(",", item.donorKernels()),
          String.join(",", item.donorRepositories()),
          String.join(",", item.donorMechanics()),
          item.requiredGates().stream().map(Enum::name).reduce("", RepositoryImplementationQueueWriter::comma),
          item.reason());
    }
    return out.toString();
  }

  static String summary(RepositoryImplementationQueue queue) {
    RepositoryImplementationQueue.Summary summary = queue.summary();
    TreeMap<String, Integer> counts = new TreeMap<>();
    summary.byDecision().forEach((decision, value) -> counts.put(decision.name(), value));

    StringBuilder out = new StringBuilder("metric\tvalue\n");
    metric(out, "items", summary.items());
    metric(out, "donorBacked", summary.donorBacked());
    counts.forEach((decision, value) -> metric(out, "decision." + decision, value));
    out.append("rootSha256\t").append(summary.rootSha256()).append('\n');
    return out.toString();
  }

  private static String comma(String left, String right) {
    return left.isEmpty() ? right : left + "," + right;
  }

  private static void metric(StringBuilder out, String key, Object value) {
    out.append(key).append('\t').append(value).append('\n');
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
