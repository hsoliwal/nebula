// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Canonical immutable output of one complete repository scan. */
public final class RepoSupersetInventory {
  public record DuplicateCluster(String sha256, List<String> paths) {
    public DuplicateCluster {
      Objects.requireNonNull(sha256, "sha256");
      if (!sha256.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("sha256");
      paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
      if (paths.size() < 2) throw new IllegalArgumentException("duplicate cluster size");
    }
  }

  /** Similarity candidate cluster. Never an equivalence proof by itself. */
  public record CandidateCluster(String lane, String value, List<String> paths) {
    public CandidateCluster {
      lane = required(lane, "lane");
      value = required(value, "value");
      paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
      if (paths.size() < 2) throw new IllegalArgumentException("candidate cluster size");
    }
  }

  /** One API mapped to its current implementation/storage signals and next mechanical strategy. */
  public record ApiCoverage(
      RepoApiRecord api,
      String requiredStructure,
      String representation,
      String strategy,
      boolean indexed,
      boolean precomputed,
      boolean jini,
      boolean cached,
      boolean serialized,
      boolean nativeBridge) {
    public ApiCoverage {
      Objects.requireNonNull(api, "api");
      requiredStructure = required(requiredStructure, "requiredStructure");
      representation = required(representation, "representation");
      strategy = required(strategy, "strategy");
    }
  }

  public record ModuleSummary(
      String module,
      int files,
      int javaFiles,
      int tests,
      int apis,
      int mindexOwners,
      int jiniFiles,
      int precomputeFiles,
      int indexFiles,
      int cacheFiles,
      int stubSignals) {
    public ModuleSummary {
      if (module == null || module.isBlank()
          || files < 0
          || javaFiles < 0
          || tests < 0
          || apis < 0
          || mindexOwners < 0
          || jiniFiles < 0
          || precomputeFiles < 0
          || indexFiles < 0
          || cacheFiles < 0
          || stubSignals < 0) {
        throw new IllegalArgumentException("invalid module summary");
      }
    }
  }

  private final List<RepoFileRecord> files;
  private final List<RepoApiRecord> apis;
  private final List<DuplicateCluster> exactDuplicates;
  private final List<CandidateCluster> candidateClusters;
  private final List<ApiCoverage> apiCoverage;
  private final List<RepoApiCapabilityMap.Row> apiCapabilityMap;
  private final List<RepoOwnerCollisionIndex.Row> ownerCollisions;
  private final List<RepoApiCollisionIndex.Row> apiCollisions;
  private final RepoDerivedIndexes derivedIndexes;
  private final java.util.Map<RepoFileCategory, List<RepoFileRecord>> filesByCategory;
  private final java.util.Map<RepoLanguage, List<RepoFileRecord>> filesByLanguage;
  private final List<RepoAction> actions;
  private final List<ModuleSummary> modules;
  private final RepositoryModulePathIndex mavenModules;
  private final String fingerprint;

  RepoSupersetInventory(List<RepoFileRecord> sourceFiles, List<RepoAction> sourceActions) {
    this(sourceFiles, sourceActions, false);
  }

  /** Reuses the canonical exact clusters when publishing scanner review actions. */
  static RepoSupersetInventory fromScan(
      List<RepoFileRecord> sourceFiles, List<RepoAction> sourceActions) {
    return new RepoSupersetInventory(sourceFiles, sourceActions, true);
  }

  private RepoSupersetInventory(
      List<RepoFileRecord> sourceFiles, List<RepoAction> sourceActions, boolean includeDuplicateActions) {
    Objects.requireNonNull(sourceFiles, "sourceFiles");
    Objects.requireNonNull(sourceActions, "sourceActions");

    ArrayList<RepoFileRecord> sortedFiles = new ArrayList<>(sourceFiles);
    sortedFiles.sort(Comparator.comparing(RepoFileRecord::path));
    files = List.copyOf(sortedFiles);

    ArrayList<RepoApiRecord> allApis = new ArrayList<>();
    for (RepoFileRecord file : files) allApis.addAll(file.apis());
    allApis.sort(
        Comparator.comparing(RepoApiRecord::path)
            .thenComparingInt(RepoApiRecord::line)
            .thenComparing(RepoApiRecord::signature));
    apis = List.copyOf(allApis);

    exactDuplicates = List.copyOf(buildDuplicates(files));
    candidateClusters = List.copyOf(buildCandidateClusters(files));
    apiCoverage = List.copyOf(buildApiCoverage(files, apis));
    mavenModules = RepositoryModulePathIndex.fromPomFiles(files);
    apiCapabilityMap = RepoApiCapabilityMap.build(files, apis, mavenModules);
    ownerCollisions = RepoOwnerCollisionIndex.build(files);
    apiCollisions = RepoApiCollisionIndex.build(apis);
    derivedIndexes = RepoDerivedIndexes.build(files, apis);

    ArrayList<RepoAction> sortedActions = new ArrayList<>(sourceActions);
    if (includeDuplicateActions) {
      for (DuplicateCluster cluster : exactDuplicates) {
        for (String path : cluster.paths()) {
          sortedActions.add(new RepoAction(RepoAction.Severity.REVIEW, "exact-duplicate", path, 0,
              "Exact SHA-256 duplicate cluster size=" + cluster.paths().size()
                  + " sha256=" + cluster.sha256()));
        }
      }
    }
    sortedActions.addAll(collisionActions(ownerCollisions, apiCollisions));
    sortedActions.sort(Comparator.naturalOrder());
    actions = List.copyOf(sortedActions);

    modules = List.copyOf(buildModules(files));
    filesByCategory = indexByCategory(files);
    filesByLanguage = indexByLanguage(files);
    fingerprint = computeFingerprint();
  }

  public List<RepoFileRecord> files() {
    return files;
  }

  public List<RepoApiRecord> apis() {
    return apis;
  }

  public List<DuplicateCluster> exactDuplicates() {
    return exactDuplicates;
  }

  public List<CandidateCluster> candidateClusters() {
    return candidateClusters;
  }

  public List<ApiCoverage> apiCoverage() {
    return apiCoverage;
  }

  /** Mechanical API-to-structure/precompute plan; never a semantic-equivalence proof. */
  public List<RepoApiCapabilityMap.Row> apiCapabilityMap() {
    return apiCapabilityMap;
  }

  /** Precomputed same-type ownership collisions across the complete repository scan. */
  public List<RepoOwnerCollisionIndex.Row> ownerCollisions() {
    return ownerCollisions;
  }

  /** Precomputed same-owner/name API collisions across distinct source paths. */
  public List<RepoApiCollisionIndex.Row> apiCollisions() {
    return apiCollisions;
  }

  /** Repository-wide immutable path/module/signal/API indexes built once per inventory. */
  public RepoDerivedIndexes derivedIndexes() {
    return derivedIndexes;
  }

  /** Files owned by one logical module from the immutable precomputed module index. */
  public List<RepoFileRecord> filesByModule(String module) {
    return derivedIndexes.filesByModule(module);
  }

  /** Files carrying one mechanical signal from the immutable precomputed signal index. */
  public List<RepoFileRecord> filesBySignal(RepoSignal signal) {
    return derivedIndexes.filesBySignal(signal);
  }

  /** APIs declared by an owner/type from the immutable precomputed API index. */
  public List<RepoApiRecord> apisByOwner(String owner) {
    return derivedIndexes.apisByOwner(owner);
  }

  public List<RepoAction> actions() {
    return actions;
  }

  public List<ModuleSummary> modules() {
    return modules;
  }

  /** Files in a category, returned from the immutable precomputed in-memory index. */
  public List<RepoFileRecord> filesByCategory(RepoFileCategory category) {
    return filesByCategory.getOrDefault(Objects.requireNonNull(category, "category"), List.of());
  }

  /** Files in a language/format family, returned from the immutable precomputed index. */
  public List<RepoFileRecord> filesByLanguage(RepoLanguage language) {
    return filesByLanguage.getOrDefault(Objects.requireNonNull(language, "language"), List.of());
  }

  /** Exact deepest Maven module owner, derived from retained pom.xml paths. */
  public String mavenModule(String path) {
    return mavenModules.ownerOf(path);
  }

  /** Canonical Maven module paths visible to the broad inventory. */
  public List<String> mavenModulePaths() {
    return mavenModules.modulePaths();
  }

  /** Deterministic root of the derived Maven module ownership index. */
  public String mavenModuleIndexRoot() {
    return mavenModules.rootSha256();
  }

  public String fingerprint() {
    return fingerprint;
  }

  public RepoFileRecord requireFile(String path) {
    return derivedIndexes.requireFile(path);
  }

  private static List<DuplicateCluster> buildDuplicates(List<RepoFileRecord> files) {
    Map<String, List<String>> byHash = new LinkedHashMap<>();
    for (RepoFileRecord file : files) {
      byHash.computeIfAbsent(file.sha256(), ignored -> new ArrayList<>()).add(file.path());
    }
    ArrayList<DuplicateCluster> result = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : byHash.entrySet()) {
      if (entry.getValue().size() < 2) continue;
      ArrayList<String> paths = new ArrayList<>(entry.getValue());
      paths.sort(String::compareTo);
      result.add(new DuplicateCluster(entry.getKey(), paths));
    }
    result.sort(
        Comparator.comparingInt((DuplicateCluster cluster) -> cluster.paths().size())
            .reversed()
            .thenComparing(DuplicateCluster::sha256));
    return result;
  }

  private static List<CandidateCluster> buildCandidateClusters(List<RepoFileRecord> files) {
    ArrayList<CandidateCluster> result = new ArrayList<>();
    clusterLane(result, "structural-sha256", files, RepoFileRecord::structuralSha256);
    clusterLane(result, "logic-sha256", files, RepoFileRecord::logicSha256);
    clusterLane(
        result,
        "simhash64-exact",
        files,
        file -> Long.toUnsignedString(file.simHash64(), 16));
    result.sort(
        Comparator.comparing(CandidateCluster::lane)
            .thenComparingInt((CandidateCluster cluster) -> cluster.paths().size())
            .reversed()
            .thenComparing(CandidateCluster::value));
    return result;
  }

  private static void clusterLane(
      List<CandidateCluster> target,
      String lane,
      List<RepoFileRecord> files,
      java.util.function.Function<RepoFileRecord, String> keyFunction) {
    Map<String, List<String>> groups = new java.util.TreeMap<>();
    for (RepoFileRecord file : files) {
      groups.computeIfAbsent(keyFunction.apply(file), ignored -> new ArrayList<>()).add(file.path());
    }
    for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
      if (entry.getValue().size() < 2) continue;
      ArrayList<String> paths = new ArrayList<>(entry.getValue());
      paths.sort(String::compareTo);
      target.add(new CandidateCluster(lane, entry.getKey(), paths));
    }
  }

  private static List<ApiCoverage> buildApiCoverage(
      List<RepoFileRecord> files, List<RepoApiRecord> apis) {
    Map<String, RepoFileRecord> byPath = new java.util.HashMap<>();
    for (RepoFileRecord file : files) byPath.put(file.path(), file);
    ArrayList<ApiCoverage> result = new ArrayList<>(apis.size());
    for (RepoApiRecord api : apis) {
      RepoFileRecord file = Objects.requireNonNull(byPath.get(api.path()), "API file");
      boolean indexed = file.signals().contains(RepoSignal.INDEX);
      boolean precomputed = file.signals().contains(RepoSignal.PRECOMPUTE);
      boolean jini = file.signals().contains(RepoSignal.JINI);
      boolean cached = file.signals().contains(RepoSignal.CACHE);
      boolean serialized = file.signals().contains(RepoSignal.SERIALIZATION);
      boolean nativeBridge = file.signals().contains(RepoSignal.JNI);
      String representation =
          (indexed ? "index;" : "")
              + (precomputed ? "precompute;" : "")
              + (jini ? "jini;" : "")
              + (cached ? "cache;" : "")
              + (serialized ? "serialization;" : "")
              + (nativeBridge ? "jni;" : "");
      if (representation.isEmpty()) representation = "runtime";
      String strategy;
      if (indexed && precomputed) strategy = "reuse-precomputed-index";
      else if (indexed) strategy = "reuse-or-extend-index";
      else if (precomputed) strategy = "reuse-or-add-index";
      else if (jini) strategy = "reuse-jini-service-boundary";
      else strategy = "runtime-review-for-precompute-index";
      result.add(
          new ApiCoverage(
              api,
              api.owner(),
              representation,
              strategy,
              indexed,
              precomputed,
              jini,
              cached,
              serialized,
              nativeBridge));
    }
    result.sort(
        Comparator.comparing((ApiCoverage coverage) -> coverage.api().path())
            .thenComparingInt(coverage -> coverage.api().line())
            .thenComparing(coverage -> coverage.api().signature()));
    return result;
  }

  private static String required(String value, String label) {
    String checked = Objects.requireNonNull(value, label).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(label);
    return checked;
  }

  private static List<ModuleSummary> buildModules(List<RepoFileRecord> files) {
    Map<String, MutableModule> byModule = new LinkedHashMap<>();
    for (RepoFileRecord file : files) {
      MutableModule module = byModule.computeIfAbsent(file.module(), MutableModule::new);
      module.files++;
      if (".java".equals(file.extension())) module.javaFiles++;
      if (file.signals().contains(RepoSignal.TEST_SOURCE)) module.tests++;
      module.apis += file.apis().size();
      if (file.signals().contains(RepoSignal.MINDEX_OWNER)
          || file.signals().contains(RepoSignal.MINEX_OWNER)) module.mindexOwners++;
      if (file.signals().contains(RepoSignal.JINI)) module.jiniFiles++;
      if (file.signals().contains(RepoSignal.PRECOMPUTE)) module.precomputeFiles++;
      if (file.signals().contains(RepoSignal.INDEX)) module.indexFiles++;
      if (file.signals().contains(RepoSignal.CACHE)) module.cacheFiles++;
      for (RepoSignal signal :
          List.of(
              RepoSignal.TODO,
              RepoSignal.FIXME,
              RepoSignal.HACK,
              RepoSignal.UNSUPPORTED_OPERATION,
              RepoSignal.RETURN_NULL,
              RepoSignal.EMPTY_METHOD,
              RepoSignal.MISSING_IMPLEMENTATION_MARKER)) {
        if (file.signals().contains(signal)) module.stubSignals++;
      }
    }
    ArrayList<ModuleSummary> result = new ArrayList<>();
    for (MutableModule value : byModule.values()) result.add(value.freeze());
    result.sort(Comparator.comparing(ModuleSummary::module));
    return result;
  }

  private static java.util.Map<RepoFileCategory, List<RepoFileRecord>> indexByCategory(
      List<RepoFileRecord> files) {
    java.util.EnumMap<RepoFileCategory, java.util.ArrayList<RepoFileRecord>> mutable =
        new java.util.EnumMap<>(RepoFileCategory.class);
    for (RepoFileRecord file : files) {
      mutable.computeIfAbsent(file.category(), ignored -> new java.util.ArrayList<>()).add(file);
    }
    java.util.EnumMap<RepoFileCategory, List<RepoFileRecord>> frozen =
        new java.util.EnumMap<>(RepoFileCategory.class);
    mutable.forEach((category, values) -> frozen.put(category, List.copyOf(values)));
    return java.util.Collections.unmodifiableMap(frozen);
  }

  private static java.util.Map<RepoLanguage, List<RepoFileRecord>> indexByLanguage(
      List<RepoFileRecord> files) {
    java.util.EnumMap<RepoLanguage, java.util.ArrayList<RepoFileRecord>> mutable =
        new java.util.EnumMap<>(RepoLanguage.class);
    for (RepoFileRecord file : files) {
      mutable.computeIfAbsent(file.language(), ignored -> new java.util.ArrayList<>()).add(file);
    }
    java.util.EnumMap<RepoLanguage, List<RepoFileRecord>> frozen =
        new java.util.EnumMap<>(RepoLanguage.class);
    mutable.forEach((language, values) -> frozen.put(language, List.copyOf(values)));
    return java.util.Collections.unmodifiableMap(frozen);
  }

  private static List<RepoAction> collisionActions(
      List<RepoOwnerCollisionIndex.Row> ownerRows,
      List<RepoApiCollisionIndex.Row> apiRows) {
    ArrayList<RepoAction> result = new ArrayList<>();
    for (RepoOwnerCollisionIndex.Row row : ownerRows) {
      RepoAction.Severity severity =
          row.explicitBridgeRequired() ? RepoAction.Severity.ERROR : RepoAction.Severity.REVIEW;
      String category =
          row.explicitBridgeRequired() ? "identity-owner-collision" : "type-owner-collision";
      result.add(
          new RepoAction(
              severity,
              category,
              row.paths().getFirst(),
              0,
              row.typeName()
                  + " -> "
                  + row.classification().name()
                  + "; paths="
                  + String.join(",", row.paths())
                  + (row.explicitBridgeRequired()
                      ? "; explicit conversion/bridge required; local IDs are not portable"
                      : "")));
    }
    for (RepoApiCollisionIndex.Row row : apiRows) {
      result.add(
          new RepoAction(
              row.divergent() ? RepoAction.Severity.REVIEW : RepoAction.Severity.INFO,
              row.divergent() ? "api-contract-collision" : "api-duplicate-candidate",
              row.paths().getFirst(),
              0,
              row.owner()
                  + "#"
                  + row.name()
                  + " -> "
                  + row.classification().name()
                  + "; paths="
                  + String.join(",", row.paths())));
    }
    return List.copyOf(result);
  }

  private String computeFingerprint() {
    MessageDigest digest = digest();
    update(digest, "RepoSupersetInventory/v3");
    putInt(digest, files.size());
    for (RepoFileRecord file : files) {
      update(digest, file.path());
      putInt(digest, file.category().code());
      putInt(digest, file.language().code());
      digest.update(HexFormat.of().parseHex(file.sha256()));
      digest.update(HexFormat.of().parseHex(file.structuralSha256()));
      digest.update(HexFormat.of().parseHex(file.logicSha256()));
      putLong(digest, file.simHash64());
    }
    putInt(digest, actions.size());
    for (RepoAction action : actions) {
      update(digest, action.severity().name());
      update(digest, action.category());
      update(digest, action.path());
      putInt(digest, action.line());
      update(digest, action.detail());
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    putInt(digest, bytes.length);
    digest.update(bytes);
  }

  private static void putInt(MessageDigest digest, int value) {
    for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte) (value >>> shift));
  }

  private static void putLong(MessageDigest digest, long value) {
    for (int shift = 56; shift >= 0; shift -= 8) digest.update((byte) (value >>> shift));
  }

  private static final class MutableModule {
    private final String module;
    private int files;
    private int javaFiles;
    private int tests;
    private int apis;
    private int mindexOwners;
    private int jiniFiles;
    private int precomputeFiles;
    private int indexFiles;
    private int cacheFiles;
    private int stubSignals;

    private MutableModule(String module) {
      this.module = module;
    }

    private ModuleSummary freeze() {
      return new ModuleSummary(
          module,
          files,
          javaFiles,
          tests,
          apis,
          mindexOwners,
          jiniFiles,
          precomputeFiles,
          indexFiles,
          cacheFiles,
          stubSignals);
    }
  }
}
