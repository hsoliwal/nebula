// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable work crate for one LLM-assisted coding task.
 *
 * <p>The crate is the durable hand-off between demand/candidate reasoning and deterministic
 * OpenRewrite execution. It records what must be reused or created, but it grants no source
 * mutation or canonical promotion authority.</p>
 */
public record M3LlmTaskRecipeCrate(
        String taskId,
        String capabilityId,
        String requirementSha256,
        String catalogueSha256,
        Disposition disposition,
        List<String> recipeIds,
        Set<String> targetPaths,
        Set<String> donorReferences,
        Set<Gate> requiredGates) {

    public M3LlmTaskRecipeCrate {
        taskId = required(taskId, "taskId");
        capabilityId = required(capabilityId, "capabilityId");
        requirementSha256 = sha256(requirementSha256, "requirementSha256");
        catalogueSha256 = sha256(catalogueSha256, "catalogueSha256");
        disposition = Objects.requireNonNull(disposition, "disposition");
        recipeIds = stable(recipeIds, "recipeIds");
        targetPaths = normalizedPaths(targetPaths);
        donorReferences = stableSet(donorReferences, "donorReferences");
        requiredGates = Set.copyOf(Objects.requireNonNull(requiredGates, "requiredGates"));
        if (requiredGates.size() != Gate.values().length) {
            throw new IllegalArgumentException("all M3 recipe gates are mandatory");
        }
        if (disposition == Disposition.REUSE_OR_COMPOSE_RECIPE && recipeIds.isEmpty()) {
            throw new IllegalArgumentException("reuse disposition requires at least one recipe id");
        }
        if (disposition == Disposition.CREATE_OR_IMPROVE_RECIPE && !recipeIds.isEmpty()) {
            throw new IllegalArgumentException("new recipe disposition cannot contain admitted recipe ids");
        }
    }

    public boolean recipeCrateIsWorkUnit() {
        return true;
    }

    public boolean candidateOnly() {
        return true;
    }

    public boolean serialPromotionRequired() {
        return true;
    }

    public boolean directFileMutationAllowed() {
        return false;
    }

    public boolean donorSourceCopyAllowed() {
        return false;
    }

    public boolean canonicalPromotionAllowed() {
        return false;
    }

    public enum Disposition {
        REUSE_OR_COMPOSE_RECIPE,
        CREATE_OR_IMPROVE_RECIPE
    }

    /** Historical marketplace compatibility action retained from PR #6474. */
    public enum Action {
        REUSE_OR_COMPOSE_RECIPE,
        AUTHOR_OR_IMPROVE_RECIPE
    }

    /**
     * Historical content-addressed task envelope retained for canonical marketplace compatibility.
     */
    public record Task(
            String taskId,
            String capabilityId,
            String sourceFilePattern,
            String contractRoot,
            String sourceRoot,
            boolean jniBoundaryTouched,
            String root) {

        public Task {
            taskId = compatText(taskId, "taskId");
            capabilityId = compatText(capabilityId, "capabilityId");
            sourceFilePattern = compatText(sourceFilePattern, "sourceFilePattern");
            contractRoot = compatSha(contractRoot, "contractRoot");
            sourceRoot = compatSha(sourceRoot, "sourceRoot");
            String expected =
                    compatDigest(
                            "M3_LLM_TASK_RECIPE_CRATE_TASK_V1",
                            taskId,
                            capabilityId,
                            sourceFilePattern,
                            contractRoot,
                            sourceRoot,
                            Boolean.toString(jniBoundaryTouched));
            root = root == null || root.isBlank() ? expected : compatSha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("task root mismatch");
            }
        }

        public boolean directTargetFileMutationAuthority() {
            return false;
        }

        public boolean canonicalPromotionAuthority() {
            return false;
        }
    }

    /**
     * Historical work-order envelope retained exactly so stacked marketplace callers keep their
     * original recipe-first contract while the newer immutable crate model remains available.
     */
    public record WorkOrder(
            Task task,
            Action action,
            String recipeCrate,
            String mavenProfile,
            String suggestedRecipeClass,
            String suggestedTestClass,
            String suggestedFixtureDirectory,
            String problemEvidenceRoot,
            String donorReviewRoot,
            List<String> recipeCandidateClasses,
            List<String> recipeCandidateRoots,
            List<String> requiredStages,
            boolean recipeCrateIsWorkUnit,
            boolean atomLocalCandidates,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            boolean directTargetFileMutationAuthority,
            boolean donorSourceCopyAuthority,
            String root) {

        public WorkOrder {
            task = Objects.requireNonNull(task, "task");
            action = Objects.requireNonNull(action, "action");
            recipeCrate = compatText(recipeCrate, "recipeCrate");
            mavenProfile = compatText(mavenProfile, "mavenProfile");
            suggestedRecipeClass = compatText(suggestedRecipeClass, "suggestedRecipeClass");
            suggestedTestClass = compatText(suggestedTestClass, "suggestedTestClass");
            suggestedFixtureDirectory =
                    compatText(suggestedFixtureDirectory, "suggestedFixtureDirectory");
            problemEvidenceRoot = compatSha(problemEvidenceRoot, "problemEvidenceRoot");
            donorReviewRoot = compatSha(donorReviewRoot, "donorReviewRoot");
            recipeCandidateClasses =
                    List.copyOf(
                            Objects.requireNonNull(
                                    recipeCandidateClasses, "recipeCandidateClasses"));
            recipeCandidateRoots =
                    List.copyOf(
                            Objects.requireNonNull(recipeCandidateRoots, "recipeCandidateRoots"));
            requiredStages =
                    List.copyOf(Objects.requireNonNull(requiredStages, "requiredStages"));
            if (recipeCandidateClasses.size() != recipeCandidateRoots.size()) {
                throw new IllegalArgumentException("recipe candidate identity mismatch");
            }
            for (String candidateRoot : recipeCandidateRoots) {
                compatSha(candidateRoot, "recipeCandidateRoot");
            }
            if (action == Action.REUSE_OR_COMPOSE_RECIPE && recipeCandidateClasses.isEmpty()) {
                throw new IllegalArgumentException("recipe reuse requires at least one candidate");
            }
            if (!requiredStages.equals(M3RecipeFirstInvariant.canonicalStages())) {
                throw new IllegalArgumentException("non-canonical recipe-first stages");
            }
            if (!recipeCrateIsWorkUnit
                    || !atomLocalCandidates
                    || !candidateOnly
                    || !serialPromotionRequired
                    || directTargetFileMutationAuthority
                    || donorSourceCopyAuthority) {
                throw new IllegalArgumentException(
                        "M3 LLM recipe-crate authority boundary violated");
            }
            String expected =
                    compatDigest(
                            "M3_LLM_TASK_RECIPE_CRATE_WORK_ORDER_V1",
                            task.root(),
                            action.name(),
                            recipeCrate,
                            mavenProfile,
                            suggestedRecipeClass,
                            suggestedTestClass,
                            suggestedFixtureDirectory,
                            problemEvidenceRoot,
                            donorReviewRoot,
                            String.join("\u001f", recipeCandidateClasses),
                            String.join("\u001f", recipeCandidateRoots),
                            String.join("\u001f", requiredStages),
                            Boolean.toString(recipeCrateIsWorkUnit),
                            Boolean.toString(atomLocalCandidates),
                            Boolean.toString(candidateOnly),
                            Boolean.toString(serialPromotionRequired));
            root = root == null || root.isBlank() ? expected : compatSha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("work-order root mismatch");
            }
        }

        public boolean sourceChangeDryRunEligible() {
            return action == Action.REUSE_OR_COMPOSE_RECIPE;
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }

        public boolean canonicalPromotionAuthority() {
            return false;
        }
    }

    /**
     * Build the historical work order using the current admitted catalogue as the resolution oracle.
     */
    public static WorkOrder plan(
            Task task,
            M3RecipeCatalogue catalogue,
            String problemEvidenceRoot,
            String donorReviewRoot) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(catalogue, "catalogue");
        M3RecipeCatalogue.Resolution resolution =
                catalogue.resolveForLlmTask(task.capabilityId());
        List<M3RecipeDescriptor> candidates =
                resolution.candidates().stream()
                        .sorted(
                                Comparator.comparing(M3RecipeDescriptor::recipeClassName)
                                        .thenComparing(M3RecipeDescriptor::identitySha256))
                        .toList();
        Action action =
                resolution.disposition()
                                == M3RecipeCatalogue.Disposition.REUSE_OR_COMPOSE_RECIPE
                        ? Action.REUSE_OR_COMPOSE_RECIPE
                        : Action.AUTHOR_OR_IMPROVE_RECIPE;
        String suggestedRecipe =
                candidates.isEmpty()
                        ? "com.synexia.rewrite.M3"
                                + compatClassStem(task.capabilityId())
                                + "CandidateRecipe"
                        : candidates.getFirst().recipeClassName();
        String suggestedTest = suggestedRecipe + "Test";
        String fixtureDirectory =
                "synexia-openrewrite-recipes/src/test/resources/com/synexia/rewrite/llm/"
                        + compatSlug(task.capabilityId())
                        + "/";
        ArrayList<String> classes = new ArrayList<>(candidates.size());
        ArrayList<String> roots = new ArrayList<>(candidates.size());
        for (M3RecipeDescriptor candidate : candidates) {
            classes.add(candidate.recipeClassName());
            roots.add(candidate.identitySha256());
        }
        return new WorkOrder(
                task,
                action,
                "synexia-openrewrite-recipes",
                "m3-llm-recipe-first",
                suggestedRecipe,
                suggestedTest,
                fixtureDirectory,
                problemEvidenceRoot,
                donorReviewRoot,
                classes,
                roots,
                M3RecipeFirstInvariant.canonicalStages(),
                true,
                true,
                true,
                true,
                false,
                false,
                "");
    }

    public enum Gate {
        IDENTITY_PREIMAGE,
        CONTRACT_PRESERVATION,
        DIFF,
        STATIC_ANALYSIS,
        COMPILE,
        BEHAVIORAL_TEST,
        RUNTIME_IF_REQUIRED,
        JAVA_ORACLE,
        JNI_PARITY_IF_DECLARED,
        BENCHMARK_IF_PERFORMANCE_CLAIM,
        IDEMPOTENCE,
        PROVENANCE
    }

    private static String compatClassStem(String value) {
        StringBuilder out = new StringBuilder();
        boolean upper = true;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!Character.isLetterOrDigit(current)) {
                upper = true;
                continue;
            }
            char lower = Character.toLowerCase(current);
            out.append(upper ? Character.toUpperCase(lower) : lower);
            upper = false;
        }
        return out.isEmpty() ? "Capability" : out.toString();
    }

    private static String compatSlug(String value) {
        StringBuilder out = new StringBuilder();
        boolean separator = false;
        for (int index = 0; index < value.length(); index++) {
            char current = Character.toLowerCase(value.charAt(index));
            if (Character.isLetterOrDigit(current)) {
                if (separator && !out.isEmpty()) out.append('-');
                out.append(current);
                separator = false;
            } else {
                separator = true;
            }
        }
        return out.isEmpty() ? "capability" : out.toString();
    }

    private static String compatText(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.length() > 8192
                || checked.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String compatSha(String value, String field) {
        String checked = compatText(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String compatDigest(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                byte[] bytes =
                        Objects.requireNonNull(field, "field")
                                .getBytes(StandardCharsets.UTF_8);
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

    private static List<String> stable(List<String> values, String field) {
        Objects.requireNonNull(values, field);
        return values.stream()
                .map(value -> required(value, field + " entry"))
                .distinct()
                .sorted()
                .toList();
    }

    private static Set<String> stableSet(Set<String> values, String field) {
        return Set.copyOf(stable(
                Objects.requireNonNull(values, field).stream().toList(), field));
    }

    private static Set<String> normalizedPaths(Set<String> paths) {
        Objects.requireNonNull(paths, "targetPaths");
        return Set.copyOf(paths.stream()
                .map(path -> required(path, "targetPath").replace('\\', '/'))
                .peek(path -> {
                    if (path.startsWith("/") || path.contains("..")) {
                        throw new IllegalArgumentException("targetPath must be repository-relative");
                    }
                })
                .distinct()
                .sorted()
                .toList());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return value.strip();
    }

    private static String sha256(String value, String field) {
        String normalized = required(value, field).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be a 64-hex SHA-256");
        }
        return normalized;
    }
}
