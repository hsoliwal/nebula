// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.openrewrite.Recipe;

/**
 * Immutable authority binding for class-backed OpenRewrite work in the sole M3/IOP mutation lane.
 *
 * <p>This does not mutate source and does not promote state. It proves that a recipe candidate is
 * mechanically subordinate to the canonical IOP source fence, exact class-hook catalogue,
 * candidate-only execution, and serial FILE-to-UNIVERSE M3 promotion.</p>
 */
public final class M3IopRecipeMutationFence {
    private static final String DOMAIN = "M3_IOP_RECIPE_MUTATION_FENCE_V1";

    public enum Role {
        CANONICAL_PIPELINE,
        TASK_OPERATOR
    }

    public record Receipt(
            Role role,
            String recipeClassName,
            String sourceFilePattern,
            List<String> requiredClassHookIds,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            boolean fullScaleProofRequired,
            boolean targetFileEditAuthority,
            boolean promotionAuthority,
            String root) {

        public Receipt {
            role = Objects.requireNonNull(role, "role");
            recipeClassName = text(recipeClassName, "recipeClassName");
            sourceFilePattern = text(sourceFilePattern, "sourceFilePattern");
            requiredClassHookIds = stable(requiredClassHookIds);
            List<String> canonicalHooks = stable(M3IopPatternClassHooks.hookIds());
            if (!requiredClassHookIds.equals(canonicalHooks)) {
                throw new IllegalArgumentException("M3/IOP recipe authority requires exact canonical class hooks");
            }
            if (!candidateOnly || !serialPromotionRequired || !fullScaleProofRequired) {
                throw new IllegalArgumentException(
                        "M3/IOP recipe authority must remain candidate-only, serial, and full-scale proof gated");
            }
            if (targetFileEditAuthority || promotionAuthority) {
                throw new IllegalArgumentException(
                        "OpenRewrite recipe authority cannot edit canonical source or promote state");
            }
            String expected = digest(
                    DOMAIN,
                    role.name(),
                    recipeClassName,
                    sourceFilePattern,
                    String.join("\u001f", requiredClassHookIds),
                    Boolean.toString(candidateOnly),
                    Boolean.toString(serialPromotionRequired),
                    Boolean.toString(fullScaleProofRequired),
                    Boolean.toString(targetFileEditAuthority),
                    Boolean.toString(promotionAuthority));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("M3/IOP recipe authority root mismatch");
            }
        }
    }

    private M3IopRecipeMutationFence() {}

    /** Bind the one named class-backed canonical M3/IOP mutation pipeline. */
    public static Receipt canonicalPipeline(
            M3IopPatternOnlySerialMechanicalJavaRecipe recipe,
            String sourceFilePattern) {
        Objects.requireNonNull(recipe, "recipe");
        String pattern = text(sourceFilePattern, "sourceFilePattern");
        List<String> actual = recipe.getRecipeList().stream().map(Recipe::getName).toList();
        List<String> expected = M3IopFullScaleMutationAuthority.canonicalPasses(pattern)
                .stream().map(Recipe::getName).toList();
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("canonical IOP recipe pass plan drift");
        }
        if (!recipe.getClass().getName().equals(M3IopFullScaleMutationAuthority.canonicalRecipeClassName())) {
            throw new IllegalArgumentException("recipe is not the canonical M3/IOP mutation pipeline");
        }
        return receipt(Role.CANONICAL_PIPELINE, recipe.getClass().getName(), pattern);
    }

    /** Bind one task-local class-backed leaf after the operator writer wraps it in IOP hooks. */
    public static Receipt taskOperator(M3IopOperatorRecipeWriter.WrittenOperator written) {
        Objects.requireNonNull(written, "written");
        if (!(written.pass().recipe() instanceof M3IopPatternScopedRecipe scoped)) {
            throw new IllegalArgumentException("task operator is not IOP pattern scoped");
        }
        Recipe delegate = scoped.delegate();
        if (!delegate.getRecipeList().isEmpty()
                || delegate.maxCycles() != 1
                || delegate.causesAnotherCycle()) {
            throw new IllegalArgumentException("task operator delegate is not a bounded class-backed leaf");
        }
        List<String> required = stable(written.receipt().requiredClassHookIds());
        if (!required.equals(stable(M3IopPatternClassHooks.hookIds()))) {
            throw new IllegalArgumentException("task operator class hooks drifted from canonical IOP hooks");
        }
        if (!written.receipt().candidateOnly()
                || !written.receipt().serialPromotionRequired()
                || written.receipt().targetFileEditAuthority()
                || written.receipt().promotionAuthority()) {
            throw new IllegalArgumentException("task operator escaped candidate-only serial M3 authority");
        }
        if (!scoped.sourceFilePattern().equals(written.receipt().sourceFilePattern())) {
            throw new IllegalArgumentException("task operator source pattern mismatch");
        }
        return receipt(
                Role.TASK_OPERATOR,
                delegate.getClass().getName(),
                scoped.sourceFilePattern());
    }

    public static List<String> canonicalClassHookIds() {
        return stable(M3IopPatternClassHooks.hookIds());
    }

    public static boolean writesCanonicalSource() {
        return false;
    }

    public static boolean promotesCanonicalState() {
        return false;
    }

    private static Receipt receipt(Role role, String recipeClassName, String sourceFilePattern) {
        return new Receipt(
                role,
                recipeClassName,
                sourceFilePattern,
                canonicalClassHookIds(),
                true,
                true,
                true,
                false,
                false,
                "");
    }

    private static List<String> stable(List<String> values) {
        return Objects.requireNonNull(values, "values").stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
