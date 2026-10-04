// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.JavaSourceFile;

import static java.util.Collections.emptyList;

/**
 * Read-only repository scan that nominates framework recipe mechanics for one M3 work order.
 *
 * <p>This intentionally uses the mechanics it studies: a ScanningRecipe accumulator, a source-file
 * precondition, and a typed DataTable. It never copies donor source or mutates target files.</p>
 */
public final class M3FrameworkRecipePatternReviewRecipe
    extends ScanningRecipe<M3FrameworkRecipePatternReviewRecipe.Accumulator> {

  @Option(
      displayName = "Source file pattern",
      description = "Glob limiting the repository scan.",
      example = "**/src/main/**",
      required = false)
  private final String sourceFilePattern;

  @Option(
      displayName = "Problem term",
      description = "Bounded project/problem text used to select relevant recipe mechanics.",
      example = "spring junit migration",
      required = false)
  private final String problemTerm;

  private final transient PatternTable patternTable = new PatternTable(this);

  public M3FrameworkRecipePatternReviewRecipe() {
    this("**", "");
  }

  @JsonCreator
  public M3FrameworkRecipePatternReviewRecipe(String sourceFilePattern, String problemTerm) {
    this.sourceFilePattern =
        token(
            Objects.requireNonNullElse(sourceFilePattern, "**"),
            "sourceFilePattern",
            4_096);
    this.problemTerm = optional(problemTerm, 4_096);
  }

  @Override
  public String getDisplayName() {
    return "M3 framework recipe-pattern review";
  }

  @Override
  public String getDescription() {
    return "Scans the admitted source scope and emits pinned framework recipe-pattern evidence. "
        + "The evidence is planning-only and grants no target mutation or donor source-copy authority.";
  }

  @Override
  public Set<String> getTags() {
    return Set.of(
        "synexia",
        "m3",
        "openrewrite",
        "framework",
        "scanning-recipe",
        "preconditions",
        "datatable",
        "candidate-only");
  }

  @Override
  public int maxCycles() {
    return 1;
  }

  @Override
  public boolean causesAnotherCycle() {
    return false;
  }

  @Override
  public Accumulator getInitialValue(ExecutionContext ctx) {
    return new Accumulator();
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
    TreeVisitor<Tree, ExecutionContext> scanner =
        new TreeVisitor<>() {
          @Override
          public Tree preVisit(Tree tree, ExecutionContext ctx) {
            if (tree instanceof SourceFile source) {
              acc.sourceCount++;
              String path =
                  source.getSourcePath().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
              if (source instanceof JavaSourceFile) {
                acc.javaSourceCount++;
                acc.cues.add("java");
              }
              if (path.endsWith("/pom.xml") || path.equals("pom.xml")) acc.cues.add("maven");
              if (path.endsWith("build.gradle") || path.endsWith("build.gradle.kts")) {
                acc.cues.add("gradle");
              }
              if (path.contains("/src/test/") || path.contains("/test/")) {
                acc.cues.add("test");
                acc.cues.add("junit");
              }
              if (path.contains("spring")) acc.cues.add("spring");
              if (path.endsWith(".xml")) acc.cues.add("xml");
            }
            return tree;
          }
        };
    return Preconditions.check(new FindSourceFiles(sourceFilePattern).getVisitor(), scanner);
  }

  @Override
  public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
    for (M3FrameworkRecipePatternCatalog.Pattern pattern :
        M3FrameworkRecipePatternCatalog.select(acc.cues, problemTerm)) {
      patternTable.insertRow(
          ctx,
          new PatternRow(
              pattern.id(),
              pattern.repository(),
              pattern.revision(),
              pattern.path(),
              pattern.blobSha(),
              pattern.license().name(),
              pattern.mechanics().stream().map(Enum::name).sorted().toList(),
              pattern.evidenceMode(),
              false,
              false,
              acc.sourceCount,
              acc.javaSourceCount,
              List.copyOf(acc.cues)));
    }
    return emptyList();
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
    return Preconditions.check(
        new FindSourceFiles(sourceFilePattern).getVisitor(),
        TreeVisitor.noop());
  }

  public List<M3FrameworkRecipePatternCatalog.Pattern> plannedPatterns(Set<String> cues) {
    return M3FrameworkRecipePatternCatalog.select(cues, problemTerm);
  }

  public String getSourceFilePattern() {
    return sourceFilePattern;
  }

  public String getProblemTerm() {
    return problemTerm;
  }

  public boolean targetFileMutationAuthority() {
    return false;
  }

  public boolean sourceCopyAuthority() {
    return false;
  }

  public boolean promotionAuthority() {
    return false;
  }

  static final class Accumulator {
    private final TreeSet<String> cues = new TreeSet<>();
    private int sourceCount;
    private int javaSourceCount;
  }

  public static final class PatternTable extends DataTable<PatternRow> {
    PatternTable(org.openrewrite.Recipe recipe) {
      super(
          recipe,
          "M3 framework recipe patterns",
          "Pinned framework mechanics that may improve a recipe implementation.");
    }
  }

  public record PatternRow(
      @Column(displayName = "Pattern id", description = "Stable Synexia evidence id.")
          String patternId,
      @Column(displayName = "Repository", description = "Pinned GitHub donor repository.")
          String repository,
      @Column(displayName = "Revision", description = "Pinned Git commit.")
          String revision,
      @Column(displayName = "Path", description = "Pinned donor file path.")
          String path,
      @Column(displayName = "Blob SHA", description = "Pinned donor blob.")
          String blobSha,
      @Column(displayName = "License", description = "Observed license family.")
          String license,
      @Column(displayName = "Mechanics", description = "Recipe mechanics nominated.")
          List<String> mechanics,
      @Column(displayName = "Evidence mode", description = "Allowed donor-use mode.")
          String evidenceMode,
      @Column(
              displayName = "Source-copy authority",
              description = "Always false in this review lane.")
          boolean sourceCopyAuthority,
      @Column(
              displayName = "Mutation authority",
              description = "Always false in this review lane.")
          boolean mutationAuthority,
      @Column(displayName = "Scanned sources", description = "Scoped source-file count.")
          int sourceCount,
      @Column(displayName = "Scanned Java sources", description = "Scoped Java-source count.")
          int javaSourceCount,
      @Column(displayName = "Scan cues", description = "Deterministic repository cues.")
          List<String> scanCues) {}

  private static String token(String value, String field, int maxLength) {
    String checked = Objects.toString(value, "").strip();
    if (checked.isEmpty()
        || checked.length() > maxLength
        || checked.indexOf('\0') >= 0
        || checked.indexOf('\r') >= 0
        || checked.indexOf('\n') >= 0) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }

  private static String optional(String value, int maxLength) {
    String checked = Objects.requireNonNullElse(value, "").strip();
    if (checked.length() > maxLength || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("problemTerm");
    }
    return checked;
  }
}
