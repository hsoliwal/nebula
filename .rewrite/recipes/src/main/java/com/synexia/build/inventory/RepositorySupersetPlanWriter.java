// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Deterministic writer for the API-driven additive-superset action plan. */
public final class RepositorySupersetPlanWriter {
  private RepositorySupersetPlanWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(RepositorySupersetPlan plan, Path outputDirectory) throws IOException {
    Objects.requireNonNull(plan, "plan");
    Path output = Objects.requireNonNull(outputDirectory, "outputDirectory")
        .toAbsolutePath().normalize();
    Files.createDirectories(output);

    Map<String, String> files = canonicalFiles(plan);
    StringBuilder sums = new StringBuilder();
    for (Map.Entry<String, String> entry : files.entrySet()) {
      Path target = output.resolve(entry.getKey()).normalize();
      if (!target.startsWith(output)) throw new IOException("plan output path escape");
      Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
      sums.append(sha256(entry.getValue())).append("  ").append(entry.getKey()).append('\n');
    }
    sums.append("ROOT  ").append(rootSha256(plan)).append('\n');
    Files.writeString(output.resolve("PLAN_SHA256SUMS"), sums, StandardCharsets.UTF_8);
  }

  static String rootSha256(RepositorySupersetPlan plan) {
    MessageDigest digest = sha();
    canonicalFiles(plan).forEach(
        (name, content) -> {
          digest.update(name.getBytes(StandardCharsets.UTF_8));
          digest.update((byte) 0);
          digest.update(content.getBytes(StandardCharsets.UTF_8));
          digest.update((byte) 0xff);
        });
    return HexFormat.of().formatHex(digest.digest());
  }

  static Map<String, String> canonicalFiles(RepositorySupersetPlan plan) {
    TreeMap<String, String> result = new TreeMap<>();
    result.put("action-plan.tsv", tasks(plan));
    result.put("api-structure-map.tsv", mappings(plan));
    result.put("plan-summary.tsv", summary(plan));
    return java.util.Collections.unmodifiableMap(result);
  }

  private static String tasks(RepositorySupersetPlan plan) {
    StringBuilder out = new StringBuilder(
        "id\tstage\taction\tmodule\tpath\tline\tcapability\tevidence\townerCandidates\n");
    plan.tasks().forEach(
        task ->
            out.append(task.id()).append('\t')
                .append(task.stage()).append('\t')
                .append(task.action()).append('\t')
                .append(tsv(task.modulePath())).append('\t')
                .append(tsv(task.path())).append('\t')
                .append(task.line()).append('\t')
                .append(tsv(task.capability())).append('\t')
                .append(tsv(task.evidence())).append('\t')
                .append(tsv(String.join(",", task.ownerCandidates()))).append('\n'));
    return out.toString();
  }

  private static String mappings(RepositorySupersetPlan plan) {
    StringBuilder out = new StringBuilder(
        "path\tmodule\tkind\tname\tsignature\trequiredStructures\texistingRoles\tstrategy\townerCandidates\n");
    plan.apiMappings().forEach(
        row ->
            out.append(tsv(row.path())).append('\t')
                .append(tsv(row.modulePath())).append('\t')
                .append(tsv(row.kind())).append('\t')
                .append(tsv(row.name())).append('\t')
                .append(tsv(row.signature())).append('\t')
                .append(tsv(String.join(",", row.requiredStructures()))).append('\t')
                .append(tsv(String.join(",", row.existingRoles()))).append('\t')
                .append(tsv(row.implementationStrategy())).append('\t')
                .append(tsv(String.join(",", row.ownerCandidates()))).append('\n'));
    return out.toString();
  }

  private static String summary(RepositorySupersetPlan plan) {
    RepositorySupersetPlan.Summary s = plan.summary();
    StringBuilder out = new StringBuilder("metric\tvalue\n");
    metric(out, "tasks", s.tasks());
    metric(out, "apiMappings", s.apiMappings());
    metric(out, "reuse", s.reuse());
    metric(out, "enhance", s.enhance());
    metric(out, "implement", s.implement());
    metric(out, "consolidate", s.consolidate());
    metric(out, "verify", s.verify());
    metric(out, "apiCoverage", s.apiCoverage());
    metric(out, "structuralSuperset", s.structuralSuperset());
    metric(out, "dataPrecompute", s.dataPrecompute());
    metric(out, "designAbstraction", s.designAbstraction());
    metric(out, "consistency", s.consistency());
    metric(out, "verification", s.verification());
    return out.toString();
  }

  private static void metric(StringBuilder out, String name, Object value) {
    out.append(name).append('\t').append(value).append('\n');
  }

  private static String tsv(String value) {
    return Objects.toString(value, "")
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }

  private static String sha256(String value) {
    MessageDigest digest = sha();
    digest.update(value.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest.digest());
  }

  private static MessageDigest sha() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
