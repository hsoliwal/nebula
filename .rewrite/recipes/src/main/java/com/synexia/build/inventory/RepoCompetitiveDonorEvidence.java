// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic projection of already-reviewed LeetCode/HackerRank/GeeksforGeeks donor evidence.
 *
 * <p>The projection reads checked-in provenance ledgers only. It never clones, downloads, compiles,
 * executes, or copies donor source. Challenge identity and observed mechanics are evidence for
 * review, not semantic-equivalence or promotion authority.</p>
 */
public final class RepoCompetitiveDonorEvidence {
  public static final String CHALLENGE_SUPERSET_MANIFEST =
      "synexia-algo/docs/challenge-supersets-20260925/DONORS.tsv";
  public static final String HACKERRANK_TYPED_MANIFEST =
      "synexia-algo/docs/hackerrank-typed-runtime-20260925/donors.tsv";
  public static final String GFG_TYPED_MANIFEST =
      "synexia-algo/docs/gfg-typed-runtime-20260928/donors.tsv";

  private static final long MAX_MANIFEST_BYTES = 8L * 1024L * 1024L;
  private static final Comparator<Evidence> ORDER =
      Comparator.comparing(Evidence::manifestPath)
          .thenComparing(Evidence::identity)
          .thenComparing(Evidence::repository)
          .thenComparing(Evidence::sourcePath)
          .thenComparing(Evidence::sourceBlob);

  /** One immutable donor-evidence row bound to the exact checked-in manifest bytes. */
  public record Evidence(
      String manifestPath,
      String manifestSha256,
      String identity,
      String title,
      String shape,
      String kernel,
      String repository,
      String commit,
      String sourcePath,
      String sourceBlob,
      String license,
      String observedMechanic,
      String promotionNote,
      String copyStatus) {

    public Evidence {
      manifestPath = required(manifestPath, "manifestPath");
      manifestSha256 = sha256(manifestSha256, "manifestSha256");
      identity = required(identity, "identity");
      title = required(title, "title");
      shape = value(shape);
      kernel = value(kernel);
      repository = required(repository, "repository");
      commit = gitObject(commit, "commit");
      sourcePath = required(sourcePath, "sourcePath");
      sourceBlob = gitObject(sourceBlob, "sourceBlob");
      license = required(license, "license");
      observedMechanic = required(observedMechanic, "observedMechanic");
      promotionNote = value(promotionNote);
      copyStatus = required(copyStatus, "copyStatus");
    }

    public boolean sourceCopied() {
      return "COPIED".equals(copyStatus);
    }
  }

  private RepoCompetitiveDonorEvidence() {
    throw new AssertionError("No instances");
  }

  /**
   * Reads the reviewed donor ledgers only when those exact files are present in the canonical
   * repository inventory.
   */
  public static List<Evidence> scan(Path repositoryRoot, RepoSupersetInventory inventory)
      throws IOException {
    Objects.requireNonNull(inventory, "inventory");
    Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot")
        .toAbsolutePath().normalize();

    ArrayList<Evidence> rows = new ArrayList<>();
    addIfPresent(root, inventory, CHALLENGE_SUPERSET_MANIFEST, rows);
    addIfPresent(root, inventory, HACKERRANK_TYPED_MANIFEST, rows);
    addIfPresent(root, inventory, GFG_TYPED_MANIFEST, rows);

    return canonicalOrder(rows);
  }

  /** Deterministic SHA-256 identity over the sorted evidence rows. */
  public static String rootSha256(List<Evidence> evidence) {
    List<Evidence> ordered = canonicalOrder(evidence);
    MessageDigest digest = sha();
    update(digest, "REPO-COMPETITIVE-DONOR-EVIDENCE/V1");
    update(digest, Integer.toString(ordered.size()));
    for (Evidence row : ordered) {
      update(digest, row.manifestPath());
      update(digest, row.manifestSha256());
      update(digest, row.identity());
      update(digest, row.title());
      update(digest, row.shape());
      update(digest, row.kernel());
      update(digest, row.repository());
      update(digest, row.commit());
      update(digest, row.sourcePath());
      update(digest, row.sourceBlob());
      update(digest, row.license());
      update(digest, row.observedMechanic());
      update(digest, row.promotionNote());
      update(digest, row.copyStatus());
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  static List<Evidence> canonicalOrder(List<Evidence> evidence) {
    ArrayList<Evidence> ordered =
        new ArrayList<>(Objects.requireNonNull(evidence, "evidence"));
    ordered.sort(ORDER);
    return List.copyOf(ordered);
  }

  private static void addIfPresent(
      Path root,
      RepoSupersetInventory inventory,
      String manifestPath,
      List<Evidence> target)
      throws IOException {
    RepoFileRecord file;
    try {
      file = inventory.requireFile(manifestPath);
    } catch (IllegalArgumentException absent) {
      return;
    }

    if (file.sizeBytes() > MAX_MANIFEST_BYTES) {
      throw new IOException("COMPETITIVE_DONOR_MANIFEST_TOO_LARGE: " + manifestPath);
    }

    Path path = safeChild(root, manifestPath);
    if (Files.isSymbolicLink(path)
        || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("COMPETITIVE_DONOR_MANIFEST_NOT_REGULAR: " + manifestPath);
    }

    long currentSize = Files.size(path);
    if (currentSize > MAX_MANIFEST_BYTES) {
      throw new IOException("COMPETITIVE_DONOR_MANIFEST_TOO_LARGE: " + manifestPath);
    }
    if (currentSize != file.sizeBytes()) {
      throw new IOException("COMPETITIVE_DONOR_MANIFEST_SIZE_DRIFT: " + manifestPath);
    }
    byte[] bytes = Files.readAllBytes(path);
    if (bytes.length != currentSize) {
      throw new IOException("COMPETITIVE_DONOR_MANIFEST_SIZE_DRIFT: " + manifestPath);
    }
    String sha256 = digest(bytes);
    if (!sha256.equals(file.sha256())) {
      throw new IOException("COMPETITIVE_DONOR_MANIFEST_SHA_DRIFT: " + manifestPath);
    }

    Tsv table = Tsv.read(bytes, manifestPath);
    if (CHALLENGE_SUPERSET_MANIFEST.equals(manifestPath)) {
      target.addAll(parseChallengeSuperset(table, manifestPath, sha256));
    } else if (HACKERRANK_TYPED_MANIFEST.equals(manifestPath)) {
      target.addAll(parseTyped(table, manifestPath, sha256, "hackerrank-typed:"));
    } else if (GFG_TYPED_MANIFEST.equals(manifestPath)) {
      target.addAll(parseTyped(table, manifestPath, sha256, "geeksforgeeks-typed:"));
    } else {
      throw new IOException("unknown competitive donor manifest: " + manifestPath);
    }
  }

  private static List<Evidence> parseChallengeSuperset(
      Tsv table, String manifestPath, String manifestSha256)
      throws IOException {
    table.requireColumns(
        "stable_id",
        "title",
        "shape",
        "kernel",
        "repository",
        "commit",
        "path",
        "blob_sha",
        "license",
        "observed_mechanic",
        "promotion_note");

    ArrayList<Evidence> result = new ArrayList<>();
    for (Map<String, String> row : table.rows()) {
      String identity = row.get("stable_id");
      if (!(identity.startsWith("leetcode:") || identity.startsWith("hackerrank:"))) {
        continue;
      }
      result.add(
          new Evidence(
              manifestPath,
              manifestSha256,
              identity,
              row.get("title"),
              row.get("shape"),
              row.get("kernel"),
              row.get("repository"),
              row.get("commit"),
              row.get("path"),
              row.get("blob_sha"),
              row.get("license"),
              row.get("observed_mechanic"),
              row.get("promotion_note"),
              "UNSPECIFIED_IN_MANIFEST"));
    }
    return List.copyOf(result);
  }

  private static List<Evidence> parseTyped(
      Tsv table,
      String manifestPath,
      String manifestSha256,
      String identityPrefix)
      throws IOException {
    table.requireColumns(
        "challenge",
        "repository",
        "commit",
        "source_path",
        "source_blob",
        "declared_license",
        "source_copied",
        "mechanic");

    ArrayList<Evidence> result = new ArrayList<>();
    for (Map<String, String> row : table.rows()) {
      String copied = row.get("source_copied");
      String copyStatus =
          switch (copied) {
            case "false" -> "NOT_COPIED";
            case "true" -> "COPIED";
            default -> "UNKNOWN:" + copied;
          };
      String challenge = row.get("challenge");
      result.add(
          new Evidence(
              manifestPath,
              manifestSha256,
              identityPrefix + challenge,
              challenge,
              "",
              "",
              row.get("repository"),
              row.get("commit"),
              row.get("source_path"),
              row.get("source_blob"),
              row.get("declared_license"),
              row.get("mechanic"),
              "",
              copyStatus));
    }
    return List.copyOf(result);
  }

  private static Path safeChild(Path root, String relative) throws IOException {
    Path path = root.resolve(relative).normalize();
    if (!path.startsWith(root)) {
      throw new IOException("COMPETITIVE_DONOR_MANIFEST_PATH_ESCAPE");
    }
    return path;
  }

  private static String digest(byte[] bytes) {
    MessageDigest digest = sha();
    digest.update(bytes);
    return HexFormat.of().formatHex(digest.digest());
  }

  private static MessageDigest sha() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = value(value).getBytes(StandardCharsets.UTF_8);
    digest.update((byte) (bytes.length >>> 24));
    digest.update((byte) (bytes.length >>> 16));
    digest.update((byte) (bytes.length >>> 8));
    digest.update((byte) bytes.length);
    digest.update(bytes);
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

  private static String gitObject(String value, String field) {
    String normalized = required(value, field).toLowerCase(java.util.Locale.ROOT);
    if (!normalized.matches("[0-9a-f]{40,64}")) throw new IllegalArgumentException(field);
    return normalized;
  }

  private record Tsv(List<String> header, List<Map<String, String>> rows) {
    static Tsv read(byte[] bytes, String label) throws IOException {
      String content = new String(bytes, StandardCharsets.UTF_8);
      List<String> lines = content.lines().toList();
      int headerIndex = -1;
      String headerLine = null;
      for (int index = 0; index < lines.size(); index++) {
        String line = lines.get(index);
        if (!line.isBlank() && !line.startsWith("#")) {
          headerIndex = index;
          headerLine = line;
          break;
        }
      }
      if (headerIndex < 0 || headerLine == null) {
        throw new IOException("empty donor TSV: " + label);
      }

      List<String> header = List.of(headerLine.split("\\t", -1));
      if (header.stream().anyMatch(String::isBlank)
          || header.stream().distinct().count() != header.size()) {
        throw new IOException("invalid donor TSV header: " + label);
      }

      ArrayList<Map<String, String>> rows = new ArrayList<>();
      for (int index = headerIndex + 1; index < lines.size(); index++) {
        String line = lines.get(index);
        if (line.isBlank() || line.startsWith("#")) continue;
        String[] fields = line.split("\\t", -1);
        if (fields.length != header.size()) {
          throw new IOException(
              "donor TSV width mismatch at " + label + ":" + (index + 1));
        }
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        for (int field = 0; field < fields.length; field++) {
          row.put(header.get(field), fields[field]);
        }
        rows.add(Map.copyOf(row));
      }
      return new Tsv(List.copyOf(header), List.copyOf(rows));
    }

    void requireColumns(String... names) throws IOException {
      for (String name : names) {
        if (!header.contains(name)) {
          throw new IOException("donor TSV missing column " + name);
        }
      }
    }
  }
}
