// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Precomputed repository-wide index of Java type-name ownership collisions.
 *
 * <p>A shared simple type name is only a mechanical collision candidate. Exact source identity is
 * distinguished from divergent implementations, and MIndex/Minex owners are explicitly fenced:
 * local numeric identity from two divergent owners is never treated as interchangeable.</p>
 */
public final class RepoOwnerCollisionIndex {

  public enum Classification {
    EXACT_DUPLICATE_OWNER,
    STRUCTURAL_VARIANT_OWNER,
    DIVERGENT_OWNER,
    DIVERGENT_MINDEX_OWNER
  }

  public record Row(
      String typeName,
      Classification classification,
      boolean explicitBridgeRequired,
      List<String> paths,
      List<String> modules,
      List<String> exactSha256,
      List<String> structuralSha256,
      List<String> logicSha256) {

    public Row {
      typeName = required(typeName, "typeName");
      Objects.requireNonNull(classification, "classification");
      paths = immutableNonEmpty(paths, "paths");
      modules = immutableNonEmpty(modules, "modules");
      exactSha256 = immutableNonEmpty(exactSha256, "exactSha256");
      structuralSha256 = immutableNonEmpty(structuralSha256, "structuralSha256");
      logicSha256 = immutableNonEmpty(logicSha256, "logicSha256");
      if (paths.size() < 2) throw new IllegalArgumentException("owner collision requires >= 2 paths");
      if (classification == Classification.DIVERGENT_MINDEX_OWNER && !explicitBridgeRequired) {
        throw new IllegalArgumentException("divergent MIndex owner must require explicit bridge");
      }
    }

    public boolean exactDuplicate() {
      return classification == Classification.EXACT_DUPLICATE_OWNER;
    }

    public boolean divergent() {
      return classification != Classification.EXACT_DUPLICATE_OWNER;
    }
  }

  private RepoOwnerCollisionIndex() {
    throw new AssertionError("No instances");
  }

  /** Builds the immutable collision index from already-scanned file records without rescanning source. */
  public static List<Row> build(List<RepoFileRecord> files) {
    Objects.requireNonNull(files, "files");
    TreeMap<String, ArrayList<RepoFileRecord>> byType = new TreeMap<>();

    for (RepoFileRecord file : files) {
      if (!".java".equals(file.extension())) continue;
      for (String typeName : file.typeNames()) {
        byType.computeIfAbsent(typeName, ignored -> new ArrayList<>()).add(file);
      }
    }

    ArrayList<Row> rows = new ArrayList<>();
    byType.forEach(
        (typeName, owners) -> {
          TreeSet<String> uniquePaths = new TreeSet<>();
          owners.forEach(owner -> uniquePaths.add(owner.path()));
          if (uniquePaths.size() < 2) return;

          owners.sort(Comparator.comparing(RepoFileRecord::path));
          TreeSet<String> modules = new TreeSet<>();
          TreeSet<String> exact = new TreeSet<>();
          TreeSet<String> structural = new TreeSet<>();
          TreeSet<String> logic = new TreeSet<>();
          boolean mindex = false;
          for (RepoFileRecord owner : owners) {
            modules.add(owner.module());
            exact.add(owner.sha256());
            structural.add(owner.structuralSha256());
            logic.add(owner.logicSha256());
            mindex |= owner.signals().contains(RepoSignal.MINDEX_OWNER)
                || owner.signals().contains(RepoSignal.MINEX_OWNER);
          }

          Classification classification;
          if (exact.size() == 1) {
            classification = Classification.EXACT_DUPLICATE_OWNER;
          } else if (mindex) {
            classification = Classification.DIVERGENT_MINDEX_OWNER;
          } else if (structural.size() == 1 || logic.size() == 1) {
            classification = Classification.STRUCTURAL_VARIANT_OWNER;
          } else {
            classification = Classification.DIVERGENT_OWNER;
          }

          rows.add(
              new Row(
                  typeName,
                  classification,
                  classification == Classification.DIVERGENT_MINDEX_OWNER,
                  List.copyOf(uniquePaths),
                  List.copyOf(modules),
                  List.copyOf(exact),
                  List.copyOf(structural),
                  List.copyOf(logic)));
        });

    rows.sort(
        Comparator.comparing((Row row) -> row.classification().ordinal())
            .thenComparing(Row::typeName)
            .thenComparing(row -> row.paths().getFirst()));
    return List.copyOf(rows);
  }

  private static String required(String value, String field) {
    String checked = Objects.requireNonNull(value, field).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(field);
    return checked;
  }

  private static List<String> immutableNonEmpty(List<String> values, String field) {
    List<String> copy = List.copyOf(Objects.requireNonNull(values, field));
    if (copy.isEmpty()) throw new IllegalArgumentException(field);
    LinkedHashSet<String> distinct = new LinkedHashSet<>(copy);
    if (distinct.size() != copy.size()) throw new IllegalArgumentException(field + " contains duplicates");
    return copy;
  }
}
