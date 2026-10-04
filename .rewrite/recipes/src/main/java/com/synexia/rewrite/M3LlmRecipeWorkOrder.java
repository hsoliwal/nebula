// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Maven/OpenRewrite recipe-crate work order for one LLM-assisted M3 task.
 *
 * <p>The work unit is the reusable recipe crate, never an ad-hoc target-file edit. Known
 * capabilities dry-run the canonical IOP mutation recipe. Unknown capabilities open one bounded
 * recipe author/improve work order. Challenge catalogues remain evidence only.</p>
 */
public final class M3LlmRecipeWorkOrder {
    public enum Mode {
        DRY_RUN_CANONICAL_IOP_RECIPE,
        AUTHOR_OR_IMPROVE_RECIPE,
        HOLD_TYPED_RESIDUE
    }

    public record WorkOrder(
            String capabilityId,
            Mode mode,
            String mavenModule,
            String mavenProfile,
            String canonicalRecipeClass,
            String suggestedRecipeClass,
            String suggestedTestClass,
            String suggestedFixtureDirectory,
            String sourceFilePattern,
            String scope,
            String challengeSearchRoot,
            String donorReviewRoot,
            String javaCorpusReviewRoot,
            List<String> javaCorpusDonorSources,
            String authorityRoot,
            List<String> canonicalPassIds,
            List<String> requiredStages,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            boolean targetFileEditAuthority,
            boolean donorSourceCopyAuthority,
            String root) {

        public WorkOrder {
            capabilityId = text(capabilityId, "capabilityId");
            mode = Objects.requireNonNull(mode, "mode");
            mavenModule = text(mavenModule, "mavenModule");
            mavenProfile = text(mavenProfile, "mavenProfile");
            canonicalRecipeClass = text(canonicalRecipeClass, "canonicalRecipeClass");
            suggestedRecipeClass = text(suggestedRecipeClass, "suggestedRecipeClass");
            suggestedTestClass = text(suggestedTestClass, "suggestedTestClass");
            suggestedFixtureDirectory = text(suggestedFixtureDirectory, "suggestedFixtureDirectory");
            sourceFilePattern = text(sourceFilePattern, "sourceFilePattern");
            scope = M3LlmRecipeWorkOrder.scope(scope);
            challengeSearchRoot = sha(challengeSearchRoot, "challengeSearchRoot");
            donorReviewRoot = sha(donorReviewRoot, "donorReviewRoot");
            javaCorpusReviewRoot = optionalSha(javaCorpusReviewRoot, "javaCorpusReviewRoot");
            javaCorpusDonorSources =
                    stable(Objects.requireNonNull(javaCorpusDonorSources, "javaCorpusDonorSources"));
            if (!javaCorpusDonorSources.isEmpty() && javaCorpusReviewRoot.isEmpty()) {
                throw new IllegalArgumentException(
                        "pinned Java donor sources require javaCorpusReviewRoot");
            }
            authorityRoot = optionalSha(authorityRoot, "authorityRoot");
            canonicalPassIds = List.copyOf(Objects.requireNonNull(canonicalPassIds, "canonicalPassIds"));
            requiredStages = List.copyOf(Objects.requireNonNull(requiredStages, "requiredStages"));
            if (!requiredStages.equals(M3RecipeFirstInvariant.canonicalStages())) {
                throw new IllegalArgumentException("non-canonical M3 recipe-first stage order");
            }
            if (!candidateOnly || !serialPromotionRequired) {
                throw new IllegalArgumentException("work order must remain candidate-only with serial promotion");
            }
            if (targetFileEditAuthority || donorSourceCopyAuthority) {
                throw new IllegalArgumentException("work order cannot own target-file edit or donor-copy authority");
            }
            if (mode == Mode.DRY_RUN_CANONICAL_IOP_RECIPE && authorityRoot.isEmpty()) {
                throw new IllegalArgumentException("canonical dry-run requires IOP authority root");
            }
            String expected = digest(
                    "M3_LLM_RECIPE_WORK_ORDER_V3",
                    capabilityId,
                    mode.name(),
                    mavenModule,
                    mavenProfile,
                    canonicalRecipeClass,
                    suggestedRecipeClass,
                    suggestedTestClass,
                    suggestedFixtureDirectory,
                    sourceFilePattern,
                    scope,
                    challengeSearchRoot,
                    donorReviewRoot,
                    javaCorpusReviewRoot,
                    String.join("\u001f", javaCorpusDonorSources),
                    authorityRoot,
                    String.join("\u001f", canonicalPassIds),
                    String.join("\u001f", requiredStages),
                    Boolean.toString(candidateOnly),
                    Boolean.toString(serialPromotionRequired));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("work order root mismatch");
        }

        /** Source-compatible constructor for pre-corpus callers. */
        public WorkOrder(
                String capabilityId,
                Mode mode,
                String mavenModule,
                String mavenProfile,
                String canonicalRecipeClass,
                String suggestedRecipeClass,
                String suggestedTestClass,
                String suggestedFixtureDirectory,
                String sourceFilePattern,
                String scope,
                String challengeSearchRoot,
                String donorReviewRoot,
                String authorityRoot,
                List<String> canonicalPassIds,
                List<String> requiredStages,
                boolean candidateOnly,
                boolean serialPromotionRequired,
                boolean targetFileEditAuthority,
                boolean donorSourceCopyAuthority,
                String root) {
            this(
                    capabilityId,
                    mode,
                    mavenModule,
                    mavenProfile,
                    canonicalRecipeClass,
                    suggestedRecipeClass,
                    suggestedTestClass,
                    suggestedFixtureDirectory,
                    sourceFilePattern,
                    scope,
                    challengeSearchRoot,
                    donorReviewRoot,
                    "",
                    List.of(),
                    authorityRoot,
                    canonicalPassIds,
                    requiredStages,
                    candidateOnly,
                    serialPromotionRequired,
                    targetFileEditAuthority,
                    donorSourceCopyAuthority,
                    root);
        }

        /** Source-compatible constructor for pre-scope callers; FILE is the conservative default. */
        public WorkOrder(
                String capabilityId,
                Mode mode,
                String mavenModule,
                String mavenProfile,
                String canonicalRecipeClass,
                String suggestedRecipeClass,
                String suggestedTestClass,
                String suggestedFixtureDirectory,
                String sourceFilePattern,
                String challengeSearchRoot,
                String donorReviewRoot,
                String authorityRoot,
                List<String> canonicalPassIds,
                List<String> requiredStages,
                boolean candidateOnly,
                boolean serialPromotionRequired,
                boolean targetFileEditAuthority,
                boolean donorSourceCopyAuthority,
                String root) {
            this(
                    capabilityId,
                    mode,
                    mavenModule,
                    mavenProfile,
                    canonicalRecipeClass,
                    suggestedRecipeClass,
                    suggestedTestClass,
                    suggestedFixtureDirectory,
                    sourceFilePattern,
                    "FILE",
                    challengeSearchRoot,
                    donorReviewRoot,
                    "",
                    List.of(),
                    authorityRoot,
                    canonicalPassIds,
                    requiredStages,
                    candidateOnly,
                    serialPromotionRequired,
                    targetFileEditAuthority,
                    donorSourceCopyAuthority,
                    root);
        }

        public boolean recipeCrateIsWorkUnit() {
            return true;
        }

        /** Unknown capability work is mechanically routed through the IOP operator writer. */
        public boolean operatorWriterRequired() {
            return mode == Mode.AUTHOR_OR_IMPROVE_RECIPE;
        }

        public String operatorWriterClassName() {
            return M3IopOperatorRecipeWriter.class.getName();
        }

        /** Deterministic full M3/IOP/OpenRewrite/Maven execution contract for this recipe crate. */
        public M3IopRecipeCrateExecutionContract.Contract executionContract() {
            return M3IopRecipeCrateExecutionContract.from(this);
        }

        public String serialAtomReviewerClassName() {
            return M3RecipeCrateSerialAtomReview.class.getName();
        }
    }

    private M3LlmRecipeWorkOrder() {}

    public static WorkOrder from(M3LlmRecipeFirstRecipe recipe, M3LlmRecipeFirstRecipe.Plan plan) {
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(plan, "plan");

        Mode mode = switch (plan.action()) {
            case DRY_RUN_CANONICAL_IOP_RECIPE -> Mode.DRY_RUN_CANONICAL_IOP_RECIPE;
            case CREATE_OR_IMPROVE_RECIPE -> Mode.AUTHOR_OR_IMPROVE_RECIPE;
            case HOLD_FOR_REINVENTORY, TYPED_RESIDUE_NON_IOP, TYPED_RESIDUE_IOP_PATTERN ->
                    Mode.HOLD_TYPED_RESIDUE;
        };

        String stem = classStem(plan.capabilityId());
        String suggestedRecipe = mode == Mode.DRY_RUN_CANONICAL_IOP_RECIPE
                ? plan.canonicalRecipeClass()
                : "com.synexia.rewrite.M3" + stem + "CandidateRecipe";
        String suggestedTest = mode == Mode.DRY_RUN_CANONICAL_IOP_RECIPE
                ? "com.synexia.rewrite.M3IopPatternOnlySerialMechanicalJavaRecipeTest"
                : suggestedRecipe + "Test";
        String fixtureDirectory = "synexia-openrewrite-recipes/src/test/resources/com/synexia/rewrite/llm/"
                + slug(plan.capabilityId()) + "/";

        return new WorkOrder(
                plan.capabilityId(),
                mode,
                "synexia-openrewrite-recipes",
                "m3-llm-recipe-first",
                M3IopFullScaleMutationAuthority.canonicalRecipeClassName(),
                suggestedRecipe,
                suggestedTest,
                fixtureDirectory,
                recipe.getSourceFilePattern(),
                plan.scope(),
                plan.challengeSearchRoot(),
                plan.donorReviewRoot(),
                plan.javaCorpusReviewRoot(),
                plan.javaCorpusDonorSources(),
                plan.authorityRoot(),
                plan.passIds(),
                M3RecipeFirstInvariant.canonicalStages(),
                true,
                true,
                false,
                false,
                "");
    }

    private static String classStem(String value) {
        StringBuilder out = new StringBuilder();
        for (String token : value.replace('.', '_').replace('-', '_').split("_+")) {
            if (token.isBlank()) continue;
            String lower = token.toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return out.length() == 0 ? "Capability" : out.toString();
    }

    private static String slug(String value) {
        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "capability" : slug;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String scope(String value) {
        String checked = text(value, "scope").toUpperCase(Locale.ROOT);
        return switch (checked) {
            case "FILE", "PACKAGE", "MODULE", "PROJECT" -> checked;
            default -> throw new IllegalArgumentException("scope");
        };
    }

    private static List<String> stable(List<String> values) {
        return values.stream()
                .map(value -> text(value, "javaCorpusDonorSource"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String optionalSha(String value, String field) {
        String checked = Objects.requireNonNullElse(value, "").strip();
        if (checked.isEmpty()) return "";
        return sha(checked, field);
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
