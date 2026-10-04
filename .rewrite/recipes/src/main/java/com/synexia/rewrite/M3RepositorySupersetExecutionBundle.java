// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/** Content-addressed receipt for one whole-repository M3 additive-superset planning run. */
public record M3RepositorySupersetExecutionBundle(
    Roots roots, Counts counts, Status status, String root) {

  public M3RepositorySupersetExecutionBundle {
    roots = Objects.requireNonNull(roots, "roots");
    counts = Objects.requireNonNull(counts, "counts");
    status = Objects.requireNonNull(status, "status");
    String emptyHistory = "0".repeat(64);
    if (status.historyCoverageComplete()) {
      if (!status.historyCoverageRequested()
          || roots.developHistory().equals(emptyHistory)) {
        throw new IllegalArgumentException("complete history coverage requires a history root");
      }
    } else if (!roots.developHistory().equals(emptyHistory) || counts.historyCandidates() != 0) {
      throw new IllegalArgumentException("history evidence present without complete coverage");
    }
    String expected = computeRoot(roots, counts, status);
    root = root == null || root.isBlank() ? expected : sha(root, "root");
    if (!expected.equals(root)) {
      throw new IllegalArgumentException("repository execution bundle root mismatch");
    }
  }

  static M3RepositorySupersetExecutionBundle from(
      String[] roots, int[] counts, boolean[] flags, String gitCommit) {
    return new M3RepositorySupersetExecutionBundle(
        roots(roots), counts(counts), status(flags, gitCommit), "");
  }

  public boolean mutationAuthority() {
    return false;
  }

  public boolean donorSourceCopyAuthority() {
    return false;
  }

  public boolean promotionAuthority() {
    return false;
  }

  public void requirePlanningReady() {
    if (status.gitTreeCoverageRequested() && !status.gitTreeCoverageComplete()) {
      throw new IllegalStateException("Git-tree inventory coverage is incomplete");
    }
    requireHistoryReady();
  }

  public void requireHistoryReady() {
    if (status.historyCoverageRequested() && !status.historyCoverageComplete()) {
      throw new IllegalStateException("develop history coverage is incomplete");
    }
  }

  public void requireRecipeExecutionReady() {
    requirePlanningReady();
    if (!status.recipeCoverageReady()) {
      throw new IllegalStateException("repository recipe capability coverage is incomplete");
    }
  }

  /** Canonical roots of every precomputed evidence owner participating in this run. */
  public record Roots(
      String broadInventory,
      String structuralInventory,
      String inventoryConvergence,
      String mechanicalPass,
      String structuralPlan,
      String competitiveDonorEvidence,
      String challengeCoverage,
      String competitivePlan,
      String implementationQueue,
      String apiExecutionIndex,
      String queueRecipeBinding,
      String recipeWorkOrder,
      String recipeCoverage,
      String recipeInventory,
      String developHistory) {

    List<String> values() {
      return List.of(
          broadInventory, structuralInventory, inventoryConvergence, mechanicalPass,
          structuralPlan, competitiveDonorEvidence, challengeCoverage, competitivePlan,
          implementationQueue, apiExecutionIndex, queueRecipeBinding, recipeWorkOrder,
          recipeCoverage, recipeInventory, developHistory);
    }
  }

  /** Cardinalities retained beside roots so drift is visible without re-reading detailed TSVs. */
  public record Counts(
      int broadFiles,
      int structuralFiles,
      int mechanicalFindings,
      int structuralPlanTasks,
      int apiRows,
      int implementationQueueItems,
      int challengeProblems,
      int challengeSourceImplementations,
      int challengePending,
      int recipeCapabilities,
      int recipeSourceRows,
      int historyCandidates) {

    int[] values() {
      return new int[] {
        broadFiles, structuralFiles, mechanicalFindings, structuralPlanTasks,
        apiRows, implementationQueueItems, challengeProblems, challengeSourceImplementations,
        challengePending, recipeCapabilities, recipeSourceRows, historyCandidates
      };
    }
  }

  /** Explicit proof-gate state for this evidence run. */
  public record Status(
      boolean recipeCoverageReady,
      boolean gitTreeCoverageRequested,
      boolean gitTreeCoverageComplete,
      boolean historyCoverageRequested,
      boolean historyCoverageComplete,
      String gitCommit) {}

  private static Roots roots(String[] values) {
    Objects.requireNonNull(values, "roots");
    String[] normalized;
    if (values.length == 13) {
      normalized = new String[15];
      System.arraycopy(values, 0, normalized, 0, 6);
      normalized[6] = "0".repeat(64);
      System.arraycopy(values, 6, normalized, 7, 7);
      normalized[14] = "0".repeat(64);
    } else if (values.length == 14) {
      normalized = java.util.Arrays.copyOf(values, 15);
      normalized[14] = "0".repeat(64);
    } else {
      requireLength(values, 15, "roots");
      normalized = values;
    }
    java.util.Arrays.stream(normalized).forEach(value -> sha(value, "rootValue"));
    return new Roots(
        normalized[0], normalized[1], normalized[2], normalized[3], normalized[4],
        normalized[5], normalized[6], normalized[7], normalized[8], normalized[9],
        normalized[10], normalized[11], normalized[12], normalized[13], normalized[14]);
  }

  private static Counts counts(int[] values) {
    Objects.requireNonNull(values, "counts");
    int[] normalized;
    if (values.length == 8) {
      normalized = new int[12];
      System.arraycopy(values, 0, normalized, 0, 6);
      normalized[6] = 0;
      normalized[7] = 0;
      normalized[8] = 0;
      normalized[9] = values[6];
      normalized[10] = values[7];
    } else if (values.length == 11) {
      normalized = java.util.Arrays.copyOf(values, 12);
    } else {
      requireLength(values, 12, "counts");
      normalized = values;
    }
    if (IntStream.of(normalized).anyMatch(value -> value < 0)
        || normalized[7] < normalized[6]
        || normalized[8] > normalized[6]
        || normalized[10] < normalized[9]) {
      throw new IllegalArgumentException("negative or inconsistent repository execution count");
    }
    return new Counts(
        normalized[0], normalized[1], normalized[2], normalized[3], normalized[4],
        normalized[5], normalized[6], normalized[7], normalized[8], normalized[9],
        normalized[10], normalized[11]);
  }

  private static Status status(boolean[] flags, String gitCommit) {
    Objects.requireNonNull(flags, "flags");
    boolean[] normalized;
    if (flags.length == 3) {
      normalized = java.util.Arrays.copyOf(flags, 5);
    } else {
      requireLength(flags, 5, "flags");
      normalized = flags;
    }
    String commit = Objects.toString(gitCommit, "").strip().toLowerCase(java.util.Locale.ROOT);
    validateGit(normalized[1], normalized[2], commit);
    if (normalized[3] && !normalized[1]) {
      throw new IllegalArgumentException("history coverage requires Git-tree coverage");
    }
    if (normalized[4] && !normalized[3]) {
      throw new IllegalArgumentException("history coverage complete without request");
    }
    return new Status(
        normalized[0], normalized[1], normalized[2], normalized[3], normalized[4], commit);
  }

  private static void validateGit(boolean requested, boolean complete, String commit) {
    if (requested && !(commit.matches("[0-9a-f]{40}") || commit.matches("[0-9a-f]{64}"))) {
      throw new IllegalArgumentException("gitCommit");
    }
    if (!requested && (!commit.isEmpty() || complete)) {
      throw new IllegalArgumentException("Git-tree state without requested coverage");
    }
  }

  private static String computeRoot(Roots roots, Counts counts, Status status) {
    MessageDigest digest = sha256();
    frame(digest, "M3-REPOSITORY-SUPERSET-EXECUTION-BUNDLE/4");
    roots.values().forEach(value -> frame(digest, value));
    IntStream.of(counts.values()).forEach(value -> frame(digest, Integer.toString(value)));
    frameStatus(digest, status);
    frameAuthority(digest);
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void frameStatus(MessageDigest digest, Status status) {
    frame(digest, Boolean.toString(status.recipeCoverageReady()));
    frame(digest, Boolean.toString(status.gitTreeCoverageRequested()));
    frame(digest, Boolean.toString(status.gitTreeCoverageComplete()));
    frame(digest, Boolean.toString(status.historyCoverageRequested()));
    frame(digest, Boolean.toString(status.historyCoverageComplete()));
    frame(digest, status.gitCommit());
  }

  private static void frameAuthority(MessageDigest digest) {
    frame(digest, "mutationAuthority=false");
    frame(digest, "donorSourceCopyAuthority=false");
    frame(digest, "promotionAuthority=false");
  }

  private static void frame(MessageDigest digest, String value) {
    byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
    digest.update((byte) (bytes.length >>> 24));
    digest.update((byte) (bytes.length >>> 16));
    digest.update((byte) (bytes.length >>> 8));
    digest.update((byte) bytes.length);
    digest.update(bytes);
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static String sha(String value, String field) {
    String checked = Objects.toString(value, "").strip().toLowerCase(java.util.Locale.ROOT);
    if (!checked.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }

  private static void requireLength(Object values, int expected, String field) {
    int actual = java.lang.reflect.Array.getLength(Objects.requireNonNull(values, field));
    if (actual != expected) {
      throw new IllegalArgumentException(field + " length");
    }
  }
}
