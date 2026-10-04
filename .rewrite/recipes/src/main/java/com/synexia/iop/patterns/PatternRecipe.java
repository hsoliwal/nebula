package com.synexia.iop.patterns;

import java.util.List;
import java.util.Objects;

/** Deterministic executable lowering recipe for one pattern descriptor. */
public record PatternRecipe(
        String patternId,
        List<Step> steps,
        List<String> invariants,
        boolean ownerAdmissionRequired,
        String root) {

    public enum StepKind {
        INVENTORY,
        MATCH,
        VALIDATE,
        CONSTRUCT,
        TRANSFORM,
        ROUTE,
        COMPOSE,
        EXECUTE_LOCAL,
        DELEGATE_TO_OWNER,
        VERIFY,
        ADMIT,
        COMPENSATE,
        EMIT_RECEIPT
    }

    public record Step(int ordinal, String id, StepKind kind, String ownerModule, String root) {
        public Step {
            if (ordinal < 0) throw new IllegalArgumentException("step ordinal must be non-negative");
            id = require(id, "id");
            kind = Objects.requireNonNull(kind, "kind");
            ownerModule = require(ownerModule, "ownerModule");
            String expected = PatternHash.sha256("SYNEXIA_PATTERN_STEP_JAVA21_V1|"
                    + ordinal + "|" + id + "|" + kind + "|" + ownerModule);
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("pattern step root mismatch");
        }

        public static Step of(int ordinal, String id, StepKind kind, String ownerModule) {
            return new Step(ordinal, id, kind, ownerModule, "");
        }
    }

    public PatternRecipe {
        patternId = require(patternId, "patternId");
        steps = steps == null ? List.of() : List.copyOf(steps);
        invariants = PatternHash.sorted(invariants);
        if (steps.isEmpty()) throw new IllegalArgumentException("recipe steps required");
        if (invariants.isEmpty()) throw new IllegalArgumentException("recipe invariants required");
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).ordinal() != i) {
                throw new IllegalArgumentException("recipe step ordinals must be contiguous from zero");
            }
        }
        String expected = PatternHash.sha256("SYNEXIA_PATTERN_RECIPE_JAVA21_V1|"
                + patternId + "|"
                + steps.stream().map(Step::root).reduce("", (a, b) -> a + b + "|")
                + "|" + String.join("|", invariants) + "|" + ownerAdmissionRequired);
        if (root == null || root.isBlank()) root = expected;
        if (!root.equals(expected)) throw new IllegalArgumentException("pattern recipe root mismatch");
    }

    private static String require(String value, String name) {
        String normalized = PatternHash.text(value);
        if (normalized.isBlank()) throw new IllegalArgumentException(name + " required");
        return normalized;
    }
}
