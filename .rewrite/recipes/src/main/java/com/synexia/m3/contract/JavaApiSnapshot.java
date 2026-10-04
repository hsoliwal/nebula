package com.synexia.m3.contract;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Stable sorted source API snapshot. */
public record JavaApiSnapshot(List<ApiMember> members) {
    public JavaApiSnapshot {
        members = List.copyOf(Objects.requireNonNull(members, "members")).stream()
                .sorted(Comparator
                        .comparing(ApiMember::sourcePath)
                        .thenComparing(ApiMember::owner)
                        .thenComparing(ApiMember::kind)
                        .thenComparing(ApiMember::signature)
                        .thenComparing(ApiMember::visibility))
                .toList();
    }
}
