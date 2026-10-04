// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Pinned framework recipe-pattern metadata. No donor source-copy authority is granted here. */
public final class M3FrameworkRecipePatternCatalog {
  public enum LicenseFamily {
    APACHE_2_0,
    MODERNE_SOURCE_AVAILABLE
  }

  public enum Mechanic {
    SCANNING_ACCUMULATOR,
    GENERATE_AFTER_SCAN,
    PRECONDITION_GATE,
    TYPED_DATA_TABLE,
    JAVA_TEMPLATE_WITH_PARSER_CONTEXT
  }

  public record Pattern(
      String id,
      String repository,
      String revision,
      String path,
      String blobSha,
      LicenseFamily license,
      Set<Mechanic> mechanics,
      Set<String> cues,
      String evidenceMode) {
    public Pattern {
      id = text(id, "id");
      repository = text(repository, "repository");
      revision = sha(revision, "revision");
      path = text(path, "path");
      blobSha = sha(blobSha, "blobSha");
      license = Objects.requireNonNull(license, "license");
      mechanics = Set.copyOf(Objects.requireNonNull(mechanics, "mechanics"));
      cues = normalized(cues);
      evidenceMode = text(evidenceMode, "evidenceMode");
      if (mechanics.isEmpty()) throw new IllegalArgumentException("mechanics");
      if (cues.isEmpty()) throw new IllegalArgumentException("cues");
    }

    public boolean sourceCopyAuthority() {
      return false;
    }

    public boolean replacementAuthority() {
      return false;
    }
  }

  private static final List<Pattern> CANONICAL =
      List.of(
          pattern(
              "openrewrite-core-scanning-recipe",
              "openrewrite/rewrite",
              "e7df319a3707f7af0aca56076be71428c9859d25",
              "rewrite-core/src/main/java/org/openrewrite/ScanningRecipe.java",
              "461a0f40149db5c6ed98a5d485d1b3d9e6b1137f",
              LicenseFamily.APACHE_2_0,
              Set.of(Mechanic.SCANNING_ACCUMULATOR, Mechanic.GENERATE_AFTER_SCAN),
              "scan",
              "repository",
              "inventory",
              "java"),
          pattern(
              "spring-api-manifest-scan",
              "openrewrite/rewrite-spring",
              "0cddf2bd899e3c423bc393afe117dd121ecf0131",
              "src/main/java/org/openrewrite/java/spring/UpdateApiManifest.java",
              "62fb79dd85267d213fe6337fe0a8bd6fc77f1f33",
              LicenseFamily.MODERNE_SOURCE_AVAILABLE,
              Set.of(
                  Mechanic.SCANNING_ACCUMULATOR,
                  Mechanic.GENERATE_AFTER_SCAN,
                  Mechanic.PRECONDITION_GATE),
              "spring",
              "api",
              "manifest",
              "endpoint"),
          pattern(
              "spring-logback-precondition",
              "openrewrite/rewrite-spring",
              "0cddf2bd899e3c423bc393afe117dd121ecf0131",
              "src/main/java/org/openrewrite/java/spring/boot3/RenameLogbackToLogbackSpring.java",
              "e07a9356364cb5af61732735d3a08372adc5c18e",
              LicenseFamily.MODERNE_SOURCE_AVAILABLE,
              Set.of(Mechanic.PRECONDITION_GATE),
              "spring",
              "logback",
              "xml",
              "configuration"),
          pattern(
              "java-migration-plan-scan",
              "openrewrite/rewrite-migrate-java",
              "a84e48342cc83e4e37f9753c2953e1bb32d063ae",
              "src/main/java/org/openrewrite/java/migrate/search/PlanJavaMigration.java",
              "e964372fb0a271adb388c212cd45c97ba34aa257",
              LicenseFamily.MODERNE_SOURCE_AVAILABLE,
              Set.of(Mechanic.SCANNING_ACCUMULATOR, Mechanic.TYPED_DATA_TABLE),
              "java",
              "migration",
              "maven",
              "gradle",
              "version"),
          pattern(
              "junit-tempdir-template",
              "openrewrite/rewrite-testing-frameworks",
              "95ef0e7b04c444ac5c96402d4dc117b0121b8383",
              "src/main/java/org/openrewrite/java/testing/junit5/TemporaryFolderToTempDir.java",
              "799da50fbd2a1d5b4722429cc7bf772c8b7d8852",
              LicenseFamily.MODERNE_SOURCE_AVAILABLE,
              Set.of(Mechanic.PRECONDITION_GATE, Mechanic.JAVA_TEMPLATE_WITH_PARSER_CONTEXT),
              "junit",
              "test",
              "testing",
              "template"),
          pattern(
              "static-analysis-java-precondition",
              "openrewrite/rewrite-static-analysis",
              "767ad0fea4ed35e3035dec99c3eb1d63558f9fef",
              "src/main/java/org/openrewrite/staticanalysis/FinalClass.java",
              "1adaa65b9af7e1b3709e80fab433208dc8aa15fb",
              LicenseFamily.MODERNE_SOURCE_AVAILABLE,
              Set.of(Mechanic.PRECONDITION_GATE),
              "java",
              "static",
              "analysis",
              "lint"));

  private M3FrameworkRecipePatternCatalog() {}

  public static List<Pattern> canonical() {
    return CANONICAL;
  }

  public static List<Pattern> select(Set<String> scanCues, String problemTerm) {
    TreeSet<String> evidence = new TreeSet<>();
    for (String cue : Objects.requireNonNull(scanCues, "scanCues")) {
      String normalized = Objects.toString(cue, "").strip().toLowerCase(Locale.ROOT);
      if (!normalized.isEmpty()) evidence.add(normalized);
    }
    String term = Objects.toString(problemTerm, "").toLowerCase(Locale.ROOT);

    ArrayList<Pattern> selected = new ArrayList<>();
    for (Pattern pattern : CANONICAL) {
      if (pattern.id().equals("openrewrite-core-scanning-recipe")
          || pattern.cues().stream().anyMatch(cue -> evidence.contains(cue) || term.contains(cue))) {
        selected.add(pattern);
      }
    }
    selected.sort(Comparator.comparing(Pattern::id));
    return List.copyOf(selected);
  }

  private static Pattern pattern(
      String id,
      String repository,
      String revision,
      String path,
      String blobSha,
      LicenseFamily license,
      Set<Mechanic> mechanics,
      String... cues) {
    return new Pattern(
        id,
        repository,
        revision,
        path,
        blobSha,
        license,
        mechanics,
        Set.of(cues),
        license == LicenseFamily.APACHE_2_0
            ? "API_AND_MECHANICS_REFERENCE"
            : "PATTERN_EVIDENCE_ONLY");
  }

  private static Set<String> normalized(Set<String> values) {
    TreeSet<String> result = new TreeSet<>();
    for (String value : Objects.requireNonNull(values, "values")) {
      String checked = Objects.toString(value, "").strip().toLowerCase(Locale.ROOT);
      if (!checked.isEmpty()) result.add(checked);
    }
    return Set.copyOf(result);
  }

  private static String text(String value, String field) {
    String checked = Objects.toString(value, "").strip();
    if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }

  private static String sha(String value, String field) {
    String checked = text(value, field);
    if (!checked.matches("[0-9a-f]{40}")) throw new IllegalArgumentException(field);
    return checked;
  }
}
