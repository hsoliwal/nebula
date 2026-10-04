// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Deterministic writer for reviewed competitive-programming donor evidence. */
public final class RepoCompetitiveDonorEvidenceWriter {
  private RepoCompetitiveDonorEvidenceWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(
      List<RepoCompetitiveDonorEvidence.Evidence> evidence, Path outputDirectory)
      throws IOException {
    List<RepoCompetitiveDonorEvidence.Evidence> checked =
        RepoCompetitiveDonorEvidence.canonicalOrder(evidence);
    Path output = Objects.requireNonNull(outputDirectory, "outputDirectory")
        .toAbsolutePath().normalize();
    Files.createDirectories(output);

    String tableContent = tableContent(checked);
    Path table = output.resolve("COMPETITIVE_DONORS.tsv");
    Files.writeString(table, tableContent, StandardCharsets.UTF_8);

    String root = RepoCompetitiveDonorEvidence.rootSha256(checked);
    String tableSha256 = sha256(tableContent);
    Files.writeString(
        output.resolve("COMPETITIVE_DONORS.sha256"),
        tableSha256 + "  COMPETITIVE_DONORS.tsv\n"
            + "ROOT  " + root + "\n",
        StandardCharsets.UTF_8);
  }

  private static String tableContent(
      List<RepoCompetitiveDonorEvidence.Evidence> evidence) {
    StringBuilder writer = new StringBuilder();
    writer.append(
        "manifest_path\tmanifest_sha256\tidentity\ttitle\tshape\tkernel\trepository\tcommit\tsource_path\tsource_blob\tlicense\tobserved_mechanic\tpromotion_note\tcopy_status\n");
    for (RepoCompetitiveDonorEvidence.Evidence row : evidence) {
      writer.append(tsv(row.manifestPath())).append('\t')
          .append(row.manifestSha256()).append('\t')
          .append(tsv(row.identity())).append('\t')
          .append(tsv(row.title())).append('\t')
          .append(tsv(row.shape())).append('\t')
          .append(tsv(row.kernel())).append('\t')
          .append(tsv(row.repository())).append('\t')
          .append(row.commit()).append('\t')
          .append(tsv(row.sourcePath())).append('\t')
          .append(row.sourceBlob()).append('\t')
          .append(tsv(row.license())).append('\t')
          .append(tsv(row.observedMechanic())).append('\t')
          .append(tsv(row.promotionNote())).append('\t')
          .append(tsv(row.copyStatus())).append('\n');
    }
    return writer.toString();
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static String tsv(String value) {
    return Objects.toString(value, "")
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }
}
