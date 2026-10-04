// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Deterministic next-action plan for one generic LLM recipe crate. */
public record M3LlmTaskRecipeDevelopmentPlan(
        String crateRoot,
        M3LlmTaskRecipeCrate.Disposition disposition,
        String suggestedRecipeName,
        String suggestedSourcePattern,
        List<String> recipeIds,
        List<String> requiredStages,
        String bootstrapGoal,
        boolean targetFileEditAuthority,
        boolean donorSourceCopyAuthority,
        boolean promotionAuthority,
        String root) {

    public M3LlmTaskRecipeDevelopmentPlan {
        crateRoot = sha(crateRoot, "crateRoot");
        disposition = Objects.requireNonNull(disposition, "disposition");
        suggestedRecipeName = text(suggestedRecipeName, "suggestedRecipeName");
        suggestedSourcePattern = text(suggestedSourcePattern, "suggestedSourcePattern");
        recipeIds = List.copyOf(Objects.requireNonNull(recipeIds, "recipeIds"));
        requiredStages = List.copyOf(Objects.requireNonNull(requiredStages, "requiredStages"));
        bootstrapGoal = text(bootstrapGoal, "bootstrapGoal");
        if (targetFileEditAuthority || donorSourceCopyAuthority || promotionAuthority) {
            throw new IllegalArgumentException("development plan cannot own mutation/copy/promotion authority");
        }
        String expected = digest(
                "M3_LLM_TASK_RECIPE_DEVELOPMENT_PLAN_V1",
                crateRoot,
                disposition.name(),
                suggestedRecipeName,
                suggestedSourcePattern,
                String.join("\u001f", recipeIds),
                String.join("\u001f", requiredStages),
                bootstrapGoal);
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!root.equals(expected)) throw new IllegalArgumentException("development plan root mismatch");
    }

    public static M3LlmTaskRecipeDevelopmentPlan from(M3LlmTaskRecipeCrate crate) {
        M3LlmTaskRecipeCrate checked = Objects.requireNonNull(crate, "crate");
        String recipeName = "synexia.m3." + classToken(checked.capabilityId());
        String pattern = sourcePattern(checked.targetPaths().stream().toList());
        String bootstrap = "com.synexia.maven:ai-maven-plugin:m3-recipe-bootstrap";
        return new M3LlmTaskRecipeDevelopmentPlan(
                M3LlmTaskRecipeCrateCodec.root(checked),
                checked.disposition(),
                recipeName,
                pattern,
                checked.recipeIds(),
                M3RecipeFirstInvariant.canonicalStages(),
                bootstrap,
                false,
                false,
                false,
                "");
    }

    public String toTsv() {
        StringBuilder out = new StringBuilder("key\tvalue\n");
        row(out, "root", root);
        row(out, "crateRoot", crateRoot);
        row(out, "disposition", disposition.name());
        row(out, "suggestedRecipeName", suggestedRecipeName);
        row(out, "suggestedSourcePattern", suggestedSourcePattern);
        recipeIds.forEach(value -> row(out, "recipe", value));
        requiredStages.forEach(value -> row(out, "stage", value));
        row(out, "bootstrapGoal", bootstrapGoal);
        row(out, "targetFileEditAuthority", "false");
        row(out, "donorSourceCopyAuthority", "false");
        row(out, "promotionAuthority", "false");
        return out.toString();
    }

    public String instructions(String recipeRoot) {
        String checkedRoot = text(recipeRoot, "recipeRoot");
        if (disposition == M3LlmTaskRecipeCrate.Disposition.REUSE_OR_COMPOSE_RECIPE) {
            return "Reuse/compose the admitted recipe ids in the crate. Run candidate-only dry-run, "
                    + "serial atom review, proof gates and fixed-point verification before promotion.\n";
        }
        return "Create/improve one reusable recipe instead of editing matching target files.\n"
                + "Bootstrap command:\n"
                + "mvn " + bootstrapGoal
                + " -Dm3.recipeRoot=" + checkedRoot
                + " -Dm3.recipeName=" + suggestedRecipeName
                + " -Dm3.recipeSourcePattern='" + suggestedSourcePattern + "'"
                + " -Dm3.recipeLanguage=" + suggestedLanguage() + "\n"
                + "Then improve rewrite.yml/fixtures, dry-run the corpus, apply only after review, "
                + "and prove the second-pass fixed point.\n";
    }

    public String suggestedLanguage() {
        if (suggestedSourcePattern.endsWith(".java")
                || suggestedSourcePattern.contains("*.java")) {
            return "JAVA";
        }
        if (suggestedSourcePattern.endsWith(".py")
                || suggestedSourcePattern.endsWith(".pyi")
                || suggestedSourcePattern.contains("*.py")) {
            return "PYTHON";
        }
        return "AUTO";
    }

    private static String sourcePattern(List<String> targets) {
        if (targets.isEmpty()) return "**/*.java";
        boolean java = targets.stream().allMatch(value -> value.endsWith(".java"));
        boolean python = targets.stream().allMatch(value -> value.endsWith(".py") || value.endsWith(".pyi"));
        if (java) return "**/*.java";
        if (python) return "**/*.py";
        return "**/*";
    }

    private static String classToken(String value) {
        StringBuilder result = new StringBuilder();
        for (String token : value.split("[^A-Za-z0-9]+")) {
            if (token.isEmpty()) continue;
            result.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) result.append(token.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.isEmpty() ? "GeneratedRecipe" : result + "Recipe";
    }

    private static void row(StringBuilder out, String key, String value) {
        if (value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("TSV value must be single-line");
        }
        out.append(key).append('\t').append(value).append('\n');
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
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String sha(String value, String field) {
        String checked = text(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }
}
