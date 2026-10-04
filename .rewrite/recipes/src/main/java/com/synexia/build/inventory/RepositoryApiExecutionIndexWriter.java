// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.TreeMap;

/** Deterministic writer for the explicit API-driven execution index. */
public final class RepositoryApiExecutionIndexWriter {
  private RepositoryApiExecutionIndexWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(RepositoryApiExecutionIndex index, Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(index, "index");
    Path output =
        Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
    Files.createDirectories(output);
    String canonicalRows = rows(index);
    Files.writeString(
        output.resolve("API_EXECUTION_INDEX.tsv"),
        canonicalRows,
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("API_CAPABILITY_FLOW.tsv"),
        canonicalRows,
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("APIS.tsv"),
        apis(index),
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("API_STRUCTURE_MAP.tsv"),
        structureMap(index),
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("API_EXECUTION_INDEX_SUMMARY.tsv"),
        summary(index),
        StandardCharsets.UTF_8);
    Files.writeString(
        output.resolve("API_EXECUTION_INDEX.sha256"),
        "ROOT  " + index.rootSha256() + "\n",
        StandardCharsets.UTF_8);
  }

  static String rows(RepositoryApiExecutionIndex index) {
    StringBuilder out =
        new StringBuilder(
            "ordinal\tstable_id\tapi_stable_id\tmodule\tpath\tline\towner\tkind\tname"
                + "\tsignature\tstructure_owner\tstructure_path\trepresentation_owner"
                + "\trepresentation_path\trepresentation_capabilities\tindexed\tprecomputed"
                + "\timplementation_strategy\tresult_type\tresult_kind"
                + "\timplementation_decision\trecipe_action\trecipe_capabilities"
                + "\tdonor_identities\tdonor_repositories\trequired_gates\treason\n");
    for (RepositoryApiExecutionIndex.Row row : index.rows()) {
      line(
          out,
          Integer.toString(row.ordinal()),
          row.stableId(),
          row.apiStableId(),
          row.modulePath(),
          row.path(),
          Integer.toString(row.line()),
          row.owner(),
          row.kind(),
          row.name(),
          row.signature(),
          row.structureOwner(),
          row.structurePath(),
          row.representationOwner(),
          row.representationPath(),
          row.representationCapabilities(),
          Boolean.toString(row.indexed()),
          Boolean.toString(row.precomputed()),
          row.implementationStrategy(),
          row.resultContract().declaredType(),
          row.resultContract().kind().name(),
          row.implementationDecision().name(),
          row.recipeAction().name(),
          String.join(",", row.recipeCapabilityIds()),
          String.join(",", row.donorIdentities()),
          String.join(",", row.donorRepositories()),
          row.requiredGates().stream().map(Enum::name).reduce("", RepositoryApiExecutionIndexWriter::comma),
          row.reason());
    }
    return out.toString();
  }

  static String apis(RepositoryApiExecutionIndex index) {
    StringBuilder out =
        new StringBuilder(
            "ordinal\tapi_stable_id\tmodule\tpath\tline\towner\tkind\tname\tsignature"
                + "\tresult_type\tresult_kind\timplementation_decision\trecipe_action\n");
    for (RepositoryApiExecutionIndex.Row row : index.rows()) {
      line(
          out,
          Integer.toString(row.ordinal()),
          row.apiStableId(),
          row.modulePath(),
          row.path(),
          Integer.toString(row.line()),
          row.owner(),
          row.kind(),
          row.name(),
          row.signature(),
          row.resultContract().declaredType(),
          row.resultContract().kind().name(),
          row.implementationDecision().name(),
          row.recipeAction().name());
    }
    return out.toString();
  }

  static String structureMap(RepositoryApiExecutionIndex index) {
    StringBuilder out =
        new StringBuilder(
            "ordinal\tstable_id\tpath\towner\tsignature\tstructure_owner\tstructure_path"
                + "\trepresentation_owner\trepresentation_path\trepresentation_capabilities"
                + "\tindexed\tprecomputed\timplementation_strategy\n");
    for (RepositoryApiExecutionIndex.Row row : index.rows()) {
      line(
          out,
          Integer.toString(row.ordinal()),
          row.stableId(),
          row.path(),
          row.owner(),
          row.signature(),
          row.structureOwner(),
          row.structurePath(),
          row.representationOwner(),
          row.representationPath(),
          row.representationCapabilities(),
          Boolean.toString(row.indexed()),
          Boolean.toString(row.precomputed()),
          row.implementationStrategy());
    }
    return out.toString();
  }

  static String capabilityFlow(RepositoryApiExecutionIndex index) {
    return rows(index);
  }

  static String summary(RepositoryApiExecutionIndex index) {
    RepositoryApiExecutionIndex.Summary summary = index.summary();
    TreeMap<String, Integer> actions = new TreeMap<>();
    summary.byRecipeAction().forEach((action, count) -> actions.put(action.name(), count));

    StringBuilder out = new StringBuilder("metric\tvalue\n");
    metric(out, "rows", summary.rows());
    metric(out, "indexed", summary.indexed());
    metric(out, "precomputed", summary.precomputed());
    metric(out, "unresolvedResults", summary.unresolvedResults());
    actions.forEach((action, count) -> metric(out, "recipeAction." + action, count));
    out.append("rootSha256\t").append(summary.rootSha256()).append('\n');
    return out.toString();
  }

  private static String comma(String left, String right) {
    return left.isEmpty() ? right : left + "," + right;
  }

  private static void metric(StringBuilder out, String key, Object value) {
    out.append(key).append('\t').append(value).append('\n');
  }

  private static void line(StringBuilder out, String... values) {
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
