// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Precomputed repository-wide collision index for same-owner/same-name Java APIs.
 *
 * <p>This index identifies review candidates only. A matching owner and method name does not prove
 * semantic equivalence, and divergent signatures are never automatically consolidated.</p>
 */
public final class RepoApiCollisionIndex {

  public enum Classification {
    EXACT_API_DUPLICATE,
    DIVERGENT_API_CONTRACT
  }

  public record Row(
      String owner,
      String kind,
      String name,
      Classification classification,
      List<String> paths,
      List<String> signatures) {

    public Row {
      owner = required(owner, "owner");
      kind = required(kind, "kind");
      name = required(name, "name");
      Objects.requireNonNull(classification, "classification");
      paths = nonEmpty(paths, "paths");
      signatures = nonEmpty(signatures, "signatures");
      if (paths.size() < 2) throw new IllegalArgumentException("API collision requires >= 2 paths");
    }

    public boolean divergent() {
      return classification == Classification.DIVERGENT_API_CONTRACT;
    }
  }

  private record Key(String owner, String kind, String name) implements Comparable<Key> {
    private Key {
      owner = required(owner, "owner");
      kind = required(kind, "kind");
      name = required(name, "name");
    }

    @Override
    public int compareTo(Key other) {
      int compared = owner.compareTo(other.owner);
      if (compared != 0) return compared;
      compared = kind.compareTo(other.kind);
      return compared != 0 ? compared : name.compareTo(other.name);
    }
  }

  private RepoApiCollisionIndex() {
    throw new AssertionError("No instances");
  }

  /** Builds collision rows from the already extracted API declarations. */
  public static List<Row> build(List<RepoApiRecord> apis) {
    Objects.requireNonNull(apis, "apis");
    TreeMap<Key, ArrayList<RepoApiRecord>> grouped = new TreeMap<>();
    for (RepoApiRecord api : apis) {
      grouped.computeIfAbsent(new Key(api.owner(), api.kind(), api.name()), ignored -> new ArrayList<>())
          .add(api);
    }

    ArrayList<Row> rows = new ArrayList<>();
    for (Map.Entry<Key, ArrayList<RepoApiRecord>> entry : grouped.entrySet()) {
      TreeSet<String> paths = new TreeSet<>();
      TreeSet<String> signatures = new TreeSet<>();
      for (RepoApiRecord api : entry.getValue()) {
        paths.add(api.path());
        signatures.add(normalize(api.signature()));
      }
      if (paths.size() < 2) continue;
      Key key = entry.getKey();
      rows.add(
          new Row(
              key.owner(),
              key.kind(),
              key.name(),
              signatures.size() == 1
                  ? Classification.EXACT_API_DUPLICATE
                  : Classification.DIVERGENT_API_CONTRACT,
              List.copyOf(paths),
              List.copyOf(signatures)));
    }

    rows.sort(
        Comparator.comparing((Row row) -> row.classification().ordinal())
            .thenComparing(Row::owner)
            .thenComparing(Row::kind)
            .thenComparing(Row::name));
    return List.copyOf(rows);
  }

  private static String normalize(String signature) {
    return required(signature, "signature").replaceAll("\\s+", " ").strip();
  }

  private static String required(String value, String field) {
    String checked = Objects.requireNonNull(value, field).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(field);
    return checked;
  }

  private static List<String> nonEmpty(List<String> values, String field) {
    List<String> copy = List.copyOf(Objects.requireNonNull(values, field));
    if (copy.isEmpty()) throw new IllegalArgumentException(field);
    return copy;
  }
}
