// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.review;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.eclipse.nebula.m3.NebulaM3FastSearchReviewRecipe;
import org.eclipse.nebula.m3.NebulaM3InventoryRecipe;
import org.eclipse.nebula.m3.NebulaM3JavaBeforeJniReviewRecipe;
import org.openrewrite.Recipe;

/** Canonical read-only repository review DAG for Nebula M3 evidence. */
public final class NebulaM3RepositoryReviewRecipeDag {
    public enum Phase {
        INVENTORY,
        CHALLENGE_DONOR_REVIEW,
        JAVA_BEFORE_JNI_REVIEW
    }

    public record Atom(String id, Phase phase, Recipe recipe) {
        public Atom {
            id = Objects.requireNonNull(id, "id").strip();
            phase = Objects.requireNonNull(phase, "phase");
            recipe = Objects.requireNonNull(recipe, "recipe");
            if (id.isEmpty()
                    || recipe.maxCycles() != 1
                    || !recipe.getTags().contains("read-only")) {
                throw new IllegalArgumentException(
                        "Nebula repository review atom must be one-cycle and read-only");
            }
        }
    }

    private static final List<Atom> ATOMS =
            validate(
                    List.of(
                            new Atom(
                                    "repository-inventory",
                                    Phase.INVENTORY,
                                    new NebulaM3InventoryRecipe()),
                            new Atom(
                                    "fast-search-challenge-donor-review",
                                    Phase.CHALLENGE_DONOR_REVIEW,
                                    new NebulaM3FastSearchReviewRecipe()),
                            new Atom(
                                    "java-before-jni-review",
                                    Phase.JAVA_BEFORE_JNI_REVIEW,
                                    new NebulaM3JavaBeforeJniReviewRecipe())));

    private NebulaM3RepositoryReviewRecipeDag() {
        throw new AssertionError("No instances");
    }

    public static List<Atom> atoms() {
        return ATOMS;
    }

    private static List<Atom> validate(List<Atom> input) {
        List<Atom> atoms = List.copyOf(Objects.requireNonNull(input, "input"));
        HashSet<String> ids = new HashSet<>();
        int prior = -1;
        ArrayList<Atom> checked = new ArrayList<>(atoms.size());
        for (Atom atom : atoms) {
            if (!ids.add(atom.id())) {
                throw new IllegalArgumentException("duplicate review atom: " + atom.id());
            }
            if (atom.phase().ordinal() < prior) {
                throw new IllegalArgumentException("repository review phase order regression");
            }
            prior = atom.phase().ordinal();
            checked.add(atom);
        }
        return List.copyOf(checked);
    }
}
