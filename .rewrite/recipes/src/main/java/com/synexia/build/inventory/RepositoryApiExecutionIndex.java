// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit API -> structure -> indexed/precomputed representation -> strategy -> result -> recipe
 * work projection.
 *
 * <p>The index consumes existing immutable repository evidence only. It does not rescan source,
 * prove semantic equivalence, authorize donor copying, or grant source-mutation/promotion
 * authority.</p>
 */
public final class RepositoryApiExecutionIndex {
  public record Row(
      int ordinal,
      String stableId,
      String apiStableId,
      String modulePath,
      String path,
      int line,
      String owner,
      String kind,
      String name,
      String signature,
      String structureOwner,
      String structurePath,
      String representationOwner,
      String representationPath,
      String representationCapabilities,
      boolean indexed,
      boolean precomputed,
      String implementationStrategy,
      RepositoryApiResultContract.Result resultContract,
      RepositoryImplementationQueue.Decision implementationDecision,
      RepositoryApiRecipeRouting.Action recipeAction,
      List<String> recipeCapabilityIds,
      List<String> donorIdentities,
      List<String> donorRepositories,
      List<RepositoryImplementationQueue.Gate> requiredGates,
      String reason) {

    public Row {
      if (ordinal < 0 || line < 1) throw new IllegalArgumentException("ordinal/line");
      stableId = sha(stableId, "stableId");
      apiStableId = sha(apiStableId, "apiStableId");
      modulePath = required(modulePath, "modulePath");
      path = required(path, "path");
      owner = required(owner, "owner");
      kind = required(kind, "kind");
      name = required(name, "name");
      signature = required(signature, "signature");
      structureOwner = required(structureOwner, "structureOwner");
      structurePath = required(structurePath, "structurePath");
      representationOwner = required(representationOwner, "representationOwner");
      representationPath = required(representationPath, "representationPath");
      representationCapabilities =
          required(representationCapabilities, "representationCapabilities");
      implementationStrategy = required(implementationStrategy, "implementationStrategy");
      resultContract = Objects.requireNonNull(resultContract, "resultContract");
      implementationDecision =
          Objects.requireNonNull(implementationDecision, "implementationDecision");
      recipeAction = Objects.requireNonNull(recipeAction, "recipeAction");
      recipeCapabilityIds = stableText(recipeCapabilityIds);
      if (recipeCapabilityIds.isEmpty()) throw new IllegalArgumentException("recipeCapabilityIds");
      donorIdentities = stableText(donorIdentities);
      donorRepositories = stableText(donorRepositories);
      requiredGates = stableGates(requiredGates);
      reason = required(reason, "reason");
    }

    public boolean mutationAuthority() {
      return false;
    }

    public boolean promotionAuthority() {
      return false;
    }
  }

  public record Summary(
      int rows,
      int indexed,
      int precomputed,
      int unresolvedResults,
      Map<RepositoryApiRecipeRouting.Action, Integer> byRecipeAction,
      String rootSha256) {
    public Summary {
      if (rows < 0
          || indexed < 0
          || precomputed < 0
          || unresolvedResults < 0
          || indexed > rows
          || precomputed > rows
          || unresolvedResults > rows) {
        throw new IllegalArgumentException("invalid execution-index counts");
      }
      EnumMap<RepositoryApiRecipeRouting.Action, Integer> stable =
          new EnumMap<>(RepositoryApiRecipeRouting.Action.class);
      stable.putAll(Objects.requireNonNull(byRecipeAction, "byRecipeAction"));
      for (RepositoryApiRecipeRouting.Action action : RepositoryApiRecipeRouting.Action.values()) {
        stable.putIfAbsent(action, 0);
      }
      if (stable.values().stream().anyMatch(value -> value < 0)
          || stable.values().stream().mapToInt(Integer::intValue).sum() != rows) {
        throw new IllegalArgumentException("invalid recipe-action counts");
      }
      byRecipeAction = java.util.Collections.unmodifiableMap(stable);
      rootSha256 = sha(rootSha256, "rootSha256");
    }
  }

  private final List<Row> rows;
  private final Summary summary;

  private RepositoryApiExecutionIndex(List<Row> source) {
    rows = List.copyOf(source);
    EnumMap<RepositoryApiRecipeRouting.Action, Integer> counts =
        new EnumMap<>(RepositoryApiRecipeRouting.Action.class);
    for (RepositoryApiRecipeRouting.Action action : RepositoryApiRecipeRouting.Action.values()) {
      counts.put(action, 0);
    }

    int indexedCount = 0;
    int precomputedCount = 0;
    int unknownResults = 0;
    for (Row row : rows) {
      counts.merge(row.recipeAction(), 1, Math::addExact);
      if (row.indexed()) indexedCount++;
      if (row.precomputed()) precomputedCount++;
      if (row.resultContract().kind() == RepositoryApiResultContract.Kind.UNKNOWN) {
        unknownResults++;
      }
    }
    summary =
        new Summary(
            rows.size(),
            indexedCount,
            precomputedCount,
            unknownResults,
            counts,
            root(rows));
  }

  public static RepositoryApiExecutionIndex build(
      RepoSupersetInventory inventory,
      RepositoryCompetitiveAlgorithmPlan competitivePlan) {
    Objects.requireNonNull(inventory, "inventory");
    Objects.requireNonNull(competitivePlan, "competitivePlan");

    RepositoryImplementationQueue queue =
        RepositoryImplementationQueue.build(inventory, competitivePlan);
    Map<String, RepositoryImplementationQueue.Item> queueByApi = new LinkedHashMap<>();
    for (RepositoryImplementationQueue.Item item : queue.items()) {
      String key = apiKey(item.path(), item.name(), item.signature());
      RepositoryImplementationQueue.Item previous = queueByApi.putIfAbsent(key, item);
      if (previous != null) throw new IllegalArgumentException("duplicate queue API: " + key);
    }

    ArrayList<Pending> pending = new ArrayList<>(inventory.apiCapabilityMap().size());
    for (RepoApiCapabilityMap.Row capability : inventory.apiCapabilityMap()) {
      RepoApiRecord api = capability.api();
      RepositoryImplementationQueue.Item item =
          Objects.requireNonNull(
              queueByApi.get(apiKey(api.path(), api.name(), api.signature())),
              "implementation queue item");
      pending.add(
          new Pending(
              capability,
              item,
              RepositoryApiResultContract.from(api),
              RepositoryApiRecipeRouting.from(item, api)));
    }

    pending.sort(
        Comparator.comparing((Pending value) -> value.capability().api().path())
            .thenComparingInt(value -> value.capability().api().line())
            .thenComparing(value -> value.capability().api().owner())
            .thenComparing(value -> value.capability().api().name())
            .thenComparing(value -> value.capability().api().signature()));

    ArrayList<Row> result = new ArrayList<>(pending.size());
    for (int ordinal = 0; ordinal < pending.size(); ordinal++) {
      Pending value = pending.get(ordinal);
      RepoApiCapabilityMap.Row capability = value.capability();
      RepoApiRecord api = capability.api();
      RepositoryImplementationQueue.Item item = value.item();
      result.add(
          new Row(
              ordinal,
              stableId(capability, item, value.resultContract(), value.routing()),
              item.stableId(),
              item.modulePath(),
              api.path(),
              api.line(),
              api.owner(),
              api.kind(),
              api.name(),
              api.signature(),
              capability.structureOwner(),
              capability.structurePath(),
              capability.representationOwner(),
              capability.representationPath(),
              capability.capabilities(),
              capability.indexed(),
              capability.precomputed(),
              capability.strategy(),
              value.resultContract(),
              item.decision(),
              value.routing().action(),
              value.routing().capabilityIds(),
              item.donorIdentities(),
              item.donorRepositories(),
              item.requiredGates(),
              item.reason()));
    }
    return new RepositoryApiExecutionIndex(result);
  }

  public List<Row> rows() {
    return rows;
  }

  public Summary summary() {
    return summary;
  }

  public String rootSha256() {
    return summary.rootSha256();
  }

  private static String stableId(
      RepoApiCapabilityMap.Row capability,
      RepositoryImplementationQueue.Item item,
      RepositoryApiResultContract.Result result,
      RepositoryApiRecipeRouting.Routing routing) {
    MessageDigest digest = digest();
    update(digest, "REPOSITORY-API-EXECUTION-ROW/1");
    update(digest, item.stableId());
    update(digest, capability.structureOwner());
    update(digest, capability.structurePath());
    update(digest, capability.representationOwner());
    update(digest, capability.representationPath());
    update(digest, capability.capabilities());
    update(digest, capability.strategy());
    update(digest, result.declaredType());
    update(digest, result.kind().name());
    update(digest, routing.action().name());
    routing.capabilityIds().forEach(value -> update(digest, value));
    return HexFormat.of().formatHex(digest.digest());
  }

  private static String root(List<Row> rows) {
    MessageDigest digest = digest();
    update(digest, "REPOSITORY-API-EXECUTION-INDEX/1");
    update(digest, Integer.toString(rows.size()));
    for (Row row : rows) {
      update(digest, row.stableId());
      update(digest, row.apiStableId());
      update(digest, row.modulePath());
      update(digest, row.path());
      update(digest, Integer.toString(row.line()));
      update(digest, row.owner());
      update(digest, row.kind());
      update(digest, row.name());
      update(digest, row.signature());
      update(digest, row.structureOwner());
      update(digest, row.structurePath());
      update(digest, row.representationOwner());
      update(digest, row.representationPath());
      update(digest, row.representationCapabilities());
      update(digest, Boolean.toString(row.indexed()));
      update(digest, Boolean.toString(row.precomputed()));
      update(digest, row.implementationStrategy());
      update(digest, row.resultContract().declaredType());
      update(digest, row.resultContract().kind().name());
      update(digest, row.implementationDecision().name());
      update(digest, row.recipeAction().name());
      row.recipeCapabilityIds().forEach(value -> update(digest, value));
      row.donorIdentities().forEach(value -> update(digest, value));
      row.donorRepositories().forEach(value -> update(digest, value));
      row.requiredGates().forEach(value -> update(digest, value.name()));
      update(digest, row.reason());
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static String apiKey(String path, String name, String signature) {
    return path + "\u0000" + name + "\u0000" + signature;
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
    digest.update((byte) (bytes.length >>> 24));
    digest.update((byte) (bytes.length >>> 16));
    digest.update((byte) (bytes.length >>> 8));
    digest.update((byte) bytes.length);
    digest.update(bytes);
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static List<String> stableText(List<String> values) {
    return Objects.requireNonNullElse(values, List.<String>of()).stream()
        .map(value -> required(value, "listValue"))
        .distinct()
        .sorted()
        .toList();
  }

  private static List<RepositoryImplementationQueue.Gate> stableGates(
      List<RepositoryImplementationQueue.Gate> values) {
    java.util.EnumSet<RepositoryImplementationQueue.Gate> result =
        java.util.EnumSet.noneOf(RepositoryImplementationQueue.Gate.class);
    result.addAll(Objects.requireNonNullElse(values, List.of()));
    return List.copyOf(result);
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
      RepoApiCapabilityMap.Row capability,
      RepositoryImplementationQueue.Item item,
      RepositoryApiResultContract.Result resultContract,
      RepositoryApiRecipeRouting.Routing routing) {}
}
