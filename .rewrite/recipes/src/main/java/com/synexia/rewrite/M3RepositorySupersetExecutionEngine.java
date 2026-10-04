// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.synexia.build.inventory.RepoCompetitiveDonorEvidence;
import com.synexia.build.inventory.RepoCompetitiveDonorEvidenceWriter;
import com.synexia.build.inventory.RepoInventoryWriter;
import com.synexia.build.inventory.RepoSupersetInventory;
import com.synexia.build.inventory.RepoSupersetScanner;
import com.synexia.build.inventory.RepositoryApiExecutionIndex;
import com.synexia.build.inventory.RepositoryApiExecutionIndexWriter;
import com.synexia.build.inventory.RepositoryCompetitiveAlgorithmPlan;
import com.synexia.build.inventory.RepositoryCompetitiveAlgorithmPlanWriter;
import com.synexia.build.inventory.RepositoryGitTreeCoverage;
import com.synexia.build.inventory.RepositoryImplementationQueue;
import com.synexia.build.inventory.RepositoryImplementationQueueWriter;
import com.synexia.build.inventory.RepositoryInventoryConvergence;
import com.synexia.build.inventory.RepositoryInventoryConvergenceWriter;
import com.synexia.build.inventory.RepositoryMechanicalPassIndex;
import com.synexia.build.inventory.RepositoryMechanicalPassWriter;
import com.synexia.build.inventory.RepositorySupersetInventory;
import com.synexia.build.inventory.RepositorySupersetInventoryWriter;
import com.synexia.build.inventory.RepositorySupersetPlan;
import com.synexia.build.inventory.RepositorySupersetPlanWriter;
import com.synexia.build.inventory.RepositorySupersetPlanner;
import com.synexia.build.inventory.RepositorySupersetScanner;
import com.synexia.job.IProgressMonitor;
import com.synexia.m3.inventory.DevelopHistoryAuditor;
import com.synexia.m3.inventory.DevelopHistoryWriter;

/** Executes the immutable whole-repository evidence pipeline without applying source changes. */
final class M3RepositorySupersetExecutionEngine {
  private M3RepositorySupersetExecutionEngine() {
    throw new AssertionError("No instances");
  }

  static M3RepositorySupersetExecutionBundle execute(
      Path repositoryRoot,
      Path outputDirectory,
      M3RepositorySupersetExecutionCli.Options options)
      throws Exception {
    return execute(repositoryRoot, outputDirectory, options, null);
  }

  static M3RepositorySupersetExecutionBundle execute(
      Path repositoryRoot,
      Path outputDirectory,
      M3RepositorySupersetExecutionCli.Options options,
      IProgressMonitor monitor)
      throws Exception {
    Context context = context(repositoryRoot, outputDirectory, options, monitor);
    phase(context, "repository inventory");
    Scan scan = scan(context);
    phase(context, "Git tree coverage");
    GitState git = gitState(context, scan.broad());
    phase(context, "develop history audit");
    HistoryState history = historyState(context, git);
    phase(context, "superset planning");
    Plan plan = plan(context, scan);
    phase(context, "evidence publication");
    writeEvidence(context, scan, plan, git, history);
    phase(context, "bundle sealing");
    return finish(context, scan, plan, git, history);
  }

  private static Context context(
      Path repositoryRoot,
      Path outputDirectory,
      M3RepositorySupersetExecutionCli.Options options,
      IProgressMonitor monitor) {
    Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot").toAbsolutePath().normalize();
    Path output = Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
    M3RepositorySupersetExecutionOutputPolicy.requireSafe(root, output);
    IProgressMonitor progress = Objects.requireNonNullElse(monitor, IProgressMonitor.noop());
    progress.checkCanceled();
    return new Context(root, output, Objects.requireNonNull(options, "options"), progress);
  }

  private static void phase(Context context, String name) {
    context.monitor().checkCanceled();
    context.monitor().subTask(name);
  }

  private static Scan scan(Context context) throws Exception {
    phase(context, "broad repository inventory");
    RepoSupersetInventory broad =
        RepoSupersetScanner.scan(context.root(), RepoSupersetScanner.Config.DEFAULT, context.monitor());
    phase(context, "structural repository inventory");
    RepositorySupersetInventory structural =
        new RepositorySupersetScanner().scan(
            context.root(), RepositorySupersetScanner.Policy.defaults(), context.monitor());
    phase(context, "inventory convergence");
    RepositoryInventoryConvergence.Report convergence =
        RepositoryInventoryConvergence.compare(broad, structural, context.monitor());
    convergence.requireSharedPathsAgree();
    RepositoryMechanicalPassIndex passes = RepositoryMechanicalPassIndex.build(broad);
    RepositorySupersetPlanner planner = new RepositorySupersetPlanner();
    return new Scan(broad, structural, convergence, passes, planner, planner.plan(structural));
  }

  private static GitState gitState(Context context, RepoSupersetInventory broad)
      throws IOException {
    if (!context.options().requireGitTreeCoverage()) {
      return new GitState("", false);
    }
    String head = RepositoryGitTreeCoverage.head(context.root(), context.monitor());
    RepositoryGitTreeCoverage.Coverage coverage =
        RepositoryGitTreeCoverage.inspect(
            context.root(), head, inventoriedPaths(broad), omissions(broad), context.monitor());
    RepositoryGitTreeCoverage.write(coverage, context.output().resolve("git-tree-coverage"));
    requireComplete(coverage);
    return new GitState(head, true);
  }

  private static HistoryState historyState(Context context, GitState git)
      throws IOException {
    if (!context.options().requireHistoryCoverage()) {
      return new HistoryState(null, false);
    }
    if (!git.complete()) {
      throw new IOException("develop history coverage requires a pinned Git-tree commit");
    }
    DevelopHistoryAuditor.Report report =
        new DevelopHistoryAuditor()
            .audit(
                context.root(),
                git.commit(),
                5000,
                List.of(),
                context.monitor());
    return new HistoryState(report, true);
  }

  private static Plan plan(Context context, Scan scan) throws IOException {
    phase(context, "competitive donor evidence");
    List<RepoCompetitiveDonorEvidence.Evidence> donors =
        RepoCompetitiveDonorEvidence.scan(context.root(), scan.broad());
    phase(context, "challenge coverage");
    M3RepositoryChallengeCoverageEvidence challenge =
        M3RepositoryChallengeCoverageEvidence.capture(context.monitor());
    phase(context, "competitive algorithm plan");
    RepositoryCompetitiveAlgorithmPlan competitive =
        scan.planner().planCompetitiveAlgorithms(context.root(), scan.structural());
    context.monitor().checkCanceled();
    RepositoryImplementationQueue queue =
        RepositoryImplementationQueue.build(scan.broad(), competitive);
    context.monitor().checkCanceled();
    RepositoryApiExecutionIndex api = RepositoryApiExecutionIndex.build(scan.broad(), competitive);
    context.monitor().checkCanceled();
    M3ImplementationQueueBinding binding = M3ImplementationQueueBinding.build(scan.broad(), queue);
    context.monitor().checkCanceled();
    List<M3RepositoryRecipeWorkOrder.WorkOrder> work = M3RepositoryRecipeWorkOrder.compile(binding);
    M3RepositoryRecipeCoverage coverage = coverage(context, work);
    return new Plan(donors, challenge, competitive, queue, api, binding, work, coverage);
  }

  private static M3RepositoryRecipeCoverage coverage(
      Context context, List<M3RepositoryRecipeWorkOrder.WorkOrder> work) {
    String[] packages = context.options().acceptedRecipePackages().toArray(String[]::new);
    return M3RepositoryRecipeCoverage.capture(work, packages);
  }

  private static void writeEvidence(
      Context context, Scan scan, Plan plan, GitState git, HistoryState history)
      throws IOException {
    writeInventoryEvidence(context.output(), scan);
    writePlanningEvidence(context.output(), plan);
    if (git.complete()) {
      writeGitReceipt(context.output(), git);
    }
    if (history.complete()) {
      new DevelopHistoryWriter()
          .write(context.output().resolve("develop-history"), history.report());
    }
  }

  private static void writeInventoryEvidence(Path output, Scan scan) throws IOException {
    RepoInventoryWriter.write(scan.broad(), output.resolve("broad-inventory"));
    RepositorySupersetInventoryWriter.write(scan.structural(), output.resolve("structural-inventory"));
    RepositoryInventoryConvergenceWriter.write(scan.convergence(), output.resolve("inventory-convergence"));
    RepositoryMechanicalPassWriter.write(scan.broad(), scan.passes(), output.resolve("mechanical-passes"));
    RepositorySupersetPlanWriter.write(scan.structuralPlan(), output.resolve("structural-plan"));
  }

  private static void writePlanningEvidence(Path output, Plan plan) throws IOException {
    RepoCompetitiveDonorEvidenceWriter.write(plan.donors(), output.resolve("competitive-donor-evidence"));
    M3RepositoryChallengeCoverageEvidenceWriter.write(
        plan.challenge(), output.resolve("challenge-superset-coverage"));
    RepositoryCompetitiveAlgorithmPlanWriter.write(plan.competitive(), output.resolve("competitive-plan"));
    RepositoryImplementationQueueWriter.write(plan.queue(), output.resolve("implementation-queue"));
    RepositoryApiExecutionIndexWriter.write(plan.api(), output.resolve("api-execution-index"));
    M3ImplementationQueueBindingWriter.write(plan.binding(), output.resolve("recipe-binding"));
    M3RepositoryRecipeCoverageWriter.write(plan.coverage(), output.resolve("recipe-coverage"));
  }

  private static M3RepositorySupersetExecutionBundle finish(
      Context context, Scan scan, Plan plan, GitState git, HistoryState history)
      throws IOException {
    M3RepositoryRecipeWorkOrderWriter.Summary work =
        M3RepositoryRecipeWorkOrderWriter.summary(plan.binding(), plan.work());
    M3RepositorySupersetExecutionBundle bundle =
        M3RepositorySupersetExecutionBundle.from(
            roots(scan, plan, work, history),
            counts(scan, plan, history),
            flags(context, plan, git, history),
            git.commit());
    M3RepositorySupersetExecutionBundleWriter.write(bundle, context.output());
    enforce(context.options(), plan.coverage(), bundle);
    return bundle;
  }

  private static String[] roots(
      Scan scan,
      Plan plan,
      M3RepositoryRecipeWorkOrderWriter.Summary work,
      HistoryState history) {
    return new String[] {
      scan.broad().fingerprint(), scan.structural().rootSha256(), scan.convergence().rootSha256(),
      M3RepositorySupersetExecutionHashes.mechanicalPassRoot(scan.broad().fingerprint(), scan.passes()),
      scan.structuralPlan().rootSha256(), RepoCompetitiveDonorEvidence.rootSha256(plan.donors()),
      plan.challenge().root(), plan.competitive().rootSha256(), plan.queue().rootSha256(),
      plan.api().rootSha256(),
      plan.binding().root(), work.root(), plan.coverage().root(),
      plan.coverage().summary().recipeInventoryRoot(),
      history.complete() ? history.report().root() : "0".repeat(64)
    };
  }

  private static int[] counts(Scan scan, Plan plan, HistoryState history) {
    return new int[] {
      scan.broad().files().size(), scan.structural().files().size(), scan.passes().findings().size(),
      scan.structuralPlan().summary().tasks(), plan.api().summary().rows(), plan.queue().summary().items(),
      plan.challenge().logicalProblems(), plan.challenge().sourceImplementations(),
      plan.challenge().pending(), plan.coverage().summary().capabilities(),
      plan.coverage().summary().sourceRows(),
      history.complete() ? history.report().candidates().size() : 0
    };
  }

  private static boolean[] flags(
      Context context, Plan plan, GitState git, HistoryState history) {
    return new boolean[] {
      plan.coverage().summary().executionReady(),
      context.options().requireGitTreeCoverage(),
      git.complete(),
      context.options().requireHistoryCoverage(),
      history.complete()
    };
  }

  private static void enforce(
      M3RepositorySupersetExecutionCli.Options options,
      M3RepositoryRecipeCoverage coverage,
      M3RepositorySupersetExecutionBundle bundle) {
    bundle.requirePlanningReady();
    if (options.requireRecipeCoverage()) {
      coverage.requireExecutionReady();
      bundle.requireRecipeExecutionReady();
    }
  }

  private static Set<String> inventoriedPaths(RepoSupersetInventory inventory) {
    return inventory.files().stream()
        .map(com.synexia.build.inventory.RepoFileRecord::path)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static Map<String, String> omissions(RepoSupersetInventory inventory) {
    return inventory.actions().stream()
        .filter(action -> action.category().equals("scan-omission"))
        .collect(
            Collectors.toMap(
                com.synexia.build.inventory.RepoAction::path,
                com.synexia.build.inventory.RepoAction::detail,
                (left, right) -> left,
                TreeMap::new));
  }

  private static void requireComplete(RepositoryGitTreeCoverage.Coverage coverage)
      throws IOException {
    if (!coverage.complete()) {
      throw new IOException(
          "incomplete Git-tree inventory coverage: missing="
              + coverage.missingTracked().size()
              + " extra="
              + coverage.untrackedInventoried());
    }
  }

  private static void writeGitReceipt(Path output, GitState git) throws IOException {
    if (!git.complete()) {
      return;
    }
    java.nio.file.Files.writeString(
        output.resolve("GIT_TREE_PINNED_HEAD.txt"), git.commit() + System.lineSeparator());
  }

  private record Context(
      Path root,
      Path output,
      M3RepositorySupersetExecutionCli.Options options,
      IProgressMonitor monitor) {}

  private record Scan(
      RepoSupersetInventory broad,
      RepositorySupersetInventory structural,
      RepositoryInventoryConvergence.Report convergence,
      RepositoryMechanicalPassIndex passes,
      RepositorySupersetPlanner planner,
      RepositorySupersetPlan structuralPlan) {}

  private record GitState(String commit, boolean complete) {}

  private record HistoryState(DevelopHistoryAuditor.Report report, boolean complete) {
    private HistoryState {
      if (complete != (report != null)) {
        throw new IllegalArgumentException("history state mismatch");
      }
    }
  }

  private record Plan(
      List<RepoCompetitiveDonorEvidence.Evidence> donors,
      M3RepositoryChallengeCoverageEvidence challenge,
      RepositoryCompetitiveAlgorithmPlan competitive,
      RepositoryImplementationQueue queue,
      RepositoryApiExecutionIndex api,
      M3ImplementationQueueBinding binding,
      List<M3RepositoryRecipeWorkOrder.WorkOrder> work,
      M3RepositoryRecipeCoverage coverage) {}
}
