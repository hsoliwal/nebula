// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable segment-boundary Maven-module ownership index.
 *
 * <p>Lookup walks only repository path ancestors and performs exact hash lookups. This replaces
 * repeated scans over every module path while retaining deterministic deepest-owner semantics.</p>
 */
public final class RepositoryModulePathIndex {
  private final Set<String> paths;
  private final List<String> ordered;
  private final String rootSha256;

  private RepositoryModulePathIndex(Collection<String> sourcePaths) {
    TreeSet<String> normalized = new TreeSet<>();
    normalized.add(".");
    for (String path : Objects.requireNonNull(sourcePaths, "sourcePaths")) {
      normalized.add(normalizeModule(path));
    }
    ordered = List.copyOf(normalized);
    paths = Set.copyOf(normalized);
    rootSha256 = fingerprint(ordered);
  }

  public static RepositoryModulePathIndex ofPaths(Collection<String> paths) {
    return new RepositoryModulePathIndex(paths);
  }

  public static RepositoryModulePathIndex fromModules(
      List<RepositorySupersetInventory.ModuleRecord> modules) {
    Objects.requireNonNull(modules, "modules");
    return ofPaths(modules.stream().map(RepositorySupersetInventory.ModuleRecord::path).toList());
  }

  public static RepositoryModulePathIndex fromPomFiles(List<RepoFileRecord> files) {
    Objects.requireNonNull(files, "files");
    ArrayList<String> modules = new ArrayList<>();
    for (RepoFileRecord file : files) {
      String path = file.path();
      if ("pom.xml".equals(path)) {
        modules.add(".");
      } else if (path.endsWith("/pom.xml")) {
        modules.add(path.substring(0, path.length() - "/pom.xml".length()));
      }
    }
    return ofPaths(modules);
  }

  /** Returns the deepest admitted Maven module owning the supplied repository-relative path. */
  public String ownerOf(String repositoryPath) {
    String path = normalizePath(repositoryPath);
    if (".".equals(path)) return ".";
    if (paths.contains(path)) return path;

    int end = path.length();
    while (end > 0) {
      int slash = path.lastIndexOf('/', end - 1);
      if (slash < 0) break;
      String candidate = path.substring(0, slash);
      if (paths.contains(candidate)) return candidate;
      end = slash;
    }
    return ".";
  }

  public boolean owns(String modulePath, String repositoryPath) {
    return normalizeModule(modulePath).equals(ownerOf(repositoryPath));
  }

  public List<String> modulePaths() {
    return ordered;
  }

  public int size() {
    return ordered.size();
  }

  public String rootSha256() {
    return rootSha256;
  }

  private static String normalizeModule(String value) {
    String normalized = Objects.requireNonNull(value, "modulePath").strip().replace('\\', '/');
    if (".".equals(normalized)) return ".";
    return requireRelative(normalized, "modulePath");
  }

  private static String normalizePath(String value) {
    String normalized = Objects.requireNonNull(value, "repositoryPath").strip().replace('\\', '/');
    if (".".equals(normalized)) return ".";
    return requireRelative(normalized, "repositoryPath");
  }

  private static String requireRelative(String value, String label) {
    if (value.isEmpty()
        || value.startsWith("/")
        || value.endsWith("/")
        || value.contains("//")) {
      throw new IllegalArgumentException(label + " must be a normalized repository-relative path");
    }
    for (String segment : value.split("/", -1)) {
      if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
        throw new IllegalArgumentException(label + " contains a non-canonical segment");
      }
    }
    return value;
  }

  private static String fingerprint(List<String> modules) {
    MessageDigest digest = digest();
    update(digest, "REPOSITORY-MODULE-PATH-INDEX/1");
    update(digest, Integer.toString(modules.size()));
    for (String module : modules) update(digest, module);
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    digest.update((byte) (bytes.length >>> 24));
    digest.update((byte) (bytes.length >>> 16));
    digest.update((byte) (bytes.length >>> 8));
    digest.update((byte) bytes.length);
    digest.update(bytes);
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }
}
