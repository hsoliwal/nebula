// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic review plan joining repository APIs to pinned LeetCode/HackerRank/GeeksforGeeks donor evidence.
 *
 * <p>A match is mechanical evidence only. It does not prove semantic equivalence, authorize source
 * copying, grant runtime authority, or establish a performance improvement without benchmark proof.</p>
 */
public record RepositoryCompetitiveAlgorithmPlan(
    List<DonorEvidence> donors,
    List<ApiCandidate> candidates,
    Summary summary) {

  public enum ManifestKind {
    CHALLENGE_SUPERSET,
    HACKERRANK_TYPED,
    GFG_TYPED
  }

  public enum SourceDisposition {
    NOT_STATED,
    NOT_COPIED,
    COPIED
  }

  public enum MatchBasis {
    KERNEL_NAME,
    CHALLENGE_TITLE
  }

  public enum Action {
    REUSE_EXISTING_KERNEL,
    REVIEW_DONOR_MECHANIC
  }

  public record DonorEvidence(
      ManifestKind manifestKind,
      String manifestPath,
      String identity,
      String title,
      String shape,
      String kernel,
      String repository,
      String revision,
      String sourcePath,
      String sourceBlob,
      String license,
      String mechanic,
      String projection,
      SourceDisposition sourceDisposition) {

    public DonorEvidence {
      manifestKind = Objects.requireNonNull(manifestKind, "manifestKind");
      manifestPath = required(manifestPath, "manifestPath");
      identity = required(identity, "identity");
      title = value(title);
      shape = value(shape);
      kernel = value(kernel);
      repository = required(repository, "repository");
      revision = gitObject(revision, "revision");
      sourcePath = required(sourcePath, "sourcePath");
      sourceBlob = gitObject(sourceBlob, "sourceBlob");
      license = required(license, "license");
      mechanic = value(mechanic);
      projection = value(projection);
      sourceDisposition = Objects.requireNonNull(sourceDisposition, "sourceDisposition");
    }

    public boolean hasExistingKernel() {
      return !kernel.isBlank();
    }
  }

  public record ApiCandidate(
      String path,
      String modulePath,
      String name,
      String signature,
      MatchBasis matchBasis,
      Action action,
      List<String> donorIdentities,
      List<String> shapes,
      List<String> kernels,
      List<String> repositories,
      List<String> mechanics) {

    public ApiCandidate {
      path = required(path, "path");
      modulePath = required(modulePath, "modulePath");
      name = required(name, "name");
      signature = value(signature);
      matchBasis = Objects.requireNonNull(matchBasis, "matchBasis");
      action = Objects.requireNonNull(action, "action");
      donorIdentities = immutableNonBlank(donorIdentities, "donorIdentities");
      shapes = immutableStrings(shapes);
      kernels = immutableStrings(kernels);
      repositories = immutableNonBlank(repositories, "repositories");
      mechanics = immutableStrings(mechanics);
      if (action == Action.REUSE_EXISTING_KERNEL && kernels.isEmpty()) {
        throw new IllegalArgumentException("reuse action requires an existing kernel");
      }
    }
  }

  public record Summary(
      int donorRows,
      int matchedApis,
      int reuseExistingKernel,
      int reviewDonorMechanic) {

    public Summary {
      if (donorRows < 0 || matchedApis < 0 || reuseExistingKernel < 0 || reviewDonorMechanic < 0
          || reuseExistingKernel + reviewDonorMechanic != matchedApis) {
        throw new IllegalArgumentException("invalid competitive plan summary");
      }
    }
  }

  public RepositoryCompetitiveAlgorithmPlan {
    donors = List.copyOf(Objects.requireNonNull(donors, "donors"));
    candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
    summary = Objects.requireNonNull(summary, "summary");
  }

  public String rootSha256() {
    return RepositoryCompetitiveAlgorithmPlanWriter.rootSha256(this);
  }

  private static List<String> immutableNonBlank(List<String> values, String field) {
    List<String> checked = immutableStrings(values);
    if (checked.isEmpty() || checked.stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }

  private static List<String> immutableStrings(List<String> values) {
    return List.copyOf(Objects.requireNonNullElse(values, List.of()));
  }

  private static String gitObject(String value, String field) {
    String normalized = required(value, field).toLowerCase(java.util.Locale.ROOT);
    if (!normalized.matches("[0-9a-f]{40,64}")) throw new IllegalArgumentException(field);
    return normalized;
  }

  private static String required(String value, String field) {
    String normalized = value(value).strip();
    if (normalized.isEmpty()) throw new IllegalArgumentException(field);
    return normalized;
  }

  private static String value(String value) {
    return Objects.toString(value, "");
  }
}
