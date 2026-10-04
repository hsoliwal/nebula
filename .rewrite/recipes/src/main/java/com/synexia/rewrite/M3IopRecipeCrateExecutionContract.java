// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic execution contract for one M3 LLM/OpenRewrite recipe crate.
 *
 * <p>This object binds the recipe-first invariant, IOP class hooks, canonical mutation recipe,
 * operator writer and Maven proof toolchain into one receipt. It never grants direct target-file,
 * donor-copy or promotion authority.</p>
 */
public final class M3IopRecipeCrateExecutionContract {
    public static final String VERSION = "M3_IOP_RECIPE_CRATE_EXECUTION_CONTRACT_V1";

    private static final List<String> PROOF_STAGES = List.of(
            "DIFF",
            "LINT_AND_STATIC_ANALYSIS",
            "COMPILE",
            "TEST",
            "RUNTIME_IF_REQUIRED",
            "PROMOTE");

    public record Contract(
            String invariantId,
            String capabilityId,
            String workOrderRoot,
            String mavenModule,
            String mavenProfile,
            String canonicalRecipeClass,
            String operatorWriterClass,
            List<String> classHookIds,
            List<String> recipeStages,
            List<String> proofStages,
            String mutationPlugin,
            List<String> proofPlugins,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            boolean targetFileEditAuthority,
            boolean donorSourceCopyAuthority,
            boolean promotionAuthority,
            String root) {

        public Contract {
            invariantId = text(invariantId, "invariantId");
            capabilityId = text(capabilityId, "capabilityId");
            workOrderRoot = sha(workOrderRoot, "workOrderRoot");
            mavenModule = text(mavenModule, "mavenModule");
            mavenProfile = text(mavenProfile, "mavenProfile");
            canonicalRecipeClass = text(canonicalRecipeClass, "canonicalRecipeClass");
            operatorWriterClass = text(operatorWriterClass, "operatorWriterClass");
            classHookIds = stable(classHookIds);
            recipeStages = List.copyOf(Objects.requireNonNull(recipeStages, "recipeStages"));
            proofStages = List.copyOf(Objects.requireNonNull(proofStages, "proofStages"));
            mutationPlugin = text(mutationPlugin, "mutationPlugin");
            proofPlugins = List.copyOf(Objects.requireNonNull(proofPlugins, "proofPlugins"));

            if (!M3RecipeFirstInvariant.INVARIANT_ID.equals(invariantId)) {
                throw new IllegalArgumentException("recipe-first invariant mismatch");
            }
            if (!"synexia-openrewrite-recipes".equals(mavenModule)
                    || !"m3-llm-recipe-first".equals(mavenProfile)) {
                throw new IllegalArgumentException("recipe crate/module profile mismatch");
            }
            if (!M3IopFullScaleMutationAuthority.canonicalRecipeClassName()
                    .equals(canonicalRecipeClass)) {
                throw new IllegalArgumentException("non-canonical IOP mutation recipe");
            }
            if (!M3IopOperatorRecipeWriter.class.getName().equals(operatorWriterClass)) {
                throw new IllegalArgumentException("non-canonical operator writer");
            }
            if (!classHookIds.equals(stable(M3IopPatternClassHooks.hookIds()))
                    || classHookIds.isEmpty()) {
                throw new IllegalArgumentException("class hooks do not match canonical IOP hooks");
            }
            if (!recipeStages.equals(M3RecipeFirstInvariant.canonicalStages())) {
                throw new IllegalArgumentException("recipe stage order mismatch");
            }
            if (!proofStages.equals(PROOF_STAGES)) {
                throw new IllegalArgumentException("proof stage order mismatch");
            }
            requireOrderedProofStages(recipeStages);
            if (!M3IopMavenToolchain.mutationPlugin().coordinate().equals(mutationPlugin)) {
                throw new IllegalArgumentException("OpenRewrite must remain the sole candidate mutator");
            }
            if (!proofPlugins.equals(M3IopMavenToolchain.canonicalProofCoordinates())) {
                throw new IllegalArgumentException("Maven proof plugin order mismatch");
            }
            if (!candidateOnly || !serialPromotionRequired) {
                throw new IllegalArgumentException("recipe crate must remain candidate-only with serial promotion");
            }
            if (targetFileEditAuthority || donorSourceCopyAuthority || promotionAuthority) {
                throw new IllegalArgumentException("recipe crate cannot own file/copy/promotion authority");
            }

            String expected = digest(
                    VERSION,
                    invariantId,
                    capabilityId,
                    workOrderRoot,
                    mavenModule,
                    mavenProfile,
                    canonicalRecipeClass,
                    operatorWriterClass,
                    String.join("\u001f", classHookIds),
                    String.join("\u001f", recipeStages),
                    String.join("\u001f", proofStages),
                    mutationPlugin,
                    String.join("\u001f", proofPlugins),
                    Boolean.toString(candidateOnly),
                    Boolean.toString(serialPromotionRequired));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("recipe crate execution-contract root mismatch");
            }
        }
    }

    private M3IopRecipeCrateExecutionContract() {}

    public static Contract from(M3LlmRecipeWorkOrder.WorkOrder workOrder) {
        M3LlmRecipeWorkOrder.WorkOrder work =
                Objects.requireNonNull(workOrder, "workOrder");

        if (!work.recipeCrateIsWorkUnit()) {
            throw new IllegalArgumentException("recipe crate must be the work unit");
        }
        if (!work.requiredStages().equals(M3RecipeFirstInvariant.canonicalStages())) {
            throw new IllegalArgumentException("work order stage order mismatch");
        }
        if (!work.candidateOnly() || !work.serialPromotionRequired()) {
            throw new IllegalArgumentException("work order lost candidate-only/serial-promotion law");
        }
        if (work.targetFileEditAuthority() || work.donorSourceCopyAuthority()) {
            throw new IllegalArgumentException("work order cannot mutate files or copy donor source directly");
        }
        if (work.mode() == M3LlmRecipeWorkOrder.Mode.AUTHOR_OR_IMPROVE_RECIPE
                && !work.operatorWriterRequired()) {
            throw new IllegalArgumentException("missing operator writer for unknown capability");
        }

        return new Contract(
                M3RecipeFirstInvariant.INVARIANT_ID,
                work.capabilityId(),
                work.root(),
                work.mavenModule(),
                work.mavenProfile(),
                work.canonicalRecipeClass(),
                work.operatorWriterClassName(),
                M3IopPatternClassHooks.hookIds(),
                work.requiredStages(),
                PROOF_STAGES,
                M3IopMavenToolchain.mutationPlugin().coordinate(),
                M3IopMavenToolchain.canonicalProofCoordinates(),
                true,
                true,
                false,
                false,
                false,
                "");
    }

    public static List<String> proofStages() {
        return PROOF_STAGES;
    }

    public static void requireOrderedProofStages(List<String> stages) {
        List<String> values = List.copyOf(Objects.requireNonNull(stages, "stages"));
        int diff = index(values, "DIFF");
        int lint = index(values, "LINT_AND_STATIC_ANALYSIS");
        int compile = index(values, "COMPILE");
        int test = index(values, "TEST");
        int runtime = index(values, "RUNTIME_IF_REQUIRED");
        int promote = index(values, "PROMOTE");
        if (!(diff < lint && lint < compile && compile < test && test < runtime && runtime < promote)) {
            throw new IllegalArgumentException(
                    "M3 proof order must be DIFF -> LINT -> COMPILE -> TEST -> RUNTIME -> PROMOTE");
        }
    }

    private static int index(List<String> stages, String stage) {
        int index = stages.indexOf(stage);
        if (index < 0) throw new IllegalArgumentException("missing M3 stage: " + stage);
        return index;
    }

    private static List<String> stable(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNullElse(value, "").strip();
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
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
