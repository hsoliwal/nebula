// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable file-local inventory result with preclassified, indexable content metadata. */
public record RepoFileRecord(
    String path,
    String module,
    String extension,
    RepoFileCategory category,
    RepoLanguage language,
    long sizeBytes,
    long lineCount,
    String sha256,
    String structuralSha256,
    String logicSha256,
    long simHash64,
    String packageName,
    List<String> typeNames,
    List<RepoApiRecord> apis,
    Set<RepoSignal> signals) {

  /** Source-compatible constructor retained for existing record clients. */
  public RepoFileRecord(
      String path,
      String module,
      String extension,
      long sizeBytes,
      long lineCount,
      String sha256,
      String structuralSha256,
      String logicSha256,
      long simHash64,
      String packageName,
      List<String> typeNames,
      List<RepoApiRecord> apis,
      Set<RepoSignal> signals) {
    this(
        path,
        module,
        extension,
        RepoFileCategory.OTHER,
        RepoLanguage.UNKNOWN,
        sizeBytes,
        lineCount,
        sha256,
        structuralSha256,
        logicSha256,
        simHash64,
        packageName,
        typeNames,
        apis,
        signals);
  }

  public RepoFileRecord {
    path = required(path, "path");
    module = required(module, "module");
    extension = Objects.requireNonNull(extension, "extension");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(language, "language");
    if (sizeBytes < 0L || lineCount < 0L) throw new IllegalArgumentException("negative size/count");
    sha256 = digest(sha256, "sha256");
    structuralSha256 = digest(structuralSha256, "structuralSha256");
    logicSha256 = digest(logicSha256, "logicSha256");
    packageName = Objects.requireNonNull(packageName, "packageName");
    typeNames = List.copyOf(Objects.requireNonNull(typeNames, "typeNames"));
    apis = List.copyOf(Objects.requireNonNull(apis, "apis"));
    signals = Set.copyOf(Objects.requireNonNull(signals, "signals"));
  }

  private static String required(String value, String label) {
    String checked = Objects.requireNonNull(value, label).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(label);
    return checked;
  }

  private static String digest(String value, String label) {
    String checked = Objects.requireNonNull(value, label);
    if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(label);
    return checked;
  }
}
