// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.build.inventory.RepositoryMechanicalPassIndex;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Content-addressing helpers for evidence without an existing canonical root. */
final class M3RepositorySupersetExecutionHashes {
  private M3RepositorySupersetExecutionHashes() {
    throw new AssertionError("No instances");
  }

  static String mechanicalPassRoot(
      String inventoryRoot, RepositoryMechanicalPassIndex passes) {
    MessageDigest digest = sha256();
    frame(digest, "M3-REPOSITORY-MECHANICAL-PASSES/1");
    frame(digest, inventoryRoot);
    passes.findings().forEach(finding -> frameFinding(digest, finding));
    passes.summaries().forEach(summary -> frameSummary(digest, summary));
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void frameFinding(
      MessageDigest digest, RepositoryMechanicalPassIndex.Finding finding) {
    frame(digest, finding.pass().name());
    frame(digest, finding.severity().name());
    frame(digest, finding.capability());
    frame(digest, finding.path());
    frame(digest, Integer.toString(finding.line()));
    frame(digest, finding.evidence());
    frame(digest, finding.strategy());
  }

  private static void frameSummary(
      MessageDigest digest, RepositoryMechanicalPassIndex.Summary summary) {
    frame(digest, summary.pass().name());
    frame(digest, Integer.toString(summary.findings()));
    frame(digest, Integer.toString(summary.info()));
    frame(digest, Integer.toString(summary.review()));
    frame(digest, Integer.toString(summary.error()));
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
}
