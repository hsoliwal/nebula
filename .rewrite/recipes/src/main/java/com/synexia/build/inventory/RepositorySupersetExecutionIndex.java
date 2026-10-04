// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Eight-phase additive execution projection over the canonical repository inventory.
 *
 * <p>The existing {@link RepositoryMechanicalPassIndex} remains unchanged for compatibility.
 * This class reuses that evidence, routes duplicate/collision work into an explicit consolidation
 * phase, and adds final-polish evidence without changing the older public pass enum.</p>
 */
public final class RepositorySupersetExecutionIndex {
  public record Finding(
      RepositorySupersetExecutionPhase phase,
      RepoAction.Severity severity,
      String capability,
      String path,
      int line,
      String evidence,
      String strategy) {
    public Finding {
      phase = Objects.requireNonNull(phase, "phase");
      severity = Objects.requireNonNull(severity, "severity");
      capability = required(capability, "capability");
      path = required(path, "path");
      if (line < 0) throw new IllegalArgumentException("line");
      evidence = required(evidence, "evidence");
      strategy = required(strategy, "strategy");
    }
  }

  public record Summary(
      RepositorySupersetExecutionPhase phase,
      int findings,
      int info,
      int review,
      int error) {
    public Summary {
      phase = Objects.requireNonNull(phase, "phase");
      if (findings < 0 || info < 0 || review < 0 || error < 0
          || info + review + error != findings) {
        throw new IllegalArgumentException("invalid execution phase summary");
      }
    }
  }

  private final List<Finding> findings;
  private final Map<RepositorySupersetExecutionPhase, List<Finding>> byPhase;
  private final List<Summary> summaries;

  private RepositorySupersetExecutionIndex(List<Finding> source) {
    ArrayList<Finding> ordered = new ArrayList<>(source);
    ordered.sort(
        Comparator.comparing(Finding::phase)
            .thenComparing(Finding::severity)
            .thenComparing(Finding::path)
            .thenComparingInt(Finding::line)
            .thenComparing(Finding::capability)
            .thenComparing(Finding::strategy)
            .thenComparing(Finding::evidence));
    this.findings = List.copyOf(ordered);

    EnumMap<RepositorySupersetExecutionPhase, List<Finding>> indexed =
        new EnumMap<>(RepositorySupersetExecutionPhase.class);
    for (RepositorySupersetExecutionPhase phase : RepositorySupersetExecutionPhase.values()) {
      indexed.put(phase, this.findings.stream().filter(f -> f.phase() == phase).toList());
    }
    this.byPhase = java.util.Collections.unmodifiableMap(indexed);
    this.summaries =
        java.util.Arrays.stream(RepositorySupersetExecutionPhase.values())
            .map(phase -> summarize(phase, indexed.get(phase)))
            .toList();
  }

  public static RepositorySupersetExecutionIndex build(RepoSupersetInventory inventory) {
    Objects.requireNonNull(inventory, "inventory");
    RepositoryMechanicalPassIndex legacy = RepositoryMechanicalPassIndex.build(inventory);
    ArrayList<Finding> result = new ArrayList<>();
    Set<Finding> identities = new HashSet<>();

    for (RepositoryMechanicalPassIndex.Finding finding : legacy.findings()) {
      RepositorySupersetExecutionPhase phase = phase(finding);
      add(
          result,
          identities,
          new Finding(
              phase,
              finding.severity(),
              finding.capability(),
              finding.path(),
              finding.line(),
              finding.evidence(),
              executionStrategy(phase, finding.capability(), finding.strategy())));
    }

    for (RepoSupersetInventory.DuplicateCluster cluster : inventory.exactDuplicates()) {
      for (String path : cluster.paths()) {
        add(
            result,
            identities,
            new Finding(
                RepositorySupersetExecutionPhase.CONSOLIDATION,
                RepoAction.Severity.REVIEW,
                "exact-duplicate",
                path,
                0,
                cluster.sha256(),
                "consolidate-only-after-contract-proof"));
      }
    }

    for (RepoOwnerCollisionIndex.Row collision : inventory.ownerCollisions()) {
      RepoAction.Severity severity =
          collision.explicitBridgeRequired()
              ? RepoAction.Severity.ERROR
              : RepoAction.Severity.REVIEW;
      String strategy =
          collision.explicitBridgeRequired()
              ? "introduce-explicit-id-space-bridge"
              : collision.exactDuplicate()
                  ? "consolidate-only-after-contract-proof"
                  : "preserve-distinct-until-semantic-proof";
      for (String path : collision.paths()) {
        add(
            result,
            identities,
            new Finding(
                RepositorySupersetExecutionPhase.CONSOLIDATION,
                severity,
                "owner-collision:" + collision.typeName(),
                path,
                0,
                collision.classification().name() + " -> " + String.join(",", collision.paths()),
                strategy));
      }
    }

    for (RepoApiCollisionIndex.Row collision : inventory.apiCollisions()) {
      String strategy =
          collision.divergent()
              ? "preserve-contracts-add-adapter-before-consolidation"
              : "consolidate-only-after-contract-proof";
      for (String path : collision.paths()) {
        add(
            result,
            identities,
            new Finding(
                RepositorySupersetExecutionPhase.CONSOLIDATION,
                RepoAction.Severity.REVIEW,
                "api-collision:" + collision.owner() + "#" + collision.name(),
                path,
                0,
                collision.classification().name()
                    + " signatures="
                    + String.join(",", collision.signatures()),
                strategy));
      }
    }

    for (RepoSupersetInventory.ModuleSummary module : inventory.modules()) {
      add(
          result,
          identities,
          new Finding(
              RepositorySupersetExecutionPhase.POLISH,
              RepoAction.Severity.INFO,
              "module-final-polish",
              module.module(),
              0,
              moduleEvidence(module),
              "review-api-ergonomics-naming-docs-errors-performance-allocation-indexing-"
                  + "precompute-concurrency-lifecycle-tests-compatibility-maintainability"));
    }

    return new RepositorySupersetExecutionIndex(result);
  }

  public List<Finding> findings() {
    return findings;
  }

  public List<Finding> findings(RepositorySupersetExecutionPhase phase) {
    return byPhase.getOrDefault(Objects.requireNonNull(phase, "phase"), List.of());
  }

  public List<Summary> summaries() {
    return summaries;
  }

  private static RepositorySupersetExecutionPhase phase(
      RepositoryMechanicalPassIndex.Finding finding) {
    if (finding.pass() == RepositoryMechanicalPassIndex.Pass.CONSISTENCY
        && isConsolidationCapability(finding.capability())) {
      return RepositorySupersetExecutionPhase.CONSOLIDATION;
    }
    return switch (finding.pass()) {
      case API_COVERAGE -> RepositorySupersetExecutionPhase.API_COVERAGE;
      case STRUCTURAL_SUPERSET -> RepositorySupersetExecutionPhase.STRUCTURAL_SUPERSET;
      case DATA_PRECOMPUTE -> RepositorySupersetExecutionPhase.DATA_PRECOMPUTE;
      case DESIGN_ABSTRACTION -> RepositorySupersetExecutionPhase.DESIGN_ABSTRACTION;
      case CONSISTENCY -> RepositorySupersetExecutionPhase.CONSISTENCY;
      case VERIFICATION -> RepositorySupersetExecutionPhase.VERIFICATION;
    };
  }

  private static boolean isConsolidationCapability(String capability) {
    String value = capability.toLowerCase(java.util.Locale.ROOT);
    return value.contains("duplicate") || value.contains("collision");
  }

  private static String executionStrategy(
      RepositorySupersetExecutionPhase phase, String capability, String legacyStrategy) {
    if (phase != RepositorySupersetExecutionPhase.CONSOLIDATION) return legacyStrategy;
    String value = capability.toLowerCase(java.util.Locale.ROOT);
    if (value.contains("identity-owner-collision")) return "introduce-explicit-id-space-bridge";
    if (value.contains("collision")) return "preserve-distinct-or-adapt-until-semantic-proof";
    return "consolidate-only-after-contract-proof";
  }

  private static String moduleEvidence(RepoSupersetInventory.ModuleSummary module) {
    return "files="
        + module.files()
        + ";java="
        + module.javaFiles()
        + ";tests="
        + module.tests()
        + ";apis="
        + module.apis()
        + ";mindexOwners="
        + module.mindexOwners()
        + ";jini="
        + module.jiniFiles()
        + ";precompute="
        + module.precomputeFiles()
        + ";index="
        + module.indexFiles()
        + ";cache="
        + module.cacheFiles()
        + ";stubs="
        + module.stubSignals();
  }

  private static void add(List<Finding> target, Set<Finding> identities, Finding finding) {
    if (identities.add(finding)) target.add(finding);
  }

  private static Summary summarize(
      RepositorySupersetExecutionPhase phase, List<Finding> findings) {
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
    return new Summary(phase, findings.size(), info, review, error);
  }

  private static String required(String value, String field) {
    String checked = Objects.requireNonNull(value, field).strip();
    if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }
}
