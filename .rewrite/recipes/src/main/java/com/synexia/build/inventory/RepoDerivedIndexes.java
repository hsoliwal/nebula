// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable precomputed indexes over one repository inventory.
 *
 * <p>The inventory scan is the expensive pass. Repeated repository queries should not repeatedly walk
 * every file/API record, so this class materializes the common lookup dimensions once and exposes only
 * immutable views.
 */
public final class RepoDerivedIndexes {
  private final Map<String, RepoFileRecord> filesByPath;
  private final Map<String, List<RepoFileRecord>> filesByModule;
  private final Map<RepoSignal, List<RepoFileRecord>> filesBySignal;
  private final Map<String, List<RepoApiRecord>> apisByOwner;
  private final Map<String, List<RepoApiRecord>> apisByQualifiedName;
  private final String fingerprint;

  private RepoDerivedIndexes(
      Map<String, RepoFileRecord> filesByPath,
      Map<String, List<RepoFileRecord>> filesByModule,
      Map<RepoSignal, List<RepoFileRecord>> filesBySignal,
      Map<String, List<RepoApiRecord>> apisByOwner,
      Map<String, List<RepoApiRecord>> apisByQualifiedName) {
    this.filesByPath = Map.copyOf(filesByPath);
    this.filesByModule = freezeLists(filesByModule);
    this.filesBySignal = freezeEnumLists(filesBySignal);
    this.apisByOwner = freezeLists(apisByOwner);
    this.apisByQualifiedName = freezeLists(apisByQualifiedName);
    this.fingerprint = computeFingerprint();
  }

  /** Builds deterministic O(1)-key indexes from the canonical sorted inventory. */
  public static RepoDerivedIndexes build(List<RepoFileRecord> files, List<RepoApiRecord> apis) {
    Objects.requireNonNull(files, "files");
    Objects.requireNonNull(apis, "apis");

    Map<String, RepoFileRecord> byPath = new LinkedHashMap<>();
    Map<String, List<RepoFileRecord>> byModule = new LinkedHashMap<>();
    EnumMap<RepoSignal, List<RepoFileRecord>> bySignal = new EnumMap<>(RepoSignal.class);
    Map<String, List<RepoApiRecord>> byOwner = new LinkedHashMap<>();
    Map<String, List<RepoApiRecord>> byQualifiedName = new LinkedHashMap<>();

    for (RepoFileRecord file : files) {
      RepoFileRecord previous = byPath.putIfAbsent(file.path(), file);
      if (previous != null) {
        throw new IllegalArgumentException("duplicate inventoried path: " + file.path());
      }
      byModule.computeIfAbsent(file.module(), ignored -> new ArrayList<>()).add(file);
      for (RepoSignal signal : file.signals()) {
        bySignal.computeIfAbsent(signal, ignored -> new ArrayList<>()).add(file);
      }
    }

    for (RepoApiRecord api : apis) {
      byOwner.computeIfAbsent(api.owner(), ignored -> new ArrayList<>()).add(api);
      byQualifiedName
          .computeIfAbsent(qualifiedName(api.owner(), api.name()), ignored -> new ArrayList<>())
          .add(api);
    }

    Comparator<RepoFileRecord> fileOrder = Comparator.comparing(RepoFileRecord::path);
    byModule.values().forEach(values -> values.sort(fileOrder));
    bySignal.values().forEach(values -> values.sort(fileOrder));

    Comparator<RepoApiRecord> apiOrder =
        Comparator.comparing(RepoApiRecord::path)
            .thenComparingInt(RepoApiRecord::line)
            .thenComparing(RepoApiRecord::signature);
    byOwner.values().forEach(values -> values.sort(apiOrder));
    byQualifiedName.values().forEach(values -> values.sort(apiOrder));

    return new RepoDerivedIndexes(byPath, byModule, bySignal, byOwner, byQualifiedName);
  }

  public RepoFileRecord requireFile(String path) {
    RepoFileRecord file = filesByPath.get(Objects.requireNonNull(path, "path"));
    if (file == null) throw new IllegalArgumentException("file not inventoried: " + path);
    return file;
  }

  public List<RepoFileRecord> filesByModule(String module) {
    return filesByModule.getOrDefault(required(module, "module"), List.of());
  }

  public List<RepoFileRecord> filesBySignal(RepoSignal signal) {
    return filesBySignal.getOrDefault(Objects.requireNonNull(signal, "signal"), List.of());
  }

  public List<RepoApiRecord> apisByOwner(String owner) {
    return apisByOwner.getOrDefault(required(owner, "owner"), List.of());
  }

  public List<RepoApiRecord> apisByQualifiedName(String owner, String name) {
    return apisByQualifiedName.getOrDefault(
        qualifiedName(required(owner, "owner"), required(name, "name")), List.of());
  }

  public List<String> modules() {
    return filesByModule.keySet().stream().sorted().toList();
  }

  public String fingerprint() {
    return fingerprint;
  }

  private String computeFingerprint() {
    MessageDigest digest = sha256();
    update(digest, "RepoDerivedIndexes/v1");

    filesByPath.keySet().stream().sorted().forEach(path -> update(digest, "P:" + path));
    filesByModule.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              update(digest, "M:" + entry.getKey());
              entry.getValue().forEach(file -> update(digest, file.path()));
            });
    for (RepoSignal signal : RepoSignal.values()) {
      List<RepoFileRecord> values = filesBySignal.getOrDefault(signal, List.of());
      update(digest, "S:" + signal.name());
      values.forEach(file -> update(digest, file.path()));
    }
    apisByQualifiedName.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              update(digest, "A:" + entry.getKey());
              entry.getValue().forEach(api -> update(digest, api.path() + ":" + api.line()));
            });
    return HexFormat.of().formatHex(digest.digest());
  }

  private static <K, V> Map<K, List<V>> freezeLists(Map<K, List<V>> source) {
    Map<K, List<V>> frozen = new LinkedHashMap<>();
    source.entrySet().stream()
        .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
        .forEach(entry -> frozen.put(entry.getKey(), List.copyOf(entry.getValue())));
    return Collections.unmodifiableMap(frozen);
  }

  private static Map<RepoSignal, List<RepoFileRecord>> freezeEnumLists(
      Map<RepoSignal, List<RepoFileRecord>> source) {
    EnumMap<RepoSignal, List<RepoFileRecord>> frozen = new EnumMap<>(RepoSignal.class);
    source.forEach((signal, values) -> frozen.put(signal, List.copyOf(values)));
    return Collections.unmodifiableMap(frozen);
  }

  private static String qualifiedName(String owner, String name) {
    return owner + "#" + name;
  }

  private static String required(String value, String label) {
    String checked = Objects.requireNonNull(value, label).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(label);
    return checked;
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    digest.update((byte) (bytes.length >>> 24));
    digest.update((byte) (bytes.length >>> 16));
    digest.update((byte) (bytes.length >>> 8));
    digest.update((byte) bytes.length);
    digest.update(bytes);
  }
}
