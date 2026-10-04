// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

/** Stable recipe routing projection derived from implementation-queue evidence. */
public final class RepositoryApiRecipeRouting {
  public enum Action {
    REUSE_OR_DRY_RUN,
    CREATE_OR_IMPROVE,
    BRIDGE_REQUIRED,
    HOLD_CONTRACT_REVIEW,
    HOLD_RUNTIME_REVIEW
  }

  public record Routing(Action action, List<String> capabilityIds) {
    public Routing {
      action = Objects.requireNonNull(action, "action");
      capabilityIds =
          Objects.requireNonNullElse(capabilityIds, List.<String>of()).stream()
              .map(value -> required(value, "capabilityId"))
              .distinct()
              .sorted()
              .toList();
      if (capabilityIds.isEmpty()) throw new IllegalArgumentException("capabilityIds");
    }
  }

  private RepositoryApiRecipeRouting() {}

  public static Routing from(
      RepositoryImplementationQueue.Item item, RepoApiRecord api) {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(api, "api");

    TreeSet<String> capabilities = new TreeSet<>();
    for (String shape : item.donorShapes()) {
      capabilities.add("algorithm.shape." + normalize(shape));
    }
    capabilities.add(
        "repository.api."
            + normalize(item.modulePath())
            + "."
            + normalize(api.owner())
            + "."
            + normalize(api.name()));
    return new Routing(action(item.decision()), List.copyOf(capabilities));
  }

  static Action action(RepositoryImplementationQueue.Decision decision) {
    return switch (Objects.requireNonNull(decision, "decision")) {
      case REUSE_EXISTING -> Action.REUSE_OR_DRY_RUN;
      case ENHANCE_EXISTING, IMPLEMENT_MISSING -> Action.CREATE_OR_IMPROVE;
      case BRIDGE_IDENTITY -> Action.BRIDGE_REQUIRED;
      case HOLD_CONTRACT_REVIEW -> Action.HOLD_CONTRACT_REVIEW;
      case HOLD_RUNTIME_REVIEW -> Action.HOLD_RUNTIME_REVIEW;
    };
  }

  static String normalize(String value) {
    String normalized =
        Objects.toString(value, "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    return normalized.isBlank() ? "unknown" : normalized;
  }

  private static String required(String value, String field) {
    String checked = Objects.toString(value, "").strip();
    if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }
}
