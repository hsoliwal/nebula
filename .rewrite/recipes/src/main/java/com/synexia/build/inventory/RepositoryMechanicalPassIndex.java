// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Precomputed findings for the repository-wide mechanical superset passes.
 *
 * <p>Findings are evidence and work candidates, never automatic semantic-equivalence proofs or
 * source-mutation authority.
 */
public final class RepositoryMechanicalPassIndex {
  public enum Pass {
    API_COVERAGE,
    STRUCTURAL_SUPERSET,
    DATA_PRECOMPUTE,
    DESIGN_ABSTRACTION,
    CONSISTENCY,
    VERIFICATION
  }

  public record Finding(
      Pass pass,
      RepoAction.Severity severity,
      String capability,
      String path,
      int line,
      String evidence,
      String strategy) {
    public Finding {
      Objects.requireNonNull(pass, "pass");
      Objects.requireNonNull(severity, "severity");
      capability = required(capability, "capability");
      path = required(path, "path");
      evidence = required(evidence, "evidence");
      strategy = required(strategy, "strategy");
      if (line < 0) throw new IllegalArgumentException("line");
    }
  }

  public record Summary(
      Pass pass,
      int findings,
      int info,
      int review,
      int error) {
    public Summary {
      Objects.requireNonNull(pass, "pass");
      if (findings < 0 || info < 0 || review < 0 || error < 0 || info + review + error != findings) {
        throw new IllegalArgumentException("invalid pass summary");
      }
    }
  }

  private final List<Finding> findings;
  private final Map<Pass, List<Finding>> byPass;
  private final List<Summary> summaries;

  private RepositoryMechanicalPassIndex(List<Finding> findings) {
    ArrayList<Finding> ordered = new ArrayList<>(findings);
    ordered.sort(
        Comparator.comparing(Finding::pass)
            .thenComparing(Finding::severity)
            .thenComparing(Finding::path)
            .thenComparingInt(Finding::line)
            .thenComparing(Finding::capability)
            .thenComparing(Finding::strategy));
    this.findings = List.copyOf(ordered);

    EnumMap<Pass, List<Finding>> indexed = new EnumMap<>(Pass.class);
    for (Pass pass : Pass.values()) {
      indexed.put(pass, this.findings.stream().filter(finding -> finding.pass() == pass).toList());
    }
    this.byPass = java.util.Collections.unmodifiableMap(indexed);
    this.summaries =
        java.util.Arrays.stream(Pass.values())
            .map(pass -> summarize(pass, indexed.get(pass)))
            .toList();
  }

  public static RepositoryMechanicalPassIndex build(RepoSupersetInventory inventory) {
    Objects.requireNonNull(inventory, "inventory");
    ArrayList<Finding> result = new ArrayList<>();

    for (RepoSupersetInventory.ApiCoverage coverage : inventory.apiCoverage()) {
      RepoApiRecord api = coverage.api();
      RepoAction.Severity severity =
          coverage.indexed() || coverage.precomputed()
              ? RepoAction.Severity.INFO
              : RepoAction.Severity.REVIEW;
      result.add(
          new Finding(
              Pass.API_COVERAGE,
              severity,
              api.owner() + "#" + api.name(),
              api.path(),
              api.line(),
              coverage.representation(),
              coverage.strategy()));

      if (!coverage.indexed() && !coverage.precomputed()) {
        result.add(
            new Finding(
                Pass.DATA_PRECOMPUTE,
                RepoAction.Severity.REVIEW,
                api.owner() + "#" + api.name(),
                api.path(),
                api.line(),
                "repeated API path currently represented as " + coverage.representation(),
                "review-index-or-precompute"));
      }
    }

    for (RepoSupersetInventory.DuplicateCluster cluster : inventory.exactDuplicates()) {
      for (String path : cluster.paths()) {
        result.add(
            new Finding(
                Pass.STRUCTURAL_SUPERSET,
                RepoAction.Severity.REVIEW,
                "exact-duplicate",
                path,
                0,
                cluster.sha256(),
                "inventory-before-consolidation"));
      }
    }

    for (RepoSupersetInventory.CandidateCluster cluster : inventory.candidateClusters()) {
      for (String path : cluster.paths()) {
        result.add(
            new Finding(
                Pass.STRUCTURAL_SUPERSET,
                RepoAction.Severity.INFO,
                cluster.lane(),
                path,
                0,
                cluster.value(),
                "candidate-only-verify-semantics"));
      }
    }

    for (RepoFileRecord file : inventory.files()) {
      if (file.signals().contains(RepoSignal.JINI)
          || file.signals().contains(RepoSignal.JNI)
          || file.signals().contains(RepoSignal.SERIALIZATION)) {
        result.add(
            new Finding(
                Pass.DESIGN_ABSTRACTION,
                RepoAction.Severity.INFO,
                abstractionCapability(file),
                file.path(),
                0,
                signalEvidence(file),
                "preserve-explicit-boundary"));
      }
    }

    for (RepoAction action : inventory.actions()) {
      result.add(
          new Finding(
              consistencyPass(action),
              action.severity(),
              action.category(),
              action.path(),
              action.line(),
              action.detail(),
              consistencyStrategy(action)));
    }

    for (RepoSupersetInventory.ModuleSummary module : inventory.modules()) {
      if (module.stubSignals() > 0) {
        result.add(
            new Finding(
                Pass.CONSISTENCY,
                RepoAction.Severity.REVIEW,
                "module-stub-signals",
                module.module(),
                0,
                "stubSignals=" + module.stubSignals(),
                "resolve-without-api-regression"));
      }
      result.add(
          new Finding(
              Pass.VERIFICATION,
              RepoAction.Severity.INFO,
              "module-verification",
              module.module(),
              0,
              "files=" + module.files() + ";java=" + module.javaFiles() + ";tests=" + module.tests(),
              "compile-lint-test-module"));
    }

    return new RepositoryMechanicalPassIndex(result);
  }

  public List<Finding> findings() {
    return findings;
  }

  public List<Finding> findings(Pass pass) {
    return byPass.getOrDefault(Objects.requireNonNull(pass, "pass"), List.of());
  }

  public List<Summary> summaries() {
    return summaries;
  }

  private static Summary summarize(Pass pass, List<Finding> findings) {
    int info = 0;
    int review = 0;
    int error = 0;
    for (Finding finding : findings) {
      switch (finding.severity()) {
        case INFO -> info++;
        case REVIEW -> review++;
        case ERROR -> error++;
      }
    }
    return new Summary(pass, findings.size(), info, review, error);
  }

  private static String abstractionCapability(RepoFileRecord file) {
    ArrayList<String> values = new ArrayList<>();
    if (file.signals().contains(RepoSignal.JINI)) values.add("jini");
    if (file.signals().contains(RepoSignal.JNI)) values.add("jni");
    if (file.signals().contains(RepoSignal.SERIALIZATION)) values.add("serialization");
    return String.join("+", values);
  }

  private static String signalEvidence(RepoFileRecord file) {
    return file.signals().stream()
        .filter(
            signal ->
                signal == RepoSignal.JINI
                    || signal == RepoSignal.JNI
                    || signal == RepoSignal.SERIALIZATION)
        .map(Enum::name)
        .sorted()
        .collect(java.util.stream.Collectors.joining(","));
  }

  private static Pass consistencyPass(RepoAction action) {
    String category = action.category();
    if (category.contains("scan")
        || category.contains("build")
        || category.contains("coverage")
        || category.contains("verify")) {
      return Pass.VERIFICATION;
    }
    return Pass.CONSISTENCY;
  }

  private static String consistencyStrategy(RepoAction action) {
    if (action.category().contains("duplicate")) return "consolidate-after-contract-proof";
    if (action.category().contains("collision")) return "introduce-explicit-owner-or-bridge";
    if (action.severity() == RepoAction.Severity.ERROR) return "block-until-resolved";
    return "mechanical-review";
  }

  private static String required(String value, String label) {
    String checked = Objects.requireNonNull(value, label).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(label);
    return checked;
  }
}
