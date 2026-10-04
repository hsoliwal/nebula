// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic action plan derived from one repository inventory snapshot.
 *
 * <p>The plan is mechanical evidence, not automatic rewrite or merge authority.</p>
 */
public record RepositorySupersetPlan(
    List<Task> tasks,
    List<ApiStructureMap> apiMappings,
    Summary summary) {

  public enum Stage {
    API_COVERAGE,
    STRUCTURAL_SUPERSET,
    DATA_PRECOMPUTE,
    DESIGN_ABSTRACTION,
    CONSISTENCY,
    VERIFICATION
  }

  public enum Action {
    REUSE,
    ENHANCE,
    IMPLEMENT,
    CONSOLIDATE,
    VERIFY
  }

  public record Task(
      String id,
      Stage stage,
      Action action,
      String modulePath,
      String path,
      int line,
      String capability,
      String evidence,
      List<String> ownerCandidates) {

    public Task {
      id = required(id, "id");
      stage = Objects.requireNonNull(stage, "stage");
      action = Objects.requireNonNull(action, "action");
      modulePath = required(modulePath, "modulePath");
      path = required(path, "path");
      if (line < 0) throw new IllegalArgumentException("line");
      capability = required(capability, "capability");
      evidence = value(evidence);
      ownerCandidates = List.copyOf(Objects.requireNonNullElse(ownerCandidates, List.of()));
    }
  }

  public record ApiStructureMap(
      String path,
      String modulePath,
      String kind,
      String name,
      String signature,
      List<String> requiredStructures,
      List<String> existingRoles,
      String implementationStrategy,
      List<String> ownerCandidates) {

    public ApiStructureMap {
      path = required(path, "path");
      modulePath = required(modulePath, "modulePath");
      kind = required(kind, "kind");
      name = value(name);
      signature = value(signature);
      requiredStructures = List.copyOf(Objects.requireNonNullElse(requiredStructures, List.of()));
      existingRoles = List.copyOf(Objects.requireNonNullElse(existingRoles, List.of()));
      implementationStrategy = required(implementationStrategy, "implementationStrategy");
      ownerCandidates = List.copyOf(Objects.requireNonNullElse(ownerCandidates, List.of()));
    }
  }

  public record Summary(
      int tasks,
      int apiMappings,
      int reuse,
      int enhance,
      int implement,
      int consolidate,
      int verify,
      int apiCoverage,
      int structuralSuperset,
      int dataPrecompute,
      int designAbstraction,
      int consistency,
      int verification) {

    public Summary {
      if (tasks < 0 || apiMappings < 0 || reuse < 0 || enhance < 0 || implement < 0
          || consolidate < 0 || verify < 0 || apiCoverage < 0 || structuralSuperset < 0
          || dataPrecompute < 0 || designAbstraction < 0 || consistency < 0 || verification < 0) {
        throw new IllegalArgumentException("negative plan summary");
      }
    }
  }

  public RepositorySupersetPlan {
    tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
    apiMappings = List.copyOf(Objects.requireNonNull(apiMappings, "apiMappings"));
    summary = Objects.requireNonNull(summary, "summary");
  }

  public String rootSha256() {
    return RepositorySupersetPlanWriter.rootSha256(this);
  }

  private static String required(String value, String field) {
    String normalized = value(value).strip();
    if (normalized.isEmpty()) throw new IllegalArgumentException(field);
    return normalized;
  }

  private static String value(String value) {
    return Objects.toString(value, "");
  }
}
