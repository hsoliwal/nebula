// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/** Conservative exact-name join between repository APIs and pinned competitive donor evidence. */
final class RepositoryCompetitiveDonorPlanner {
  private RepositoryCompetitiveDonorPlanner() {}

  static RepositoryCompetitiveAlgorithmPlan plan(
      Path repositoryRoot, RepositorySupersetInventory inventory) throws IOException {
    Objects.requireNonNull(inventory, "inventory");
    Map<String, RepositorySupersetInventory.FileRecord> fileByPath = new TreeMap<>();
    inventory.files().forEach(file -> fileByPath.put(file.path(), file));
    List<CandidateApi> apis = inventory.apiFacts().stream()
        .filter(api -> api.kind().equals("METHOD") || api.kind().equals("INTERFACE_METHOD"))
        .filter(api -> fileByPath.containsKey(api.path()) && !fileByPath.get(api.path()).testSource())
        .map(api -> new CandidateApi(api.path(), api.name(), api.signature()))
        .toList();
    return plan(repositoryRoot, RepositoryModulePathIndex.fromModules(inventory.modules()), apis);
  }

  /** Reuses the admitted broad snapshot; only the pinned donor ledgers are read from disk. */
  static RepositoryCompetitiveAlgorithmPlan planFromEvidence(
      Path repositoryRoot, RepoSupersetInventory inventory) throws IOException {
    Objects.requireNonNull(inventory, "inventory");
    List<CandidateApi> apis = inventory.files().stream()
        .filter(file -> !file.signals().contains(RepoSignal.TEST_SOURCE))
        .flatMap(file -> file.apis().stream())
        .filter(api -> api.kind().equals("method"))
        .map(api -> new CandidateApi(api.path(), api.name(), api.signature()))
        .toList();
    return plan(repositoryRoot, RepositoryModulePathIndex.fromPomFiles(inventory.files()), apis);
  }

  private record CandidateApi(String path, String name, String signature) {}

  private static RepositoryCompetitiveAlgorithmPlan plan(
      Path repositoryRoot, RepositoryModulePathIndex modules, List<CandidateApi> apis)
      throws IOException {
    List<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> donors =
        RepositoryCompetitiveDonorCatalog.load(repositoryRoot);

    Map<String, List<RepositoryCompetitiveAlgorithmPlan.DonorEvidence>> byKernel =
        new TreeMap<>();
    Map<String, List<RepositoryCompetitiveAlgorithmPlan.DonorEvidence>> byTitle =
        new TreeMap<>();
    for (RepositoryCompetitiveAlgorithmPlan.DonorEvidence donor : donors) {
      if (donor.hasExistingKernel()) {
        byKernel.computeIfAbsent(normalize(kernelLeaf(donor.kernel())), ignored -> new ArrayList<>())
            .add(donor);
      }
      byTitle.computeIfAbsent(normalize(donor.title()), ignored -> new ArrayList<>()).add(donor);
    }

    ArrayList<RepositoryCompetitiveAlgorithmPlan.ApiCandidate> candidates = new ArrayList<>();
    for (CandidateApi api : apis) {

      String apiKey = normalize(api.name());
      if (apiKey.isEmpty()) continue;

      LinkedHashSet<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> matches =
          new LinkedHashSet<>();
      RepositoryCompetitiveAlgorithmPlan.MatchBasis basis = null;

      List<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> kernelMatches = byKernel.get(apiKey);
      if (kernelMatches != null) {
        matches.addAll(kernelMatches);
        basis = RepositoryCompetitiveAlgorithmPlan.MatchBasis.KERNEL_NAME;
      }
      if (basis == null) {
        List<RepositoryCompetitiveAlgorithmPlan.DonorEvidence> titleMatches = byTitle.get(apiKey);
        if (titleMatches != null) {
          matches.addAll(titleMatches);
          basis = RepositoryCompetitiveAlgorithmPlan.MatchBasis.CHALLENGE_TITLE;
        }
      }
      if (matches.isEmpty()) continue;

      TreeSet<String> identities = new TreeSet<>();
      TreeSet<String> shapes = new TreeSet<>();
      TreeSet<String> kernels = new TreeSet<>();
      TreeSet<String> repositories = new TreeSet<>();
      TreeSet<String> mechanics = new TreeSet<>();
      for (RepositoryCompetitiveAlgorithmPlan.DonorEvidence donor : matches) {
        identities.add(donor.identity());
        addNonBlank(shapes, donor.shape());
        addNonBlank(kernels, donor.kernel());
        repositories.add(donor.repository());
        addNonBlank(mechanics, donor.mechanic());
      }

      RepositoryCompetitiveAlgorithmPlan.Action action =
          kernels.isEmpty()
              ? RepositoryCompetitiveAlgorithmPlan.Action.REVIEW_DONOR_MECHANIC
              : RepositoryCompetitiveAlgorithmPlan.Action.REUSE_EXISTING_KERNEL;
      candidates.add(
          new RepositoryCompetitiveAlgorithmPlan.ApiCandidate(
              api.path(),
              modules.ownerOf(api.path()),
              api.name(),
              api.signature(),
              Objects.requireNonNull(basis, "matchBasis"),
              action,
              List.copyOf(identities),
              List.copyOf(shapes),
              List.copyOf(kernels),
              List.copyOf(repositories),
              List.copyOf(mechanics)));
    }

    candidates.sort(
        Comparator.comparing(RepositoryCompetitiveAlgorithmPlan.ApiCandidate::path)
            .thenComparing(RepositoryCompetitiveAlgorithmPlan.ApiCandidate::name)
            .thenComparing(RepositoryCompetitiveAlgorithmPlan.ApiCandidate::signature)
            .thenComparing(candidate -> candidate.matchBasis().name()));

    int reuse =
        (int)
            candidates.stream()
                .filter(
                    candidate ->
                        candidate.action()
                            == RepositoryCompetitiveAlgorithmPlan.Action.REUSE_EXISTING_KERNEL)
                .count();
    int review = candidates.size() - reuse;
    return new RepositoryCompetitiveAlgorithmPlan(
        donors,
        candidates,
        new RepositoryCompetitiveAlgorithmPlan.Summary(
            donors.size(), candidates.size(), reuse, review));
  }

  private static String kernelLeaf(String kernel) {
    String value = Objects.toString(kernel, "");
    int dot = value.lastIndexOf('.');
    return dot < 0 ? value : value.substring(dot + 1);
  }

  private static String normalize(String value) {
    String lower = Objects.toString(value, "").toLowerCase(Locale.ROOT);
    StringBuilder out = new StringBuilder(lower.length());
    for (int i = 0; i < lower.length(); i++) {
      char ch = lower.charAt(i);
      if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') out.append(ch);
    }
    return out.toString();
  }

  private static void addNonBlank(TreeSet<String> target, String value) {
    String checked = Objects.toString(value, "").strip();
    if (!checked.isEmpty()) target.add(checked);
  }
}
