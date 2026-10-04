// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic per-API implementation queue derived entirely from canonical inventory evidence.
 *
 * <p>The queue joins API ownership/precompute gaps with exact competitive-donor planner matches.
 * It never grants mutation, donor-copy, semantic-equivalence, or merge authority. One item is the
 * complete review unit for one API declaration.</p>
 */
public final class RepositoryImplementationQueue {
  public enum Decision {
    REUSE_EXISTING,
    ENHANCE_EXISTING,
    IMPLEMENT_MISSING,
    BRIDGE_IDENTITY,
    HOLD_CONTRACT_REVIEW,
    HOLD_RUNTIME_REVIEW
  }

  public enum Gate {
    SOURCE_REVIEW,
    API_CONTRACT,
    COMPATIBILITY,
    COMPILE,
    STATIC_ANALYSIS,
    UNIT_TEST,
    INTEGRATION_TEST,
    DIFFERENTIAL,
    BENCHMARK,
    NATIVE_ABI,
    SERVICE_LIFECYCLE,
    SERIALIZATION_COMPATIBILITY
  }

  public record Item(
      int ordinal,
      String stableId,
      Decision decision,
      String modulePath,
      String path,
      int line,
      String owner,
      String name,
      String signature,
      String structureOwner,
      String structurePath,
      String representationOwner,
      String representationPath,
      String capabilities,
      String implementationStrategy,
      RepoApiCapabilityMap.Confidence confidence,
      RepoApiCapabilityMap.GapAction gapAction,
      String donorAction,
      List<String> donorIdentities,
      List<String> donorShapes,
      List<String> donorKernels,
      List<String> donorRepositories,
      List<String> donorMechanics,
      List<Gate> requiredGates,
      String reason) {

    public Item {
      if (ordinal < 0) throw new IllegalArgumentException("ordinal");
      stableId = sha(stableId, "stableId");
      decision = Objects.requireNonNull(decision, "decision");
      modulePath = required(modulePath, "modulePath");
      path = required(path, "path");
      if (line < 0) throw new IllegalArgumentException("line");
      owner = required(owner, "owner");
      name = required(name, "name");
      signature = required(signature, "signature");
      structureOwner = required(structureOwner, "structureOwner");
      structurePath = required(structurePath, "structurePath");
      representationOwner = required(representationOwner, "representationOwner");
      representationPath = required(representationPath, "representationPath");
      capabilities = required(capabilities, "capabilities");
      implementationStrategy = required(implementationStrategy, "implementationStrategy");
      confidence = Objects.requireNonNull(confidence, "confidence");
      gapAction = Objects.requireNonNull(gapAction, "gapAction");
      donorAction = Objects.requireNonNullElse(donorAction, "").strip();
      donorIdentities = stableText(donorIdentities);
      donorShapes = stableText(donorShapes);
      donorKernels = stableText(donorKernels);
      donorRepositories = stableText(donorRepositories);
      donorMechanics = stableText(donorMechanics);
      requiredGates = stableGates(requiredGates);
      reason = required(reason, "reason");
    }

    public boolean donorBacked() {
      return !donorIdentities.isEmpty();
    }
  }

  public record Summary(
      int items,
      int donorBacked,
      Map<Decision, Integer> byDecision,
      String rootSha256) {
    public Summary {
      if (items < 0 || donorBacked < 0 || donorBacked > items) {
        throw new IllegalArgumentException("invalid queue counts");
      }
      EnumMap<Decision, Integer> stable = new EnumMap<>(Decision.class);
      stable.putAll(Objects.requireNonNull(byDecision, "byDecision"));
      for (Decision decision : Decision.values()) stable.putIfAbsent(decision, 0);
      int total = stable.values().stream().mapToInt(Integer::intValue).sum();
      if (total != items || stable.values().stream().anyMatch(value -> value < 0)) {
        throw new IllegalArgumentException("invalid decision counts");
      }
      byDecision = java.util.Collections.unmodifiableMap(stable);
      rootSha256 = sha(rootSha256, "rootSha256");
    }
  }

  private final List<Item> items;
  private final Summary summary;

  private RepositoryImplementationQueue(List<Item> items) {
    this.items = List.copyOf(items);
    String root = root(this.items);
    EnumMap<Decision, Integer> counts = new EnumMap<>(Decision.class);
    for (Decision decision : Decision.values()) counts.put(decision, 0);
    int donorBacked = 0;
    for (Item item : this.items) {
      counts.merge(item.decision(), 1, Math::addExact);
      if (item.donorBacked()) donorBacked++;
    }
    this.summary = new Summary(this.items.size(), donorBacked, counts, root);
  }

  public static RepositoryImplementationQueue build(
      RepoSupersetInventory inventory,
      RepositoryCompetitiveAlgorithmPlan competitivePlan) {
    Objects.requireNonNull(inventory, "inventory");
    Objects.requireNonNull(competitivePlan, "competitivePlan");

    Map<String, RepositoryCompetitiveAlgorithmPlan.ApiCandidate> donors =
        competitiveByApi(competitivePlan);
    Set<String> bridgePaths = bridgePaths(inventory);
    Set<String> divergentApiKeys = divergentApiKeys(inventory);

    ArrayList<Pending> pending = new ArrayList<>(inventory.apiCapabilityMap().size());
    for (RepoApiCapabilityMap.Row row : inventory.apiCapabilityMap()) {
      RepoApiRecord api = row.api();
      RepositoryCompetitiveAlgorithmPlan.ApiCandidate donor =
          donors.get(apiKey(api.path(), api.name(), api.signature()));
      Decision decision = decision(row, api, bridgePaths, divergentApiKeys);
      pending.add(
          new Pending(
              row,
              donor,
              decision,
              gates(row, decision, donor),
              reason(row, api, decision)));
    }

    pending.sort(
        Comparator.comparing((Pending value) -> value.row().api().path())
            .thenComparingInt(value -> value.row().api().line())
            .thenComparing(value -> value.row().api().owner())
            .thenComparing(value -> value.row().api().name())
            .thenComparing(value -> value.row().api().signature()));

    ArrayList<Item> items = new ArrayList<>(pending.size());
    for (int ordinal = 0; ordinal < pending.size(); ordinal++) {
      Pending value = pending.get(ordinal);
      RepoApiCapabilityMap.Row row = value.row();
      RepoApiRecord api = row.api();
      RepositoryCompetitiveAlgorithmPlan.ApiCandidate donor = value.donor();
      items.add(
          new Item(
              ordinal,
              stableId(row, value.decision()),
              value.decision(),
              row.mavenModule(),
              api.path(),
              api.line(),
              api.owner(),
              api.name(),
              api.signature(),
              row.structureOwner(),
              row.structurePath(),
              row.representationOwner(),
              row.representationPath(),
              row.capabilities(),
              row.strategy(),
              row.confidence(),
              row.gapAction(),
              donor == null ? "" : donor.action().name(),
              donor == null ? List.of() : donor.donorIdentities(),
              donor == null ? List.of() : donor.shapes(),
              donor == null ? List.of() : donor.kernels(),
              donor == null ? List.of() : donor.repositories(),
              donor == null ? List.of() : donor.mechanics(),
              value.gates(),
              value.reason()));
    }
    return new RepositoryImplementationQueue(items);
  }

  public List<Item> items() {
    return items;
  }

  public Summary summary() {
    return summary;
  }

  public String rootSha256() {
    return summary.rootSha256();
  }

  private static Decision decision(
      RepoApiCapabilityMap.Row row,
      RepoApiRecord api,
      Set<String> bridgePaths,
      Set<String> divergentApiKeys) {
    if (bridgePaths.contains(api.path())) return Decision.BRIDGE_IDENTITY;
    if (divergentApiKeys.contains(collisionKey(api))) return Decision.HOLD_CONTRACT_REVIEW;
    return switch (row.gapAction()) {
      case NONE -> Decision.REUSE_EXISTING;
      case ADD_PRECOMPUTE_INDEX, ADD_INDEX -> Decision.ENHANCE_EXISTING;
      case RESOLVE_STRUCTURE_OWNER -> Decision.IMPLEMENT_MISSING;
      case REVIEW_RUNTIME_ONLY -> Decision.HOLD_RUNTIME_REVIEW;
    };
  }

  private static List<Gate> gates(
      RepoApiCapabilityMap.Row row,
      Decision decision,
      RepositoryCompetitiveAlgorithmPlan.ApiCandidate donor) {
    EnumSet<Gate> gates =
        EnumSet.of(
            Gate.SOURCE_REVIEW,
            Gate.API_CONTRACT,
            Gate.COMPATIBILITY,
            Gate.COMPILE,
            Gate.STATIC_ANALYSIS,
            Gate.UNIT_TEST);
    if (decision != Decision.REUSE_EXISTING) gates.add(Gate.INTEGRATION_TEST);
    if (decision == Decision.BRIDGE_IDENTITY
        || decision == Decision.HOLD_CONTRACT_REVIEW
        || donor != null) {
      gates.add(Gate.DIFFERENTIAL);
    }
    if (donor != null) gates.add(Gate.BENCHMARK);
    if (hasCapability(row.capabilities(), RepoSignal.JNI)) gates.add(Gate.NATIVE_ABI);
    if (hasCapability(row.capabilities(), RepoSignal.JINI)) gates.add(Gate.SERVICE_LIFECYCLE);
    if (hasCapability(row.capabilities(), RepoSignal.SERIALIZATION)) {
      gates.add(Gate.SERIALIZATION_COMPATIBILITY);
    }
    return List.copyOf(gates);
  }

  private static String reason(
      RepoApiCapabilityMap.Row row, RepoApiRecord api, Decision decision) {
    return switch (decision) {
      case BRIDGE_IDENTITY ->
          "divergent MIndex/Minex owner identity at " + api.path()
              + "; explicit conversion/bridge required before reuse";
      case HOLD_CONTRACT_REVIEW ->
          "same-owner API name has divergent contracts across repository paths";
      case IMPLEMENT_MISSING ->
          row.unresolvedReason().isBlank()
              ? "required structure owner is unresolved"
              : row.unresolvedReason();
      case ENHANCE_EXISTING ->
          "existing owner requires " + row.gapAction().name().toLowerCase(java.util.Locale.ROOT);
      case HOLD_RUNTIME_REVIEW ->
          "runtime-only representation requires explicit precompute/index cost review";
      case REUSE_EXISTING ->
          "reuse admitted structure/representation owner; preserve the API contract";
    };
  }

  private static Map<String, RepositoryCompetitiveAlgorithmPlan.ApiCandidate> competitiveByApi(
      RepositoryCompetitiveAlgorithmPlan plan) {
    HashMap<String, RepositoryCompetitiveAlgorithmPlan.ApiCandidate> result = new HashMap<>();
    for (RepositoryCompetitiveAlgorithmPlan.ApiCandidate candidate : plan.candidates()) {
      String key = apiKey(candidate.path(), candidate.name(), candidate.signature());
      RepositoryCompetitiveAlgorithmPlan.ApiCandidate previous = result.putIfAbsent(key, candidate);
      if (previous != null) {
        throw new IllegalArgumentException("duplicate competitive API candidate: " + key);
      }
    }
    return Map.copyOf(result);
  }

  private static Set<String> bridgePaths(RepoSupersetInventory inventory) {
    HashSet<String> result = new HashSet<>();
    for (RepoOwnerCollisionIndex.Row collision : inventory.ownerCollisions()) {
      if (collision.explicitBridgeRequired()) result.addAll(collision.paths());
    }
    return Set.copyOf(result);
  }

  private static Set<String> divergentApiKeys(RepoSupersetInventory inventory) {
    HashSet<String> result = new HashSet<>();
    for (RepoApiCollisionIndex.Row collision : inventory.apiCollisions()) {
      if (!collision.divergent()) continue;
      for (String path : collision.paths()) {
        result.add(
            collisionKey(
                path, collision.owner(), collision.kind(), collision.name()));
      }
    }
    return Set.copyOf(result);
  }

  private static String collisionKey(RepoApiRecord api) {
    return collisionKey(api.path(), api.owner(), api.kind(), api.name());
  }

  private static String collisionKey(String path, String owner, String kind, String name) {
    return path + "\u0000" + owner + "\u0000" + kind + "\u0000" + name;
  }

  private static String apiKey(String path, String name, String signature) {
    return path + "\u0000" + name + "\u0000" + signature;
  }

  private static boolean hasCapability(String capabilities, RepoSignal signal) {
    String wanted = signal.name();
    for (String value : capabilities.split(",")) {
      if (value.equals(wanted)) return true;
    }
    return false;
  }

  private static String stableId(RepoApiCapabilityMap.Row row, Decision decision) {
    MessageDigest digest = sha256();
    update(digest, "REPOSITORY-IMPLEMENTATION-QUEUE-ITEM/1");
    RepoApiRecord api = row.api();
    update(digest, api.path());
    update(digest, Integer.toString(api.line()));
    update(digest, api.owner());
    update(digest, api.kind());
    update(digest, api.name());
    update(digest, api.signature());
    update(digest, row.mavenModule());
    update(digest, decision.name());
    return HexFormat.of().formatHex(digest.digest());
  }

  private static String root(List<Item> items) {
    MessageDigest digest = sha256();
    update(digest, "REPOSITORY-IMPLEMENTATION-QUEUE/1");
    update(digest, Integer.toString(items.size()));
    for (Item item : items) {
      update(digest, item.stableId());
      update(digest, item.decision().name());
      update(digest, item.modulePath());
      update(digest, item.path());
      update(digest, Integer.toString(item.line()));
      update(digest, item.owner());
      update(digest, item.name());
      update(digest, item.signature());
      update(digest, item.structureOwner());
      update(digest, item.structurePath());
      update(digest, item.representationOwner());
      update(digest, item.representationPath());
      update(digest, item.capabilities());
      update(digest, item.implementationStrategy());
      update(digest, item.confidence().name());
      update(digest, item.gapAction().name());
      update(digest, item.donorAction());
      item.donorIdentities().forEach(value -> update(digest, value));
      item.donorShapes().forEach(value -> update(digest, value));
      item.donorKernels().forEach(value -> update(digest, value));
      item.donorRepositories().forEach(value -> update(digest, value));
      item.donorMechanics().forEach(value -> update(digest, value));
      item.requiredGates().forEach(value -> update(digest, value.name()));
      update(digest, item.reason());
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
    digest.update((byte) (bytes.length >>> 24));
    digest.update((byte) (bytes.length >>> 16));
    digest.update((byte) (bytes.length >>> 8));
    digest.update((byte) bytes.length);
    digest.update(bytes);
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static List<String> stableText(List<String> source) {
    return Objects.requireNonNullElse(source, List.<String>of()).stream()
        .map(value -> required(value, "listValue"))
        .distinct()
        .sorted()
        .toList();
  }

  private static List<Gate> stableGates(List<Gate> source) {
    EnumSet<Gate> gates = EnumSet.noneOf(Gate.class);
    gates.addAll(Objects.requireNonNullElse(source, List.of()));
    return List.copyOf(gates);
  }

  private static String required(String value, String field) {
    String checked = Objects.toString(value, "").strip();
    if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }

  private static String sha(String value, String field) {
    String checked = required(value, field).toLowerCase(java.util.Locale.ROOT);
    if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
    return checked;
  }

  private record Pending(
      RepoApiCapabilityMap.Row row,
      RepositoryCompetitiveAlgorithmPlan.ApiCandidate donor,
      Decision decision,
      List<Gate> gates,
      String reason) {}
}
