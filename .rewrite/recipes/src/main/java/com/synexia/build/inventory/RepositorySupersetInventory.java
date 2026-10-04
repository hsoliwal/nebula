// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Deterministic whole-repository baseline for additive-superset work.
 *
 * <p>The inventory is evidence only. A finding is not automatic rewrite or promotion authority.</p>
 */
public record RepositorySupersetInventory(
    List<ModuleRecord> modules,
    List<DependencyRecord> dependencies,
    List<FileRecord> files,
    List<ApiFact> apiFacts,
    List<Finding> findings,
    Summary summary) {

  public RepositorySupersetInventory {
    modules = List.copyOf(Objects.requireNonNull(modules, "modules"));
    dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
    files = List.copyOf(Objects.requireNonNull(files, "files"));
    apiFacts = List.copyOf(Objects.requireNonNull(apiFacts, "apiFacts"));
    findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
    summary = Objects.requireNonNull(summary, "summary");
  }

  public String rootSha256() {
    return RepositorySupersetInventoryWriter.rootSha256(this);
  }

  public record ModuleRecord(
      String path,
      String groupId,
      String artifactId,
      String packaging,
      boolean reactorRegistered,
      int mainJavaFiles,
      int testJavaFiles,
      int resourceFiles,
      SortedSet<String> roles,
      String pomSha256) {

    public ModuleRecord {
      path = required(path, "path");
      groupId = value(groupId);
      artifactId = required(artifactId, "artifactId");
      packaging = value(packaging).isBlank() ? "jar" : packaging;
      roles = java.util.Collections.unmodifiableSortedSet(
          new TreeSet<>(Objects.requireNonNullElse(roles, new TreeSet<>())));
      pomSha256 = sha256(pomSha256, "pomSha256");
      if (mainJavaFiles < 0 || testJavaFiles < 0 || resourceFiles < 0) {
        throw new IllegalArgumentException("negative module file count");
      }
    }
  }

  public record DependencyRecord(
      String modulePath,
      String groupId,
      String artifactId,
      String version,
      String scope,
      boolean optional) {

    public DependencyRecord {
      modulePath = required(modulePath, "modulePath");
      groupId = value(groupId);
      artifactId = required(artifactId, "artifactId");
      version = value(version);
      scope = value(scope);
    }
  }

  public record FileRecord(
      String path,
      long bytes,
      String sha256,
      String packageName,
      boolean javaSource,
      boolean testSource,
      int classes,
      int interfaces,
      int methods,
      SortedSet<String> roles) {

    public FileRecord {
      path = required(path, "path");
      if (bytes < 0L) throw new IllegalArgumentException("bytes");
      sha256 = RepositorySupersetInventory.sha256(sha256, "sha256");
      packageName = value(packageName);
      if (classes < 0 || interfaces < 0 || methods < 0) {
        throw new IllegalArgumentException("negative Java declaration count");
      }
      roles = java.util.Collections.unmodifiableSortedSet(
          new TreeSet<>(Objects.requireNonNullElse(roles, new TreeSet<>())));
    }
  }

  public record ApiFact(
      String path,
      String packageName,
      String kind,
      String name,
      String signature,
      String contract) {

    public ApiFact {
      path = required(path, "path");
      packageName = value(packageName);
      kind = required(kind, "kind");
      name = value(name);
      signature = value(signature);
      contract = value(contract);
    }
  }

  public record Finding(
      String kind,
      String path,
      int line,
      String evidence) {

    public Finding {
      kind = required(kind, "kind");
      path = required(path, "path");
      if (line < 0) throw new IllegalArgumentException("line");
      evidence = value(evidence);
    }
  }

  public record Summary(
      long files,
      long bytes,
      int modules,
      int registeredModules,
      int javaFiles,
      int testJavaFiles,
      int apiFacts,
      int findings,
      int exactDuplicateJavaFiles,
      int placeholderFindings,
      int unsupportedOperationFindings,
      int mindexFiles,
      int indexFiles,
      int precomputeFiles,
      int cacheFiles,
      int adapterFiles,
      int serializationFiles,
      int jiniFiles,
      int progressMonitorFiles) {

    public Summary {
      if (files < 0 || bytes < 0 || modules < 0 || registeredModules < 0
          || javaFiles < 0 || testJavaFiles < 0 || apiFacts < 0 || findings < 0
          || exactDuplicateJavaFiles < 0 || placeholderFindings < 0
          || unsupportedOperationFindings < 0 || mindexFiles < 0 || indexFiles < 0
          || precomputeFiles < 0 || cacheFiles < 0 || adapterFiles < 0
          || serializationFiles < 0 || jiniFiles < 0 || progressMonitorFiles < 0) {
        throw new IllegalArgumentException("negative summary count");
      }
    }
  }

  private static String required(String value, String field) {
    String normalized = value(value).strip();
    if (normalized.isEmpty()) throw new IllegalArgumentException(field);
    return normalized;
  }

  private static String value(String value) {
    return Objects.toString(value, "");
  }

  private static String sha256(String value, String field) {
    String normalized = required(value, field).toLowerCase(java.util.Locale.ROOT);
    if (!normalized.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
    return normalized;
  }
}
