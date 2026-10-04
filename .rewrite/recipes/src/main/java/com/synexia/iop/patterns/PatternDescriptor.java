package com.synexia.iop.patterns;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable canonical descriptor for one pattern. */
public record PatternDescriptor(
        String id,
        Family family,
        String name,
        String category,
        String description,
        String targetModule,
        Realization realization,
        Authority authority,
        List<String> tags,
        String root) {

    public enum Family {
        GOF,
        EIP,
        DAG,
        DISTRIBUTED,
        MICROSERVICE,
        SPRING,
        AOP_ADVICE,
        SYNEXIA
    }

    public enum Realization {
        JAVA_PRIMITIVE,
        MODULE_RUNTIME,
        FRAMEWORK_ADAPTER,
        CONTRACT_RECIPE
    }

    public enum Authority {
        PURE_LOCAL,
        CONTROL_ONLY,
        CANDIDATE_ONLY,
        LOCAL_ADMISSION_REQUIRED,
        EXTERNAL_EFFECT
    }

    public PatternDescriptor {
        family = Objects.requireNonNull(family, "family");
        name = require(name, "name");
        category = require(category, "category");
        description = require(description, "description");
        targetModule = require(targetModule, "targetModule");
        realization = Objects.requireNonNull(realization, "realization");
        authority = Objects.requireNonNull(authority, "authority");
        tags = PatternHash.sorted(tags);

        String expectedId = family.name().toLowerCase(Locale.ROOT) + ":" + normalizeId(name);
        if (id == null || id.isBlank()) id = expectedId;
        if (!id.equals(expectedId)) throw new IllegalArgumentException("pattern id mismatch");

        String expectedRoot = PatternHash.sha256("SYNEXIA_PATTERN_DESCRIPTOR_JAVA21_V1|"
                + id + "|" + family + "|" + name + "|" + category + "|" + description + "|"
                + targetModule + "|" + realization + "|" + authority + "|" + String.join("|", tags));
        if (root == null || root.isBlank()) root = expectedRoot;
        if (!root.equals(expectedRoot)) throw new IllegalArgumentException("pattern descriptor root mismatch");
    }

    public static PatternDescriptor of(
            Family family,
            String name,
            String category,
            String description,
            String targetModule,
            Realization realization,
            Authority authority,
            List<String> tags) {
        return new PatternDescriptor(
                "",
                family,
                name,
                category,
                description,
                targetModule,
                realization,
                authority,
                tags,
                "");
    }

    private static String normalizeId(String value) {
        String normalized = PatternHash.text(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) throw new IllegalArgumentException("pattern name normalizes to blank");
        return normalized;
    }

    private static String require(String value, String name) {
        String normalized = PatternHash.text(value);
        if (normalized.isBlank()) throw new IllegalArgumentException(name + " required");
        return normalized;
    }
}
