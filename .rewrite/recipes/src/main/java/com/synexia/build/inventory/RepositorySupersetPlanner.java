// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Mechanical multipass planner over repository inventory evidence.
 *
 * <p>The planner prefers an owner already present in the same module, then surfaces cross-module
 * candidates, and only emits IMPLEMENT when no indexed/precomputed/design owner is visible.</p>
 */
public final class RepositorySupersetPlanner {

  public RepositorySupersetPlan plan(RepositorySupersetInventory inventory) {
    Objects.requireNonNull(inventory, "inventory");

    Map<String, RepositorySupersetInventory.FileRecord> fileByPath = new TreeMap<>();
    inventory.files().forEach(file -> fileByPath.put(file.path(), file));

    RepositoryModulePathIndex moduleOwnership =
        RepositoryModulePathIndex.fromModules(inventory.modules());

    Map<String, RepositorySupersetInventory.ModuleRecord> moduleByPath = new TreeMap<>();
    inventory.modules().forEach(module -> moduleByPath.put(module.path(), module));

    TreeMap<String, TreeSet<String>> ownersByRole = new TreeMap<>();
    inventory.modules().forEach(
        module ->
            module.roles().forEach(
                role -> ownersByRole.computeIfAbsent(role, ignored -> new TreeSet<>()).add(module.path())));

    TreeMap<String, RepositorySupersetPlan.Task> tasks = new TreeMap<>();

    for (RepositorySupersetInventory.Finding finding : inventory.findings()) {
      String module = moduleOwnership.ownerOf(finding.path());
      FindingProjection projection = project(finding.kind());
      if (projection == null) continue;
      List<String> candidates = ownerCandidates(module, projection.ownerRole(), ownersByRole);
      addTask(
          tasks,
          projection.stage(),
          projection.action(),
          module,
          finding.path(),
          finding.line(),
          projection.capability(),
          finding.kind() + ": " + finding.evidence(),
          candidates);
    }

    ArrayList<RepositorySupersetPlan.ApiStructureMap> mappings = new ArrayList<>();
    for (RepositorySupersetInventory.ApiFact api : inventory.apiFacts()) {
      if (!api.kind().equals("METHOD") && !api.kind().equals("INTERFACE_METHOD")) continue;
      RepositorySupersetInventory.FileRecord file = fileByPath.get(api.path());
      if (file == null) continue;
      String module = moduleOwnership.ownerOf(api.path());
      List<String> required = requiredStructures(api);
      if (required.isEmpty()) continue;

      LinkedHashSet<String> candidates = new LinkedHashSet<>();
      boolean allLocal = true;
      boolean allMissingSatisfiedByModule = true;
      boolean unresolvedStructure = false;
      boolean crossModuleOwner = false;
      RepositorySupersetInventory.ModuleRecord moduleRecord = moduleByPath.get(module);
      for (String structure : required) {
        if (file.roles().contains(structure)) continue;
        allLocal = false;
        if (moduleRecord != null && moduleRecord.roles().contains(structure)) {
          candidates.add(module);
          continue;
        }
        allMissingSatisfiedByModule = false;
        List<String> owners = ownerCandidates(module, structure, ownersByRole);
        if (owners.isEmpty()) {
          unresolvedStructure = true;
        } else {
          crossModuleOwner = true;
          candidates.addAll(owners);
        }
      }

      String strategy;
      if (allLocal) {
        strategy = "REUSE_FILE_OWNER";
      } else if (allMissingSatisfiedByModule) {
        strategy = "ADAPT_EXISTING_MODULE_OWNER";
      } else if (unresolvedStructure) {
        strategy = "IMPLEMENT_REQUIRED_STRUCTURE";
      } else if (crossModuleOwner) {
        strategy = "REVIEW_CROSS_MODULE_REUSE";
      } else {
        strategy = "IMPLEMENT_REQUIRED_STRUCTURE";
      }

      mappings.add(
          new RepositorySupersetPlan.ApiStructureMap(
              api.path(),
              module,
              api.kind(),
              api.name(),
              api.signature(),
              required,
              List.copyOf(file.roles()),
              strategy,
              candidates.stream().limit(12).toList()));

      if (!allLocal) {
        RepositorySupersetPlan.Action action =
            strategy.equals("IMPLEMENT_REQUIRED_STRUCTURE")
                ? RepositorySupersetPlan.Action.IMPLEMENT
                : RepositorySupersetPlan.Action.ENHANCE;
        addTask(
            tasks,
            RepositorySupersetPlan.Stage.DATA_PRECOMPUTE,
            action,
            module,
            api.path(),
            0,
            String.join("+", required),
            api.signature() + " -> " + strategy,
            candidates.stream().limit(12).toList());
      }
    }

    for (RepositorySupersetInventory.ModuleRecord module : inventory.modules()) {
      if (module.roles().contains("JINI") && !module.roles().contains("PROGRESS_MONITOR")) {
        addTask(
            tasks,
            RepositorySupersetPlan.Stage.DESIGN_ABSTRACTION,
            RepositorySupersetPlan.Action.ENHANCE,
            module.path(),
            module.path(),
            0,
            "PROGRESS_MONITOR_PROPAGATION",
            "JINI module has no mechanical ProgressMonitor signal",
            ownerCandidates(module.path(), "PROGRESS_MONITOR", ownersByRole));
      }
      if (module.roles().contains("PRECOMPUTE") && !module.roles().contains("INDEX")) {
        addTask(
            tasks,
            RepositorySupersetPlan.Stage.DATA_PRECOMPUTE,
            RepositorySupersetPlan.Action.ENHANCE,
            module.path(),
            module.path(),
            0,
            "INDEXED_PRECOMPUTE",
            "precompute role without index role",
            ownerCandidates(module.path(), "INDEX", ownersByRole));
      }
    }

    mappings.sort(
        Comparator.comparing(RepositorySupersetPlan.ApiStructureMap::path)
            .thenComparing(RepositorySupersetPlan.ApiStructureMap::kind)
            .thenComparing(RepositorySupersetPlan.ApiStructureMap::name)
            .thenComparing(RepositorySupersetPlan.ApiStructureMap::signature));

    List<RepositorySupersetPlan.Task> ordered = List.copyOf(tasks.values());
    RepositorySupersetPlan.Summary summary = summary(ordered, mappings);
    return new RepositorySupersetPlan(ordered, mappings, summary);
  }

  /**
   * Builds a review-only LeetCode/HackerRank reuse plan from checked-in pinned donor ledgers.
   *
   * <p>Matching is deliberately conservative: normalized API names must equal either a canonical
   * Synexia kernel leaf or an exact challenge title. Generic words such as "search" never imply
   * equivalence. Existing kernels are always preferred over new implementations.</p>
   */
  public RepositoryCompetitiveAlgorithmPlan planCompetitiveAlgorithms(
      Path repositoryRoot, RepositorySupersetInventory inventory) throws IOException {
    return RepositoryCompetitiveDonorPlanner.plan(
        Objects.requireNonNull(repositoryRoot, "repositoryRoot"),
        Objects.requireNonNull(inventory, "inventory"));
  }

  private static List<String> requiredStructures(RepositorySupersetInventory.ApiFact api) {
    String text =
        (api.name() + " " + api.signature() + " " + api.contract()).toLowerCase(Locale.ROOT);
    TreeSet<String> required = new TreeSet<>();

    if (containsAny(
        text,
        "lookup",
        "find",
        "search",
        "resolve",
        "contains",
        "prefix",
        "suffix",
        "rank",
        "sort",
        "compare",
        "distance",
        "similar",
        "translate",
        "concept",
        "frequency",
        "occurrence")) {
      required.add("INDEX");
    }
    if (containsAny(
        text,
        "lookup",
        "resolve",
        "prefix",
        "suffix",
        "distance",
        "translate",
        "hash",
        "frequency",
        "occurrence",
        "compile",
        "precompute")) {
      required.add("PRECOMPUTE");
    }
    if (containsAny(text, "cache", "memo", "intern")) required.add("CACHE");
    if (containsAny(text, "serialize", "deserialize", "codec", "json", "yaml", "xml", "csv")) {
      required.add("SERIALIZATION");
    }
    if (containsAny(text, "jini", "jeri", "remote", "service discovery", "lease")) {
      required.add("JINI");
    }
    if (containsAny(text, "progress", "cancel")) required.add("PROGRESS_MONITOR");
    if (containsAny(text, "jni", "jna", "native")) required.add("NATIVE");
    return List.copyOf(required);
  }

  private static boolean containsAny(String text, String... needles) {
    for (String needle : needles) {
      if (text.contains(needle)) return true;
    }
    return false;
  }

  private static FindingProjection project(String kind) {
    return switch (kind) {
      case "PLACEHOLDER_MARKER", "UNSUPPORTED_OPERATION" ->
          new FindingProjection(
              RepositorySupersetPlan.Stage.API_COVERAGE,
              RepositorySupersetPlan.Action.IMPLEMENT,
              "MISSING_IMPLEMENTATION",
              "");
      case "EXACT_DUPLICATE_JAVA", "DUPLICATE_ARTIFACT_ID" ->
          new FindingProjection(
              RepositorySupersetPlan.Stage.CONSISTENCY,
              RepositorySupersetPlan.Action.CONSOLIDATE,
              "DUPLICATE_IMPLEMENTATION",
              "");
      case "UNREGISTERED_MODULE_CANDIDATE", "MISSING_REACTOR_MODULE",
          "DUPLICATE_REACTOR_DECLARATION" ->
          new FindingProjection(
              RepositorySupersetPlan.Stage.STRUCTURAL_SUPERSET,
              RepositorySupersetPlan.Action.VERIFY,
              "REACTOR_STRUCTURE",
              "");
      case "MODULE_WITHOUT_TEST_SOURCE" ->
          new FindingProjection(
              RepositorySupersetPlan.Stage.VERIFICATION,
              RepositorySupersetPlan.Action.IMPLEMENT,
              "TEST_COVERAGE",
              "");
      case "NULL_RETURN" ->
          new FindingProjection(
              RepositorySupersetPlan.Stage.CONSISTENCY,
              RepositorySupersetPlan.Action.VERIFY,
              "NULL_CONTRACT",
              "");
      case "SYMLINK_DIRECTORY", "SYMLINK_FILE" ->
          new FindingProjection(
              RepositorySupersetPlan.Stage.STRUCTURAL_SUPERSET,
              RepositorySupersetPlan.Action.VERIFY,
              "PATH_SAFETY",
              "");
      case "JAVA_SOURCE_TOO_LARGE_FOR_LEXICAL_SCAN" ->
          new FindingProjection(
              RepositorySupersetPlan.Stage.VERIFICATION,
              RepositorySupersetPlan.Action.ENHANCE,
              "INVENTORY_BOUNDS",
              "");
      default -> null;
    };
  }

  private static void addTask(
      Map<String, RepositorySupersetPlan.Task> tasks,
      RepositorySupersetPlan.Stage stage,
      RepositorySupersetPlan.Action action,
      String module,
      String path,
      int line,
      String capability,
      String evidence,
      List<String> candidates) {
    String id = taskId(stage, action, module, path, line, capability, evidence);
    tasks.putIfAbsent(
        id,
        new RepositorySupersetPlan.Task(
            id,
            stage,
            action,
            module,
            path,
            line,
            capability,
            evidence,
            candidates));
  }

  private static List<String> ownerCandidates(
      String module,
      String role,
      Map<String, TreeSet<String>> ownersByRole) {
    if (role == null || role.isBlank()) return List.of();
    TreeSet<String> owners = ownersByRole.get(role);
    if (owners == null || owners.isEmpty()) return List.of();
    ArrayList<String> ordered = new ArrayList<>();
    if (owners.contains(module)) ordered.add(module);
    for (String owner : owners) {
      if (!owner.equals(module)) ordered.add(owner);
      if (ordered.size() >= 12) break;
    }
    return List.copyOf(ordered);
  }

  private static String taskId(
      RepositorySupersetPlan.Stage stage,
      RepositorySupersetPlan.Action action,
      String module,
      String path,
      int line,
      String capability,
      String evidence) {
    MessageDigest digest = sha();
    update(digest, stage.name());
    update(digest, action.name());
    update(digest, module);
    update(digest, path);
    update(digest, Integer.toString(line));
    update(digest, capability);
    update(digest, evidence);
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void update(MessageDigest digest, String value) {
    digest.update(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8));
    digest.update((byte) 0);
  }

  private static MessageDigest sha() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static RepositorySupersetPlan.Summary summary(
      List<RepositorySupersetPlan.Task> tasks,
      List<RepositorySupersetPlan.ApiStructureMap> mappings) {
    return new RepositorySupersetPlan.Summary(
        tasks.size(),
        mappings.size(),
        countAction(tasks, RepositorySupersetPlan.Action.REUSE),
        countAction(tasks, RepositorySupersetPlan.Action.ENHANCE),
        countAction(tasks, RepositorySupersetPlan.Action.IMPLEMENT),
        countAction(tasks, RepositorySupersetPlan.Action.CONSOLIDATE),
        countAction(tasks, RepositorySupersetPlan.Action.VERIFY),
        countStage(tasks, RepositorySupersetPlan.Stage.API_COVERAGE),
        countStage(tasks, RepositorySupersetPlan.Stage.STRUCTURAL_SUPERSET),
        countStage(tasks, RepositorySupersetPlan.Stage.DATA_PRECOMPUTE),
        countStage(tasks, RepositorySupersetPlan.Stage.DESIGN_ABSTRACTION),
        countStage(tasks, RepositorySupersetPlan.Stage.CONSISTENCY),
        countStage(tasks, RepositorySupersetPlan.Stage.VERIFICATION));
  }

  private static int countAction(
      List<RepositorySupersetPlan.Task> tasks, RepositorySupersetPlan.Action action) {
    return (int) tasks.stream().filter(task -> task.action() == action).count();
  }

  private static int countStage(
      List<RepositorySupersetPlan.Task> tasks, RepositorySupersetPlan.Stage stage) {
    return (int) tasks.stream().filter(task -> task.stage() == stage).count();
  }

  private record FindingProjection(
      RepositorySupersetPlan.Stage stage,
      RepositorySupersetPlan.Action action,
      String capability,
      String ownerRole) {}
}
