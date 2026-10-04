// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reads existing pinned competitive-programming donor ledgers; never donor implementation bodies. */
final class RepositoryCompetitiveDonorCatalog {
  static final String CHALLENGE_SUPERSET =
      "synexia-algo/docs/challenge-supersets-20260925/DONORS.tsv";
  static final String HACKERRANK_TYPED =
      "synexia-algo/docs/hackerrank-typed-runtime-20260925/donors.tsv";
  static final String HACKERRANK_BINDINGS =
      "synexia-algo/docs/hackerrank-typed-runtime-20260925/bindings.tsv";
  static final String GFG_TYPED =
      "synexia-algo/docs/gfg-typed-runtime-20260928/donors.tsv";
  static final String GFG_BINDINGS =
      "synexia-algo/docs/gfg-typed-runtime-20260928/bindings.tsv";

  private RepositoryCompetitiveDonorCatalog() {}

  static List<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> load(Path repositoryRoot)
      throws IOException {
    Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot").toAbsolutePath().normalize();
    if (!Files.isDirectory(root)) throw new IOException("not a repository directory: " + root);

    ArrayList<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> rows = new ArrayList<>();
    Path challenge = safe(root, CHALLENGE_SUPERSET);
    if (Files.isRegularFile(challenge)) rows.addAll(readChallengeSuperset(challenge));
    Path typed = safe(root, HACKERRANK_TYPED);
    if (Files.isRegularFile(typed)) {
      Path bindingPath = safe(root, HACKERRANK_BINDINGS);
      if (!Files.isRegularFile(bindingPath)) {
        throw new IOException("typed HackerRank donor manifest requires " + HACKERRANK_BINDINGS);
      }
      rows.addAll(
          readTypedDonors(
              typed,
              HACKERRANK_TYPED,
              RepositoryCompetitiveAlgorithmPlan.ManifestKind.HACKERRANK_TYPED,
              "",
              readTypedBindings(
                  bindingPath,
                  HACKERRANK_BINDINGS,
                  "HackerRankTypedProblems")));
    }

    Path gfgTyped = safe(root, GFG_TYPED);
    if (Files.isRegularFile(gfgTyped)) {
      Path bindingPath = safe(root, GFG_BINDINGS);
      if (!Files.isRegularFile(bindingPath)) {
        throw new IOException("typed GeeksforGeeks donor manifest requires " + GFG_BINDINGS);
      }
      rows.addAll(
          readTypedDonors(
              gfgTyped,
              GFG_TYPED,
              RepositoryCompetitiveAlgorithmPlan.ManifestKind.GFG_TYPED,
              "geeksforgeeks:",
              readTypedBindings(
                  bindingPath,
                  GFG_BINDINGS,
                  "GeeksForGeeksTypedProblems")));
    }

    rows.sort(
        Comparator.comparing((RepositoryCompetitiveAlgorithmPlan.DonorEvidence row) ->
                row.manifestKind().name())
            .thenComparing(RepositoryCompetitiveAlgorithmPlan.DonorEvidence::identity)
            .thenComparing(RepositoryCompetitiveAlgorithmPlan.DonorEvidence::repository)
            .thenComparing(RepositoryCompetitiveAlgorithmPlan.DonorEvidence::sourcePath)
            .thenComparing(RepositoryCompetitiveAlgorithmPlan.DonorEvidence::sourceBlob));
    return List.copyOf(rows);
  }

  private static List<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> readChallengeSuperset(
      Path path) throws IOException {
    Tsv tsv = Tsv.read(path);
    tsv.requireColumns(
        "stable_id", "title", "shape", "kernel", "repository", "commit", "path", "blob_sha",
        "license", "observed_mechanic", "promotion_note");

    ArrayList<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> rows = new ArrayList<>();
    for (Map<String, String> row : tsv.rows()) {
      String identity = row.get("stable_id");
      if (!(identity.startsWith("leetcode:") || identity.startsWith("hackerrank:"))) continue;
      rows.add(
          new RepositoryCompetitiveAlgorithmPlan.DonorEvidence(
              RepositoryCompetitiveAlgorithmPlan.ManifestKind.CHALLENGE_SUPERSET,
              CHALLENGE_SUPERSET,
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
              RepositoryCompetitiveAlgorithmPlan.SourceDisposition.NOT_STATED));
    }
    return rows;
  }

  private static List<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> readTypedDonors(
      Path path,
      String manifestPath,
      RepositoryCompetitiveAlgorithmPlan.ManifestKind manifestKind,
      String identityPrefix,
      Map<String, TypedBinding> bindings)
      throws IOException {
    Tsv tsv = Tsv.read(path);
    tsv.requireColumns(
        "challenge", "repository", "commit", "source_path", "source_blob", "declared_license",
        "source_copied", "mechanic");

    ArrayList<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> rows = new ArrayList<>();
    for (Map<String, String> row : tsv.rows()) {
      String challenge = row.get("challenge");
      TypedBinding binding = bindings.get(challenge);
      RepositoryCompetitiveAlgorithmPlan.SourceDisposition disposition =
          switch (row.get("source_copied")) {
            case "false" -> RepositoryCompetitiveAlgorithmPlan.SourceDisposition.NOT_COPIED;
            case "true" -> RepositoryCompetitiveAlgorithmPlan.SourceDisposition.COPIED;
            default -> throw new IOException("invalid source_copied in " + manifestPath);
          };
      rows.add(
          new RepositoryCompetitiveAlgorithmPlan.DonorEvidence(
              manifestKind,
              manifestPath,
              identityPrefix.isEmpty() ? challenge : identityPrefix + slug(challenge),
              challenge,
              binding == null ? "" : binding.shape(),
              binding == null ? "" : binding.owner(),
              row.get("repository"),
              row.get("commit"),
              row.get("source_path"),
              row.get("source_blob"),
              row.get("declared_license"),
              row.get("mechanic"),
              binding == null ? "" : binding.projection(),
              disposition));
    }
    return rows;
  }

  private static Map<String, TypedBinding> readTypedBindings(
      Path path, String bindingPath, String expectedOwnerPrefix) throws IOException {
    Tsv tsv = Tsv.read(path);
    tsv.requireColumns(
        "challenge", "api_method", "shape", "mode", "mechanism", "complexity", "owner");
    HashMap<String, TypedBinding> result = new HashMap<>();
    for (Map<String, String> row : tsv.rows()) {
      String challenge = row.get("challenge");
      String apiMethod = row.get("api_method");
      String owner = row.get("owner");
      if (!owner.equals(expectedOwnerPrefix + "." + apiMethod)) {
        throw new IOException(
            "typed binding owner/method mismatch in " + bindingPath + ": " + challenge);
      }
      TypedBinding binding =
          new TypedBinding(
              apiMethod,
              row.get("shape"),
              row.get("mode"),
              row.get("mechanism"),
              row.get("complexity"),
              owner,
              bindingPath);
      if (result.putIfAbsent(challenge, binding) != null) {
        throw new IOException("duplicate typed binding in " + bindingPath + ": " + challenge);
      }
    }
    return Map.copyOf(result);
  }

  private static String slug(String value) {
    String lower = required(value, "identity").toLowerCase(java.util.Locale.ROOT);
    StringBuilder out = new StringBuilder(lower.length());
    for (int index = 0; index < lower.length(); index++) {
      char current = lower.charAt(index);
      if (Character.isLetterOrDigit(current)) out.append(current);
    }
    return out.toString();
  }

  private static Path safe(Path root, String relative) throws IOException {
    Path path = root.resolve(relative).normalize();
    if (!path.startsWith(root)) throw new IOException("donor manifest path escape");
    return path;
  }

  private record TypedBinding(
      String apiMethod,
      String shape,
      String mode,
      String mechanism,
      String complexity,
      String owner,
      String bindingPath) {
    private TypedBinding {
      apiMethod = required(apiMethod, "apiMethod");
      shape = required(shape, "shape");
      mode = required(mode, "mode");
      mechanism = required(mechanism, "mechanism");
      complexity = required(complexity, "complexity");
      owner = required(owner, "owner");
      bindingPath = required(bindingPath, "bindingPath");
    }

    String projection() {
      return "binding=" + bindingPath
          + ";mode=" + mode
          + ";complexity=" + complexity
          + ";mechanism=" + mechanism;
    }
  }

  private static String required(String value, String field) {
    String normalized = Objects.toString(value, "").strip();
    if (normalized.isEmpty()) throw new IllegalArgumentException(field);
    return normalized;
  }

  private record Tsv(List<String> header, List<Map<String, String>> rows) {
    static Tsv read(Path path) throws IOException {
      List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
      String headerLine = null;
      int headerLineNumber = -1;
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        if (!line.isBlank() && !line.startsWith("#")) {
          headerLine = line;
          headerLineNumber = i;
          break;
        }
      }
      if (headerLine == null) throw new IOException("empty donor TSV: " + path);
      List<String> header = List.of(headerLine.split("\\t", -1));
      Set<String> unique = new HashSet<>(header);
      if (header.stream().anyMatch(String::isBlank) || unique.size() != header.size()) {
        throw new IOException("invalid donor TSV header: " + path);
      }

      ArrayList<Map<String, String>> rows = new ArrayList<>();
      for (int i = headerLineNumber + 1; i < lines.size(); i++) {
        String line = lines.get(i);
        if (line.isBlank() || line.startsWith("#")) continue;
        String[] fields = line.split("\\t", -1);
        if (fields.length != header.size()) {
          throw new IOException("donor TSV width mismatch: " + path + ":" + (i + 1));
        }
        HashMap<String, String> row = new HashMap<>();
        for (int column = 0; column < fields.length; column++) {
          row.put(header.get(column), fields[column]);
        }
        rows.add(Map.copyOf(row));
      }
      return new Tsv(List.copyOf(header), List.copyOf(rows));
    }

    void requireColumns(String... columns) throws IOException {
      for (String column : columns) {
        if (!header.contains(column)) {
          throw new IOException("donor TSV missing column " + column);
        }
      }
    }
  }
}
