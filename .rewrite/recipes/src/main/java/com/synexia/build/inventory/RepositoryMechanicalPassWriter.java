// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Writes deterministic repository-wide mechanical-pass and precompute/index evidence. */
public final class RepositoryMechanicalPassWriter {
  private RepositoryMechanicalPassWriter() {}

  public static void write(
      RepoSupersetInventory inventory,
      RepositoryMechanicalPassIndex passes,
      Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(inventory, "inventory");
    Objects.requireNonNull(passes, "passes");
    Path output = Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
    Files.createDirectories(output);

    writeFindings(passes, output.resolve("MECHANICAL_PASS_FINDINGS.tsv"));
    writeSummary(passes, output.resolve("MECHANICAL_PASS_SUMMARY.tsv"));
    writeApiDataFlow(inventory, output.resolve("API_DATA_FLOW.tsv"));
    writeApiCapabilityFlow(inventory, output.resolve("API_CAPABILITY_FLOW.tsv"));
    writeIndexManifest(inventory, output.resolve("PRECOMPUTED_INDEX_MANIFEST.tsv"));
    RepositorySupersetExecutionWriter.write(
        RepositorySupersetExecutionIndex.build(inventory), output);
  }

  private static void writeFindings(RepositoryMechanicalPassIndex passes, Path file)
      throws IOException {
    StringBuilder out = new StringBuilder();
    out.append("pass\tseverity\tcapability\tpath\tline\tevidence\tstrategy\n");
    for (RepositoryMechanicalPassIndex.Finding finding : passes.findings()) {
      row(
          out,
          finding.pass().name(),
          finding.severity().name(),
          finding.capability(),
          finding.path(),
          Integer.toString(finding.line()),
          finding.evidence(),
          finding.strategy());
    }
    Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
  }

  private static void writeSummary(RepositoryMechanicalPassIndex passes, Path file)
      throws IOException {
    StringBuilder out = new StringBuilder();
    out.append("pass\tfindings\tinfo\treview\terror\n");
    for (RepositoryMechanicalPassIndex.Summary summary : passes.summaries()) {
      row(
          out,
          summary.pass().name(),
          Integer.toString(summary.findings()),
          Integer.toString(summary.info()),
          Integer.toString(summary.review()),
          Integer.toString(summary.error()));
    }
    Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
  }

  private static void writeApiDataFlow(RepoSupersetInventory inventory, Path file)
      throws IOException {
    StringBuilder out = new StringBuilder();
    out.append(
        "api\tpath\tline\trequired_structure\tindexed_precomputed_representation\timplementation_strategy\tresult\n");
    for (RepoSupersetInventory.ApiCoverage coverage : inventory.apiCoverage()) {
      RepoApiRecord api = coverage.api();
      row(
          out,
          api.owner() + "#" + api.name(),
          api.path(),
          Integer.toString(api.line()),
          coverage.requiredStructure(),
          coverage.representation(),
          coverage.strategy(),
          coverage.indexed() && coverage.precomputed()
              ? "direct-precomputed-index"
              : coverage.indexed()
                  ? "indexed"
                  : coverage.precomputed() ? "precomputed" : "runtime-review");
    }
    Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
  }

  private static void writeApiCapabilityFlow(RepoSupersetInventory inventory, Path file)
      throws IOException {
    StringBuilder out = new StringBuilder();
    out.append(
        "api\tpath\tline\tmaven_module\tstructure_owner\tstructure_path"
            + "\trepresentation_owner\trepresentation_path\tcapabilities"
            + "\timplementation_strategy\tconfidence\tgap_action\tresult\tunresolved_reason\n");
    for (RepoApiCapabilityMap.Row coverage : inventory.apiCapabilityMap()) {
      RepoApiRecord api = coverage.api();
      row(
          out,
          api.owner() + "#" + api.name(),
          api.path(),
          Integer.toString(api.line()),
          coverage.mavenModule(),
          coverage.structureOwner(),
          coverage.structurePath(),
          coverage.representationOwner(),
          coverage.representationPath(),
          coverage.capabilities(),
          coverage.strategy(),
          coverage.confidence().name(),
          coverage.gapAction().name(),
          capabilityResult(coverage),
          coverage.unresolvedReason());
    }
    Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
  }

  private static String capabilityResult(RepoApiCapabilityMap.Row coverage) {
    if (!coverage.unresolvedReason().isEmpty()) return "unresolved";
    if (coverage.gapAction() == RepoApiCapabilityMap.GapAction.NONE) {
      return coverage.indexed() && coverage.precomputed()
          ? "direct-precomputed-index"
          : coverage.indexed() ? "indexed" : "resolved-runtime";
    }
    return coverage.gapAction().name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
  }

  private static void writeIndexManifest(RepoSupersetInventory inventory, Path file)
      throws IOException {
    RepoDerivedIndexes indexes = inventory.derivedIndexes();
    StringBuilder out = new StringBuilder();
    out.append("index\tkey_count\trecord_count\tfingerprint\n");
    row(
        out,
        "path",
        Integer.toString(inventory.files().size()),
        Integer.toString(inventory.files().size()),
        indexes.fingerprint());
    row(
        out,
        "module",
        Integer.toString(indexes.modules().size()),
        Integer.toString(inventory.files().size()),
        indexes.fingerprint());
    row(
        out,
        "signal",
        Integer.toString(RepoSignal.values().length),
        Integer.toString(
            java.util.Arrays.stream(RepoSignal.values())
                .mapToInt(signal -> indexes.filesBySignal(signal).size())
                .sum()),
        indexes.fingerprint());
    row(
        out,
        "api-owner",
        Integer.toString(
            inventory.apis().stream().map(RepoApiRecord::owner).collect(java.util.stream.Collectors.toSet()).size()),
        Integer.toString(inventory.apis().size()),
        indexes.fingerprint());
    row(
        out,
        "api-capability-route",
        Integer.toString(inventory.apiCapabilityMap().size()),
        Integer.toString(inventory.apiCapabilityMap().size()),
        indexes.fingerprint());
    row(
        out,
        "owner-collision",
        Integer.toString(inventory.ownerCollisions().size()),
        Integer.toString(
            inventory.ownerCollisions().stream().mapToInt(row -> row.paths().size()).sum()),
        indexes.fingerprint());
    row(
        out,
        "api-collision",
        Integer.toString(inventory.apiCollisions().size()),
        Integer.toString(
            inventory.apiCollisions().stream().mapToInt(row -> row.paths().size()).sum()),
        indexes.fingerprint());
    Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
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
        .replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }
}
