// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.corpus.ChallengePlatform;
import com.synexia.algorithms.corpus.ChallengeSupersetCoverage;
import com.synexia.job.IProgressMonitor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Content-addressed tri-platform challenge coverage evidence for one repository superset run.
 *
 * <p>The evidence joins the existing LeetCode, HackerRank and GeeksforGeeks catalogue state into
 * the repository receipt. It is catalogue/provenance evidence only and never grants challenge-site
 * source-copy, replacement, mutation or promotion authority.</p>
 */
public record M3RepositoryChallengeCoverageEvidence(
    String tsv,
    String root,
    int logicalProblems,
    int sourceImplementations,
    int sourceSupersetted,
    int pending,
    List<Platform> platforms) {

  public M3RepositoryChallengeCoverageEvidence {
    tsv = Objects.requireNonNull(tsv, "tsv");
    root = sha(root, "root");
    platforms = List.copyOf(Objects.requireNonNull(platforms, "platforms")).stream()
        .sorted(Comparator.comparing(Platform::platform))
        .toList();
    if (!root.equals(sha256(tsv))) {
      throw new IllegalArgumentException("challenge coverage root mismatch");
    }
    if (logicalProblems < 0
        || sourceImplementations < logicalProblems
        || sourceSupersetted < 0
        || sourceSupersetted > logicalProblems
        || pending < 0
        || pending > logicalProblems
        || sourceSupersetted + pending != logicalProblems) {
      throw new IllegalArgumentException("challenge coverage counts");
    }
    int summedProblems = platforms.stream().mapToInt(Platform::logicalProblems).sum();
    int summedImplementations = platforms.stream().mapToInt(Platform::sourceImplementations).sum();
    int summedSupersetted = platforms.stream().mapToInt(Platform::sourceSupersetted).sum();
    int summedPending = platforms.stream().mapToInt(Platform::pending).sum();
    if (summedProblems != logicalProblems
        || summedImplementations != sourceImplementations
        || summedSupersetted != sourceSupersetted
        || summedPending != pending) {
      throw new IllegalArgumentException("challenge platform summary mismatch");
    }
  }

  public static M3RepositoryChallengeCoverageEvidence capture(IProgressMonitor monitor) {
    Objects.requireNonNull(monitor, "monitor").checkCanceled();
    String tsv = ChallengeSupersetCoverage.renderTsv(monitor);
    ArrayList<Platform> rows = new ArrayList<>();
    int problems = 0;
    int implementations = 0;
    int supersetted = 0;
    int pending = 0;
    for (ChallengePlatform platform : ChallengeSupersetCoverage.reviewPlatforms()) {
      ChallengeSupersetCoverage.PlatformSummary summary =
          ChallengeSupersetCoverage.platformSummary(platform);
      Platform row =
          new Platform(
              platform.name(),
              summary.logicalProblems(),
              summary.sourceImplementations(),
              summary.sourceSupersetted(),
              summary.pending());
      rows.add(row);
      problems = Math.addExact(problems, row.logicalProblems());
      implementations = Math.addExact(implementations, row.sourceImplementations());
      supersetted = Math.addExact(supersetted, row.sourceSupersetted());
      pending = Math.addExact(pending, row.pending());
    }
    return new M3RepositoryChallengeCoverageEvidence(
        tsv, sha256(tsv), problems, implementations, supersetted, pending, rows);
  }

  public boolean complete() {
    return pending == 0;
  }

  public boolean mutationAuthority() {
    return false;
  }

  public boolean sourceCopyAuthority() {
    return false;
  }

  public boolean replacementAuthority() {
    return false;
  }

  public boolean promotionAuthority() {
    return false;
  }

  public record Platform(
      String platform,
      int logicalProblems,
      int sourceImplementations,
      int sourceSupersetted,
      int pending) {

    public Platform {
      platform = token(platform, "platform");
      if (logicalProblems < 0
          || sourceImplementations < logicalProblems
          || sourceSupersetted < 0
          || sourceSupersetted > logicalProblems
          || pending < 0
          || sourceSupersetted + pending != logicalProblems) {
        throw new IllegalArgumentException("platform counts");
      }
    }
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static String sha(String value, String field) {
    String checked = token(value, field).toLowerCase(java.util.Locale.ROOT);
    if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
    return checked;
  }

  private static String token(String value, String field) {
    String checked = Objects.toString(value, "").strip();
    if (checked.isEmpty()
        || checked.indexOf('\0') >= 0
        || checked.indexOf('\t') >= 0
        || checked.indexOf('\n') >= 0
        || checked.indexOf('\r') >= 0) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }
}
