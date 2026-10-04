// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.Objects;

/** Deterministic mechanical action candidate; not an automatic source mutation. */
public record RepoAction(
    Severity severity,
    String category,
    String path,
    int line,
    String detail) implements Comparable<RepoAction> {

  public enum Severity {
    INFO,
    REVIEW,
    ERROR
  }

  public RepoAction {
    Objects.requireNonNull(severity, "severity");
    category = required(category, "category");
    path = required(path, "path");
    detail = required(detail, "detail");
    if (line < 0) throw new IllegalArgumentException("line");
  }

  @Override
  public int compareTo(RepoAction other) {
    int compared = severity.compareTo(other.severity);
    if (compared != 0) return compared;
    compared = category.compareTo(other.category);
    if (compared != 0) return compared;
    compared = path.compareTo(other.path);
    if (compared != 0) return compared;
    compared = Integer.compare(line, other.line);
    return compared != 0 ? compared : detail.compareTo(other.detail);
  }

  private static String required(String value, String label) {
    String checked = Objects.requireNonNull(value, label).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(label);
    return checked;
  }
}
