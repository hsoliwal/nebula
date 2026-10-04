// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.Objects;

/** One lexically discovered Java API declaration. */
public record RepoApiRecord(
    String path,
    String owner,
    String visibility,
    String kind,
    String name,
    String signature,
    int line) {

  public RepoApiRecord {
    path = required(path, "path");
    owner = required(owner, "owner");
    visibility = required(visibility, "visibility");
    kind = required(kind, "kind");
    name = required(name, "name");
    signature = required(signature, "signature");
    if (line < 1) throw new IllegalArgumentException("line");
  }

  private static String required(String value, String label) {
    String checked = Objects.requireNonNull(value, label).strip();
    if (checked.isEmpty()) throw new IllegalArgumentException(label);
    return checked;
  }
}
