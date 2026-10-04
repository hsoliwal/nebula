// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/** Immutable canonical gate set derived without rewriting the source work-order format. */
public record M3RecipeGateSet(String sourceKind, List<M3RecipeGate> gates) {
    public M3RecipeGateSet {
        sourceKind = token(sourceKind);
        EnumSet<M3RecipeGate> ordered = EnumSet.noneOf(M3RecipeGate.class);
        ordered.addAll(Objects.requireNonNull(gates, "gates"));
        gates = List.copyOf(ordered);
    }

    public static M3RecipeGateSet fromProblem(
            M3ProblemRecipeWorkOrder.WorkOrder workOrder) {
        Objects.requireNonNull(workOrder, "workOrder");
        return fromStrings("PROBLEM", workOrder.requiredGates());
    }

    public static M3RecipeGateSet fromRepository(
            M3RepositoryRecipeWorkOrder.WorkOrder workOrder) {
        Objects.requireNonNull(workOrder, "workOrder");
        return fromStrings("REPOSITORY", workOrder.requiredGates());
    }

    public static M3RecipeGateSet fromStrings(String sourceKind, List<String> gates) {
        return new M3RecipeGateSet(
                sourceKind,
                Objects.requireNonNull(gates, "gates").stream()
                        .map(M3RecipeGate::parse)
                        .toList());
    }

    public boolean requires(M3RecipeGate gate) {
        return gates.contains(Objects.requireNonNull(gate, "gate"));
    }

    private static String token(String value) {
        String checked = Objects.requireNonNull(value, "sourceKind").strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceKind");
        }
        return checked;
    }
}
