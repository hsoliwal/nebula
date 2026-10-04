// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Deterministic writer for {@link M3RepositorySupersetExecutionBundle}. */
public final class M3RepositorySupersetExecutionBundleWriter {
  public static final String SUMMARY_FILE = "M3_REPOSITORY_SUPERSET_EXECUTION.tsv";
  public static final String ROOT_FILE = "M3_REPOSITORY_SUPERSET_EXECUTION.sha256";

  private M3RepositorySupersetExecutionBundleWriter() {
    throw new AssertionError("No instances");
  }

  public static void write(
      M3RepositorySupersetExecutionBundle bundle, Path outputDirectory) throws IOException {
    Objects.requireNonNull(bundle, "bundle");
    Path output = Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
    Files.createDirectories(output);
    Files.writeString(output.resolve(SUMMARY_FILE), render(bundle), StandardCharsets.UTF_8);
    Files.writeString(output.resolve(ROOT_FILE), "ROOT  " + bundle.root() + "\n", StandardCharsets.UTF_8);
  }

  static String render(M3RepositorySupersetExecutionBundle bundle) {
    StringBuilder out = new StringBuilder("metric\tvalue\n");
    roots(out, bundle.roots());
    counts(out, bundle.counts());
    status(out, bundle);
    metric(out, "root", bundle.root());
    return out.toString();
  }

  private static void roots(
      StringBuilder out, M3RepositorySupersetExecutionBundle.Roots roots) {
    metric(out, "broadInventoryRoot", roots.broadInventory());
    metric(out, "structuralInventoryRoot", roots.structuralInventory());
    metric(out, "inventoryConvergenceRoot", roots.inventoryConvergence());
    metric(out, "mechanicalPassRoot", roots.mechanicalPass());
    metric(out, "structuralPlanRoot", roots.structuralPlan());
    donorRoots(out, roots);
  }

  private static void donorRoots(
      StringBuilder out, M3RepositorySupersetExecutionBundle.Roots roots) {
    metric(out, "competitiveDonorEvidenceRoot", roots.competitiveDonorEvidence());
    metric(out, "challengeCoverageRoot", roots.challengeCoverage());
    metric(out, "competitivePlanRoot", roots.competitivePlan());
    metric(out, "implementationQueueRoot", roots.implementationQueue());
    metric(out, "apiExecutionIndexRoot", roots.apiExecutionIndex());
    metric(out, "queueRecipeBindingRoot", roots.queueRecipeBinding());
    recipeRoots(out, roots);
  }

  private static void recipeRoots(
      StringBuilder out, M3RepositorySupersetExecutionBundle.Roots roots) {
    metric(out, "recipeWorkOrderRoot", roots.recipeWorkOrder());
    metric(out, "recipeCoverageRoot", roots.recipeCoverage());
    metric(out, "recipeInventoryRoot", roots.recipeInventory());
    metric(out, "developHistoryRoot", roots.developHistory());
  }

  private static void counts(
      StringBuilder out, M3RepositorySupersetExecutionBundle.Counts counts) {
    metric(out, "broadFiles", counts.broadFiles());
    metric(out, "structuralFiles", counts.structuralFiles());
    metric(out, "mechanicalFindings", counts.mechanicalFindings());
    metric(out, "structuralPlanTasks", counts.structuralPlanTasks());
    metric(out, "apiRows", counts.apiRows());
    remainingCounts(out, counts);
  }

  private static void remainingCounts(
      StringBuilder out, M3RepositorySupersetExecutionBundle.Counts counts) {
    metric(out, "implementationQueueItems", counts.implementationQueueItems());
    metric(out, "challengeProblems", counts.challengeProblems());
    metric(out, "challengeSourceImplementations", counts.challengeSourceImplementations());
    metric(out, "challengePending", counts.challengePending());
    metric(out, "recipeCapabilities", counts.recipeCapabilities());
    metric(out, "recipeSourceRows", counts.recipeSourceRows());
    metric(out, "historyCandidates", counts.historyCandidates());
  }

  private static void status(
      StringBuilder out, M3RepositorySupersetExecutionBundle bundle) {
    M3RepositorySupersetExecutionBundle.Status state = bundle.status();
    metric(out, "recipeCoverageReady", state.recipeCoverageReady());
    metric(out, "gitTreeCoverageRequested", state.gitTreeCoverageRequested());
    metric(out, "gitTreeCoverageComplete", state.gitTreeCoverageComplete());
    metric(out, "historyCoverageRequested", state.historyCoverageRequested());
    metric(out, "historyCoverageComplete", state.historyCoverageComplete());
    metric(out, "gitCommit", state.gitCommit());
    metric(out, "mutationAuthority", bundle.mutationAuthority());
    metric(out, "donorSourceCopyAuthority", bundle.donorSourceCopyAuthority());
    metric(out, "promotionAuthority", bundle.promotionAuthority());
  }

  private static void metric(StringBuilder out, String name, Object value) {
    String cell = Objects.toString(value, "").replace("\t", "\\t").replace("\r", "\\r")
        .replace("\n", "\\n");
    out.append(name).append('\t').append(cell).append('\n');
  }
}
