// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import com.synexia.job.IProgressMonitor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Exact reconciliation between the two existing build-inventory projections.
 *
 * <p>This consumes already-built immutable inventories. It does not reread source, execute code,
 * infer semantic equivalence, or grant mutation/promotion authority.</p>
 */
public final class RepositoryInventoryConvergence {
  private RepositoryInventoryConvergence() {}

  public enum DifferenceKind {
    SHA256,
    MAVEN_MODULE_OWNER,
    JAVA_SOURCE_KIND,
    TEST_SOURCE_KIND
  }

  public record Difference(
      DifferenceKind kind,
      String path,
      String broadValue,
      String structuralValue) {

    public Difference {
      kind = Objects.requireNonNull(kind, "kind");
      path = required(path, "path");
      broadValue = Objects.toString(broadValue, "");
      structuralValue = Objects.toString(structuralValue, "");
    }
  }

  public record Report(
      String broadRoot,
      String structuralRoot,
      int broadFiles,
      int structuralFiles,
      int commonFiles,
      int broadOnlyFiles,
      int structuralOnlyFiles,
      int exactShaMatches,
      int moduleOwnerMatches,
      List<Difference> differences,
      String rootSha256) {

    public Report {
      broadRoot = sha(broadRoot, "broadRoot");
      structuralRoot = sha(structuralRoot, "structuralRoot");
      rootSha256 = sha(rootSha256, "rootSha256");
      if (broadFiles < 0
          || structuralFiles < 0
          || commonFiles < 0
          || broadOnlyFiles < 0
          || structuralOnlyFiles < 0
          || exactShaMatches < 0
          || moduleOwnerMatches < 0
          || commonFiles > broadFiles
          || commonFiles > structuralFiles
          || exactShaMatches > commonFiles
          || moduleOwnerMatches > commonFiles) {
        throw new IllegalArgumentException("invalid convergence counts");
      }
      differences = List.copyOf(Objects.requireNonNull(differences, "differences"));
      String expected =
          root(
              broadRoot,
              structuralRoot,
              broadFiles,
              structuralFiles,
              commonFiles,
              broadOnlyFiles,
              structuralOnlyFiles,
              exactShaMatches,
              moduleOwnerMatches,
              differences);
      if (!expected.equals(rootSha256)) {
        throw new IllegalArgumentException("repository convergence root mismatch");
      }
    }

    public boolean sharedPathsAgree() {
      return differences.isEmpty();
    }

    public void requireSharedPathsAgree() {
      if (!sharedPathsAgree()) {
        Difference first = differences.getFirst();
        throw new IllegalStateException(
            "REPOSITORY_INVENTORY_DIVERGENCE:"
                + first.kind()
                + ":"
                + first.path());
      }
    }

    public boolean executionAuthority() {
      return false;
    }

    public boolean canonicalPromotionAuthorized() {
      return false;
    }
  }

  public static Report compare(
      RepoSupersetInventory broad,
      RepositorySupersetInventory structural) {
    return compare(broad, structural, IProgressMonitor.noop());
  }

  public static Report compare(
      RepoSupersetInventory broad,
      RepositorySupersetInventory structural,
      IProgressMonitor monitor) {
    Objects.requireNonNull(broad, "broad");
    Objects.requireNonNull(structural, "structural");
    IProgressMonitor progress = Objects.requireNonNullElse(monitor, IProgressMonitor.noop());

    Map<String, RepoFileRecord> broadByPath = new TreeMap<>();
    broad.files().forEach(file -> broadByPath.put(file.path(), file));
    Map<String, RepositorySupersetInventory.FileRecord> structuralByPath = new TreeMap<>();
    structural.files().forEach(file -> structuralByPath.put(file.path(), file));

    TreeSet<String> common = new TreeSet<>(broadByPath.keySet());
    common.retainAll(structuralByPath.keySet());
    RepositoryModulePathIndex structuralModules =
        RepositoryModulePathIndex.fromModules(structural.modules());

    ArrayList<Difference> differences = new ArrayList<>();
    int exactShaMatches = 0;
    int moduleOwnerMatches = 0;
    progress.beginTask("Repository inventory convergence", common.size());
    try {
      for (String path : common) {
        progress.checkCanceled();
        RepoFileRecord broadFile = broadByPath.get(path);
        RepositorySupersetInventory.FileRecord structuralFile = structuralByPath.get(path);

        if (broadFile.sha256().equals(structuralFile.sha256())) {
          exactShaMatches++;
        } else {
          differences.add(
              new Difference(
                  DifferenceKind.SHA256,
                  path,
                  broadFile.sha256(),
                  structuralFile.sha256()));
        }

        String broadOwner = broad.mavenModule(path);
        String structuralOwner = structuralModules.ownerOf(path);
        if (broadOwner.equals(structuralOwner)) {
          moduleOwnerMatches++;
        } else {
          differences.add(
              new Difference(
                  DifferenceKind.MAVEN_MODULE_OWNER,
                  path,
                  broadOwner,
                  structuralOwner));
        }

        boolean broadJava = ".java".equals(broadFile.extension());
        if (broadJava != structuralFile.javaSource()) {
          differences.add(
              new Difference(
                  DifferenceKind.JAVA_SOURCE_KIND,
                  path,
                  Boolean.toString(broadJava),
                  Boolean.toString(structuralFile.javaSource())));
        }

        boolean broadTest = broadFile.signals().contains(RepoSignal.TEST_SOURCE);
        if (broadTest != structuralFile.testSource()) {
          differences.add(
              new Difference(
                  DifferenceKind.TEST_SOURCE_KIND,
                  path,
                  Boolean.toString(broadTest),
                  Boolean.toString(structuralFile.testSource())));
        }
        progress.worked(1L);
      }
    } finally {
      progress.done();
    }

    differences.sort(
        Comparator.comparing((Difference value) -> value.kind().name())
            .thenComparing(Difference::path)
            .thenComparing(Difference::broadValue)
            .thenComparing(Difference::structuralValue));

    int broadOnly = broadByPath.size() - common.size();
    int structuralOnly = structuralByPath.size() - common.size();
    String broadRoot = broad.fingerprint();
    String structuralRoot = structural.rootSha256();
    String root =
        root(
            broadRoot,
            structuralRoot,
            broadByPath.size(),
            structuralByPath.size(),
            common.size(),
            broadOnly,
            structuralOnly,
            exactShaMatches,
            moduleOwnerMatches,
            differences);
    return new Report(
        broadRoot,
        structuralRoot,
        broadByPath.size(),
        structuralByPath.size(),
        common.size(),
        broadOnly,
        structuralOnly,
        exactShaMatches,
        moduleOwnerMatches,
        differences,
        root);
  }

  private static String root(
      String broadRoot,
      String structuralRoot,
      int broadFiles,
      int structuralFiles,
      int commonFiles,
      int broadOnlyFiles,
      int structuralOnlyFiles,
      int exactShaMatches,
      int moduleOwnerMatches,
      List<Difference> differences) {
    MessageDigest digest = digest();
    update(digest, "REPOSITORY-INVENTORY-CONVERGENCE/1");
    update(digest, broadRoot);
    update(digest, structuralRoot);
    update(digest, Integer.toString(broadFiles));
    update(digest, Integer.toString(structuralFiles));
    update(digest, Integer.toString(commonFiles));
    update(digest, Integer.toString(broadOnlyFiles));
    update(digest, Integer.toString(structuralOnlyFiles));
    update(digest, Integer.toString(exactShaMatches));
    update(digest, Integer.toString(moduleOwnerMatches));
    update(digest, Integer.toString(differences.size()));
    for (Difference difference : differences) {
      update(digest, difference.kind().name());
      update(digest, difference.path());
      update(digest, difference.broadValue());
      update(digest, difference.structuralValue());
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
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

  private static String required(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).strip();
    if (normalized.isEmpty()) throw new IllegalArgumentException(field);
    return normalized;
  }

  private static String sha(String value, String field) {
    String normalized = required(value, field);
    if (!normalized.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
    return normalized;
  }
}
