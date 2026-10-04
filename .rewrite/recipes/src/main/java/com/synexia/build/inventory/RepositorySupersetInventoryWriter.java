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

/** Deterministic TSV + SHA-256 writer for repository-superset inventory evidence. */
public final class RepositorySupersetInventoryWriter {
  private RepositorySupersetInventoryWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(RepositorySupersetInventory inventory, Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(inventory, "inventory");
    Path output = Objects.requireNonNull(outputDirectory, "outputDirectory")
        .toAbsolutePath().normalize();
    Files.createDirectories(output);

    Map<String, String> files = canonicalFiles(inventory);
    StringBuilder manifest = new StringBuilder();
    for (Map.Entry<String, String> entry : files.entrySet()) {
      Path target = output.resolve(entry.getKey()).normalize();
      if (!target.startsWith(output)) throw new IOException("inventory output path escape");
      Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
      manifest.append(sha256(entry.getValue())).append("  ").append(entry.getKey()).append('\n');
    }
    manifest.append("ROOT  ").append(rootSha256(inventory)).append('\n');
    Files.writeString(output.resolve("SHA256SUMS"), manifest, StandardCharsets.UTF_8);
  }

  static String rootSha256(RepositorySupersetInventory inventory) {
    MessageDigest digest = sha();
    canonicalFiles(inventory).forEach((name, content) -> {
      update(digest, name);
      digest.update((byte) 0);
      digest.update(content.getBytes(StandardCharsets.UTF_8));
      digest.update((byte) 0xff);
    });
    return HexFormat.of().formatHex(digest.digest());
  }

  static Map<String, String> canonicalFiles(RepositorySupersetInventory inventory) {
    TreeMap<String, String> result = new TreeMap<>();
    result.put("modules.tsv", modules(inventory));
    result.put("dependencies.tsv", dependencies(inventory));
    result.put("files.tsv", files(inventory));
    result.put("api.tsv", api(inventory));
    result.put("findings.tsv", findings(inventory));
    result.put("summary.tsv", summary(inventory));
    return java.util.Collections.unmodifiableMap(result);
  }

  private static String modules(RepositorySupersetInventory inventory) {
    StringBuilder out = new StringBuilder(
        "path\tgroupId\tartifactId\tpackaging\treactorRegistered\tmainJava\ttestJava\tresources\troles\tpomSha256\n");
    inventory.modules().forEach(row -> out.append(tsv(row.path())).append('\t')
        .append(tsv(row.groupId())).append('\t')
        .append(tsv(row.artifactId())).append('\t')
        .append(tsv(row.packaging())).append('\t')
        .append(row.reactorRegistered()).append('\t')
        .append(row.mainJavaFiles()).append('\t')
        .append(row.testJavaFiles()).append('\t')
        .append(row.resourceFiles()).append('\t')
        .append(tsv(String.join(",", row.roles()))).append('\t')
        .append(row.pomSha256()).append('\n'));
    return out.toString();
  }

  private static String dependencies(RepositorySupersetInventory inventory) {
    StringBuilder out = new StringBuilder(
        "modulePath\tgroupId\tartifactId\tversion\tscope\toptional\n");
    inventory.dependencies().forEach(row -> out.append(tsv(row.modulePath())).append('\t')
        .append(tsv(row.groupId())).append('\t')
        .append(tsv(row.artifactId())).append('\t')
        .append(tsv(row.version())).append('\t')
        .append(tsv(row.scope())).append('\t')
        .append(row.optional()).append('\n'));
    return out.toString();
  }

  private static String files(RepositorySupersetInventory inventory) {
    StringBuilder out = new StringBuilder(
        "path\tbytes\tsha256\tpackage\tjava\ttest\tclasses\tinterfaces\tmethods\troles\n");
    inventory.files().forEach(row -> out.append(tsv(row.path())).append('\t')
        .append(row.bytes()).append('\t')
        .append(row.sha256()).append('\t')
        .append(tsv(row.packageName())).append('\t')
        .append(row.javaSource()).append('\t')
        .append(row.testSource()).append('\t')
        .append(row.classes()).append('\t')
        .append(row.interfaces()).append('\t')
        .append(row.methods()).append('\t')
        .append(tsv(String.join(",", row.roles()))).append('\n'));
    return out.toString();
  }

  private static String api(RepositorySupersetInventory inventory) {
    StringBuilder out = new StringBuilder(
        "path\tpackage\tkind\tname\tsignature\tcontract\n");
    inventory.apiFacts().forEach(row -> out.append(tsv(row.path())).append('\t')
        .append(tsv(row.packageName())).append('\t')
        .append(tsv(row.kind())).append('\t')
        .append(tsv(row.name())).append('\t')
        .append(tsv(row.signature())).append('\t')
        .append(tsv(row.contract())).append('\n'));
    return out.toString();
  }

  private static String findings(RepositorySupersetInventory inventory) {
    StringBuilder out = new StringBuilder("kind\tpath\tline\tevidence\n");
    inventory.findings().forEach(row -> out.append(tsv(row.kind())).append('\t')
        .append(tsv(row.path())).append('\t')
        .append(row.line()).append('\t')
        .append(tsv(row.evidence())).append('\n'));
    return out.toString();
  }

  private static String summary(RepositorySupersetInventory inventory) {
    RepositorySupersetInventory.Summary s = inventory.summary();
    StringBuilder out = new StringBuilder("metric\tvalue\n");
    metric(out, "files", s.files());
    metric(out, "bytes", s.bytes());
    metric(out, "modules", s.modules());
    metric(out, "registeredModules", s.registeredModules());
    metric(out, "javaFiles", s.javaFiles());
    metric(out, "testJavaFiles", s.testJavaFiles());
    metric(out, "apiFacts", s.apiFacts());
    metric(out, "findings", s.findings());
    metric(out, "exactDuplicateJavaFiles", s.exactDuplicateJavaFiles());
    metric(out, "placeholderFindings", s.placeholderFindings());
    metric(out, "unsupportedOperationFindings", s.unsupportedOperationFindings());
    metric(out, "mindexFiles", s.mindexFiles());
    metric(out, "indexFiles", s.indexFiles());
    metric(out, "precomputeFiles", s.precomputeFiles());
    metric(out, "cacheFiles", s.cacheFiles());
    metric(out, "adapterFiles", s.adapterFiles());
    metric(out, "serializationFiles", s.serializationFiles());
    metric(out, "jiniFiles", s.jiniFiles());
    metric(out, "progressMonitorFiles", s.progressMonitorFiles());
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

  private static void update(MessageDigest digest, String value) {
    digest.update(value.getBytes(StandardCharsets.UTF_8));
  }
}
