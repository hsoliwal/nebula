// SPDX-License-Identifier: Apache-2.0
package com.synexia.fastsearch.problem;

import java.net.URI;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata-only description of a public programming-problem shape.
 *
 * <p>No problem statement or solution body is stored here.
 */
public record ProblemDescriptor(
        ProblemSource source,
        String externalId,
        String title,
        URI uri,
        Set<ProblemCategory> categories,
        String asymptoticTarget,
        boolean evidenceOnly,
        String licenseNote) {

    public ProblemDescriptor {
        source = Objects.requireNonNull(source, "source");
        externalId = required(externalId, "externalId");
        title = required(title, "title");
        uri = Objects.requireNonNull(uri, "uri");
        source.validateUri(uri);
        categories = categories == null || categories.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(categories));
        asymptoticTarget = Objects.toString(asymptoticTarget, "").strip();
        licenseNote = required(licenseNote, "licenseNote");
        if (source.requiresEvidenceOnly() && !evidenceOnly) {
            throw new IllegalArgumentException(
                    "third-party problem sources remain evidence-only without independent license proof");
        }
    }

    public String canonicalKey() {
        return source.name() + ":" + externalId;
    }

    private static String required(String value, String name) {
        String normalized = Objects.toString(value, "").strip();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name);
        return normalized;
    }
}
