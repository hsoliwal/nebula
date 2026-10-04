// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.job.IProgressMonitor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/** CLI facade for the whole-repository additive-superset evidence pipeline. */
public final class M3RepositorySupersetExecutionCli {
  private static final Logger LOG =
      Logger.getLogger(M3RepositorySupersetExecutionCli.class.getName());

  private M3RepositorySupersetExecutionCli() {
    throw new AssertionError("No instances");
  }

  /** Immutable execution switches; accepted recipe packages are canonicalized once. */
  public record Options(
      boolean requireRecipeCoverage,
      boolean requireGitTreeCoverage,
      boolean requireHistoryCoverage,
      List<String> acceptedRecipePackages) {

    public Options {
      List<String> packages = Objects.requireNonNullElse(acceptedRecipePackages, List.of());
      acceptedRecipePackages = packages.stream().map(M3RepositorySupersetExecutionCli::required)
          .distinct().sorted().toList();
      if (acceptedRecipePackages.isEmpty()) {
        acceptedRecipePackages = List.of("com.synexia.rewrite");
      }
    }

    /** Backward-compatible constructor for pre-history callers. */
    public Options(
        boolean requireRecipeCoverage,
        boolean requireGitTreeCoverage,
        List<String> acceptedRecipePackages) {
      this(requireRecipeCoverage, requireGitTreeCoverage, false, acceptedRecipePackages);
    }

    public static Options defaults() {
      return new Options(false, false, false, List.of("com.synexia.rewrite"));
    }

    /** Strict evidence mode for every LLM-assisted source-changing repository task. */
    public static Options strictLlmTask() {
      return new Options(true, true, true, List.of("com.synexia.rewrite"));
    }
  }

  public static void main(String[] args) throws Exception {
    Arguments parsed = new Parser().parse(Objects.requireNonNull(args, "args"));
    if (parsed.help()) {
      return;
    }
    M3RepositorySupersetExecutionBundle bundle =
        M3RepositorySupersetExecutionEngine.execute(
            parsed.root(), parsed.output(), parsed.options());
    LOG.info(() -> "M3_REPOSITORY_SUPERSET_EXECUTION_ROOT=" + bundle.root());
  }

  public static M3RepositorySupersetExecutionBundle execute(
      Path repositoryRoot, Path outputDirectory, Options options) throws Exception {
    return execute(repositoryRoot, outputDirectory, options, null);
  }

  /**
   * Executes the complete repository evidence pipeline with caller-owned cancellation/progress.
   *
   * <p>A {@code null} monitor preserves the historical no-op behavior.</p>
   */
  public static M3RepositorySupersetExecutionBundle execute(
      Path repositoryRoot,
      Path outputDirectory,
      Options options,
      IProgressMonitor monitor)
      throws Exception {
    return M3RepositorySupersetExecutionEngine.execute(
        repositoryRoot,
        outputDirectory,
        Objects.requireNonNullElse(options, Options.defaults()),
        monitor);
  }

  private static String required(String value) {
    String checked = Objects.toString(value, "").strip();
    if (checked.isEmpty() || checked.chars().anyMatch(M3RepositorySupersetExecutionCli::control)) {
      throw new IllegalArgumentException("acceptedRecipePackage");
    }
    return checked;
  }

  private static boolean control(int value) {
    return value == 0 || value == '\n' || value == '\r' || value == '\t';
  }

  private record Arguments(Path root, Path output, Options options, boolean help) {}

  private static final class Parser {
    private Path root = Path.of(".");
    private Path output = Path.of("target", "m3-repository-superset-execution");
    private boolean requireRecipeCoverage;
    private boolean requireGitTreeCoverage;
    private boolean requireHistoryCoverage;
    private final List<String> packages = new ArrayList<>();

    Arguments parse(String[] args) {
      return consume(args, 0);
    }

    private Arguments consume(String[] args, int index) {
      if (index >= args.length) {
        return arguments(false);
      }
      return option(args, index, args[index]);
    }

    private Arguments option(String[] args, int index, String option) {
      return switch (option) {
        case "--root" -> root(args, index);
        case "--out" -> output(args, index);
        case "--require-recipe-coverage" -> recipeCoverage(args, index);
        case "--git-tree-coverage" -> gitCoverage(args, index);
        case "--history-coverage" -> historyCoverage(args, index);
        case "--strict-llm-task" -> strictLlmTask(args, index);
        case "--accepted-recipe-package" -> recipePackage(args, index);
        case "--help", "-h" -> help();
        default -> throw new IllegalArgumentException("unknown argument: " + option);
      };
    }

    private Arguments root(String[] args, int index) {
      root = Path.of(value(args, index + 1, "--root"));
      return consume(args, index + 2);
    }

    private Arguments output(String[] args, int index) {
      output = Path.of(value(args, index + 1, "--out"));
      return consume(args, index + 2);
    }

    private Arguments recipeCoverage(String[] args, int index) {
      requireRecipeCoverage = true;
      return consume(args, index + 1);
    }

    private Arguments gitCoverage(String[] args, int index) {
      requireGitTreeCoverage = true;
      return consume(args, index + 1);
    }

    private Arguments historyCoverage(String[] args, int index) {
      requireGitTreeCoverage = true;
      requireHistoryCoverage = true;
      return consume(args, index + 1);
    }

    private Arguments strictLlmTask(String[] args, int index) {
      requireRecipeCoverage = true;
      requireGitTreeCoverage = true;
      requireHistoryCoverage = true;
      return consume(args, index + 1);
    }

    private Arguments recipePackage(String[] args, int index) {
      packages.add(value(args, index + 1, "--accepted-recipe-package"));
      return consume(args, index + 2);
    }

    private Arguments help() {
      LOG.info(M3RepositorySupersetExecutionCli::usage);
      return arguments(true);
    }

    private Arguments arguments(boolean help) {
      return new Arguments(
          root.toAbsolutePath().normalize(),
          output.toAbsolutePath().normalize(),
          new Options(
              requireRecipeCoverage,
              requireGitTreeCoverage,
              requireHistoryCoverage,
              packages),
          help);
    }

    private static String value(String[] args, int index, String option) {
      if (index >= args.length) {
        throw new IllegalArgumentException("missing value for " + option);
      }
      return args[index];
    }
  }

  private static String usage() {
    return "Usage: M3RepositorySupersetExecutionCli"
        + " [--root DIR] [--out DIR] [--git-tree-coverage] [--history-coverage]"
        + " [--require-recipe-coverage] [--strict-llm-task]"
        + " [--accepted-recipe-package PACKAGE]...";
  }
}
