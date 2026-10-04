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

/** Deterministic evidence writer for the competitive donor/API reuse plan. */
public final class RepositoryCompetitiveAlgorithmPlanWriter {
  private RepositoryCompetitiveAlgorithmPlanWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(RepositoryCompetitiveAlgorithmPlan plan, Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(plan, "plan");
    Path output =
        Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
    Files.createDirectories(output);

    Map<String, String> files = canonicalFiles(plan);
    StringBuilder sums = new StringBuilder();
    for (Map.Entry<String, String> entry : files.entrySet()) {
      Path target = output.resolve(entry.getKey()).normalize();
      if (!target.startsWith(output)) throw new IOException("competitive plan output path escape");
      Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
      sums.append(sha256(entry.getValue())).append("  ").append(entry.getKey()).append('\n');
    }
    sums.append("ROOT  ").append(rootSha256(plan)).append('\n');
    Files.writeString(output.resolve("COMPETITIVE_PLAN_SHA256SUMS"), sums, StandardCharsets.UTF_8);
  }

  static String rootSha256(RepositoryCompetitiveAlgorithmPlan plan) {
    MessageDigest digest = sha();
    canonicalFiles(plan)
        .forEach(
            (name, content) -> {
              digest.update(name.getBytes(StandardCharsets.UTF_8));
              digest.update((byte) 0);
              digest.update(content.getBytes(StandardCharsets.UTF_8));
              digest.update((byte) 0xff);
            });
    return HexFormat.of().formatHex(digest.digest());
  }

  static Map<String, String> canonicalFiles(RepositoryCompetitiveAlgorithmPlan plan) {
    TreeMap<String, String> files = new TreeMap<>();
    files.put("competitive-donors.tsv", donors(plan));
    files.put("competitive-api-candidates.tsv", candidates(plan));
    files.put("competitive-summary.tsv", summary(plan));
    return java.util.Collections.unmodifiableMap(files);
  }

  private static String donors(RepositoryCompetitiveAlgorithmPlan plan) {
    StringBuilder out =
        new StringBuilder(
            "manifestKind\tmanifestPath\tidentity\ttitle\tshape\tkernel\trepository\trevision"
                + "\tsourcePath\tsourceBlob\tlicense\tmechanic\tprojection\tsourceDisposition\n");
    plan.donors()
        .forEach(
            row ->
                out.append(row.manifestKind()).append('\t')
                    .append(tsv(row.manifestPath())).append('\t')
                    .append(tsv(row.identity())).append('\t')
                    .append(tsv(row.title())).append('\t')
                    .append(tsv(row.shape())).append('\t')
                    .append(tsv(row.kernel())).append('\t')
                    .append(tsv(row.repository())).append('\t')
                    .append(row.revision()).append('\t')
                    .append(tsv(row.sourcePath())).append('\t')
                    .append(row.sourceBlob()).append('\t')
                    .append(tsv(row.license())).append('\t')
                    .append(tsv(row.mechanic())).append('\t')
                    .append(tsv(row.projection())).append('\t')
                    .append(row.sourceDisposition()).append('\n'));
    return out.toString();
  }

  private static String candidates(RepositoryCompetitiveAlgorithmPlan plan) {
    StringBuilder out =
        new StringBuilder(
            "path\tmodule\tname\tsignature\tmatchBasis\taction\tdonorIdentities\tshapes"
                + "\tkernels\trepositories\tmechanics\n");
    plan.candidates()
        .forEach(
            row ->
                out.append(tsv(row.path())).append('\t')
                    .append(tsv(row.modulePath())).append('\t')
                    .append(tsv(row.name())).append('\t')
                    .append(tsv(row.signature())).append('\t')
                    .append(row.matchBasis()).append('\t')
                    .append(row.action()).append('\t')
                    .append(tsv(String.join(",", row.donorIdentities()))).append('\t')
                    .append(tsv(String.join(",", row.shapes()))).append('\t')
                    .append(tsv(String.join(",", row.kernels()))).append('\t')
                    .append(tsv(String.join(",", row.repositories()))).append('\t')
                    .append(tsv(String.join(",", row.mechanics()))).append('\n'));
    return out.toString();
  }

  private static String summary(RepositoryCompetitiveAlgorithmPlan plan) {
    RepositoryCompetitiveAlgorithmPlan.Summary summary = plan.summary();
    return "metric\tvalue\n"
        + "donorRows\t" + summary.donorRows() + "\n"
        + "matchedApis\t" + summary.matchedApis() + "\n"
        + "reuseExistingKernel\t" + summary.reuseExistingKernel() + "\n"
        + "reviewDonorMechanic\t" + summary.reviewDonorMechanic() + "\n";
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
