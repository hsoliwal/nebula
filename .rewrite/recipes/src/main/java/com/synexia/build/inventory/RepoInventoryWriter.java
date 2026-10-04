// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

/** Deterministic machine-readable publication for repository inventory passes. */
public final class RepoInventoryWriter {
  private RepoInventoryWriter() {}

  public static void write(RepoSupersetInventory inventory, Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(inventory, "inventory");
    Path output = Objects.requireNonNull(outputDirectory, "outputDirectory")
        .toAbsolutePath().normalize();
    Files.createDirectories(output);
    writeFiles(inventory, output.resolve("FILES.tsv"));
    writeApis(inventory, output.resolve("APIS.tsv"));
    writeModules(inventory, output.resolve("MODULES.tsv"));
    writeMavenModuleOwnership(inventory, output.resolve("MAVEN_MODULE_OWNERSHIP.tsv"));
    writeDuplicates(inventory, output.resolve("DUPLICATES_EXACT.tsv"));
    writeCandidateClusters(inventory, output.resolve("DUPLICATES_CANDIDATE.tsv"));
    writeApiCoverage(inventory, output.resolve("API_COVERAGE.tsv"));
    writeOwnerCollisions(inventory, output.resolve("OWNER_COLLISIONS.tsv"));
    writeApiCollisions(inventory, output.resolve("API_COLLISIONS.tsv"));
    writeApiStructureMap(inventory, output.resolve("API_STRUCTURE_MAP.tsv"));
    writeApiPrecomputeGaps(inventory, output.resolve("API_PRECOMPUTE_GAPS.tsv"));
    writeActions(inventory, output.resolve("ACTION_QUEUE.tsv"));
    writeSummary(inventory, output.resolve("SUMMARY.json"));
  }

  private static void writeFiles(RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write(
          "path\tmodule\textension\tcategory\tlanguage\tsize_bytes\tlines\tsha256\tstructural_sha256\tlogic_sha256\tsimhash64\tpackage\ttypes\tsignals");
      writer.newLine();
      for (RepoFileRecord file : inventory.files()) {
        writer.write(tsv(file.path()));
        writer.write('\t');
        writer.write(tsv(file.module()));
        writer.write('\t');
        writer.write(tsv(file.extension()));
        writer.write('\t');
        writer.write(file.category().text());
        writer.write('\t');
        writer.write(file.language().text());
        writer.write('\t');
        writer.write(Long.toString(file.sizeBytes()));
        writer.write('\t');
        writer.write(Long.toString(file.lineCount()));
        writer.write('\t');
        writer.write(file.sha256());
        writer.write('\t');
        writer.write(file.structuralSha256());
        writer.write('\t');
        writer.write(file.logicSha256());
        writer.write('\t');
        writer.write(Long.toUnsignedString(file.simHash64(), 16));
        writer.write('\t');
        writer.write(tsv(file.packageName()));
        writer.write('\t');
        writer.write(tsv(String.join(",", file.typeNames())));
        writer.write('\t');
        ArrayList<String> signals = new ArrayList<>();
        for (RepoSignal signal : file.signals()) signals.add(signal.name());
        signals.sort(String::compareTo);
        writer.write(String.join(",", signals));
        writer.newLine();
      }
    }
  }

  private static void writeApis(RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("path\tline\towner\tvisibility\tkind\tname\tsignature");
      writer.newLine();
      for (RepoApiRecord api : inventory.apis()) {
        writer.write(tsv(api.path()));
        writer.write('\t');
        writer.write(Integer.toString(api.line()));
        writer.write('\t');
        writer.write(tsv(api.owner()));
        writer.write('\t');
        writer.write(tsv(api.visibility()));
        writer.write('\t');
        writer.write(tsv(api.kind()));
        writer.write('\t');
        writer.write(tsv(api.name()));
        writer.write('\t');
        writer.write(tsv(api.signature()));
        writer.newLine();
      }
    }
  }

  private static void writeModules(RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write(
          "module\tfiles\tjava_files\ttests\tapis\tmindex_owners\tjini_files\tprecompute_files\tindex_files\tcache_files\tstub_signals");
      writer.newLine();
      for (RepoSupersetInventory.ModuleSummary module : inventory.modules()) {
        writer.write(tsv(module.module()));
        writer.write('\t');
        writer.write(Integer.toString(module.files()));
        writer.write('\t');
        writer.write(Integer.toString(module.javaFiles()));
        writer.write('\t');
        writer.write(Integer.toString(module.tests()));
        writer.write('\t');
        writer.write(Integer.toString(module.apis()));
        writer.write('\t');
        writer.write(Integer.toString(module.mindexOwners()));
        writer.write('\t');
        writer.write(Integer.toString(module.jiniFiles()));
        writer.write('\t');
        writer.write(Integer.toString(module.precomputeFiles()));
        writer.write('\t');
        writer.write(Integer.toString(module.indexFiles()));
        writer.write('\t');
        writer.write(Integer.toString(module.cacheFiles()));
        writer.write('\t');
        writer.write(Integer.toString(module.stubSignals()));
        writer.newLine();
      }
    }
  }

  private static void writeMavenModuleOwnership(
      RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("path\tlegacy_module_bucket\tmaven_module");
      writer.newLine();
      for (RepoFileRecord file : inventory.files()) {
        writer.write(tsv(file.path()));
        writer.write('\t');
        writer.write(tsv(file.module()));
        writer.write('\t');
        writer.write(tsv(inventory.mavenModule(file.path())));
        writer.newLine();
      }
    }
  }

  private static void writeDuplicates(RepoSupersetInventory inventory, Path path)
      throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("sha256\tcluster_size\tpath");
      writer.newLine();
      for (RepoSupersetInventory.DuplicateCluster cluster : inventory.exactDuplicates()) {
        for (String duplicate : cluster.paths()) {
          writer.write(cluster.sha256());
          writer.write('\t');
          writer.write(Integer.toString(cluster.paths().size()));
          writer.write('\t');
          writer.write(tsv(duplicate));
          writer.newLine();
        }
      }
    }
  }

  private static void writeCandidateClusters(
      RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("lane\tvalue\tcluster_size\tpath\tproof");
      writer.newLine();
      for (RepoSupersetInventory.CandidateCluster cluster : inventory.candidateClusters()) {
        for (String candidate : cluster.paths()) {
          writer.write(tsv(cluster.lane()));
          writer.write('\t');
          writer.write(tsv(cluster.value()));
          writer.write('\t');
          writer.write(Integer.toString(cluster.paths().size()));
          writer.write('\t');
          writer.write(tsv(candidate));
          writer.write('\t');
          writer.write("candidate-only");
          writer.newLine();
        }
      }
    }
  }

  private static void writeApiCoverage(
      RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write(
          "path\tline\towner\tapi\trequired_structure\trepresentation\tstrategy\tindexed\tprecomputed\tjini\tcached\tserialized\tjni_native");
      writer.newLine();
      for (RepoSupersetInventory.ApiCoverage coverage : inventory.apiCoverage()) {
        RepoApiRecord api = coverage.api();
        writer.write(tsv(api.path()));
        writer.write('\t');
        writer.write(Integer.toString(api.line()));
        writer.write('\t');
        writer.write(tsv(api.owner()));
        writer.write('\t');
        writer.write(tsv(api.signature()));
        writer.write('\t');
        writer.write(tsv(coverage.requiredStructure()));
        writer.write('\t');
        writer.write(tsv(coverage.representation()));
        writer.write('\t');
        writer.write(tsv(coverage.strategy()));
        writer.write('\t');
        writer.write(Boolean.toString(coverage.indexed()));
        writer.write('\t');
        writer.write(Boolean.toString(coverage.precomputed()));
        writer.write('\t');
        writer.write(Boolean.toString(coverage.jini()));
        writer.write('\t');
        writer.write(Boolean.toString(coverage.cached()));
        writer.write('\t');
        writer.write(Boolean.toString(coverage.serialized()));
        writer.write('\t');
        writer.write(Boolean.toString(coverage.nativeBridge()));
        writer.newLine();
      }
    }
  }

  private static void writeOwnerCollisions(
      RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write(
          "type\tclassification\texplicit_bridge_required\tmodules\tpaths\texact_sha256"
              + "\tstructural_sha256\tlogic_sha256");
      writer.newLine();
      for (RepoOwnerCollisionIndex.Row row : inventory.ownerCollisions()) {
        writer.write(tsv(row.typeName()));
        writer.write('\t');
        writer.write(row.classification().name());
        writer.write('\t');
        writer.write(Boolean.toString(row.explicitBridgeRequired()));
        writer.write('\t');
        writer.write(tsv(String.join(",", row.modules())));
        writer.write('\t');
        writer.write(tsv(String.join(",", row.paths())));
        writer.write('\t');
        writer.write(tsv(String.join(",", row.exactSha256())));
        writer.write('\t');
        writer.write(tsv(String.join(",", row.structuralSha256())));
        writer.write('\t');
        writer.write(tsv(String.join(",", row.logicSha256())));
        writer.newLine();
      }
    }
  }

  private static void writeApiCollisions(
      RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("owner\tkind\tname\tclassification\tpaths\tsignatures");
      writer.newLine();
      for (RepoApiCollisionIndex.Row row : inventory.apiCollisions()) {
        writer.write(tsv(row.owner()));
        writer.write('\t');
        writer.write(tsv(row.kind()));
        writer.write('\t');
        writer.write(tsv(row.name()));
        writer.write('\t');
        writer.write(row.classification().name());
        writer.write('\t');
        writer.write(tsv(String.join(",", row.paths())));
        writer.write('\t');
        writer.write(tsv(String.join(" || ", row.signatures())));
        writer.newLine();
      }
    }
  }

  private static void writeApiStructureMap(
      RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write(
          "path\tline\towner\tapi\tmaven_module\tstructure_owner\tstructure_path"
              + "\trepresentation_owner\trepresentation_path\tcapabilities\tstrategy"
              + "\tconfidence\tgap_action\tunresolved_reason");
      writer.newLine();
      for (RepoApiCapabilityMap.Row row : inventory.apiCapabilityMap()) {
        RepoApiRecord api = row.api();
        writer.write(tsv(api.path()));
        writer.write('\t');
        writer.write(Integer.toString(api.line()));
        writer.write('\t');
        writer.write(tsv(api.owner()));
        writer.write('\t');
        writer.write(tsv(api.signature()));
        writer.write('\t');
        writer.write(tsv(row.mavenModule()));
        writer.write('\t');
        writer.write(tsv(row.structureOwner()));
        writer.write('\t');
        writer.write(tsv(row.structurePath()));
        writer.write('\t');
        writer.write(tsv(row.representationOwner()));
        writer.write('\t');
        writer.write(tsv(row.representationPath()));
        writer.write('\t');
        writer.write(tsv(row.capabilities()));
        writer.write('\t');
        writer.write(tsv(row.strategy()));
        writer.write('\t');
        writer.write(row.confidence().name());
        writer.write('\t');
        writer.write(row.gapAction().name());
        writer.write('\t');
        writer.write(tsv(row.unresolvedReason()));
        writer.newLine();
      }
    }
  }

  private static void writeApiPrecomputeGaps(
      RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write(
          "gap_action\tpath\tline\towner\tapi\tmaven_module\tstructure_path"
              + "\trepresentation_path\tcapabilities\tunresolved_reason");
      writer.newLine();
      for (RepoApiCapabilityMap.Row row : inventory.apiCapabilityMap()) {
        if (row.gapAction() == RepoApiCapabilityMap.GapAction.NONE) continue;
        RepoApiRecord api = row.api();
        writer.write(row.gapAction().name());
        writer.write('\t');
        writer.write(tsv(api.path()));
        writer.write('\t');
        writer.write(Integer.toString(api.line()));
        writer.write('\t');
        writer.write(tsv(api.owner()));
        writer.write('\t');
        writer.write(tsv(api.signature()));
        writer.write('\t');
        writer.write(tsv(row.mavenModule()));
        writer.write('\t');
        writer.write(tsv(row.structurePath()));
        writer.write('\t');
        writer.write(tsv(row.representationPath()));
        writer.write('\t');
        writer.write(tsv(row.capabilities()));
        writer.write('\t');
        writer.write(tsv(row.unresolvedReason()));
        writer.newLine();
      }
    }
  }

  private static void writeActions(RepoSupersetInventory inventory, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("severity\tcategory\tpath\tline\tdetail");
      writer.newLine();
      for (RepoAction action : inventory.actions()) {
        writer.write(action.severity().name());
        writer.write('\t');
        writer.write(tsv(action.category()));
        writer.write('\t');
        writer.write(tsv(action.path()));
        writer.write('\t');
        writer.write(Integer.toString(action.line()));
        writer.write('\t');
        writer.write(tsv(action.detail()));
        writer.newLine();
      }
    }
  }

  private static void writeSummary(RepoSupersetInventory inventory, Path path) throws IOException {
    long javaFiles = inventory.files().stream().filter(file -> ".java".equals(file.extension())).count();
    long mindexOwners = inventory.files().stream()
        .filter(file -> file.signals().contains(RepoSignal.MINDEX_OWNER)
            || file.signals().contains(RepoSignal.MINEX_OWNER))
        .count();
    long jini = inventory.files().stream().filter(file -> file.signals().contains(RepoSignal.JINI)).count();
    long precompute = inventory.files().stream()
        .filter(file -> file.signals().contains(RepoSignal.PRECOMPUTE))
        .count();

    String json =
        "{\n"
            + "  \"fingerprint\": \"" + json(inventory.fingerprint()) + "\",\n"
            + "  \"files\": " + inventory.files().size() + ",\n"
            + "  \"javaFiles\": " + javaFiles + ",\n"
            + "  \"modules\": " + inventory.modules().size() + ",\n"
            + "  \"filesByCategory\": " + categoryCounts(inventory) + ",\n"
            + "  \"filesByLanguage\": " + languageCounts(inventory) + ",\n"
            + "  \"mavenModules\": " + inventory.mavenModulePaths().size() + ",\n"
            + "  \"mavenModuleIndexRoot\": \"" + json(inventory.mavenModuleIndexRoot()) + "\",\n"
            + "  \"apis\": " + inventory.apis().size() + ",\n"
            + "  \"mindexOwners\": " + mindexOwners + ",\n"
            + "  \"jiniFiles\": " + jini + ",\n"
            + "  \"precomputeFiles\": " + precompute + ",\n"
            + "  \"exactDuplicateClusters\": " + inventory.exactDuplicates().size() + ",\n"
            + "  \"candidateDuplicateClusters\": " + inventory.candidateClusters().size() + ",\n"
            + "  \"apiCoverageRows\": " + inventory.apiCoverage().size() + ",\n"
            + "  \"ownerCollisionRows\": " + inventory.ownerCollisions().size() + ",\n"
            + "  \"explicitBridgeRequiredRows\": "
            + inventory.ownerCollisions().stream()
                .filter(RepoOwnerCollisionIndex.Row::explicitBridgeRequired)
                .count()
            + ",\n"
            + "  \"apiCollisionRows\": " + inventory.apiCollisions().size() + ",\n"
            + "  \"divergentApiCollisionRows\": "
            + inventory.apiCollisions().stream()
                .filter(RepoApiCollisionIndex.Row::divergent)
                .count()
            + ",\n"
            + "  \"apiStructureRows\": " + inventory.apiCapabilityMap().size() + ",\n"
            + "  \"apiPrecomputeGaps\": "
            + inventory.apiCapabilityMap().stream()
                .filter(row -> row.gapAction() != RepoApiCapabilityMap.GapAction.NONE)
                .count()
            + ",\n"
            + "  \"actionCandidates\": " + inventory.actions().size() + "\n"
            + "}\n";
    Files.writeString(path, json, StandardCharsets.UTF_8);
  }

  private static String categoryCounts(RepoSupersetInventory inventory) {
    StringBuilder counts = new StringBuilder("{");
    boolean first = true;
    for (RepoFileCategory category : RepoFileCategory.values()) {
      if (!first) counts.append(", ");
      counts.append('"').append(json(category.text())).append("\": ");
      counts.append(inventory.filesByCategory(category).size());
      first = false;
    }
    return counts.append('}').toString();
  }

  private static String languageCounts(RepoSupersetInventory inventory) {
    StringBuilder counts = new StringBuilder("{");
    boolean first = true;
    for (RepoLanguage language : RepoLanguage.values()) {
      if (!first) counts.append(", ");
      counts.append('"').append(json(language.text())).append("\": ");
      counts.append(inventory.filesByLanguage(language).size());
      first = false;
    }
    return counts.append('}').toString();
  }

  private static String tsv(String value) {
    return Objects.requireNonNull(value, "value")
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }

  private static String json(String value) {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      switch (current) {
        case '"' -> result.append("\\\"");
        case '\\' -> result.append("\\\\");
        case '\b' -> result.append("\\b");
        case '\f' -> result.append("\\f");
        case '\n' -> result.append("\\n");
        case '\r' -> result.append("\\r");
        case '\t' -> result.append("\\t");
        default -> {
          if (current < 0x20) result.append("\\u%04x".formatted((int) current));
          else result.append(current);
        }
      }
    }
    return result.toString();
  }
}
