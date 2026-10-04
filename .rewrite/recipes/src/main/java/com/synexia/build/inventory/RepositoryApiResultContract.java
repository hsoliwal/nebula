// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Conservative declared-result projection for repository API inventory rows. */
public final class RepositoryApiResultContract {
  public enum Kind {
    CONSTRUCTOR_INSTANCE,
    VOID,
    PRIMITIVE,
    ARRAY,
    OPTIONAL,
    COLLECTION,
    STREAM,
    ASYNC,
    REFERENCE,
    UNKNOWN
  }

  public record Result(String declaredType, Kind kind, String evidence) {
    public Result {
      declaredType = required(declaredType, "declaredType");
      kind = Objects.requireNonNull(kind, "kind");
      evidence = required(evidence, "evidence");
    }
  }

  private static final Set<String> PRIMITIVES =
      Set.of("boolean", "byte", "short", "int", "long", "float", "double", "char");

  private RepositoryApiResultContract() {}

  public static Result from(RepoApiRecord api) {
    Objects.requireNonNull(api, "api");
    if ("constructor".equalsIgnoreCase(api.kind())) {
      return new Result(api.owner(), Kind.CONSTRUCTOR_INSTANCE, api.signature());
    }

    String declaredType = declaredReturnType(api.signature(), api.name());
    if ("(unknown)".equals(declaredType)) {
      return new Result(declaredType, Kind.UNKNOWN, api.signature());
    }
    return new Result(declaredType, kind(declaredType), api.signature());
  }

  static String declaredReturnType(String signature, String name) {
    String text = Objects.toString(signature, "").replaceAll("\\s+", " ").strip();
    int call = text.indexOf(name + "(");
    if (call < 0) return "(unknown)";
    String prefix = text.substring(0, call).strip();
    if (prefix.isEmpty()) return "(unknown)";

    prefix = prefix.replaceFirst("^(public|protected)\\s+", "");
    boolean changed;
    do {
      changed = false;
      for (String modifier :
          List.of(
              "static",
              "final",
              "abstract",
              "synchronized",
              "native",
              "default",
              "strictfp")) {
        String token = modifier + " ";
        if (prefix.startsWith(token)) {
          prefix = prefix.substring(token.length()).strip();
          changed = true;
          break;
        }
      }
    } while (changed);

    if (prefix.startsWith("<")) {
      int end = genericPrefixEnd(prefix);
      if (end > 0 && end < prefix.length()) prefix = prefix.substring(end).strip();
    }
    return prefix.isBlank() ? "(unknown)" : prefix;
  }

  static Kind kind(String declaredType) {
    String value = required(declaredType, "declaredType");
    String lower = value.toLowerCase(Locale.ROOT);
    if ("void".equals(lower)) return Kind.VOID;
    if (PRIMITIVES.contains(lower)) return Kind.PRIMITIVE;
    if (value.endsWith("[]")) return Kind.ARRAY;
    if (containsType(lower, "optional")) return Kind.OPTIONAL;
    if (containsType(lower, "stream")) return Kind.STREAM;
    if (containsAny(lower, "future", "completionstage", "completablefuture", "promise")) {
      return Kind.ASYNC;
    }
    if (containsAny(
        lower,
        "list",
        "set",
        "collection",
        "iterable",
        "iterator",
        "map",
        "queue",
        "deque")) {
      return Kind.COLLECTION;
    }
    return Kind.REFERENCE;
  }

  private static int genericPrefixEnd(String value) {
    int depth = 0;
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current == '<') depth++;
      else if (current == '>') {
        depth--;
        if (depth == 0) {
          int next = index + 1;
          while (next < value.length() && Character.isWhitespace(value.charAt(next))) next++;
          return next;
        }
      }
    }
    return -1;
  }

  private static boolean containsType(String value, String needle) {
    return value.equals(needle)
        || value.endsWith("." + needle)
        || value.contains("<" + needle)
        || value.contains(needle + "<");
  }

  private static boolean containsAny(String value, String... needles) {
    for (String needle : needles) if (value.contains(needle)) return true;
    return false;
  }

  private static String required(String value, String field) {
    String checked = Objects.toString(value, "").strip();
    if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(field);
    }
    return checked;
  }
}
