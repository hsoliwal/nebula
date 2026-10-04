// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic projection from discovered APIs to admitted MIndex/Minex and representation owners.
 *
 * <p>This is inventory evidence, not a semantic-equivalence proof. Candidates are ranked only
 * within the exact Maven module, or the legacy top-level bucket when that module has no candidates.
 * No source text is rescanned.</p>
 */
public final class RepoApiCapabilityMap {
  public enum Confidence {
    EXACT_OWNER,
    TOKEN_MATCH,
    MODULE_ONLY,
    UNRESOLVED
  }

  public enum GapAction {
    NONE,
    ADD_PRECOMPUTE_INDEX,
    ADD_INDEX,
    REVIEW_RUNTIME_ONLY,
    RESOLVE_STRUCTURE_OWNER
  }

  public record Row(
      RepoApiRecord api,
      String mavenModule,
      String structureOwner,
      String structurePath,
      String representationOwner,
      String representationPath,
      String capabilities,
      String strategy,
      Confidence confidence,
      GapAction gapAction,
      String unresolvedReason) {
    public Row {
      Objects.requireNonNull(api, "api");
      mavenModule = required(mavenModule, "mavenModule");
      structureOwner = required(structureOwner, "structureOwner");
      structurePath = required(structurePath, "structurePath");
      representationOwner = required(representationOwner, "representationOwner");
      representationPath = required(representationPath, "representationPath");
      capabilities = required(capabilities, "capabilities");
      strategy = required(strategy, "strategy");
      Objects.requireNonNull(confidence, "confidence");
      Objects.requireNonNull(gapAction, "gapAction");
      unresolvedReason = Objects.requireNonNull(unresolvedReason, "unresolvedReason");
    }

    public boolean indexed() {
      return capabilities.contains("INDEX");
    }

    public boolean precomputed() {
      return capabilities.contains("PRECOMPUTE");
    }
  }

  private static final EnumSet<RepoSignal> REPRESENTATION_SIGNALS =
      EnumSet.of(
          RepoSignal.INDEX,
          RepoSignal.PRECOMPUTE,
          RepoSignal.CACHE,
          RepoSignal.SERIALIZATION,
          RepoSignal.JINI,
          RepoSignal.JNI);

  private RepoApiCapabilityMap() {}

  public static List<Row> build(
      List<RepoFileRecord> files,
      List<RepoApiRecord> apis,
      RepositoryModulePathIndex modules) {
    Objects.requireNonNull(files, "files");
    Objects.requireNonNull(apis, "apis");
    Objects.requireNonNull(modules, "modules");

    Map<RepoFileRecord, FileProjection> projections = new java.util.IdentityHashMap<>();
    Map<String, Set<String>> tokensByName = new HashMap<>();
    Map<String, OwnerScope> mavenScopes = new HashMap<>();
    Map<String, OwnerScope> legacyScopes = new HashMap<>();
    Map<String, RepoFileRecord> byPath = new HashMap<>();
    Map<String, List<RepoFileRecord>> byMavenModule = new LinkedHashMap<>();
    Map<String, List<RepoFileRecord>> byLegacyModule = new LinkedHashMap<>();
    for (RepoFileRecord file : files) {
      byPath.put(file.path(), file);
      projections.put(file, new FileProjection(Set.copyOf(file.typeNames()),
          typeTokens(file.typeNames()), packageParts(file.packageName()), capabilityDensity(file)));
      byMavenModule.computeIfAbsent(modules.ownerOf(file.path()), ignored -> new ArrayList<>())
          .add(file);
      byLegacyModule.computeIfAbsent(file.module(), ignored -> new ArrayList<>()).add(file);
    }
    byMavenModule.values().forEach(list -> list.sort(Comparator.comparing(RepoFileRecord::path)));
    byLegacyModule.values().forEach(list -> list.sort(Comparator.comparing(RepoFileRecord::path)));

    ArrayList<Row> rows = new ArrayList<>(apis.size());
    for (RepoApiRecord api : apis) {
      RepoFileRecord apiFile = Objects.requireNonNull(byPath.get(api.path()), "API file");
      String mavenModule = modules.ownerOf(api.path());
      // Scope filtering and file metadata are computed once per immutable inventory.
      OwnerScope scope = mavenScopes.computeIfAbsent(mavenModule, module ->
          ownerScope(module.equals(".") && !byPath.containsKey("pom.xml")
              ? List.of() : byMavenModule.getOrDefault(module, List.of())));
      if (scope.structures().isEmpty() && scope.representations().isEmpty()) {
        scope = legacyScopes.computeIfAbsent(apiFile.module(), module ->
            ownerScope(byLegacyModule.getOrDefault(module, List.of())));
      }

      Candidate structure = best(api, apiFile, mavenModule, scope.structures(), projections, tokensByName);
      Candidate representation = best(api, apiFile, mavenModule, scope.representations(), projections, tokensByName);
      Confidence confidence = confidence(api, structure, representation);
      String capabilities = capabilities(representation == null ? null : representation.file());
      GapAction gap = gap(structure, representation);
      String strategy = strategy(structure, representation, capabilities, gap);
      String unresolved = unresolved(structure, representation);

      rows.add(
          new Row(
              api,
              mavenModule,
              owner(structure),
              path(structure),
              owner(representation),
              path(representation),
              capabilities,
              strategy,
              confidence,
              gap,
              unresolved));
    }

    rows.sort(
        Comparator.comparing((Row row) -> row.api().path())
            .thenComparingInt(row -> row.api().line())
            .thenComparing(row -> row.api().signature()));
    return List.copyOf(rows);
  }

  private static List<RepoFileRecord> selectStructures(List<RepoFileRecord> files) {
    ArrayList<RepoFileRecord> result = new ArrayList<>();
    for (RepoFileRecord file : files) {
      if (file.signals().contains(RepoSignal.MINDEX_OWNER)
          || file.signals().contains(RepoSignal.MINEX_OWNER)) {
        result.add(file);
      }
    }
    return result;
  }

  private static List<RepoFileRecord> selectRepresentations(List<RepoFileRecord> files) {
    ArrayList<RepoFileRecord> result = new ArrayList<>();
    for (RepoFileRecord file : files) {
      if (hasAny(file.signals(), REPRESENTATION_SIGNALS)) result.add(file);
    }
    return result;
  }

  private record FileProjection(
      Set<String> owners, Set<String> typeTokens, String[] packageParts, int density) {}

  private record OwnerScope(List<RepoFileRecord> structures, List<RepoFileRecord> representations) {}

  private static OwnerScope ownerScope(List<RepoFileRecord> files) {
    return new OwnerScope(selectStructures(files), selectRepresentations(files));
  }

  private record Candidate(RepoFileRecord file, int score, boolean exactOwner, boolean tokenMatch) {}

  private static Candidate best(
      RepoApiRecord api,
      RepoFileRecord apiFile,
      String mavenModule,
      List<RepoFileRecord> candidates,
      Map<RepoFileRecord, FileProjection> projections,
      Map<String, Set<String>> tokensByName) {
    Candidate best = null;
    Set<String> ownerTokens = tokensByName.computeIfAbsent(api.owner(), RepoApiCapabilityMap::tokens);
    Set<String> apiTokens = tokensByName.computeIfAbsent(api.name(), RepoApiCapabilityMap::tokens);
    String[] apiPackage = projections.get(apiFile).packageParts();

    for (RepoFileRecord file : candidates) {
      FileProjection projection = projections.get(file);
      boolean exactOwner = projection.owners().contains(api.owner());
      int ownerOverlap = overlap(ownerTokens, projection.typeTokens());
      int apiOverlap = overlap(apiTokens, projection.typeTokens());
      boolean tokenMatch = ownerOverlap > 0 || apiOverlap > 0;
      int packageProximity = packagePrefix(apiPackage, projection.packageParts());
      int density = projection.density();
      int exactModule = mavenModule.equals(moduleOf(file, mavenModule)) ? 1 : 0;
      int score =
          (exactOwner ? 1000 : 0)
              + ownerOverlap * 100
              + apiOverlap * 20
              + packageProximity * 5
              + density
              + exactModule * 10;
      Candidate candidate = new Candidate(file, score, exactOwner, tokenMatch);
      if (best == null
          || candidate.score() > best.score()
          || (candidate.score() == best.score()
              && candidate.file().path().compareTo(best.file().path()) < 0)) {
        best = candidate;
      }
    }
    return best;
  }

  /**
   * Candidate lists are already scoped by module. This method intentionally does not rediscover the
   * module from source paths.
   */
  private static String moduleOf(RepoFileRecord file, String admittedModule) {
    Objects.requireNonNull(file, "file");
    return admittedModule;
  }

  private static Confidence confidence(
      RepoApiRecord api, Candidate structure, Candidate representation) {
    if (structure == null && representation == null) return Confidence.UNRESOLVED;
    if ((structure != null && structure.exactOwner())
        || (representation != null && representation.exactOwner())) {
      return Confidence.EXACT_OWNER;
    }
    if ((structure != null && structure.tokenMatch())
        || (representation != null && representation.tokenMatch())) {
      return Confidence.TOKEN_MATCH;
    }
    return Confidence.MODULE_ONLY;
  }

  private static GapAction gap(Candidate structure, Candidate representation) {
    if (structure == null) return GapAction.RESOLVE_STRUCTURE_OWNER;
    if (representation == null) return GapAction.ADD_PRECOMPUTE_INDEX;
    Set<RepoSignal> signals = representation.file().signals();
    if (signals.contains(RepoSignal.PRECOMPUTE) && signals.contains(RepoSignal.INDEX)) {
      return GapAction.NONE;
    }
    if (signals.contains(RepoSignal.PRECOMPUTE)) return GapAction.ADD_INDEX;
    if (signals.contains(RepoSignal.INDEX)) return GapAction.NONE;
    if (signals.contains(RepoSignal.JINI)
        || signals.contains(RepoSignal.JNI)
        || signals.contains(RepoSignal.CACHE)
        || signals.contains(RepoSignal.SERIALIZATION)) {
      return GapAction.ADD_PRECOMPUTE_INDEX;
    }
    return GapAction.REVIEW_RUNTIME_ONLY;
  }

  private static String strategy(
      Candidate structure,
      Candidate representation,
      String capabilities,
      GapAction gap) {
    if (structure == null) return "resolve-structure-owner";
    if (representation == null) return "add-precomputed-index";
    if (capabilities.contains("PRECOMPUTE") && capabilities.contains("INDEX")) {
      return "reuse-precomputed-index";
    }
    if (capabilities.contains("INDEX")) return "reuse-or-extend-index";
    if (capabilities.contains("PRECOMPUTE")) return "reuse-precompute-add-index";
    if (capabilities.contains("JINI")) return "reuse-jini-boundary-add-index";
    if (capabilities.contains("JNI")) return "reuse-jni-boundary-add-index";
    if (gap == GapAction.ADD_PRECOMPUTE_INDEX) return "add-precomputed-index";
    return "runtime-review";
  }

  private static String unresolved(Candidate structure, Candidate representation) {
    if (structure == null && representation == null) {
      return "no admitted MIndex/Minex or representation owner in module scope";
    }
    if (structure == null) return "no admitted MIndex/Minex structure owner in module scope";
    if (representation == null) return "no admitted index/precompute/cache/serialization/JINI/JNI owner";
    return "";
  }

  private static String owner(Candidate candidate) {
    if (candidate == null) return "(unresolved)";
    if (candidate.file().typeNames().isEmpty()) return candidate.file().path();
    return String.join(",", candidate.file().typeNames());
  }

  private static String path(Candidate candidate) {
    return candidate == null ? "(unresolved)" : candidate.file().path();
  }

  private static String capabilities(RepoFileRecord file) {
    if (file == null) return "NONE";
    ArrayList<String> values = new ArrayList<>();
    for (RepoSignal signal : REPRESENTATION_SIGNALS) {
      if (file.signals().contains(signal)) values.add(signal.name());
    }
    return values.isEmpty() ? "RUNTIME" : String.join(",", values);
  }

  private static int capabilityDensity(RepoFileRecord file) {
    int count = 0;
    for (RepoSignal signal : REPRESENTATION_SIGNALS) {
      if (file.signals().contains(signal)) count++;
    }
    if (file.signals().contains(RepoSignal.MINDEX_OWNER)) count++;
    if (file.signals().contains(RepoSignal.MINEX_OWNER)) count++;
    return count;
  }

  private static String[] packageParts(String value) {
    return value == null || value.isBlank() ? new String[0] : value.split("\\.");
  }

  private static int packagePrefix(String[] a, String[] b) {
    int count = 0;
    while (count < a.length && count < b.length && a[count].equals(b[count])) count++;
    return count;
  }

  private static Set<String> typeTokens(List<String> typeNames) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (String type : typeNames) result.addAll(tokens(type));
    return result;
  }

  private static Set<String> tokens(String value) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    if (value == null || value.isBlank()) return result;
    String splitCamel =
        value.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
            .replaceAll("[^A-Za-z0-9]+", " ")
            .toLowerCase(Locale.ROOT);
    for (String token : splitCamel.split("\\s+")) {
      if (token.length() >= 2
          && !Set.of("get", "set", "is", "to", "of", "for", "from", "with").contains(token)) {
        result.add(token);
      }
    }
    return result;
  }

  private static int overlap(Set<String> left, Set<String> right) {
    int result = 0;
    for (String value : left) if (right.contains(value)) result++;
    return result;
  }

  private static boolean hasAny(Set<RepoSignal> source, Set<RepoSignal> wanted) {
    for (RepoSignal signal : wanted) if (source.contains(signal)) return true;
    return false;
  }

  private static String required(String value, String label) {
    String checked = Objects.requireNonNull(value, label).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(label);
    return checked;
  }
}
